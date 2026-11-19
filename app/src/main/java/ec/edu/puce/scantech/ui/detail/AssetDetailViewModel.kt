package ec.edu.puce.scantech.ui.detail

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
        observarActivo()
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
            /* =================== [DEFENSA CASO 19: Despacho en Dispatchers.IO en ViewModel] ===================
            // 🛠️ PASO PARA ACTIVAR EN EL EXAMEN:
            // Para asegurar aislamiento del hilo principal, envuelve la llamada del use case en:
            // withContext(Dispatchers.IO) { ... }

            // 💡 RESPUESTA TEÓRICA RÁPIDA:
            // - Dispatchers.Main: Optimizado para manipulación de vistas de UI en el hilo principal (60/120 FPS).
            // - Dispatchers.IO: Pool compartido de hilos elásticos optimizado para operaciones de bloqueo de entrada/salida
            //   (I/O), como lecturas/escrituras en disco (Room/SQLite) y llamadas a red (Retrofit).
            // - Previene el error ANR (Application Not Responding) que Android dispara si el Main Thread se bloquea >5 segundos.
            ================================================================================================== */
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

    /* =================== [DEFENSA CASO 16: Sobrescribir onCleared() para Auditoría de Ciclo de Vida] ===================
    // 🛠️ PASO PARA ACTIVAR EN EL EXAMEN:
    // Descomenta este método override del ViewModel:
    // override fun onCleared() {
    //     super.onCleared()
    //     val activoActual = (_uiState.value as? AssetDetailUiState.Success)?.asset
    //     android.util.Log.i(
    //         "ScanTech_Lifecycle",
    //         "Auditoría: AssetDetailViewModel finalizado. Último activo inspeccionado: ${activoActual?.name ?: "ID: $assetId"}"
    //     )
    // }

    // 💡 RESPUESTA TEÓRICA RÁPIDA:
    // - 'onCleared()' se invoca ÚNICAMENTE cuando el ViewModel va a ser destruido definitivamente
    //   (ej. el usuario navega hacia atrás o la actividad finaliza con 'finish()').
    // - NO se ejecuta durante rotaciones de pantalla (Configuration Changes), ya que el ViewModelStore
    //   retiene la instancia viva en memoria a través de NonConfigurationInstances.
    // - Es el punto canónico para cancelar suscripciones manuales, cerrar conexiones abiertas o registrar auditoría.
    ==================================================================================================================== */

    /**
     * Factory que arma los casos de uso con el repositorio compartido del
     * [ec.edu.puce.scantech.di.AppContainer]; el ViewModel no crea Room ni Retrofit.
     */
    companion object {
        /** Clave del borrador en [SavedStateHandle]; `null` = sin cambios respecto a Room. */
        private const val KEY_BORRADOR_OBSERVACIONES = "observaciones_borrador"

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
