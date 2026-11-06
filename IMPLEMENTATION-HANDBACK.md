# Creation wizard — next-version checkpoint

The November 6 user request authorizes moving directly toward the next live version, saving checkpoints and debugging as needed; do not stop at the original sequential stage boundaries. Extend the existing app, not a new prototype.

Current working checkout: `C:/Users/sorro/Documents/Codex/void-canon-live`; branch `feat/creation-wizard`. Baseline was main/0.4.3. Source is now 0.5.0/build 9. The historical distribution notes below document 0.4.3, not the new increment.

Implemented: character/world wizard, approved visual-description inputs, editable proposals and selection of up to four starter entries; persistent atomic private draft; manual no-AI path; existing OpenRouter provider/configuration; single-page Fandom/Wikia importer and pasted fallback; transactionally accepted stable-ID creations with source provenance. Room schema 3 and portable format 4 are unchanged. See `docs/CREATION-WIZARD.md` for behavior and limits.

Checkpoints pushed to `feat/creation-wizard`: `2dec522` (initial implementation), `0f0118e` (validation, recovery, provenance), and `5fa4c0c` (Android CI and correct target-world context for resumed drafts). Review: https://github.com/sorrowscry86/Void-Canon/pull/4.

## Current verification and release blockers

- Nine new tests cover parsing/bounds, untrusted source separation, wiki URL/revision/text conversion, duplicate acceptance, world targeting, draft persistence, and backup/restore. No passing test result is claimed yet.
- `git diff --check` passed. Java 17.0.17, SDK Platform 35, Build Tools 35.0.0, and Gradle 8.11.1 were installed/prepared outside Git.
- Local command `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` is running through unusually slow first-run Gradle initialization/dependency setup. Process inspection showed active work, not a completed test run. The running command session in this chat is `87005`; inspect it before starting a competing Gradle build. Toolchain environment paths are described below; JAVA_HOME is the extracted JDK directory, ANDROID_HOME is `work/void-build/android-sdk`, GRADLE_USER_HOME is `work/void-build/gradle`, and ANDROID_USER_HOME is `work/void-build/android-user`, all beneath `C:/Users/sorro/Documents/Codex/`.
- GitHub Actions runs https://github.com/sorrowscry86/Void-Canon/actions/runs/42185963773 and https://github.com/sorrowscry86/Void-Canon/actions/runs/42185958908 failed before any steps or runner assignment. GitHub's diagnostic annotation says: “The job was not started because your account is locked due to a billing issue.” No billing settings were changed.
- No emulator/device walkthrough, live OpenRouter generation, or live Fandom import has been verified for this increment. Wiki source handling is covered by added fixtures, not live-access evidence.
- No 0.5.0 APK, source release tag, or replacement regular download has been published. Main and the regular 0.4.3 download remain untouched. The original signing key/workspace location was requested from the user and is pending. Do not imply the PR is a live app release.

This new Windows workspace initially had neither Java nor Android SDK. Local build tooling is being prepared outside Git at `C:/Users/sorro/Documents/Codex/work/void-build`. `local.properties` is untracked. The original signing key described below was available in a different distribution workspace and has not been found here. Do not publish an APK signed by a new key as a regular upgrade. The user has been asked for the original key/workspace location; no secret value should be pasted into chat or committed.

---

# Void Canon — regular-app update and navigation

Project: `/workspace/Void-Canon`. Source version **0.4.3** (code 8), Android 8.0/API 26+.
The regular **Void Canon 0.4.3** update includes the reviewed Cloudflare image
workflow, central Settings, installed version/build display, and persistent
Close/Back controls. PR #3 supplies the implementation; the original distribution
workspace supplies the matching signing identity and regular APK. See the
[release notes](downloads/RELEASE-0.4.3.md), [release verification](docs/release-043-evidence/README.md),
and [UI/navigation handback](docs/UI-NAVIGATION-HANDBACK.md).

The artwork increment was previously distributed only as the separate 0.4.2
preview. It is now included in the regular package. Earlier scene-continuity
behavior is preserved in the [continuity handback](docs/SCENE-CONTINUITY-HANDBACK.md).

## Flow and author control

**Gallery → choose character/location → Generate artwork.** Import the private
image connection JSON once. Enter a prompt, select 512×512, 1024×1024 or 1024×576,
optionally select one image from this entry, and allow sending that material to
Cloudflare. Generate, preview, then save to Gallery or discard. No history, lore,
or author notes are silently included. Selected references are hash-checked and
reduced to PNG with a maximum side of 448 pixels.

Saving creates a new exploratory reference with a stable request/asset ID and
model, prompt, size, original reference ID/hash and timestamp in provenance.
Saving is idempotent and retains the recorded owner across selection changes.
It does not select artwork, approve descriptions, or change continuity/canon.
Use the existing Gallery controls to select a portrait/background and review its
description/reference use. These outputs are not transparent cutouts.

