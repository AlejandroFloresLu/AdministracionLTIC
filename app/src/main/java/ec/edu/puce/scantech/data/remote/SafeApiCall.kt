package ec.edu.puce.scantech.data.remote

import ec.edu.puce.scantech.domain.Result
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/**
 * [Caso de práctica 7: Envoltura segura de llamadas HTTP (safeApiCall)]
 *
 * Ejecuta [llamada] y clasifica cualquier fallo en un [Result.Error] con un mensaje listo para
 * la UI, en lugar de dejar que la excepción cierre la app:
 * - [IOException]: no hay conexión física (modo avión, servidor inalcanzable, timeout).
 * - [HttpException]: el servidor respondió, pero con un código 4xx/5xx.
 * - Cualquier otra excepción: error inesperado.
 *
 * [CancellationException] se relanza siempre: si el `viewModelScope` se cancela, la corrutina
 * debe terminar y no debe tratarse como un error de red.
 */
suspend fun <T> safeApiCall(llamada: suspend () -> T): Result<T> {
    return try {
        Result.Success(llamada())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.Error("Sin conexión con el servidor. Los cambios se enviarán más tarde.", e)
    } catch (e: HttpException) {
        val tipo = if (e.code() in 400..499) "la solicitud" else "el servidor"
        Result.Error("Error HTTP ${e.code()}: falló $tipo.", e)
    } catch (e: Exception) {
        Result.Error("No se pudo sincronizar: ${e.message ?: "error desconocido"}", e)
    }
}
