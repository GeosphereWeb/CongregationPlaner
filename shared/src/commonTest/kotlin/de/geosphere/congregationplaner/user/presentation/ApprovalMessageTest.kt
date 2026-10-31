package de.geosphere.congregationplaner.user.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class ApprovalMessageTest {

    @Test
    fun `approval message names the account and highlights the email`() {
        val message = approvalMessage("name@beispiel.de")

        assertEquals(
            "Dein Konto name@beispiel.de wurde angelegt, ist aber noch nicht freigegeben. " +
                "Bitte wende dich an den Admin deiner Versammlung.",
            message.text,
        )
        val span = message.spanStyles.single()
        assertEquals("name@beispiel.de", message.text.substring(span.start, span.end))
    }

    @Test
    fun `approval message without an email still reads as a sentence`() {
        assertEquals(true, approvalMessage("").text.startsWith("Dein Konto  wurde angelegt"))
    }
}
