package ec.edu.puce.scantech.data.local

/**
 * Representa el estado de sincronización de un activo tecnológico en ScanTech LTIC.
 * Esencial para la arquitectura Offline-First: permite operar sin conexión en sótanos/bodegas
 * y sincronizar cambios en lote con la API REST una vez que se recupera la conectividad.
 */
enum class SyncStatus {
    /**
     * El activo está completamente sincronizado con el servidor central LTIC.
     */
    SYNCED,

    /**
     * El activo fue registrado localmente offline y está pendiente de enviarse vía POST a la API REST.
     */
    PENDING_CREATE,

    /**
     * Los datos u observaciones del activo fueron editados localmente offline y están pendientes de subirse vía PUT/PATCH.
     */
    PENDING_UPDATE,

    /**
     * El activo fue marcado para baja/eliminación en la app y está pendiente de removerse del servidor central.
     */
    PENDING_DELETE
}
