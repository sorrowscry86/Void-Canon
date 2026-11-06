# Void Canon 0.4.3 — Artwork, Settings and visible exits

[Download the regular APK](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.4.3/downloads/void-canon-0.4.3.apk) · [APK checksum](void-canon-0.4.3.apk.sha256) · [Source ZIP](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.4.3/downloads/void-canon-0.4.3-source.zip) · [Source checksum](void-canon-0.4.3-source.zip.sha256)

## Install over the regular app

This is **Void Canon**, package `org.voidcat.voidcanon`, version **0.4.3 / build 8**, Android 8.0+. It is signed with the same certificate as regular 0.4.0/0.4.1. Open the APK and choose **Update** over your existing app. Do not uninstall or clear app data. Existing worlds, artwork, drafts, transcripts, chapter memory, scene continuity and device-local AI configuration remain in that installation. An optional portable export provides an additional copy.

Open **Settings** in the app header to confirm **Version 0.4.3 · build 8** and **Regular Void Canon app**. The historical **Void Canon Test** app has separate data and is not the target of this update. No new preview is published.

## Included features

- **Artwork in the regular app:** Gallery → choose a character/location → Generate artwork. Review the prompt, size and optional selected image before consent; preview the result before Save to Gallery or Discard. Saved images start as exploratory references, with prompt/model/source provenance. Existing portraits, source transcripts, descriptions and continuity are not silently replaced.
- **Central Settings:** access OpenRouter AI/models, the artwork connection and backup export/restore from one menu. Closing a child returns to Settings. The app header and Settings show installed version/build.
- **Visible exits:** Back remains available when the keyboard hides bottom navigation. Leaving Play retains the unsent draft. Modal Close controls remain outside scrolling content.
- **Existing library and continuity:** includes regular 0.4.1 behavior, retaining Room schema 3 and portable backup format 4. There is no data reset or package migration.

## Artwork setup and limits

In **Settings → Artwork connection**, import the private image connection JSON supplied separately. The public APK/source contain no service access code or Cloudflare account key. Connection credentials are encrypted on the device and excluded from backups. Existing OpenRouter settings remain local; artwork uses its separate configured Cloudflare service.

Generation sends only the reviewed prompt and optional one selected reference. Saved art works offline; generating requires networking and the configured service. Klein 4B has fixed supported sizes, a conservative 5,000-estimated-neuron UTC daily development budget, and no automatic retries, alternate models or paid fallback. This development guard is not an account-wide billing cap. Generated images are not transparent cutouts. Saving does not automatically select them or approve their visual description.

## Verification and delivery

The exact regular APK was rebuilt with the original signing identity. Build, 59 unit tests and lint passed. The published 0.4.1 APK upgraded in place to this 0.4.3 artifact on the API 29 software emulator, retaining all nine domain tables, stable IDs, draft, continuity revisions, image hashes and model-preference bytes. Current Android navigation/artwork results and captures are recorded in [release evidence](../docs/release-043-evidence/README.md).

Android artwork checks use a local HTTPS solid-color fixture. They do not establish live model quality or a phone-to-deployed-Worker roundtrip. Physical-phone testing is separate. The source workspace's earlier provider/service evidence is preserved in the linked handbacks. This remains an early debug build.

The version tag is `v0.4.3`. The source ZIP records its source revision in `SOURCE-REVISION.txt` and excludes distribution binaries/archives, private connections, signing/provider keys and emulator/user data. APK/source checksums are published beside the files. Files are hosted directly in the repository because the publishing environment supports Git pushes but blocks the release-upload API; no formal GitHub Release object is claimed.
