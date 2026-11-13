package de.geosphere.congregationplaner.talk.presentation.schedule

/** Hinweis am Termin, wenn sein Vortrag in der Vortragsliste nicht mehr aktiv ist. */
internal fun inactiveTalkHint(number: Int): String =
    "Nr. $number ist in der Vortragsliste nicht mehr aktiv; bitte prüfen, ob er noch gehalten wird."
