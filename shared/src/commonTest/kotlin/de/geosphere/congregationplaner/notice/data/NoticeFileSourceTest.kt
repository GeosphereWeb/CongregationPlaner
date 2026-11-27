package de.geosphere.congregationplaner.notice.data

import kotlin.test.Test
import kotlin.test.assertEquals

class NoticeFileSourceTest {
    @Test
    fun `picked name drops the extension and is cut to 60 characters`() {
        assertEquals("Logo", pickedPngName("Logo.png"))
        assertEquals("a.b", pickedPngName("a.b.png"))
        assertEquals("Logo", pickedPngName("Logo"))
        assertEquals("x".repeat(60), pickedPngName("${"x".repeat(80)}.png"))
    }
}
