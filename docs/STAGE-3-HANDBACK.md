# Void Canon — Stage 3 implementation handback

Project: `/workspace/Void-Canon` · application: `org.voidcat.voidcanon` · version: `0.3.1` (code 4). The user's goal covers Stage 2 prerequisites through Stage 3, followed by user verification. Stage 1 was accepted by the user; its original handback is preserved in `docs/STAGE-1-HANDBACK.md`.

## Summary-limit maintenance update (0.3.1)

The user reports Stage 3 works well on their device except a summary attempt that mentioned a limit. They do not remember the model or exact error, so the original failure cannot be classified conclusively. The prior 1,000-token cap was a plausible cause, particularly for reasoning models; provider quotas remain another possibility.

- Summaries now request at most 500 words and allow up to 4,096 output tokens (including reasoning). The public catalog's provider completion maximum and estimated remaining context reduce this budget when necessary. Very small context fails locally with guidance; sources are never silently truncated. RP/image budgets remain 1,000.
- Output exhaustion is recognized even when reasoning consumed the budget without producing text. HTTP and in-body provider rate/quota errors have separate recovery guidance. No incomplete summary is published, no automatic retries or paid fallbacks occur, and transcripts remain intact.
- Chapters retains the latest failed/cancelled summary diagnostic, including after reopening from stored request records, and offers explicit retry. Setup failures are readable beyond the transient snackbar.
- Room/backup formats and signing identity are unchanged. Install 0.3.1 over the existing app to retain data and device-local provider settings.

## Delivered scope

- The original offline World, Character/Location, Gallery, layered Play, editing, image import, scripted Demo, persistence, and portable backups remain.
- Gallery visual descriptions are source/hash linked, draft or approved, manually editable offline, and optionally proposed from one selected image through OpenRouter. Exploratory references are excluded, alternate appearances use the selected scene asset, and new proposals do not silently replace approved appearance text.
- Live RP and chapter-summary proposals use one configurable OpenRouter adapter. Roleplay, image-understanding models, and Free only are configured on-device. The public model picker refreshes current prices/capabilities, searches model IDs/names, filters image-capable models, and defaults to free/no-credit-purchase variants.
- Free requests set all supported pricing caps to zero, disable provider fallbacks, and never supply alternate models or auto-retry. Paid choices require disabling Free only. Free availability/quotas remain provider controlled.
- Context combines recent dialogue, known facts, selected approved visual descriptions, bounded lore, provisional state, and approved chapter memory. Private author notes on entries/scenes are excluded. Context previews expose missing/stale sources and omissions; persisted generation records expose exact text context, source identity/hash, requested/actual model, settings, status, output, and reported usage.
- Live responses atomically append one user/reply pair. Failure/cancellation preserves history and draft; duplicate completions are ignored. Stale responses after scene edits are retained as diagnostics without applying their text. Interrupted requests are not auto-resumed.
- Chapters preserve their source-message IDs separately from editable summaries. Manual summaries work offline; AI summaries remain drafts. Approval adds memory to subsequent context without rewriting transcripts or world lore.
- OpenRouter keys are encrypted with Android Keystore in private preferences, never in source/APK constants, Room, logs, model context, or portable backups. Browsing models requires no key; sending story material requires consent and explicit generation actions.

## Architecture and compatibility

One Android module with Compose/Material 3, Android ViewModel, Room, private image files, and OkHttp. `ai/OpenRouterClient.kt` implements the fixed HTTPS provider endpoint and catalog parsing; `ai/SceneContext.kt` assembles reference-bounded prompts; `ai/OpenRouterSettings.kt` handles encrypted device-local configuration. `ui/NarrativeScreens.kt` contains model/settings, description review, context, scene-knowledge, and chapter views.

Room schema **2** has an explicit `MIGRATION_1_2`. Existing IDs, user edits, scene selections, assets, and transcripts remain intact. Both schemas are checked into `app/schemas/`. Portable exports are **version 2**, including descriptions, chapters, request diagnostics, and source references. Version 1 archives are normalized and their image hashes calculated without changing IDs. API settings/keys are deliberately device-local and require setup on another phone.

