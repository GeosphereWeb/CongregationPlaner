package de.geosphere.congregationplaner.user.di

import de.geosphere.congregationplaner.user.data.createThemePreferenceRepository
import de.geosphere.congregationplaner.user.domain.repository.ThemePreferenceRepository
import de.geosphere.congregationplaner.user.presentation.theme.ThemeViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface ThemeGraph {
    val themeViewModel: ThemeViewModel

    @Provides
    fun provideThemePreferenceRepository(): ThemePreferenceRepository = createThemePreferenceRepository()
}
