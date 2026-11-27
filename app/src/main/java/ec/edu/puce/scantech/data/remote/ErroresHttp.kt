package ec.edu.puce.scantech.data.remote

/**
 * Clasificación de errores HTTP para la sincronización (Estrategia de Datos §4).
 *
 * - 4xx: el servidor rechazó los datos; reintentar no lo arregla, se marca para revisión.
 *   Excepciones: 408 (timeout) y 429 (demasiadas peticiones) son temporales.
 * - 5xx: falla del servidor; se reintenta con backoff exponencial.
 */
object ErroresHttp {
    fun esRechazoDefinitivo(codigo: Int): Boolean = codigo in 400..499 && codigo != 408 && codigo != 429

    fun esTemporal(codigo: Int): Boolean = codigo >= 500 || codigo == 408 || codigo == 429
}
