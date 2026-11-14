package de.geosphere.congregationplaner.organization.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CountryOption
import de.geosphere.congregationplaner.organization.domain.model.CountryRef
import de.geosphere.congregationplaner.organization.domain.model.preferredFor
import de.geosphere.congregationplaner.organization.testing.FakeCountryRepository
import de.geosphere.congregationplaner.organization.testing.listCountries
import de.geosphere.congregationplaner.organization.testing.testCountry
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListCountriesUseCaseTest {
    private val austria = CountryOption(testCountry.ref.copy(countryId = "at"), "Österreich")
    private val brazil = CountryOption(CountryRef("AM", "south", "br-branch", "br"), "")

    @Test
    fun `countries of all regions are collected and sorted by name`() = runBlocking {
        val countries = listCountries(austria, brazil, testCountry)().getOrThrow()

        assertEquals(listOf(brazil, testCountry, austria), countries) // a blank name sorts by its id
        assertEquals("br", brazil.displayName)
    }

    @Test
    fun `a failed read fails the whole list`() = runBlocking {
        val repository = FakeCountryRepository(listOf(testCountry), IllegalStateException("offline"))

        assertEquals("offline", listCountries(repository)().exceptionOrNull()?.message)
    }

    @Test
    fun `the country of the own congregation is preferred and else the only one`() {
        assertEquals(testCountry.ref, listOf(austria, testCountry).preferredFor(testCongregation))
        assertEquals(austria.ref, listOf(austria).preferredFor(null))
        assertNull(listOf(austria, testCountry).preferredFor(null))
        assertNull(emptyList<CountryOption>().preferredFor(testCongregation))
    }

    @Test
    fun `country paths and ids`() {
        val ref = testCongregation.country

        assertEquals("region/EMEA/subregion/europe/branch/central-europe/country/de", ref.documentPath)
        assertEquals(ref, CountryRef.fromIds(ref.ids))
        assertNull(CountryRef.fromIds(listOf("EMEA", "europe")))
    }
}
