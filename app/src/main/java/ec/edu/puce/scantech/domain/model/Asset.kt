package ec.edu.puce.scantech.domain.model

import ec.edu.puce.scantech.data.local.SyncStatus
import java.util.UUID

/**
 * Activo del inventario tal como lo ven la UI y los ViewModels.
 *
 * Es inmutable: para cambiar un campo se usa `copy`. No conoce Room ni Retrofit; la
 * conversión desde/hacia `AssetEntity` y `AssetDto` vive en `data.mapper`.
 */
data class Asset(
    val id: String = UUID.randomUUID().toString(),
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
) {
    /** Ubicación legible, p. ej. "Bloque A · Estante 3 · Casillero 2". */
    val location: String
        get() = "Bloque $block · Estante $shelf · Casillero $locker"
}
