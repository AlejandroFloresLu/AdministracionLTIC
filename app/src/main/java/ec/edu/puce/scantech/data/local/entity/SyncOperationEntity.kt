package ec.edu.puce.scantech.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ec.edu.puce.scantech.data.local.SyncStatus

/**
 * Operación pendiente de enviar al servidor (tabla `sync_queue`).
 *
 * Se procesan en orden de [createdAt]. [tipo] indica si la operación es un alta, una
 * modificación o una baja pendiente; [payload] guarda el JSON del activo a enviar.
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["assetId"]),
        Index(value = ["createdAt"])
    ]
)
data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val assetId: String,
    val tipo: SyncStatus,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val intentos: Int = 0
)
