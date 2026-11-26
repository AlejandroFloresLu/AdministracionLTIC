package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.validacion.CampoActivo
import ec.edu.puce.scantech.domain.validacion.ValidadorActivo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GuardarActivoUseCaseTest {

    private lateinit var repo: RepositorioGuardadoFalso
    private lateinit var guardar: GuardarActivoUseCase

    @Before
    fun preparar() {
        repo = RepositorioGuardadoFalso()
        guardar = GuardarActivoUseCase(repo)
    }

    @Test
    fun datosValidosSeGuardan() = runTest {
        val resultado = guardar(activoDePrueba())

        assertTrue(resultado is ResultadoGuardado.Guardado)
        assertEquals(1, repo.escrituras.size)
    }

    @Test
    fun qrDuplicadoDevuelveErrorEnElCampoQrYNoGuarda() = runTest {
        repo.activos += activoDePrueba(id = "otro", qr = "LTIC-000001")

        val resultado = guardar(activoDePrueba(id = "nuevo", qr = "LTIC-000001"))

        assertEquals(
            ResultadoGuardado.Invalido(mapOf(CampoActivo.QR_CODE to ValidadorActivo.MENSAJE_QR_DUPLICADO)),
            resultado
        )
        assertTrue(repo.escrituras.isEmpty())
    }

    @Test
    fun alEditarElPropioQrNoCuentaComoDuplicado() = runTest {
        repo.activos += activoDePrueba(id = "id-1", qr = "LTIC-000001")

        val resultado = guardar(activoDePrueba(id = "id-1", nombre = "Renombrado"), isEditMode = true)

        assertTrue(resultado is ResultadoGuardado.Guardado)
        assertEquals("debe excluir al propio activo", "id-1", repo.consultasQr.single().second)
    }

    @Test
    fun variosCamposInvalidosDevuelvenTodosLosErroresYNoGuarda() = runTest {
        repo.activos += activoDePrueba(id = "otro", qr = "LTIC-000001")

        val resultado = guardar(
            activoDePrueba(
                id = "nuevo",
                nombre = " ",
                unidades = -3,
                observaciones = "x".repeat(ValidadorActivo.MAX_OBSERVACIONES + 1)
            )
        ) as ResultadoGuardado.Invalido

        assertEquals(
            setOf(CampoActivo.QR_CODE, CampoActivo.NOMBRE, CampoActivo.UNITS, CampoActivo.OBSERVACIONES),
            resultado.errores.keys
        )
        assertTrue(repo.escrituras.isEmpty())
    }

    @Test
    fun conFormatoDeQrInvalidoNoSeConsultaRoom() = runTest {
        val resultado = guardar(activoDePrueba(qr = "ABC")) as ResultadoGuardado.Invalido

        assertTrue(CampoActivo.QR_CODE in resultado.errores)
        assertTrue(repo.consultasQr.isEmpty())
        assertTrue(repo.escrituras.isEmpty())
    }

    @Test
    fun siNoSePuedeVerificarElQrNoSeGuarda() = runTest {
        repo.fallarConsultaQr = true

        val resultado = guardar(activoDePrueba())

        assertTrue(resultado is ResultadoGuardado.Fallo)
        assertTrue(repo.escrituras.isEmpty())
    }

    @Test
    fun unFalloAlEscribirSeDevuelveComoFallo() = runTest {
        repo.fallarEscritura = true

        val resultado = guardar(activoDePrueba())

        assertEquals(ResultadoGuardado.Fallo("No se pudo guardar"), resultado)
    }
}
