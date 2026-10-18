package de.geosphere.congregationplaner.user.domain.model

/**
 * Repräsentiert einen Nutzer aus der Firestore-Collection `users`.
 *
 * @param uid Die ID aus Firebase Authentication; dient als Dokument-ID.
 * @param approved Gibt an, ob der Nutzer für die App-Nutzung freigeschaltet ist.
 * @param role Die Rolle, aus der die Berechtigungen abgeleitet werden.
 */
data class AppUser(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val approved: Boolean = false,
    val role: UserRole = UserRole.SPEAKING_ASSISTANT,
)
