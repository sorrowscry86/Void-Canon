# Changelog

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
