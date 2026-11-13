package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.DEFAULT_MESSAGE_TEMPLATES
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import de.geosphere.congregationplaner.user.domain.model.MessageTemplateException
import de.geosphere.congregationplaner.user.domain.repository.MessageTemplateRepository
import de.geosphere.congregationplaner.user.testing.FakeMessageTemplateRepository
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MessageTemplateUseCasesTest {
    private val member = AppUser(uid = "u1", approved = true, congregation = testCongregation)
    private val own = listOf(MessageTemplate("Kurz", "Hallo {Vorname}"))

    @Test
    fun `own templates are observed and the defaults apply without any`() = runBlocking {
        val repository = FakeMessageTemplateRepository()
        val observe = ObserveMessageTemplatesUseCase(repository)

        assertEquals(DEFAULT_MESSAGE_TEMPLATES, observe(member).first())
        repository.templates.value = own
        assertEquals(own, observe(member).first())
    }

    @Test
    fun `users without an own member document and read failures get the defaults`() = runBlocking {
        val repository = FakeMessageTemplateRepository(own)
        val observe = ObserveMessageTemplatesUseCase(repository)
        listOf(
            member.copy(hasAccount = false),
            member.copy(isMember = false, isGlobalAdmin = true),
            member.copy(congregation = null),
        ).forEach { assertEquals(DEFAULT_MESSAGE_TEMPLATES, observe(it).first()) }

        val failing = object : MessageTemplateRepository by repository {
            override fun observeTemplates(congregation: CongregationRef, uid: String): Flow<List<MessageTemplate>> =
                flow { throw IllegalStateException("offline") }
        }
        assertEquals(DEFAULT_MESSAGE_TEMPLATES, ObserveMessageTemplatesUseCase(failing)(member).first())
    }

    @Test
    fun `saving trims and stores valid templates`() = runBlocking {
        val repository = FakeMessageTemplateRepository()

        val saved = SaveMessageTemplatesUseCase(repository)(member, listOf(MessageTemplate(" Kurz ", " Hallo ")))

        assertEquals(listOf(MessageTemplate("Kurz", "Hallo")), saved.getOrThrow())
        assertEquals("u1" to listOf(MessageTemplate("Kurz", "Hallo")), repository.saved.single())
    }

    @Test
    fun `invalid templates and users without a member document are rejected`() = runBlocking {
        val repository = FakeMessageTemplateRepository()
        val save = SaveMessageTemplatesUseCase(repository)
        val tooMany = (1..5).map { MessageTemplate("V$it", "T") }

        listOf(
            emptyList(),
            tooMany,
            listOf(MessageTemplate(" ", "T")),
            listOf(MessageTemplate("A", "x".repeat(MessageTemplate.MAX_TEXT_LENGTH + 1))),
            listOf(MessageTemplate("x".repeat(MessageTemplate.MAX_NAME_LENGTH + 1), "T")),
        ).forEach { assertIs<MessageTemplateException.Invalid>(save(member, it).exceptionOrNull()) }
        assertIs<MessageTemplateException.NotAllowed>(save(member.copy(hasAccount = false), own).exceptionOrNull())
        assertTrue(repository.saved.isEmpty())
    }
}
