package de.geosphere.congregationplaner.organization.presentation.selection

/** Text des Bestätigungsschritts; nennt die Zieladresse, wenn sie bekannt ist. */
internal fun verificationHint(congregationName: String, email: String?): String {
    val recipient = email?.trim()?.takeIf { it.isNotEmpty() }?.let { " an $it" }.orEmpty()
    return "Wir haben dir eine E-Mail$recipient geschickt. " +
        "Öffne den Link darin, dann legen wir „$congregationName“ an. " +
        "Hast du sie erneut gesendet, gilt nur der Link aus der neuesten Mail."
}
