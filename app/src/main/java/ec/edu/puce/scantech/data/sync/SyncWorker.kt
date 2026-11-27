package ec.edu.puce.scantech.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ec.edu.puce.scantech.ScanTechApp
import ec.edu.puce.scantech.domain.Result as ResultadoDominio
import ec.edu.puce.scantech.domain.repository.SyncSummary
import ec.edu.puce.scantech.domain.usecase.SincronizarActivosUseCase
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Qué debe hacer WorkManager después de una ronda de sincronización. */
enum class AccionSync { EXITO, REINTENTAR, FALLO }

/**
 * Sincronización en segundo plano (Estrategia de Datos §4).
 *
 * Cada ronda sube la cola de pendientes y luego baja de forma incremental los cambios del
 * servidor posteriores a `lastSyncTimestamp` (ver [SincronizarActivosUseCase]). Solo corre
 * con conexión; si la red se cae o el servidor falla, WorkManager reintenta con backoff.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as ScanTechApp).container.assetRepository
        val resultado = SincronizarActivosUseCase(repository)()
        return when (decidir(resultado, runAttemptCount)) {
            AccionSync.EXITO -> Result.success()
            AccionSync.REINTENTAR -> Result.retry()
            AccionSync.FALLO -> Result.failure()
        }
    }

    companion object {
        const val NOMBRE_UNICO = "scantech_sync"
        const val INTERVALO_MINUTOS = 15L
        const val MAX_REINTENTOS = 5

        /**
         * Traduce el resultado de la ronda a una acción de WorkManager. Los fallos temporales
         * (sin red, error 5xx o 429 del servidor) se reintentan hasta [MAX_REINTENTOS]; el
         * resto (p. ej. 4xx) no se arregla reintentando.
         *
         * Las operaciones rechazadas por el servidor no cuentan como fallo: siguen en la cola
         * y se vuelven a enviar en la próxima ronda.
         */
        fun decidir(resultado: ResultadoDominio<SyncSummary>, intento: Int): AccionSync = when (resultado) {
            is ResultadoDominio.Success -> AccionSync.EXITO
            is ResultadoDominio.Error -> {
                val causa = resultado.cause
                val temporal = causa is IOException ||
                    (causa is HttpException && (causa.code() >= 500 || causa.code() == 429))
                if (temporal && intento < MAX_REINTENTOS) AccionSync.REINTENTAR else AccionSync.FALLO
            }
        }

        /**
         * Programa la sincronización periódica, solo con red. `KEEP` evita reiniciar el
         * calendario cada vez que se abre la app.
         */
        fun programar(context: Context) {
            val peticion = PeriodicWorkRequestBuilder<SyncWorker>(INTERVALO_MINUTOS, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NOMBRE_UNICO,
                ExistingPeriodicWorkPolicy.KEEP,
                peticion
            )
        }
    }
}
