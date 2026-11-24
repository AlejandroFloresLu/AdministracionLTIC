package ec.edu.puce.scantech.ui.inventory

import ec.edu.puce.scantech.domain.model.Asset

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
     * Activos que cumplen el filtro actual. Room ya los devuelve filtrados (texto y categoría),
     * así que la lista se muestra tal cual.
     */
    data class Success(
        val activos: List<Asset>
    ) : InventoryUiState

    /**
     * Estado de error con mensaje descriptivo para feedback accesible.
     */
    /**
     * [Caso de práctica 4] La búsqueda de texto no encontró activos. Es distinto de un catálogo
     * vacío: aquí sí hay datos, pero ninguno coincide con [query].
     */
    data class SinResultados(
        val query: String
    ) : InventoryUiState

    data class Error(
        val mensaje: String
    ) : InventoryUiState
}
