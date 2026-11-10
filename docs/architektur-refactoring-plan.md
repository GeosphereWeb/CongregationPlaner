# Umsetzungsplan: Architektur-Refactoring

Stand: 2026-11-10. Dieser Plan beschreibt, wie der Code der App auf eine strikte Trennung der Zuständigkeiten gebracht wird. Er ist so geschrieben, dass die Arbeit in einer späteren Sitzung ohne Vorwissen fortgesetzt werden kann.

## Fortschritt

Nach jeder Phase hier abhaken und Ticket und PR eintragen.

| Phase | Inhalt | Ticket | PR | Status |
|---|---|---|---|---|
| 0 | Package-Struktur | CP-130 | #148 | in Review |
| 1 | Policies vereinheitlichen | CP-131 | – | in Review |
| 2 | Login entkoppeln | – | – | offen |
| 3 | Logik aus Composables holen | – | – | offen |
| 4 | Große Klassen aufteilen | – | – | offen |
| 5 | `uilib` auffüllen | – | – | offen |
| 6 | UiState-Feinschliff | – | – | offen |

Nächster Schritt: Phase 2 (Login entkoppeln). Die Branches sind gestapelt: jede Phase baut auf dem Branch der vorigen auf, bis deren PR gemergt ist.

## Ziele

- Composables sind „dumm“: Sie rendern Zustand und leiten Ereignisse weiter, ohne Geschäftslogik.
- Wiederverwendbare Composables (Atoms und darauf aufbauende Bausteine) liegen im Modul `uilib`.
- ViewModels halten die darzustellenden Daten als `UiState`.
- Jede Aufgabe hat einen eigenen UseCase.
- Zugriffsrechte liegen in separaten Policies und werden an genau einer Stelle durchgesetzt.
- Dateien und Klassen liegen in logisch geclusterten Packages.
- Keine Monsterklassen: Jede Klasse tut möglichst wenig, das aber gut.

Blueprint für die Policies ist das Projekt `NewSpeechPlan` (`data/.../authentication/permission`). Dessen Lösung wird hier nicht kopiert, sondern verbessert (siehe Zielbild Policies).

## Ausgangslage

Grundlage ist eine Suche über den Code vom 2026-11-10; nicht jede Datei wurde vollständig gelesen. Pfade beziehen sich auf `shared/src/commonMain/kotlin/de/geosphere/congregationplaner/`.

### Was schon passt

- Sechs Policies, per Metro injiziert: `UserPermissionPolicy`, `CongregationPermissionPolicy`, `OrganizationPermissionPolicy`, `MidweekPermissionPolicy`, `TalkSchedulePermissionPolicy`, `TalkReferenceDataPolicy`.
- `NavigationAccess` leitet die sichtbaren Bereiche ausschließlich aus den Policies ab.
- Policy-Tests existieren: `PermissionPolicyTest`, `TalkSchedulePermissionPolicyTest`, `TalkReferenceDataPolicyTest`, `NavigationAccessTest`.
- Kein ViewModel greift direkt auf ein Repository zu; alle gehen über UseCases.
- Jeder Screen hat eine `…UiState`-Datenklasse; die meisten Screens sind in Route, Screen und Content geteilt.
- `uilib` hängt nur an `theming` und Compose und enthält 19 Bausteine.

### Lücken

