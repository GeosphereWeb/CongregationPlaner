# ADR-0029: WhatsApp-Vorlagen in einem eigenen Einstellungs-Dokument des Mitglieds

## Status
Accepted

## Context
Beim Wischen eines Termins nach links öffnet WhatsApp (ADR-0020). Der Planer soll dabei einen vorbereiteten Text mit Platzhaltern (`{Vorname}`, `{Datum}`, …) vorfinden und bis zu vier eigene Vorlagen pflegen, eine davon als Standard. Die Vorlagen gehören dem Nutzer und sollen auf all seinen Geräten gelten.

Naheliegend wäre ein Feld im Mitgliedsdokument (`members/{uid}`). Dessen Update-Regel prüft aber bei jedem Schreiben das ganze Profil samt Notfallkontakten (ADR-0016) und liegt damit schon nahe an der Grenze von 1000 ausgewerteten Ausdrücken pro Anfrage. Mit der Prüfung von vier Vorlagen riss die Grenze im Rules-Test, auch wenn die Prüfung nur bei geänderten Vorlagen lief.

## Decision
- Die Vorlagen liegen in `…/members/{uid}/settings/messageTemplates` als `{templates: [{name, text}, …]}`: eins bis vier Einträge, Name 1 bis 40, Text 1 bis 1000 Zeichen, keine Zusatzfelder. Die erste Vorlage ist der Standard; „Als Standard“ rückt eine Vorlage nach vorn, ein eigenes Feld dafür gibt es nicht.
- Nur das Mitglied selbst liest und schreibt das Dokument; andere Dokumente unter `settings` sind nicht erlaubt. Die Regel für das Mitgliedsdokument bleibt unverändert.
- Ohne gespeicherte Vorlagen (oder wenn sie sich nicht lesen lassen) gelten eingebaute Standardvorlagen (`DEFAULT_MESSAGE_TEMPLATES`). Globale Admins ohne Mitgliedsdokument und Personen ohne Konto bekommen sie ebenfalls und können keine eigenen speichern.
- `ObserveMessageTemplatesUseCase` liefert die Vorlagen an die Vortragsplanung und die Einstellungen; das Ausfüllen der Platzhalter (`slotWhatsAppMessages`) ist eine reine, getestete Funktion. Der Text wird als `?text=` an den `wa.me`-Link gehängt.

## Consequences
- Die Regel für die Vorlagen ist klein und unabhängig von der Größe des Profils.
- Ein eigener Lesezugriff je Nutzer und Bildschirm statt der ohnehin geladenen Mitgliedsdaten.
- Entfernt ein Admin ein Mitglied, bleibt dessen Vorlagen-Dokument liegen (Firestore löscht keine Unterdokumente); es ist für niemanden außer dem ehemaligen Mitglied lesbar.
- Weitere persönliche Einstellungen können als eigene Dokumente unter `settings` folgen; die Regel muss sie dann ausdrücklich zulassen.
