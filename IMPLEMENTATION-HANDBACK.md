# Void Canon — scene continuity handback

Project: `/workspace/Void-Canon` · Android package `org.voidcat.voidcanon` · source version **0.4.1** (code 6), minimum Android 8.0/API 26.

The user authorized character/scene state tracking as the foundation for future Freeform Story and AURPG play modes. Scope is this continuity increment only. The prior implementation/evidence is preserved in [0.4.0 handback](docs/WORLD-LIBRARY-HANDBACK.md), [Stage 3](docs/STAGE-3-HANDBACK.md), and [Stage 1](docs/STAGE-1-HANDBACK.md).

## Delivered behavior

- Structured current state for the player and selected character, optional existing-world participants (up to 8 total), and up to 16 scene objects. Track locations, presence, positions, activities, relevant conditions/appearance changes, explicitly learned facts, holders/hands, an optional time anchor, and explicit elapsed seconds.
- State is scene-local and provisional. No retroactive model extraction from old transcripts, inferred motives, autonomous offscreen simulation, world-canon writes, or chapter-driven resource/state changes.
- Compact state is reserved ahead of history/lore in RP context, capped at 4,000 characters within the existing 24,000-character total ceiling. Whole-record omissions are reported. Player/focus character remain; core facts are never silently clipped. Older state events stay local.
- Live RP uses a single model response containing narrative and source-quoted changes. The app checks structure, exact source passages, base revision, field completeness, references/ownership, holders/location, duplicate updates, hand occupancy, bounds, and the next context projection. Invalid/stale results retain raw output, model and usage diagnostics, but publish neither story nor state.
- Successful story, current snapshot, before/after event and request completion commit in one Room transaction. Repeated completions are ignored; failure/cancellation preserves the draft/history/state; completions target the recorded source scene even after repository selection changes.
- Optional correction UI: **Play → Scene → Review scene continuity**. Add participants/objects, correct current facts, or use the state before the most recent update. Corrections append revisions and invalidate pending fingerprints without rewriting transcripts or lore. State fields are model-visible; keep secrets in author-only notes.
- RP output allows up to 4,096 tokens (including reasoning), constrained by catalog/context limits, with at least 1,024 available for a tracked RP response. Image allowance stays 1,000; summary behavior stays bounded. No automatic retry or paid fallback.

## Storage and entry points

Room schema **3** adds `scenes.continuityJson` with a blank default and `continuity_events`. Explicit `MIGRATION_2_3` follows the existing `MIGRATION_1_2`. Stable domain IDs and old rows are retained. Legacy scenes initialize unknown details from their selected player/character/location, without rewriting their stored transcript.

Portable backups are **format 4** and contain the entire library, state and source-linked events. Formats 1–3 remain readable. Restore validates revision chains, source/scene/world references and agreement between current state and latest event before replacing local data. Keys/settings remain excluded.

- `data/Continuity.kt`: typed observations, bounded projection, response protocol and validation.
- `data/Models.kt`, `CanonDatabase.kt`, `BackupArchive.kt`, `WorldLibrary.kt`: schema/migration, event persistence, scoped views, archive validation.
- `data/CanonRepository.kt`: atomic completion and revision-checked author corrections.
- `ai/SceneContext.kt`, `RequestBudget.kt`, `CanonViewModel.kt`: reserved state context, protocol audit marker and model-aware RP allowance.
- `ui/ContinuitySheet.kt`, `NarrativeScreens.kt`: optional state review/correction.
- `ContinuityTest`, extended `RepositoryTest`, and `ContinuityUiTest`: protocol/domain, migration/persistence and Android editor coverage.

The full contract and explicit limitations are in [Scene continuity](docs/SCENE-CONTINUITY.md).

## Verification

