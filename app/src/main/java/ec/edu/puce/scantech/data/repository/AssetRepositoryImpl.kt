package ec.edu.puce.scantech.data.repository

import android.database.sqlite.SQLiteConstraintException
import ec.edu.puce.scantech.data.local.SyncStatus
import ec.edu.puce.scantech.data.local.dao.AssetDao
import ec.edu.puce.scantech.data.local.dao.SyncQueueDao
import ec.edu.puce.scantech.data.mapper.toDomain
import ec.edu.puce.scantech.data.mapper.toDto
import ec.edu.puce.scantech.data.mapper.toEntity
import ec.edu.puce.scantech.data.remote.AssetApi
import ec.edu.puce.scantech.data.remote.AssetDto
import ec.edu.puce.scantech.data.remote.SyncBatchRequest
import ec.edu.puce.scantech.data.remote.SyncOperationDto
import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.repository.AssetRepository
import ec.edu.puce.scantech.domain.repository.RechazoSync
import ec.edu.puce.scantech.domain.repository.SyncSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
 */
class AssetRepositoryImpl(
    private val assetDao: AssetDao,
    private val syncQueueDao: SyncQueueDao,
    private val api: AssetApi,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val clock: () -> Long = System::currentTimeMillis
) : AssetRepository {

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

    override suspend fun create(asset: Asset): Result<Asset> = local {
        val entidad = asset.toEntity().copy(
            qrCode = asset.qrCode.trim(),
            syncStatus = SyncStatus.PENDING_CREATE,
            updatedAt = clock(),
            deletedAt = null
        )
        assetDao.insertWithQueue(entidad, payloadDe(entidad.toDto()))
        Result.Success(entidad.toDomain())
    }

    override suspend fun update(asset: Asset): Result<Asset> = local {
        val entidad = asset.toEntity().copy(qrCode = asset.qrCode.trim(), updatedAt = clock())
        if (!assetDao.updateWithQueue(entidad, payloadDe(entidad.toDto()))) {
            Result.Error("El activo ya no existe")
        } else {
            // Se relee para devolver el syncStatus que decidió la transacción.
            Result.Success((assetDao.getById(entidad.id) ?: entidad).toDomain())
        }
    }

    override suspend fun updateObservations(id: String, observations: String?): Result<Asset> = local {
        val texto = observations?.trim()?.takeIf { it.isNotEmpty() }
        val actualizado = assetDao.updateObservationsWithQueue(id, texto, clock()) { payloadDe(it.toDto()) }
        if (actualizado == null) {
            Result.Error("El activo ya no existe")
        } else {
            Result.Success(actualizado.toDomain())
        }
    }

    override suspend fun delete(id: String): Result<Unit> = local {
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
            var enviadas = 0
            val rechazos = mutableListOf<RechazoSync>()

            // Lotes acotados para controlar el payload en conexiones inestables. Cada lote se
            // confirma por separado: si la red se corta, lo ya aceptado queda guardado.
            for (lote in syncQueueDao.listarPendientes().chunked(TAMANO_LOTE)) {
                val request = SyncBatchRequest(
                    lote.map {
                        SyncOperationDto(
                            opId = it.id.toString(),
                            tipo = it.tipo.name,
                            asset = json.decodeFromString(AssetDto.serializer(), it.payload)
                        )
                    }
                )
                val resultados = api.sincronizar(request).resultados.associateBy { it.opId }
                val (ok, fallidas) = lote.partition { resultados[it.id.toString()]?.aceptado == true }

                assetDao.confirmOperations(ok)
                fallidas.forEach { op ->
                    syncQueueDao.sumarIntento(op.id)
                    rechazos += RechazoSync(
                        op.assetId,
                        resultados[op.id.toString()]?.motivo ?: "El servidor no confirmó la operación"
                    )
                }
                enviadas += ok.size
            }

            // Bajada incremental: solo lo posterior al cursor guardado en Room. El DAO aplica
            // los cambios sin pisar pendientes locales y avanza el cursor en la misma transacción.
            val remotos = api.obtenerCambios(assetDao.lastSyncTimestamp())
            val paraRevisar = assetDao.applyRemoteChanges(remotos.map { it.toEntity() })

            Result.Success(SyncSummary(enviadas, rechazos, remotos.size, paraRevisar))
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
}
