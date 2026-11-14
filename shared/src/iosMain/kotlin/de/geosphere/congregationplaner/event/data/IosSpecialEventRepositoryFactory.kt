package de.geosphere.congregationplaner.event.data

import de.geosphere.congregationplaner.event.domain.repository.SpecialEventRepository

actual fun createSpecialEventRepository(): SpecialEventRepository = unavailableSpecialEventRepository("iOS")
