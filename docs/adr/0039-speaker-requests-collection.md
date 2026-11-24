# ADR-0039: Redneranfragen in eigener Sammlung, Termin verweist darauf

## Status
Accepted

## Context
Planer fragen Redner an, bevor diese zusagen, oft ohne sofortige Antwort. Bisher stand eine Anfrage erst nach der Zusage im Termin, sodass ein angefragter Tag frei aussah und doppelt vergeben werden konnte. Planer wollen Kontaktversuche (Anruf, Anrufbeantworter …) festhalten und nachhaken, auch bei Anfragen ohne festen Tag. Freigegebene Mitglieder dürfen Termine mit Wochenereignis komplett lesen (ADR-0037); Kontaktversuche und Notizen sollen sie nicht sehen.

## Decision
- Jede offene Anfrage ist ein Dokument in `talkRequests` (Anlagetag, Notiz, Kontaktversuche). Nur Planer und Helfer lesen es, nur Planer schreiben.
- Eine Anfrage mit Tag ist ein Termin mit Redner und `requestId`. „Ein Tag, ein Termin“ blockiert den Tag damit ohne zusätzliche Regel; Verschieben und Tauschen nehmen nur den Verweis mit. Eine Anfrage ohne Tag trägt Redner und Vortrag selbst, bis ihr ein Tag zugewiesen wird.
- Änderungen an Termin und Anfrage laufen gemeinsam über einen Batch (`SpeakerRequestRepository.commit`). Der Termin-Editor speichert darüber auch das Verschieben (`SaveEditedSlotUseCase`).
- „Informiert“ ist erst nach der Zusage erlaubt. Eine Zusage oder Absage löscht die Anfrage samt Kontaktversuchen.

## Consequences
- Wer Anfragen anzeigt, liest zwei Sammlungen und verbindet sie über die ID (`openRequestsOf`). Eine Anfrage, auf die kein Termin verweist und die keinen Redner hat, gilt als verwaist und wird ausgeblendet.
- Die Regeln prüfen bei den Kontaktversuchen nur Typ und Länge der Liste (≤ 20); jeden Eintrag prüft die App, weil 20 einzeln geprüfte Einträge das Ausdruckslimit von Firestore überschreiten.
- Bearbeiten zwei Planer denselben Termin gleichzeitig, gewinnt der letzte Schreiber (`shortcut:` in `SaveEditedSlotUseCase`).
- Nach einer Zusage gibt es keine Historie der Kontaktversuche mehr.
