package ec.edu.puce.scantech.ui.scanner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ec.edu.puce.scantech.appContainer
import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.usecase.ObtenerActivoPorQrUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [RDA-1 Tema 4: Ciclo de Vida y Resiliencia ante Process Death con SavedStateHandle]
 * [RDA-3 Temas 9 y 10: MVVM + UDF y Concurrencia Estructurada]
 * [RDA-3 Temas 11 y 12: Verificación de Existencia Local en SQLite (Room)]
 *
 * ViewModel especializado para la pantalla de escaneo óptico inteligente (Tareas #4, #5 y #27).
 *
 * Conceptos Teóricos Aplicados:
 * 1. Resiliencia de Proceso (SavedStateHandle): El estado del hardware (linterna y pausa) se persiste
 *    en `SavedStateHandle`. Si el sistema operativo destruye la Activity por rotación o liquida el proceso
 *    por falta de RAM (*Process Death*), el visor recupera su estado sin perder continuidad.
 * 2. Flujo Unidireccional de Datos (UDF):
 *    - Los eventos suben desde la UI (ej. `onCodeScanned()`, `toggleFlashlight()`).
 *    - El estado fluye hacia abajo mediante [StateFlow] inmutable (`uiState`), garantizando que la vista
 *      no altere la lógica interna por cuenta propia.
 * 3. Verificación Local Inmediata (Tarea #5): Al decodificar el código QR con ML Kit, consulta la base de datos
 *    local Room. Si existe, despacha la navegación a la Ficha Técnica; si no existe, emite el estado
 *    para mostrar el diálogo de alta asistida con código prellenado.
 */
class ScanViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val obtenerActivoPorQrUseCase: ObtenerActivoPorQrUseCase
) : ViewModel() {

    companion object {
        private const val KEY_FLASHLIGHT = "key_scan_flashlight_state"
        private const val KEY_SCAN_PAUSED = "key_scan_paused_state"

        /** Factory que inyecta [ObtenerActivoPorQrUseCase] con el repositorio de [appContainer]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = appContainer().assetRepository
                ScanViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    obtenerActivoPorQrUseCase = ObtenerActivoPorQrUseCase(repository)
                )
            }
        }
    }

    // Flujos respaldados en disco/bundle por SavedStateHandle
    val isFlashlightOn: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_FLASHLIGHT, false)
    val isScanningPaused: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_SCAN_PAUSED, false)

    // Estado efímero de análisis y navegación
    private data class InternalScanState(
        val isAnalyzing: Boolean = false,
        val lastDetectedQr: String? = null,
        val navigateToAssetId: String? = null,
        val unregisteredQrCode: String? = null
    )

    private val _internalScanState = MutableStateFlow(InternalScanState())

    // Estado reactivo compuesto expuesto para la vista (UDF con Flow combine tipado)
    val uiState: StateFlow<ScanUiState> = combine(
        isFlashlightOn,
        isScanningPaused,
        _internalScanState
    ) { flashlight, paused, internal ->
        ScanUiState(
            isFlashlightOn = flashlight,
            isScanningPaused = paused,
            isAnalyzing = internal.isAnalyzing,
            lastDetectedQr = internal.lastDetectedQr,
            navigateToAssetId = internal.navigateToAssetId,
            unregisteredQrCode = internal.unregisteredQrCode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScanUiState(
            isFlashlightOn = savedStateHandle[KEY_FLASHLIGHT] ?: false,
            isScanningPaused = savedStateHandle[KEY_SCAN_PAUSED] ?: false
        )
    )

    /**
     * Alterna el estado de la linterna y persiste el nuevo valor en [SavedStateHandle].
     */
    fun toggleFlashlight() {
        val currentState = isFlashlightOn.value
        savedStateHandle[KEY_FLASHLIGHT] = !currentState
    }

    /**
     * Pausa o reanuda el análisis óptico de frames de la cámara.
     */
    fun setScanningPaused(paused: Boolean) {
        savedStateHandle[KEY_SCAN_PAUSED] = paused
    }

    /**
     * Procesa un código QR detectado por ML Kit o simulado.
     * Consulta inmediatamente a Room mediante [ObtenerActivoPorQrUseCase] (Tarea #5).
     */
    fun onCodeScanned(qrCode: String) {
        val codigoLimpio = qrCode.trim()
        if (codigoLimpio.isBlank() || isScanningPaused.value) return

        // Pausar escáner para evitar detecciones duplicadas
        setScanningPaused(true)
        _internalScanState.update {
            it.copy(isAnalyzing = true, lastDetectedQr = codigoLimpio)
        }

        viewModelScope.launch {
            when (val result = obtenerActivoPorQrUseCase(codigoLimpio)) {
                is Result.Success -> {
                    // El activo existe localmente: preparamos navegación al detalle
                    _internalScanState.update {
                        it.copy(
                            isAnalyzing = false,
                            navigateToAssetId = result.data.id
                        )
                    }
                }
                is Result.Error -> {
                    // El activo no existe en la base local: mostramos diálogo para registrar
                    _internalScanState.update {
                        it.copy(
                            isAnalyzing = false,
                            unregisteredQrCode = codigoLimpio
                        )
                    }
                }
            }
        }
    }

    /**
     * Notifica que la navegación hacia el detalle ya fue consumida por el Composable.
     */
    fun onNavigationHandled() {
        _internalScanState.update { it.copy(navigateToAssetId = null) }
    }

    /**
     * Descarta el diálogo de activo no registrado y reanuda el escaneo.
     */
    fun dismissUnregisteredDialog() {
        _internalScanState.update {
            it.copy(unregisteredQrCode = null, lastDetectedQr = null)
        }
        setScanningPaused(false)
    }

    /**
     * Reinicia el escáner para una nueva lectura manual.
     */
    fun resumeScanning() {
        _internalScanState.value = InternalScanState()
        setScanningPaused(false)
    }
}
