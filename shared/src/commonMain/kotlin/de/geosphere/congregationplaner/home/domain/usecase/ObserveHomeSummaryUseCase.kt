package de.geosphere.congregationplaner.home.domain.usecase

import de.geosphere.congregationplaner.home.domain.model.HomeSummary
import de.geosphere.congregationplaner.home.domain.model.TalkSummary
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkYearUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMembersUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Beobachtet die Daten der Startseite. Jeder Block wird nur abgefragt, wenn die Policy es erlaubt: die Termine für
 * Vortragsplaner, die offenen Freigaben für Versammlungsadmins.
 */
@Inject
class ObserveHomeSummaryUseCase(
    private val talkPolicy: TalkSchedulePermissionPolicy,
    private val userPolicy: UserPermissionPolicy,
    private val observeTalkYear: ObserveTalkYearUseCase,
    private val observeMembers: ObserveMembersUseCase,
) {
    operator fun invoke(user: AppUser, today: LocalDate): Flow<HomeSummary> {
        val congregation = user.congregation
        val talk: Flow<TalkSummary?> = if (talkPolicy.canPlanTalks(user) && congregation != null) {
            observeSlots(user, today).map { slots -> summarizeTalks(slots, today) }
        } else {
            flowOf(null)
        }
        val approvals: Flow<Int?> = if (userPolicy.canManageUsers(user) && congregation != null) {
            observeMembers(congregation).map { members -> members.count { it.hasAccount && !it.approved } }
        } else {
            flowOf(null)
        }
        return combine(talk, approvals) { talkSummary, pending -> HomeSummary(talkSummary, pending) }
    }

    /** Das Fenster von vier Wochen kann über den Jahreswechsel reichen; dann kommt das Folgejahr dazu. */
    private fun observeSlots(user: AppUser, today: LocalDate): Flow<List<TalkSlot>> {
        val years = setOf(today.year, horizonEnd(today).year)
        val flows = years.map { observeTalkYear(user, it) }
        return if (flows.isEmpty()) emptyFlow() else combine(flows) { lists -> lists.toList().flatten() }
    }
}

private fun horizonEnd(today: LocalDate): LocalDate = today.plus(DatePeriod(days = TalkSummary.HORIZON_DAYS))

/**
 * Der nächste Termin und die Lücken der nächsten vier Wochen. Termine in der Vergangenheit und abgesagte Termine
 * zählen nicht; ein Termin ist offen, wenn Vortrag oder Redner fehlen.
 */
internal fun summarizeTalks(slots: List<TalkSlot>, today: LocalDate): TalkSummary {
    val upcoming = slots.distinctBy { it.date }.filter { it.date >= today && !it.talkCancelled }.sortedBy { it.date }
    val end = horizonEnd(today)
    return TalkSummary(
        nextSlot = upcoming.firstOrNull(),
        openDates = upcoming.filter { it.date < end && (it.talkNumber == null || !it.hasSpeaker) }.map { it.date },
    )
}
