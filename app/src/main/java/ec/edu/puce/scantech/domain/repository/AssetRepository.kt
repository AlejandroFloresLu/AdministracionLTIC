package ec.edu.puce.scantech.domain.repository

import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.model.Asset
import kotlinx.coroutines.flow.Flow

/** Operación que el servidor no aceptó; se conserva en la cola para revisarla o reintentarla. */
data class RechazoSync(val assetId: String, val motivo: String)

/** Resumen de una ronda de sincronización. */
data class SyncSummary(
    /** Operaciones locales que el servidor aceptó. */
    val sent: Int,
    /**
     * Operaciones que el servidor rechazó (4xx o `aceptado = false`), con su motivo. Siguen en
     * la cola marcadas para revisión y no se reintentan.
     */
    val rejected: List<RechazoSync>,
    /** Activos descargados del servidor. */
    val received: Int,
    /** Ids de activos en conflicto que requieren revisión manual (se conservó la copia local). */
    val needsReview: List<String>
)

/**
 * Único punto de acceso a los datos de activos: une Room (fuente de verdad) y la API REST.
 * Los ViewModels dependen solo de esta interfaz.
 *
 * Offline-first: crear, editar y eliminar escriben en Room y encolan la operación; nunca
 * esperan a la red. [sync] sube la cola y descarga los cambios del servidor.
 */
interface AssetRepository {

    /** Catálogo sin borrados. [query] vacío = sin filtro de texto; [category] nulo = todas. */
    fun observeAssets(query: String = "", category: String? = null): Flow<List<Asset>>

    /** Emite el activo o `null` si no existe o fue dado de baja. */
    fun observeById(id: String): Flow<Asset?>

    /** Cantidad de operaciones esperando subirse al servidor. */
    fun observePendingCount(): Flow<Int>

    /** Busca un activo por el QR escaneado. */
    suspend fun getByQrCode(qrCode: String): Result<Asset>

    /** Indica si el QR ya está en uso; [excludeId] ignora al propio activo al editar. */
    suspend fun existsQrCode(qrCode: String, excludeId: String? = null): Result<Boolean>

    suspend fun create(asset: Asset): Result<Asset>

    suspend fun update(asset: Asset): Result<Asset>

    /**
     * Guarda solo las observaciones del activo y encola la actualización. Texto en blanco las
     * borra (`null`). El activo pasa a PENDING_UPDATE salvo que siga como PENDING_CREATE.
     */
    suspend fun updateObservations(id: String, observations: String?): Result<Asset>

    suspend fun delete(id: String): Result<Unit>

    /** Sube la cola de pendientes y descarga cambios del servidor. */
    suspend fun sync(): Result<SyncSummary>
}
