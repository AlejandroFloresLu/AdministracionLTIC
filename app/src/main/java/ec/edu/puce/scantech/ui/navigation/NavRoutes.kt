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
     */
    @Serializable
    data object DashboardRoute : NavRoute

    /**
     * Pantalla de Escáner Óptico de Códigos QR mediante Edge AI.
     */
    @Serializable
    data object ScannerRoute : NavRoute

    /**
     * Pantalla de Ficha Técnica del Activo con especificaciones completas.
     * @param assetId Identificador único o QR del activo.
     * @param origen [Caso de práctica 3] Pantalla desde la que se abrió la ficha (auditoría).
     *   Es obligatorio y no anulable: si alguna llamada no lo envía, el proyecto no compila.
     */
    @Serializable
    data class AssetDetailRoute(
        val assetId: String,
        val origen: String
    ) : NavRoute

    /**
     * Módulo de Formulario Ultra-Resiliente (Enunciado §3 y Tarea #55).
     * @param assetId `null` para modo Alta / Registro; ID existente para modo Edición.
     */
    @Serializable
    data class AssetFormRoute(
        val assetId: String? = null
    ) : NavRoute

    /**
     * Pantalla explicativa de Solicitud de Permiso de Cámara.
     */
    @Serializable
    data object CameraPermissionRoute : NavRoute
}
