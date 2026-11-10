# Changelog

## 0.4.5 — Persist artwork settings and populate captioned Gallery

- Save scene permission/brief and world style independently of connection/reference readiness; show save errors inside the sheet.
- Automatically retain completed scene illustrations in Gallery and show visible-context captions in Gallery and Play.
- Add separate opt-in OpenRouter image understanding: populate Gallery descriptions and use matching image observations in the next RP context, with source hashes, Free only, durable request tracking and no automatic retries.
- Recover interrupted local Gallery publication idempotently; retain original owners, protect restored libraries and preserve images/captions across cache eviction and backup.
- Retain Room 4, backup format 4, the original package/signature, and existing private connections.

See [release notes](downloads/RELEASE-0.4.5.md) and [verification](docs/release-045-evidence/README.md).

## 0.4.4 — Automatic scene artwork

- Optional meaningful-change illustrations in Play, with independent RP/image work,
  selected-reference prompt building, quoted visual cues and prompt inspection.
- Durable/coalescing jobs, foreground dispatch, daily allowance, no automatic replay,
  a bounded cache, layered-view toggle and explicit exploratory Gallery retention.
- Room 3→4 migration preserves story records. Backup format remains 4; operational
  image state and upload permissions stay device-local.
- Align image HTTP read/overall timeouts and keep the artwork dialog open while editing with the keyboard.
- Publish a regular update signed with the original identity; see [release notes](downloads/RELEASE-0.4.4.md) and [distribution checks](docs/release-044-evidence/README.md).

## 0.4.3 — Artwork and regular-app navigation

- Bring the reviewed Cloudflare artwork workflow from the 0.4.2 preview into the regular app: optional selected reference, explicit consent, recoverable preview, and author-controlled save/discard. Saved art starts as exploratory, with provenance.
- Add central Settings for AI/models, artwork connection, and backups, plus visible installed version/build.
- Keep Back available with the keyboard open and Close outside modal scrollers; child settings return to Settings. Preserve the unsent draft when leaving Play.
- Publish the original package signed with the regular 0.4.0/0.4.1 certificate; retain Room schema 3 and backup format 4.

See [0.4.3 release notes](downloads/RELEASE-0.4.3.md) and [distribution verification](docs/release-043-evidence/README.md). The earlier 0.4.2 artwork preview is retained as historical evidence.

## 0.4.1 — Scene continuity

- Persist bounded participant/object state, explicit elapsed time, and source-linked before/after revisions separately from transcripts and chapter memory.
- Reserve a compact current-state snapshot in RP context, with whole-record omissions and no revision history.
- Validate structured story/state replies and publish both atomically; retain malformed/stale response diagnostics without changing history or state.
- Add optional author corrections through Play → Scene, plus Room schema 3 and backup format 4 with legacy imports.
- Increase the model-limited RP output allowance to accommodate story and state in one request.

The regular **Void Canon 0.4.1** APK includes these features and uses the original package/signing identity to update 0.4.0 in place. The earlier **Void Canon Test** preview remains a separate optional package. See the current handback for verification and limits.

## 0.4.0 — Personal world library

- Create and edit independent worlds, characters, and locations; search people and places.
- Import and approve visual references before scene setup. Location images set their own world's cover.
- Select a character/location pairing to resume its saved scene or start a new one. Keep drafts, transcripts, chapter memory, and diagnostics scoped to that scene.
- Handle blank worlds without requiring images, an AI key, or a scene. Keep the scripted harbor demo confined to the sample scene.
- Export the entire library using backup format 3, with support for earlier formats 1 and 2. Reject references that cross world or scene ownership boundaries.
- Preserve existing data and signing identity on upgrade; retain Room schema 2.

Validation: 40 JVM tests, Android lint, an API 29 emulator walkthrough, and an in-place 0.3.1 upgrade passed. See [the handback](IMPLEMENTATION-HANDBACK.md) for exact evidence, limits, and device/provider testing distinctions.

## 0.3.1 — Summary limit handling

- Increase chapter-summary output budgets up to 4,096 tokens, respecting advertised model limits and estimated remaining context.
- Distinguish output exhaustion from provider rate/quota errors, including reasoning-only exhaustion.
- Keep summary failures visible in Chapters with explicit retry guidance; preserve transcripts on failure.

## 0.3.0 — Visual grounding and live roleplay

- Add source-linked visual-description drafts and author approval.
- Connect OpenRouter for live roleplay, image descriptions, and chapter-summary proposals, with current free-model discovery and explicit pricing controls.
- Add inspectable bounded context, private-note exclusion, encrypted device-local credentials, persistent request diagnostics, and approved chapter memory.
- Introduce Room schema 2 and backup format 2 with explicit upgrade support.

The earlier offline visual prototype and its checks are preserved in [the Stage 1 handback](docs/STAGE-1-HANDBACK.md). These are early Android debug builds; the broader product roadmap remains incremental.
