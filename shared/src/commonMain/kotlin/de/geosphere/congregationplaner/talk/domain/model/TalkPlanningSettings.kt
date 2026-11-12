package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.LocalDate

/**
 * Stichtage des Planungsassistenten einer Versammlung aus `…/congregation/{id}/settings/talkPlanning`: Ein Redner bzw.
 * ein Vortrag, der an einem Tag ab dem Stichtag eingeplant ist, wird nicht vorgeschlagen. `null` heißt: keine Sperre.
 */
data class TalkPlanningSettings(val speakerFreeSince: LocalDate? = null, val talkFreeSince: LocalDate? = null)
