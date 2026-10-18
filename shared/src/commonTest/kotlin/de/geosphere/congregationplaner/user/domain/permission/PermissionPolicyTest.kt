package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.user.domain.model.AppUser
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
    fun `approved admin may manage congregations and users`() {
        val admin = AppUser(uid = "1", approved = true, role = UserRole.ADMIN)

        assertTrue(congregationPolicy.canCreate(admin))
        assertTrue(congregationPolicy.canEdit(admin, congregation))
        assertTrue(congregationPolicy.canDelete(admin, congregation))
        assertTrue(congregationPolicy.canManageGeneral(admin))
        assertTrue(userPolicy.canManageUsers(admin))
    }

    @Test
    fun `unapproved admin gets no permissions`() {
        val admin = AppUser(uid = "1", approved = false, role = UserRole.ADMIN)

        assertFalse(congregationPolicy.canCreate(admin))
        assertFalse(congregationPolicy.canEdit(admin, congregation))
        assertFalse(congregationPolicy.canDelete(admin, congregation))
        assertFalse(congregationPolicy.canManageGeneral(admin))
        assertFalse(userPolicy.canManageUsers(admin))
    }

    @Test
    fun `non admin roles may not modify congregations or manage users`() {
        listOf(UserRole.SPEAKING_PLANER, UserRole.SPEAKING_ASSISTANT, UserRole.NONE).forEach { role ->
            val user = AppUser(uid = "2", approved = true, role = role)

            assertFalse(congregationPolicy.canCreate(user), "canCreate for $role")
            assertFalse(congregationPolicy.canEdit(user, congregation), "canEdit for $role")
            assertFalse(congregationPolicy.canDelete(user, congregation), "canDelete for $role")
            assertFalse(congregationPolicy.canManageGeneral(user), "canManageGeneral for $role")
            assertFalse(userPolicy.canManageUsers(user), "canManageUsers for $role")
        }
    }

    @Test
    fun `unknown role values map to NONE`() {
        assertEquals(UserRole.ADMIN, UserRole.fromValue("ADMIN"))
        assertEquals(UserRole.NONE, UserRole.fromValue("SUPER_ADMIN"))
        assertEquals(UserRole.NONE, UserRole.fromValue(null))
    }
}
