package de.geosphere.congregationplaner.presentation.navigation

import de.geosphere.congregationplaner.event.domain.permission.SpecialEventPolicy
import de.geosphere.congregationplaner.midweek.domain.permission.MidweekPermissionPolicy
import de.geosphere.congregationplaner.notification.domain.model.NotificationCategory
import de.geosphere.congregationplaner.notification.domain.permission.NotificationPermissionPolicy
import de.geosphere.congregationplaner.organization.domain.permission.OrganizationPermissionPolicy
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import dev.zacsweers.metro.Inject

/**
 * Leitet die sichtbaren Bereiche ausschließlich aus den bestehenden Policies ab, damit Navigation und
 * Berechtigungen nicht auseinanderlaufen.
 */
@Inject
class NavigationAccess(
    private val userPolicy: UserPermissionPolicy,
    private val talkSchedulePolicy: TalkSchedulePermissionPolicy,
    private val midweekPolicy: MidweekPermissionPolicy,
    private val organizationPolicy: OrganizationPermissionPolicy,
    private val talkReferencePolicy: TalkReferenceDataPolicy,
    private val specialEventPolicy: SpecialEventPolicy,
) {
    // Zustandslos wie alle Policies; ein eigener Konstruktorparameter wäre der siebte.
    private val notificationPolicy = NotificationPermissionPolicy(talkSchedulePolicy)

    fun visibleFor(user: AppUser): VisibleNavigation = VisibleNavigation(
        congregationSections = CongregationSection.entries.filter { canSee(user, it) },
        globalSections = GlobalSection.entries.filter { canSee(user, it) },
    )

    private fun canSee(user: AppUser, section: CongregationSection): Boolean = when (section) {
        CongregationSection.GENERAL_DATA,
        CongregationSection.USER_MANAGEMENT,
        -> userPolicy.canManageUsers(user)

        CongregationSection.SPECIAL_EVENTS,
        CongregationSection.EVENT_TYPES,
        -> specialEventPolicy.canManage(user)

        CongregationSection.ANNOUNCEMENTS -> notificationPolicy.canSend(user, NotificationCategory.ANNOUNCEMENT)

        CongregationSection.MIDWEEK_PLANNING -> midweekPolicy.canPlanMidweek(user)

        CongregationSection.WEEKEND_TALKS,
        CongregationSection.EXTERNAL_SPEAKERS,
        CongregationSection.TALK_HISTORY,
        -> talkSchedulePolicy.canViewTalkPlanning(user)
    }

    private fun canSee(user: AppUser, section: GlobalSection): Boolean = when (section) {
        GlobalSection.ORGANIZATION -> organizationPolicy.canManage(user)
        GlobalSection.TALK_REFERENCE -> talkReferencePolicy.canManage(user)
    }
}
