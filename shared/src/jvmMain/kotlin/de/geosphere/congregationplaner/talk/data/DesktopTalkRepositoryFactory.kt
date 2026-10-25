package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkHistoryRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository

actual fun createTalkScheduleRepository(): TalkScheduleRepository = FirestoreRestTalkScheduleRepository()

actual fun createWeekEventRepository(): WeekEventRepository = FirestoreRestWeekEventRepository()

actual fun createTalkCatalogRepository(): TalkCatalogRepository = FirestoreRestTalkCatalogRepository()

actual fun createExternalSpeakerDirectoryRepository(): ExternalSpeakerDirectoryRepository =
    FirestoreRestExternalSpeakerDirectoryRepository()

actual fun createTalkHistoryRepository(): TalkHistoryRepository = FirestoreRestTalkHistoryRepository()
