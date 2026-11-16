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
            val pendientes = syncQueueDao.listarPendientes()
            var enviadas = 0
            var rechazadas = 0

            if (pendientes.isNotEmpty()) {
                val lote = SyncBatchRequest(
                    pendientes.map {
                        SyncOperationDto(
                            opId = it.id.toString(),
                            tipo = it.tipo.name,
                            asset = json.decodeFromString(AssetDto.serializer(), it.payload)
                        )
                    }
                )
                val aceptadas = api.sincronizar(lote).resultados
                    .filter { it.aceptado }
                    .map { it.opId }
                    .toSet()
                val (ok, fallidas) = pendientes.partition { it.id.toString() in aceptadas }

                assetDao.confirmOperations(ok)
                fallidas.forEach { syncQueueDao.sumarIntento(it.id) }
                enviadas = ok.size
                rechazadas = fallidas.size
            }

            val remotos = api.obtenerCambios(assetDao.lastSyncedUpdatedAt())
            val paraRevisar = assetDao.applyRemoteChanges(remotos.map { it.toEntity() })

            Result.Success(SyncSummary(enviadas, rechazadas, remotos.size, paraRevisar))
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
