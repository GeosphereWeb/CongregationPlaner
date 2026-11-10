package de.geosphere.congregationplaner.talk.domain.usecase

/** Wie viele Termine [CreateYearSlotsUseCase] angelegt hat und wie viele es schon gab. */
data class YearSlotsResult(val created: Int, val alreadyExisting: Int)
