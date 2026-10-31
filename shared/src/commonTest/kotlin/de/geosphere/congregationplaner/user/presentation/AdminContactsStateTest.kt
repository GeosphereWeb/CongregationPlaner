package de.geosphere.congregationplaner.user.presentation

import de.geosphere.congregationplaner.user.domain.model.AdminContact
import kotlin.test.Test
import kotlin.test.assertEquals

class AdminContactsStateTest {
    @Test
    fun `contact shows first and last name`() {
        assertEquals("Anna Admin", AdminContact("a", "Anna", "Admin").displayName())
    }

    @Test
    fun `contact without a name says so`() {
        assertEquals("Name nicht hinterlegt", AdminContact("a", "", " ").displayName())
    }

    @Test
    fun `texts match the agreed wording`() {
        assertEquals("kein Admin vorhanden", NO_ADMIN_TEXT)
    }
}
