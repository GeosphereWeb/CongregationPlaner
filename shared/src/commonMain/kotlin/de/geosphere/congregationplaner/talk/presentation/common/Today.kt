package de.geosphere.congregationplaner.talk.presentation.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/** Polling instead of one long delay: a sleeping device would otherwise wake up long after midnight. */
private val TODAY_CHECK_PERIOD = 1.minutes

/** Der heutige Tag; meldet einen Datumswechsel spätestens eine Minute danach, auch bei offener Anzeige (CP-192). */
fun todayFlow(clock: Clock = Clock.System, zone: TimeZone = TimeZone.currentSystemDefault()): Flow<LocalDate> = flow {
    while (true) {
        emit(clock.todayIn(zone))
        delay(TODAY_CHECK_PERIOD)
    }
}.distinctUntilChanged()
