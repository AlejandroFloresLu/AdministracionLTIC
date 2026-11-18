package ec.edu.puce.scantech.domain.model

/**
 * [RDA-3 Temas 9 y 10: Clean Architecture - Domain Layer (Reglas de Negocio Puras)]
 * [RDA-3 Temas 11 y 12: Arquitectura de Persistencia Local y Diseño Offline-First]
 *
 * Representa el estado de sincronización de un activo tecnológico en ScanTech LTIC.
 *
 * Conceptos Teóricos Aplicados:
 * 1. Dominio Puro (Core): Este enum reside en la capa central del sistema (`domain/model`),
 *    totalmente desvinculado de frameworks, bibliotecas de Android, Room o Retrofit.
 * 2. Estrategia Offline-First: En entornos hostiles como bodegas o sótanos sin conectividad WiFi,
 *    la base de datos local actúa como fuente única de verdad (SSOT). Este enum modela el ciclo
 *    de vida de las mutaciones locales pendientes de conciliar con la API REST remota.
 */
enum class SyncStatus {
    /**
     * El activo está completamente sincronizado y coincide de forma idéntica con el servidor central LTIC.
     */
    SYNCED,

    /**
     * El activo fue registrado localmente offline; está encolado para enviarse vía POST a la API REST.
     */
    PENDING_CREATE,

    /**
     * Los datos o especificaciones fueron editados localmente; pendiente de sincronizarse vía PUT/PATCH.
     */
    PENDING_UPDATE,

    /**
     * El activo fue marcado para baja/eliminación lógica; pendiente de confirmarse con DELETE en el servidor.
     */
    PENDING_DELETE
}

