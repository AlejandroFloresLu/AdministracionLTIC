package ec.edu.puce.scantech.ui.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ec.edu.puce.scantech.data.local.AppDatabase
import ec.edu.puce.scantech.data.remote.RetrofitClient
import ec.edu.puce.scantech.data.repository.AssetRepositoryImpl
import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.usecase.EliminarActivoUseCase
import ec.edu.puce.scantech.domain.usecase.GuardarObservacionesUseCase
import ec.edu.puce.scantech.domain.usecase.ObtenerActivoPorIdUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AssetDetailUiState>(AssetDetailUiState.Loading)
    val uiState: StateFlow<AssetDetailUiState> = _uiState.asStateFlow()

    init {
        observarActivo()
    }

    /**
     * Se suscribe al [kotlinx.coroutines.flow.Flow] emitido por Room para mantener los datos de la ficha
     * sincronizados en tiempo real si el activo es modificado desde otra pantalla o por sincronización remota.
     */
    private fun observarActivo() {
        viewModelScope.launch {
            obtenerActivoPorIdUseCase(assetId)
                .catch { error ->
                    _uiState.value = AssetDetailUiState.Error(
                        mensaje = error.localizedMessage ?: "Error al consultar el activo en la base de datos"
                    )
                }
                .collect { asset ->
                    if (asset == null) {
                        _uiState.value = AssetDetailUiState.Error(
                            mensaje = "El activo con ID $assetId no existe o fue dado de baja."
                        )
                    } else {
                        _uiState.value = AssetDetailUiState.Success(asset = asset)
                    }
                }
        }
    }

    /**
     * Guarda las observaciones físicas del hardware en Room y encola la actualización
     * (Offline-First). Solo se escriben las observaciones: el resto del activo no se toca.
     * La ficha se refresca sola porque observa el Flow de Room.
     */
    fun updateObservaciones(nuevasObservaciones: String, onCompleted: () -> Unit = {}) {
        if (_uiState.value !is AssetDetailUiState.Success) return
        viewModelScope.launch {
            if (guardarObservacionesUseCase(assetId, nuevasObservaciones) is Result.Success) {
                onCompleted()
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
     * Factory para inicializar [AssetDetailViewModel] inyectando dependencias de la base local y casos de uso.
     */
    companion object {
        fun provideFactory(
            context: Context,
            assetId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = AppDatabase.obtener(context)
                val api = RetrofitClient.crearAssetApi("https://api.scantech.puce.edu.ec/")
                val repository = AssetRepositoryImpl(
                    assetDao = database.assetDao(),
                    syncQueueDao = database.syncQueueDao(),
                    api = api
                )

                return AssetDetailViewModel(
                    assetId = assetId,
                    obtenerActivoPorIdUseCase = ObtenerActivoPorIdUseCase(repository),
                    eliminarActivoUseCase = EliminarActivoUseCase(repository),
                    guardarObservacionesUseCase = GuardarObservacionesUseCase(repository)
                ) as T
            }
        }
    }
}
