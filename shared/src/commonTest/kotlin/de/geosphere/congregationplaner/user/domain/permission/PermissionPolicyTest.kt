package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
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
    fun `unapproved congregation admin and other roles may not manage users`() {
        val users = listOf(AppUser(uid = "1", approved = false, role = UserRole.CONGREGATION_ADMIN)) +
            listOf(UserRole.MEMBER, UserRole.NONE).map { role -> AppUser(uid = "2", approved = true, role = role) }

        users.forEach { user -> assertFalse(userPolicy.canManageUsers(user), "canManageUsers for $user") }
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
    fun `unknown role and permission values are denied`() {
        assertEquals(UserRole.CONGREGATION_ADMIN, UserRole.fromValue("CONGREGATION_ADMIN"))
        assertEquals(UserRole.NONE, UserRole.fromValue("ADMIN"))
        assertEquals(UserRole.NONE, UserRole.fromValue(null))
        assertEquals(setOf(UserPermission.DUMMY), UserPermission.fromValues(listOf("DUMMY", "SUPERUSER", null)))
    }
}
