package ec.edu.puce.scantech.ui.inventory

/**
 * [Caso de práctica 7: Estados de red con jerarquía sellada]
 *
 * Estado de la sincronización manual con el servidor. Al ser `sealed`, el `when` de la UI
 * debe cubrir los cuatro casos o el proyecto no compila.
 */
sealed interface SyncUiState {

    /** Sin sincronizaciones en curso ni mensajes pendientes de mostrar. */
    data object Idle : SyncUiState

    /** Esperando la respuesta del servidor: el botón se desactiva para evitar envíos dobles. */
    data object Loading : SyncUiState

    /** El servidor confirmó la operación. */
    data class Success(val mensaje: String) : SyncUiState

    /** Fallo clasificado por safeApiCall (sin conexión, HTTP 4xx/5xx o inesperado). */
    data class Error(val mensaje: String) : SyncUiState
}
