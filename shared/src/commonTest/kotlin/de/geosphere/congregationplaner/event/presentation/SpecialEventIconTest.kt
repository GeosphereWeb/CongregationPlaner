package de.geosphere.congregationplaner.event.presentation

import congregationplaner.uilib.generated.resources.Res
import congregationplaner.uilib.generated.resources.ic_cp_arena_duo
import congregationplaner.uilib.generated.resources.ic_cp_persons_doorstep_duo
import congregationplaner.uilib.generated.resources.ic_cp_square
import congregationplaner.uilib.generated.resources.ic_cp_wine_bread_duo
import kotlin.test.Test
import kotlin.test.assertEquals

class SpecialEventIconTest {
    @Test
    fun `known keywords pick their icon regardless of case`() {
        assertEquals(Res.drawable.ic_cp_arena_duo, specialEventIcon("Kreiskongress"))
        assertEquals(Res.drawable.ic_cp_arena_duo, specialEventIcon("KREISKONGRESS Frühjahr"))
        assertEquals(Res.drawable.ic_cp_wine_bread_duo, specialEventIcon("Abendmahl"))
        assertEquals(Res.drawable.ic_cp_wine_bread_duo, specialEventIcon("Feier des Gedächtnismahls"))
        assertEquals(Res.drawable.ic_cp_persons_doorstep_duo, specialEventIcon("Dienstwoche des Kreisaufsehers"))
    }

    @Test
    fun `other titles get the square`() {
        assertEquals(Res.drawable.ic_cp_square, specialEventIcon("Putztag"))
        assertEquals(Res.drawable.ic_cp_square, specialEventIcon(""))
    }
}
