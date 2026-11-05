# Void Canon downloads

## 0.4.2 preview — Cloudflare artwork

**[Download Void Canon Test 0.4.2 (APK)](https://github.com/sorrowscry86/Void-Canon/raw/refs/heads/feature/scene-continuity/downloads/void-canon-0.4.2-preview.apk)** · [SHA-256](void-canon-0.4.2-preview.apk.sha256)

Updates the existing **Void Canon Test** 0.4.1 preview in place; signing certificate and package are retained. Keep your original Void Canon app installed. Backing up the test app before updating is recommended. Database/backup formats are unchanged.

In **Gallery**, choose an entry and tap **Generate artwork**. Import the private image connection file supplied separately, enter a prompt, optionally select an existing reference, allow sending the selection to Cloudflare, and generate. Preview before **Save to Gallery**. Saving creates an exploratory reference without replacing your existing image; use the portrait/background controls when ready.

The private connection is deliberately absent from this public repository and APK. Saved artwork works offline and exports with its provenance; access codes stay on the device and are excluded from backups. Generation uses Cloudflare Klein 4B with a bounded development budget and no retries/fallback. It does not create transparent cutouts or generate images during dialogue.

See [verification and limitations](../docs/image-generation-evidence/README.md), [handback](../IMPLEMENTATION-HANDBACK.md), and [service contract](../server/README.md). The live workers.dev endpoint is deployed, but the build environment blocks outbound access to it; full public-endpoint verification remains pending.

## 0.4.1 preview — scene continuity

**[Download Void Canon Test (APK)](https://github.com/sorrowscry86/Void-Canon/raw/refs/heads/feature/scene-continuity/downloads/void-canon-0.4.1-preview.apk)** · [SHA-256](void-canon-0.4.1-preview.apk.sha256) · [Preview notes](PREVIEW-NOTES.md)

Requires Android 8.0 or newer. This is a separate test app named **Void Canon Test** (`org.voidcat.voidcanon.preview`), version **0.4.1-preview**. It installs alongside your current Void Canon app. Its worlds, scenes, settings and keys are stored separately.

1. Download and open the APK on your phone. Android may ask you to allow installs from the browser or Files app.
2. Launch **Void Canon Test**. Explore the sample or create a world.
3. To test your existing worlds, export a ZIP backup from your current app, then restore it through World's box icon in **Void Canon Test**. This replaces the test app's library. The original app retains its own data.
4. In the test app's AI settings, enter your OpenRouter key, select current RP/image models, and enable the disclosure checkbox. Provider keys/settings do not transfer in a backup.
5. In **Play**, select **Live RP**. After a successful turn, use **Scene → Review scene continuity** to inspect or correct positions, activities, held objects and other state. The scripted Demo does not perform automatic tracking.

The original signing key is unavailable here, so this APK uses a separate package rather than attempting an in-place upgrade. Keep your original app installed. Preview backups use format 4 and cannot be imported into the older 0.4.0 app; keep your original export separately.

New source is in the [feature branch](https://github.com/sorrowscry86/Void-Canon/tree/feature/scene-continuity). No provider keys, signing keys, or user-created device data are included in the APK.

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
