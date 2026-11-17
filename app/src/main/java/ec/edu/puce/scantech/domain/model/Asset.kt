package ec.edu.puce.scantech.domain.model

import java.util.UUID

/**
 * [RDA-1 Tema 4: Componentes Fundamentales e Inmutabilidad]
 * [RDA-3 Temas 9 y 10: Clean Architecture - Entidades de Dominio (Core)]
 *
 * Modelo inmutable que representa un activo tecnológico del departamento de LTIC (PUCE).
 *
 * Conceptos Teóricos Aplicados:
 * 1. Dominio Puro (Core Layer): Clase de datos Kotlin pura sin anotaciones de persistencia (@Entity)
 *    ni serialización de red (@Serializable). Cero dependencias de Android framework o SQLite.
 * 2. Inmutabilidad y Flujo Unidireccional (UDF): Todos sus campos son `val`. Las transformaciones
 *    se realizan mediante copias explícitas (`copy`), eliminando efectos secundarios y garantizando
 *    que la interfaz observe estados consistentes e incorruptibles.
 * 3. Desacoplamiento de Infraestructura: La conversión hacia y desde entidades físicas (`AssetEntity`)
 *    y DTOs remotos (`AssetDto`) se delega exclusivamente a la capa de datos (`data.mapper`).
 */
data class Asset(
    val id: String = UUID.randomUUID().toString(),
    val qrCode: String,
    val name: String,
    val category: String,
    val technicalSpecs: String,
    val color: String? = null,
    val dimensions: String? = null,
    val block: String,
    val shelf: String,
    val locker: String,
    val units: Int,
    val observations: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
) {
    /**
     * Propiedad calculada que expone la ubicación estructurada del activo en bodegas LTIC.
     * Ejemplo: "Bloque A · Estante 3 · Casillero 2".
     */
    val location: String
        get() = "Bloque $block · Estante $shelf · Casillero $locker"
}
