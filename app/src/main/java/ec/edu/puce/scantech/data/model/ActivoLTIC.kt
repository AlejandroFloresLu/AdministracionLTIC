package ec.edu.puce.scantech.data.model

import ec.edu.puce.scantech.data.local.SyncStatus
import kotlinx.serialization.Serializable

/**
 * Modelo de datos inmutable que representa un activo tecnológico del inventario LTIC.
 * Cumple con el "Mindset Kotlin" exigido en el Tema 4 y la especificación del Documento Maestro.
 */
@Serializable
data class ActivoLTIC(
    val id: String,
    val qrCode: String,
    val nombre: String,
    val serie: String,
    val categoria: String,
    val marca: String,
    val modelo: String,
    val ram: String = "N/A",
    val cpu: String = "N/A",
    val ubicacion: String = "Bodega Central LTIC",
    val observaciones: String = "",
    val estadoOperativo: EstadoOperativo = EstadoOperativo.OPERATIVO,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis()
)
