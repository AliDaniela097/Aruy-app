package ec.edu.puce.aruy.data.red

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Avisa si el teléfono tiene internet, para mostrar "Sin conexión" y recargar cuando vuelve. */
class ConexionRed(contexto: Context) {

    private val manager = contexto.applicationContext.getSystemService(ConnectivityManager::class.java)

    private fun hayInternetAhora(): Boolean {
        val red = manager.activeNetwork ?: return false
        return manager.getNetworkCapabilities(red)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    val hayConexion: Flow<Boolean> = callbackFlow {
        val aviso = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(hayInternetAhora())
            }

            override fun onCapabilitiesChanged(network: Network, capacidades: NetworkCapabilities) {
                trySend(capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }
        manager.registerDefaultNetworkCallback(aviso)
        trySend(hayInternetAhora())
        awaitClose { manager.unregisterNetworkCallback(aviso) }
    }.distinctUntilChanged()
}
