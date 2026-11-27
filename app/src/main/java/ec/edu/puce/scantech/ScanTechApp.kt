package ec.edu.puce.scantech

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import ec.edu.puce.scantech.di.AppContainer
import ec.edu.puce.scantech.data.sync.SyncWorker
import ec.edu.puce.scantech.di.DefaultAppContainer

/**
 * Clase Application de ScanTech (registrada en `AndroidManifest.xml` con `android:name`).
 *
 * Crea el [AppContainer] al arrancar el proceso; las factories de los ViewModels lo leen
 * desde aquí para obtener las dependencias compartidas.
 */
class ScanTechApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        // Sube pendientes y baja cambios del servidor cada 15 min cuando hay red
        SyncWorker.programar(this)
    }
}

/**
 * Atajo para las factories de ViewModel: obtiene el [AppContainer] a partir de los
 * [CreationExtras] que entrega el sistema al crear el ViewModel.
 */
fun CreationExtras.appContainer(): AppContainer =
    (checkNotNull(this[APPLICATION_KEY]) { "Falta APPLICATION_KEY en CreationExtras" } as ScanTechApp).container
