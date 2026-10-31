package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.MeetingTimes
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

private const val PLANNED_YEARS_AHEAD = 2

@Composable
internal fun rememberCurrentYear(): Int = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()).year }

/** Erlaubt je einem der nächsten zwei Kalenderjahre abweichende Zusammenkunftszeiten (z. B. für die Vortragsplanung). */
@Composable
internal fun YearlyMeetingTimesEditor(congregation: CongregationMetadata, onChange: (CongregationMetadata) -> Unit) {
    val currentYear = rememberCurrentYear()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Zeiten der nächsten Jahre", style = MaterialTheme.typography.titleSmall)
        (1..PLANNED_YEARS_AHEAD).map { currentYear + it }.forEach { year ->
            val override = congregation.yearlyMeetingTimes[year]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = override != null,
                    onCheckedChange = { checked ->
                        val times = congregation.yearlyMeetingTimes
                        onChange(
                            congregation.copy(
                                yearlyMeetingTimes = if (checked) {
                                    times + (year to congregation.defaultMeetingTimes)
                                } else {
                                    times - year
                                },
                            ),
                        )
                    },
                )
                Text("$year: abweichende Zeiten")
            }
            if (override != null) {
                fun update(times: MeetingTimes) =
                    onChange(congregation.copy(yearlyMeetingTimes = congregation.yearlyMeetingTimes + (year to times)))
                WeekdaySelector("Unter der Woche ($year): Wochentag", override.midweekWeekday) {
                    update(override.copy(midweekWeekday = it))
                }
                MeetingTimeInput("Unter der Woche ($year): Uhrzeit", override.midweekTime) {
                    update(override.copy(midweekTime = it))
                }
                WeekdaySelector("Wochenende ($year): Wochentag", override.weekendWeekday) {
                    update(override.copy(weekendWeekday = it))
                }
                MeetingTimeInput("Wochenende ($year): Uhrzeit", override.weekendTime) {
                    update(override.copy(weekendTime = it))
                }
            }
        }
    }
}
