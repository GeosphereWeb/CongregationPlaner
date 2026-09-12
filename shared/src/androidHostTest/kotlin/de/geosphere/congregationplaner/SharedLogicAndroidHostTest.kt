package de.geosphere.congregationplaner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SharedLogicAndroidHostTest {
    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }

    @Test
    fun `android firebase platform support initializes safely without context`() {
        FirebaseAndroidContextHolder.context = null

        val support = FirebasePlatformSupport()
        support.initialize()

        assertFalse(support.isReady())
    }

    @Test
    fun `android firebase holder safely handles null context`() {
        FirebaseAndroidContextHolder.context = null

        assertNull(FirebaseAndroidContextHolder.context)
        assertFalse(FirebasePlatformSupport().isReady())
    }
}
