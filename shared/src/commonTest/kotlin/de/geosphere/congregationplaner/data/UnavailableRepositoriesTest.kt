package de.geosphere.congregationplaner.data

import de.geosphere.congregationplaner.congregation.data.unavailableCongregationMetadataRepository
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class UnavailableRepositoriesTest {
    @Test
    fun `unavailable congregation repository reports the platform`(): Unit = runBlocking {
        val repository = unavailableCongregationMetadataRepository("iOS")

        val failure = repository.save(CongregationMetadata(name = "Berlin")).exceptionOrNull()
        assertIs<UnsupportedOperationException>(failure)
        assertEquals("Firestore is not configured for iOS.", failure.message)
        assertIs<UnsupportedOperationException>(repository.delete("609a").exceptionOrNull())
        assertFailsWith<UnsupportedOperationException> { repository.observeAll().first() }
    }
}
