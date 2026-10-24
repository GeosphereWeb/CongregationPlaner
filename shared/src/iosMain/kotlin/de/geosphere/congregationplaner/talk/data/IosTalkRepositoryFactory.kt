package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository

actual fun createTalkScheduleRepository(): TalkScheduleRepository = unavailableTalkScheduleRepository("iOS")

actual fun createWeekEventRepository(): WeekEventRepository = unavailableWeekEventRepository("iOS")
