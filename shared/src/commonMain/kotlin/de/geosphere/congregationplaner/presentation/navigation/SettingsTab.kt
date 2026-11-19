package de.geosphere.congregationplaner.presentation.navigation

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.usecase.canKeepMessageTemplates

/** Die Reiter der Einstellungen. */
enum class SettingsTab(val label: String) {
    PROFILE("Mein Profil"),
    EMERGENCY_CONTACTS("Notfallkontakte"),
    MESSAGE_TEMPLATES("WhatsApp"),
    GENERAL("Allgemein"),
}

/**
 * Die Reiter, die für [user] gelten: Das Profil pflegen Mitglieder mit eigenem Konto in einer Versammlung und globale
 * Admins; Notfallkontakte haben globale Admins nicht. „Allgemein“ (Import, Datenquellen) gibt es immer.
 */
fun settingsTabs(user: AppUser): List<SettingsTab> {
    val hasProfile = user.hasAccount && (user.congregation != null || user.isGlobalAdmin)
    return buildList {
        if (hasProfile) add(SettingsTab.PROFILE)
        if (hasProfile && !user.isGlobalAdmin) add(SettingsTab.EMERGENCY_CONTACTS)
        if (user.canKeepMessageTemplates) add(SettingsTab.MESSAGE_TEMPLATES)
        add(SettingsTab.GENERAL)
    }
}
