package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.sync.MonitorConectividad
import kotlinx.coroutines.flow.Flow

/** Estado de la conexión para que la UI avise cuando la app trabaja sin internet (#15). */
class ObservarConexionUseCase(
    private val monitor: MonitorConectividad
) {
    operator fun invoke(): Flow<Boolean> = monitor.observarConexion()
}