- Full JDK 21.0.9, Android SDK 35 / Build Tools 35.0.0, pinned Gradle 8.11.1.
- `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest`: passed. **52 JVM tests, zero failures; Android lint reports no issues.** The final run includes the missing-field regression assertion.
- Domain/repository coverage includes exact source quotes, required update fields, elapsed time, old/stale/malformed payloads, omitted participant exclusion, hand conflicts, holder co-location, unknown references, bounded prompts, private-note/history exclusion, state revision preservation with empty changes, atomic/idempotent completions, rejected-result diagnostics, source-scene ownership, manual correction concurrency, rename stability, DB reopen, chapter separation, backup round trip, invalid audit rejection, and actual schema 1→3 and 2→3 migrations.
- New **Android continuity-editor test passed** (`OK (1 test)`, 109.359 seconds standalone; repeated successfully with a clean render). It writes and reopens a correction, checks that corrected facts enter context, and verifies the transcript is unchanged.
- The **full Android regression walkthrough passed** (`OK (1 test)`, 223.493 seconds) on the API 29 AOSP software emulator, 720 × 1280 / 320 dpi, with Wi-Fi/mobile data disabled. It exercises navigation, artwork/keyboard/history, descriptions, chapter approval, context inspection, model-picker fixtures/Keystore, blank-world creation and world/scene draft recovery.
- An initial capture exposed an emulator System UI ANR. Dismissing its dialog and disabling animation produced a clean editor capture. The chapter approval assertion now waits for the observed unmerged UI label, rather than racing asynchronous publication. Evidence/logs are in `docs/continuity-evidence/`; this does not establish physical-phone performance.
- No live OpenRouter requests or user credentials were used. Model adherence, semantic extraction quality, live quotas, and physical-phone behavior remain unverified.

## Main-branch integration

[PR #2](https://github.com/sorrowscry86/Void-Canon/pull/2) was merged into `main` on November 3, 2026. Integration checks reran `testDebugUnitTest lintDebug assemblePreview` with JDK 17: **52 tests passed, lint found no issues, and the preview build succeeded**. The committed downloadable preview's SHA-256 was verified and its bytes are unchanged. Download links now use `main`. Android/device and live-provider checks were not repeated during this integration; their evidence and limitations above remain distinct.

## Regular 0.4.1 distribution and signing

The primary download is **[Void Canon 0.4.1](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/main/downloads/void-canon-0.4.1.apk)**. It contains the continuity implementation in the regular app, version **0.4.1/code 6**, Android package **`org.voidcat.voidcanon`**, launcher name **Void Canon**. Build it with `assembleDebug`; output is `app/build/outputs/apk/debug/app-debug.apk`.

The original signing identity is available in the distribution workspace. Verification against the published 0.4.0 APK confirmed the same certificate SHA-256: `d2c8a958809475255d9fa24ead371f3225f19e3a5189db3b416217817de5f56a`. This regular APK can update that package without uninstalling. Signing keys stay outside Git and must be retained privately for future updates; matching package names alone does not establish signing compatibility.

`assembleDebug assembleDebugAndroidTest` succeeded for the regular build. APK metadata, signature continuity, and packaged continuity code were checked. Packaging did not change application behavior from the already verified source. The earlier 52 JVM/lint checks remain applicable.

Regular 0.4.0 → 0.4.1 upgrade verification **passed** on the disposable API 29 emulator. Installing with `adb install -r` retained every original record/ID/field in a fixture containing a renamed character, three messages, approved chapter, draft, private notes, image references and model settings. Room migrated 2→3, image hashes and preference bytes matched, and the regular-app continuity editor saved a new state revision without altering old transcripts (`OK (1 test)`, 78.045 seconds). See [regular release evidence](docs/release-041-evidence/README.md). This is emulator evidence, not a physical-phone or live-provider claim.

The earlier downloadable **[Void Canon Test 0.4.1-preview](https://github.com/sorrowscry86/Void-Canon/raw/refs/heads/main/downloads/void-canon-0.4.1-preview.apk)** uses package **`org.voidcat.voidcanon.preview`**, with separate local data and a different signing identity from its original build environment. Its `assemblePreview lintPreview` and API 29 installation/editor checks remain recorded in `docs/continuity-evidence/`. It remains optional; it is no longer the primary download for normal upgrades.

The preview does not automatically transfer its data to the regular app. Both 0.4.1 packages accept format-4 portable backups, and restore replaces the receiving library. Provider keys/settings are excluded from backups; keep original exports because 0.4.0 cannot import format 4. Earlier claims about the signing identity being unavailable described the preview's build environment and do not apply to the regular APK now verified here.

## Limits and next decisions

Quoted evidence is traceability, not proof of correct interpretation. A model can still omit a change, misread a failed attempt, misinterpret duration, or introduce an unsupported fictional detail. Validation cannot guarantee physical plausibility or knowledge isolation within one shared model context. The correction editor is the recovery path; complete sources and before/after records are retained.

Current character/location pairings still define scenes. This increment does not move artwork/backdrops automatically or carry state between scenes. Additional participants receive scoped state and names, not independent model orchestration. New actors and returning omitted actors can be introduced through the correction editor. Scripted Demo remains unchanged and does not extract state.

Campaign identity, cross-scene continuity, MLRPE-inspired narrative presets, AURPG dice/momentum/clocks, scheduling, relationships, branching, ensemble autonomy and integrations are deferred. Stop here for review of the state foundation.
