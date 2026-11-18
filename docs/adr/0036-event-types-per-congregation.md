# ADR-0036: Ereignisarten je Versammlung mit Vorlage des Landes

## Status
Accepted

## Context
Termine der Wochenendplanung verweisen auf die Wochenereignisse des Landes (ADR-0034), besondere Ereignisse haben nur einen freien Titel. Ein Icon pro Ereignis ließ sich so nur über Stichwörter im Titel zuordnen, und eine Umbenennung verlor es. Versammlungen wollen ihre Ereignisse außerdem selbst pflegen.

## Decision
- Jede Versammlung hat eine eigene Liste `…/congregation/{id}/eventTypes/{id}` mit Name, „Vortrag entfällt“ und Icon (`EventType`). Termine und besondere Ereignisse verweisen per ID darauf.
- Die Wochenereignisse des Landes bleiben die Vorlage: Der Versammlungsadmin übernimmt sie per Knopf; ergänzt werden nur Arten, deren Name noch fehlt (`AdoptEventTypeBlueprintUseCase`).
- Das Icon wird als Name der Ressource gespeichert (`ic_cp_…`) und über `Res.allDrawableResources` aufgelöst; unbekannte Namen zeigen das Quadrat. Ressourcen-IDs (`Int`) wären zwischen Builds nicht stabil.
- Lesen dürfen freigegebene Mitglieder; ändern und löschen nur der Versammlungsadmin. Der Vortragsplaner darf anlegen, damit der Termin-Import fehlende Arten ergänzen kann.

## Consequences
- Eine neue Versammlung startet ohne Arten, bis der Admin die Vorlage übernimmt; Änderungen an der Vorlage erreichen bestehende Versammlungen nur über erneutes Übernehmen.
- Gelöschte Arten hinterlassen Verweise; Termine behalten ihren mitgespeicherten Namen und zeigen das Standard-Icon.
- Umbenannte oder entfernte Icon-Dateien im App-Satz fallen still auf das Quadrat zurück.
