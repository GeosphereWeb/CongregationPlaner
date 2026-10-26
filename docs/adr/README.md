# Architecture Decision Records (ADRs)

This directory records architecturally significant decisions for CongregationPlaner, using a lightweight MADR-style format.

## When to write an ADR

Write an ADR whenever you make a decision that:

- Introduces a new architectural pattern or convention
- Chooses a non-obvious threshold, constant, or trade-off with lasting consequences
- Changes a module boundary or a core data flow
- Would be genuinely useful for someone joining the project later to understand *why*, not just *what*

Write the ADR in the same PR that introduces the decision.

## Format

Create a new file `docs/adr/000X-title-in-kebab-case.md` (sequential numbering, zero-padded to 4 digits, never reused or renumbered) using this template:

```markdown
# ADR-000X: <Title>

## Status
Accepted

## Context
<What forces or problem led to this decision>

## Decision
<What was decided>

## Consequences
<What becomes easier or harder as a result; trade-offs accepted>
```

## Keep it concise

An ADR is a short record, not a design doc:

- Keep each section (Context, Decision, Consequences) to 2-4 sentences or bullets.
- Do not paste code snippets or diffs. The code already lives in the PR and the source file; a copy here just goes stale. Name the file or function instead.
- Skip implementation detail such as lint/SonarQube rule IDs or step-by-step walkthroughs — that belongs in code comments or the PR description.
- List only real trade-offs in Consequences. Do not restate the Decision or introduce alternatives that Context never mentioned.

## Status values

- **Proposed** — under discussion, not yet final
- **Accepted** — the current, active decision
- **Superseded by ADR-000Y** — replaced by a newer decision; keep the original file, just update its status and link to the superseding ADR
- **Deprecated** — no longer relevant, but kept for historical context

## Numbering

Numbers are sequential and permanent. Never reuse or renumber an existing ADR, even after it is superseded or deprecated.

## Index

<!-- Add one line per ADR, newest last: - [ADR-0001: Title](0001-title.md) — Status -->

- [ADR-0001: Einladungslinks mit AES-GCM verschlüsseln](0001-encrypt-invite-links-with-aes-gcm.md) — Accepted
- [ADR-0002: Vortragsplanung – Datenmodell und Berechtigung](0002-talk-schedule-data-model-and-permission.md) — Accepted
- [ADR-0003: Globale Vortragsliste mit Titel-Momentaufnahme im Termin](0003-global-talk-catalog.md) — Accepted
- [ADR-0004: Lese-Berechtigung VIEW_MEMBERS für Mitgliederdaten](0004-view-members-permission.md) — Accepted
- [ADR-0005: Externe Redner je Versammlung ohne Verweis auf fremde Mitglieder](0005-external-speakers.md) — Superseded by ADR-0008
- [ADR-0008: Verzeichnis externer Redner mit eigener Hierarchie Kreis → Versammlung → Redner](0008-external-speaker-directory.md) — Accepted
