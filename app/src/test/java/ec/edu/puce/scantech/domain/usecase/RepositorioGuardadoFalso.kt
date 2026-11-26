package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.repository.AssetRepository
import ec.edu.puce.scantech.domain.repository.SyncSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repositorio en memoria para probar el guardado: registra cada escritura en [escrituras]
 * para comprobar que con errores no se toca Room.
 */
class RepositorioGuardadoFalso : AssetRepository {
    val activos = mutableListOf<Asset>()
    val escrituras = mutableListOf<Asset>()
    val consultasQr = mutableListOf<Pair<String, String?>>()
    var fallarConsultaQr = false
    var fallarEscritura = false

    override suspend fun existsQrCode(qrCode: String, excludeId: String?): Result<Boolean> {
        consultasQr += qrCode to excludeId
        if (fallarConsultaQr) return Result.Error("disco lleno")
        return Result.Success(activos.any { it.qrCode == qrCode.trim() && it.id != excludeId })
    }

    override suspend fun create(asset: Asset): Result<Asset> = escribir(asset)

    override suspend fun update(asset: Asset): Result<Asset> = escribir(asset)

    private fun escribir(asset: Asset): Result<Asset> {
        if (fallarEscritura) return Result.Error("No se pudo guardar")
        escrituras += asset
        activos.removeAll { it.id == asset.id }
        activos += asset
        return Result.Success(asset)
    }

    override fun observeById(id: String): Flow<Asset?> = flowOf(activos.find { it.id == id })
    override fun observeAssets(query: String, category: String?): Flow<List<Asset>> = error("no usado")
    override fun observePendingCount(): Flow<Int> = error("no usado")
    override suspend fun getByQrCode(qrCode: String): Result<Asset> = error("no usado")
    override suspend fun updateObservations(id: String, observations: String?): Result<Asset> = error("no usado")
    override suspend fun delete(id: String): Result<Unit> = error("no usado")
    override suspend fun sync(): Result<SyncSummary> = error("no usado")
}

fun activoDePrueba(
    id: String = "id-1",
    qr: String = "LTIC-000001",
    nombre: String = "Laptop Dell",
    categoria: String = "Laptops",
    unidades: Int = 1,
    observaciones: String? = null
) = Asset(
    id = id,
    qrCode = qr,
    name = nombre,
    category = categoria,
    technicalSpecs = "",
    block = "A",
    shelf = "1",
    locker = "1",
    units = unidades,
    observations = observaciones
)
