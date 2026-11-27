package ec.edu.puce.scantech.di

import ec.edu.puce.scantech.data.network.ConexionObserver
import ec.edu.puce.scantech.domain.sync.MonitorConectividad
import android.content.Context
import ec.edu.puce.scantech.data.local.AppDatabase
import ec.edu.puce.scantech.data.remote.AssetApi
import ec.edu.puce.scantech.data.remote.RetrofitClient
import ec.edu.puce.scantech.data.repository.AssetRepositoryImpl
import ec.edu.puce.scantech.data.sync.ProgramadorSyncWorkManager
import ec.edu.puce.scantech.domain.repository.AssetRepository
import ec.edu.puce.scantech.domain.sync.ProgramadorSync

/**
 * Contenedor de dependencias de la app (inyección de dependencias manual).
 *
 * Los ViewModels dependen solo de [AssetRepository] (una interfaz del dominio) a través de sus
 * casos de uso; no saben que detrás hay Room y Retrofit. Así se cumple la inversión de
 * dependencias sin necesidad de Hilt.
 */
interface AppContainer {
    val assetRepository: AssetRepository
    val programadorSync: ProgramadorSync
    val monitorConectividad: MonitorConectividad
}

/**
 * Implementación real: crea [AppDatabase], [AssetApi] y [AssetRepository] **una sola vez**
 * (`by lazy`, que es seguro entre hilos) y los comparte con todos los ViewModels.
 *
 * Vive en [ec.edu.puce.scantech.ScanTechApp], así que dura lo mismo que el proceso.
 */
class DefaultAppContainer(context: Context) : AppContainer {

    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.obtener(appContext) }

    val assetApi: AssetApi by lazy { RetrofitClient.crearAssetApi(BASE_URL) }

    override val programadorSync: ProgramadorSync by lazy { ProgramadorSyncWorkManager(appContext) }

    override val monitorConectividad: MonitorConectividad by lazy { ConexionObserver(appContext) }

    override val assetRepository: AssetRepository by lazy {
        AssetRepositoryImpl(
            assetDao = database.assetDao(),
            syncQueueDao = database.syncQueueDao(),
            api = assetApi,
            programador = programadorSync
        )
    }

    companion object {
        /** URL base de la API REST de ScanTech (debe terminar en `/`). */
        const val BASE_URL = "https://api.scantech.puce.edu.ec/"
    }
}
