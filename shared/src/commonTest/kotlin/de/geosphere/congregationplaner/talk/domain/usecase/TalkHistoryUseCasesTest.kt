package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.testing.FakeTalkHistoryRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TalkHistoryUseCasesTest {
    private val repository = FakeTalkHistoryRepository()
    private val policy = TalkSchedulePermissionPolicy()
    private val observe = ObserveTalkHistoryUseCase(repository)
    private val save = SaveTalkHistoryEntryUseCase(repository, policy)
    private val delete = DeleteTalkHistoryEntryUseCase(repository, policy)
    private val sync = SyncTalkHistoryWithSlotUseCase(repository, policy)

    private val admin = AppUser(
        uid = "1",
        approved = true,
        role = UserRole.CONGREGATION_ADMIN,
        congregation = testCongregation,
    )
    private val member = AppUser(uid = "2", approved = true, congregation = testCongregation)

    private val spring = LocalDate(2026, 3, 1)
    private val autumn = LocalDate(2026, 10, 25)

    @Test
    fun `last held day is the latest date and missing for no dates`() {
        assertEquals(autumn, TalkHistoryEntry(7, listOf(spring, autumn)).lastHeldOn)
        assertNull(TalkHistoryEntry(7, emptyList()).lastHeldOn)
    }

    @Test
    fun `planner saves the dates sorted and without duplicates in his congregation`() = runBlocking {
        val result = save(admin, TalkHistoryEntry(7, listOf(autumn, spring, autumn)))

        val expected = TalkHistoryEntry(7, listOf(spring, autumn))
        assertEquals(expected, result.getOrThrow())
        assertEquals(listOf(expected), repository.savedEntries)
        assertEquals(testCongregation, repository.requestedCongregation)
    }

    @Test
    fun `planned days are kept and always count as held days`() = runBlocking {
        val result = save(admin, TalkHistoryEntry(7, listOf(spring), plannedOn = listOf(autumn, autumn)))

        val expected = TalkHistoryEntry(7, listOf(spring, autumn), plannedOn = listOf(autumn))
        assertEquals(expected, result.getOrThrow())
    }

    @Test
    fun `saving needs the permission to plan talks and a congregation`() = runBlocking {
        assertIs<TalkScheduleException.NotAllowed>(save(member, TalkHistoryEntry(7, listOf(spring))).exceptionOrNull())
        val withoutCongregation = admin.copy(congregation = null)
        assertIs<TalkScheduleException.NotAllowed>(
            save(withoutCongregation, TalkHistoryEntry(7, listOf(spring))).exceptionOrNull(),
        )
        assertTrue(repository.savedEntries.isEmpty())
    }

    @Test
    fun `invalid entries are refused`() = runBlocking {
        val tooMany = List(TalkHistoryEntry.MAX_DATES + 1) { LocalDate(2000, 1, 1).plus(it, DateTimeUnit.DAY) }
        val invalid = listOf(
            TalkHistoryEntry(0, listOf(spring)),
            TalkHistoryEntry(10_000, listOf(spring)),
            TalkHistoryEntry(7, emptyList()),
            TalkHistoryEntry(7, tooMany),
        )
        invalid.forEach { entry ->
            assertIs<TalkScheduleException.InvalidTalkHistory>(save(admin, entry).exceptionOrNull())
        }
        assertIs<TalkScheduleException.InvalidTalkHistory>(delete(admin, 0).exceptionOrNull())
        assertTrue(repository.savedEntries.isEmpty())
    }

    @Test
    fun `observe sorts by talk number and is empty without a congregation`() = runBlocking {
        repository.entries.value = listOf(TalkHistoryEntry(9, listOf(spring)), TalkHistoryEntry(2, listOf(autumn)))

        assertEquals(listOf(2, 9), observe(admin).first().map { it.talkNumber })
        assertEquals(0, observe(admin.copy(congregation = null)).let { flow -> countEmissions(flow) })
    }

    @Test
    fun `planner deletes an entry and failures are returned`() = runBlocking {
        repository.entries.value = listOf(TalkHistoryEntry(7, listOf(spring)))

        assertTrue(delete(admin, 7).isSuccess)
        assertEquals(listOf(7), repository.deletedNumbers)
        assertTrue(repository.entries.value.isEmpty())

        assertIs<TalkScheduleException.NotAllowed>(delete(member, 7).exceptionOrNull())
        repository.deleteResult = Result.failure(IllegalStateException("denied"))
        assertEquals("denied", delete(admin, 7).exceptionOrNull()?.message)
    }

    private suspend fun countEmissions(flow: kotlinx.coroutines.flow.Flow<*>): Int {
        var count = 0
        flow.collect { count++ }
        return count
    }

    private fun slot(day: LocalDate, number: Int? = 7, cancelled: Boolean = false) = TalkSlot(
        date = day,
        time = LocalTime(17, 30),
        talkCancelled = cancelled,
        talkNumber = number,
        talkTitle = number?.let { "Vortrag $it" },
    )

    @Test
    fun `the speaker of a slot is stored per day and follows changes of the slot`() = runBlocking {
        val member = slot(autumn).copy(speakerMemberId = "m1", speakerLastName = "Muster", speakerFirstName = "Max")
        sync(admin, null, member)
        assertEquals(mapOf(autumn to "Muster, Max"), repository.entries.value.single().speakers)

        val external = slot(autumn).copy(
            externalSpeakerId = "e1",
            speakerLastName = "Gast",
            speakerFirstName = "Gustav",
            speakerCongregation = "Nord",
        )
        sync(admin, member, external)
        assertEquals(mapOf(autumn to "Gast, Gustav (Nord)"), repository.entries.value.single().speakers)

        sync(admin, external, slot(autumn))
        assertEquals(TalkHistoryEntry(7, listOf(autumn), listOf(autumn)), repository.entries.value.single())
    }

    @Test
    fun `saving drops speakers of days that were not held`() = runBlocking {
        val entry = TalkHistoryEntry(7, listOf(spring), speakers = mapOf(spring to "A", autumn to "B"))

        assertEquals(mapOf(spring to "A"), save(admin, entry).getOrThrow().speakers)
    }

    @Test
    fun `a new slot with a talk adds its day to the history of the talk`() = runBlocking {
        repository.entries.value = listOf(TalkHistoryEntry(7, listOf(spring)))

        assertTrue(sync(admin, null, slot(autumn)).isSuccess)

        val expected = TalkHistoryEntry(7, listOf(spring, autumn), plannedOn = listOf(autumn))
        assertEquals(listOf(expected), repository.entries.value)
    }

    @Test
    fun `moving a slot replaces the old day and changing the talk moves the day to the other talk`() = runBlocking {
        repository.entries.value = listOf(TalkHistoryEntry(7, listOf(spring)), TalkHistoryEntry(8, listOf(autumn)))

        sync(admin, slot(spring), slot(LocalDate(2026, 4, 5)))
        val moved = repository.entries.value.first { it.talkNumber == 7 }
        assertEquals(TalkHistoryEntry(7, listOf(LocalDate(2026, 4, 5)), listOf(LocalDate(2026, 4, 5))), moved)

        sync(admin, slot(autumn, number = 8), slot(autumn, number = 9))
        val byNumber = repository.entries.value.associateBy { it.talkNumber }
        assertEquals(listOf(autumn), byNumber.getValue(9).heldOn)
        assertTrue(8 !in byNumber)
    }

    @Test
    fun `a cancelled talk and a slot without a talk do not count and cancelling removes the day`() = runBlocking {
        repository.entries.value = listOf(TalkHistoryEntry(7, listOf(spring)))

        sync(admin, null, slot(autumn, cancelled = true))
        sync(admin, null, slot(autumn, number = null))
        assertEquals(listOf(TalkHistoryEntry(7, listOf(spring))), repository.entries.value)

        sync(admin, slot(spring), slot(spring, cancelled = true))
        assertTrue(repository.entries.value.isEmpty())
    }

    @Test
    fun `deleting a slot removes its day and an unchanged slot writes nothing`() = runBlocking {
        repository.entries.value = listOf(TalkHistoryEntry(7, listOf(spring, autumn)))

        sync(admin, slot(spring), slot(spring))
        assertTrue(repository.savedEntries.isEmpty())

        sync(admin, slot(spring), null)
        assertEquals(listOf(TalkHistoryEntry(7, listOf(autumn))), repository.entries.value)
        sync(admin, slot(LocalDate(2026, 1, 1)), null)
        assertEquals(1, repository.savedEntries.size)
    }

    @Test
    fun `syncing needs the permission to plan talks and repository failures are returned`() = runBlocking {
        assertIs<TalkScheduleException.NotAllowed>(sync(member, null, slot(spring)).exceptionOrNull())
        val withoutCongregation = admin.copy(congregation = null)
        assertIs<TalkScheduleException.NotAllowed>(sync(withoutCongregation, null, slot(spring)).exceptionOrNull())

        repository.saveResult = Result.failure(IllegalStateException("denied"))
        assertEquals("denied", sync(admin, null, slot(spring)).exceptionOrNull()?.message)

        repository.entries.value = listOf(TalkHistoryEntry(7, listOf(spring)))
        repository.deleteResult = Result.failure(IllegalStateException("gone"))
        assertEquals("gone", sync(admin, slot(spring), null).exceptionOrNull()?.message)
    }
}
