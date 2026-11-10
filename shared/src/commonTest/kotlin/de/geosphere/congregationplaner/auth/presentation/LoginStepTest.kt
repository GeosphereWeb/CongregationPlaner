package de.geosphere.congregationplaner.auth.presentation

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.organization.presentation.selection.CongregationSelectionUiState
import de.geosphere.congregationplaner.organization.presentation.selection.NewCongregationDraft
import de.geosphere.congregationplaner.organization.presentation.selection.OrgLevelSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginStepTest {
    private val ref = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")
    private val selected = SelectedCongregation(ref, "Berlin", "Mitte")

    @Test
    fun `without a congregation the first step asks for it`() {
        assertEquals(
            LoginStep(stepLabel = "SCHRITT 1 / 2", title = "Welche Versammlung?"),
            loginStep(CongregationSelectionUiState(), globalLogin = false),
        )
    }

    @Test
    fun `a chosen congregation leads to the credentials`() {
        assertEquals(
            LoginStep(
                stepLabel = "SCHRITT 2 / 2",
                title = "Berlin",
                previousSteps = listOf("Kreis Mitte"),
                showCredentials = true,
                canSignIn = true,
            ),
            loginStep(CongregationSelectionUiState(selected = selected), globalLogin = false),
        )
    }

    @Test
    fun `picking again hides the credentials`() {
        val picking = CongregationSelectionUiState(
            selected = selected,
            levels = listOf(OrgLevelSelection(OrgLevel.REGION)),
        )

        val step = loginStep(picking, globalLogin = false)

        assertFalse(step.showCredentials)
        assertEquals("Welche Versammlung?", step.title)
        assertEquals(emptyList(), step.previousSteps)
    }

    @Test
    fun `a global admin skips the congregation`() {
        assertEquals(
            LoginStep(
                stepLabel = "SCHRITT 2 / 2",
                title = "Globale Verwaltung",
                showCredentials = true,
                canSignIn = true,
            ),
            loginStep(CongregationSelectionUiState(selected = selected), globalLogin = true),
        )
    }

    @Test
    fun `a new congregation adds the name step`() {
        val draft = NewCongregationDraft(circuitIds = ref.ids.dropLast(1), circuitName = "609a", name = "Nord")

        val step = loginStep(CongregationSelectionUiState(newCongregation = draft), globalLogin = false)

        assertEquals("SCHRITT 2 / 3", step.stepLabel)
        assertEquals("Neue Versammlung", step.title)
        assertEquals(listOf("Kreis 609a"), step.previousSteps)
        assertTrue(step.isNewCongregationFlow)
        assertTrue(step.canSignIn)

        val confirmed = loginStep(
            CongregationSelectionUiState(newCongregation = draft.copy(confirmed = true)),
            globalLogin = false,
        )
        assertEquals("SCHRITT 3 / 3", confirmed.stepLabel)
        assertEquals("Nord", confirmed.title)
    }
}
