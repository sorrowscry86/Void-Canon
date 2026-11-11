# Void Canon downloads

## 0.5.0 — current regular Void Canon update

**[Download Void Canon 0.5.0 (APK)](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.5.0/downloads/void-canon-0.5.0.apk)** · [APK SHA-256](void-canon-0.5.0.apk.sha256) · [Release notes](RELEASE-0.5.0.md)

[Source ZIP](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.5.0/downloads/void-canon-0.5.0-source.zip) · [Source SHA-256](void-canon-0.5.0-source.zip.sha256)

Regular **Void Canon 0.5.0 / build 11**, Android 8.0+. Install over your existing regular app to retain data and private connections. Original signing identity retained.

Adds the first MLRPE-inspired Freeform Story controls, per-scene narrative preferences, a compact accessible Play icon row and a larger response panel. Classic remains the default; AURPG mechanics remain planned. Room and backup are now 5; backups 1–4 remain readable. Earlier app versions cannot import new version-5 backups.

See [mode contract](../docs/NARRATIVE-MODES.md) and [verification](../docs/release-050-evidence/README.md). No private connection, service token or signing material is bundled.

## Earlier 0.4.5 — regular Void Canon update

**[Download Void Canon 0.4.5 (APK)](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.4.5/downloads/void-canon-0.4.5.apk)** · [APK SHA-256](void-canon-0.4.5.apk.sha256) · [Release notes](RELEASE-0.4.5.md)

[Source ZIP](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.4.5/downloads/void-canon-0.4.5-source.zip) · [Source SHA-256](void-canon-0.4.5-source.zip.sha256)

Regular **Void Canon 0.4.5 / build 10**, Android 8.0+. Install over your current regular app to retain data and private connections. The original signing certificate matches 0.4.4.

Fixes automatic-artwork settings that failed to save when connection/reference setup was incomplete. Completed scene images now populate Gallery automatically with visible-context captions, also displayed in Play. Gallery copies survive cache eviction and are included in backups. They remain exploratory; portraits, approved descriptions and story records stay under your control.

See [current verification](../docs/release-045-evidence/README.md). The existing private connection file is configured separately; it is never bundled in public downloads.

## Earlier 0.4.4 — regular Void Canon update

**[Download Void Canon 0.4.4 (APK)](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.4.4/downloads/void-canon-0.4.4.apk)** · [APK SHA-256](void-canon-0.4.4.apk.sha256) · [Release notes](RELEASE-0.4.4.md)

[Source ZIP](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.4.4/downloads/void-canon-0.4.4-source.zip) · [Source SHA-256](void-canon-0.4.4-source.zip.sha256)

Regular **Void Canon**, package `org.voidcat.voidcanon`, version **0.4.4 / build 9**, Android 8.0+. Install over the existing regular app without uninstalling or clearing data. The original signing certificate matches 0.4.3.

Adds opt-in automatic scene illustrations, grounded prompt building, independent RP/image work, prompt inspection, pause/view controls and explicit Gallery retention. Import your private connection through **Settings → Artwork connection**, then configure the current scene through **Play → Artwork**. Automation starts disabled; defaults are 512×512, 30 automatic attempts/device/UTC day and at least 30 seconds between automatic dispatches, within the existing shared service budget.

Room migrates 3→4, preserving narrative records and private images. Portable backup format remains 4; artwork jobs/cache/permissions stay device-local. See [signed-upgrade and Android evidence](../docs/release-044-evidence/README.md). Physical-phone/live artwork assessment is separate from local HTTPS fixture checks.

## Earlier 0.4.3 — regular Void Canon update

**[Download Void Canon 0.4.3 (APK)](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.4.3/downloads/void-canon-0.4.3.apk)** · [APK SHA-256](void-canon-0.4.3.apk.sha256) · [Release notes](RELEASE-0.4.3.md)

[Source ZIP](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.4.3/downloads/void-canon-0.4.3-source.zip) · [Source SHA-256](void-canon-0.4.3-source.zip.sha256)

Updates your existing **Void Canon** app: package `org.voidcat.voidcanon`, version **0.4.3 / build 8**, Android 8.0+. Install over it without uninstalling or clearing data. The APK uses the same signing certificate as regular 0.4.0/0.4.1. **Void Canon Test** remains a separate historical preview.

Includes Cloudflare artwork generation with reviewed prompts/references and explicit save, central Settings, visible version/build, and Back/Close fixes. Open **Settings → Artwork connection** to import the private connection file supplied separately; it is absent from the APK and public source. Saved images remain usable offline. Live generation requires networking and the configured service.

See [verification, data retention and limits](../docs/release-043-evidence/README.md). Room schema 3 and portable backup format 4 are unchanged; no user-data reset or migration to a separate package is required.

## Earlier 0.4.1 — regular Void Canon update

**[Download Void Canon 0.4.1 (APK)](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/main/downloads/void-canon-0.4.1.apk)** · [APK SHA-256](void-canon-0.4.1.apk.sha256) · [Release notes](RELEASE-0.4.1.md)

[Source ZIP](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/main/downloads/void-canon-0.4.1-source.zip) · [Source SHA-256](void-canon-0.4.1-source.zip.sha256)

