package de.geosphere.congregationplaner.talk.presentation.common

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class TodayTest {
    @Test
    fun `the day switches after midnight without reopening`() = runTest {
        val clock = clockFrom(LocalDateTime(2026, 11, 25, 23, 59, 30))

        val days = todayFlow(clock, TimeZone.UTC).take(2).toList()

        assertEquals(listOf(LocalDate(2026, 11, 25), LocalDate(2026, 11, 26)), days)
    }

    @Test
    fun `the same day is emitted only once`() = runTest {
        val clock = clockFrom(LocalDateTime(2026, 11, 25, 10, 0))
        val days = mutableListOf<LocalDate>()

        backgroundScope.launch { todayFlow(clock, TimeZone.UTC).toList(days) }
        advanceTimeBy(2.hours)
        runCurrent()

        assertEquals(listOf(LocalDate(2026, 11, 25)), days)
    }

    /** A clock that starts at [start] (UTC) and moves with the virtual time of the test. */
    private fun TestScope.clockFrom(start: LocalDateTime): Clock {
        val origin: Instant = start.toInstant(TimeZone.UTC)
        return object : Clock {
            override fun now(): Instant = origin + testScheduler.currentTime.milliseconds
        }
    }
}
