package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.model.ResumenInventario
import ec.edu.puce.scantech.domain.model.SyncStatus
import ec.edu.puce.scantech.domain.repository.AssetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * [Caso de práctica 8: Concurrencia - cálculo fuera del hilo principal]
 *
 * Calcula los totales del inventario cada vez que Room emite una lista nueva.
 * El recorrido de la lista es trabajo de CPU, así que se ejecuta en [Dispatchers.Default]
 * con `flowOn`: el hilo principal solo recibe el resultado y dibuja (presupuesto de 16,67 ms
 * por frame a 60 Hz). El despachador se recibe por constructor para poder probarlo.
 */
class ObtenerResumenInventarioUseCase(
    private val repository: AssetRepository,
    private val dispatcherCalculo: CoroutineDispatcher = Dispatchers.Default
) {
    operator fun invoke(): Flow<ResumenInventario> =
        repository.observeAssets()
            .map { activos -> calcular(activos) }
            .flowOn(dispatcherCalculo)

    private fun calcular(activos: List<Asset>) = ResumenInventario(
        totalActivos = activos.size,
        totalUnidades = activos.sumOf { it.units },
        pendientesSync = activos.count { it.syncStatus != SyncStatus.SYNCED }
    )
}
