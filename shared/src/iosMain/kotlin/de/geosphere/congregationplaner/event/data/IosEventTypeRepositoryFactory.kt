package de.geosphere.congregationplaner.event.data

import de.geosphere.congregationplaner.event.domain.repository.EventTypeRepository

actual fun createEventTypeRepository(): EventTypeRepository = unavailableEventTypeRepository("iOS")
