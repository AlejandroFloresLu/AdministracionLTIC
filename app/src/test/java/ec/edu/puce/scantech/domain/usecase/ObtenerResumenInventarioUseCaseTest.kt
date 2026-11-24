package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.model.ResumenInventario
import ec.edu.puce.scantech.domain.model.SyncStatus
import ec.edu.puce.scantech.domain.repository.AssetRepository
import ec.edu.puce.scantech.domain.repository.SyncSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObtenerResumenInventarioUseCaseTest {

    @Test
    fun sumaActivosUnidadesYPendientes() = runTest {
        val activos = listOf(
            activo("LTIC-000001", unidades = 3, estado = SyncStatus.SYNCED),
            activo("LTIC-000002", unidades = 5, estado = SyncStatus.PENDING_CREATE),
            activo("LTIC-000003", unidades = 2, estado = SyncStatus.PENDING_UPDATE)
        )
        val useCase = ObtenerResumenInventarioUseCase(
            RepositorioFijo(activos),
            StandardTestDispatcher(testScheduler)
        )

        assertEquals(ResumenInventario(totalActivos = 3, totalUnidades = 10, pendientesSync = 2), useCase().first())
    }

    private fun activo(qr: String, unidades: Int, estado: SyncStatus) = Asset(
        id = qr, qrCode = qr, name = "Activo $qr", category = "Laptops", technicalSpecs = "",
        block = "A", shelf = "1", locker = "1", units = unidades, syncStatus = estado
    )

    private class RepositorioFijo(private val activos: List<Asset>) : AssetRepository {
        override fun observeAssets(query: String, category: String?): Flow<List<Asset>> = flowOf(activos)
        override fun observeById(id: String): Flow<Asset?> = error("no usado")
        override fun observePendingCount(): Flow<Int> = error("no usado")
        override suspend fun getByQrCode(qrCode: String): Result<Asset> = error("no usado")
        override suspend fun existsQrCode(qrCode: String, excludeId: String?): Result<Boolean> = error("no usado")
        override suspend fun create(asset: Asset): Result<Asset> = error("no usado")
        override suspend fun update(asset: Asset): Result<Asset> = error("no usado")
        override suspend fun updateObservations(id: String, observations: String?): Result<Asset> = error("no usado")
        override suspend fun delete(id: String): Result<Unit> = error("no usado")
        override suspend fun sync(): Result<SyncSummary> = error("no usado")
    }
}
