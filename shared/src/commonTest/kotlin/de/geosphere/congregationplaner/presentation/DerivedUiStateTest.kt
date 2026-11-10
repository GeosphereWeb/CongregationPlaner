package de.geosphere.congregationplaner.presentation

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.organization.presentation.selection.CongregationSelectionUiState
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.presentation.management.MemberEditorState
import de.geosphere.congregationplaner.user.presentation.management.UserManagementUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Werte, die der UI-Zustand ableitet, statt dass die Composables sie berechnen. */
class DerivedUiStateTest {
    @Test
    fun `an invite link can be applied once entered and while none is resolving`() {
        assertFalse(CongregationSelectionUiState().canApplyInviteLink)
        assertFalse(CongregationSelectionUiState(inviteLinkInput = " ").canApplyInviteLink)
        assertTrue(CongregationSelectionUiState(inviteLinkInput = "link").canApplyInviteLink)
        assertFalse(CongregationSelectionUiState(inviteLinkInput = "link", isResolvingLink = true).canApplyInviteLink)
    }

    @Test
    fun `a congregation without id is new`() {
        assertTrue(CongregationMetadata(name = "Nord").isNew)
        assertFalse(CongregationMetadata(id = "k1", name = "Nord").isNew)
    }

    @Test
    fun `the selected member follows the list`() {
        val member = AppUser(uid = "1")
        val approved = member.copy(approved = true)

        assertNull(UserManagementUiState().selectedMember)
        assertEquals(
            approved,
            UserManagementUiState(
                members = listOf(approved),
                editor = MemberEditorState(member = member),
            ).selectedMember,
        )
        assertEquals(member, UserManagementUiState(editor = MemberEditorState(member = member)).selectedMember)
    }
}
