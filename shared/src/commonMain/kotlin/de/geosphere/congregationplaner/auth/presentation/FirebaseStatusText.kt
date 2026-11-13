package de.geosphere.congregationplaner.auth.presentation

/** Statuszeile zu Firebase: [ready] ist `null`, solange Firebase startet; ohne Verbindung hilft auch Firebase nicht. */
internal fun firebaseStatusText(ready: Boolean?, online: Boolean): String = when {
    ready == null -> "Firebase wird initialisiert..."
    !ready -> "Firebase nicht konfiguriert"
    !online -> "Keine Internetverbindung"
    else -> "Firebase verfügbar"
}
