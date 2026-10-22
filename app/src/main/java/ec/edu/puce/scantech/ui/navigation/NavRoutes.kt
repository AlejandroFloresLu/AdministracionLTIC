package ec.edu.puce.scantech.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Contratos de Navegación Fuertemente Tipada (Type-Safe Navigation Contracts).
 *
 * Exigido en API 36 y en los Temas 7 y 8 de la cátedra:
 * - Elimina por completo las cadenas de texto mágicas ("profile/{id}").
 * - Valida tipos en tiempo de compilación.
 * - Deserialización automática con `toRoute<T>()`.
 */
sealed interface NavRoute {

    /**
     * Pantalla principal del Catálogo de Inventario LTIC con búsqueda y filtros rápidos.
     */
    @Serializable
    data object DashboardRoute : NavRoute

    /**
     * Pantalla de Escáner Óptico de Códigos QR/Barras mediante Edge AI (CameraX + ML Kit).
     */
    @Serializable
    data object ScannerRoute : NavRoute

    /**
     * Pantalla de Ficha Técnica y Registro de Observaciones Físicas de un Activo específico.
     * @param assetId Identificador único del activo (ej. "LTIC-2048").
     */
    @Serializable
    data class AssetDetailRoute(
        val assetId: String
    ) : NavRoute

    /**
     * Pantalla explicativa de Solicitud de Permiso de Cámara.
     */
    @Serializable
    data object CameraPermissionRoute : NavRoute
}
