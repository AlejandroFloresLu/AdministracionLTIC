package ec.edu.puce.scantech.ui.inventory

import androidx.lifecycle.SavedStateHandle
import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.repository.AssetRepository
import ec.edu.puce.scantech.domain.repository.SyncSummary
import ec.edu.puce.scantech.domain.usecase.ObtenerActivosUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: RepositorioFalso
    private lateinit var vm: InventoryViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        repo = RepositorioFalso()
        vm = InventoryViewModel(SavedStateHandle(), ObtenerActivosUseCase(repo))
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    @Test
    fun escribirRapidoSoloConsultaRoomConElUltimoTextoTrasElDebounce() = runTest(dispatcher) {
        observar()
        advanceUntilIdle()
        repo.consultas.clear()

        vm.onSearchQueryChange("l")
        advanceTimeBy(100)
        vm.onSearchQueryChange("la")
        advanceTimeBy(100)
        vm.onSearchQueryChange("lap")
        advanceTimeBy(InventoryViewModel.DEBOUNCE_BUSQUEDA_MS - 1)
        runCurrent()
        assertTrue("no debe consultar antes de que termine el debounce", repo.consultas.isEmpty())

        advanceUntilIdle()
        assertEquals(listOf("lap" to null), repo.consultas)
    }

    @Test
    fun laCategoriaSeAplicaSinDebounceYTodosQuitaElFiltro() = runTest(dispatcher) {
        observar()
        advanceUntilIdle()

        vm.onCategorySelect("Laptops")
        runCurrent()
        assertEquals("" to "Laptops", repo.consultas.last())

        vm.onCategorySelect(InventoryViewModel.CATEGORIA_TODAS)
        runCurrent()
        assertEquals("" to null, repo.consultas.last())
    }

    @Test
    fun borrarLaBusquedaSeAplicaAlInstante() = runTest(dispatcher) {
        observar()
        vm.onSearchQueryChange("dell")
        advanceUntilIdle()

        vm.clearSearchQuery()
        runCurrent()

        assertEquals("" to null, repo.consultas.last())
    }

    @Test
    fun laListaSeActualizaSolaCuandoCambiaLaBase() = runTest(dispatcher) {
        observar()
        advanceUntilIdle()
        val consultasAntes = repo.consultas.size
        assertEquals(emptyList<Asset>(), (vm.uiState.value as InventoryUiState.Success).activos)

        repo.base.value = listOf(activo("LTIC-000001", "Laptop Dell"))
        runCurrent()

        assertEquals(
            listOf("LTIC-000001"),
            (vm.uiState.value as InventoryUiState.Success).activos.map { it.qrCode }
        )
        // la actualización llegó por el Flow, sin volver a consultar
        assertEquals(consultasAntes, repo.consultas.size)
    }

    @Test
    fun elFiltradoLoHaceElRepositorioNoLaMemoria() = runTest(dispatcher) {
        repo.base.value = listOf(activo("LTIC-000001", "Laptop Dell"), activo("LTIC-000002", "Switch Cisco"))
        observar()

        vm.onSearchQueryChange("  cisco ")
        advanceUntilIdle()

        assertEquals("cisco" to null, repo.consultas.last())
        assertEquals(
            listOf("LTIC-000002"),
            (vm.uiState.value as InventoryUiState.Success).activos.map { it.qrCode }
        )
    }

    @Test
    fun unErrorDeLecturaSeMuestraYReintentarVuelveAConsultar() = runTest(dispatcher) {
        repo.fallar = true
        observar()
        advanceUntilIdle()
        assertTrue(vm.uiState.value is InventoryUiState.Error)

        repo.fallar = false
        vm.reintentar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is InventoryUiState.Success)
    }

    /** `uiState` usa WhileSubscribed: hace falta un suscriptor para que consulte. */
    private fun TestScope.observar() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    private fun activo(qr: String, nombre: String) = Asset(
        id = qr,
        qrCode = qr,
        name = nombre,
        category = "Laptops",
        technicalSpecs = "",
        block = "A",
        shelf = "1",
        locker = "1",
        units = 1
    )

    /** Simula Room: un Flow que vuelve a emitir cuando cambia [base] y filtra como el DAO. */
    private class RepositorioFalso : AssetRepository {
        val base = MutableStateFlow<List<Asset>>(emptyList())
        val consultas = mutableListOf<Pair<String, String?>>()
        var fallar = false

        override fun observeAssets(query: String, category: String?): Flow<List<Asset>> {
            consultas += query to category
            if (fallar) return flow { throw IllegalStateException("disco lleno") }
            return base.map { lista ->
                lista.filter {
                    (category == null || it.category.equals(category, ignoreCase = true)) &&
                        (query.isEmpty() || it.name.contains(query, ignoreCase = true))
                }
            }
        }

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
