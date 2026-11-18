package ec.edu.puce.scantech.data.mapper

import ec.edu.puce.scantech.data.model.ActivoLTIC
import ec.edu.puce.scantech.domain.model.Asset

/**
 * Convierte el modelo de datos de prueba [ActivoLTIC] al modelo de dominio [Asset].
 *
 * Es un puente temporal: las pantallas ya trabajan con [Asset] y, cuando el catálogo
 * lea desde Room (#53), los datos llegarán como [Asset] desde el repositorio y este
 * archivo podrá eliminarse junto con [ActivoLTIC].
 *
 * Marca, modelo, serie, RAM y CPU no tienen campo propio en [Asset], así que se agrupan
 * en [Asset.technicalSpecs], una por línea. RAM y CPU se omiten cuando valen "N/A".
 */
fun ActivoLTIC.toAsset(): Asset {
    val especificaciones = buildList {
        add("Marca: $marca")
        add("Modelo: $modelo")
        add("Serie: $serie")
        if (ram != SIN_DATO) add("RAM: $ram")
        if (cpu != SIN_DATO) add("CPU: $cpu")
    }.joinToString(separator = "\n")

    return Asset(
        id = id,
        qrCode = qrCode,
        name = nombre,
        category = categoria,
        technicalSpecs = especificaciones,
        color = color,
        dimensions = dimensiones,
        block = bloque,
        shelf = estante,
        locker = casillero,
        units = unidades,
        observations = observaciones.ifBlank { null },
        syncStatus = syncStatus,
        updatedAt = updatedAt
    )
}

private const val SIN_DATO = "N/A"
