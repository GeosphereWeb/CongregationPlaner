package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.talk.domain.model.ContactAttempt
import de.geosphere.congregationplaner.talk.domain.model.ContactKind
import kotlinx.datetime.LocalDate

/** Ein Kontaktversuch aus den Firestore-Feldern; ungültige Einträge werden übersprungen (`null`). */
internal fun contactAttemptOrNull(date: String?, kind: String?, note: String?): ContactAttempt? = runCatching {
    ContactAttempt(LocalDate.parse(date.orEmpty()), ContactKind.valueOf(kind.orEmpty()), note)
}.getOrNull()
