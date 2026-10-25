package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkHistoryRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository

actual fun createTalkScheduleRepository(): TalkScheduleRepository = unavailableTalkScheduleRepository("iOS")

actual fun createWeekEventRepository(): WeekEventRepository = unavailableWeekEventRepository("iOS")

actual fun createTalkCatalogRepository(): TalkCatalogRepository = unavailableTalkCatalogRepository("iOS")

actual fun createExternalSpeakerDirectoryRepository(): ExternalSpeakerDirectoryRepository =
    unavailableExternalSpeakerDirectoryRepository("iOS")

actual fun createTalkHistoryRepository(): TalkHistoryRepository = unavailableTalkHistoryRepository("iOS")
