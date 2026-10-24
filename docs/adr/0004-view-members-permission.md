# ADR-0004: Lese-Berechtigung VIEW_MEMBERS für Mitgliederdaten

## Status
Accepted

## Context
Planer wählen den Redner aus den Mitgliedern ihrer Versammlung. Die Mitgliedsdokumente enthalten aber auch Kontakt- und Zugangsdaten und waren bisher nur für Admins lesbar. Die Rules können Felder eines Dokuments nicht einzeln freigeben, ein Planer soll die Mitglieder nur lesen und nie ändern können.

## Decision
- Neue Berechtigung `UserPermission.VIEW_MEMBERS`, getrennt von `PLAN_TALKS`: Planen allein erlaubt kein Lesen der Mitglieder. Admins behalten ihre Rechte.
- Die Rules erlauben `get` (zusätzlich zum eigenen Dokument) und `list` der Mitglieder nur über `canViewMembers`: globale Admins, Versammlungsadmins dieser Versammlung oder freigegebene Mitglieder dieser Versammlung mit der Berechtigung. Schreiben bleibt unverändert Admins vorbehalten.
- Der Termin speichert Redner und Vorsitzenden als Momentaufnahme (`speakerMemberId`, `speakerLastName`, `speakerFirstName` und entsprechend `chairman…`, je alle drei oder keiner); die Rules prüfen nur die Form, nicht ob das Mitglied existiert oder noch Redner bzw. Ältester oder Dienstamtgehilfe ist. Zur Auswahl stehen Mitglieder mit `isSpeaker` bzw. Brüder mit Dienstamt, beide aus einer gemeinsamen Beobachtung der Mitglieder.

## Consequences
- Wer `VIEW_MEMBERS` erhält, sieht alle Felder aller Mitglieder seiner Versammlung, auch Telefonnummern und E-Mail-Adressen; der Admin vergibt die Berechtigung bewusst. Eine eigene Redner-Collection ohne Kontaktdaten wäre sparsamer, müsste aber parallel gepflegt werden.
- Jeder Lesezugriff auf Mitglieder kostet ein zusätzliches `get()` auf das eigene Mitgliedsdokument.
- Ein Planer ohne `VIEW_MEMBERS` kann keinen Redner auswählen; der Editor weist darauf hin.
- Ein Termin behält den Rednernamen, auch wenn das Mitglied später kein Redner mehr ist oder entfernt wird.
