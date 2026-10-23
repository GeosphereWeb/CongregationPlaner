# ADR-0001: Einladungslinks mit AES-GCM verschlüsseln

## Status
Accepted

## Context
Einladungslinks enthielten den Versammlungspfad im Klartext (`region/…/congregation/{id}`) und verrieten damit die Organisationsstruktur. Die Links werden ohne eigenen Server geöffnet (Android-Intent, Desktop-Argument, Einfügen im Login), daher gibt es keinen Ort, um ein Token serverseitig aufzulösen.

## Decision
`InviteLinkCodec` verschlüsselt den Pfad mit AES-GCM (cryptography-kotlin, `cryptography-provider-optimal`) und kodiert ihn URL-sicher als Base64. Der Schlüssel liegt als Konstante in `shared`; das Präfix `congregationplaner://join/` bleibt. Alte Klartext-Links werden nicht mehr akzeptiert.

## Consequences
- Der Link ist für Mitlesende unleserlich und manipulationssicher, aber kein Zugriffsschutz: Wer die App zerlegt, kann Links entschlüsseln. Echten Schutz oder Widerruf bräuchte ein serverseitig aufgelöstes Token.
- Der Nonce ist zufällig, jeder Aufruf von `CongregationRef.toInviteLink()` liefert einen anderen gültigen Link; die Userverwaltung merkt sich den angezeigten Link.
- Neue Abhängigkeit `dev.whyoleg.cryptography`; bereits verteilte Klartext-Links müssen neu erzeugt werden.
