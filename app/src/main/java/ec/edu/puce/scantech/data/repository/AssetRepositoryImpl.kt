package ec.edu.puce.scantech.data.repository

import android.database.sqlite.SQLiteConstraintException
import ec.edu.puce.scantech.data.local.SyncStatus
import ec.edu.puce.scantech.data.local.dao.AssetDao
import ec.edu.puce.scantech.data.local.dao.SyncQueueDao
import ec.edu.puce.scantech.data.local.entity.SyncOperationEntity
import ec.edu.puce.scantech.data.mapper.toDomain
import ec.edu.puce.scantech.data.mapper.toDto
import ec.edu.puce.scantech.data.mapper.toEntity
import ec.edu.puce.scantech.data.remote.AssetApi
import ec.edu.puce.scantech.data.remote.AssetDto
import ec.edu.puce.scantech.data.remote.ErroresHttp
import ec.edu.puce.scantech.data.remote.SyncBatchRequest
import ec.edu.puce.scantech.data.remote.SyncOperationDto
import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.repository.AssetRepository
import ec.edu.puce.scantech.domain.repository.RechazoSync
import ec.edu.puce.scantech.domain.repository.SyncSummary
import ec.edu.puce.scantech.domain.sync.ProgramadorSync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * Implementación de [AssetRepository] sobre Room + Retrofit.
 *
 * Las escrituras locales usan los métodos `@Transaction` de [AssetDao], de modo que `assets`
 * y `sync_queue` nunca quedan desalineados. Las funciones `suspend` de Room y Retrofit ya
 * mueven el trabajo fuera del hilo principal, por lo que no hace falta cambiar de dispatcher.
 *
 * @param clock reloj inyectable para poder fijar `updatedAt` en las pruebas.
 * @param programador disparador de la sincronización tras cada escritura local.
 */
