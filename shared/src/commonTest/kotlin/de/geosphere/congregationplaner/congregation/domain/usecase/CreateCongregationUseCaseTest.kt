package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationException
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.CongregationName
import de.geosphere.congregationplaner.congregation.domain.model.NewCongregationAdmin
import de.geosphere.congregationplaner.congregation.domain.permission.CongregationPermissionPolicy
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateCongregationUseCaseTest {
    private val circuit = listOf("EMEA", "europe", "central", "609a")
    private val repository = FakeRepository()
    private val admin = NewCongregationAdmin("uid-1", "max@example.com", "Max Muster", "Max", "Muster", true)
    private val policy = CongregationPermissionPolicy()
    private val globalAdmin = AppUser(uid = "global-uid", isGlobalAdmin = true)
    private val congregationAdmin = AppUser(
        uid = "uid-2",
        approved = true,
        roles = setOf(CongregationRole.CONGREGATION_ADMIN),
        congregation = CongregationRef.fromIds(circuit + "k1"),
    )

    @Test
    fun `the name key ignores case and blanks`() {
        assertEquals("grafing süd", CongregationName.key("  Grafing   SÜD "))
        assertEquals(CongregationName.key("Grafing Süd"), CongregationName.key(" grafing\tsüd"))
    }

    @Test
    fun `a free name creates the congregation for the creator as first admin`() = runBlocking<Unit> {
        val created = CreateOwnCongregationUseCase(repository)(circuit, "  Grafing Süd ", admin).getOrThrow()

        assertEquals("new-id", created.id)
        assertEquals("Grafing Süd", repository.created.single().name)
        assertEquals(circuit, repository.circuits.single())
        assertEquals("uid-1", repository.createdBy)
        assertEquals(admin, repository.firstAdmin)
    }

    @Test
    fun `a global admin creates without becoming a member`() = runBlocking<Unit> {
        CreateCongregationUseCase(repository, policy)(globalAdmin, circuit, "Nord").getOrThrow()

        assertNull(repository.firstAdmin)
        assertEquals("global-uid", repository.createdBy)
    }

    @Test
    fun `only a global admin creates a congregation without becoming its admin`() = runBlocking<Unit> {
        val failure = CreateCongregationUseCase(repository, policy)(congregationAdmin, circuit, "Nord")

        assertIs<CongregationException.NotAllowed>(failure.exceptionOrNull())
        assertTrue(repository.created.isEmpty())
        assertTrue(repository.lookedUp.isEmpty())
    }

    @Test
    fun `invalid names are rejected before anything is read or written`() = runBlocking<Unit> {
        val useCase = CreateOwnCongregationUseCase(repository)

        assertIs<CongregationException.MissingName>(useCase(circuit, "   ", admin).exceptionOrNull())
        assertIs<CongregationException.NameTooLong>(
            useCase(circuit, "x".repeat(CongregationName.MAX_LENGTH + 1), admin).exceptionOrNull(),
        )
        assertIs<CongregationException.InvalidName>(useCase(circuit, "Nord/Süd", admin).exceptionOrNull())
        assertTrue(repository.created.isEmpty())
        assertTrue(repository.lookedUp.isEmpty())
    }

    @Test
    fun `a taken name is reported`() = runBlocking<Unit> {
        repository.nameOwner = "other-congregation"

        val failure = CreateOwnCongregationUseCase(repository)(circuit, "Grafing Süd", admin).exceptionOrNull()

        assertIs<CongregationException.NameTaken>(failure)
        assertTrue(repository.created.isEmpty())
    }

    @Test
    fun `an unconfirmed e-mail address cannot create a congregation as admin`() = runBlocking<Unit> {
        val failure = CreateOwnCongregationUseCase(repository)(
            circuit,
            "Grafing Süd",
            admin.copy(emailVerified = false),
        ).exceptionOrNull()

        assertIs<CongregationException.EmailNotVerified>(failure)
        assertTrue(repository.created.isEmpty())
    }

    @Test
    fun `a failing name lookup is passed on`() = runBlocking<Unit> {
        val error = IllegalStateException("offline")
        repository.lookupError = error

        assertEquals(error, CreateOwnCongregationUseCase(repository)(circuit, "Nord", admin).exceptionOrNull())
        assertTrue(repository.created.isEmpty())
    }

    @Test
    fun `saving may keep the own name but not take the name of another congregation`() = runBlocking<Unit> {
        val useCase = SaveCongregationUseCase(repository, policy)
        val own = CongregationMetadata(id = "k1", name = " Grafing Süd ")

        repository.nameOwner = "k1"
        assertEquals("Grafing Süd", useCase(congregationAdmin, circuit, own).getOrThrow().name)

        repository.nameOwner = "k2"
        assertIs<CongregationException.NameTaken>(useCase(globalAdmin, circuit, own).exceptionOrNull())
        assertIs<CongregationException.MissingName>(
            useCase(globalAdmin, circuit, own.copy(name = " ")).exceptionOrNull(),
        )
    }

    @Test
    fun `a congregation admin cannot save another congregation`() = runBlocking<Unit> {
        val other = CongregationMetadata(id = "k2", name = "Nord")

        val failure = SaveCongregationUseCase(repository, policy)(congregationAdmin, circuit, other)

        assertIs<CongregationException.NotAllowed>(failure.exceptionOrNull())
        assertTrue(repository.lookedUp.isEmpty())
    }

    @Test
    fun `saving needs an existing congregation`() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking {
                SaveCongregationUseCase(repository, policy)(globalAdmin, circuit, CongregationMetadata(name = "Nord"))
            }
        }
    }

    private class FakeRepository : CongregationMetadataRepository {
        var nameOwner: String? = null
        var lookupError: Throwable? = null
        var createdBy: String? = null
        var firstAdmin: NewCongregationAdmin? = null
        val created = mutableListOf<CongregationMetadata>()
        val circuits = mutableListOf<List<String>>()
        val lookedUp = mutableListOf<String>()

        override fun observeAll(circuitIds: List<String>): Flow<List<CongregationMetadata>> = emptyFlow()

        override suspend fun findNameOwner(circuitIds: List<String>, name: String): Result<String?> {
            lookedUp += name
            return lookupError?.let { Result.failure(it) } ?: Result.success(nameOwner)
        }

        override suspend fun create(
            circuitIds: List<String>,
            congregation: CongregationMetadata,
            createdBy: String,
            firstAdmin: NewCongregationAdmin?,
        ): Result<CongregationMetadata> {
            circuits += circuitIds
            created += congregation
            this.createdBy = createdBy
            this.firstAdmin = firstAdmin
            return Result.success(congregation.copy(id = "new-id"))
        }

        override suspend fun save(
            circuitIds: List<String>,
            congregation: CongregationMetadata,
        ): Result<CongregationMetadata> = Result.success(congregation)

        override suspend fun delete(circuitIds: List<String>, congregationId: String): Result<Unit> =
            Result.success(Unit)
    }
}
