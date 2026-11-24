package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow

/**
 * [Caso de práctica 1: Filtro local reactivo]
 *
 * Expone el catálogo restringido a los activos con cambios locales aún no confirmados
 * por el servidor. Es Kotlin puro: no conoce Room ni Android, solo el contrato del dominio.
 */
class ObtenerActivosPendientesUseCase(
    private val repository: AssetRepository
) {
    operator fun invoke(query: String = "", category: String? = null): Flow<List<Asset>> {
        return repository.observePendingAssets(query = query, category = category)
    }
}