class AssetRepositoryImpl(
    private val assetDao: AssetDao,
    private val syncQueueDao: SyncQueueDao,
    private val api: AssetApi,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val clock: () -> Long = System::currentTimeMillis,
    private val programador: ProgramadorSync = ProgramadorSync.Nulo
) : AssetRepository {

    private val candadoSync = Mutex()

    // ------------------------------------------------------------------ Lecturas

    override fun observeAssets(query: String, category: String?): Flow<List<Asset>> =
        assetDao.observeAssets(
            query = AssetDao.escaparLike(query.trim()),
            category = category?.takeIf { it.isNotBlank() }
        ).map { it.toDomain() }

    override fun observeById(id: String): Flow<Asset?> =
        assetDao.observeById(id).map { it?.toDomain() }

    override fun observePendingCount(): Flow<Int> = syncQueueDao.observarPendientes()

    override suspend fun getByQrCode(qrCode: String): Result<Asset> = local {
        val activo = assetDao.getByQrCode(qrCode.trim())
        if (activo == null) {
            Result.Error("No hay un activo registrado con el código $qrCode")
        } else {
            Result.Success(activo.toDomain())
        }
    }

    override suspend fun existsQrCode(qrCode: String, excludeId: String?): Result<Boolean> = local {
        Result.Success(assetDao.existsQrCode(qrCode.trim(), excludeId))
    }

    // ----------------------------------------------------------------- Escrituras

    override suspend fun create(asset: Asset): Result<Asset> = escrituraLocal {
        val entidad = asset.toEntity().copy(
            qrCode = asset.qrCode.trim(),
            syncStatus = SyncStatus.PENDING_CREATE,
            updatedAt = clock(),
            deletedAt = null
        )
        assetDao.insertWithQueue(entidad, payloadDe(entidad.toDto()))
        Result.Success(entidad.toDomain())
    }

    override suspend fun update(asset: Asset): Result<Asset> = escrituraLocal {
        val entidad = asset.toEntity().copy(qrCode = asset.qrCode.trim(), updatedAt = clock())
        if (!assetDao.updateWithQueue(entidad, payloadDe(entidad.toDto()))) {
            Result.Error("El activo ya no existe")
        } else {
            // Se relee para devolver el syncStatus que decidió la transacción.
            Result.Success((assetDao.getById(entidad.id) ?: entidad).toDomain())
        }
    }

    override suspend fun updateObservations(id: String, observations: String?): Result<Asset> = escrituraLocal {
        val texto = observations?.trim()?.takeIf { it.isNotEmpty() }
        val actualizado = assetDao.updateObservationsWithQueue(id, texto, clock()) { payloadDe(it.toDto()) }
        if (actualizado == null) {
            Result.Error("El activo ya no existe")
        } else {
            Result.Success(actualizado.toDomain())
        }
    }

    override suspend fun delete(id: String): Result<Unit> = escrituraLocal {
        val previo = assetDao.getById(id)
        val ahora = clock()
        val dto = previo?.toDto()?.copy(updatedAt = ahora, deletedAt = ahora)
        if (dto == null || !assetDao.deleteWithQueue(id, ahora, payloadDe(dto))) {
            Result.Error("El activo ya no existe")
        } else {
            Result.Success(Unit)
        }
    }

    // ------------------------------------------------------------- Sincronización

    override suspend fun sync(): Result<SyncSummary> {
        return try {
            // Los tres disparadores (al guardar, periódico y manual) pueden coincidir; el candado
            // evita que dos rondas suban la misma operación a la vez.
            candadoSync.withLock { rondaDeSync() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.Error("Sin conexión con el servidor. Los cambios se enviarán más tarde.", e)
        } catch (e: HttpException) {
            Result.Error("El servidor respondió con error ${e.code()}", e)
        } catch (e: Exception) {
            Result.Error("No se pudo sincronizar: ${e.message ?: "error desconocido"}", e)
        }
    }

    private suspend fun rondaDeSync(): Result<SyncSummary> {
        var enviadas = 0
        val rechazos = mutableListOf<RechazoSync>()
        // Activos con una operación rechazada en esta ronda: sus siguientes operaciones esperan.
        val activosEnRevision = mutableSetOf<String>()

        // Lotes acotados para controlar el payload en conexiones inestables. Cada lote se
        // confirma por separado: si la red se corta, lo ya aceptado queda guardado.
        for (loteCompleto in syncQueueDao.listarParaEnviar().chunked(TAMANO_LOTE)) {
            val lote = loteCompleto.filter { it.assetId !in activosEnRevision }
            if (lote.isEmpty()) continue

            val request = SyncBatchRequest(
                lote.map {
                    SyncOperationDto(
                        opId = it.id.toString(),
                        tipo = it.tipo.name,
                        asset = json.decodeFromString(AssetDto.serializer(), it.payload)
                    )
                }
            )

            val respuesta = try {
                api.sincronizar(request)
            } catch (e: HttpException) {
                // 4xx: el servidor rechazó el lote. No se reintenta: va a revisión y se sigue
                // con el resto de la cola. 5xx y otros se propagan para reintentar con backoff.
                if (!ErroresHttp.esRechazoDefinitivo(e.code())) throw e
                val motivo = "El servidor rechazó el lote (HTTP ${e.code()})"
                syncQueueDao.marcarEnRevision(lote.map { it.id }, motivo)
                lote.forEach { rechazos += RechazoSync(it.assetId, motivo) }
                activosEnRevision += lote.map { it.assetId }
                continue
            }

            val resultados = respuesta.resultados.associateBy { it.opId }
            val ok = mutableListOf<SyncOperationEntity>()
            for (op in lote) {
                val resultado = resultados[op.id.toString()]
                when {
                    resultado?.aceptado == true -> ok += op
                    resultado != null -> {
                        // Rechazo explícito del servidor: equivale a un 4xx, va a revisión.
                        val motivo = resultado.motivo ?: "El servidor rechazó la operación"
                        syncQueueDao.marcarEnRevision(listOf(op.id), motivo)
                        rechazos += RechazoSync(op.assetId, motivo)
                        activosEnRevision += op.assetId
                    }
                    // Sin respuesta para esta operación: no se sabe si llegó, se reintenta.
                    else -> syncQueueDao.sumarIntento(op.id)
                }
            }
            assetDao.confirmOperations(ok)
            enviadas += ok.size
        }

        // Bajada incremental: solo lo posterior al cursor guardado en Room. El DAO aplica
        // los cambios sin pisar pendientes locales y avanza el cursor en la misma transacción.
        val remotos = api.obtenerCambios(assetDao.lastSyncTimestamp())
        val paraRevisar = assetDao.applyRemoteChanges(remotos.map { it.toEntity() })

        return Result.Success(SyncSummary(enviadas, rechazos, remotos.size, paraRevisar))
    }

    // -------------------------------------------------------------------- Helpers

    private companion object {
        const val TAMANO_LOTE = 50
    }

    private fun payloadDe(dto: AssetDto): String = json.encodeToString(AssetDto.serializer(), dto)

    /** Ejecuta una operación sobre Room y traduce las excepciones a [Result.Error]. */
    private inline fun <T> local(block: () -> Result<T>): Result<T> {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteConstraintException) {
            Result.Error("Ya existe un activo con este código QR", e)
        } catch (e: Exception) {
            Result.Error("No se pudo completar la operación: ${e.message ?: "error desconocido"}", e)
        }
    }

    /**
     * Escritura local (alta, edición, observaciones o baja). Si se guardó, dispara la
     * sincronización única "al guardar"; la escritura nunca espera a la red.
     */
    private inline fun <T> escrituraLocal(block: () -> Result<T>): Result<T> =
        local(block).also { if (it is Result.Success) programador.programarTrasGuardar() }
}
