# Umsetzungsplan: Planungsassistent für Redner und Vorträge

Stand: 2026-11-11. Noch nichts umgesetzt; Schritt 1 wartet auf Freigabe.

## Ziel

Beim Planen der Vorträge schlägt die App nur Redner und Vorträge vor, die seit einem Stichtag nicht eingeplant
waren. Die Planung soll einfach und übersichtlich sein, die Planungsliste bleibt beim Planen sichtbar, und ein
geplanter Vortrag mit Redner lässt sich leicht auf einen anderen Tag umplanen.

## Fachliche Regel

- Zwei Stichtage pro Versammlung: einer für Redner, einer für Vorträge.
- Ein Redner oder Vortrag ist gesperrt, wenn er an irgendeinem Tag ab dem Stichtag eingeplant ist, vergangen oder
  zukünftig. Der gerade bearbeitete Termin zählt nicht mit.
- Frei eingetragene Redner (ohne ID) und Vorträge (ohne Nummer) werden nicht gesperrt und sperren niemanden.

## Entscheidungen

- Stichtage gelten pro Versammlung (nicht pro Gerät): Dokument `…/congregation/{id}/settings/talkPlanning` mit
  `speakerFreeSince` und `talkFreeSince` (`yyyy-MM-dd`). Lesen: Planer und Vortragshelfer; schreiben: Planer.
- Die Historie wird keine eigene Sammlung mehr: `talkHistory` entfällt. Ein nachgetragener Tag ist ein normaler
  Termin in `talkSchedule` in der Vergangenheit, damit auch mit Redner-ID. Die Historie-Ansicht liest die Termine
  und gruppiert sie nach Vortrag.
- Redner und Vortrag lassen sich frei eintragen, wenn sie nicht in der Liste stehen (beim Planen und Nachtragen):
  Redner als Name ohne ID, Vortrag als Titel ohne Nummer.
- Keine Migration, keine Rücksicht auf Altdaten (Testumgebung). Vorhandene `talkHistory`-Daten werden gelöscht;
  von Hand nachgetragene Tage gehen dabei verloren.
- Umplanen per Drag-and-drop ist bewusst nicht enthalten.

## Ausgangslage im Code

- Termin = ein Dokument pro Datum (`talkSchedule/{yyyy-MM-dd}`), geladen wird nur je Kalenderjahr (`observeYear`).
- Der Import schreibt auch vergangene Termine mit Redner-ID nach `talkSchedule`; die Historie wird daraus abgeleitet
  (`SyncTalkHistoryWithSlotUseCase`, `planTalkHistory`).
- Auswahldialoge filtern nur nach Suchtext (`SpeakerSearch.kt`, `TalkSearch.kt`).
- `MoveTalkSlotUseCase` ändert das Datum eines ganzen Termins und überschreibt den Zieltag.
- Tablet/Desktop zeigen Liste und Editor nebeneinander (`ListDetailPaneScaffold`); auf dem Handy verdeckt der Editor
  die Liste.

## Schritte (je ein Jira-Ticket und PR)

1. **Historie auf Termine umstellen.** `talkHistory` samt Abgleich, Regeln und Import-Teil entfernen. Die
   Historie-Ansicht liest die Termine; Nachtragen legt einen Termin in der Vergangenheit an. Termine über
   Jahresgrenzen laden (Abfrage „ab Datum“ für Android und Desktop). ADR, Rules-Test, Deploy, Emulator-Kopie der
   Regeln, Datenbereinigung in Cloud und Emulator.
2. **Freie Eingabe von Redner und Vortrag.** Im Auswahldialog wird der Suchtext mit „„…“ frei eintragen“ übernommen.
   Regeln lockern (Name ohne ID, Titel ohne Nummer), Rules-Test. Freie Einträge sind als „frei eingetragen“ markiert.
   Ein freier Redner ist nur ein Name (die Versammlung kann in Klammern dahinter stehen); ein freier Vortrag hat keine
   Nummer und erscheint deshalb nicht in der Historie nach Nummern.
3. **Stichtage pro Versammlung.** Modell, Repository, UseCases, Regeln mit Rules-Test. Zeile oben in der Planung:
   „Redner frei seit … · Vorträge frei seit …“ mit Datumsauswahl.
4. **Sperrlogik und Vorschläge.** Reine Funktion mit Tests (Stichtag-Grenze, Zukunft, eigener Termin, externer
   Redner, freie Einträge). Dialoge zeigen standardmäßig nur freie Einträge, sortiert nach „am längsten nicht dran“,
   mit letztem Datum; Schalter „alle anzeigen“ blendet gesperrte ausgegraut mit Grund ein. Bei externen Rednern
   greifen ihre hinterlegten Vortragsnummern in beide Richtungen.
5. **Planungsliste auf dem Handy sichtbar.** Editor als halbhohes Bottom-Sheet; die Liste scrollt zum bearbeiteten
   Termin.
6. **Umplanen und Tauschen.** `ReassignTalkUseCase` überträgt Vortrag, Redner und Bestätigung auf den Zieltermin und
   leert den Quelltermin; Ereignis, Uhrzeit und Vorsitz bleiben am Tag. Belegter Zieltag: Rückfrage „Tauschen?“.
   Beide Termine in einem Schreibvorgang. Bedienung: „Verschieben“ am Termin, dann Zieltag antippen.

Reihenfolge: 1 → 2 → 3 → 4; 6 ist unabhängig und kann vorgezogen werden; 5 zuletzt.

## Bekannte Einschränkungen und offene Punkte

- Pro Tag gibt es genau einen Termin, also lässt sich nur ein Vortrag je Tag nachtragen.
- Nicht geprüft: ob in den Testdaten von Hand nachgetragene Historie-Tage liegen, die mit Schritt 1 verloren gingen.