| Bereich | Lücke |
|---|---|
| Policy-Durchsetzung | `talk` und `organization` prüfen im UseCase, `user` und `congregation` nur im ViewModel |
| Policy-Zugriff in der UI | `UserManagementViewModel` bietet `canEdit(member)`, `canEditRoles(member)`, `canToggleRole`, `canEditProfile` als Funktionen an, statt die Rechte im State zu liefern |
| Policy-Ort | Policies für Versammlung, Organisation und Midweek liegen in `user/domain/permission` |
| Login | `App.kt` (614 Zeilen) hält den Login-Zustand in `remember` und ruft `FirebaseAuthManager` direkt auf: kein ViewModel, kein UseCase |
| Logik in Composables | Suchfilter in `MemberPickerDialog`, `SpeakerPickerContent`, `TalkPickerDialog`; Gruppierung in `ExternalDirectoryContent`; Längenbegrenzung per `.take(MAX…)` in `OrganizationContent`, `TalkSlotEditor`, `ContactFields`; Titelwahl in `CongregationEditor` |
| Packages | `talk/presentation` (28 Dateien), `user/presentation` (15), `congregation/presentation` (12) sind flach; `importer` (12 Dateien) und `auth` haben keine Schichten |
| UseCase-Dateien | Sammeldateien: `TalkScheduleUseCases.kt` (10 Klassen), `ExternalSpeakerDirectoryUseCases.kt` (8), `TalkHistoryUseCases.kt` (4), `TalkReferenceUseCases.kt` (4), `OrgUnitUseCases.kt` (3), `ImportUseCases.kt` (3) |
| `uilib` | Generische Teile liegen noch in `shared`: `ListDetailPane`, `DetailPlaceholder`, `ListPaneHeader`, `SelectableListCard`, `ProfileField`, `PhoneField`, Zeit- und Datumseingabe |

### Zu große Klassen und Dateien

| Datei | Größe | Vermischte Aufgaben |
|---|---|---|
| `TalkScheduleViewModel` | 510 Zeilen, 34 Funktionen, 13 Abhängigkeiten | Terminliste und Filter, Termin-Editor, Jahr anlegen und Jahreszeit ändern, Laden der Stammdaten |
| `ExternalDirectoryViewModel` | 354 Zeilen, 25 Funktionen, 8 Abhängigkeiten | Filter und Suche, drei Editoren (Kreis, Versammlung, Redner) |
| `CongregationSelectionViewModel` | 311 Zeilen, 16 Funktionen | Auswahl im Organisationsbaum, neue Versammlung anlegen, Einladungslink |
| `UserManagementViewModel` | 306 Zeilen, 22 Funktionen | Mitgliederliste, Rechteabfragen, Profil-Editor, Rollen und Freigabe |
| `CongregationMetadataViewModel` | 263 Zeilen, 14 Funktionen | Liste, Editor, Ableitung der Rechte |
| `ExternalDirectoryContent.kt` | 617 Zeilen, 18 Composables | Liste, Filterleiste, drei Editoren |
| `App.kt` | 614 Zeilen | App-Wurzel, Login-Zustand, Login-UI |
| `TalkScheduleScreen.kt` | 577 Zeilen | mehrere Bausteine |
| `TalkSlotEditor.kt` | 465 Zeilen, 11 Composables | mehrere Bausteine |
| `UserManagementContent.kt` | 375 Zeilen | mehrere Bausteine |

detekt begrenzt Klassen auf 11 Funktionen (`TooManyFunctions`), Funktionen auf 60 Zeilen (`LongMethod`) und Klassen auf 600 Zeilen (`LargeClass`). Warum die großen ViewModels trotzdem durchgehen (Suppression oder Baseline), ist noch nicht geprüft.

## Zielbild

### Policies

1. **Ein Durchsetzungspunkt.** Jeder schreibende UseCase prüft seine Policy selbst und liefert `Result.failure` mit der vorhandenen Exception des Fachbereichs. ViewModels prüfen nicht mehr; sie leiten nur noch UI-Rechte ab.
2. **Rechte als State.** Pro Screen gibt es eine kleine `…Permissions`-Datenklasse im `UiState`, Standard „alles verboten“, bei Listen pro Zeile. Composables rufen nie eine `can…`-Funktion auf. Vorbild ist `CongregationMetadataViewModel`.
3. **Kein wiederholter Rollencode.** Eine Erweiterung `AppUser.hasRole(role)` ersetzt das wiederholte `approved && X in roles`.
4. **Policy im eigenen Fachpaket.** Jede Policy liegt unter `<feature>/domain/permission`.
5. **Tabellengetriebene Tests.** Je Policy ein Test über Rolle × Aktion, als Gegenstück zum Firestore-Rules-Test.

