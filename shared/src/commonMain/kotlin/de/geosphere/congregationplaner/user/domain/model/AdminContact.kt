package de.geosphere.congregationplaner.user.domain.model

/**
 * Name eines freigegebenen Versammlungsadmins. Mehr als den Namen sehen Nutzer ohne Freigabe nicht;
 * die Mitglieder selbst bleiben für sie unlesbar (siehe `adminContacts` in `firestore.rules`).
 */
data class AdminContact(val uid: String, val firstName: String, val lastName: String) {
    val fullName: String get() = listOf(firstName, lastName).filter(String::isNotBlank).joinToString(" ")
}

/** Der Kontakt, den dieses Mitglied haben soll; nur freigegebene Versammlungsadmins haben einen. */
fun AppUser.adminContact(): AdminContact? = if (approved && role == UserRole.CONGREGATION_ADMIN) {
    AdminContact(uid, profile.firstName, profile.lastName)
} else {
    null
}
