package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate

expect fun createTalkScheduleRepository(): TalkScheduleRepository

expect fun createWeekEventRepository(): WeekEventRepository

expect fun createTalkCatalogRepository(): TalkCatalogRepository

expect fun createExternalSpeakerRepository(): ExternalSpeakerRepository

internal const val TALK_SCHEDULE_SUBCOLLECTION = "talkSchedule"

/** Redner aus anderen Versammlungen, je Versammlung gepflegt. */
internal const val EXTERNAL_SPEAKERS_SUBCOLLECTION = "externalSpeakers"

/** Globale Liste der Wochenereignisse; sie gilt für alle Versammlungen und Kreise. */
internal const val WEEK_EVENTS_COLLECTION_PATH = "weekEvents"

/** Globale Vortragsliste; die Dokument-ID ist die Vortragsnummer. */
internal const val TALK_CATALOG_COLLECTION_PATH = "talkCatalog"

internal val CongregationRef.talkSchedulePath: String get() = "$documentPath/$TALK_SCHEDULE_SUBCOLLECTION"

internal val CongregationRef.externalSpeakersPath: String get() = "$documentPath/$EXTERNAL_SPEAKERS_SUBCOLLECTION"

/** Die Dokument-IDs des Jahres sind ISO-Daten und damit lexikographisch geordnet. */
internal fun yearIdRange(year: Int): ClosedRange<String> =
    LocalDate(year, 1, 1).toString()..LocalDate(year, 12, 31).toString()

fun unavailableTalkScheduleRepository(platform: String): TalkScheduleRepository = object : TalkScheduleRepository {
    private fun <T> unavailable(): Result<T> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

    override fun observeYear(congregation: CongregationRef, year: Int): Flow<List<TalkSlot>> = flow {
        throw UnsupportedOperationException("Firestore is not configured for $platform.")
    }

    override suspend fun saveSlot(congregation: CongregationRef, slot: TalkSlot): Result<TalkSlot> = unavailable()

    override suspend fun deleteSlot(congregation: CongregationRef, date: LocalDate): Result<Unit> = unavailable()
}

fun unavailableWeekEventRepository(platform: String): WeekEventRepository = object : WeekEventRepository {
    override fun observeWeekEvents(): Flow<List<WeekEvent>> = flow {
        throw UnsupportedOperationException("Firestore is not configured for $platform.")
    }
}

fun unavailableTalkCatalogRepository(platform: String): TalkCatalogRepository = object : TalkCatalogRepository {
    override fun observeTalks(): Flow<List<Talk>> = flow {
        throw UnsupportedOperationException("Firestore is not configured for $platform.")
    }
}

fun unavailableExternalSpeakerRepository(platform: String): ExternalSpeakerRepository =
    object : ExternalSpeakerRepository {
        private fun <T> unavailable(): Result<T> =
            Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

        override fun observeAll(congregation: CongregationRef): Flow<List<ExternalSpeaker>> = flow {
            throw UnsupportedOperationException("Firestore is not configured for $platform.")
        }

        override suspend fun save(congregation: CongregationRef, speaker: ExternalSpeaker): Result<ExternalSpeaker> =
            unavailable()

        override suspend fun delete(congregation: CongregationRef, id: String): Result<Unit> = unavailable()
    }
