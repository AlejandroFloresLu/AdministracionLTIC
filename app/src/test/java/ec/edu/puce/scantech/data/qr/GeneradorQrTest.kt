package ec.edu.puce.scantech.data.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneradorQrTest {

    @Test
    fun formateaConPrefijoYCeroAlaIzquierda() {
        assertEquals("LTIC-000123", GeneradorQr.formatearCodigo(123))
        assertEquals("LTIC-999999", GeneradorQr.formatearCodigo(999_999))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rechazaNumerosFueraDeRango() {
        GeneradorQr.formatearCodigo(1_000_000)
    }

    @Test
    fun validaElFormatoDelIdentificador() {
        assertTrue(GeneradorQr.esCodigoValido("LTIC-000123"))
        assertFalse(GeneradorQr.esCodigoValido("ltic-000123"))
        assertFalse(GeneradorQr.esCodigoValido("LTIC-12"))
        assertFalse(GeneradorQr.esCodigoValido("LTIC-000123 "))
    }
}
