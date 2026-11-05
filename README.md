# Void Canon

A native Android worldbuilding and roleplay studio. Its illustrated world library lets you create your own worlds, characters, locations, and scene pairings, with **OpenRouter**, optional **Cloudflare artwork generation**, source-linked visual descriptions, inspectable scene context, and approved chapter memory. World editing, saved artwork, transcripts, chapters, and backups remain usable offline.

## Install / upgrade

Download the **[0.4.2 preview APK](https://github.com/sorrowscry86/Void-Canon/raw/refs/heads/feature/scene-continuity/downloads/void-canon-0.4.2-preview.apk)** to test Cloudflare artwork generation and scene continuity. It appears as **Void Canon Test**, updates the 0.4.1 test app in place, and installs alongside the original Void Canon app with separate local data.

Requires Android 8.0 or newer. In your current app, export a ZIP backup if you want to test your existing worlds. Open **Void Canon Test**, restore that ZIP, then enter your OpenRouter key and choose models again; credentials are excluded from backups. Restore replaces only the test app's library. Select **Live RP** to exercise automatic state tracking; review it under **Play → Scene → Review scene continuity**.

The [Downloads](downloads/README.md) page has the APK checksum, test instructions, and the earlier 0.4.0 downloads. The preview has its own Android package (`org.voidcat.voidcanon.preview`) because the original signing key is not available in this build environment. Updating the original app in place still requires that original key. Keep your existing app installed.

Build the preview with:

```sh
./gradlew assemblePreview lintPreview
adb install -r app/build/outputs/apk/preview/app-preview.apk
```

The ordinary `debug` variant retains the original package and app name for builds made with the original signing identity. The Room database migrates without destructive recreation.

## Build your own world

1. On World, tap **Worlds → New world**. Give it a name and description; images and AI are optional.
2. Use **Add character** and **Add location**. Open their cards to edit descriptions, lore, and private author notes. Search people and places as the collection grows.
3. Import their images in **Gallery** before or after scene setup. Location backgrounds also set that world's cover. Approve visual descriptions manually or, after scene setup, request an OpenRouter proposal.
4. Tap **Choose scene**, or open an entry and choose **Use in scene**. Select a character and location from this world. An existing pairing resumes; a new pairing gets its own transcript, draft, and chapter memory.
5. Choose **Worlds** to switch settings. Your last selected scene is remembered per world. The Glass Harbor remains available as a sample; its canned demo replies are never used for your own worlds.

The library is an early product milestone, not the complete campaign/branch system. See [the roadmap](docs/PRODUCT-ROADMAP.md) for the larger vision and boundaries.

## OpenRouter and current free models

1. Open **AI** on World, **AI settings** in Gallery, or **AI** in Play.
2. Tap **Refresh current models**. Browsing the public catalog needs no key. **Free only** and **Free variants · no credit purchase** are enabled by default. Search by name or ID, or enable **Image-capable only**. Each result shows its text/image capability and context size.
3. Choose **Use for RP** for roleplay and chapter proposals, and **Use for images** for image descriptions. Select both if one model supports both tasks. There is no hard-coded generation model; choose from the refreshed catalog.
4. Enter your OpenRouter key on your Android device, enable the disclosure/consent checkbox, and tap **Save**. The key is encrypted using Android Keystore and never bundled in the APK or portable backups. Leave the key field blank to keep an existing saved key.
5. In Play, select **Live RP**. **Demo** is explicitly scripted and never calls a model. Without a key, live actions report setup needed; they never pretend that demo replies are AI.

The picker uses current numeric pricing and capabilities, not a static list or a name suffix alone. Free variants are shown first and the initial filter targets `:free` models and the free router. Other zero-price models can require a funded account, so you can turn the variant filter off to see all zero-price options. Free availability and quotas change; the cached catalog shows its refresh date.

In Free only mode, requests set zero maximum prices for prompt, completion, request, and image pricing. Provider fallback is disabled, no alternate models are supplied, and there are no automatic retries. If free routing is unavailable, the app shows an error instead of changing to a paid model. Disabling Free only explicitly allows your selected model to spend OpenRouter credits. Image descriptions have a 1,000-output-token cap. Live RP includes structured state changes and allows up to 4,096 output tokens; chapter summaries also allow up to 4,096 output tokens, reduced to respect the catalog's provider output maximum and estimated remaining context. This includes reasoning tokens. Summaries request at most 500 words. Incomplete outputs are rejected; transcripts remain unchanged. The Chapters screen retains failure details and offers an explicit retry. Provider rate/quota errors explain waiting or choosing another current free model.

## Images that matter to the story

- **Gallery:** import using Android's picker. Images are copied privately and normalized to PNG; choose a portrait, scene background, or actual transparent cutout.
- **Describe / review image:** write observable details manually, or choose **Propose from image**. The latter explicitly confirms sending just that image to OpenRouter. Review/correct the draft, then **Approve** it.
- Descriptions retain their immutable asset ID, source hash, approval state, and provenance. New image imports get new identities; old approvals never silently move to a replacement image.
- **Exploratory** references are excluded from context. Alternate outfits affect the scene only when selected as its active cutout/appearance. Generating another proposal does not replace an existing approved description until you choose to review/save it.
- RP sends approved descriptions, not source images. Image interpretation sends one explicitly selected image and an observation prompt.

## Generate artwork with Cloudflare

1. Download the private image connection JSON supplied separately for your development service. It is not included in this public repository or APK.
2. In **Gallery**, choose a character or location, then **Generate artwork → Import image connection** and choose that file.
3. Enter a prompt and choose a size. Optionally choose one existing image of this entry as the reference; for variants, describe the change while preserving the subject. Review the selected image before allowing transmission.
4. Check the disclosure checkbox and tap **Generate image**. Review the preview, then **Save to Gallery** or **Discard preview**.
5. Saved results are exploratory references with prompt/model/source provenance. Use the existing controls to set a portrait/background and review their visual description/reference use. Generation does not automatically replace existing art or change canon.

Only the displayed prompt and selected reference are sent to the authenticated Cloudflare service. References are reduced to at most 448 pixels per side. Klein 4B is fixed; there are no retries or alternate models. The service reserves up to 5,000 estimated neurons per UTC day, including failed calls, as a conservative development budget. Cloudflare's free 10,000-neuron allowance is shared with other account usage; this guard is not an account billing cap. No paid plan was activated.

The scoped service code is encrypted with Android Keystore and excluded from backups. **Disconnect image service** removes it from the device while retaining saved artwork. Do not share the private connection file: it grants access to the development service. Generated JPEGs are not transparent cutouts. See [implementation notes](IMPLEMENTATION-HANDBACK.md) and [service details](server/README.md).

## Play, context, and chapters

**Scene** edits your role, author direction, character-known facts, provisional scene state, and separate author-only notes. Entry editors also have a private author-only field. Description/lore and character-known fields can reach the model; private author-only fields do not. Do not place secrets in public lore. Model compliance with supplied knowledge is not a deterministic guarantee.

**Context** shows the next request preview, missing/stale descriptions, budget omissions, source IDs, and recorded requests with requested/actual model, settings, status, response, and reported usage. Recent dialogue, selected approved visual references, and approved chapter summaries are prioritized within a bounded character budget. The budget is an approximation, not a model-specific tokenizer.

**Cancel** is available during generation. A completed response atomically saves one user/reply pair. Failed/cancelled requests preserve the draft and existing history; interrupted requests are marked failed at restart and never auto-resumed. Duplicate completion callbacks do not add duplicate turns. A response that finishes committing before cancellation may already be saved. OpenRouter can still account for work performed before cancellation.

**Chapters** lets you write a summary offline or request a draft from OpenRouter. New drafts cover messages after the last approved chapter. Review the read-only source transcript, edit the summary, and **Approve chapter** to include its memory in subsequent RP. Saving or editing a summary never edits the source messages. New RP events and model proposals never rewrite world lore automatically.

## Scene continuity (0.4.1 source)

Live RP keeps compact, scene-local records of participants, positions, activities, relevant conditions, held objects, and explicit elapsed time. The model receives at most 4,000 characters of current state; older revisions stay local. Story and validated changes save together. Invalid model responses preserve your draft, transcript, and state, with details in Context.

Open **Play → Scene → Review scene continuity** to correct details, add existing participants or objects, or use the state before the last update. State is model-visible, provisional scene data; private author notes remain separate. Existing scenes start with unknown details rather than an invented reconstruction of their history. Chapter summaries never update state. Scripted Demo does not extract state.

See [the continuity contract](docs/SCENE-CONTINUITY.md) for bounds, validation, known limitations, and the distinction from future campaigns/play modes.

## Backup and restore

World's box icon exports/restores the **entire local library** as a ZIP containing all worlds, entities, images, approved/draft descriptions, scene selections, drafts, source transcripts, chapters, private author notes, and generation diagnostics. Treat it as your private world data. API keys and AI settings are excluded. Set up the key/models again on another device.

Current exports are version 4; earlier versions 1, 2, and 3 are accepted and upgraded. Blank worlds and multiple scenes are supported. Restore replaces all local worlds after confirmation; it does not merge archives. Restore validates graph references, image hashes, transparency, versions, and archive paths before staging unique image files and transactionally replacing the database. Existing data stays usable on validation failures. No automatic synchronization is provided.

## Build and checks

Use a full **JDK 17 or 21**, Android SDK Platform 35, and Build Tools 35.0.0. Set `ANDROID_HOME`, or put `sdk.dir` in an untracked `local.properties`. Windows uses `gradlew.bat`.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Gradle, Kotlin, Compose, Room, and HTTP dependencies are pinned. Development downloads require networking; core installed-app use does not require a PC or hosted server.

The JVM suite covers multiple worlds/scenes, pre-scene images, selection persistence, cross-world/cross-scene reference rejection, storage, version 1/2 migration/restore, source/approval rules, private-note exclusion, context truncation, chapter memory, duplicate/stale/cancelled generations, numeric free-price filtering, and OpenRouter request/response behavior using a local test server.

Use a **disposable Android installation** for UI tests because they edit the sample world and test key storage. Enable the emulator's software keyboard with `adb shell settings put secure show_ime_with_hard_keyboard 1`, then run `./gradlew connectedDebugAndroidTest`. To retain private screenshots, install and run the same test APK directly:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w org.voidcat.voidcanon.test/androidx.test.runner.AndroidJUnitRunner
adb exec-out run-as org.voidcat.voidcanon tar -C files -cf - screenshots > screenshots.tar
```

The UI test uses explicitly labeled model-catalog fixtures, not live provider calls. Real OpenRouter key/account/model behavior and physical-phone performance require your verification. See [the implementation handback](IMPLEMENTATION-HANDBACK.md), [architecture](docs/ARCHITECTURE.md), [assets](docs/ASSETS.md), and the preserved [Stage 1 handback](docs/STAGE-1-HANDBACK.md).

Original specifications remain in `docs/specifications/`. Stage 4 integrations, ensemble RP, branching, manuscript tooling, and publication remain deferred.

## Documentation

See the [documentation index](docs/README.md) for the current handback, architecture, roadmap, asset provenance, historical specifications, and Android verification evidence. [The changelog](CHANGELOG.md) records release changes.
