package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.LocalDate

/** Die Termine ab [today], an denen ein Vortrag gehalten wird (abgesagte zählen nicht), nach Datum sortiert. */
fun List<TalkSlot>.upcomingTalkSlots(today: LocalDate): List<TalkSlot> =
    distinctBy { it.date }.filter { it.date >= today && !it.talkCancelled }.sortedBy { it.date }
