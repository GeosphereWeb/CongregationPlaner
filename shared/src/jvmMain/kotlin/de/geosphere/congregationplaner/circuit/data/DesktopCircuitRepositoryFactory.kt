package de.geosphere.congregationplaner.circuit.data

import de.geosphere.congregationplaner.circuit.domain.repository.CircuitRepository

actual fun createCircuitRepository(): CircuitRepository = FirestoreRestCircuitRepository()
