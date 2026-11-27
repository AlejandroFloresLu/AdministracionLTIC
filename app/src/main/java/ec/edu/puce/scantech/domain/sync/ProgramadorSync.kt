package ec.edu.puce.scantech.domain.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Disparadores de la sincronización (Estrategia de Datos §4). El dominio no sabe que detrás
 * está WorkManager; la implementación real vive en `data.sync`.
 *
 * Los tres disparadores ejecutan la misma ronda (subir la cola y bajar cambios) y solo corren
 * con conexión.
 */
interface ProgramadorSync {

    /** Respaldo periódico. Se llama una vez al arrancar la app. */
    fun programarPeriodica()

    /** Tras guardar algo en Room: sincroniza en cuanto haya red. */
    fun programarTrasGuardar()

    /** Sincronización pedida por el usuario. */
    fun sincronizarAhora()

    /** Estado de la última sincronización manual. */
    fun observarSincronizacionManual(): Flow<EstadoSincronizacion>

    /** No programa nada; para pruebas y para el repositorio cuando no hay WorkManager. */
    object Nulo : ProgramadorSync {
        override fun programarPeriodica() = Unit
        override fun programarTrasGuardar() = Unit
        override fun sincronizarAhora() = Unit
        override fun observarSincronizacionManual(): Flow<EstadoSincronizacion> =
            flowOf(EstadoSincronizacion.Inactiva)
    }
}

/** Estado de una sincronización programada, tal como lo necesita la UI. */
sealed interface EstadoSincronizacion {
    /** Nunca se pidió, o se canceló. */
    data object Inactiva : EstadoSincronizacion

    /** En cola: esperando conexión o su turno. */
    data object EnEspera : EstadoSincronizacion

    /** Falló por red o error 5xx y WorkManager la reintentará con backoff exponencial. */
    data object Reintentando : EstadoSincronizacion

    data object EnCurso : EstadoSincronizacion

    /**
     * @param enRevision operaciones que el servidor rechazó (4xx): no se reintentan y quedan
     * marcadas para revisión.
     * @param ronda identifica la ejecución, para no confundir su resultado con el de otra anterior.
     */
    data class Completada(
        val enviadas: Int,
        val recibidas: Int,
        val enRevision: Int,
        val ronda: String = ""
    ) : EstadoSincronizacion

    data class Fallida(val mensaje: String, val ronda: String = "") : EstadoSincronizacion

    /** `true` mientras la sincronización todavía no terminó. */
    val enProgreso: Boolean
        get() = this is EnEspera || this is Reintentando || this is EnCurso
}
