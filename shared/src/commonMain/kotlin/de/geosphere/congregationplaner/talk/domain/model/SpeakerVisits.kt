package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.LocalDate

/** Wie oft und wann zuletzt ein externer Redner bei uns war oder eingeplant ist (CP-193). */
data class SpeakerVisits(val lastVisit: LocalDate, val count: Int)

/** Die Besuche je externem Redner (`externalSpeakerId`); abgesagte Vorträge zählen nicht, ein Tag zählt einmal. */
fun speakerVisitsOf(slots: List<TalkSlot>): Map<String, SpeakerVisits> = slots
    .filter { !it.talkCancelled }
    .mapNotNull { slot -> slot.externalSpeakerId?.let { it to slot.date } }
    .groupBy({ it.first }, { it.second })
    .mapValues { (_, days) -> SpeakerVisits(days.max(), days.distinct().size) }
