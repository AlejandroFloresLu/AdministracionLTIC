package ec.edu.puce.scantech.data.repository

import androidx.test.platform.app.InstrumentationRegistry
import ec.edu.puce.scantech.data.local.AppDatabase
import ec.edu.puce.scantech.data.local.SyncStatus
import ec.edu.puce.scantech.data.remote.AssetApi
import ec.edu.puce.scantech.data.remote.AssetDto
import ec.edu.puce.scantech.data.remote.CategoryDto
import ec.edu.puce.scantech.data.remote.SyncBatchRequest
import ec.edu.puce.scantech.data.remote.SyncBatchResponse
import ec.edu.puce.scantech.data.remote.SyncResultDto
import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.model.Asset
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import ec.edu.puce.scantech.domain.sync.EstadoSincronizacion
import ec.edu.puce.scantech.domain.sync.ProgramadorSync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

class AssetRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var api: ApiFalsa
    private lateinit var repo: AssetRepositoryImpl
    private lateinit var programador: ProgramadorContador

    @Before
    fun preparar() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        db = AppDatabase.enMemoria(contexto)
        api = ApiFalsa()
        programador = ProgramadorContador()
        repo = AssetRepositoryImpl(db.assetDao(), db.syncQueueDao(), api, clock = { 10_000L }, programador = programador)
    }

    @After
    fun cerrar() {
        db.close()
    }

    @Test
    fun crearGuardaLocalmenteYEncolaSinTocarLaRed() = runBlocking {
        val resultado = repo.create(nuevo("LTIC-000001"))

        assertTrue(resultado is Result.Success)
        assertEquals(1, repo.observePendingCount().first())
        assertEquals(0, api.lotes.size)
        assertEquals(10_000L, (resultado as Result.Success).data.updatedAt)
    }

    @Test
    fun crearConQrDuplicadoDevuelveError() = runBlocking {
        repo.create(nuevo("LTIC-000001"))

        val resultado = repo.create(nuevo("LTIC-000001"))

        assertTrue(resultado is Result.Error)
        assertEquals(1, repo.observePendingCount().first())
    }

    @Test
    fun obtenerPorQrInexistenteDevuelveError() = runBlocking {
        assertTrue(repo.getByQrCode("LTIC-999999") is Result.Error)
    }

    @Test
    fun eliminarOcultaElActivoYEncolaLaBaja() = runBlocking {
        val activo = (repo.create(nuevo("LTIC-000001")) as Result.Success).data

        assertTrue(repo.delete(activo.id) is Result.Success)

        assertNull(repo.observeById(activo.id).first())
        assertEquals(2, repo.observePendingCount().first())
    }

    @Test
    fun syncSubeLaColaYMarcaComoSincronizado() = runBlocking {
        val activo = (repo.create(nuevo("LTIC-000001")) as Result.Success).data

        val resultado = repo.sync()

        assertTrue(resultado is Result.Success)
        assertEquals(1, (resultado as Result.Success).data.sent)
        assertEquals(0, repo.observePendingCount().first())
        assertEquals(SyncStatus.SYNCED, repo.observeById(activo.id).first()?.syncStatus)
        assertEquals("PENDING_CREATE", api.lotes.single().operaciones.single().tipo)
    }

    @Test
    fun syncConOperacionRechazadaLaMarcaParaRevisionYNoLaReintenta() = runBlocking {
        repo.create(nuevo("LTIC-000001"))
        api.aceptar = false
        api.motivo = "QR ya registrado en el servidor"

        val resultado = repo.sync() as Result.Success

        assertEquals(1, resultado.data.rejected.size)
        val op = db.syncQueueDao().listarPendientes().single()
        assertTrue(op.enRevision)
        assertEquals("QR ya registrado en el servidor", op.motivoRevision)

        // La siguiente ronda ya no la envía
        api.aceptar = true
        repo.sync()
        assertEquals(1, api.lotes.size)
        assertEquals(1, repo.observePendingCount().first())
    }

    @Test
    fun unErrorHttp4xxAlSubirMarcaElLoteParaRevisionYNoEsUnFallo() = runBlocking {
        repo.create(nuevo("LTIC-000001"))
        repo.create(nuevo("LTIC-000002"))
        api.codigoHttp = 400

        val resultado = repo.sync()

        assertTrue("un 4xx no se reintenta: la ronda termina", resultado is Result.Success)
        assertEquals(2, (resultado as Result.Success).data.rejected.size)
        assertTrue(db.syncQueueDao().listarPendientes().all { it.enRevision })
        assertTrue(db.syncQueueDao().listarParaEnviar().isEmpty())
    }

    @Test
    fun unErrorHttp5xxAlSubirDevuelveErrorParaReintentarYNoMarcaNada() = runBlocking {
        repo.create(nuevo("LTIC-000001"))
        api.codigoHttp = 503

        val resultado = repo.sync()

        assertTrue(resultado is Result.Error)
        assertEquals(503, ((resultado as Result.Error).cause as HttpException).code())
        assertTrue(db.syncQueueDao().listarPendientes().none { it.enRevision })
    }

    @Test
    fun lasOperacionesPosterioresDeUnActivoEnRevisionNoSeEnvian() = runBlocking {
        val activo = (repo.create(nuevo("LTIC-000001")) as Result.Success).data
        api.aceptar = false
        repo.sync()

        repo.update(activo.copy(name = "Editado"))
        repo.create(nuevo("LTIC-000002"))
        api.aceptar = true
        repo.sync()

        val enviadas = api.lotes.last().operaciones
        assertEquals(listOf("LTIC-000002"), enviadas.map { it.asset.qrCode })
    }

    @Test
    fun cadaEscrituraLocalDisparaLaSincronizacionAlGuardar() = runBlocking {
        val activo = (repo.create(nuevo("LTIC-000001")) as Result.Success).data
        repo.update(activo.copy(name = "Editado"))
        repo.updateObservations(activo.id, "Rayado")
        repo.delete(activo.id)

        assertEquals(4, programador.trasGuardar)
    }

    @Test
    fun unaEscrituraFallidaNoDisparaLaSincronizacion() = runBlocking {
        repo.create(nuevo("LTIC-000001"))
        programador.trasGuardar = 0

        repo.create(nuevo("LTIC-000001"))
        repo.delete("no-existe")

        assertEquals(0, programador.trasGuardar)
    }

    @Test
    fun syncEnviaLaColaEnLotesDe50() = runBlocking {
        repeat(120) { repo.create(nuevo("LTIC-%06d".format(it + 1))) }

        val resultado = repo.sync() as Result.Success

        assertEquals(120, resultado.data.sent)
        assertEquals(listOf(50, 50, 20), api.lotes.map { it.operaciones.size })
        assertEquals(0, repo.observePendingCount().first())
    }

    @Test
    fun syncSinConexionDevuelveErrorYConservaLosDatos() = runBlocking {
        repo.create(nuevo("LTIC-000001"))
        api.sinRed = true

        assertTrue(repo.sync() is Result.Error)
        assertEquals(1, repo.observePendingCount().first())
    }

    @Test
    fun syncIncorporaActivosDescargadosDelServidor() = runBlocking {
        api.remotos = listOf(
            AssetDto(id = "r1", qrCode = "LTIC-000050", name = "Remoto", category = "Mouse", units = 3, updatedAt = 5_000L)
        )

        repo.sync()

        val activo = (repo.getByQrCode("LTIC-000050") as Result.Success).data
        assertEquals("Remoto", activo.name)
        assertEquals(SyncStatus.SYNCED, activo.syncStatus)
    }

    @Test
    fun laSegundaSyncPideSoloLosCambiosPosterioresAlCursorGuardado() = runBlocking {
        api.remotos = listOf(
            AssetDto(id = "r1", qrCode = "LTIC-000050", name = "Remoto", category = "Mouse", updatedAt = 5_000L),
            AssetDto(id = "r2", qrCode = "LTIC-000051", name = "Remoto 2", category = "Mouse", updatedAt = 7_000L)
        )
        repo.sync()

        api.remotos = emptyList()
        repo.sync()

        assertEquals(listOf(0L, 7_000L), api.pedidosDesde)
    }

    @Test
    fun siLaBajadaFallaElCursorNoAvanza() = runBlocking {
        api.remotos = listOf(
            AssetDto(id = "r1", qrCode = "LTIC-000050", name = "Remoto", category = "Mouse", updatedAt = 5_000L)
        )
        api.sinRed = true
        repo.sync()

        assertEquals(0L, db.assetDao().lastSyncTimestamp())
    }

    @Test
    fun observeAssetsBuscaYFiltraPorCategoria() = runBlocking {
        repo.create(nuevo("LTIC-000001", nombre = "Mouse Gamer", categoria = "Mouse"))
        repo.create(nuevo("LTIC-000002", nombre = "Teclado Gamer", categoria = "Teclado"))

        assertEquals(2, repo.observeAssets("gamer").first().size)
        assertEquals(listOf("LTIC-000002"), repo.observeAssets("gamer", "Teclado").first().map { it.qrCode })
    }

    @Test
    fun guardarObservacionesActualizaEncolaYNoTocaLaRed() = runBlocking {
        val creado = (repo.create(nuevo("LTIC-000001")) as Result.Success).data

        val resultado = repo.updateObservations(creado.id, "  Teclas flojas  ")

        assertTrue(resultado is Result.Success)
        val guardado = (resultado as Result.Success).data
        assertEquals("Teclas flojas", guardado.observations)
        assertEquals(10_000L, guardado.updatedAt)
        // el alta todavía no se subió: se mantiene PENDING_CREATE
        assertEquals(SyncStatus.PENDING_CREATE, guardado.syncStatus)
        assertEquals("Teclas flojas", repo.observeById(creado.id).first()?.observations)
        assertEquals(2, repo.observePendingCount().first())
        assertEquals(0, api.lotes.size)
    }

    @Test
    fun guardarObservacionesEnBlancoLasBorra() = runBlocking {
        val creado = (repo.create(nuevo("LTIC-000001").copy(observations = "vieja")) as Result.Success).data

        repo.updateObservations(creado.id, "   ")

        assertNull(repo.observeById(creado.id).first()?.observations)
    }

    @Test
    fun guardarObservacionesDeUnActivoInexistenteDevuelveError() = runBlocking {
        assertTrue(repo.updateObservations("no-existe", "x") is Result.Error)
        assertEquals(0, repo.observePendingCount().first())
    }

    @Test
    fun observeAssetsSeActualizaSoloCuandoCambiaLaBase() = runBlocking {
        val catalogo = repo.observeAssets("dell", "Laptops")
        assertTrue(catalogo.first().isEmpty())

        // Se suscribe antes de escribir: Room debe volver a emitir por sí solo.
        val siguiente = async { withTimeout(5_000) { catalogo.first { it.isNotEmpty() } } }
        repo.create(nuevo("LTIC-000001", nombre = "Laptop Dell", categoria = "Laptops"))
        repo.create(nuevo("LTIC-000002", nombre = "Mouse Dell", categoria = "Mouse"))

        assertEquals(listOf("LTIC-000001"), siguiente.await().map { it.qrCode })

        val creado = catalogo.first().single()
        repo.delete(creado.id)
        assertTrue(withTimeout(5_000) { catalogo.first { it.isEmpty() } }.isEmpty())
    }

    private fun nuevo(qr: String, nombre: String = "Activo", categoria: String = "Mouse") = Asset(
        qrCode = qr,
        name = nombre,
        category = categoria,
        technicalSpecs = "",
        block = "A",
        shelf = "1",
        locker = "1",
        units = 1
    )

    private class ApiFalsa : AssetApi {
        val lotes = mutableListOf<SyncBatchRequest>()
        var aceptar = true
        var motivo: String? = null
        var codigoHttp: Int? = null
        var sinRed = false
        var remotos: List<AssetDto> = emptyList()
        val pedidosDesde = mutableListOf<Long>()

        override suspend fun sincronizar(lote: SyncBatchRequest): SyncBatchResponse {
            if (sinRed) throw IOException("sin red")
            codigoHttp?.let { throw HttpException(Response.error<Any>(it, "".toResponseBody())) }
            lotes += lote
            return SyncBatchResponse(lote.operaciones.map { SyncResultDto(it.opId, aceptar, if (aceptar) null else motivo) })
        }

        override suspend fun obtenerCambios(updatedSince: Long): List<AssetDto> {
            if (sinRed) throw IOException("sin red")
            pedidosDesde += updatedSince
            return remotos
        }

        override suspend fun obtenerCategorias(): List<CategoryDto> = emptyList()
    }

    private class ProgramadorContador : ProgramadorSync {
        var trasGuardar = 0

        override fun programarPeriodica() = Unit
        override fun programarTrasGuardar() {
            trasGuardar++
        }
        override fun sincronizarAhora() = Unit
        override fun observarSincronizacionManual(): Flow<EstadoSincronizacion> =
            flowOf(EstadoSincronizacion.Inactiva)
    }
}
