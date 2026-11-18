package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow

/**
 * [RDA-3 Tema 9: Clean Architecture - Casos de Uso (Interactors)]
 * [RDA-3 Tema 10: Concurrencia y Programación Asíncrona con Flow]
 *
 * Caso de uso responsable de obtener y observar de manera reactiva el catálogo de activos de LTIC.
 *
 * Conceptos Teóricos Aplicados:
 * 1. Principio de Responsabilidad Única (SoC): Encapsula una única intención del usuario
 *    (consultar el inventario disponible con filtros opcionales de texto y categoría).
 * 2. Independencia de Frameworks: Es Kotlin puro. No conoce Android, Jetpack Compose, Room ni Retrofit.
 * 3. Reactividad No Bloqueante: Retorna un [Flow] asíncrono que emite automáticamente cada vez que
 *    la base de datos local (Room) sufre inserciones, actualizaciones o eliminaciones, asegurando
 *    que la UI permanezca siempre sincronizada con la fuente única de verdad (SSOT).
 */
class ObtenerActivosUseCase(
    private val repository: AssetRepository
) {
    /**
     * Ejecuta la consulta reactiva de activos.
     *
     * @param query Texto de búsqueda para filtrar por nombre, código QR o número de serie.
     * @param category Categoría tecnológica específica o `null` para consultar todas.
     * @return [Flow] que emite la lista inmutable de activos que satisfacen el criterio.
     */
    operator fun invoke(query: String = "", category: String? = null): Flow<List<Asset>> {
        return repository.observeAssets(query = query, category = category)
    }
}

