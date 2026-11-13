package de.geosphere.congregationplaner.auth.data

import de.geosphere.congregationplaner.auth.domain.repository.ConnectivityRepository

/** Die Verbindungsprüfung der Plattform. */
expect fun createConnectivityRepository(): ConnectivityRepository
