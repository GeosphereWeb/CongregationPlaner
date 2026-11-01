package de.geosphere.congregationplaner.presentation.navigation

import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.permission.CircuitPermissionPolicy
import de.geosphere.congregationplaner.user.domain.permission.CongregationPermissionPolicy
import de.geosphere.congregationplaner.user.domain.permission.MidweekPermissionPolicy
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import dev.zacsweers.metro.Inject

/**
 * Leitet die sichtbaren Bereiche ausschließlich aus den bestehenden Policies ab, damit Navigation und
 * Berechtigungen nicht auseinanderlaufen.
 */
@Inject
class NavigationAccess(
    private val userPolicy: UserPermissionPolicy,
    private val congregationPolicy: CongregationPermissionPolicy,
    private val talkSchedulePolicy: TalkSchedulePermissionPolicy,
    private val midweekPolicy: MidweekPermissionPolicy,
    private val circuitPolicy: CircuitPermissionPolicy,
    private val talkReferencePolicy: TalkReferenceDataPolicy,
) {
    fun visibleFor(user: AppUser): VisibleNavigation = VisibleNavigation(
        congregationSections = CongregationSection.entries.filter { canSee(user, it) },
        globalSections = GlobalSection.entries.filter { canSee(user, it) },
    )

    private fun canSee(user: AppUser, section: CongregationSection): Boolean = when (section) {
        CongregationSection.GENERAL_DATA,
        CongregationSection.USER_MANAGEMENT,
        -> userPolicy.canManageUsers(user)

        CongregationSection.MIDWEEK_PLANNING -> midweekPolicy.canPlanMidweek(user)

        CongregationSection.WEEKEND_TALKS,
        CongregationSection.EXTERNAL_SPEAKERS,
        CongregationSection.TALK_HISTORY,
        -> talkSchedulePolicy.canPlanTalks(user)
    }

    private fun canSee(user: AppUser, section: GlobalSection): Boolean = when (section) {
        GlobalSection.CIRCUITS -> circuitPolicy.canManageGeneral(user)
        GlobalSection.CONGREGATIONS -> congregationPolicy.canManageGeneral(user)
        GlobalSection.TALK_REFERENCE -> talkReferencePolicy.canManage(user)
    }
}
