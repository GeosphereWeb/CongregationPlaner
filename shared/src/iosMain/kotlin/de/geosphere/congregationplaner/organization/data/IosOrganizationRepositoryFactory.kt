package de.geosphere.congregationplaner.organization.data

import de.geosphere.congregationplaner.organization.domain.repository.OrganizationRepository
import platform.Foundation.NSUserDefaults

actual fun createOrganizationRepository(): OrganizationRepository = unavailableOrganizationRepository("iOS")

actual fun createKeyValueStorage(): KeyValueStorage = object : KeyValueStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}
