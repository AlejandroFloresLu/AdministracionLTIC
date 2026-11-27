package ec.edu.puce.scantech.domain.sync

import kotlinx.coroutines.flow.Flow

/**
 * Contrato del dominio para saber si hay internet. La implementación real
 * ([ec.edu.puce.scantech.data.network.ConexionObserver]) usa ConnectivityManager; el dominio
 * y los ViewModels solo conocen esta interfaz, así se pueden probar sin Android.
 */
interface MonitorConectividad {
    /** Emite `true` con internet y `false` sin él, cada vez que cambia. */
    fun observarConexion(): Flow<Boolean>
}
