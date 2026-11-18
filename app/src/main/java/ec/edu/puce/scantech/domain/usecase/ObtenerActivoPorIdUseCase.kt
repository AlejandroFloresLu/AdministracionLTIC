package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow

/**
 * [RDA-3 Tema 9: Clean Architecture - Casos de Uso]
 * [RDA-3 Temas 11 y 12: Observabilidad Reactiva con Flow sobre Room]
 *
 * Caso de uso responsable de observar reactivamente el detalle de un activo por su ID interno.
 * Utilizado por la Ficha Técnica (AssetDetail) y el Formulario de Edición (AssetForm).
 */
class ObtenerActivoPorIdUseCase(
    private val repository: AssetRepository
) {
    /**
     * Observa el activo en tiempo real.
     * Emite `null` si el activo no existe o si fue dado de baja mediante borrado lógico.
     */
    operator fun invoke(id: String): Flow<Asset?> {
        return repository.observeById(id)
    }
}

