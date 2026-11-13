package de.geosphere.congregationplaner.auth.data

import de.geosphere.congregationplaner.auth.domain.repository.ConnectivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// shortcut: iOS always reports online, upgrade to NWPathMonitor once iOS is actively developed.
actual fun createConnectivityRepository(): ConnectivityRepository = object : ConnectivityRepository {
    override fun observeOnline(): Flow<Boolean> = flowOf(true)
}
