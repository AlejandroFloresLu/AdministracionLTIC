package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.repository.AssetRepository

/**
 * [RDA-3 Tema 9: Clean Architecture - Casos de Uso]
 * [RDA-3 Temas 11 y 12: Borrado Lógico en Arquitectura Offline-First]
 *
 * Caso de uso responsable de dar de baja (eliminar) un activo tecnológico de LTIC.
 *
 * Conceptos Teóricos Aplicados:
 * 1. Resiliencia de Datos: En sistemas distribuidos offline-first, los activos no se destruyen físicamente
 *    de inmediato; se realiza un borrado lógico marcando `deletedAt` y encolando la operación `DELETE`
 *    para propagarla a la API REST central.
 * 2. Desacoplamiento: El ViewModel delega toda la orquestación a este caso de uso sin conocer DAOs ni tablas.
 */
class EliminarActivoUseCase(
    private val repository: AssetRepository
) {
    /**
     * Da de baja el activo por su ID.
     *
     * @param id Identificador UUID del activo a eliminar.
     * @return [Result.Success] si la baja fue procesada correctamente o [Result.Error] si no existe.
     */
    suspend operator fun invoke(id: String): Result<Unit> {
        if (id.isBlank()) {
            return Result.Error("El identificador del activo no es válido")
        }
        return repository.delete(id)
    }
}

