# ADR-0020: Wischbare Listeneinträge mit Aktionen, Anrufen und WhatsApp über Links

## Status
Accepted

## Context
Im Wochenplan soll man zum Redner eines Termins telefonieren oder ihm per WhatsApp schreiben können, ohne den Termin zu öffnen. Die Aktionen sollen hinter dem Listeneintrag liegen, den man zur Seite wischt, und die Komponente soll auch in anderen Listen verwendbar sein. Telefon und WhatsApp gibt es auf Android und iOS, nicht auf dem Desktop; Plattformcode für das Starten der Apps wäre für zwei Links unverhältnismäßig.

## Decision
- `SwipeableItemWithActions` und `SwipeActionButton` liegen in `uilib` (`ui/list`). Der Aufrufer hält fest, welcher Eintrag offen ist (`isRevealed`, `onRevealedChange`), damit immer nur einer offen bleibt. Wohin ein losgelassener Eintrag einrastet, ist eine reine, getestete Funktion (`SwipeAnchors.kt`).
- Telefon und WhatsApp starten über Links mit `LocalUriHandler`: `tel:+49…` und `https://wa.me/49…`. Die Nummern bereinigt `internationalNumber` (führende 0 wird zu +49). Nach rechts wischen zeigt je ein Icon für Handy und Festnetz (soweit die Nummer vorhanden ist), nach links wischen das WhatsApp-Logo, das es nur mit einer Handynummer gibt (`SpeakerContact`).
- Die Nummern des Redners kommen mit den ohnehin geladenen Mitgliedern und externen Rednern in die Planung (`MemberRef`, `ExternalSpeaker`). Ohne die Berechtigung „Mitglieder einsehen“ fehlen die Nummern der Mitglieder, der Eintrag ist dann nicht wischbar.
- Auf dem Desktop (`HostPlatform.isDesktop`) werden die Aktionen nicht angeboten. Für TalkBack stehen sie zusätzlich als Aktionsliste am Eintrag.

## Consequences
- Kein Plattformcode und keine Berechtigung nötig: `tel:` öffnet nur den Wählbildschirm, `wa.me` startet WhatsApp oder den Browser.
- Wer keine Telefon- oder WhatsApp-App hat, bekommt vom System einen abgelehnten Link; die App fängt das ab, es passiert dann nichts.
- „WhatsApp nur mit Handynummer“ ist fest; eine Nummer, die sich nicht wählen lässt (keine Vorwahl, zu kurz), zählt als nicht vorhanden.
- Das WhatsApp-Logo ist eine Marke von WhatsApp und steht nur als Verweis auf die App; es stammt aus Simple Icons (CC0).
- Weitere Listen können die Komponente übernehmen, müssen ihre Aktionen und die Nummernquelle aber selbst liefern.
