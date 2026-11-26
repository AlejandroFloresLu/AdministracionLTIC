package ec.edu.puce.scantech.ui.inventory

import androidx.compose.runtime.Immutable
import ec.edu.puce.scantech.domain.model.Asset

/**
 * Máquina de estados finita (FSM) que modela el estado reactivo del Inventario en ScanTech LTIC.
 * Implementada como sealed interface según las normativas de Arquitectura Limpia y UDF
 * (Unidirectional Data Flow) para permitir una evaluación exhaustiva con `when` en Compose
 * y blindar la aplicación contra nulos e inconsistencias visuales.
 */
sealed interface InventoryUiState {

    /**
     * Estado de carga mientras Room entrega la primera lista.
     */
    data object Loading : InventoryUiState

    /**
     * Activos que cumplen el filtro actual. Room ya los devuelve filtrados (texto y categoría),
     * así que la lista se muestra tal cual.
     *
     * `@Immutable`: la lista nunca se modifica después de crear el estado (cada cambio en la
     * base trae una lista nueva), así Compose puede saltarse la recomposición cuando el estado
     * no cambió. Sin la anotación, `List` se consideraría inestable.
     *
     * @param hayFiltros `true` si hay texto de búsqueda o una categoría elegida; permite
     * distinguir "no hay resultados para el filtro" de "el inventario está vacío".
     */
    @Immutable
    data class Success(
        val activos: List<Asset>,
        val hayFiltros: Boolean = false
    ) : InventoryUiState

    /**
     * Estado de error con mensaje descriptivo para feedback accesible.
     */
    data class Error(
        val mensaje: String
    ) : InventoryUiState
}