Die verbindliche Durchsetzung bleibt in `firebase/firestore.rules`; die Policies sind die Client-Seite davon.

### Package-Struktur

```
<feature>/
  domain/{model, repository, usecase, permission}
  data/{mappers}
  di/
  presentation/<screen>/
```

- `talk/presentation`: `schedule`, `directory`, `history`, `reference`, `picker`, `common`.
- `user/presentation`: `management`, `profile`, `access`.
- `importer` und `auth`: dieselben Schichten wie die anderen Fachpakete.
- Ein UseCase pro Datei.

### ViewModels und UiState

- Ein ViewModel pro Screen, ein `UiState` pro Screen aus einer Quelle (entschieden am 2026-11-10, konform zu ADR-0006).
- Unabhängiger Zustand wird ein eigenes ViewModel, zum Beispiel die Jahresverwaltung im Wochenplan oder Einladungslink und Neuanlage in der Versammlungsauswahl.
- Gekoppelter Zustand (Liste und Editor) bleibt ein ViewModel. Die Zustandsübergänge des Editors werden reine, getestete Funktionen in eigenen Dateien.
- Liegt ein ViewModel danach noch über 11 Funktionen, wird es doch geteilt, mit ADR und nach Rücksprache.
- Datenklassen mit `isLoading` und `errorMessage` bleiben für Editor-Screens. Sealed States nur dort, wo sich Zustände ausschließen (wie `AdminContactsState`).
- ViewModels werden wie bisher in `App()` per `remember { createGraph<…>() }` gehalten (ADR-0019), kein `viewModel { … }`.

### Composables und `uilib`

- Composable mit ViewModel- oder Navigationsbezug wird in Route und zustandslosen Content geteilt.
- Ein Baustein pro Datei, jeweils mit Preview.
- `uilib` kennt keine Domain-Typen und nutzt das Material-Theme aus ADR-0014.

## Phasen

Jede Phase bekommt ein eigenes Jira-Ticket, einen Branch und einen PR.

### Phase 0: Package-Struktur

Reiner Verschiebe-PR ohne inhaltliche Änderung, damit die Git-Historie die Umbenennungen erkennt.

1. `talk/presentation` in `schedule`, `directory`, `history`, `reference`, `picker`, `common` aufteilen.
2. `user/presentation` in `management`, `profile`, `access` aufteilen.
3. `congregation/presentation` und `organization/presentation` prüfen und bei Bedarf nach Screen aufteilen; `organization/InviteLinkInbox.kt` einordnen.
4. `importer` in `domain/model`, `domain/usecase`, `presentation`, `di` aufteilen.
5. `auth` in `domain/repository` und weitere Schichten aufteilen.
6. Die sechs UseCase-Sammeldateien in eine Klasse pro Datei teilen.
7. Tests (`commonTest`, `jvmTest`, `androidHostTest`) und Plattform-SourceSets (`androidMain`, `jvmMain`, `iosMain`) in die parallelen Pakete umziehen.
8. Imports anpassen; `jvmTest`, `testAndroidHostTest` und `check` laufen lassen.

Im PR begründen, dass ein reiner Verschiebe-PR keine neuen Tests braucht.

Umgesetzt in CP-130 (Abweichungen vom Entwurf):

