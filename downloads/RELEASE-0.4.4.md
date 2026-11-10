# Void Canon 0.4.4 — Automatic scene artwork

[Download the regular APK](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.4.4/downloads/void-canon-0.4.4.apk) · [APK checksum](void-canon-0.4.4.apk.sha256) · [Source ZIP](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.4.4/downloads/void-canon-0.4.4-source.zip) · [Source checksum](void-canon-0.4.4-source.zip.sha256)

## Install / upgrade

Regular **Void Canon**, package `org.voidcat.voidcanon`, version **0.4.4 / build 9**, Android 8.0+. The original signing certificate matches regular 0.4.3. Install over the existing regular app without uninstalling or clearing data. Worlds, characters, locations, private images, drafts, transcripts, chapter memory, continuity and device-local provider settings remain in that installation. Settings shows **Version 0.4.4 · build 9**. An optional portable export provides an additional copy before updating.

## Start using automatic artwork

1. Keep or import the existing private image connection through **Settings → Artwork connection**. The public APK/source contain no service access code. OpenRouter RP configuration remains separate.
2. Select a scene and open **Play → Artwork**, or **Settings → Automatic scene artwork**. Automation starts disabled. Review the selected reference/approved description; optionally enter a character visual brief and world style.
3. Check **Allow automatic uploads…** and choose **Save artwork settings**. The first illustration and meaningful visible changes can generate while Play is foregrounded. Dialogue and story completion continue independently of an image request.
4. Open **Artwork → View prompt** to inspect the assembled prompt, selected reference, source IDs/quotes, omitted optional material and estimated usage. Pause, switch layered/illustrated views, or explicitly regenerate when needed.
5. Choose **Keep in Gallery** to retain/export a result as an exploratory asset. Unkept images live in a bounded cache. Generated artwork does not replace portraits, approve descriptions or change lore/continuity automatically.

## Included changes and bounds

- Local grounded prompt building from selected non-exploratory references, approved visual descriptions, visible continuity state, optional brief/style and bounded source-quoted visual cues. Private notes, knowledge, lore and chapter memory are excluded. No second text-model request is added. Invalid optional visual data does not reject a valid story/state turn.
- Independent RP/image jobs, latest-pending coalescing, foreground dispatch and stale-result checks. Manual Gallery requests have priority after the current image. Submitted/interrupted/failed requests are not automatically replayed.
- Defaults: **512×512**, **30 automatic attempts/device/UTC day**, at least **30 seconds** between automatic dispatches, and a **50 MiB** LRU cache. Failed/interrupted attempts can count. The unchanged shared service budget also applies; there is no automatic retry or paid fallback.
- Room **3→4** adds image jobs, visual frames and an allowance ledger while retaining existing narrative rows/IDs. Portable backup format stays **4**. Jobs, frames, cache, permissions and artwork preferences are device-local; restoring clears operational jobs/frames/upload permission while retaining consumed device allowance. Only explicitly kept Gallery assets enter exports.
- Image read and overall call timeouts are aligned at 120 seconds. Artwork settings remain open while editing with the keyboard; the Close control stays outside scrolling content.

## Verification and delivery

The original release workspace rebuilt and signed the regular APK, ran the full 81-test regression suite and lint, and checked the published 0.4.3 → signed 0.4.4 upgrade with retained authored records, image hashes and model-setting bytes. Current Android fixture results, migration detail and portrait/keyboard captures are in [release evidence](../docs/release-044-evidence/README.md). Source-workspace checks are preserved separately in [automatic-artwork evidence](../docs/automatic-artwork-evidence/README.md).

Android tests use local HTTPS/solid-color image fixtures. Physical-phone behavior, live Klein identity/outfit/hand fidelity and phone-to-deployed-Worker delivery require separate assessment. The Worker API/model, deployment and billing settings are unchanged. This remains an early debug build.

Tag: `v0.4.4`. The source ZIP records its source revision in `SOURCE-REVISION.txt` and excludes distribution binaries/archives, private connections, provider/signing keys and device data. Files use the repository's existing Git-hosted download route because release-upload API access is unavailable here; no formal GitHub Release object is claimed. Prior release downloads remain available.
