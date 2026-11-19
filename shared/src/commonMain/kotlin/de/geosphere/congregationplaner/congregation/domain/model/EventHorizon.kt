package de.geosphere.congregationplaner.congregation.domain.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private const val DEFAULT_MONTHS = 3
private const val MAX_AMOUNT_DIGITS = 3

/**
 * Wie weit die Startseite die Termine mit Wochenereignis im Voraus zeigt (ADR-0037): [amount] Tage oder Monate ab
 * heute. Gespeichert als `event_horizon` am Versammlungsdokument, z. B. `3M` oder `30D`.
 */
data class EventHorizon(val amount: Int = DEFAULT_MONTHS, val unit: Unit = Unit.MONTHS) {
    enum class Unit(val code: Char, val max: Int, val label: String) {
        DAYS('D', MAX_DAYS, "Tage"),
        MONTHS('M', MAX_MONTHS, "Monate"),
    }

    val isValid: Boolean get() = amount in 1..unit.max

    /** Der letzte Tag, der noch gezeigt wird. */
    fun lastDay(today: LocalDate): LocalDate = when (unit) {
        Unit.DAYS -> today.plus(DatePeriod(days = amount))
        Unit.MONTHS -> today.plus(DatePeriod(months = amount))
    }

    fun encode(): String = "$amount${unit.code}"

    /** Die Zahl als Text fürs Eingabefeld; ein geleertes Feld bleibt leer. */
    val amountText: String get() = if (amount == 0) "" else amount.toString()

    /** Übernimmt die Eingabe des Zahlenfelds: nur Ziffern, höchstens drei; leer heißt 0 (ungültig). */
    fun withAmountText(text: String): EventHorizon =
        copy(amount = text.filter(Char::isDigit).take(MAX_AMOUNT_DIGITS).toIntOrNull() ?: 0)

    companion object {
        const val MAX_DAYS = 365
        const val MAX_MONTHS = 24

        /** Ein fehlender oder fehlerhafter Wert ergibt den Standard von drei Monaten. */
        fun decode(value: String?): EventHorizon {
            val unit = Unit.entries.firstOrNull { it.code == value?.lastOrNull() } ?: return EventHorizon()
            val horizon = EventHorizon(value!!.dropLast(1).toIntOrNull() ?: 0, unit)
            return if (horizon.isValid) horizon else EventHorizon()
        }
    }
}
