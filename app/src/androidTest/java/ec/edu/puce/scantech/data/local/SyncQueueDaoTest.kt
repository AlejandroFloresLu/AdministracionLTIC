package ec.edu.puce.scantech.data.local

import androidx.test.platform.app.InstrumentationRegistry
import ec.edu.puce.scantech.data.local.dao.SyncQueueDao
import ec.edu.puce.scantech.data.local.entity.SyncOperationEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncQueueDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: SyncQueueDao

    @Before
    fun crearBaseEnMemoria() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        db = AppDatabase.enMemoria(contexto)
        dao = db.syncQueueDao()
    }

    @After
    fun cerrarBase() {
        db.close()
    }

    @Test
    fun listarPendientesRespetaElOrdenDeCreacion() = runBlocking {
        dao.insertar(operacion(asset = "B", creada = 2_000L))
        dao.insertar(operacion(asset = "A", creada = 1_000L))
        dao.insertar(operacion(asset = "C", creada = 3_000L))

        val pendientes = dao.listarPendientes()

        assertEquals(listOf("A", "B", "C"), pendientes.map { it.assetId })
    }

    @Test
    fun sumarIntentoIncrementaElContador() = runBlocking {
        val id = dao.insertar(operacion(asset = "A", creada = 1_000L))

        dao.sumarIntento(id)
        dao.sumarIntento(id)

        assertEquals(2, dao.listarPendientes().single().intentos)
    }

    @Test
    fun borrarConfirmadasEliminaSoloLasIndicadas() = runBlocking {
        val idA = dao.insertar(operacion(asset = "A", creada = 1_000L))
        val idB = dao.insertar(operacion(asset = "B", creada = 2_000L))

        dao.borrarConfirmadas(listOf(idA))

        val restantes = dao.listarPendientes()
        assertEquals(1, restantes.size)
        assertTrue(restantes.single().id == idB)
    }

    private fun operacion(asset: String, creada: Long) = SyncOperationEntity(
        assetId = asset,
        tipo = SyncStatus.PENDING_CREATE,
        payload = "{}",
        createdAt = creada
    )
}
