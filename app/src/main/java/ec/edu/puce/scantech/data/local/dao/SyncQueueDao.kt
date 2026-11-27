package ec.edu.puce.scantech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ec.edu.puce.scantech.data.local.entity.SyncOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    /** Encola una operación de sincronización y devuelve su id. */
    @Insert
    suspend fun insertar(operacion: SyncOperationEntity): Long

    /** Lista las operaciones pendientes en el orden en que se crearon. */
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC, id ASC")
    suspend fun listarPendientes(): List<SyncOperationEntity>

    /**
     * Operaciones que se pueden subir, en orden: excluye las que están en revisión y todas las
     * de un activo que tenga alguna en revisión, para no enviar sus cambios fuera de orden.
     */
    @Query(
        """
        SELECT * FROM sync_queue
        WHERE assetId NOT IN (SELECT assetId FROM sync_queue WHERE enRevision = 1)
        ORDER BY createdAt ASC, id ASC
        """
    )
    suspend fun listarParaEnviar(): List<SyncOperationEntity>

    /** Marca operaciones rechazadas por el servidor (4xx): no se reintentan hasta revisarlas. */
    @Query("UPDATE sync_queue SET enRevision = 1, motivoRevision = :motivo, intentos = intentos + 1 WHERE id IN (:ids)")
    suspend fun marcarEnRevision(ids: List<Long>, motivo: String)

    /** Observa cuántas operaciones esperan revisión manual. */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE enRevision = 1")
    fun observarEnRevision(): Flow<Int>

    /** Borra las operaciones que el servidor confirmó. */
    @Query("DELETE FROM sync_queue WHERE id IN (:ids)")
    suspend fun borrarConfirmadas(ids: List<Long>)

    /** Suma un intento fallido a una operación. */
    @Query("UPDATE sync_queue SET intentos = intentos + 1 WHERE id = :id")
    suspend fun sumarIntento(id: Long)

    /** Observa cuántas operaciones siguen pendientes de subir (indicador de sincronización). */
    @Query("SELECT COUNT(*) FROM sync_queue")
    fun observarPendientes(): Flow<Int>

    /** Observa cuántas operaciones pendientes tiene un activo concreto. */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE assetId = :assetId")
    fun observarPendientesDeActivo(assetId: String): Flow<Int>
}
