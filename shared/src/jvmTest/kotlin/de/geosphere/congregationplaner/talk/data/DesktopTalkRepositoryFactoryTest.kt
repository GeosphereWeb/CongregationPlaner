package de.geosphere.congregationplaner.talk.data

import kotlin.test.Test
import kotlin.test.assertIs

class DesktopTalkRepositoryFactoryTest {
    @Test
    fun `desktop factories create the rest repositories`() {
        assertIs<FirestoreRestTalkScheduleRepository>(createTalkScheduleRepository())
        assertIs<FirestoreRestWeekEventRepository>(createWeekEventRepository())
        assertIs<FirestoreRestTalkCatalogRepository>(createTalkCatalogRepository())
    }
}