This is **Void Canon**, package `org.voidcat.voidcanon`, version **0.4.1** (code 6), Android 8.0 or newer. It contains the new scene-continuity features in the regular app. The APK signing certificate matches the published 0.4.0 build.

1. Download the APK and open it on your Android device.
2. Choose **Update** over your existing Void Canon installation. Do not uninstall or clear app data; existing worlds, drafts, history, artwork, chapter memory, and AI settings remain in that app.
3. Open **Play → Scene → Review scene continuity** to use the new state review/correction screen. Validated Live RP turns save story and state together.

The older **Void Canon Test** preview below is optional and installs separately. Its data is not automatically merged into the regular app. Keep original exports when transferring data manually; restore replaces the receiving library, and 0.4.0 cannot read new format-4 backups.

## Historical 0.4.2 preview — Cloudflare artwork

**[Download Void Canon Test 0.4.2 (APK)](https://github.com/sorrowscry86/Void-Canon/raw/refs/heads/feature/scene-continuity/downloads/void-canon-0.4.2-preview.apk)** · [SHA-256](void-canon-0.4.2-preview.apk.sha256)

Updates the existing **Void Canon Test** 0.4.1 preview in place; signing certificate and package are retained. Keep your original Void Canon app installed. Backing up the test app before updating is recommended. Database/backup formats are unchanged.

In **Gallery**, choose an entry and tap **Generate artwork**. Import the private image connection file supplied separately, enter a prompt, optionally select an existing reference, allow sending the selection to Cloudflare, and generate. Preview before **Save to Gallery**. Saving creates an exploratory reference without replacing your existing image; use the portrait/background controls when ready.

The private connection is deliberately absent from this public repository and APK. Saved artwork works offline and exports with its provenance; access codes stay on the device and are excluded from backups. Generation uses Cloudflare Klein 4B with a bounded development budget and no retries/fallback. It does not create transparent cutouts or generate images during dialogue.

See [verification and limitations](../docs/image-generation-evidence/README.md), [handback](../IMPLEMENTATION-HANDBACK.md), and [service contract](../server/README.md). The live workers.dev endpoint is deployed, but the build environment blocks outbound access to it; full public-endpoint verification remains pending.


## Earlier isolated 0.4.1 preview

**[Download Void Canon Test (APK)](https://github.com/sorrowscry86/Void-Canon/raw/refs/heads/main/downloads/void-canon-0.4.1-preview.apk)** · [SHA-256](void-canon-0.4.1-preview.apk.sha256) · [Preview notes](PREVIEW-NOTES.md)

Requires Android 8.0 or newer. This is a separate test app named **Void Canon Test** (`org.voidcat.voidcanon.preview`), version **0.4.1-preview**. It installs alongside your current Void Canon app. Its worlds, scenes, settings and keys are stored separately.

1. Download and open the APK on your phone. Android may ask you to allow installs from the browser or Files app.
2. Launch **Void Canon Test**. Explore the sample or create a world.
3. To test your existing worlds, export a ZIP backup from your current app, then restore it through World's box icon in **Void Canon Test**. This replaces the test app's library. The original app retains its own data.
4. In the test app's AI settings, enter your OpenRouter key, select current RP/image models, and enable the disclosure checkbox. Provider keys/settings do not transfer in a backup.
5. In **Play**, select **Live RP**. After a successful turn, use **Scene → Review scene continuity** to inspect or correct positions, activities, held objects and other state. The scripted Demo does not perform automatic tracking.

This earlier preview was built in an environment without the original signing identity and uses a separate package. The regular update above now uses the verified original signing identity. Keep your original app installed. Preview backups use format 4 and cannot be imported into the older 0.4.0 app; keep your original export separately.

The 0.4.1 source and this preview are included on [main](https://github.com/sorrowscry86/Void-Canon/tree/main). No provider keys, signing keys, or user-created device data are included in the APK.

## Earlier 0.4.0 downloads

This is an early Android debug build. Requires Android 8.0 or newer.

- [Download the APK](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/main/downloads/void-canon-0.4.0.apk)
- [Download the source ZIP](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/main/downloads/void-canon-0.4.0-source.zip)
- [APK SHA-256](void-canon-0.4.0.apk.sha256)
- [Source ZIP SHA-256](void-canon-0.4.0-source.zip.sha256)
- [Build notes and validation](RELEASE-NOTES.md)

Open the APK on your Android device and install it over your existing Void Canon app to keep its data. Android may ask you to allow installs from the browser or Files app. In Void Canon, tap **Worlds → New world** to begin your own setting.

If the browser opens a file preview, use its download/save action. These are actual GitHub-hosted files; no workspace access or Google Drive storage is required.

The source ZIP is the source snapshot at commit `1e0707f3ddb30c91e84cdecf0d7e06d43e17b2e8`, including documentation and verification evidence. Later commits add the GitHub download packaging; app behavior and APK bytes are unchanged. The APK retains the signing identity used by earlier builds. Provider credentials, signing keys, and user-created on-device worlds are excluded.

The files are hosted directly in Git because the publishing environment permits Git pushes but blocks the GitHub release-upload API. A formal GitHub Release has not been created.
