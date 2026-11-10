# Void Canon — product roadmap

The user accepts the prototype direction and chose **their own worlds and characters first** for the first full-app milestone. This document maps the original vision to incremental implementation; it does not label the entire product complete or authorize a commercial launch.

| Milestone | User outcome | Status |
| --- | --- | --- |
| Prototype, Stages 1–3 | Illustrated offline app, meaningful visual references, live OpenRouter RP, approved chapter memory, portable backups | Delivered as 0.3.1; user reports the core works. A reported summary-limit issue received budget/error improvements; exact original provider error remains unknown. |
| 1. Personal world library | Create independent worlds, characters, locations, and their references; choose who and where to play without overwriting another scene | Delivered as 0.4.0; 40 JVM tests, Android UI walkthrough, and in-place upgrade checks passed |
| Scene continuity foundation | Persistent participant/object state, compact context, validated turn updates, and author corrections | Implemented in 0.4.1 source; see current handback for verification and signing limits |
| Automatic scene artwork | Opt-in illustrations after visible changes, grounded prompts, independent image queue and explicit Gallery retention | Implemented in 0.4.4 source; regular APK packaging and live quality assessment remain separate |
| 2. World Workshop | Capture fragments, search and connect broader lore types, organize tags/collections, review revisions and AI proposals | Planned; scope after Milestone 1 review |
| 3. Portrayal review | Annotate mismatches, inspect relevant context, compare proposed lore changes, replay deliberately | Planned |
| 4. Campaign depth | Named campaigns/sessions, explicit chapter boundaries, lore baselines, branches and character-specific knowledge; ensemble RP in a separate increment | Planned |
| 5. Manuscript Studio | Select approved campaign material, outline, adapt scenes, revise prose, export a traceable manuscript | Planned |
| 6. Broader release | Reliable distribution/update path, onboarding, accessibility, larger-device coverage and performance; licensing/commercial decisions if requested | Planned; private Android builds continue throughout |

Optional tool integrations can fit after an explicit tool/task is selected. They are not a prerequisite for the personal world library, nor a requirement to replace the standalone app with hosted infrastructure. The earlier optional Stage 4 brief is an integration proposal, distinct from the product milestones above.

## Milestone 1 boundary

- Keep Kotlin/Compose/Room and the existing illustration-led Android design. Build on tested behavior and preserve existing IDs/data rather than restarting the app.
- Provide a world library, blank-world creation and editing, character/location creation and editing, basic collection search, and pre-scene image import/manual visual-description approval.
- Selecting a character and location from the same world resumes their saved scene or starts a new pairing. Pairings retain separate drafts, messages, chapters, and diagnostics. This is a basic scene library, not a complete campaign/branch system.
- Keep Play available through OpenRouter with current free-model discovery. Own-world scenes never receive canned harbor replies. All worldbuilding/manual editing remains offline.
- Export the complete local library with images/references. Validate versions and ownership of references before replacing any current records. Continue accepting Stage 1/3 backups. Explain that restore replaces the entire library.
- Preserve private author notes, explicit approval, source transcripts, and inspectable context. No silent canon edits.

The milestone does not add arbitrary entry types, relationships, revisions, fragment inbox, deletion/archiving, ensemble RP, branching, manuscripts, integrations, synchronization, accounts, or publishing. Those are follow-up decisions, not hidden scope in this build.

## Technical changes to verify

The prototype's record tables already contain world/scene IDs, so Milestone 1 can reuse Room schema 2. Remove single-world/single-scene assumptions from repository mutations and archive validation. Portable archive version 3 permits multiple worlds, worlds without scenes, and per-scene message ordering; versions 1 and 2 remain readable. The active world and per-world selected scene are checked device-local preferences, separate from portable domain records.

A selected projection supplies only that world's entries/assets and that scene's transcript/chapters/diagnostics to the UI and context builder. Completion commits target the recorded request's original scene. Switching worlds while a model request is active is blocked in the UI; repository checks still protect in-flight results. Scene selection and queued draft writes must not attach an old draft to another scene.

Particular regression risks found in the prototype:

- `WorldSnapshot.world` and `.scene` resolve first records; callers must receive an explicitly selected projection.
- `updateScene`, Save, background selection, and image imports previously assumed a single scene/world. Mutations must target the intended records.
- Summary source-message selection previously considered all messages/approved chapters; it must stay scene scoped.
- Backup validation previously allowed exactly one world and scene and globally unique message sequence values. New validation must preserve world ownership and per-scene source references.
- New worlds may have no images, entries, or scene. These are normal empty states, not an endless loading indicator or a reason to seed demo content again.

## Exit checks

Create a blank world without a model key, add and rename people/places, attach images before scene setup, and restart with selections intact. Switch between independent worlds and scene pairings without moving drafts, memories, or artwork. Confirm the selected pair reaches Play and its context excludes other worlds/private notes. Restore a library containing multiple worlds and an unwritten world, with image hashes and links intact. Reject cross-world scene/cover references and cross-scene chapter sources without changing the existing library. Run the earlier prototype regression suite plus new domain and Android UI checks, and report live-provider/device evidence separately.

## Sources

- [Original vision](specifications/vision-spec-v0.1.pdf): proposed long-term capabilities and campaign-to-manuscript destination.
- [Product brief](specifications/PRODUCT-BRIEF.md): Android-first, independent offline usefulness, author control.
- [Implementation handback](../IMPLEMENTATION-HANDBACK.md): actual delivered behavior and verification.

The current user choices settle the name (Void Canon), native Android stack, OpenRouter provider, and immediate worldbuilding priority. Older documents that list those decisions as open are historical context.
