# Stage 1 — Android local visual prototype

## Context to supply

Send this packet with `PRODUCT-BRIEF.md` and `visual-concept-v1.png`. The user approved that reference and wants a small Android-first POC, usable without hosting a server or keeping a PC online. No application stack or repository has been selected. This stage is implementation of the agreed visual concept, not implementation of the full original PDF.

## Outcome

A usable local prototype of World → Character → Play with one sample world, one character, one location, and a small gallery. Validate the composition and actual phone use before adding AI.

## Work

1. Establish the project destination and propose a minimal Android delivery/storage choice. Distinguish developer build tooling from what the user must run day to day. Do not call a PC-served website a standalone mobile solution.
2. Build the three reference screens with shared styling and touch-friendly navigation. Implement the local gallery as a small view, not a fourth large subsystem.
3. Use separate background and transparent character assets for Play. Sample assets may be temporary but must be labeled and their provenance recorded. Do not pretend crops from the mockup are true cutouts. Provide asset replacement without changing application code, through local import/selection if practical.
4. Support local character/location editing, gallery image import, and explicit selection of scene background and character appearance. One active character is sufficient.
5. Provide a clearly labeled scripted/demo exchange to test freeform input, long dialogue, keyboard behavior, and history. Do not imply model intelligence or real image understanding exists.
6. Persist edits, imported assets, selected references, and demo history across restart. Supply backup/export and restore that include images and stable links. Handle corrupt or unsupported imports without destroying current data.

## Acceptance

- On an Android phone, open the app and navigate all three screens in portrait.
- Import/select a background and cutout; the play scene visibly composites them as distinct layers without opaque sprite backgrounds.
- Face/torso remain visible in normal use. Long dialogue and the keyboard do not block input or navigation.
- Edit an entry, close/reopen, and see the retained data.
- Disconnect networking: browsing, editing, imported art, and demo play still work.
- Export, restore into a clean test workspace, and verify images and references survive.
- No API key, account, user-hosted server, or continuously running PC is required for this stage's runtime.

Use available device/emulator testing; identify anything not actually verified on Android. Run checks appropriate to persistence and restore. Include a small-screen screenshot and a keyboard-visible screenshot in the handback where possible.

## Stop boundary

No live AI, automatic image interpretation, per-turn artwork generation, cloud sync, chapters engine, integrations, or publication. Report visual compromises for user review. Update `IMPLEMENTATION-HANDBACK.md` as specified in START-HERE, then stop.
