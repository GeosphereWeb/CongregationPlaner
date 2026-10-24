package de.geosphere.congregationplaner.talk.domain.permission

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TalkReferenceDataPolicyTest {
    private val policy = TalkReferenceDataPolicy()

    @Test
    fun `only global admins may manage the talk list and week events`() {
        assertTrue(policy.canManage(AppUser(uid = "1", isGlobalAdmin = true)))
        assertFalse(policy.canManage(AppUser(uid = "2", approved = true, role = UserRole.CONGREGATION_ADMIN)))
        assertFalse(
            policy.canManage(
                AppUser(uid = "3", approved = true, permissions = setOf(UserPermission.PLAN_TALKS)),
            ),
        )
    }
}
