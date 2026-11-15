package de.geosphere.congregationplaner.notification.domain.usecase

import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import de.geosphere.congregationplaner.organization.domain.repository.CongregationSelectionStore
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

/**
 * Ergebnis der Prüfung: die Termine, deren Redner noch nicht informiert ist, und ob jetzt erinnert werden soll.
 * Ohne offene Termine entfernt das Gerät eine noch sichtbare Erinnerung.
 */
data class SpeakerReminderCheck(val open: List<TalkSlot>, val remindNow: Boolean) {
    val title: String get() = "Redner informieren"

    /** Z. B. „22.11.2026: Muster, Max ist noch nicht informiert.“, je offenem Termin eine Zeile. */
    val body: String
        get() = open.joinToString("\n") { slot ->
            "${slot.date.dayText()}: ${slot.speakerDisplayName ?: "Der Redner"} ist noch nicht informiert."
        }
}

/**
 * Erinnert Vortragsplaner und Helfer daran, den Redner zu informieren (CP-167): ab dem Montag der Vortragswoche bis
 * zum Vortragstag, solange der Termin einen Redner hat, nicht abgesagt und nicht „Informiert“ ist. Erinnert wird
 * einmal am Tag ([remindedOn] ist der Tag der letzten Erinnerung) und nicht vor [REMIND_FROM_HOUR] Uhr.
 */
@Inject
class CheckSpeakerRemindersUseCase(
    private val auth: FirebaseAuthRepository,
    private val selection: CongregationSelectionStore,
    private val users: UserRepository,
    private val schedule: TalkScheduleRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(now: LocalDateTime, remindedOn: LocalDate?): Result<SpeakerReminderCheck> =
        runCatching {
            val uid = auth.currentUserId()
            val congregation = selection.load()?.ref
            val user = if (uid != null && congregation != null) users.observeUser(congregation, uid).first() else null
            if (congregation == null || user == null || !policy.canConfirmSpeaker(user)) {
                SpeakerReminderCheck(emptyList(), remindNow = false)
            } else {
                val today = now.date
                // Die Woche kann über den Jahreswechsel gehen.
                val years = setOf(today.year, today.plus(DatePeriod(days = DAYS_AHEAD)).year)
                val slots = years.flatMap { schedule.observeYear(congregation, it).first() }
                val open = slots.filter { it.needsReminder(today) }.sortedBy { it.date }
                SpeakerReminderCheck(
                    open = open,
                    remindNow = open.isNotEmpty() && remindedOn != today && now.hour >= REMIND_FROM_HOUR,
                )
            }
        }

    companion object {
        /** Frühestens um 8 Uhr, damit niemand nachts geweckt wird. */
        const val REMIND_FROM_HOUR = 8
        private const val DAYS_AHEAD = 6
    }
}

/** Ab dem Montag der Woche des Termins bis zum Termin selbst, mit Redner, nicht abgesagt, nicht informiert. */
internal fun TalkSlot.needsReminder(today: LocalDate): Boolean {
    val monday = date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - 1))
    return today in monday..date && hasSpeaker && !talkCancelled && !confirmed
}

private fun LocalDate.dayText(): String =
    "${day.toString().padStart(2, '0')}.${month.number.toString().padStart(2, '0')}.$year"
