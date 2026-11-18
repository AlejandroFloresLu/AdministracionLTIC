package ec.edu.puce.scantech.ui.form

import ec.edu.puce.scantech.domain.validacion.CampoActivo

/**
 * [RDA-1 Tema 5: UI Declarativa - Estado Completo e Inmutable para Formularios]
 * [RDA-3 Temas 9 y 10: UDF y Resiliencia en Registro de Datos Críticos]
 *
 * Estado inmutable del Formulario Ultra-Resiliente de Alta y Edición de Activos.
 *
 * Conceptos Teóricos Aplicados:
 * 1. Validación Reactiva por Campo: El mapa [errores] contiene los mensajes de error asociados a cada
 *    [CampoActivo], permitiendo que la interfaz muestre `isError = true` y `supportingText` en tiempo real.
 * 2. Unidirectional Data Flow (UDF): Modela de forma explícita las banderas de estado (`isLoading`,
 *    `isSaving`, `isSavedSuccess`, `isValid`) blindando al usuario contra clicks múltiples o envíos duplicados.
 */
data class AssetFormUiState(
    val qrCode: String = "",
    val name: String = "",
    val category: String = "",
    val technicalSpecs: String = "",
    val color: String = "",
    val dimensions: String = "",
    val block: String = "A",
    val shelf: String = "1",
    val locker: String = "1",
    val units: String = "1",
    val observations: String = "",
    val errores: Map<CampoActivo, String> = emptyMap(),
    val isValid: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val isSavedSuccess: Boolean = false,
    val errorGeneral: String? = null
)

