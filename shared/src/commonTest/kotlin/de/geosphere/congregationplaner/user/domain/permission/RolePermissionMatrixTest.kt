package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.congregation.domain.permission.CongregationPermissionPolicy
import de.geosphere.congregationplaner.midweek.domain.permission.MidweekPermissionPolicy
import de.geosphere.congregationplaner.organization.domain.permission.OrganizationPermissionPolicy
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Rolle × Berechtigung über alle Policies, als Gegenstück zum Firestore-Rules-Test: Jede Zeile nennt die
 * Berechtigungen, die eine Person hat; alle übrigen hat sie nicht.
 */
class RolePermissionMatrixTest {
    private val user = UserPermissionPolicy()
    private val talk = TalkSchedulePermissionPolicy()
    private val reference = TalkReferenceDataPolicy()
    private val midweek = MidweekPermissionPolicy()
    private val organization = OrganizationPermissionPolicy()
    private val congregation = CongregationPermissionPolicy()

    private enum class Permission {
        MANAGE_USERS,
        VIEW_MEMBERS,
        PLAN_TALKS,
        VIEW_TALK_PLANNING,
        CONFIRM_SPEAKER,
        PLAN_MIDWEEK,
        MANAGE_TALK_REFERENCE,
        MANAGE_ORGANIZATION,
        MANAGE_CONGREGATIONS,
    }

    private fun granted(person: AppUser): Set<Permission> = Permission.entries.filterTo(mutableSetOf()) {
        when (it) {
            Permission.MANAGE_USERS -> user.canManageUsers(person)
            Permission.VIEW_MEMBERS -> user.canViewMembers(person)
            Permission.PLAN_TALKS -> talk.canPlanTalks(person)
            Permission.VIEW_TALK_PLANNING -> talk.canViewTalkPlanning(person)
            Permission.CONFIRM_SPEAKER -> talk.canConfirmSpeaker(person)
            Permission.PLAN_MIDWEEK -> midweek.canPlanMidweek(person)
            Permission.MANAGE_TALK_REFERENCE -> reference.canManage(person)
            Permission.MANAGE_ORGANIZATION -> organization.canManage(person)
            Permission.MANAGE_CONGREGATIONS -> congregation.canManageGeneral(person)
        }
    }

    private fun approved(role: CongregationRole) = AppUser(uid = "u", approved = true, roles = setOf(role))

    @Test
    fun `every person gets exactly the permissions of the own roles`() {
        val talkReading = setOf(Permission.VIEW_MEMBERS, Permission.VIEW_TALK_PLANNING, Permission.CONFIRM_SPEAKER)
        val matrix = listOf(
            "member without role" to AppUser(uid = "u", approved = true) to emptySet(),
            "unapproved admin" to AppUser(uid = "u", roles = CongregationRole.entries.toSet()) to emptySet(),
            "congregation admin" to approved(CongregationRole.CONGREGATION_ADMIN) to
                setOf(Permission.MANAGE_USERS, Permission.VIEW_MEMBERS),
            "talk planner" to approved(CongregationRole.TALK_PLANNER) to talkReading + Permission.PLAN_TALKS,
            "talk helper" to approved(CongregationRole.TALK_HELPER) to talkReading,
            "task planner" to approved(CongregationRole.TASK_PLANNER) to
                setOf(Permission.VIEW_MEMBERS, Permission.PLAN_MIDWEEK),
            "global admin" to AppUser(uid = "u", isGlobalAdmin = true) to setOf(
                Permission.MANAGE_TALK_REFERENCE,
                Permission.MANAGE_ORGANIZATION,
                Permission.MANAGE_CONGREGATIONS,
            ),
        )

        matrix.forEach { (named, expected) ->
            val (name, person) = named
            assertEquals(expected, granted(person), name)
        }
    }
}
