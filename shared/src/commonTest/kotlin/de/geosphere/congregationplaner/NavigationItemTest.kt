package de.geosphere.congregationplaner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NavigationItemTest {

    @Test
    fun testHomeNavigationItem() {
        val item = NavigationItem.Home
        assertEquals("Home", item.label)
        assertEquals("home", item.routeName)
        assertNotNull(item.iconRes)
    }

    @Test
    fun testSettingsNavigationItem() {
        val item = NavigationItem.Settings
        assertEquals("Settings", item.label)
        assertEquals("settings", item.routeName)
        assertNotNull(item.iconRes)
    }

    @Test
    fun testPlanungUnterDerWocheNavigationItem() {
        val item = NavigationItem.PlanungUnterDerWoche
        assertEquals("Planung unter der Woche", item.label)
        assertEquals("leben_und_dienst", item.routeName)
        assertNotNull(item.iconRes)
    }

    @Test
    fun testPlanungUnterWochenendNavigationItem() {
        val item = NavigationItem.PlanungUnterWochenende
        assertEquals("Planung Wochenende", item.label)
        assertEquals("planung_wochenende", item.routeName)
        assertNotNull(item.iconRes)
    }

    @Test
    fun testVersammlungNavigationItem() {
        val item = NavigationItem.Versammlung
        assertEquals("Versammlung Metadaten", item.label)
        assertEquals("versammlung_metadata", item.routeName)
        assertNotNull(item.iconRes)
    }

    @Test
    fun testDiensteNavigationItem() {
        val item = NavigationItem.Dienste
        assertEquals("Dienste", item.label)
        assertEquals("dienste", item.routeName)
        assertNotNull(item.iconRes)
    }

    @Test
    fun testUsererwaltungNavigationItem() {
        val item = NavigationItem.Usererwaltung
        assertEquals("Userverwaltung", item.label)
        assertEquals("userverwaltung", item.routeName)
        assertNotNull(item.iconRes)
    }

    @Test
    fun testAllNavigationItemsCount() {
        val items = NavigationItem.entries
        assertEquals(7, items.size)
    }

    @Test
    fun testAllNavigationItemsExist() {
        val items = NavigationItem.entries
        val names = items.map { it.name }
        
        assertEquals(true, names.contains("Home"))
        assertEquals(true, names.contains("Settings"))
        assertEquals(true, names.contains("PlanungUnterDerWoche"))
        assertEquals(true, names.contains("PlanungUnterWochenende"))
        assertEquals(true, names.contains("Versammlung"))
        assertEquals(true, names.contains("Dienste"))
        assertEquals(true, names.contains("Usererwaltung"))
    }

    @Test
    fun testNavigationItemRouteNamesAreUnique() {
        val items = NavigationItem.entries
        val routeNames = items.map { it.routeName }
        
        assertEquals(routeNames.size, routeNames.toSet().size)
    }

    @Test
    fun testNavigationItemLabelsAreNotEmpty() {
        val items = NavigationItem.entries
        
        items.forEach { item ->
            assertEquals(true, item.label.isNotEmpty())
        }
    }

    @Test
    fun testNavigationItemRouteNamesAreNotEmpty() {
        val items = NavigationItem.entries
        
        items.forEach { item ->
            assertEquals(true, item.routeName.isNotEmpty())
        }
    }

    @Test
    fun testNavigationItemValueOf() {
        val item = NavigationItem.valueOf("Home")
        assertEquals(NavigationItem.Home, item)
        assertEquals("Home", item.label)
    }
}
