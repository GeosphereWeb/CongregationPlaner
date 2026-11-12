package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.presentation.common.toDisplayText
import kotlinx.datetime.LocalDate

/** Beschriftung eines Stichtags, z. B. „Redner frei seit So, 01.11.2026“ oder „Redner: kein Stichtag“. */
internal fun cutoffLabel(subject: String, date: LocalDate?): String =
    date?.let { "$subject frei seit ${it.toDisplayText()}" } ?: "$subject: kein Stichtag"
