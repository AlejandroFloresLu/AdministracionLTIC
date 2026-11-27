package ec.edu.puce.scantech.ui.navigation

import kotlinx.serialization.Serializable

/**
 * [RDA-2 Temas 7 y 8: Navegación Progresiva y Fuertemente Tipada (Type-Safe Navigation)]
 *
 * Contratos de navegación declarativa para Android 16 (API 36).
 *
 * Conceptos Teóricos Aplicados:
 * 1. Rutas como Clases/Objetos Serializables: Se erradican por completo los strings mágicos ("asset/{id}")
 *    que producían fallos silenciosos en tiempo de ejecución.
 * 2. Validación Estricta en Tiempo de Compilación: Los parámetros de navegación son chequeados por el compilador
 *    de Kotlin y el plugin `kotlinx.serialization`.
 * 3. Deserialización Automática: La extracción de argumentos se realiza mediante `toRoute<T>()` sin boilerplate.
 */
sealed interface NavRoute {

    /**
     * Pantalla principal del Catálogo de Inventario LTIC con búsqueda y filtros rápidos.
     * @param auditHash [Caso 1] Hash de sesión para auditoría de inmutabilidad. Es obligatorio
     *   y no anulable: toda navegación al catálogo debe enviarlo o el proyecto no compila.
     */
    @Serializable
    data class DashboardRoute(
        val auditHash: String
    ) : NavRoute

    /**
     * Pantalla de Escáner Óptico de Códigos QR mediante Edge AI.
     */
    @Serializable
    data object ScannerRoute : NavRoute

    /**
     * Pantalla de Ficha Técnica del Activo con especificaciones completas.
     * @param assetId Identificador único o QR del activo.
     */
    @Serializable
    data class AssetDetailRoute(
        val assetId: String
    ) : NavRoute

    /**
     * Módulo de Formulario Ultra-Resiliente (Enunciado §3 y Tarea #55).
     * @param assetId `null` para modo Alta / Registro; ID existente para modo Edición.
     * @param initialQr Código QR prellenado opcional al dar de alta desde el escáner óptico (Tarea #5).
     */
    @Serializable
    data class AssetFormRoute(
        val assetId: String? = null,
        val initialQr: String? = null
    ) : NavRoute

    /**
     * Pantalla explicativa de Solicitud de Permiso de Cámara.
     */
    @Serializable
    data object CameraPermissionRoute : NavRoute

}
