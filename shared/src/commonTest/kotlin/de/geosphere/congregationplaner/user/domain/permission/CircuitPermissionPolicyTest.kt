package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CircuitPermissionPolicyTest {
    private val policy = CircuitPermissionPolicy()
    private val circuit = Circuit(id = "609a", name = "Nord")

    @Test
    fun `approved admin may manage circuits`() {
        val admin = AppUser(uid = "1", approved = true, role = UserRole.ADMIN)

        assertTrue(policy.canCreate(admin))
        assertTrue(policy.canEdit(admin, circuit))
        assertTrue(policy.canDelete(admin, circuit))
        assertTrue(policy.canManageGeneral(admin))
    }

    @Test
    fun `unapproved admin and non admin roles get no permissions`() {
        val users = listOf(AppUser(uid = "1", approved = false, role = UserRole.ADMIN)) +
            listOf(UserRole.SPEAKING_PLANER, UserRole.SPEAKING_ASSISTANT, UserRole.NONE).map { role ->
                AppUser(uid = "2", approved = true, role = role)
            }

        users.forEach { user ->
            assertFalse(policy.canCreate(user), "canCreate for $user")
            assertFalse(policy.canEdit(user, circuit), "canEdit for $user")
            assertFalse(policy.canDelete(user, circuit), "canDelete for $user")
            assertFalse(policy.canManageGeneral(user), "canManageGeneral for $user")
        }
    }
}
