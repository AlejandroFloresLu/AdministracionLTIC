package ec.edu.puce.scantech.data.mapper

import ec.edu.puce.scantech.data.local.SyncStatus
import ec.edu.puce.scantech.data.model.ActivoLTIC
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ActivoLTICMapperTest {

    private val activo = ActivoLTIC(
        id = "LTIC-000123",
        qrCode = "LTIC-000123",
        nombre = "Servidor HP ProLiant DL360 Gen10",
        serie = "XYZ987SER",
        categoria = "Servidores",
        marca = "HP",
        modelo = "ProLiant DL360 Gen10",
        ram = "64GB ECC",
        cpu = "Dual Intel Xeon Silver 4210R",
        color = "Negro",
        dimensiones = "43.5 × 70.7 × 4.3 cm",
        bloque = "Data Center",
        estante = "Rack 04",
        casillero = "U12",
        unidades = 2,
        observaciones = "Fuente descalibrada",
        syncStatus = SyncStatus.PENDING_UPDATE
    )

    @Test
    fun copiaTodosLosCamposDeLaFicha() {
        val asset = activo.toAsset()

        assertEquals("LTIC-000123", asset.id)
        assertEquals("LTIC-000123", asset.qrCode)
        assertEquals("Servidor HP ProLiant DL360 Gen10", asset.name)
        assertEquals("Servidores", asset.category)
        assertEquals("Negro", asset.color)
        assertEquals("43.5 × 70.7 × 4.3 cm", asset.dimensions)
        assertEquals("Data Center", asset.block)
        assertEquals("Rack 04", asset.shelf)
        assertEquals("U12", asset.locker)
        assertEquals(2, asset.units)
        assertEquals("Fuente descalibrada", asset.observations)
        assertEquals(SyncStatus.PENDING_UPDATE, asset.syncStatus)
    }

    @Test
    fun agrupaMarcaModeloSerieRamYCpuEnEspecificaciones() {
        val esperado = "Marca: HP\nModelo: ProLiant DL360 Gen10\nSerie: XYZ987SER\n" +
            "RAM: 64GB ECC\nCPU: Dual Intel Xeon Silver 4210R"

        assertEquals(esperado, activo.toAsset().technicalSpecs)
    }

    @Test
    fun omiteRamYCpuCuandoNoAplicanYObservacionVaciaEsNull() {
        val asset = activo.copy(ram = "N/A", cpu = "N/A", observaciones = "  ").toAsset()

        assertFalse(asset.technicalSpecs.contains("RAM"))
        assertFalse(asset.technicalSpecs.contains("CPU"))
        assertNull(asset.observations)
    }
}
