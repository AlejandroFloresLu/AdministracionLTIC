package ec.edu.puce.scantech.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Fábrica de la API REST con JSON basado en kotlinx.serialization.
 *
 * `ignoreUnknownKeys` evita que un campo nuevo del servidor rompa la app, y los tiempos de
 * espera están acotados para no dejar corrutinas colgadas en zonas con mala señal.
 */
object RetrofitClient {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val clienteHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** Crea el cliente de [AssetApi] apuntando a [baseUrl], que debe terminar en `/`. */
    fun crearAssetApi(baseUrl: String): AssetApi {
        require(baseUrl.endsWith("/")) { "La URL base debe terminar en '/'" }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clienteHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AssetApi::class.java)
    }
}
