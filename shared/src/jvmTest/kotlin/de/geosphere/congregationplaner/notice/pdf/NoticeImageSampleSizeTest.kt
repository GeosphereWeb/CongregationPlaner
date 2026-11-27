package de.geosphere.congregationplaner.notice.pdf

import kotlin.test.Test
import kotlin.test.assertEquals

class NoticeImageSampleSizeTest {
    @Test
    fun `small images are not subsampled`() {
        assertEquals(1, noticeImageSampleSize(600, 400))
    }

    @Test
    fun `the sample size is the power of two that brings the longer edge to the maximum`() {
        assertEquals(2, noticeImageSampleSize(1200, 800))
        assertEquals(4, noticeImageSampleSize(1201, 100))
        assertEquals(8, noticeImageSampleSize(4000, 4000))
    }

    @Test
    fun `the taller edge counts too`() {
        assertEquals(2, noticeImageSampleSize(100, 1200))
    }
}