- `user/presentation` hat zusätzlich `contact` für die Kontaktbausteine, die Profil und Nutzerverwaltung gemeinsam nutzen (`ContactFields`, `ContactIcons`, `MemberProfileEditing`, `PhoneVisualTransformation`).
- `organization/presentation` ist in `selection` (inklusive `InviteLinkInbox`) und `management` geteilt.
- `congregation/presentation` bleibt flach, weil es nur einen Screen enthält.
- `importer`: `domain/model`, `domain/planning` (Tabellen-Auswertung), `domain/usecase`, `data` (Dateiquellen, `XlsxReader`), `presentation`, `di`.
- `auth`: Interfaces in `domain/repository`; `FirebaseAuthManager` (mit der `expect`-Fabrik), `FirebaseSupport` und alle Plattform-Implementierungen in `data`, damit `expect` und `actual` im selben Paket liegen.
- Gemeinsame private Helfer der geteilten UseCase-Dateien liegen als `internal` in eigenen Dateien (`ExternalDirectoryUseCaseSupport`, `OrgUnitUseCaseSupport`, `ImportWriting`); `TalkMembers`, `YearSlotsResult` und `ImportReport` haben eigene Dateien.
- Die Tests der Sammeldateien (`TalkScheduleUseCasesTest` usw.) sind unverändert geblieben.

### Phase 1: Policies vereinheitlichen

1. `AppUser.hasRole(role)` einführen und in allen Policies verwenden.
2. `CongregationPermissionPolicy` nach `congregation/domain/permission`, `OrganizationPermissionPolicy` nach `organization/domain/permission` verschieben; für `MidweekPermissionPolicy` den passenden Ort festlegen.
3. Policy-Prüfung in die schreibenden UseCases von `user` ziehen: `UpdateMemberUseCase`, `UpdateMemberProfileUseCase`, `RemoveMemberUseCase`, `CreateMemberWithoutAccountUseCase`.
4. Policy-Prüfung in die schreibenden UseCases von `congregation` ziehen: `CreateCongregationUseCase`, `SaveCongregationUseCase`, `DeleteCongregationUseCase`.
5. Die doppelten Prüfungen aus `UserManagementViewModel` und `CongregationMetadataViewModel` entfernen.
6. `UserManagementViewModel`: `can…`-Funktionen durch Rechte pro Mitglied im `UserManagementUiState` ersetzen; `UserManagementContent` liest nur noch den State.
7. Übrige ViewModels auf dasselbe Muster prüfen (`TalkScheduleViewModel`, `ExternalDirectoryViewModel`, `TalkHistoryViewModel`, `ImportViewModel`, `UserAccessViewModel.canManageUsers`).
8. Policies gegen `firebase/firestore.rules` abgleichen. Abweichungen im selben PR in den Rules beheben und `node firebase/rules-test/run.mjs` ausführen.
9. Tests: Policy-Tests tabellengetrieben erweitern; je UseCase ein Test „ohne Recht abgelehnt“; ViewModel-Tests auf den neuen State anpassen.
10. ADR-0024 „Policy-Durchsetzung im UseCase, Rechte als UiState“ schreiben und im Index eintragen.

Risiko: Bestehende ViewModel-Tests können brechen, wenn die UseCases neu ablehnen. Sie werden angepasst, nicht abgeschwächt.

Umgesetzt in CP-131:

- `MidweekPermissionPolicy` liegt in `midweek/domain/permission`.
- `UpdateMemberUseCase` ist in `SetMemberApprovalUseCase` und `SetMemberRoleUseCase` geteilt, weil Freigabe und Rollen unterschiedliche Rechte brauchen; das Anlegen einer Versammlung in `CreateCongregationUseCase` (globaler Admin) und `CreateOwnCongregationUseCase` (Login, ADR-0018).
- Die Talk-ViewModels prüfen vor dem Öffnen eines Editors weiterhin selbst (UI-Steuerung); die Schreibaktionen schützen ihre UseCases schon.
- `UserAccessViewModel.canManageUsers` ist entfernt (nicht genutzt).
- Abgleich mit `firebase/firestore.rules`: keine Abweichung, die Rules bleiben unverändert. Nur der Schutz der letzten Admin-Rolle existiert allein in der App.

