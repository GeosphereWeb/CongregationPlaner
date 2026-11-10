package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Chairman
import de.geosphere.congregationplaner.talk.domain.model.Speaker

/** Die Mitglieder, die im Termin als Redner und als Vorsitzender zur Auswahl stehen. */
data class TalkMembers(val speakers: List<Speaker>, val chairmen: List<Chairman>)
