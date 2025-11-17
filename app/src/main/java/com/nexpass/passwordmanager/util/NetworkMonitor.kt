package com.nexpass.passwordmanager.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Monitor network connectivity status.
 *
 * Features:
 * - Real-time network state updates via Flow
 * - Distinguishes between different connection types (WiFi, Cellular, etc.)
 * - Validates internet connectivity (not just network connection)
 */
class NetworkMonitor(private val context: Context) {

    companion object {
        private const val TAG = "NetworkMonitor"
    }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Observe network connectivity state as a Flow
     */
    fun observeNetworkState(): Flow<NetworkState> = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available: $network")
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val state = capabilities?.toNetworkState() ?: NetworkState.Unavailable
                trySend(state)
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost: $network")
                trySend(NetworkState.Unavailable)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                Log.d(TAG, "Network capabilities changed: $network")
                val state = capabilities.toNetworkState()
                trySend(state)
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Send initial state
        val initialState = getCurrentNetworkState()
        trySend(initialState)

        awaitClose {
            Log.d(TAG, "Unregistering network callback")
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged()

    /**
     * Get current network state synchronously
     */
    fun getCurrentNetworkState(): NetworkState {
        val network = connectivityManager.activeNetwork ?: return NetworkState.Unavailable
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkState.Unavailable

        return capabilities.toNetworkState()
    }

    /**
     * Check if device has internet connectivity
     */
    fun isOnline(): Boolean {
        return getCurrentNetworkState() is NetworkState.Available
    }

    /**
     * Check if device is connected to WiFi
     */
    fun isWiFiConnected(): Boolean {
        val state = getCurrentNetworkState()
        return state is NetworkState.Available && state.connectionType == ConnectionType.WiFi
    }

    /**
     * Check if device is connected to cellular
     */
    fun isCellularConnected(): Boolean {
        val state = getCurrentNetworkState()
        return state is NetworkState.Available && state.connectionType == ConnectionType.Cellular
    }

    private fun NetworkCapabilities.toNetworkState(): NetworkState {
        if (!hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkState.Unavailable
        }

        if (!hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            return NetworkState.Available(
                connectionType = getConnectionType(),
                isMetered = !hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
                isValidated = false
            )
        }

        return NetworkState.Available(
            connectionType = getConnectionType(),
            isMetered = !hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            isValidated = true
        )
    }

    private fun NetworkCapabilities.getConnectionType(): ConnectionType {
        return when {
            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WiFi
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.Cellular
            hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.Ethernet
            hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> ConnectionType.VPN
            else -> ConnectionType.Other
        }
    }
}

/**
 * Network connectivity state
 */
sealed class NetworkState {
    /**
     * Network is available and validated
     */
    data class Available(
        val connectionType: ConnectionType,
        val isMetered: Boolean,
        val isValidated: Boolean
    ) : NetworkState()

    /**
     * No network available
     */
    object Unavailable : NetworkState()
}

/**
 * Type of network connection
 */
enum class ConnectionType {
    WiFi,
    Cellular,
    Ethernet,
    VPN,
    Other
}

/**
 * Extension functions for NetworkState
 */
fun NetworkState.isAvailable(): Boolean = this is NetworkState.Available

fun NetworkState.isValidated(): Boolean = this is NetworkState.Available && this.isValidated

fun NetworkState.isMetered(): Boolean = this is NetworkState.Available && this.isMetered

fun NetworkState.getConnectionType(): ConnectionType? =
    if (this is NetworkState.Available) this.connectionType else null
