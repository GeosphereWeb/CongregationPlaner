package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.user.domain.repository.UserRepository

actual fun createUserRepository(): UserRepository = unavailableUserRepository("iOS")