### Phase 2: Login entkoppeln

1. UseCases für Anmelden, Registrieren, Abmelden und E-Mail-Verifizierung über das vorhandene `FirebaseAuthRepository` anlegen. Kein neues `expect`/`actual`, keine neue Library.
2. `AuthViewModel` mit `LoginUiState` anlegen (Modus, Felder, Fehler, Hinweis, laufende Aktion); Trimmen und Pflichtfeldprüfung wandern dorthin.
3. `App.kt` teilen: App-Wurzel, `LoginScreen` (Route) und zustandsloser `LoginContent`, dazu `CredentialsForm`, `GlobalLoginHint` und `CongregationRequiredScreen` in eigenen Dateien unter `auth/presentation`.
4. Previews für alle Zustände (Login, Registrierung, Fehler, Versammlung gewählt).
5. Tests für UseCases und `AuthViewModel`.
6. ADR nur, falls sich der Datenfluss des Logins ändert.

### Phase 3: Logik aus Composables holen

1. Suchfilter der drei Picker in eine gemeinsame, getestete Hilfsfunktion oder ins ViewModel verlagern.
2. Gruppierung und Filterzusammenfassung aus `ExternalDirectoryContent` ins ViewModel verlagern; der State liefert die fertig gruppierte Liste. Sortierung wie im UseCase (ADR-0006).
3. Längenbegrenzungen (`.take(MAX…)`) in die `onChange`-Behandlung der ViewModels verlagern.
4. Abgeleitete Texte und Schalter (Titel in `CongregationEditor`, `enabled`-Bedingungen in `CongregationSelector`) als Felder im State liefern.
5. `ImportContent`: die Begrenzung der angezeigten Probleme in den State verlagern.
6. Tests für jede verlagerte Logik.

### Phase 4: Große Klassen aufteilen

Ein PR pro Fachbereich. Reihenfolge: Wochenplan, Rednerverzeichnis, Versammlungsauswahl, Nutzerverwaltung.

1. **Wochenplan:** kombinierten UseCase für die Stammdaten anlegen (Mitglieder, Vorträge, Wochenereignisse, Verzeichnis); Jahresverwaltung in ein eigenes ViewModel; Editor-Übergänge (`selectTalk`, `selectSpeaker`, `selectChairman`, `rejectSamePerson`) als reine Funktionen.
2. **Rednerverzeichnis:** Filter- und Suchzustand aus dem ViewModel herauslösen; Speichern und Löschen der drei Entitäten vereinheitlichen.
3. **Versammlungsauswahl:** Einladungslink und Neuanlage in eigene ViewModels oder Zustandshalter.
4. **Nutzerverwaltung:** Profil-Editor vom Rollen- und Freigabeteil trennen.
5. **UI-Dateien:** `ExternalDirectoryContent.kt`, `TalkScheduleScreen.kt`, `TalkSlotEditor.kt`, `UserManagementContent.kt` in einen Baustein pro Datei teilen, jeweils mit Preview.
6. Suppressions und Baseline-Einträge für Klassengröße entfernen, sobald die Klasse klein genug ist.
7. Tests für jede herausgelöste Klasse und Funktion.

Die genauen Schnittlinien werden beim Lesen der Klassen festgelegt.

### Phase 5: `uilib` auffüllen

1. `ListDetailPane`, `DetailPlaceholder`, `ListPaneHeader`, `SelectableListCard` nach `uilib` verschieben.
2. `ProfileField` und `PhoneField` als generische Textfelder nach `uilib` verschieben.
3. `MeetingTimeInput`, `TalkDateInput`, `WeekdaySelector`: nur verschieben, wenn sie ohne Domain-Typen auskommen; sonst den generischen Kern herauslösen.
4. Weitere mehrfach verwendete Bausteine in `shared` suchen und verschieben.
5. Previews ziehen mit um.

### Phase 6: UiState-Feinschliff

