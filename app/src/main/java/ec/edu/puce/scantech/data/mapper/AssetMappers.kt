package ec.edu.puce.scantech.data.mapper

import ec.edu.puce.scantech.data.local.SyncStatus
import ec.edu.puce.scantech.data.local.entity.AssetEntity
import ec.edu.puce.scantech.data.remote.AssetDto
import ec.edu.puce.scantech.domain.model.Asset

/*
 * Mapeadores entre las tres capas: Room (AssetEntity) <-> dominio (Asset) <-> red (AssetDto).
 * Funciones puras y sin efectos secundarios, por lo que se prueban en la JVM.
 */

// ---------------------------------------------------------------- Entity -> ...

fun AssetEntity.toDomain(): Asset = Asset(
    id = id,
    qrCode = qrCode,
    name = name,
    category = category,
    technicalSpecs = technicalSpecs,
    color = color,
    dimensions = dimensions,
    block = block,
    shelf = shelf,
    locker = locker,
    units = units,
    observations = observations,
    responsable = responsable,
    syncStatus = syncStatus,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

/** El estado de sincronización es solo local: no viaja en el DTO. */
fun AssetEntity.toDto(): AssetDto = AssetDto(
    id = id,
    qrCode = qrCode,
    name = name,
    category = category,
    technicalSpecs = technicalSpecs,
    color = color,
    dimensions = dimensions,
    block = block,
    shelf = shelf,
    locker = locker,
    units = units,
    observations = observations,
    responsable = responsable,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

// ---------------------------------------------------------------- Domain -> ...

fun Asset.toEntity(): AssetEntity = AssetEntity(
    id = id,
    qrCode = qrCode,
    name = name,
    category = category,
    technicalSpecs = technicalSpecs,
    color = color,
    dimensions = dimensions,
    block = block,
    shelf = shelf,
    locker = locker,
    units = units,
    observations = observations,
    responsable = responsable,
    syncStatus = syncStatus,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun Asset.toDto(): AssetDto = toEntity().toDto()

// ------------------------------------------------------------------ Dto -> ...

/**
 * Un activo recibido del servidor ya está sincronizado, salvo que se indique otro estado.
 * Los campos de ubicación que el servidor no envíe se guardan vacíos.
 */
fun AssetDto.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED): AssetEntity = AssetEntity(
    id = id,
    qrCode = qrCode,
    name = name,
    category = category,
    technicalSpecs = technicalSpecs,
    color = color,
    dimensions = dimensions,
    block = block.orEmpty(),
    shelf = shelf.orEmpty(),
    locker = locker.orEmpty(),
    units = units,
    observations = observations,
    responsable = responsable,
    syncStatus = syncStatus,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun AssetDto.toDomain(syncStatus: SyncStatus = SyncStatus.SYNCED): Asset =
    toEntity(syncStatus).toDomain()

// ------------------------------------------------------------------- Listas

fun List<AssetEntity>.toDomain(): List<Asset> = map { it.toDomain() }
