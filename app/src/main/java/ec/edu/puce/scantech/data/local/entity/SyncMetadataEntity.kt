package ec.edu.puce.scantech.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Valores de control de la sincronización (tabla `sync_metadata`), guardados como clave/valor.
 *
 * Vive en Room y no en SharedPreferences para que el cursor de la bajada se actualice en la
 * misma transacción que aplica los cambios: si algo falla a medias, el cursor no avanza.
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val clave: String,
    val valor: Long
) {
    companion object {
        /**
         * `updatedAt` (reloj del servidor) del cambio más reciente ya aplicado en Room. La
         * siguiente bajada pide solo lo posterior a este valor (Estrategia de Datos §4).
         */
        const val LAST_SYNC_TIMESTAMP = "lastSyncTimestamp"
    }
}
