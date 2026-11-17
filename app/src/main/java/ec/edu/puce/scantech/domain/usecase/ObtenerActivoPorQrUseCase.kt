package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.repository.AssetRepository

/**
 * [RDA-3 Tema 9: Clean Architecture - Casos de Uso (Interactors)]
 * [RDA-3 Tema 10: Concurrencia - Funciones de Suspensión (suspend)]
 *
 * Caso de uso responsable de buscar un activo específico mediante su código QR óptico escaneado.
 *
 * Conceptos Teóricos Aplicados:
 * 1. Suspensión Cooperativa: Utiliza la palabra clave `suspend`, permitiendo que el hilo de ejecución
 *    se libere de inmediato mientras se consulta la base de datos local o la memoria persistente,
 *    evitando bloqueos del hilo principal (UI Thread) y eliminando el riesgo de errores ANR (>5s).
 * 2. Envoltura Segura de Resultados: Retorna [Result], imponiendo una estructura tipada que fuerza
 *    al ViewModel a manejar tanto el éxito ([Result.Success]) como la ausencia o fallo ([Result.Error]).
 */
class ObtenerActivoPorQrUseCase(
    private val repository: AssetRepository
) {
    /**
     * Consulta un activo por su código QR único.
     *
     * @param qrCode Cadena del código QR decodificado (p. ej. "LTIC-000123").
     * @return [Result] con el activo hallado o un error descriptivo si no existe.
     */
    suspend operator fun invoke(qrCode: String): Result<Asset> {
        val codigoLimpio = qrCode.trim()
        if (codigoLimpio.isBlank()) {
            return Result.Error("El código QR proporcionado está vacío")
        }
        return repository.getByQrCode(codigoLimpio)
    }
}

