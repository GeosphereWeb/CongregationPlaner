package de.geosphere.congregationplaner.circuit.di

import de.geosphere.congregationplaner.circuit.data.createCircuitRepository
import de.geosphere.congregationplaner.circuit.domain.repository.CircuitRepository
import de.geosphere.congregationplaner.circuit.presentation.CircuitViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface CircuitGraph {
    val circuitViewModel: CircuitViewModel

    @Provides
    fun provideCircuitRepository(): CircuitRepository = createCircuitRepository()
}
