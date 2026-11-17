package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.repository.AssetRepository
import ec.edu.puce.scantech.domain.repository.SyncSummary

/**
 * [RDA-3 Tema 9: Clean Architecture - Casos de Uso]
 * [RDA-3 Temas 11 y 12: Sincronización Bidireccional Asíncrona con Retrofit y Room]
 *
 * Caso de uso responsable de subir la cola de operaciones locales a la API REST central
 * y descargar las novedades o bajas registradas en el servidor desde la última sincronización.
 */
class SincronizarActivosUseCase(
    private val repository: AssetRepository
) {
    /**
     * Dispara la ronda de sincronización en segundo plano.
     *
     * @return [Result.Success] con el resumen [SyncSummary] de operaciones enviadas, recibidas
     *         y conflictos detectados, o [Result.Error] si ocurrió un fallo de red o HTTP.
     */
    suspend operator fun invoke(): Result<SyncSummary> {
        return repository.sync()
    }
}