Completed previews persist privately across app restarts until saved/discarded.
Only saved images enter exports. **Room schema 3 and backup format 4 remain
unchanged**; original transcripts, IDs, chapters and offline use are preserved.

## Service and connection

Endpoint: `https://void-canon-dev-images.sorrowscry86.workers.dev`.
Worker: `void-canon-dev-images`; D1: `void-canon-image-budget`,
ID `fe57d66c-c590-40b3-877c-e33e09df5b01`. See [server](server/README.md).

Android contains no Cloudflare account key or bundled service access code.
`/workspace/void-canon-image-connection.json` is the private development handoff,
outside the repository. Importing encrypts its scoped image-service access code
with Android Keystore. Credentials/settings are excluded from portable and
framework backups. Disconnect removes the local connection, keeping images.
Anyone holding this file can use the development image service; rotate the
Worker's APP_ACCESS_TOKEN and regenerate connection files if shared.

Only FLUX.2 Klein 4B and bounded sizes/references are allowed. D1 transactionally
reserves a conservative integer estimate against a **5,000-neuron UTC daily
development budget**, including failures/timeouts. Unique request IDs prevent
replay inference. No automatic retries, alternate models or paid fallback.
This is not an account-wide billing cap: the 10,000-neuron free allowance is
shared with other account AI usage. No billing settings were changed.
The server stores IDs/dates/reservations/status, not images or prompts.

## Verification

The original distribution workspace rebuilt the regular 0.4.3 artifact using
JDK 17.0.17, SDK/build tools 35 and Gradle 8.11.1:
`assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest`.
All 59 JVM tests passed and lint reported no issues. Exact current APK identity,
upgrade results, Android test logs, and screenshots are in
[0.4.3 release evidence](docs/release-043-evidence/README.md).

The JVM suite covers HTTPS privacy/response validation, no retries/redirects,
cancellation, stable owners/baselines, duplicate saves, portable provenance,
preview recovery, dimensions, reference resizing, repository/backup validation,
continuity and OpenRouter behavior. Earlier provider/service verification is
preserved in [artwork evidence](docs/image-generation-evidence/README.md) and
[navigation evidence](docs/ui-navigation-evidence/README.md), including four
workerd/Miniflare checks with fixture AI and real SQLite D1. Those fixtures are
not live generation evidence.

The source workspace verified Worker deployment, AI/D1/secret bindings and
workers.dev activation. This environment blocks the deployed hostname; a
phone → deployed Worker → AI result is still not claimed. The Android artwork
walkthrough uses a local HTTPS fixture with a solid-color response. Physical-phone
layout, live provider behavior and generated artwork quality require separate
verification. See release evidence for the software-emulator System UI stall
and recovery; no application data was cleared.

## Build and signing

Regular updates must use package `org.voidcat.voidcanon`, launcher **Void Canon**,
and the original certificate SHA-256
`d2c8a958809475255d9fa24ead371f3225f19e3a5189db3b416217817de5f56a`.
The original key was available privately in the distribution workspace.
The regular 0.4.3 APK was built with `-PvoidCanonDebugKeystore=<original-key-path>`
and its public certificate verified against the published regular 0.4.1 APK.
The key is never included in Git, APKs, portable backups or source archives.
Use the same identity for future updates and verify every outgoing APK.
A different development key cannot update the existing regular installation.

Historical preview: `org.voidcat.voidcanon.preview` / **Void Canon Test**,
certificate SHA-256
`be36b09130f79882b6a032b7cbfc8c0515737952dac493d23c71cbbb7265a4c2`.
It is separate from the regular app. If rebuilding that historical variant,
select its own private preview key; the key paths in other workspaces are not
interchangeable. No new preview is published for 0.4.3.

## Storage and next increment

Structured library data remains in Room schema 3; app-private image files keep
stable asset IDs and hashes. Completed unsaved image previews remain private
until saved/discarded; only saved art enters portable backup format 4. Device-local
provider credentials/settings remain outside portable backups. Artwork service
codes are encrypted with Android Keystore. The update does not change schemas
or reset existing work. The released source ZIP excludes signing keys, service
connections, provider secrets, emulator databases and user-created content.

The immediate follow-up is physical-phone verification of the regular update,
Settings/keyboard exits, private connection import and one reviewed live artwork
request. Keep further feature work incremental; the broader roadmap remains
in [the product roadmap](docs/PRODUCT-ROADMAP.md).

## Boundary

Manual generation/reference editing and review/save only. No per-turn generation,
background removal, automatic canon/state changes, account system, or general
media orchestration. Access is a shared private development code; per-device
revocation and exact account usage metering remain future work. A cancelled or
lost HTTP response may still consume provider work; retries are explicit.
