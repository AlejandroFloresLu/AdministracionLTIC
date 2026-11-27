package ec.edu.puce.scantech.ui.detail

import androidx.lifecycle.SavedStateHandle
import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.repository.AssetRepository
import ec.edu.puce.scantech.domain.repository.SyncSummary
import ec.edu.puce.scantech.domain.usecase.EliminarActivoUseCase
import ec.edu.puce.scantech.domain.usecase.GuardarObservacionesUseCase
import ec.edu.puce.scantech.domain.usecase.ObtenerActivoPorIdUseCase
import ec.edu.puce.scantech.domain.usecase.activoDePrueba
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Baja lógica desde la ficha técnica (Estrategia de Datos §3.c). */
@OptIn(ExperimentalCoroutinesApi::class)
class AssetDetailViewModelBajaTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: RepositorioBajaFalso

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        repo = RepositorioBajaFalso(activoDePrueba(id = "id-1"))
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearVm() = AssetDetailViewModel(
        assetId = "id-1",
        obtenerActivoPorIdUseCase = ObtenerActivoPorIdUseCase(repo),
        eliminarActivoUseCase = EliminarActivoUseCase(repo),
        guardarObservacionesUseCase = GuardarObservacionesUseCase(repo),
        savedStateHandle = SavedStateHandle()
    )

    @Test
    fun bajaExitosaNavegaAtrasSinMostrarError() = runTest(dispatcher) {
        val vm = crearVm()
        val eventos = mutableListOf<BajaEvento>()
        backgroundScope.launch { vm.eventosBaja.toList(eventos) }
        advanceUntilIdle()

        vm.eliminarActivo()
        advanceUntilIdle()

        assertEquals(listOf<BajaEvento>(BajaEvento.Completada), eventos)
        assertEquals(listOf("id-1"), repo.bajas)
        // Room ya oculta el activo (emite null), pero no es un error: la ficha se mantiene.
        val estado = vm.uiState.value
        assertTrue(estado is AssetDetailUiState.Success)
        assertTrue((estado as AssetDetailUiState.Success).isDeleting)
    }

    @Test
    fun toquesRepetidosSoloEncolanUnaBaja() = runTest(dispatcher) {
        repo.retenerBaja = CompletableDeferred()
        val vm = crearVm()
        advanceUntilIdle()

        vm.eliminarActivo()
        advanceUntilIdle()
        assertTrue((vm.uiState.value as AssetDetailUiState.Success).isDeleting)
        vm.eliminarActivo()
        repo.retenerBaja?.complete(Unit)
        advanceUntilIdle()
        vm.eliminarActivo()
        advanceUntilIdle()

        assertEquals(1, repo.bajas.size)
    }

    @Test
    fun bajaFallidaAvisaYDejaLaFichaUsable() = runTest(dispatcher) {
        repo.errorBaja = "El activo ya no existe"
        val vm = crearVm()
        val eventos = mutableListOf<BajaEvento>()
        backgroundScope.launch { vm.eventosBaja.toList(eventos) }
        advanceUntilIdle()

        vm.eliminarActivo()
        advanceUntilIdle()

        assertEquals(listOf<BajaEvento>(BajaEvento.Error("El activo ya no existe")), eventos)
        val estado = vm.uiState.value as AssetDetailUiState.Success
        assertFalse(estado.isDeleting)
    }

    @Test
    fun activoBorradoPorOtroLadoSiMuestraError() = runTest(dispatcher) {
        val vm = crearVm()
        advanceUntilIdle()

        // Baja que no inició esta pantalla (p. ej. la sincronización): sí es "no existe".
        repo.ocultar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is AssetDetailUiState.Error)
    }

    @Test
    fun casoDeUsoRechazaIdVacioSinTocarElRepositorio() = runTest(dispatcher) {
        val resultado = EliminarActivoUseCase(repo)("  ")

        assertTrue(resultado is Result.Error)
        assertTrue(repo.bajas.isEmpty())
    }
}

/** Imita a Room: tras la baja lógica, `observeById` emite `null` (el activo queda oculto). */
private class RepositorioBajaFalso(activo: Asset) : AssetRepository {
    private val visible = MutableStateFlow<Asset?>(activo)
    val bajas = mutableListOf<String>()
    var errorBaja: String? = null
    var retenerBaja: CompletableDeferred<Unit>? = null

    fun ocultar() {
        visible.value = null
    }

    override suspend fun delete(id: String): Result<Unit> {
        bajas += id
        retenerBaja?.await()
        errorBaja?.let { return Result.Error(it) }
        ocultar()
        return Result.Success(Unit)
    }

    override fun observeById(id: String): Flow<Asset?> = visible
    override fun observeAssets(query: String, category: String?): Flow<List<Asset>> = error("no usado")
    override fun observePendingCount(): Flow<Int> = error("no usado")
    override suspend fun getByQrCode(qrCode: String): Result<Asset> = error("no usado")
    override suspend fun existsQrCode(qrCode: String, excludeId: String?): Result<Boolean> = error("no usado")
    override suspend fun create(asset: Asset): Result<Asset> = error("no usado")
    override suspend fun update(asset: Asset): Result<Asset> = error("no usado")
    override suspend fun updateObservations(id: String, observations: String?): Result<Asset> = error("no usado")
    override suspend fun sync(): Result<SyncSummary> = error("no usado")
}
