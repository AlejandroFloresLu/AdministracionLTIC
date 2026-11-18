package ec.edu.puce.scantech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import ec.edu.puce.scantech.data.local.SyncStatus
import ec.edu.puce.scantech.data.local.entity.AssetEntity
import ec.edu.puce.scantech.data.local.entity.SyncOperationEntity
import ec.edu.puce.scantech.domain.sync.ActivoSincronizable
import ec.edu.puce.scantech.domain.sync.PoliticaConflictos
import ec.edu.puce.scantech.domain.sync.ResolucionConflicto
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a la tabla `assets`.
 *
 * Es una clase abstracta (y no una interfaz) para poder declarar métodos `@Transaction`
 * que modifican `assets` y `sync_queue` de forma atómica: o se guardan el activo y su
 * operación pendiente, o no se guarda ninguno.
 *
 * Todas las consultas que alimentan la UI excluyen los registros con `deletedAt`.
 */
@Dao
abstract class AssetDao {

    // ---------------------------------------------------------------- Lecturas (UI)

    /**
     * Catálogo filtrado. [query] busca (LIKE) en nombre, código QR y especificaciones;
     * vacío = sin filtro de texto. [category] nulo = todas las categorías.
     * El escape de `%`, `_` y `\` en [query] lo hace quien llama (ver [escaparLike]).
     */
    @Query(
        """
        SELECT * FROM assets
        WHERE deletedAt IS NULL
          AND (:category IS NULL OR category = :category COLLATE NOCASE)
          AND (:query = ''
               OR name LIKE '%' || :query || '%' ESCAPE '\'
               OR qrCode LIKE '%' || :query || '%' ESCAPE '\'
               OR technicalSpecs LIKE '%' || :query || '%' ESCAPE '\')
        ORDER BY name COLLATE NOCASE ASC, id ASC
        """
    )
    abstract fun observeAssets(query: String, category: String?): Flow<List<AssetEntity>>

    /** Observa un activo no borrado; emite `null` si no existe o fue dado de baja. */
    @Query("SELECT * FROM assets WHERE id = :id AND deletedAt IS NULL")
    abstract fun observeById(id: String): Flow<AssetEntity?>

    /** Busca un activo no borrado por su código QR (resultado del escaneo). */
    @Query("SELECT * FROM assets WHERE qrCode = :qrCode AND deletedAt IS NULL LIMIT 1")
    abstract suspend fun getByQrCode(qrCode: String): AssetEntity?

    // ------------------------------------------------------- Lecturas (internas / sync)

    /** Busca por id incluyendo borrados lógicos (uso interno del repositorio). */
    @Query("SELECT * FROM assets WHERE id = :id")
    abstract suspend fun getById(id: String): AssetEntity?

    /**
     * Indica si el QR ya está tomado. Cuenta también los borrados lógicos, porque siguen
     * ocupando el índice único hasta que el servidor confirme la baja.
     * [excluirId] permite ignorar el propio activo al editarlo.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM assets
            WHERE qrCode = :qrCode AND (:excluirId IS NULL OR id != :excluirId)
        )
        """
    )
    abstract suspend fun existsQrCode(qrCode: String, excluirId: String?): Boolean

    /** Marca de tiempo más reciente entre los activos ya sincronizados (para el pull incremental). */
    @Query("SELECT COALESCE(MAX(updatedAt), 0) FROM assets WHERE syncStatus = 'SYNCED'")
    abstract suspend fun lastSyncedUpdatedAt(): Long

    // ------------------------------------------------------------------ Escrituras

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(asset: AssetEntity)

    @Update
    abstract suspend fun update(asset: AssetEntity): Int

