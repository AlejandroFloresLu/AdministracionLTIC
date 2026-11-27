package ec.edu.puce.scantech.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import ec.edu.puce.scantech.domain.sync.EstadoSincronizacion
import ec.edu.puce.scantech.domain.sync.ProgramadorSync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * Los tres disparadores de la sincronización sobre WorkManager (Estrategia de Datos §4).
 *
 * Todos exigen `NetworkType.CONNECTED` y usan backoff exponencial: cuando [SyncWorker]
 * devuelve `retry()` (sin red o 5xx), WorkManager espera 30 s, 60 s, 120 s... antes de reintentar.
 */
class ProgramadorSyncWorkManager(context: Context) : ProgramadorSync {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    /** Respaldo cada [INTERVALO_MINUTOS]. `KEEP` no reinicia el calendario al abrir la app. */
    override fun programarPeriodica() {
        val peticion = PeriodicWorkRequestBuilder<SyncWorker>(INTERVALO_MINUTOS, TimeUnit.MINUTES)
            .configurar()
            .build()
        workManager.enqueueUniquePeriodicWork(TRABAJO_PERIODICO, ExistingPeriodicWorkPolicy.KEEP, peticion)
    }

    /**
     * Trabajo único tras cada escritura local, con `KEEP`: varios guardados seguidos sin red
     * se agrupan en una sola ronda pendiente.
     *
     * Limitación aceptada: si se guarda mientras esta ronda ya está corriendo, `KEEP` descarta
     * la nueva petición y ese cambio sube con el siguiente disparador (periódico o manual).
     */
    override fun programarTrasGuardar() {
        workManager.enqueueUniqueWork(TRABAJO_AL_GUARDAR, ExistingWorkPolicy.KEEP, rondaUnica())
    }

    /** Sincronización manual. `KEEP` ignora toques repetidos mientras ya hay una en camino. */
    override fun sincronizarAhora() {
        workManager.enqueueUniqueWork(TRABAJO_MANUAL, ExistingWorkPolicy.KEEP, rondaUnica())
    }

    override fun observarSincronizacionManual(): Flow<EstadoSincronizacion> =
        workManager.getWorkInfosForUniqueWorkFlow(TRABAJO_MANUAL)
            .map { infos ->
                // Con un trabajo único puede quedar el historial de rondas anteriores:
                // manda la que sigue activa o, si no hay, la última.
                val info = infos.firstOrNull { !it.state.isFinished } ?: infos.lastOrNull()
                if (info == null) {
                    EstadoSincronizacion.Inactiva
                } else {
                    mapearEstado(info.state, info.runAttemptCount, info.outputData.aSalida(), info.id.toString())
                }
            }
            .distinctUntilChanged()

    private fun rondaUnica() = OneTimeWorkRequestBuilder<SyncWorker>().configurar().build()

    private fun <B : WorkRequest.Builder<B, *>> B.configurar(): B =
        setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_INICIAL_SEGUNDOS, TimeUnit.SECONDS)

    private fun Data.aSalida() = SalidaSync(
        enviadas = getInt(SyncWorker.SALIDA_ENVIADAS, 0),
        recibidas = getInt(SyncWorker.SALIDA_RECIBIDAS, 0),
        enRevision = getInt(SyncWorker.SALIDA_EN_REVISION, 0),
        mensaje = getString(SyncWorker.SALIDA_MENSAJE)
    )

    companion object {
        // Mismo nombre que usaba el #36: así no queda un segundo periódico duplicado al actualizar
        const val TRABAJO_PERIODICO = "scantech_sync"
        const val TRABAJO_AL_GUARDAR = "scantech_sync_al_guardar"
        const val TRABAJO_MANUAL = "scantech_sync_manual"
        const val INTERVALO_MINUTOS = 15L
        const val BACKOFF_INICIAL_SEGUNDOS = 30L

        /** Datos que deja [SyncWorker] al terminar. */
        data class SalidaSync(val enviadas: Int, val recibidas: Int, val enRevision: Int, val mensaje: String?)

        /** Traduce el estado de WorkManager al del dominio (separado para probarlo en la JVM). */
        fun mapearEstado(
            estado: WorkInfo.State,
            intentos: Int,
            salida: SalidaSync,
            ronda: String
        ): EstadoSincronizacion =
            when (estado) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED ->
                    if (intentos > 0) EstadoSincronizacion.Reintentando else EstadoSincronizacion.EnEspera
                WorkInfo.State.RUNNING -> EstadoSincronizacion.EnCurso
                WorkInfo.State.SUCCEEDED -> EstadoSincronizacion.Completada(
                    enviadas = salida.enviadas,
                    recibidas = salida.recibidas,
                    enRevision = salida.enRevision,
                    ronda = ronda
                )
                WorkInfo.State.FAILED -> EstadoSincronizacion.Fallida(
                    mensaje = salida.mensaje ?: "No se pudo sincronizar",
                    ronda = ronda
                )
                WorkInfo.State.CANCELLED -> EstadoSincronizacion.Inactiva
            }
    }
}
