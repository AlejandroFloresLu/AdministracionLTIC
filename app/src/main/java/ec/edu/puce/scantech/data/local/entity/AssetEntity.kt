package ec.edu.puce.scantech.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ec.edu.puce.scantech.data.local.SyncStatus
import java.util.UUID

/**
 * Activo del inventario LTIC (tabla `assets`).
 *
 * - [id] es un UUID generado en el dispositivo, así se puede crear un activo sin conexión
 *   sin riesgo de colisión con los ids de otros dispositivos.
 * - [qrCode] es único: un código QR identifica un solo activo.
 * - [deletedAt] != null indica borrado lógico; el registro se conserva hasta que el servidor
 *   confirme la baja y las consultas de la UI lo excluyen.
 * - [syncStatus] y [updatedAt] alimentan la sincronización y la política de conflictos.
 */
@Entity(
    tableName = "assets",
    indices = [
        Index(value = ["qrCode"], unique = true),
        Index(value = ["category"]),
        Index(value = ["name"])
    ]
)
data class AssetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val qrCode: String,
    val name: String,
    val category: String,
    val technicalSpecs: String,
    val color: String? = null,
    val dimensions: String? = null,
    val block: String,
    val shelf: String,
    val locker: String,
    val units: Int,
    val observations: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
