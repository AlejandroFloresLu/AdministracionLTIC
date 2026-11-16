package ec.edu.puce.scantech.domain

/**
 * Resultado de una operación del repositorio. Los ViewModels lo consumen con `when`
 * exhaustivo en lugar de capturar excepciones.
 *
 * Nota: comparte nombre con `kotlin.Result` a propósito (pedido del equipo); al importar
 * `ec.edu.puce.scantech.domain.Result` el import explícito tiene prioridad.
 */
sealed interface Result<out T> {

    data class Success<out T>(val data: T) : Result<T>

    /** [message] está pensado para mostrarse al usuario; [cause] es para logs. */
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>
}

/** Transforma el dato de un [Result.Success] y deja pasar un [Result.Error] sin cambios. */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
}
