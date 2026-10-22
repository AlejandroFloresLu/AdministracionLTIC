package ec.edu.puce.scantech.ui.inventory

import ec.edu.puce.scantech.data.model.ActivoLTIC

/**
 * Máquina de estados finita (FSM) que modela el estado reactivo del Inventario en ScanTech LTIC.
 * Implementada como sealed interface según las normativas de Arquitectura Limpia y UDF
 * (Unidirectional Data Flow) para permitir una evaluación exhaustiva con `when` en Compose
 * y blindar la aplicación contra nulos e inconsistencias visuales.
 */
sealed interface InventoryUiState {

    /**
     * Estado inicial antes de disparar la carga de datos.
     */
    data object Idle : InventoryUiState

    /**
     * Estado de carga asíncrona mientras se consultan los activos en Room/Red.
     */
    data object Loading : InventoryUiState

    /**
     * Estado exitoso con la lista inmutable de activos recuperados y filtrados.
     */
    data class Success(
        val activos: List<ActivoLTIC>,
        val filteredActivos: List<ActivoLTIC> = activos
    ) : InventoryUiState

    /**
     * Estado de error con mensaje descriptivo para feedback accesible.
     */
    data class Error(
        val mensaje: String
    ) : InventoryUiState
}
