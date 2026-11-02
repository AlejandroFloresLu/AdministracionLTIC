package ec.edu.puce.scantech.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Contrato de la API REST de ScanTech LTIC. Todas las funciones son `suspend` y se
 * ejecutan en Dispatchers.IO desde el repositorio o el worker de sincronización.
 */
interface AssetApi {

    /** Envía un lote de operaciones; la respuesta trae el resultado de cada registro. */
    @POST("assets/sync")
    suspend fun sincronizar(@Body lote: SyncBatchRequest): SyncBatchResponse

    /** Descarga los activos modificados después de [updatedSince] (epoch en milisegundos). */
    @GET("assets")
    suspend fun obtenerCambios(@Query("updatedSince") updatedSince: Long): List<AssetDto>

    /** Descarga las categorías controladas. */
    @GET("categories")
    suspend fun obtenerCategorias(): List<CategoryDto>
}
