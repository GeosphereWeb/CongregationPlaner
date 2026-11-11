# ADR-0025: Zustandshalter je Teilaufgabe statt großer Bildschirm-ViewModels

## Status
Accepted

## Context
Mehrere ViewModels (Wochenplan mit 34 Funktionen, Rednerverzeichnis, Nutzerverwaltung, Versammlungsauswahl) hatten mehr als zehn öffentliche Aktionen. ADR-0006 hält Liste und Editor eines Bildschirms in einem Zustand; zwei ViewModels müssten sich bei Speichern und Löschen abstimmen.

## Decision
- Ein Bildschirm hat weiter ein ViewModel und einen `UiState`. Der Zustand liegt in einem `UiStateStore` (`presentation/state`), den ViewModel und Zustandshalter gemeinsam ändern.
- Teilaufgaben (Editor, Auswahl, Speichern, Jahresverwaltung, Filter, Einladungslink) sind einfache Klassen ohne `ViewModel`-Basis. Das ViewModel erzeugt sie mit `viewModelScope` und gibt sie als Eigenschaften heraus (z. B. `viewModel.editor.save()`).
- UseCases einer Teilaufgabe werden als `@Inject`-Bündel übergeben, damit Konstruktoren kurz bleiben. Rechte und Zustandsübergänge sind reine Funktionen in eigenen Dateien.

## Consequences
- Jede Klasse bleibt unter den detekt-Grenzen und ist einzeln lesbar; die Route ruft Aktionen über den Halter auf.
- Halter teilen sich den Zustand über den Store; eine falsche Änderung an fremden Feldern fällt erst in den Tests auf.
