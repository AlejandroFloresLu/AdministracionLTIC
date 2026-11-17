package ec.edu.puce.scantech.ui.scanner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * [RDA-1 Tema 4: Ciclo de Vida y Resiliencia ante Process Death con SavedStateHandle]
 * [RDA-3 Temas 9 y 10: MVVM + UDF y Concurrencia Estructurada]
 *
 * ViewModel especializado para la pantalla de escaneo óptico inteligente.
 *
 * Conceptos Teóricos Aplicados:
 * 1. Resiliencia de Proceso (SavedStateHandle): Las preferencias efímeras del hardware de la cámara
 *    (linterna y pausa de escaneo) se guardan en el mapa serializado de `SavedStateHandle`. Si el sistema
 *    operativo mata el proceso en segundo plano por presión de memoria RAM (*Process Death*), el estado
 *    se restaura de forma automática al regresar.
 * 2. Flujo Unidireccional de Datos (UDF):
 *    - Los eventos suben desde la UI (ej. `toggleFlashlight()`).
 *    - El estado fluye hacia abajo mediante [StateFlow] inmutable (`uiState`), garantizando que la vista
 *      no altere la lógica interna por cuenta propia.
 * 3. Desacoplamiento (Anti-God Activity): La cámara tiene su propio ciclo y estado; no comparte responsabilidades
 *    con el inventario general (`InventoryViewModel`).
 */
class ScanViewModel(
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    companion object {
        private const val KEY_FLASHLIGHT = "key_scan_flashlight_state"
        private const val KEY_SCAN_PAUSED = "key_scan_paused_state"
    }

    // Flujos respaldados en disco/bundle por SavedStateHandle
    val isFlashlightOn: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_FLASHLIGHT, false)
    val isScanningPaused: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_SCAN_PAUSED, false)

    private val _lastDetectedQr = MutableStateFlow<String?>(null)

    // Estado reactivo compuesto expuesto para la vista
    val uiState: StateFlow<ScanUiState> = combine(
        isFlashlightOn,
        isScanningPaused,
        _lastDetectedQr
    ) { flashlight, paused, lastQr ->
        ScanUiState(
            isFlashlightOn = flashlight,
            isScanningPaused = paused,
            lastDetectedQr = lastQr
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
     * Registra un código QR detectado y pausa temporalmente para evitar lecturas duplicadas (antirrebote).
     */
    fun onCodeScanned(qrCode: String) {
        _lastDetectedQr.value = qrCode
        setScanningPaused(true)
    }

    /**
     * Reinicia el escáner para una nueva lectura.
     */
    fun resumeScanning() {
        _lastDetectedQr.value = null
        setScanningPaused(false)
    }
}

