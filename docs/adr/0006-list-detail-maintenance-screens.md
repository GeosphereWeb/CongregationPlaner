# ADR-0006: Pflegeoberflächen als Liste/Detail mit sofortiger Listenaktualisierung

## Status
Accepted

## Context
Pflegeoberflächen für Datensätze (Versammlungs-Metadaten, Kreise, Mitglieder, Vortragsliste, Wochenereignisse) sollen auf Handy und Desktop gleich bedienbar sein. Der Snapshot-Flow von Firestore liefert am Desktop Änderungen teils verzögert, sodass neu gespeicherte Einträge erst später in der Liste erscheinen.

## Decision
- Jede Pflegeoberfläche besteht aus einem `ListDetailPaneScaffold`: Liste mit `SelectableListCard`, Detail mit Editor bzw. Platzhalter. Der Zustand implementiert `ListDetailState`, die Navigation übernimmt `rememberListDetailNavigation`; auf kleinen Bildschirmen gibt es „Zurück zur Liste“.
- Vorlage ist `CongregationMetadataContent`. Neue Pflege-Features folgen diesem Aufbau, Abweichungen werden begründet.
- Das ViewModel übernimmt gespeicherte und gelöschte Einträge sofort in die Liste im UI-Zustand; der Flow bleibt Quelle der Wahrheit und überschreibt sie danach.

## Consequences
- Einheitliche Bedienung und weniger neuer UI-Code pro Feature.
- Der Zustand kennt Liste und Editor zugleich; Listenpflege im ViewModel steht neben dem Flow und muss dieselbe Sortierung wie der Use Case verwenden.
