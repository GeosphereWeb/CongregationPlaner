package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionPolicyTest {
    private val congregationPolicy = CongregationPermissionPolicy()
    private val userPolicy = UserPermissionPolicy()
    private val congregation = CongregationMetadata(id = "609a", name = "Berlin")

    @Test
    fun `global admin may manage congregations and users`() {
        val admin = AppUser(uid = "1", isGlobalAdmin = true)

        assertTrue(congregationPolicy.canCreate(admin))
        assertTrue(congregationPolicy.canEdit(admin, congregation))
        assertTrue(congregationPolicy.canDelete(admin, congregation))
        assertTrue(congregationPolicy.canManageGeneral(admin))
        assertTrue(userPolicy.canManageUsers(admin))
    }

    @Test
    fun `approved congregation admin may manage users but not congregations`() {
        val admin = AppUser(uid = "1", approved = true, role = UserRole.CONGREGATION_ADMIN)

        assertTrue(userPolicy.canManageUsers(admin))
        assertFalse(congregationPolicy.canCreate(admin))
        assertFalse(congregationPolicy.canEdit(admin, congregation))
        assertFalse(congregationPolicy.canDelete(admin, congregation))
        assertFalse(congregationPolicy.canManageGeneral(admin))
    }

    @Test
    fun `congregation admin may edit only the own congregation`() {
        val own = CongregationRef("r", "s", "b", "c", "609a")
        val admin = AppUser(uid = "1", approved = true, role = UserRole.CONGREGATION_ADMIN, congregation = own)

        assertTrue(congregationPolicy.canEdit(admin, congregation))
        assertFalse(congregationPolicy.canEdit(admin, congregation.copy(id = "other")))
        assertFalse(congregationPolicy.canEdit(admin.copy(approved = false), congregation))
        assertFalse(congregationPolicy.canEdit(admin.copy(role = UserRole.MEMBER), congregation))
        assertFalse(congregationPolicy.canDelete(admin, congregation))
    }

    @Test
    fun `unapproved congregation admin and other roles may not manage users`() {
        val users = listOf(AppUser(uid = "1", approved = false, role = UserRole.CONGREGATION_ADMIN)) +
            listOf(UserRole.MEMBER, UserRole.NONE).map { role -> AppUser(uid = "2", approved = true, role = role) }

        users.forEach { user -> assertFalse(userPolicy.canManageUsers(user), "canManageUsers for $user") }
    }

    @Test
    fun `viewing members is a separate permission besides the admin rights`() {
        val viewer = AppUser(uid = "1", approved = true, permissions = setOf(UserPermission.VIEW_MEMBERS))

        assertTrue(userPolicy.canViewMembers(viewer))
        assertTrue(userPolicy.canViewMembers(AppUser(uid = "2", isGlobalAdmin = true)))
        assertTrue(userPolicy.canViewMembers(AppUser(uid = "3", approved = true, role = UserRole.CONGREGATION_ADMIN)))
        // PLAN_TALKS und eine fehlende Freigabe genügen nicht; Lesen heißt nicht Bearbeiten.
        assertFalse(userPolicy.canViewMembers(viewer.copy(permissions = setOf(UserPermission.PLAN_TALKS))))
        assertFalse(userPolicy.canViewMembers(viewer.copy(approved = false)))
        assertFalse(userPolicy.canManageUsers(viewer))
        assertFalse(userPolicy.canEditMember(viewer, AppUser(uid = "9")))
        assertEquals(setOf(UserPermission.VIEW_MEMBERS), UserPermission.fromValues(listOf("VIEW_MEMBERS", "OTHER")))
    }

    @Test
    fun `admins may edit other members but not themselves`() {
        val admin = AppUser(uid = "1", approved = true, role = UserRole.CONGREGATION_ADMIN)
        val member = AppUser(uid = "2", approved = true)

        assertTrue(userPolicy.canEditMember(admin, member))
        assertFalse(userPolicy.canEditMember(admin, admin))
        assertFalse(userPolicy.canEditMember(member, admin))
    }

    @Test
    fun `admins may change permissions of members and their own`() {
        val admin = AppUser(uid = "1", approved = true, role = UserRole.CONGREGATION_ADMIN)
        val member = AppUser(uid = "2", approved = true)

        assertTrue(userPolicy.canEditPermissions(admin, member))
        assertTrue(userPolicy.canEditPermissions(admin, admin))
        assertFalse(userPolicy.canEditPermissions(member, admin))
        assertFalse(userPolicy.canEditPermissions(member, member))
    }

    @Test
    fun `admins maintain profiles including their own and record members without account`() {
        val admin = AppUser(uid = "1", approved = true, role = UserRole.CONGREGATION_ADMIN)
        val member = AppUser(uid = "2", approved = true)

        assertTrue(userPolicy.canEditProfile(admin, member))
        assertTrue(userPolicy.canEditProfile(admin, admin))
        assertTrue(userPolicy.canCreateMemberWithoutAccount(admin))
        assertFalse(userPolicy.canEditProfile(member, member))
        assertFalse(userPolicy.canCreateMemberWithoutAccount(member))
    }

    @Test
    fun `unknown role and permission values are denied`() {
        assertEquals(UserRole.CONGREGATION_ADMIN, UserRole.fromValue("CONGREGATION_ADMIN"))
        assertEquals(UserRole.NONE, UserRole.fromValue("ADMIN"))
        assertEquals(UserRole.NONE, UserRole.fromValue(null))
        assertEquals(setOf(UserPermission.DUMMY), UserPermission.fromValues(listOf("DUMMY", "SUPERUSER", null)))
    }
}
