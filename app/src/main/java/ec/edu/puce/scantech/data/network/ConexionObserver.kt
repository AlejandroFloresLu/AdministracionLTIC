package ec.edu.puce.scantech.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observa si el dispositivo tiene acceso a Internet. Alimenta el banner "Sin conexión" (#15)
 * y dispara la sincronización cuando vuelve la red.
 */
class ConexionObserver(context: Context) {

    private val gestorRed = context.applicationContext
        .getSystemService(ConnectivityManager::class.java)

    /** Emite `true` con conexión a Internet y `false` sin ella, solo cuando cambia. */
    fun observarConexion(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(hayInternet())
            }

            override fun onLost(network: Network) {
                trySend(hayInternet())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capacidades: NetworkCapabilities
            ) {
                trySend(hayInternet())
            }
        }

        trySend(hayInternet())
        gestorRed.registerDefaultNetworkCallback(callback)

        awaitClose { gestorRed.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    /** Estado actual de la conexión, sin esperar cambios. */
    fun hayInternet(): Boolean {
        val capacidades = gestorRed.getNetworkCapabilities(gestorRed.activeNetwork)
        return capacidades?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
