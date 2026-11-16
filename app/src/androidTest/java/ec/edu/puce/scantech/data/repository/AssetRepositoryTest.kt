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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class AssetRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var api: ApiFalsa
    private lateinit var repo: AssetRepositoryImpl

    @Before
    fun preparar() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        db = AppDatabase.enMemoria(contexto)
        api = ApiFalsa()
        repo = AssetRepositoryImpl(db.assetDao(), db.syncQueueDao(), api, clock = { 10_000L })
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
    fun eliminarUnActivoRecienCreadoLoQuitaTodoDeLaCola() = runBlocking {
        val activo = (repo.create(nuevo("LTIC-000001")) as Result.Success).data

        assertTrue(repo.delete(activo.id) is Result.Success)

        assertNull(repo.observeById(activo.id).first())
        assertEquals(0, repo.observePendingCount().first())
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
    fun syncConOperacionRechazadaLaDejaEnColaYSumaIntento() = runBlocking {
        repo.create(nuevo("LTIC-000001"))
        api.aceptar = false

        val resultado = repo.sync() as Result.Success

        assertEquals(1, resultado.data.rejected)
        assertEquals(1, repo.observePendingCount().first())
        assertEquals(1, db.syncQueueDao().listarPendientes().single().intentos)
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
    fun observeAssetsBuscaYFiltraPorCategoria() = runBlocking {
        repo.create(nuevo("LTIC-000001", nombre = "Mouse Gamer", categoria = "Mouse"))
        repo.create(nuevo("LTIC-000002", nombre = "Teclado Gamer", categoria = "Teclado"))

        assertEquals(2, repo.observeAssets("gamer").first().size)
        assertEquals(listOf("LTIC-000002"), repo.observeAssets("gamer", "Teclado").first().map { it.qrCode })
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
        var sinRed = false
        var remotos: List<AssetDto> = emptyList()

        override suspend fun sincronizar(lote: SyncBatchRequest): SyncBatchResponse {
            if (sinRed) throw IOException("sin red")
            lotes += lote
            return SyncBatchResponse(lote.operaciones.map { SyncResultDto(it.opId, aceptar) })
        }

        override suspend fun obtenerCambios(updatedSince: Long): List<AssetDto> {
            if (sinRed) throw IOException("sin red")
            return remotos
        }

        override suspend fun obtenerCategorias(): List<CategoryDto> = emptyList()
    }
}
