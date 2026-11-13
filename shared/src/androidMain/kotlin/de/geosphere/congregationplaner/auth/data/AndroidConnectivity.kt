package de.geosphere.congregationplaner.auth.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import de.geosphere.congregationplaner.auth.domain.repository.ConnectivityRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

actual fun createConnectivityRepository(): ConnectivityRepository = AndroidConnectivityRepository()

/** Hört auf das Standardnetz des Systems; online heißt: Internet vorhanden und vom System geprüft. */
private class AndroidConnectivityRepository : ConnectivityRepository {
    override fun observeOnline(): Flow<Boolean> {
        val manager = FirebaseAndroidContextHolder.context
            ?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return flowOf(true)
        return callbackFlow {
            trySend(manager.getNetworkCapabilities(manager.activeNetwork).isOnline())
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    trySend(capabilities.isOnline())
                }

                override fun onLost(network: Network) {
                    trySend(false)
                }
            }
            manager.registerDefaultNetworkCallback(callback)
            awaitClose { manager.unregisterNetworkCallback(callback) }
        }
    }
}

private fun NetworkCapabilities?.isOnline(): Boolean = this != null &&
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
    hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
