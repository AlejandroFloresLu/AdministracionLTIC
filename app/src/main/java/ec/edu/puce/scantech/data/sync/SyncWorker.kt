package ec.edu.puce.scantech.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ec.edu.puce.scantech.ScanTechApp
import ec.edu.puce.scantech.data.remote.ErroresHttp
import ec.edu.puce.scantech.domain.Result as ResultadoDominio
import ec.edu.puce.scantech.domain.repository.SyncSummary
import ec.edu.puce.scantech.domain.usecase.SincronizarActivosUseCase
import retrofit2.HttpException
import java.io.IOException

/** Qué debe hacer WorkManager después de una ronda de sincronización. */
enum class AccionSync { EXITO, REINTENTAR, FALLO }

/**
 * Ronda de sincronización en segundo plano (Estrategia de Datos §4): sube la cola de
 * pendientes y baja de forma incremental los cambios del servidor.
 *
 * La programan los tres disparadores de [ProgramadorSyncWorkManager] (al guardar, periódico y
 * manual), siempre con `NetworkType.CONNECTED`.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as ScanTechApp).container.assetRepository
        val resultado = SincronizarActivosUseCase(repository)()
        return when (decidir(resultado, runAttemptCount)) {
            AccionSync.EXITO -> {
                val resumen = (resultado as ResultadoDominio.Success).data
                Result.success(
                    workDataOf(
                        SALIDA_ENVIADAS to resumen.sent,
                        SALIDA_RECIBIDAS to resumen.received,
                        SALIDA_EN_REVISION to resumen.rejected.size
                    )
                )
            }
            AccionSync.REINTENTAR -> Result.retry()
            AccionSync.FALLO -> Result.failure(
                workDataOf(SALIDA_MENSAJE to (resultado as? ResultadoDominio.Error)?.message)
            )
        }
    }

    companion object {
        const val MAX_REINTENTOS = 5

        const val SALIDA_ENVIADAS = "enviadas"
        const val SALIDA_RECIBIDAS = "recibidas"
        const val SALIDA_EN_REVISION = "en_revision"
        const val SALIDA_MENSAJE = "mensaje"

        /**
         * Traduce el resultado de la ronda a una acción de WorkManager:
         * - Sin red o 5xx (también 408 y 429): [AccionSync.REINTENTAR], y WorkManager aplica el
         *   backoff exponencial, hasta [MAX_REINTENTOS].
         * - 4xx u otro error: [AccionSync.FALLO], no se reintenta. Los 4xx al subir la cola ya
         *   no llegan aquí: el repositorio marca esas operaciones para revisión y sigue.
         *
         * Las operaciones rechazadas por el servidor no son un fallo del worker: quedan en
         * revisión y la ronda termina con éxito.
         */
        fun decidir(resultado: ResultadoDominio<SyncSummary>, intento: Int): AccionSync = when (resultado) {
            is ResultadoDominio.Success -> AccionSync.EXITO
            is ResultadoDominio.Error -> {
                val causa = resultado.cause
                val temporal = causa is IOException ||
                    (causa is HttpException && ErroresHttp.esTemporal(causa.code()))
                if (temporal && intento < MAX_REINTENTOS) AccionSync.REINTENTAR else AccionSync.FALLO
            }
        }
    }
}