1. Verbliebene Composables mit direktem ViewModel-Zugriff in Route und zustandslosen Content teilen, zum Beispiel `OwnProfileSection`.
2. States prüfen: Sealed States dort einführen, wo sich Zustände ausschließen.
3. Einheitliche Benennung von State, Aktionen und Ereignissen über alle Screens.

## Regeln für jede Phase

Sie stammen aus `CLAUDE.md` und gelten für jeden PR.

- **Ablauf:** zuerst das Jira-Ticket (Projekt `CP`, ID vergibt Jira), dann Branch `feature/CP-<nr>-kurzbeschreibung`, Commits mit `CP-<nr>:`. Nie auf `master`, Commit und Push nur auf ausdrückliche Anfrage, kein Session-Link in Commits oder PRs.
- **Vor der Freigabe:** auf `origin/master` rebasen, wenige thematische Commits, Push mit `--force-with-lease`. PR nach `.github/pull_request_template.md`.
- **Tests:** im selben PR, in `commonTest` mit `kotlin.test`, Turbine und MockK. Sie müssen in der Pipeline laufen (`:shared:jvmTest`, `:shared:testAndroidHostTest`). Tests werden nie entfernt oder abgeschwächt. Testnamen ohne Kommas.
- **Prüfläufe:** vor „fertig“ der passende `jvmTest` und `./gradlew check`. detekt steht auf `maxIssues: 0`; Verstöße beheben, keine neuen Suppressions ohne Begründung.
- **iOS:** Der iOS-Compile ist unter Windows nicht ausführbar; das im PR ausdrücklich nennen.
- **Previews:** Jedes neue, geänderte oder verschobene Composable bekommt in derselben Datei eine `private fun …Preview()` mit `@ThemePreviews` und `PreviewThemeWrapper` bzw. `NightPreviewWrapper`.
- **KMP:** in `commonMain` kein `java.*` und keine Android-Klassen; neue Libraries nur über `gradle/libs.versions.toml`.
- **Format:** nur neuen und geänderten Code nach `.editorconfig` formatieren, LF-Zeilenenden.
- **Sprache:** Bezeichner, Code-Kommentare und Commit-Texte Englisch; ADR, PR-Beschreibung und Dokumentation Deutsch.
- **ADR:** architekturrelevante Entscheidungen im selben PR, Format nach `docs/adr/README.md`. Nächste freie Nummer am 2026-11-10: 0024.
- **Altdaten:** keine Migration, keine Kompatibilitätslogik; vorhandene Altdaten-Behandlung entfernen, sobald sie auffällt.
- **Firestore-Rules:** bei Änderungen `node firebase/rules-test/run.mjs` ausführen.

## Bewusst nicht geplant

- Generische RBAC-Engine oder `Permission`-Enum: Sechs kleine Policies sind lesbarer und testbarer.
- `BaseViewModel` oder ein generisches `UiState<T>` für alle Screens: Editor-Screens brauchen Daten, Ladezustand und Fehler gleichzeitig.
- Eigene Gradle-Module für Atoms und Molecules: Die Pakete in `uilib` reichen.
- Getrennte ViewModels für Liste und Editor desselben Screens (siehe Zielbild ViewModels).

## Offene Punkte

- `README.md` und `KOVER_SETUP.md` sind nicht gelesen; eine mögliche Coverage-Schwelle vor Phase 1 prüfen.
- Ob `firebase/firestore.rules` heute zu den Policies passt, ist ungeprüft (Phase 1, Schritt 8).
- Warum detekt die großen ViewModels durchlässt, ist ungeprüft (Phase 4, Schritt 6).
- Die Plattform-SourceSets (`androidMain`, `jvmMain`, `iosMain`) sind nur der Paketliste nach bekannt.
- Zielpaket für `MidweekPermissionPolicy` ist offen; ein Fachpaket für die Planung unter der Woche gibt es noch nicht.
