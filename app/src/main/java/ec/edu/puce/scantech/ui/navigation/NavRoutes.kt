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
     */
    @Serializable
    data class AssetDetailRoute(
        val assetId: String
        /* =================== [DEFENSA CASO 6: Parámetro modoAuditoria en Ruta Tipada] ===================
        // 🛠️ PASO PARA ACTIVAR EN EL EXAMEN:
        // Agrega una coma arriba y descomenta este parámetro en la data class:
        // , val modoAuditoria: Boolean = false

        // 💡 RESPUESTA TEÓRICA RÁPIDA:
        // - Rutas Tipadas con @Serializable de KotlinX Serialization (Compose Navigation 2.8+):
        //   Reemplazan los strings mágicos propensos a errores en tiempo de ejecución ("asset/{id}?audit={audit}").
        // - El compilador valida los tipos en tiempo de compilación. Los parámetros con valores por defecto
        //   se tratan automáticamente como argumentos opcionales en el grafo.
        ================================================================================================ */
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

    /* =================== [DEFENSA CASO 8: Registro de HistorialBajasRoute en Rutas] ===================
    // 🛠️ PASO PARA ACTIVAR EN EL EXAMEN:
    // Descomenta este data object de ruta tipada:
    // @Serializable
    // data object HistorialBajasRoute : NavRoute

    // 💡 RESPUESTA TEÓRICA RÁPIDA:
    // - 'data object' para rutas sin argumentos: genera un singleton inmutable serializable automáticamente.
    // - Frente a un 'object' simple, 'data object' genera 'toString()' limpio (útil para depuración y logs de navegación).
    // - Si la ruta requiriera parámetros, se usaría 'data class' con sus propiedades tipadas.
    ==================================================================================================== */
}
