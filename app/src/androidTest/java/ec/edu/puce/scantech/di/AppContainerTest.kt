package ec.edu.puce.scantech.di

import androidx.test.platform.app.InstrumentationRegistry
import ec.edu.puce.scantech.ScanTechApp
import ec.edu.puce.scantech.data.local.AppDatabase
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppContainerTest {

    private val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test
    fun laAppRegistradaEnElManifiestoEsScanTechAppYTieneContenedor() {
        assertTrue("android:name debe apuntar a ScanTechApp", app is ScanTechApp)
        assertTrue((app as ScanTechApp).container is DefaultAppContainer)
    }

    @Test
    fun elContenedorCreaCadaDependenciaUnaSolaVez() {
        val contenedor = (app as ScanTechApp).container as DefaultAppContainer

        assertSame(contenedor.assetRepository, contenedor.assetRepository)
        assertSame(contenedor.assetApi, contenedor.assetApi)
        assertSame(contenedor.database, contenedor.database)
        // y es la misma base que el singleton de Room, no una segunda conexión
        assertSame(AppDatabase.obtener(app), contenedor.database)
    }
}
