package de.geosphere.congregationplaner.user.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FamilyTest {
    private fun member(uid: String, firstName: String, head: String? = null) = AppUser(
        uid = uid,
        profile = MemberProfile(lastName = "Muster", firstName = firstName, familyHeadId = head),
    )

    private val max = member("max", "Max")
    private val anna = member("anna", "Anna", head = "max")
    private val tim = member("tim", "Tim", head = "max")
    private val lea = member("lea", "Lea")
    private val members = listOf(max, anna, tim, lea)

    @Test
    fun `candidates are the other members without a head of their own sorted by name`() {
        assertEquals(listOf("lea", "max"), familyHeadCandidates(members, "anna").map { it.uid })
        assertEquals(listOf("max"), familyHeadCandidates(members, "lea").map { it.uid })
        // A new person may choose every head.
        assertEquals(listOf("lea", "max"), familyHeadCandidates(members, null).map { it.uid })
    }

    @Test
    fun `a family head cannot get a head of his own`() {
        assertTrue(familyHeadCandidates(members, "max").isEmpty())
    }

    @Test
    fun `the family of a head are the members naming him sorted by name`() {
        assertEquals(listOf(anna, tim), familyOf(members, "max"))
        assertTrue(familyOf(members, "lea").isEmpty())
    }

    @Test
    fun `normalizing keeps a relation only with a head and defaults it to other`() {
        assertEquals(
            FamilyRelation.OTHER,
            MemberProfile(familyHeadId = "max").normalized().relationToHead,
        )
        val child = MemberProfile(familyHeadId = "max", relationToHead = FamilyRelation.CHILD)
        assertEquals(child, child.normalized())
        assertNull(child.copy(familyHeadId = null).normalized().relationToHead)
        assertEquals(MemberProfile(), child.copy(familyHeadId = " ").normalized())
    }

    @Test
    fun `unknown relations are ignored`() {
        assertEquals(FamilyRelation.SPOUSE, FamilyRelation.fromValue("SPOUSE"))
        assertNull(FamilyRelation.fromValue("COUSIN"))
        assertNull(FamilyRelation.fromValue(null))
    }
}