    /** Borrado lógico: conserva la fila para poder avisar la baja al servidor. */
    @Query(
        """
        UPDATE assets
        SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'PENDING_DELETE'
        WHERE id = :id
        """
    )
    abstract suspend fun markDeleted(id: String, deletedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(asset: AssetEntity)

    /**
     * Cambia solo las observaciones de un activo no borrado, refresca [updatedAt] y aplica la
     * regla de syncStatus: PENDING_CREATE se mantiene, cualquier otro pasa a PENDING_UPDATE.
     * Usar desde [updateObservationsWithQueue] para que la operación quede encolada.
     */
    @Query(
        """
        UPDATE assets
        SET observations = :observations,
            updatedAt = :updatedAt,
            syncStatus = CASE WHEN syncStatus = 'PENDING_CREATE' THEN 'PENDING_CREATE'
                              ELSE 'PENDING_UPDATE' END
        WHERE id = :id AND deletedAt IS NULL
        """
    )
    abstract suspend fun setObservations(id: String, observations: String?, updatedAt: Long): Int

    // ------------------------------------------------------ sync_queue (uso transaccional)

    @Insert
    abstract suspend fun enqueue(operation: SyncOperationEntity): Long

    @Query("DELETE FROM sync_queue WHERE assetId = :assetId")
    abstract suspend fun clearQueueOf(assetId: String)

    @Query("DELETE FROM sync_queue WHERE id IN (:ids)")
    abstract suspend fun deleteOperations(ids: List<Long>)

    /** Pasa a SYNCED solo si no quedó otra operación pendiente (p. ej. una edición posterior). */
    @Query(
        """
        UPDATE assets SET syncStatus = 'SYNCED'
        WHERE id = :id AND NOT EXISTS (SELECT 1 FROM sync_queue WHERE assetId = :id)
        """
    )
    abstract suspend fun markSyncedIfIdle(id: String): Int

    /** Elimina la fila de una baja confirmada si no hay operaciones nuevas sobre ella. */
    @Query(
        """
        DELETE FROM assets
        WHERE id = :id AND syncStatus = 'PENDING_DELETE'
          AND NOT EXISTS (SELECT 1 FROM sync_queue WHERE assetId = :id)
        """
    )
    abstract suspend fun purgeConfirmedDeletion(id: String): Int

    // ------------------------------------------------------------ Transacciones

    /** Guarda un activo nuevo y encola su alta, todo en una sola transacción. */
    @Transaction
    open suspend fun insertWithQueue(asset: AssetEntity, payload: String) {
        insert(asset.copy(syncStatus = SyncStatus.PENDING_CREATE))
        enqueue(SyncOperationEntity(assetId = asset.id, tipo = SyncStatus.PENDING_CREATE, payload = payload, createdAt = asset.updatedAt))
    }

    /**
     * Guarda los cambios de un activo y encola una operación de actualización; las ediciones
     * hechas sin conexión viajan en orden, no solo el último estado. Si el alta aún no se
     * subió, el activo conserva PENDING_CREATE (el servidor todavía no lo conoce).
     * @return `false` si el activo no existe o ya está dado de baja.
     */
    @Transaction
    open suspend fun updateWithQueue(asset: AssetEntity, payload: String): Boolean {
        val previo = getById(asset.id)
        if (previo == null || previo.deletedAt != null) return false

        val estado = if (previo.syncStatus == SyncStatus.PENDING_CREATE) {
            SyncStatus.PENDING_CREATE
        } else {
            SyncStatus.PENDING_UPDATE
        }
        update(asset.copy(syncStatus = estado, deletedAt = null))
        enqueue(SyncOperationEntity(assetId = asset.id, tipo = SyncStatus.PENDING_UPDATE, payload = payload, createdAt = asset.updatedAt))
        return true
    }

    /**
     * Guarda observaciones (Estrategia de Datos §3.b) en una sola transacción: actualiza el
     * registro, refresca `updatedAt`, aplica la regla de syncStatus y encola un PENDING_UPDATE.
     * A diferencia de [updateWithQueue], no reescribe el resto de campos, así que no pisa
     * cambios hechos por otra pantalla o por la sincronización.
     *
     * @param payload arma el JSON a encolar a partir del registro ya actualizado.
     * @return el activo actualizado, o `null` si no existe o está dado de baja (no se encola nada).
     */
    @Transaction
    open suspend fun updateObservationsWithQueue(
        id: String,
        observations: String?,
        updatedAt: Long,
        payload: (AssetEntity) -> String
    ): AssetEntity? {
        if (setObservations(id, observations, updatedAt) == 0) return null
        val actualizado = getById(id) ?: return null
        enqueue(SyncOperationEntity(assetId = id, tipo = SyncStatus.PENDING_UPDATE, payload = payload(actualizado), createdAt = updatedAt))
        return actualizado
    }

    /**
     * Borrado lógico: oculta el activo, lo marca PENDING_DELETE y encola la baja. La fila solo
     * se elimina físicamente cuando el servidor confirma (ver [confirmOperations]).
     * @return `false` si el activo no existe o ya estaba dado de baja.
     */
    @Transaction
    open suspend fun deleteWithQueue(id: String, deletedAt: Long, payload: String): Boolean {
        val previo = getById(id)
        if (previo == null || previo.deletedAt != null) return false

        markDeleted(id, deletedAt)
        enqueue(SyncOperationEntity(assetId = id, tipo = SyncStatus.PENDING_DELETE, payload = payload, createdAt = deletedAt))
        return true
    }

    /** Aplica la confirmación del servidor: saca las operaciones de la cola y cierra cada activo. */
    @Transaction
    open suspend fun confirmOperations(confirmadas: List<SyncOperationEntity>) {
        if (confirmadas.isEmpty()) return
        deleteOperations(confirmadas.map { it.id })
        confirmadas.forEach { op ->
            if (op.tipo == SyncStatus.PENDING_DELETE) purgeConfirmedDeletion(op.assetId) else markSyncedIfIdle(op.assetId)
        }
    }

    /**
     * Incorpora los cambios descargados del servidor aplicando [PoliticaConflictos].
     * @return ids de los activos que requieren revisión manual (se conserva la copia local).
     */
    @Transaction
    open suspend fun applyRemoteChanges(remotos: List<AssetEntity>): List<String> {
        val paraRevisar = mutableListOf<String>()
        for (remoto in remotos) {
            val local = getById(remoto.id)
            when {
                local == null -> {
                    // Un QR repetido en otro id no se puede insertar sin romper el índice único.
                    if (existsQrCode(remoto.qrCode, remoto.id)) paraRevisar += remoto.id else upsert(remoto)
                }
                local.syncStatus == SyncStatus.SYNCED -> if (!existsQrCode(remoto.qrCode, remoto.id)) upsert(remoto) else paraRevisar += remoto.id
                else -> when (PoliticaConflictos.resolver(local.paraSync(), remoto.paraSync(), unitsVisto = null)) {
                    is ResolucionConflicto.GanaServidor -> {
                        clearQueueOf(local.id)
                        upsert(remoto)
                    }
                    is ResolucionConflicto.GanaLocal -> Unit
                    is ResolucionConflicto.RevisionManual -> paraRevisar += remoto.id
                }
            }
        }
        return paraRevisar
    }

    private fun AssetEntity.paraSync() = ActivoSincronizable(id = id, updatedAt = updatedAt, units = units)

    companion object {
        /** Escapa `%`, `_` y `\` para que el texto del usuario se busque literalmente en LIKE. */
        fun escaparLike(texto: String): String =
            texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}
