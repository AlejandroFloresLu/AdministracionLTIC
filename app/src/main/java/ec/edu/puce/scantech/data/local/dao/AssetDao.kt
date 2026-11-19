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
/* =================== [DEFENSA CASO 30: Trazabilidad del Flujo SSOT de Extremo a Extremo] ===================
 * 💡 RESPUESTA TEÓRICA RÁPIDA:
 * - Single Source of Truth (SSOT - Fuente Única de Verdad):
 *   La base de datos local SQLite (Room) es la ÚNICA fuente de verdad autoritativa para la UI de la app,
 *   NO el backend remoto ni variables temporales en memoria.
 * - Flujo Reactivo Unidireccional:
 *   [1] Room DAO: observeAssets() emite un Flow<List<AssetEntity>> reactivo automático ante cualquier cambio en la tabla.
 *   [2] Repositorio: AssetRepositoryImpl mapea AssetEntity -> Asset (Modelo de Dominio Puro) desacoplando infraestructura.
 *   [3] Caso de Uso: ObtenerActivosUseCase orquesta el Flow exponiéndolo limpiamente al ViewModel.
 *   [4] ViewModel: Convierte el Flow frío en StateFlow caliente mediante '.stateIn(SharingStarted.WhileSubscribed(5000))',
 *       otorgando una ventana de gracia de 5 segundos ante cambios de configuración (rotación) para no reiniciar la consulta.
 *   [5] Compose UI: 'collectAsStateWithLifecycle()' recolecta el estado deteniéndose automáticamente cuando la UI está en STOPPED.
 *   LazyColumn se recompone en <50ms reflejando inserciones o actualizaciones sin requerir recargas manuales (cero polling).
 ============================================================================================================ */

@Dao
abstract class AssetDao {

    // ---------------------------------------------------------------- Lecturas (UI)

    /* =================== [DEFENSA CASO 26: Consulta Reactiva por Bloque con Flow en Room] ===================
    // 🛠️ PASO PARA ACTIVAR EN EL EXAMEN:
    // Descomenta esta consulta reactiva para filtrar por bloque físico:
    // @Query("""
    //     SELECT * FROM assets
    //     WHERE block = :bloque
    //       AND deletedAt IS NULL
    //     ORDER BY name COLLATE NOCASE ASC
    // """)
    // abstract fun observarPorBloque(bloque: String): Flow<List<AssetEntity>>

    // 💡 RESPUESTA TEÓRICA RÁPIDA:
    // - Consultas reactivas en Room: Retornar 'Flow<T>' hace que Room registre un InvalidationTracker sobre la tabla 'assets'.
    // - Cada vez que ocurre un INSERT, UPDATE o DELETE sobre cualquier fila de 'assets', Room reejecuta la query
    //   en un hilo secundario y emite la nueva lista automáticamente al colector.
    // - Las funciones que retornan 'Flow' NO deben ser 'suspend': el Flow es frío y se ejecuta al colectarse.
    ======================================================================================================== */

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
    /* =================== [DEFENSA CASO 28: Baja Lógica (Soft Delete) vs Baja Física (Hard Delete)] ===================
    // 💡 RESPUESTA TEÓRICA RÁPIDA:
    // - Borrado Físico (Hard Delete): 'DELETE FROM assets WHERE id = :id'. Elimina permanentemente la fila de SQLite.
    //   En una arquitectura Offline-First es fatal: si el usuario borra sin conexión y la fila desaparece, la app
    //   no tendría forma de recordar qué registro debe eliminarse en el servidor cuando retorne el internet.
    // - Borrado Lógico (Soft Delete): 'UPDATE assets SET deletedAt = :ts, syncStatus = PENDING_DELETE'.
    //   La fila se conserva en SQLite pero se oculta de la UI con 'WHERE deletedAt IS NULL'. Permite encolar
    //   la orden de baja y sincronizarla con el servidor cuando haya conectividad. Solo tras la confirmación
    //   remota se ejecuta el borrado físico ('purgeConfirmedDeletion').
    ================================================================================================================== */
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
    /* =================== [DEFENSA CASO 27: Atomicidad Transaccional con @Transaction en Room] ===================
    // 💡 RESPUESTA TEÓRICA RÁPIDA:
    // - Principio ACID (Atomicidad): La inserción del activo en 'assets' y el encolado de la operación
    //   en 'sync_queue' deben ocurrir de forma indivisible: o se confirman ambas (COMMIT), o ninguna (ROLLBACK).
    // - Si omitiéramos la anotación '@Transaction', un corte de energía, muerte del proceso o excepción en disco
    //   entre las dos sentencias dejaría un activo creado localmente pero sin orden de subida al servidor,
    //   generando inconsistencia permanente de inventario.
    // - Al estar en una clase abstracta ('abstract class AssetDao'), Room genera la implementación que envuelve
    //   el bloque en 'beginTransaction()' / 'setTransactionSuccessful()' / 'endTransaction()'.
    ============================================================================================================= */
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
        /* =================== [DEFENSA CASO 29: Escape de Caracteres Comodín en Consultas LIKE] ===================
        // 💡 RESPUESTA TEÓRICA RÁPIDA:
        // - En SQL LIKE, '%' representa cero o más caracteres arbitrarios y '_' representa exactamente un carácter.
        // - Si un usuario busca '100%' o 'LTIC_01', sin escapar, '%' y '_' se interpretarían como comodines SQL,
        //   devolviendo resultados erróneos o provocando un escaneo completo de la tabla innecesario.
        // - 'escaparLike()' antepone la barra invertida '\\' a '%', '_' y '\\', y la consulta SQL declara:
        //   "name LIKE '%' || :query || '%' ESCAPE '\'", garantizando que el carácter se busque como literal exacto.
        // - Inyección SQL: Room previene la inyección SQL clásica al parametrizar los valores con '?', pero la lógica
        //   de comodines dentro del patrón LIKE requiere este saneamiento explícito a nivel de aplicación.
        ========================================================================================================== */
        /** Escapa `%`, `_` y `\` para que el texto del usuario se busque literalmente en LIKE. */
        fun escaparLike(texto: String): String =
            texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}
