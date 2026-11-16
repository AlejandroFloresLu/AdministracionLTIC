package ec.edu.puce.scantech.data.mapper

import ec.edu.puce.scantech.data.local.SyncStatus
import ec.edu.puce.scantech.data.local.entity.AssetEntity
import ec.edu.puce.scantech.data.remote.AssetDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssetMappersTest {

    private val entidad = AssetEntity(
        id = "uuid-1",
        qrCode = "LTIC-000123",
        name = "Mouse Logitech",
        category = "Mouse",
        technicalSpecs = "USB, 1600 DPI",
        color = "Negro",
        dimensions = "10x6x4 cm",
        block = "A",
        shelf = "3",
        locker = "2",
        units = 5,
        observations = "Sin caja",
        syncStatus = SyncStatus.PENDING_UPDATE,
        updatedAt = 1_700_000_000_000L,
        deletedAt = null
    )

    @Test
    fun entityADominioYDeVueltaConservaTodosLosCampos() {
        assertEquals(entidad, entidad.toDomain().toEntity())
    }

    @Test
    fun entityADtoNoIncluyeElEstadoDeSincronizacionPeroConservaElResto() {
        val dto = entidad.toDto()

        assertEquals(entidad.id, dto.id)
        assertEquals(entidad.qrCode, dto.qrCode)
        assertEquals(entidad.units, dto.units)
        assertEquals(entidad.color, dto.color)
        assertEquals(entidad.block, dto.block)
        assertEquals(entidad.updatedAt, dto.updatedAt)
    }

    @Test
    fun dtoAEntityMarcaSyncedPorDefectoYRellenaUbicacionFaltante() {
        val dto = AssetDto(id = "x", qrCode = "LTIC-000001", name = "N", category = "Mouse", units = 2)

        val entidad = dto.toEntity()

        assertEquals(SyncStatus.SYNCED, entidad.syncStatus)
        assertEquals("", entidad.block)
        assertEquals("", entidad.shelf)
        assertEquals("", entidad.locker)
        assertNull(entidad.color)
    }

    @Test
    fun dtoConservaElBorradoLogico() {
        val dto = entidad.copy(deletedAt = 99L).toDto()

        assertEquals(99L, dto.deletedAt)
        assertEquals(99L, dto.toEntity().deletedAt)
    }

    @Test
    fun ciclosDtoEntityDtoSonEstables() {
        val dto = entidad.toDto()

        assertEquals(dto, dto.toEntity().toDto())
    }

    @Test
    fun listaDeEntitiesSeMapeaADominio() {
        val lista = listOf(entidad, entidad.copy(id = "uuid-2")).toDomain()

        assertEquals(listOf("uuid-1", "uuid-2"), lista.map { it.id })
    }
}
