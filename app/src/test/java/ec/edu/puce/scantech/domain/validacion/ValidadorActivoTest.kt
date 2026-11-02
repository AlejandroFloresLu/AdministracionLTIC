package ec.edu.puce.scantech.domain.validacion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorActivoTest {

    private fun datosValidos() = DatosActivo(
        qrCode = "LTIC-000123",
        nombre = "Switch Cisco",
        categoria = "Redes",
        units = "2",
        observaciones = ""
    )

    @Test
    fun datosCorrectosSonValidos() {
        assertEquals(
            ResultadoValidacion.Valido,
            ValidadorActivo.validar(datosValidos(), qrYaExiste = false)
        )
    }

    @Test
    fun camposVaciosDevuelvenErrorPorCampo() {
        val resultado = ValidadorActivo.validar(
            datosValidos().copy(nombre = " ", categoria = ""),
            qrYaExiste = false
        ) as ResultadoValidacion.Invalido

        assertTrue(CampoActivo.NOMBRE in resultado.errores)
        assertTrue(CampoActivo.CATEGORIA in resultado.errores)
    }

    @Test
    fun unitsNegativasONoNumericasSonInvalidas() {
        val negativas = ValidadorActivo.validar(
            datosValidos().copy(units = "-1"), qrYaExiste = false
        ) as ResultadoValidacion.Invalido
        val texto = ValidadorActivo.validar(
            datosValidos().copy(units = "dos"), qrYaExiste = false
        ) as ResultadoValidacion.Invalido

        assertTrue(CampoActivo.UNITS in negativas.errores)
        assertTrue(CampoActivo.UNITS in texto.errores)
    }

    @Test
    fun qrConFormatoIncorrectoOYaExistenteEsInvalido() {
        val formato = ValidadorActivo.validar(
            datosValidos().copy(qrCode = "123"), qrYaExiste = false
        ) as ResultadoValidacion.Invalido
        val duplicado = ValidadorActivo.validar(
            datosValidos(), qrYaExiste = true
        ) as ResultadoValidacion.Invalido

        assertTrue(CampoActivo.QR_CODE in formato.errores)
        assertTrue(CampoActivo.QR_CODE in duplicado.errores)
    }

    @Test
    fun observacionesSobreellimiteSonInvalidas() {
        val largo = "x".repeat(ValidadorActivo.MAX_OBSERVACIONES + 1)
        val resultado = ValidadorActivo.validar(
            datosValidos().copy(observaciones = largo), qrYaExiste = false
        ) as ResultadoValidacion.Invalido

        assertTrue(CampoActivo.OBSERVACIONES in resultado.errores)
    }
}
