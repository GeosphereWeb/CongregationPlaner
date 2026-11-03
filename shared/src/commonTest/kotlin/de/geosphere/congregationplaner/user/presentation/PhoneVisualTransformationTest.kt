package de.geosphere.congregationplaner.user.presentation

import androidx.compose.ui.text.AnnotatedString
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PhoneVisualTransformationTest {
    @Test
    fun `typed digits are shown grouped while the input stays unchanged`() {
        val result = PhoneVisualTransformation.filter(AnnotatedString("01701234567"))

        assertEquals("0170 1234567", result.text.text)
        assertEquals(0, result.offsetMapping.originalToTransformed(0))
        assertEquals(3, result.offsetMapping.originalToTransformed(3))
        assertEquals(5, result.offsetMapping.originalToTransformed(4))
        assertEquals(12, result.offsetMapping.originalToTransformed(11))
        assertEquals(4, result.offsetMapping.transformedToOriginal(5))
        assertEquals(11, result.offsetMapping.transformedToOriginal(12))
    }

    @Test
    fun `international numbers get both spaces`() {
        assertEquals("+49 170 1234567", phoneDisplay("+491701234567"))
        assertContentEquals(
            intArrayOf(0, 1, 2, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15),
            rawToShownOffsets("+491701234567", "+49 170 1234567"),
        )
    }

    @Test
    fun `input with own separators or an unknown prefix is shown as typed`() {
        val result = PhoneVisualTransformation.filter(AnnotatedString("0201 123456"))

        assertEquals("0201 123456", result.text.text)
        assertEquals(7, result.offsetMapping.originalToTransformed(7))
        assertEquals("0201123456", phoneDisplay("0201123456"))
        assertEquals(" 0170", phoneDisplay(" 0170"))
    }
}
