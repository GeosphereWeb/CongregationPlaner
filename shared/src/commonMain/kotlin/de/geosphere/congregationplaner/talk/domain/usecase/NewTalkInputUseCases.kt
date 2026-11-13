package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkReferenceException
import dev.zacsweers.metro.Inject

/** Die eingegebene Vortragsnummer: nur Ziffern, höchstens so viele, wie [Talk.MAX_NUMBER] hat. */
@Inject
class CleanTalkNumberInputUseCase {
    operator fun invoke(text: String): String = text.filter(Char::isDigit).take(Talk.MAX_NUMBER.toString().length)
}

/** Ein neuer Vortrag braucht eine Nummer, die es in [talks] noch nicht gibt; `null`, wenn alles passt. */
@Inject
class CheckNewTalkNumberUseCase {
    operator fun invoke(number: Int?, talks: List<Talk>): TalkReferenceException? = when {
        number == null -> TalkReferenceException.MissingNumber()
        talks.any { it.number == number } -> TalkReferenceException.NumberTaken(number)
        else -> null
    }
}
