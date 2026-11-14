package de.geosphere.congregationplaner.user.presentation.management

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorFamilyTest {
    private val max = AppUser(uid = "max", profile = MemberProfile(lastName = "Muster", firstName = "Max"))
    private val anna = AppUser(
        uid = "anna",
        profile = MemberProfile(lastName = "Muster", firstName = "Anna", familyHeadId = "max").normalized(),
    )
    private val tim = AppUser(
        uid = "tim",
        profile = MemberProfile(lastName = "Muster", firstName = "Tim", familyHeadId = "max").normalized(),
    )
    private val state = UserManagementUiState(members = listOf(max, anna, tim))

    @Test
    fun `a head sees his family and cannot choose a head`() {
        val family = state.copy(editor = MemberEditorState(max.profile, max)).editorFamily

        assertTrue(family.isHead)
        assertEquals("Anna Muster, Tim Muster", family.relativesText)
        assertTrue(family.candidates.isEmpty())
    }

    @Test
    fun `a relative sees the chosen head and the candidates`() {
        val family = state.copy(editor = MemberEditorState(anna.profile, anna)).editorFamily

        assertFalse(family.isHead)
        assertEquals(max, family.head)
        assertEquals(listOf(max), family.candidates)
    }

    @Test
    fun `a new person may choose every head`() {
        val family = state.copy(editor = MemberEditorState()).editorFamily

        assertEquals(null, family.head)
        assertEquals(listOf(max), family.candidates)
        assertFalse(family.isHead)
    }
}
