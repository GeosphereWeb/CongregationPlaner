package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectoryException
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.testing.FakeExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExternalSpeakerDirectoryUseCasesTest {
    private val policy = TalkSchedulePermissionPolicy()
    private val repository = FakeExternalSpeakerDirectoryRepository(
        circuits = listOf(ExternalCircuit("c2", "süd"), ExternalCircuit("c1", "Nord")),
        congregations = listOf(
            ExternalCongregation("g2", "c2", "Zell"),
            ExternalCongregation("g1", "c1", "Altdorf"),
        ),
        speakers = listOf(
            VisitingSpeaker("s2", "g2", "zorn", "Tim"),
            VisitingSpeaker("s1", "g1", "Berg", "Lea", talkNumbers = listOf(12)),
        ),
    )
    private val admin = AppUser(
        uid = "1",
        approved = true,
        role = UserRole.CONGREGATION_ADMIN,
        congregation = testCongregation,
    )
    private val member = AppUser(uid = "2", approved = true, congregation = testCongregation)
    private val withoutCongregation = admin.copy(congregation = null)

    @Test
    fun `directory is sorted by name and empty without a congregation`() = runBlocking {
        val directory = ObserveExternalDirectoryUseCase(repository)(admin).first()

        assertEquals(listOf("c1", "c2"), directory.circuits.map { it.id })
        assertEquals(listOf("g1", "g2"), directory.congregations.map { it.id })
        assertEquals(listOf("s1", "s2"), directory.speakers.map { it.id })
        assertEquals(testCongregation, repository.requestedCongregation)
        assertTrue(ObserveExternalDirectoryUseCase(repository)(withoutCongregation).toList().isEmpty())
    }

    @Test
    fun `picker speakers carry the name of their congregation and skip orphans`() = runBlocking {
        repository.speakers.value += VisitingSpeaker("s3", "missing", "Waise")

        val speakers = ObserveExternalSpeakersUseCase(ObserveExternalDirectoryUseCase(repository))(admin).first()

        assertEquals(
            listOf(ExternalSpeaker("s1", "Berg", "Lea", "Altdorf"), ExternalSpeaker("s2", "zorn", "Tim", "Zell")),
            speakers,
        )
        assertEquals("Berg, Lea (Altdorf)", speakers.first().displayName)
    }

    @Test
    fun `circuit is saved trimmed and validated`() = runBlocking {
        val save = SaveExternalCircuitUseCase(repository, policy)

        assertEquals(ExternalCircuit("c-1", "Ost"), save(admin, ExternalCircuit(name = " Ost ")).getOrThrow())
        listOf("", "  ", "x".repeat(VisitingSpeaker.MAX_NAME_LENGTH + 1)).forEach {
            assertIs<ExternalDirectoryException.InvalidCircuit>(
                save(admin, ExternalCircuit(name = it)).exceptionOrNull(),
            )
        }
        assertEquals(1, repository.savedCircuits.size)
    }

    @Test
    fun `congregation needs a name and an existing circuit`() = runBlocking {
        val save = SaveExternalCongregationUseCase(repository, policy)

        val saved = save(admin, ExternalCongregation(circuitId = "c1", name = " Neustadt ")).getOrThrow()
        assertEquals(ExternalCongregation("g-1", "c1", "Neustadt"), saved)
        listOf(
            ExternalCongregation(circuitId = "c1", name = " "),
            ExternalCongregation(circuitId = "unknown", name = "Neustadt"),
            ExternalCongregation(circuitId = "c1", name = "x".repeat(VisitingSpeaker.MAX_NAME_LENGTH + 1)),
        ).forEach { assertIs<ExternalDirectoryException.InvalidCongregation>(save(admin, it).exceptionOrNull()) }
        assertEquals(1, repository.savedCongregations.size)
    }

    @Test
    fun `speaker is saved trimmed with sorted distinct talk numbers`() = runBlocking {
        val save = SaveVisitingSpeakerUseCase(repository, policy)

        val saved = save(
            admin,
            VisitingSpeaker(
                congregationId = "g1",
                lastName = " Neu ",
                firstName = " Nina ",
                phone = " 0170 ",
                email = " nina@example.org ",
                talkNumbers = listOf(45, 12, 45),
            ),
        ).getOrThrow()

        assertEquals(
            VisitingSpeaker("s-1", "g1", "Neu", "Nina", "0170", "nina@example.org", listOf(12, 45)),
            saved,
        )
    }

    @Test
    fun `an existing speaker can move to another congregation`() = runBlocking {
        val moved = repository.speakers.value.first { it.id == "s1" }.copy(congregationId = "g2")

        val saved = SaveVisitingSpeakerUseCase(repository, policy)(admin, moved).getOrThrow()

        assertEquals("g2", saved.congregationId)
        assertEquals("s1", saved.id)
    }

    @Test
    fun `invalid speakers are rejected`() = runBlocking {
        val save = SaveVisitingSpeakerUseCase(repository, policy)
        val valid = VisitingSpeaker(congregationId = "g1", lastName = "Neu")

        listOf(
            valid.copy(lastName = " "),
            valid.copy(congregationId = "unknown"),
            valid.copy(lastName = "x".repeat(VisitingSpeaker.MAX_NAME_LENGTH + 1)),
            valid.copy(phone = "1".repeat(VisitingSpeaker.MAX_PHONE_LENGTH + 1)),
            valid.copy(email = "e".repeat(VisitingSpeaker.MAX_EMAIL_LENGTH + 1)),
            valid.copy(talkNumbers = listOf(0)),
            valid.copy(talkNumbers = listOf(10_000)),
            valid.copy(talkNumbers = (1..VisitingSpeaker.MAX_TALKS + 1).toList()),
        ).forEach { assertIs<ExternalDirectoryException.InvalidSpeaker>(save(admin, it).exceptionOrNull()) }
        assertTrue(repository.savedSpeakers.isEmpty())
    }

    @Test
    fun `members without planning permission cannot change the directory`() = runBlocking {
        val speaker = VisitingSpeaker(congregationId = "g1", lastName = "Neu")

        listOf(member, withoutCongregation).forEach { user ->
            assertIs<ExternalDirectoryException.NotAllowed>(
                SaveExternalCircuitUseCase(repository, policy)(user, ExternalCircuit(name = "Ost")).exceptionOrNull(),
            )
            assertIs<ExternalDirectoryException.NotAllowed>(
                SaveExternalCongregationUseCase(
                    repository,
                    policy,
                )(user, ExternalCongregation(circuitId = "c1", name = "X"))
                    .exceptionOrNull(),
            )
            assertIs<ExternalDirectoryException.NotAllowed>(
                SaveVisitingSpeakerUseCase(repository, policy)(user, speaker).exceptionOrNull(),
            )
            assertIs<ExternalDirectoryException.NotAllowed>(
                DeleteExternalCircuitUseCase(repository, policy)(user, "c1").exceptionOrNull(),
            )
            assertIs<ExternalDirectoryException.NotAllowed>(
                DeleteExternalCongregationUseCase(repository, policy)(user, "g1").exceptionOrNull(),
            )
            assertIs<ExternalDirectoryException.NotAllowed>(
                DeleteVisitingSpeakerUseCase(repository, policy)(user, "s1").exceptionOrNull(),
            )
        }
        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test
    fun `a circuit or congregation with children cannot be deleted`() = runBlocking {
        val deleteCircuit = DeleteExternalCircuitUseCase(repository, policy)
        val deleteCongregation = DeleteExternalCongregationUseCase(repository, policy)

        assertIs<ExternalDirectoryException.CircuitNotEmpty>(deleteCircuit(admin, "c1").exceptionOrNull())
        assertIs<ExternalDirectoryException.CongregationNotEmpty>(deleteCongregation(admin, "g1").exceptionOrNull())
        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test
    fun `children are deleted bottom up`() = runBlocking {
        assertTrue(DeleteVisitingSpeakerUseCase(repository, policy)(admin, "s1").isSuccess)
        assertTrue(DeleteExternalCongregationUseCase(repository, policy)(admin, "g1").isSuccess)
        assertTrue(DeleteExternalCircuitUseCase(repository, policy)(admin, "c1").isSuccess)

        assertEquals(listOf("s1", "g1", "c1"), repository.deletedIds)
    }

    @Test
    fun `blank ids are rejected on delete`(): Unit = runBlocking {
        assertIs<ExternalDirectoryException.InvalidCircuit>(
            DeleteExternalCircuitUseCase(repository, policy)(admin, " ").exceptionOrNull(),
        )
        assertIs<ExternalDirectoryException.InvalidCongregation>(
            DeleteExternalCongregationUseCase(repository, policy)(admin, " ").exceptionOrNull(),
        )
        assertIs<ExternalDirectoryException.InvalidSpeaker>(
            DeleteVisitingSpeakerUseCase(repository, policy)(admin, " ").exceptionOrNull(),
        )
    }
}
