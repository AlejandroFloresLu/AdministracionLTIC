package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.repository.AssetRepository
import ec.edu.puce.scantech.domain.validacion.ValidadorActivo

/**
 * Guarda las observaciones de auditoría física de un activo (Estrategia de Datos §3.b).
 *
 * Solo toca las observaciones: no revalida ni reescribe el resto del activo. La capa de datos
 * refresca `updatedAt`, pasa el activo a PENDING_UPDATE (o lo deja en PENDING_CREATE si aún
 * no se subió) y encola la operación, todo en una sola transacción de Room.
 */
class GuardarObservacionesUseCase(
    private val repository: AssetRepository
) {
    suspend operator fun invoke(assetId: String, observaciones: String): Result<Asset> {
        if (assetId.isBlank()) {
            return Result.Error("El identificador del activo no es válido")
        }
        if (observaciones.trim().length > ValidadorActivo.MAX_OBSERVACIONES) {
            return Result.Error("Máximo ${ValidadorActivo.MAX_OBSERVACIONES} caracteres")
        }
        return repository.updateObservations(assetId, observaciones)
    }
}
