package ec.edu.puce.scantech.data.sync

import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.repository.RechazoSync
import ec.edu.puce.scantech.domain.repository.SyncSummary
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class SyncWorkerDecisionTest {

    private fun http(codigo: Int) = HttpException(Response.error<Any>(codigo, "".toResponseBody()))

    @Test
    fun unaRondaExitosaTerminaEnExito() {
        val resumen = SyncSummary(sent = 1, rejected = emptyList(), received = 2, needsReview = emptyList())
        assertEquals(AccionSync.EXITO, SyncWorker.decidir(Result.Success(resumen), intento = 0))
    }

    @Test
    fun lasOperacionesRechazadasNoSonUnFalloDelWorker() {
        val resumen = SyncSummary(0, listOf(RechazoSync("1", "QR duplicado")), 0, emptyList())
        assertEquals(AccionSync.EXITO, SyncWorker.decidir(Result.Success(resumen), intento = 0))
    }

    @Test
    fun sinRedOErrorDelServidorSeReintenta() {
        assertEquals(AccionSync.REINTENTAR, SyncWorker.decidir(Result.Error("sin red", IOException()), 0))
        assertEquals(AccionSync.REINTENTAR, SyncWorker.decidir(Result.Error("500", http(503)), 0))
        assertEquals(AccionSync.REINTENTAR, SyncWorker.decidir(Result.Error("429", http(429)), 0))
        assertEquals(AccionSync.REINTENTAR, SyncWorker.decidir(Result.Error("408", http(408)), 0))
    }

    @Test
    fun unErrorDelClienteNoSeReintenta() {
        assertEquals(AccionSync.FALLO, SyncWorker.decidir(Result.Error("400", http(400)), 0))
        assertEquals(AccionSync.FALLO, SyncWorker.decidir(Result.Error("404", http(404)), 0))
        assertEquals(AccionSync.FALLO, SyncWorker.decidir(Result.Error("desconocido"), 0))
    }

    @Test
    fun trasElMaximoDeReintentosSeDejaDeReintentar() {
        val error = Result.Error("sin red", IOException())
        assertEquals(AccionSync.REINTENTAR, SyncWorker.decidir(error, SyncWorker.MAX_REINTENTOS - 1))
        assertEquals(AccionSync.FALLO, SyncWorker.decidir(error, SyncWorker.MAX_REINTENTOS))
    }
}
