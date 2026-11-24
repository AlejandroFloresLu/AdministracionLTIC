package ec.edu.puce.scantech.ui.scanner

/**
 * [RDA-1 Tema 5: UI Declarativa - Estado como Única Fuente de Verdad]
 * [RDA-3 Temas 9 y 10: Flujo Unidireccional de Datos (UDF) y Arquitectura MVVM]
 *
 * Modela el estado visual y operativo del visor de escaneo óptico Edge AI (Tareas #4, #5 y #27).
 *
 * Conceptos Teóricos Aplicados:
 * 1. UI = f(State): La pantalla `ScannerScreen` es un renderizador puro del estado inmutable emitido por [ScanViewModel].
 * 2. Inmutabilidad: Modela datos inmutables donde cualquier cambio (linterna, pausa, detección o activo no registrado)
 *    produce una nueva instancia mediante `copy()`, evitando condiciones de carrera.
 */
data class ScanUiState(
    val isFlashlightOn: Boolean = false,
    val isScanningPaused: Boolean = false,
    val isAnalyzing: Boolean = false,
    val lastDetectedQr: String? = null,
    val navigateToAssetId: String? = null,
    val unregisteredQrCode: String? = null
)
