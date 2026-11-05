# Void Canon — Cloudflare artwork increment

Project: `/workspace/Void-Canon`. Version **0.4.2** (code 7), Android 8.0/API 26+.
The user approved adding the tested Cloudflare image workflow to the app for development.
The prior implementation is preserved in [scene continuity handback](docs/SCENE-CONTINUITY-HANDBACK.md).

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

- 59 JVM tests pass: HTTPS privacy, response validation, no retries/redirects,
  cancellation, stable owner/baseline, duplicate save, portable provenance,
  preview recovery, dimensions, and reference resizing without source mutation.
- 4 workerd/Miniflare tests pass with real SQLite D1 and a fixture AI binding:
  authorization/validation, multipart input, duplicate IDs, failed reservations,
  and concurrent daily-limit enforcement. Fixtures are not live AI evidence.
- SDK 35, Gradle 8.11.1 and JDK 21: assemblePreview, assembleDebug,
  testDebugUnitTest, lintPreview and assembleDebugAndroidTest are the build checks.
  Final logs and device status are in docs/image-generation-evidence/.
- Worker upload, AI/D1/secret bindings, logs/traces and workers.dev activation
  were verified through Cloudflare's API.
- The deployed workers.dev hostname is blocked by this execution environment's
  destination policy, including connector native fetch. A phone → new Worker
  → AI end-to-end result is not claimed. Earlier live Klein lab tests prove
  model behavior separately, not this deployed service's complete delivery.
- An Android HTTPS fixture walkthrough covers consent, preview and explicit
  save. Its execution/evidence status is recorded in the evidence README.

## Build and signing

The preview retains `org.voidcat.voidcanon.preview` / **Void Canon Test**.
Use the existing preview key for in-place updates; no private key is committed:

```sh
./gradlew -PvoidCanonDebugKeystore=/workspace/.android/debug.keystore assemblePreview lintPreview
```

The optional Gradle property selects the debug-derived variants' existing key.
The preview certificate SHA-256 is
`be36b09130f79882b6a032b7cbfc8c0515737952dac493d23c71cbbb7265a4c2`.
The original production app remains a separate package.

## Boundary

Manual generation/reference editing and review/save only. No per-turn generation,
background removal, automatic canon/state changes, account system, or general
media orchestration. Access is a shared private development code; per-device
revocation and exact account usage metering remain future work. A cancelled or
lost HTTP response may still consume provider work; retries are explicit.
