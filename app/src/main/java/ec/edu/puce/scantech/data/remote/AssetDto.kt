package ec.edu.puce.scantech.data.remote

import kotlinx.serialization.Serializable

/** Representación de un activo en la API REST (JSON). */
@Serializable
data class AssetDto(
    val id: String,
    val qrCode: String,
    val name: String,
    val category: String,
    val technicalSpecs: String = "",
    val color: String? = null,
    val dimensions: String? = null,
    val block: String? = null,
    val shelf: String? = null,
    val locker: String? = null,
    val units: Int = 0,
    val observations: String? = null,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)

/** Operación individual dentro de un lote de sincronización. */
@Serializable
data class SyncOperationDto(
    val opId: String,
    val tipo: String,
    val asset: AssetDto
)

/** Lote de operaciones enviado a `POST /assets/sync`. */
@Serializable
data class SyncBatchRequest(
    val operaciones: List<SyncOperationDto>
)

/** Resultado por registro devuelto por el servidor. */
@Serializable
data class SyncResultDto(
    val opId: String,
    val aceptado: Boolean,
    val motivo: String? = null
)

/** Respuesta de `POST /assets/sync`: un resultado por cada operación enviada. */
@Serializable
data class SyncBatchResponse(
    val resultados: List<SyncResultDto>
)

/** Categoría controlada devuelta por `GET /categories`. */
@Serializable
data class CategoryDto(
    val clave: String,
    val nombre: String
)
