package ec.edu.puce.scantech.ui.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ec.edu.puce.scantech.appContainer
import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.usecase.EliminarActivoUseCase
import ec.edu.puce.scantech.domain.usecase.GuardarObservacionesUseCase
import ec.edu.puce.scantech.domain.usecase.ObtenerActivoPorIdUseCase
import ec.edu.puce.scantech.domain.validacion.ValidadorActivo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [RDA-1 Tema 4: Ciclo de Vida y Resiliencia ante Cambios de Configuración]
 * [RDA-3 Temas 9 y 10: MVVM + Clean Architecture - Inyección de Casos de Uso]
 * [RDA-3 Temas 11 y 12: Observabilidad Reactiva de la Fuente Única de Verdad (Room)]
 *
 * ViewModel especializado en la orquestación y ciclo de vida de la Ficha Técnica del Activo.
 *
 * Conceptos Teóricos Aplicados:
 * 1. Desacoplamiento (SoC): Separa la presentación de la lógica de negocio; interactúa
 *    exclusivamente con la capa de dominio a través de casos de uso puros.
 * 2. Concurrencia Estructurada (viewModelScope): Toda corrutina se delimita al ciclo de vida
 *    del ViewModel. Si la pantalla se destruye de forma definitiva, los trabajos pendientes
 *    se cancelan automáticamente liberando memoria y evitando leaks.
 * 3. Hilos de Fondo (Dispatchers.IO): Las operaciones de persistencia en disco y transacciones
 *    se despachan fuera del hilo principal, garantizando fluidez a 60 FPS y cero ANRs.
 */
