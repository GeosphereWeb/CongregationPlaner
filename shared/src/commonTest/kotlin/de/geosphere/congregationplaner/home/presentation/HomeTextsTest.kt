package de.geosphere.congregationplaner.home.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeTextsTest {
    @Test
    fun `the talk section names the helper role`() {
        assertEquals("Als Vortragsplaner", talkSectionLabel(asHelper = false))
        assertEquals("Als Vortragsplanungs-Helfer", talkSectionLabel(asHelper = true))
    }
}
