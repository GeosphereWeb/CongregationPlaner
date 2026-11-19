package de.geosphere.congregationplaner.presentation.navigation

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.presentation.navigation.SettingsTab.EMERGENCY_CONTACTS
import de.geosphere.congregationplaner.presentation.navigation.SettingsTab.GENERAL
import de.geosphere.congregationplaner.presentation.navigation.SettingsTab.MESSAGE_TEMPLATES
import de.geosphere.congregationplaner.presentation.navigation.SettingsTab.PROFILE
import de.geosphere.congregationplaner.user.domain.model.AppUser
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsTabTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")

    @Test
    fun `a member with an account sees all tabs`() {
        assertEquals(
            listOf(PROFILE, EMERGENCY_CONTACTS, MESSAGE_TEMPLATES, GENERAL),
            settingsTabs(AppUser(uid = "u", congregation = congregation)),
        )
    }

    @Test
    fun `a global admin without congregation has no emergency contacts or templates`() {
        assertEquals(listOf(PROFILE, GENERAL), settingsTabs(AppUser(uid = "u", isGlobalAdmin = true)))
    }

    @Test
    fun `without congregation or account only general remains`() {
        assertEquals(listOf(GENERAL), settingsTabs(AppUser(uid = "u")))
        assertEquals(listOf(GENERAL), settingsTabs(AppUser(uid = "u", congregation = congregation, hasAccount = false)))
    }
}
