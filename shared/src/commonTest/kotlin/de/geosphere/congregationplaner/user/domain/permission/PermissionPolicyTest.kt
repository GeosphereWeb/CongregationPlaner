package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionPolicyTest {
    private val congregationPolicy = CongregationPermissionPolicy()
    private val userPolicy = UserPermissionPolicy()
    private val congregation = CongregationMetadata(id = "609a", name = "Berlin")

    @Test
    fun `global admin manages congregations but has no access to their members`() {
        val admin = AppUser(uid = "1", approved = true, isGlobalAdmin = true)

        assertTrue(congregationPolicy.canCreate(admin))
        assertTrue(congregationPolicy.canEdit(admin, congregation))
        assertTrue(congregationPolicy.canDelete(admin, congregation))
        assertTrue(congregationPolicy.canManageGeneral(admin))
        assertFalse(userPolicy.canManageUsers(admin))
        assertFalse(userPolicy.canViewMembers(admin))
        assertFalse(userPolicy.canCreateMemberWithoutAccount(admin))
    }

    @Test
    fun `global admin changes the name but not the meeting times`() {
        val admin = AppUser(uid = "1", approved = true, isGlobalAdmin = true)
        val own = CongregationRef("r", "s", "b", "c", "609a")
        val congregationAdmin = AppUser(
            uid = "2",
            approved = true,
            roles = setOf(CongregationRole.CONGREGATION_ADMIN),
            congregation = own,
        )

        assertTrue(congregationPolicy.canEdit(admin, congregation))
        assertFalse(congregationPolicy.canEditMeetingTimes(admin, congregation))
        assertTrue(congregationPolicy.canEditMeetingTimes(congregationAdmin, congregation))
        assertFalse(congregationPolicy.canEditMeetingTimes(congregationAdmin, congregation.copy(id = "other")))
        assertFalse(congregationPolicy.canEditMeetingTimes(congregationAdmin.copy(approved = false), congregation))
    }

    @Test
    fun `global admin with roles uses them like everybody else`() {
        val admin = AppUser(
            uid = "1",
            approved = true,
            isGlobalAdmin = true,
            roles = setOf(CongregationRole.TALK_PLANNER),
        )

        assertTrue(userPolicy.canViewMembers(admin))
        assertFalse(userPolicy.canManageUsers(admin))
    }

    @Test
    fun `approved congregation admin may manage users but not congregations`() {
        val admin = AppUser(uid = "1", approved = true, roles = setOf(CongregationRole.CONGREGATION_ADMIN))

        assertTrue(userPolicy.canManageUsers(admin))
        assertFalse(congregationPolicy.canCreate(admin))
        assertFalse(congregationPolicy.canEdit(admin, congregation))
        assertFalse(congregationPolicy.canDelete(admin, congregation))
        assertFalse(congregationPolicy.canManageGeneral(admin))
    }

    @Test
    fun `congregation admin may edit only the own congregation`() {
        val own = CongregationRef("r", "s", "b", "c", "609a")
        val admin = AppUser(
            uid = "1",
            approved = true,
            roles = setOf(CongregationRole.CONGREGATION_ADMIN),
            congregation = own,
        )

        assertTrue(congregationPolicy.canEdit(admin, congregation))
        assertFalse(congregationPolicy.canEdit(admin, congregation.copy(id = "other")))
        assertFalse(congregationPolicy.canEdit(admin.copy(approved = false), congregation))
        assertFalse(congregationPolicy.canEdit(admin.copy(roles = emptySet()), congregation))
        assertFalse(congregationPolicy.canDelete(admin, congregation))
    }

    @Test
    fun `unapproved congregation admin and other roles may not manage users`() {
        val users = listOf(AppUser(uid = "1", approved = false, roles = setOf(CongregationRole.CONGREGATION_ADMIN))) +
            listOf(CongregationRole.TALK_PLANNER, CongregationRole.TASK_PLANNER)
                .map { role -> AppUser(uid = "2", approved = true, roles = setOf(role)) } +
            AppUser(uid = "3", approved = true)

        users.forEach { user -> assertFalse(userPolicy.canManageUsers(user), "canManageUsers for $user") }
    }

    @Test
    fun `admins and approved planners may view members`() {
        val talkPlanner = AppUser(uid = "1", approved = true, roles = setOf(CongregationRole.TALK_PLANNER))
        val taskPlanner = AppUser(uid = "2", approved = true, roles = setOf(CongregationRole.TASK_PLANNER))
        val admin = AppUser(uid = "3", approved = true, roles = setOf(CongregationRole.CONGREGATION_ADMIN))

        assertTrue(userPolicy.canViewMembers(talkPlanner))
        assertTrue(userPolicy.canViewMembers(taskPlanner))
        assertTrue(userPolicy.canViewMembers(admin))
        assertFalse(userPolicy.canViewMembers(AppUser(uid = "4", approved = true, isGlobalAdmin = true)))
    }

    @Test
    fun `viewing members needs approval and a role and does not allow editing`() {
        val planner = AppUser(uid = "1", approved = true, roles = setOf(CongregationRole.TALK_PLANNER))

        assertFalse(userPolicy.canViewMembers(planner.copy(approved = false)))
        assertFalse(userPolicy.canViewMembers(AppUser(uid = "2", approved = true)))
        assertFalse(userPolicy.canManageUsers(planner))
        assertFalse(userPolicy.canEditMember(planner, AppUser(uid = "9")))
        assertFalse(userPolicy.canEditRoles(planner, planner))
    }

    @Test
    fun `admins may edit other members but not themselves`() {
        val admin = AppUser(uid = "1", approved = true, roles = setOf(CongregationRole.CONGREGATION_ADMIN))
        val member = AppUser(uid = "2", approved = true)

        assertTrue(userPolicy.canEditMember(admin, member))
        assertFalse(userPolicy.canEditMember(admin, admin))
        assertFalse(userPolicy.canEditMember(member, admin))
    }

    @Test
    fun `admins may change roles of members and their own`() {
        val admin = AppUser(uid = "1", approved = true, roles = setOf(CongregationRole.CONGREGATION_ADMIN))
        val member = AppUser(uid = "2", approved = true)

        assertTrue(userPolicy.canEditRoles(admin, member))
        assertTrue(userPolicy.canEditRoles(admin, admin))
        assertFalse(userPolicy.canEditRoles(member, admin))
        assertFalse(userPolicy.canEditRoles(member, member))
    }

    @Test
    fun `admins maintain profiles including their own and record members without account`() {
        val admin = AppUser(uid = "1", approved = true, roles = setOf(CongregationRole.CONGREGATION_ADMIN))
        val member = AppUser(uid = "2", approved = true)

        assertTrue(userPolicy.canEditProfile(admin, member))
        assertTrue(userPolicy.canEditProfile(admin, admin))
        assertTrue(userPolicy.canCreateMemberWithoutAccount(admin))
        assertFalse(userPolicy.canEditProfile(member, member))
        assertFalse(userPolicy.canCreateMemberWithoutAccount(member))
    }

    @Test
    fun `unknown role values are ignored`() {
        assertEquals(
            setOf(CongregationRole.TASK_PLANNER),
            CongregationRole.fromValues(listOf("TASK_PLANNER", "SUPERUSER", null)),
        )
    }

    @Test
    fun `only approved task planners may plan midweek`() {
        val policy = MidweekPermissionPolicy()
        val planner = AppUser(uid = "1", approved = true, roles = setOf(CongregationRole.TASK_PLANNER))

        assertTrue(policy.canPlanMidweek(planner))
        assertFalse(policy.canPlanMidweek(planner.copy(approved = false)))
        assertFalse(policy.canPlanMidweek(planner.copy(roles = setOf(CongregationRole.CONGREGATION_ADMIN))))
        assertFalse(policy.canPlanMidweek(AppUser(uid = "2", isGlobalAdmin = true)))
    }
}
