package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.repository.ThemePreference
import de.geosphere.congregationplaner.user.domain.repository.ThemePreferenceRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.StateFlow

@Inject
class ObserveThemePreferenceUseCase(private val repository: ThemePreferenceRepository) {
    operator fun invoke(): StateFlow<ThemePreference> = repository.preference
}

@Inject
class SaveThemePreferenceUseCase(private val repository: ThemePreferenceRepository) {
    suspend operator fun invoke(preference: ThemePreference) = repository.save(preference)
}
