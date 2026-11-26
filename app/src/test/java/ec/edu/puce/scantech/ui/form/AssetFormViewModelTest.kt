package ec.edu.puce.scantech.ui.form

import androidx.lifecycle.SavedStateHandle
import ec.edu.puce.scantech.domain.usecase.GuardarActivoUseCase
import ec.edu.puce.scantech.domain.usecase.ObtenerActivoPorIdUseCase
import ec.edu.puce.scantech.domain.usecase.RepositorioGuardadoFalso
import ec.edu.puce.scantech.domain.usecase.activoDePrueba
import ec.edu.puce.scantech.domain.validacion.CampoActivo
import ec.edu.puce.scantech.domain.validacion.ValidadorActivo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class AssetFormViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: RepositorioGuardadoFalso

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        repo = RepositorioGuardadoFalso()
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearVm(assetId: String? = null) = AssetFormViewModel(
        assetId = assetId,
        guardarActivoUseCase = GuardarActivoUseCase(repo),
        obtenerActivoPorIdUseCase = ObtenerActivoPorIdUseCase(repo),
        savedStateHandle = SavedStateHandle(),
        ioDispatcher = dispatcher
    )

    private fun AssetFormViewModel.llenarValido(qr: String = "LTIC-000002") {
        onQrChange(qr)
        onNameChange("Switch Cisco")
        onCategoryChange("Redes")
        onUnitsChange("2")
    }

    @Test
    fun camposInvalidosMuestranErroresPorCampoYNoGuardan() = runTest(dispatcher) {
        val vm = crearVm()
        vm.onQrChange("abc")
        vm.onNameChange("")
        vm.onUnitsChange("-1")
        vm.onObservationsChange("x".repeat(ValidadorActivo.MAX_OBSERVACIONES + 1))

        vm.saveAsset()
        advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(
            setOf(CampoActivo.QR_CODE, CampoActivo.NOMBRE, CampoActivo.UNITS, CampoActivo.OBSERVACIONES),
            estado.errores.keys
        )
        assertEquals(AssetFormViewModel.MENSAJE_CORREGIR_CAMPOS, estado.errorGeneral)
        assertFalse(estado.isSavedSuccess)
        assertTrue("no debe consultar ni escribir en Room", repo.consultasQr.isEmpty())
        assertTrue(repo.escrituras.isEmpty())
    }

    @Test
    fun qrDuplicadoSeMuestraEnElCampoQrYNoGuarda() = runTest(dispatcher) {
        repo.activos += activoDePrueba(id = "otro", qr = "LTIC-000002")
        val vm = crearVm()
        vm.llenarValido(qr = "LTIC-000002")

        vm.saveAsset()
        advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(ValidadorActivo.MENSAJE_QR_DUPLICADO, estado.errores[CampoActivo.QR_CODE])
        assertFalse(estado.isSaving)
        assertFalse(estado.isSavedSuccess)
        assertTrue(repo.escrituras.isEmpty())
    }

    @Test
    fun alCorregirElQrDuplicadoSeLimpiaSuError() = runTest(dispatcher) {
        repo.activos += activoDePrueba(id = "otro", qr = "LTIC-000002")
        val vm = crearVm()
        vm.llenarValido(qr = "LTIC-000002")
        vm.saveAsset()
        advanceUntilIdle()

        vm.onQrChange("LTIC-000003")

        assertTrue(CampoActivo.QR_CODE !in vm.uiState.value.errores)
    }

    @Test
    fun datosValidosSeGuardanUnaSolaVezAunqueSeToqueDosVeces() = runTest(dispatcher) {
        val vm = crearVm()
        vm.llenarValido()

        vm.saveAsset()
        vm.saveAsset()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isSavedSuccess)
        assertEquals(1, repo.escrituras.size)
        assertEquals("LTIC-000002", repo.escrituras.single().qrCode)
    }

    @Test
    fun unFalloDeRoomSeMuestraComoErrorGeneral() = runTest(dispatcher) {
        repo.fallarEscritura = true
        val vm = crearVm()
        vm.llenarValido()

        vm.saveAsset()
        advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals("No se pudo guardar", estado.errorGeneral)
        assertFalse(estado.isSaving)
        assertFalse(estado.isSavedSuccess)
    }
}
