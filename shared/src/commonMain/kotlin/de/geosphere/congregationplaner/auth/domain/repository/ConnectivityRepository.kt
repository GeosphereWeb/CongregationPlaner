package de.geosphere.congregationplaner.auth.domain.repository

import kotlinx.coroutines.flow.Flow

/** Ob das Gerät gerade eine Internetverbindung hat. */
interface ConnectivityRepository {
    /** Emittiert den aktuellen Stand und danach jede Änderung. */
    fun observeOnline(): Flow<Boolean>
}
