package de.geosphere.congregationplaner.notice.domain.model

import de.geosphere.congregationplaner.notice.testing.testPng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PngSizeTest {
    @Test
    fun `reads width and height from the IHDR chunk`() {
        assertEquals(640 to 480, pngSize(testPng(640, 480)))
        assertEquals(70_000 to 3, pngSize(testPng(70_000, 3)))
    }

    @Test
    fun `anything but a PNG with an IHDR chunk has no size`() {
        assertNull(pngSize(byteArrayOf()))
        assertNull(pngSize(testPng().copyOf(23)))
        assertNull(pngSize(testPng().also { it[0] = 0 }))
        assertNull(pngSize(testPng().also { it[12] = 'X'.code.toByte() }))
    }

    @Test
    fun `an image name is valid with 1 to 60 characters after trimming`() {
        assertTrue(isValidImageName("a"))
        assertTrue(isValidImageName(" ${"a".repeat(60)} "))
        assertTrue(!isValidImageName("   "))
        assertTrue(!isValidImageName("a".repeat(61)))
    }
}
