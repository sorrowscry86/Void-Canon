# Void Canon — personal world library handback

Project: `/workspace/Void-Canon` · Android package `org.voidcat.voidcanon` · version **0.4.0** (code 5), minimum Android 8.0/API 26. The user accepted the prototype direction and explicitly chose **their own worlds and characters first** for the full-app work. This is the first bounded product milestone, not completion of the broader vision.

Earlier verification/history is preserved in [Stage 3 handback](STAGE-3-HANDBACK.md) and [Stage 1 handback](STAGE-1-HANDBACK.md). See [product roadmap](PRODUCT-ROADMAP.md) for the proposed sequence and the original PDF's larger scope.

## Implemented behavior

- World → **Worlds → New world** creates an independent, initially blank setting. Edit its name/description without changing IDs. The existing Glass Harbor sample and all edited records remain available.
- **Add character / Add location** creates author-owned entries with descriptions, public lore, and private author notes. Existing detail editing, image references and approvals remain. The people/place collection searches names/descriptions and uses rows of two cards rather than squeezing an arbitrary number into one row.
- Blank worlds work without scenes or artwork. Gallery handles empty collections; image import, base portraits/cutouts/location backgrounds and manual appearance approval work before scene setup. Location backgrounds also set only their own world's cover. Gallery entry chips scroll horizontally as collections grow.
- **Choose scene / Use in scene** selects a character and location from the same world. A saved pairing resumes its scene; a new pairing gets separate messages, draft, chapter memory, and request diagnostics. Saved pairings appear in World. This is not yet a named campaign/branch system.
- Active world and selected scene per world persist on-device. World/scene switches reset presentation/error state and recover the destination scene's draft. Ordered draft writes complete before a switch; a busy overlay prevents typing into an old scene during that operation. The UI blocks switching during model generation.
- World-specific entries/assets and scene-specific messages/chapters/diagnostics form the selected projection for UI/context. Generation completion commits to the recorded source scene even if a repository caller changes selection. Summary source selection is explicitly scene scoped. Public world description enters bounded RP context; author-only notes remain excluded.
- Own-world scenes use Live RP/setup mode and never receive the harbor's canned responses. The sample retains its explicitly labeled offline demo. OpenRouter configuration, current/free model discovery, encrypted key storage, approval rules, cancellation, diagnostic metadata, and 0.3.1 summary-budget/error improvements remain.

## Storage and portability

Room schema remains **2**: existing tables already have world/scene IDs and this milestone needs no new columns or destructive migration. `MIGRATION_1_2` remains supported. `WorldSnapshot` is a portable collection; the selected projection is defined in `data/WorldLibrary.kt`. Device-local `library-selection` preferences hold the world ID and selected scene ID per world, with checked commits on the IO dispatcher. Missing/stale selection IDs fall back to available records.

Portable ZIP exports are **version 3**, containing the entire local library, including worlds without scenes, image bytes/hashes, transcripts, chapters, private author notes, and diagnostics. Message order is unique within each scene. Version 1 and 2 backups remain readable and normalize to version 3; older APKs cannot import the new version. Credentials/provider settings and UI selection preferences are excluded.

Restore asks to replace **all local worlds**, not just the visible world. It validates versions, IDs, ownership, source references, paths, image integrity and archive limits before transactional replacement; it does not merge libraries. Cross-world scene/cover/generation-asset references and cross-scene chapter/message sources are rejected. Staging/unique private filenames and failure cleanup preserve existing usable data if restore fails.

## Entry points and changed areas

- `data/WorldLibrary.kt`: world/scene projections and library selection view.
- `data/CanonRepository.kt`: creation/edit/selection APIs, scoped mutations, pre-scene assets, full-library export.
- `data/Models.kt`, `data/BackupArchive.kt`: portable format 3 and generalized ownership validation; Room entities unchanged.
- `CanonViewModel.kt`: library state, loading/empty-state distinction, ordered/busy selection actions and draft recovery.
- `ui/LibraryScreens.kt`, `ui/CanonApp.kt`: world library/forms, scene selection, scalable entry cards/chips, blank states and honest Play mode.
- `ai/SceneContext.kt`: selected-scene summary sources and bounded public world context.
- Repository tests and Android walkthrough extended; README/architecture/roadmap updated.

## Verification

- `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `assembleDebugAndroidTest`: passed; **40 JVM tests**, zero failures (30 repository/context/storage + 10 HTTP/budget tests).
- New domain checks cover blank-world persistence and stable rename links, scene/world draft and memory isolation, pairing reuse, persisted selections across database reopening, pending result ownership, pre-scene artwork, multi-world/multi-scene backup with images/messages/approved memory, legacy v2 import, and cross-world/cross-scene restore rejection without replacement. Earlier malformed/archive/image/migration, cancellation and model-price tests still pass.
- Final small presentation changes limit cover name/description previews and explain location cover selection; the final APK/lint build includes those changes.
- In-place emulator upgrade from **0.3.1/code 4 to 0.4.0/code 5** passed. Compared every row before/after: 1 world, 2 entries, 2 assets, 1 scene, 3 messages, 1 approved visual description, 1 chapter, and no generation rows. All values/IDs and Room version 2 were unchanged; the new library UI opened the retained world. The signing certificate matches the earlier APK. The first manual accessibility snapshot preceded the slow software emulator's first draw; a later snapshot succeeded without an app-code change.
- **Android UI walkthrough passed** (`OK (1 test)`, 238.458 seconds) on the API 29 software emulator with airplane mode enabled. It covered the earlier navigation/artwork/keyboard/history/approval/context/model-picker/Keystore checks plus UI world creation, normal blank Gallery/Play states, character/location creation, actual pair selection, saved draft recovery across world switches, source history preservation, public world context, and exclusion of another world's chapter/private notes. Screenshots are in `docs/world-library-evidence/screenshots/`; model-picker fixtures are conspicuously labeled and not a production catalog.
- This environment did not make live OpenRouter requests and has no real user key. API/device-provider behavior and physical-phone performance remain separate from build/JVM/emulator evidence.

Use a full JDK 17 or 21, SDK 35/Build Tools 35.0.0, and the pinned Gradle wrapper (8.11.1). Build/install commands and private-key setup are in README. Build output: `app/build/outputs/apk/debug/app-debug.apk`. Use a disposable installation for the instrumentation test, which changes sample lore, creates a test world, and exercises encrypted credential storage with a non-secret fixture.

## Limits and next increment

This milestone is deliberately limited to personal worlds, characters/locations, their visual references, and resumable pairings. No deletion/archiving, generic lore types, tags/relationships/revisions, fragment inbox, campaign naming, branching, ensemble RP, manuscript pipeline, sync, accounts, integrations, hosting, or publication was added. An image AI proposal still needs a configured scene for its generation audit; manual visual descriptions work before scene setup. Model context is approximate and bounded, not a tokenizer/retrieval engine. Image/document-provider round trips and physical Android versions require separate device verification; provider quotas and real model quality are account dependent.

Stop after this milestone for user review. The next recommended increment is World Workshop (fragments and richer lore organization), scoped with the user. The older optional Stage 4 tool integration is not a prerequisite; no external operational tool has been selected or authorized. Do not treat the full PDF or roadmap as an instruction to implement all remaining capabilities.
