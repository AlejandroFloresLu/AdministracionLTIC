package ec.edu.puce.scantech.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.test.platform.app.InstrumentationRegistry
import ec.edu.puce.scantech.data.local.dao.AssetDao
import ec.edu.puce.scantech.data.local.dao.SyncQueueDao
import ec.edu.puce.scantech.data.local.entity.AssetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AssetDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AssetDao
    private lateinit var cola: SyncQueueDao

    @Before
    fun crearBaseEnMemoria() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        db = AppDatabase.enMemoria(contexto)
        dao = db.assetDao()
        cola = db.syncQueueDao()
    }

    @After
    fun cerrarBase() {
        db.close()
    }

    @Test
    fun observeAssetsFiltraPorTextoYCategoria() = runBlocking {
        dao.insert(activo(id = "1", qr = "LTIC-000001", nombre = "Mouse Logitech", categoria = "Mouse"))
        dao.insert(activo(id = "2", qr = "LTIC-000002", nombre = "Teclado Logitech", categoria = "Teclado"))
        dao.insert(activo(id = "3", qr = "LTIC-000003", nombre = "Monitor Dell", categoria = "Monitor"))

        assertEquals(3, dao.observeAssets("", null).first().size)
        assertEquals(listOf("1", "2"), dao.observeAssets("logitech", null).first().map { it.id })
        assertEquals(listOf("2"), dao.observeAssets("logitech", "teclado").first().map { it.id })
        assertEquals(listOf("3"), dao.observeAssets("000003", null).first().map { it.id })
    }

    @Test
    fun laBusquedaTrataPorcentajeYGuionBajoComoTextoLiteral() = runBlocking {
        dao.insert(activo(id = "1", qr = "LTIC-000001", nombre = "Cable 100%"))
        dao.insert(activo(id = "2", qr = "LTIC-000002", nombre = "Cable 1000"))

        val resultado = dao.observeAssets(AssetDao.escaparLike("100%"), null).first()

        assertEquals(listOf("1"), resultado.map { it.id })
    }

    @Test
    fun getByQrCodeYObserveByIdIgnoranBorradosLogicos() = runBlocking {
        dao.insert(activo(id = "1", qr = "LTIC-000001", nombre = "A"))
        assertNotNull(dao.getByQrCode("LTIC-000001"))
        assertNotNull(dao.observeById("1").first())

        dao.markDeleted("1", deletedAt = 5_000L)

        assertNull(dao.getByQrCode("LTIC-000001"))
        assertNull(dao.observeById("1").first())
        assertTrue(dao.observeAssets("", null).first().isEmpty())
        // pero el QR sigue ocupado hasta que el servidor confirme la baja
        assertTrue(dao.existsQrCode("LTIC-000001", null))
    }

    @Test(expected = SQLiteConstraintException::class)
    fun elIndiceUnicoRechazaQrDuplicado() = runBlocking {
        dao.insert(activo(id = "1", qr = "LTIC-000001", nombre = "A"))
        dao.insert(activo(id = "2", qr = "LTIC-000001", nombre = "B"))
    }

    @Test
    fun insertWithQueueGuardaActivoYOperacionJuntos() = runBlocking {
        dao.insertWithQueue(activo(id = "1", qr = "LTIC-000001", nombre = "A"), payload = "{}")

        assertEquals(SyncStatus.PENDING_CREATE, dao.getById("1")?.syncStatus)
        val pendientes = cola.listarPendientes()
        assertEquals(1, pendientes.size)
        assertEquals(SyncStatus.PENDING_CREATE, pendientes.single().tipo)
    }

    @Test
    fun editarVariasVecesSinConexionEncolaCadaCambioEnOrden() = runBlocking {
        dao.insertWithQueue(activo(id = "1", qr = "LTIC-000001", nombre = "A", actualizado = 1_000L), payload = "v1")
        dao.updateWithQueue(activo(id = "1", qr = "LTIC-000001", nombre = "A2", actualizado = 2_000L), payload = "v2")
        dao.updateWithQueue(activo(id = "1", qr = "LTIC-000001", nombre = "A3", actualizado = 3_000L), payload = "v3")

        val ops = cola.listarPendientes()
        assertEquals(listOf("v1", "v2", "v3"), ops.map { it.payload })
        assertEquals(
            listOf(SyncStatus.PENDING_CREATE, SyncStatus.PENDING_UPDATE, SyncStatus.PENDING_UPDATE),
            ops.map { it.tipo }
        )
        // el servidor aún no conoce el activo: sigue como alta pendiente
        assertEquals(SyncStatus.PENDING_CREATE, dao.getById("1")?.syncStatus)
    }

    @Test
    fun editarUnActivoSincronizadoEncolaUpdate() = runBlocking {
        dao.insert(activo(id = "1", qr = "LTIC-000001", nombre = "A", estado = SyncStatus.SYNCED))

        dao.updateWithQueue(activo(id = "1", qr = "LTIC-000001", nombre = "A2"), payload = "p")

        assertEquals(SyncStatus.PENDING_UPDATE, dao.getById("1")?.syncStatus)
        assertEquals(SyncStatus.PENDING_UPDATE, cola.listarPendientes().single().tipo)
    }

    @Test
    fun eliminarUnAltaNoSubidaTambienEsBorradoLogico() = runBlocking {
        dao.insertWithQueue(activo(id = "1", qr = "LTIC-000001", nombre = "A"), payload = "p")

        assertTrue(dao.deleteWithQueue("1", deletedAt = 9_000L, payload = "p"))

        assertNull(dao.observeById("1").first())
        assertEquals(9_000L, dao.getById("1")?.deletedAt)
        assertEquals(
            listOf(SyncStatus.PENDING_CREATE, SyncStatus.PENDING_DELETE),
            cola.listarPendientes().map { it.tipo }
        )
    }

    @Test
    fun eliminarUnActivoSincronizadoLoMarcaYEncolaLaBaja() = runBlocking {
        dao.insert(activo(id = "1", qr = "LTIC-000001", nombre = "A", estado = SyncStatus.SYNCED))

        assertTrue(dao.deleteWithQueue("1", deletedAt = 9_000L, payload = "p"))

        val fila = dao.getById("1")
        assertEquals(9_000L, fila?.deletedAt)
        assertEquals(SyncStatus.PENDING_DELETE, cola.listarPendientes().single().tipo)
        assertFalse(dao.deleteWithQueue("1", deletedAt = 9_500L, payload = "p"))
    }

    @Test
    fun confirmarOperacionesCierraActivosYPurgaBajas() = runBlocking {
        dao.insertWithQueue(activo(id = "1", qr = "LTIC-000001", nombre = "A"), payload = "p")
        dao.insert(activo(id = "2", qr = "LTIC-000002", nombre = "B", estado = SyncStatus.SYNCED))
        dao.deleteWithQueue("2", deletedAt = 9_000L, payload = "p")

        dao.confirmOperations(cola.listarPendientes())

        assertEquals(SyncStatus.SYNCED, dao.getById("1")?.syncStatus)
        assertNull(dao.getById("2"))
        assertTrue(cola.listarPendientes().isEmpty())
    }

    @Test
    fun confirmarSoloElAltaDejaPendienteLaEdicionPosterior() = runBlocking {
        dao.insertWithQueue(activo(id = "1", qr = "LTIC-000001", nombre = "A"), payload = "v1")
        dao.updateWithQueue(activo(id = "1", qr = "LTIC-000001", nombre = "A2"), payload = "v2")

        dao.confirmOperations(cola.listarPendientes().take(1))

        assertEquals(SyncStatus.PENDING_CREATE, dao.getById("1")?.syncStatus)
        assertEquals(1, cola.listarPendientes().size)
    }

    @Test
    fun cambiosRemotosRespetanLaPoliticaDeConflictos() = runBlocking {
        // local pendiente más reciente que el servidor -> gana local
        dao.insertWithQueue(activo(id = "1", qr = "LTIC-000001", nombre = "Local", actualizado = 2_000L), payload = "p")
        // local sincronizado -> el servidor lo reemplaza
        dao.insert(activo(id = "2", qr = "LTIC-000002", nombre = "Viejo", estado = SyncStatus.SYNCED, actualizado = 1_000L))

        val revisar = dao.applyRemoteChanges(
            listOf(
                activo(id = "1", qr = "LTIC-000001", nombre = "Servidor", estado = SyncStatus.SYNCED, actualizado = 1_500L),
                activo(id = "2", qr = "LTIC-000002", nombre = "Nuevo", estado = SyncStatus.SYNCED, actualizado = 3_000L),
                activo(id = "3", qr = "LTIC-000003", nombre = "Otro", estado = SyncStatus.SYNCED, actualizado = 3_000L)
            )
        )

        assertTrue(revisar.isEmpty())
        assertEquals("Local", dao.getById("1")?.name)
        assertEquals("Nuevo", dao.getById("2")?.name)
        assertEquals("Otro", dao.getById("3")?.name)
        assertEquals(3_000L, dao.lastSyncedUpdatedAt())
    }

    @Test
    fun cambioRemotoConQrDeOtroActivoSeMarcaParaRevision() = runBlocking {
        dao.insert(activo(id = "1", qr = "LTIC-000001", nombre = "A", estado = SyncStatus.SYNCED))

        val revisar = dao.applyRemoteChanges(
            listOf(activo(id = "2", qr = "LTIC-000001", nombre = "B", estado = SyncStatus.SYNCED))
        )

        assertEquals(listOf("2"), revisar)
        assertNull(dao.getById("2"))
    }

    private fun activo(
        id: String,
        qr: String,
        nombre: String,
        categoria: String = "Mouse",
        estado: SyncStatus = SyncStatus.PENDING_CREATE,
        actualizado: Long = 1_000L
    ) = AssetEntity(
        id = id,
        qrCode = qr,
        name = nombre,
        category = categoria,
        technicalSpecs = "",
        block = "A",
        shelf = "1",
        locker = "1",
        units = 1,
        syncStatus = estado,
        updatedAt = actualizado
    )
}
