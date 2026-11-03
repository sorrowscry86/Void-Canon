# Void Canon — Stage 1 implementation handback

Project: `/workspace/Void-Canon` · package: `org.voidcat.voidcanon` · version: `0.1.0`.

## Implemented behavior

- Native Compose app with World, Gallery, Play, and a clearly planned Chapters destination.
- One seeded world (The Glass Harbor), character (Mira), and location (The Docks). The sample is created once; reopening or restoring does not overwrite edited names or content.
- Character/location detail views with editable name, description, and lore, plus scene/gallery navigation.
- Android document-picker image import, local PNG normalization/orientation, entity associations, explicit portrait/background/cutout selection, and cutout transparency checks.
- Separately generated harbor and transparent Mira assets. Play composites the character over the setting and makes space for the keyboard and scrollable dialogue.
- Clearly labeled deterministic demo replies, freeform author input, preserved history, saved draft text, automatic local persistence, and explicit Save confirmation.
- Versioned ZIP export and confirmed replacement restore with graph/image/path validation and staged files before a transactional database replacement.
- No Internet permission, accounts, API keys, AI generation, image interpretation, chapter engine, or external integrations. Android backup/transfer exclusions keep the portable export workflow explicit.

## Entry points and storage

`MainActivity` installs the dark theme and system bars; `ui/CanonApp.kt` contains the screens. `CanonViewModel` publishes state and serializes UI commands. `data/CanonRepository.kt` owns local operations and the scripted demo. `CanonDatabase` is Room schema version 1; its schema is checked into `app/schemas/`.

Stable IDs link worlds, entries, assets, scenes, and messages. Images are private files, not document-provider URIs. Restore assigns new private filenames while preserving public record IDs and references. Source messages are stored separately from editable lore. There is no chapter-summary table or model API yet. See `docs/ARCHITECTURE.md` for the portable archive contract and failure behavior.

## Build and installation

Follow `README.md`. A full JDK 17 or 21, SDK Platform 35, and Build Tools 35.0.0 are required. Android 8.0/API 26 is the minimum runtime. The wrapper pins Gradle 8.11.1 with a distribution checksum; Android Gradle Plugin is 8.9.2 and Kotlin is 2.1.20.

Build output: `app/build/outputs/apk/debug/app-debug.apk`. This is a debug-signed prototype. Export before uninstalling or clearing app data.

## Verification

- Debug APK assembly succeeded.
- **14/14 repository tests passed** under Robolectric API 28 with native graphics: database reopen, idempotent seeding, rename stability, selections/drafts/messages, backup round trip, clean-install restore, screen closure during restore, and rejection of corrupt/version-mismatched/unsafe/missing-image/broken-reference inputs.
- Legacy Robolectric graphics initially failed two alpha-channel checks. Native graphics decoded the real PNG pixels correctly; production transparency validation was retained.
- **Android lint passed with no issues.** `assembleDebug`, `testDebugUnitTest`, `lintDebug`, and `assembleDebugAndroidTest` completed successfully.
- **Android UI smoke test passed** (`OK (1 test)`), run directly with AndroidJUnitRunner on an Android 10/API 29 AOSP emulator at 720 × 1280 pixels / 320 dpi (360 × 640 dp). It navigated World → Character → Gallery → Play, renamed the character, typed with the software keyboard, sent a scripted turn, and verified source input in history. It also checked that the installed package has no Internet permission. Airplane mode was enabled.
- Earlier smoke-test attempts hit transient feedback covering a scrolled action. The final harness explicitly dismisses the save snackbar through its accessibility action before continuing. Direct instrumentation retains private screenshots; Gradle's runner uninstalled its test installation after the earlier run.
- Reviewed PNG evidence is in `docs/screenshots/`, including normal Play and Play with the software keyboard visible. The final screenshot harness decodes the real bundled assets through Coil on Android before capture; Compose idleness alone does not wait for asynchronous artwork loading.
- After force-stopping the Android process, inspection of its on-device Room database confirmed the renamed character, all three transcript messages, and the selected character/location/background/cutout references were retained. The app reopened with its illustrated world intact.

The managed environment initially contained only a Java runtime, so a full JDK and Android SDK were installed for verification. Builds use one worker and in-process Kotlin compilation to fit the environment's memory budget. An Android 10/API 29 AOSP emulator is available without hardware acceleration; first boot and screen capture are slow. No physical Android phone has been tested.

## Files and handoff

Changes add the Android module, pinned wrapper/build scripts, Room schema, bundled demo images, repository tests, Android UI smoke test, development instructions, architecture and asset records, and preserved supplied specifications. Six reviewed screenshots are in `docs/screenshots/`.

The original planning package remains in `docs/specifications/`. The current product name and stack supersede its historical undecided-name/stack statements. The original flattened visual reference was supplied in chat; the separately generated app assets are stored in the repository. Additional Kuro mascot artwork requested in chat is delivered separately under `/workspace/outputs/` and does not add a second story character.

## Limitations and next stage

- Stage 1 supports one world and one active scene. It provides editing and image replacement, not a full world/entity creation or deletion workflow.
- Demo responses follow a three-step script and do not understand the user's text, images, or edited lore.
- Imports are bounded to 16 MiB/24 megapixels and normalized to PNG; backup size and format limits are documented in the architecture note. Cross-device transfer is manual backup/restore.
- No automatic background removal, cloud sync, publication, commercial distribution, or phone installation has been performed.
- Physical-device review, Android 8.0 runtime behavior, and newer Android versions still require device verification beyond the API 29 emulator. Android's external document-provider picker and backup-provider round trip were not automated; repository import/restore behavior was verified by the native-graphics data tests.

Stop after Stage 1 review. Stage 2 should read the actual implementation and `02-visual-grounding.md`, then add source-linked editable visual descriptions and approval state using the existing stable asset IDs. Provider choice and credentials remain unresolved until that stage; do not introduce them into the offline prototype.