class AssetDetailViewModel(
    private val assetId: String,
    private val obtenerActivoPorIdUseCase: ObtenerActivoPorIdUseCase,
    private val eliminarActivoUseCase: EliminarActivoUseCase,
    private val guardarObservacionesUseCase: GuardarObservacionesUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<AssetDetailUiState>(AssetDetailUiState.Loading)
    val uiState: StateFlow<AssetDetailUiState> = _uiState.asStateFlow()

    private val guardandoObservaciones = MutableStateFlow(false)

    private val _eventos = Channel<ObservacionesEvento>(Channel.BUFFERED)

    /** Resultado de cada guardado de observaciones, para mostrar un Snackbar una sola vez. */
    val eventos: Flow<ObservacionesEvento> = _eventos.receiveAsFlow()

    init {
        // Caso de práctica 5: se imprime una sola vez por ficha abierta; al rotar NO se repite,
        // porque el ViewModelStore conserva esta misma instancia.
        Log.d(TAG_CICLO_VIDA, "Ficha del activo creada: $assetId")
        observarActivo()
    }

    /**
     * [Caso de práctica 5: Ciclo de vida del ViewModel]
     *
     * Android llama a este método una sola vez, cuando el destino sale del back stack para
     * siempre (flecha atrás, popBackStack o dar de baja). No se llama al rotar la pantalla:
     * la Activity se recrea, pero el ViewModel sigue vivo. `viewModelScope` se cancela justo
     * antes, así que la observación de Room ya quedó detenida.
     */
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG_CICLO_VIDA, "Memoria de la ficha liberada: $assetId")
    }

    /**
     * Se suscribe al [kotlinx.coroutines.flow.Flow] emitido por Room para mantener los datos de la ficha
     * sincronizados en tiempo real si el activo es modificado desde otra pantalla o por sincronización remota.
     */
    private fun observarActivo() {
        viewModelScope.launch {
            combine(
                obtenerActivoPorIdUseCase(assetId),
                savedStateHandle.getStateFlow<String?>(KEY_BORRADOR_OBSERVACIONES, null),
                guardandoObservaciones
            ) { asset, borrador, guardando -> Triple(asset, borrador, guardando) }
                .catch { error ->
                    _uiState.value = AssetDetailUiState.Error(
                        mensaje = error.localizedMessage ?: "Error al consultar el activo en la base de datos"
                    )
                }
                .collect { (asset, borrador, guardando) ->
                    if (asset == null) {
                        _uiState.value = AssetDetailUiState.Error(
                            mensaje = "El activo con ID $assetId no existe o fue dado de baja."
                        )
                    } else {
                        val guardadas = asset.observations.orEmpty()
                        // Sin cambios pendientes: se suelta el borrador para que el campo siga a Room.
                        if (borrador != null && borrador == guardadas) {
                            savedStateHandle[KEY_BORRADOR_OBSERVACIONES] = null
                        }
                        _uiState.value = AssetDetailUiState.Success(
                            asset = asset,
                            isDeleting = (_uiState.value as? AssetDetailUiState.Success)?.isDeleting ?: false,
                            observacionesDraft = borrador ?: guardadas,
                            isSavingObservaciones = guardando
                        )
                    }
                }
        }
    }

    /** Actualiza el borrador de observaciones (limitado a [ValidadorActivo.MAX_OBSERVACIONES]). */
    fun onObservacionesChange(texto: String) {
        savedStateHandle[KEY_BORRADOR_OBSERVACIONES] = texto.take(ValidadorActivo.MAX_OBSERVACIONES)
    }

    /** Descarta el borrador y vuelve a mostrar las observaciones guardadas en Room. */
    fun descartarObservaciones() {
        savedStateHandle[KEY_BORRADOR_OBSERVACIONES] = null
    }

    /**
     * Guarda las observaciones físicas del hardware en Room y encola la actualización
     * (Offline-First) mediante [GuardarObservacionesUseCase]. Solo se escriben las observaciones:
     * el resto del activo no se toca. El SyncStatus nuevo llega solo porque la ficha observa Room.
     */
    fun guardarObservaciones() {
        val estado = _uiState.value as? AssetDetailUiState.Success ?: return
        if (guardandoObservaciones.value || !estado.hayCambiosObservaciones) return
        guardandoObservaciones.value = true
        viewModelScope.launch {
            try {
                when (val resultado = guardarObservacionesUseCase(assetId, estado.observacionesDraft)) {
                    is Result.Success -> {
                        // Se muestra el texto tal como quedó guardado (recortado) hasta que Room emita.
                        savedStateHandle[KEY_BORRADOR_OBSERVACIONES] = resultado.data.observations.orEmpty()
                        _eventos.send(ObservacionesEvento.Guardadas)
                    }
                    is Result.Error -> _eventos.send(ObservacionesEvento.Error(resultado.message))
                }
            } finally {
                guardandoObservaciones.value = false
            }
        }
    }

    /**
     * Procesa la baja lógica del activo tecnológico.
     */
    fun eliminarActivo(onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val resultado = eliminarActivoUseCase(assetId)
            withContext(Dispatchers.Main) {
                if (resultado is Result.Success) {
                    onSuccess()
                } else if (resultado is Result.Error) {
                    _uiState.value = AssetDetailUiState.Error(resultado.message)
                }
            }
        }
    }

    /**
     * Factory que arma los casos de uso con el repositorio compartido del
     * [ec.edu.puce.scantech.di.AppContainer]; el ViewModel no crea Room ni Retrofit.
     */
    companion object {
        /** Clave del borrador en [SavedStateHandle]; `null` = sin cambios respecto a Room. */
        private const val KEY_BORRADOR_OBSERVACIONES = "observaciones_borrador"

        /** Etiqueta para filtrar en Logcat: `tag:ScanTech_Lifecycle`. */
        const val TAG_CICLO_VIDA = "ScanTech_Lifecycle"

        fun provideFactory(assetId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = appContainer().assetRepository
                AssetDetailViewModel(
                    assetId = assetId,
                    obtenerActivoPorIdUseCase = ObtenerActivoPorIdUseCase(repository),
                    eliminarActivoUseCase = EliminarActivoUseCase(repository),
                    guardarObservacionesUseCase = GuardarObservacionesUseCase(repository),
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}