The Stage 3 APK has the **same signing certificate as Stage 1**, verified with apksigner. An in-place emulator upgrade retained the edited character, all three prior transcript messages, and the original scene references; the on-device Room version became 2 and new tables were created.

See `README.md` for installation/setup and `docs/ARCHITECTURE.md` for storage, context, network, cancellation, and archive details. Original specifications remain in `docs/specifications/`; their older unselected-name/stack statements are historical.

## Verification

- `assembleDebug`, `testDebugUnitTest`, `lintDebug`, and `assembleDebugAndroidTest` completed successfully.
- **33 JVM tests passed**: 23 repository/context/migration tests, 7 HTTP/catalog contract tests, and 3 summary-budget tests. Coverage includes native PNG handling, private-note exclusion, approval/correction/staleness, selected alternate appearance, exploratory exclusion, bounded context/omissions, chapter memory/source preservation, portable v1 upgrade, actual v1 database migration, failed/cancelled/stale/idempotent requests, numeric free pricing, zero-price request caps, image payload shape, response metadata, rate-limit errors, network cancellation without retry, expanded summary requests, empty/partial output exhaustion, in-body quota errors, catalog output limits, and context/output budgeting.
- **Android lint: no issues found.**
- In-place API 29 emulator migration and signing continuity were verified before resetting the disposable test installation.
- **Android UI test passed** (`OK (1 test)`, 167.81 seconds): baseline navigation/keyboard/history, description approval, persistent summary failure/retry guidance after snackbar dismissal, chapter approval, context inspection, a free-only model picker with labeled fixtures and paid entries excluded, real Android Keystore encryption/decryption, and exclusion of the test credential from private-preference plaintext and portable backups.
- Reviewed screenshots are under `docs/stage3-evidence/screenshots/`. The model-picker screenshot explicitly uses `[TEST]` fixtures to verify controls; those fixtures are not bundled as a production catalog. The app fetches its real catalog at runtime.

Build environment: full JDK 17, SDK 35/Build Tools 35.0.0, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20. Minimum runtime remains Android 8.0/API 26. The Android 10/API 29 emulator uses software acceleration and a 360 × 640 dp display; it is not representative of physical-phone performance.

## User verification path

Install the Stage 3 APK over Stage 1. Open AI settings, refresh current models, keep Free only and the free-variant filter enabled, choose RP/image models, enter the key on-device, enable the data-sharing checkbox, and Save. Approve a visual description in Gallery; choose Live RP, review Context, send a scene action, and approve a chapter. Restart and continue from that memory. Try offline browsing and portable backup/restore.

No usable OpenRouter key was provided to this environment. HTTP behavior is verified with a controlled local server; UI model options use conspicuously labeled test fixtures. **Live OpenRouter authentication, model quality, current account quotas, and live free-model availability must be verified with the user's account/device.** No real key is bundled or included in screenshots. Physical-device and Android versions other than the API 29 emulator remain unverified here. The external Android document-provider picker/export-provider round trip is not automated; repository import/restore behavior is tested.

## Boundaries and next stage

The POC still has one world and one active character/location/scene. There is no streaming, ensemble autonomy, branching, manuscript pipeline, world/entity creation/deletion system, generalized tool framework, media generation, automatic canon promotion, automatic sync, publication, or commercial deployment. RP/image requests are capped at 1,000 output tokens; summaries allow up to 4,096, reduced by model limits and approximate input budgeting. Reasoning-heavy models can still exhaust the output budget, and provider quotas remain account dependent. Source images are sent only for an explicitly confirmed image-description action; RP uses approved descriptions.

Stop after **Stage 3** for the user's verification. Stage 4 should begin only after acceptance and selection of one real complementary tool/exchange. Do not introduce integrations or operational agent permissions into the fictional world runtime.
