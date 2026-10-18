package de.geosphere.congregationplaner.user.domain.model

/**
 * Rollen eines App-Nutzers. Die Namen entsprechen den Werten im Firestore-Feld `role`
 * der `users`-Collection (kompatibel zu NewSpeechPlan).
 */
enum class UserRole {
    SPEAKING_PLANER,
    SPEAKING_ASSISTANT,
    ADMIN,
    NONE,
    ;

    companion object {
        /** Unbekannte oder fehlende Werte werden bewusst auf [NONE] abgebildet (deny by default). */
        fun fromValue(value: String?): UserRole = entries.firstOrNull { it.name == value } ?: NONE
    }
}
