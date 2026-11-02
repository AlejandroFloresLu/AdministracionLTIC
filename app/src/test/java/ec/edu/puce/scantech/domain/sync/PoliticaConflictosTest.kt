package ec.edu.puce.scantech.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliticaConflictosTest {

    private fun activo(updatedAt: Long, units: Int) =
        ActivoSincronizable(id = "A1", updatedAt = updatedAt, units = units)

    @Test
    fun gananaVersionMasRecienteDelServidor() {
        val resultado = PoliticaConflictos.resolver(
            local = activo(updatedAt = 1_000L, units = 5),
            servidor = activo(updatedAt = 2_000L, units = 5),
            unitsVisto = 5
        )
        assertTrue(resultado is ResolucionConflicto.GanaServidor)
    }

    @Test
    fun ganaLocalCuandoEsMasReciente() {
        val resultado = PoliticaConflictos.resolver(
            local = activo(updatedAt = 3_000L, units = 5),
            servidor = activo(updatedAt = 2_000L, units = 5),
            unitsVisto = 5
        )
        assertTrue(resultado is ResolucionConflicto.GanaLocal)
    }

    @Test
    fun empateConservaLaVersionLocal() {
        val resultado = PoliticaConflictos.resolver(
            local = activo(updatedAt = 2_000L, units = 5),
            servidor = activo(updatedAt = 2_000L, units = 5),
            unitsVisto = 5
        )
        assertTrue(resultado is ResolucionConflicto.GanaLocal)
    }

    @Test
    fun cambioDeUnitsEnServidorYLocalSeMarcaParaRevision() {
        val resultado = PoliticaConflictos.resolver(
            local = activo(updatedAt = 3_000L, units = 7),
            servidor = activo(updatedAt = 2_000L, units = 9),
            unitsVisto = 5
        )
        assertTrue(resultado is ResolucionConflicto.RevisionManual)
    }

    @Test
    fun siServidorYLocalCoincidenEnUnitsNoHayRevision() {
        val resultado = PoliticaConflictos.resolver(
            local = activo(updatedAt = 3_000L, units = 9),
            servidor = activo(updatedAt = 2_000L, units = 9),
            unitsVisto = 5
        )
        assertEquals(ResolucionConflicto.GanaLocal(activo(3_000L, 9)), resultado)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rechazaCompararActivosDistintos() {
        PoliticaConflictos.resolver(
            local = activo(1L, 1),
            servidor = activo(1L, 1).copy(id = "OTRO"),
            unitsVisto = null
        )
    }
}
