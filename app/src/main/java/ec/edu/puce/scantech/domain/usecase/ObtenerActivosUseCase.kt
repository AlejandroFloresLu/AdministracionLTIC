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

/* =================== [DEFENSA CASO 21: Caso de Uso Puro FiltrarActivosPorEstadoUseCase con invoke] ===================
// 🛠️ PASO PARA ACTIVAR EN EL EXAMEN:
// Descomenta esta clase de caso de uso de dominio puro:
// class FiltrarActivosPorEstadoUseCase {
//     /**
//      * Sobrecarga del operador `invoke` que permite invocar la instancia como si fuera una función:
//      * val filtrados = filtrarActivosUseCase(listaActivos, soloActivos = true)
//      */
//     operator fun invoke(activos: List<Asset>, soloActivos: Boolean): List<Asset> =
//         if (soloActivos) {
//             activos.filter { it.deletedAt == null }
//         } else {
//             activos.filter { it.deletedAt != null }
//         }
// }

// 💡 RESPUESTA TEÓRICA RÁPIDA:
// - Los Casos de Uso (Interactors) representan una sola acción de negocio (Single Responsibility Principle).
// - El modificador 'operator fun invoke' permite llamar a la instancia del caso de uso con sintaxis funcional
//   (ej. 'casoDeUso(params)'), mejorando la expresividad y simplificando el consumo desde el ViewModel.
// - Es Kotlin puro sin dependencias de base de datos ni Android, garantizando pruebas unitarias independientes.
====================================================================================================================== */

