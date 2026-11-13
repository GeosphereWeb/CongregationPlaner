package de.geosphere.congregationplaner.auth.domain.usecase

import de.geosphere.congregationplaner.auth.domain.repository.ConnectivityRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Beobachtet, ob eine Internetverbindung besteht; gleiche Werte hintereinander werden nur einmal gemeldet. */
@Inject
class ObserveConnectivityUseCase(private val repository: ConnectivityRepository) {
    operator fun invoke(): Flow<Boolean> = repository.observeOnline().distinctUntilChanged()
}
