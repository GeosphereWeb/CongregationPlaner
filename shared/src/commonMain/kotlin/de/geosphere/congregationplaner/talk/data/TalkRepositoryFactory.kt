package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkHistoryRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate

expect fun createTalkScheduleRepository(): TalkScheduleRepository

expect fun createWeekEventRepository(): WeekEventRepository

expect fun createTalkCatalogRepository(): TalkCatalogRepository

expect fun createExternalSpeakerDirectoryRepository(): ExternalSpeakerDirectoryRepository

expect fun createTalkHistoryRepository(): TalkHistoryRepository

internal const val TALK_SCHEDULE_SUBCOLLECTION = "talkSchedule"

/** Wann welcher Vortrag in der Versammlung gehalten wurde; ein Dokument je Vortragsnummer. */
internal const val TALK_HISTORY_SUBCOLLECTION = "talkHistory"

/** Verzeichnis externer Redner (Kreis → Versammlung → Redner), je eigener Versammlung gepflegt. */
internal const val EXTERNAL_CIRCUITS_SUBCOLLECTION = "externalCircuits"
internal const val EXTERNAL_CONGREGATIONS_SUBCOLLECTION = "externalCongregations"
internal const val VISITING_SPEAKERS_SUBCOLLECTION = "visitingSpeakers"

/** Globale Liste der Wochenereignisse; sie gilt für alle Versammlungen und Kreise. */
internal const val WEEK_EVENTS_COLLECTION_PATH = "weekEvents"

/** Globale Vortragsliste; die Dokument-ID ist die Vortragsnummer. */
internal const val TALK_CATALOG_COLLECTION_PATH = "talkCatalog"

internal val CongregationRef.talkSchedulePath: String get() = "$documentPath/$TALK_SCHEDULE_SUBCOLLECTION"

internal val CongregationRef.talkHistoryPath: String get() = "$documentPath/$TALK_HISTORY_SUBCOLLECTION"

internal val CongregationRef.externalCircuitsPath: String get() = "$documentPath/$EXTERNAL_CIRCUITS_SUBCOLLECTION"

internal val CongregationRef.externalCongregationsPath: String
    get() = "$documentPath/$EXTERNAL_CONGREGATIONS_SUBCOLLECTION"

internal val CongregationRef.visitingSpeakersPath: String get() = "$documentPath/$VISITING_SPEAKERS_SUBCOLLECTION"

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
    private fun <T> unavailable(): Result<T> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

    override fun observeWeekEvents(): Flow<List<WeekEvent>> = flow {
        throw UnsupportedOperationException("Firestore is not configured for $platform.")
    }

    override suspend fun save(event: WeekEvent): Result<WeekEvent> = unavailable()

    override suspend fun delete(id: String): Result<Unit> = unavailable()
}

fun unavailableTalkCatalogRepository(platform: String): TalkCatalogRepository = object : TalkCatalogRepository {
    private fun <T> unavailable(): Result<T> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

    override fun observeTalks(): Flow<List<Talk>> = flow {
        throw UnsupportedOperationException("Firestore is not configured for $platform.")
    }

    override suspend fun save(talk: Talk): Result<Talk> = unavailable()

    override suspend fun delete(number: Int): Result<Unit> = unavailable()
}

fun unavailableTalkHistoryRepository(platform: String): TalkHistoryRepository = object : TalkHistoryRepository {
    private fun <T> unavailable(): Result<T> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

    override fun observeAll(congregation: CongregationRef): Flow<List<TalkHistoryEntry>> = flow {
        throw UnsupportedOperationException("Firestore is not configured for $platform.")
    }

    override suspend fun save(congregation: CongregationRef, entry: TalkHistoryEntry): Result<TalkHistoryEntry> =
        unavailable()

    override suspend fun delete(congregation: CongregationRef, talkNumber: Int): Result<Unit> = unavailable()
}

fun unavailableExternalSpeakerDirectoryRepository(platform: String): ExternalSpeakerDirectoryRepository =
    object : ExternalSpeakerDirectoryRepository {
        private fun <T> unavailable(): Result<T> =
            Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

        private fun <T> unavailableFlow(): Flow<List<T>> = flow {
            throw UnsupportedOperationException("Firestore is not configured for $platform.")
        }

        override fun observeCircuits(congregation: CongregationRef): Flow<List<ExternalCircuit>> = unavailableFlow()

        override fun observeCongregations(congregation: CongregationRef): Flow<List<ExternalCongregation>> =
            unavailableFlow()

        override fun observeSpeakers(congregation: CongregationRef): Flow<List<VisitingSpeaker>> = unavailableFlow()

        override suspend fun saveCircuit(
            congregation: CongregationRef,
            circuit: ExternalCircuit,
        ): Result<ExternalCircuit> = unavailable()

        override suspend fun saveCongregation(
            congregation: CongregationRef,
            externalCongregation: ExternalCongregation,
        ): Result<ExternalCongregation> = unavailable()

        override suspend fun saveSpeaker(
            congregation: CongregationRef,
            speaker: VisitingSpeaker,
        ): Result<VisitingSpeaker> = unavailable()

        override suspend fun deleteCircuit(congregation: CongregationRef, id: String): Result<Unit> = unavailable()

        override suspend fun deleteCongregation(congregation: CongregationRef, id: String): Result<Unit> = unavailable()

        override suspend fun deleteSpeaker(congregation: CongregationRef, id: String): Result<Unit> = unavailable()
    }
