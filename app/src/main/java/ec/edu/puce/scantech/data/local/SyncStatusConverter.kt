package ec.edu.puce.scantech.data.local

import androidx.room.TypeConverter

/**
 * TypeConverter para Room Database que serializa y deserializa el enum [SyncStatus].
 * Aplica las mejores prácticas de Null Safety de Kotlin (cero uso de !!, uso de Elvis ?:
 * y fallback ante entradas nulas o valores corruptos/desconocidos en la base de datos).
 */
class SyncStatusConverter {

    /**
     * Convierte el enum [SyncStatus] a su representación en [String] para persistirlo en SQLite.
     */
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus?): String? {
        return status?.name
    }

    /**
     * Convierte el [String] almacenado en SQLite de vuelta al enum [SyncStatus].
     * Si el valor es nulo o no coincide con ninguna constante del enum, retorna [SyncStatus.SYNCED] por seguridad.
     */
    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus {
        if (value.isNullOrBlank()) {
            return SyncStatus.SYNCED
        }

        return try {
            SyncStatus.valueOf(value.trim().uppercase())
        } catch (_: IllegalArgumentException) {
            // Manejo seguro ante cambios de esquema o valores desconocidos en la BD
            SyncStatus.SYNCED
        }
    }
}
