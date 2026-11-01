package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.user.domain.repository.AdminContactRepository
import de.geosphere.congregationplaner.user.domain.repository.UserRepository

actual fun createUserRepository(): UserRepository =
    FirestoreRestUserRepository().withAdminContacts(FirestoreRestAdminContactRepository())

actual fun createAdminContactRepository(): AdminContactRepository = FirestoreRestAdminContactRepository()
