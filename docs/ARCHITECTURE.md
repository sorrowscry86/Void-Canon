# Void Canon architecture

The 0.4.1 state extension is specified in [Scene continuity](SCENE-CONTINUITY.md): Room schema 3, portable format 4, source-linked state revisions, a 4,000-character state projection within the existing context ceiling, and atomic structured RP completions. Explicit migrations 1→2→3 and archive imports 1–3 are preserved. RP now allows up to 4,096 output tokens subject to model/context limits; image descriptions remain at 1,000.

One Android application module: Compose/Material 3 views, `CanonViewModel`, a local Room repository, and one OpenRouter HTTP adapter. Core storage remains independent of model providers. There is no application server, account backend, tool execution, cloud sync, or media-generation integration.

## World library and selection

`WorldSnapshot` contains portable domain records. `WorldLibrary.scoped` supplies only one selected world's entries/assets/descriptions and one selected scene's messages/chapters/diagnostics. The repository's library flow pairs that projection with world and scene choices. Active IDs are device-local checked preferences, separate from backups. New worlds can contain no entries or scenes; the ViewModel's ready flag distinguishes loading from normal empty states.

Creation/editing reuses existing Room tables. Scene pairing is limited to one character/location pair in the same world; an existing pair resumes rather than rewriting its history. New pairings are independent scenes. Mutations target the selected/source scene, images update only their owner's world, and generation completion resolves the recorded scene independently of UI selection. Source-message selection uses only the active scene's approved chapters. Draft writes and selection actions share the ViewModel action queue; switching is disabled during provider calls.

## Storage and migration

Room schema version 3 retains source-linked `visual_descriptions`, `chapters`, and `generations`; author-only entry notes; scene direction/role/knowledge/private/provisional fields; image SHA-256/reference kind; and message origin/generation links. `MIGRATION_1_2` explicitly adds columns/tables and retains existing IDs and rows. Legacy image hashes are populated from private files at initialization. `MIGRATION_2_3` adds the scene continuity snapshot and before/after event table. Schemas 1, 2 and 3 are checked in.

Asset bytes are immutable through the UI. Importing a different image creates a new asset ID/file/hash. A description records its source hash, status, and provenance. Context excludes draft, stale, and exploratory references, and uses only the selected appearance/background rather than mixing every gallery image. Existing approved descriptions remain active when another AI proposal is generated; the new result is retained in the generation record for explicit review.

Messages retain source text, chronological sequence, speaker role, demo/live origin, and generation ID. Chapters retain immutable source-message references separately from editable summaries and approval status. Saving a chapter never updates source messages or entry lore. Scene provisional state remains separate from approved chapter memory.

## Context and generation

`SceneContext` produces bounded prompt messages, source IDs, and omissions. It reserves recent dialogue, includes author direction and character-known facts, prioritizes approved selected visual descriptions/chapter memory, and trims lower-priority lore explicitly. Maximum budget is 24,000 characters, reduced for small advertised model contexts; it is an estimate rather than exact tokenization. Both entry and scene author-only notes are mechanically excluded from RP, image-description, and chapter-summary prompts. This does not guarantee model compliance with narrative secrecy or prevent an author from explicitly including a secret in public text.

Before a request, the repository persists a unique pending generation record containing the exact text context, source identities/hashes, requested model, fixed request settings, and source-message references where applicable. Image payload bytes are not duplicated as base64 in the audit: the immutable archived image plus hash identifies what was sent. Roleplay sends approved descriptions only; the vision action sends one selected PNG.

Network calls are cancellable and outside the repository mutation lock. A successful RP completion commits one deterministic-ID user/reply pair and updates the request atomically. Repeated completions are ignored. A scene fingerprint detects edits while a request is in flight: stale responses/usage remain in diagnostics but are not applied. Failure/cancellation preserves history and unsent text. Pending requests found after process restart/restore are marked failed, never retried automatically. File/database mutations finish their critical publication section even if the screen closes, so cancellation cannot delete committed images.

## OpenRouter and credentials

The HTTPS adapter uses `GET /api/v1/models` and `POST /api/v1/chat/completions`. Catalog requests include no story material or key. Only text-output chat models are offered, with an image-input filter for vision. Numeric prompt/completion/request/image prices decide zero-price classification; unknown/nonzero pricing is not considered free. The picker initially filters free variants (`:free` or the free router), which OpenRouter distinguishes from other zero-price models that may require a funded account.

Free mode sets `provider.max_price` to zero for all four supported pricing dimensions and disables provider fallbacks. No alternate-model list or automatic retry is used. Paid mode requires disabling Free only and explicitly selecting a model. Requests are non-streaming, capped at 1,000 output tokens for images and up to 4,096 for RP/state responses and chapter summaries, with a 90-second timeout. Summary output is reduced to the catalog's `top_provider.max_completion_tokens` and estimated remaining context (roughly two characters/token plus overhead). Too-small context is rejected locally without dropping source messages; the 24,000-character transcript ceiling remains. Length termination, including empty text after reasoning exhaustion, reports the output budget separately from provider rate/quota errors. Chapters retain failure details and expose explicit retry. Responses record the actual returned model and reported usage; unreported cost is not invented. Cancellation may not stop already-accounted provider work.

The key is entered on the device and encrypted with an Android Keystore AES-GCM key, random IV, and private preferences. It never enters Room, source code, an APK constant, model context, logs, or portable exports. Settings/cache remain device-local. Android cloud backup and device-transfer exclusions cover the private data/preferences. The app requests Internet permission, but sends story material only after opt-in and explicit generation actions.

Authoritative references: [OpenRouter API](https://openrouter.ai/docs/api_reference/overview), [model catalog](https://openrouter.ai/docs/api/api-reference/models/list-all-models-and-their-properties), [routing and price caps](https://openrouter.ai/docs/guides/routing/provider-selection), and [free-model availability](https://openrouter.ai/collections/free-models).

## Portable backups

Version 4 ZIPs contain `manifest.json` and the referenced PNG files. The full-library manifest supports multiple worlds and scene pairings, including empty worlds. The manifest includes the portable record collections, UTF-8 text, stable IDs, source hashes, chapter references, approval states, and diagnostics, plus current continuity state and source-linked revision events. Credentials/settings are excluded; author-private notes remain part of the author's local world backup. Version 1, 2 and 3 archives normalize to version 4 and calculate missing hashes without changing IDs.

Validation rejects unsupported versions, duplicate IDs/files/per-scene sequences, broken/mis-scoped world/scene/source references, invalid approval/source data, unreadable images, mismatched hashes/transparency, unsafe/unexpected paths, and missing/extra images. Limits are 16 MiB manifest, 16 MiB per image, 128 MiB expanded archive, 10,000 total records/files, and bounded text/audit fields. Images imported from the picker are at most 16 MiB/24 megapixels, oriented from EXIF and normalized to PNG up to 2048 pixels on the longest side.

Restore stages and validates the entire archive, copies assets to new unique private filenames, then switches the graph in one Room transaction. Before commit, failed operations remove only new/staged files. After commit, old unreferenced files are removed. A hard process interruption can leave harmless orphan files, but committed rows must not refer to partially copied assets. All import/export/restore mutations share the repository lock.

## Deferred boundaries

Multiple independent worlds and resumable one-character/location pairings are supported. Named campaigns, generic lore types, branching, and ensemble RP remain deferred. There is no branching, ensemble autonomy, automatic canon promotion, manuscript pipeline, external tool action, or commercial deployment. A chosen complementary integration remains optional; the next proposed product milestone is richer World Workshop organization.
