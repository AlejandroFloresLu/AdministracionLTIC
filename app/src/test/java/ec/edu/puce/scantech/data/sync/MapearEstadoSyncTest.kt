package ec.edu.puce.scantech.data.sync

import androidx.work.WorkInfo
import ec.edu.puce.scantech.data.sync.ProgramadorSyncWorkManager.Companion.SalidaSync
import ec.edu.puce.scantech.data.sync.ProgramadorSyncWorkManager.Companion.mapearEstado
import ec.edu.puce.scantech.domain.sync.EstadoSincronizacion
import org.junit.Assert.assertEquals
import org.junit.Test

class MapearEstadoSyncTest {

    private val sinSalida = SalidaSync(enviadas = 0, recibidas = 0, enRevision = 0, mensaje = null)

    @Test
    fun enColaSinIntentosEsperaConexion() {
        assertEquals(EstadoSincronizacion.EnEspera, mapearEstado(WorkInfo.State.ENQUEUED, 0, sinSalida, "r"))
    }

    @Test
    fun enColaTrasUnFalloEstaReintentandoConBackoff() {
        assertEquals(EstadoSincronizacion.Reintentando, mapearEstado(WorkInfo.State.ENQUEUED, 2, sinSalida, "r"))
    }

    @Test
    fun corriendoEstaEnCurso() {
        assertEquals(EstadoSincronizacion.EnCurso, mapearEstado(WorkInfo.State.RUNNING, 0, sinSalida, "r"))
    }

    @Test
    fun terminadaLlevaLosTotalesYLaRonda() {
        val salida = SalidaSync(enviadas = 3, recibidas = 5, enRevision = 1, mensaje = null)

        assertEquals(
            EstadoSincronizacion.Completada(enviadas = 3, recibidas = 5, enRevision = 1, ronda = "r1"),
            mapearEstado(WorkInfo.State.SUCCEEDED, 0, salida, "r1")
        )
    }

    @Test
    fun fallidaLlevaElMensajeDelWorker() {
        val salida = sinSalida.copy(mensaje = "El servidor respondió con error 400")

        assertEquals(
            EstadoSincronizacion.Fallida("El servidor respondió con error 400", ronda = "r1"),
            mapearEstado(WorkInfo.State.FAILED, 0, salida, "r1")
        )
    }
}
