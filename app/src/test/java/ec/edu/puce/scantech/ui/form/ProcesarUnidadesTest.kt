package ec.edu.puce.scantech.ui.form

import org.junit.Assert.assertEquals
import org.junit.Test

/** [Caso 5] El parseo de unidades nunca lanza excepción: lo inválido se vuelve 0 por Elvis. */
class ProcesarUnidadesTest {

    @Test
    fun numeroValidoSeConvierte() {
        assertEquals(12, AssetFormViewModel.procesarUnidades(" 12 "))
    }

    @Test
    fun letrasVacioOTextoMixtoDevuelvenElValorPorDefecto() {
        listOf("abc", "", "   ", "5 días", "3.5").forEach { entrada ->
            assertEquals(
                "entrada: '$entrada'",
                AssetFormViewModel.UNIDADES_POR_DEFECTO,
                AssetFormViewModel.procesarUnidades(entrada)
            )
        }
    }
}
