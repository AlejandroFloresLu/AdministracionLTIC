package ec.edu.puce.scantech.ui.scanner

/**
 * [RDA-1 Tema 5: UI Declarativa - Estado como Única Fuente de Verdad]
 * [RDA-3 Temas 9 y 10: Flujo Unidireccional de Datos (UDF) y Arquitectura MVVM]
 *
 * Modela el estado visual y operativo del visor de escaneo óptico Edge AI.
 *
 * Conceptos Teóricos Aplicados:
 * 1. UI = f(State): La pantalla `ScannerScreen` no mantiene variables mutables sueltas;
 *    se comporta como un renderizador puro del estado inmutable emitido por el `ScanViewModel`.
 * 2. Inmutabilidad: Modela datos inmutables donde cualquier cambio (ej. encender linterna o pausar)
 *    produce una nueva instancia mediante `copy()`, evitando estados inconsistentes y condiciones de carrera.
 */
data class ScanUiState(
    val isFlashlightOn: Boolean = false,
    val isScanningPaused: Boolean = false,
    val lastDetectedQr: String? = null
)

