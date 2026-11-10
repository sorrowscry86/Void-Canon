# Void Canon — automatic scene artwork

Regular release **0.4.4 / code 9**, package `org.voidcat.voidcanon`, Android 8.0+.
PR #5 implements automatic scene artwork and grounded prompt building. The
original distribution workspace signs the regular update with the same identity
as 0.4.3, validates the populated in-place upgrade, and publishes the APK/source
archive, checksums and release documentation. See [0.4.4 release notes](downloads/RELEASE-0.4.4.md)
and [signed-artifact evidence](docs/release-044-evidence/README.md).
The prior release is preserved in [its handback](docs/RELEASE-043-HANDBACK.md).

## Implemented behavior

Settings → **Automatic scene artwork**, or Play → **Artwork**, configures the
selected scene. Import the existing private image connection first. Enable the
upload permission once, optionally write an 800-character visual brief and a
300-character world style, then save. Defaults: 512-square illustrations, 30
automatic attempts per device per UTC day, at least 30 seconds between automatic
dispatches, and illustrated Play. Automation starts disabled in existing scenes.

The prompt compiler consumes selected non-exploratory references, hash-matched
approved visual descriptions, the optional author brief/style and visible
continuity state. It does not read author notes, knowledge, lore or chapter
memory. A bounded optional visual observation in the existing RP response can
supply major action/environment excerpts with exact supporting quotes. Invalid
optional artwork data does not reject an otherwise valid story/state response.
No second text-model request is made. The source-message IDs and quotes for
carried action/weather details remain attached to those original observations.

Prompts prioritize identity, current appearance, visible action, setting and
composition/style. They stay within 2,000 characters, omit whole optional blocks
with diagnostics, and fail locally rather than silently clipping essential
identity. The character reference is preferred when the character is present
with the player; otherwise the current location reference is used. The player
stays outside the frame. Missing identity/reference material pauses artwork
until the author supplies a visual brief/reference.

Visual fingerprints ignore dialogue, known facts, elapsed seconds and minor
positions. Location, visible appearance/objects, reference/profile/style changes
and supported major visual moments can create a new candidate. The latest pending
automatic candidate supersedes older pending work. RP and images have separate
jobs; a shared image lane gives manual Gallery work priority after the current
request. Dialogue and story commits do not wait for an image.

Completed matching images appear in Play, without the layered cutout. **Layered
view / Illustrated view** switches rendering; **Pause artwork** stops automatic
uploads, and **View prompt** exposes sources, omissions, cost, explicit regeneration
and **Keep in Gallery**. Late/mismatched images never overwrite the active view.
Image errors fall back to the existing Play rendering and leave story data intact.

## Persistence and compatibility

Room **4**, migration `MIGRATION_3_4`, adds operational illustration jobs, current
visual frames and a device allowance ledger. Accepted RP commits record eligible
image intents in the same transaction. These tables are absent from the narrative
snapshot/fingerprint and portable archives. **Backup format remains 4**; older
archives remain readable, and restore clears image jobs/frames and upload
permissions while retaining the device's consumed daily allowance.

The image cache is separate from Gallery and bounded to 50 MiB with LRU eviction.
The current image and Gallery originals are protected. Kept illustrations use
existing idempotent exploratory-asset import, with prompt/model/reference and
scene/message/fingerprint provenance; they do not select portraits, approve
visual descriptions or become future automatic identity references.

Pending jobs survive restart. Submitted requests become interrupted and are
never automatically replayed. Failures/cancellations after an attempt reservation
retain its local allowance. Authentication errors require reconnection; service
budget exhaustion pauses until the next UTC day. A foreground Play view is required
for dispatch. No WorkManager service, per-turn requirement or background simulation.

## Service and verification

The deployed Klein Worker/API and its 5,000-estimated-neuron UTC ledger are
unchanged. Each referenced 512-square image reserves 32 estimated neurons; 30
automatic attempts reserve at most 960, shared with the service's existing overall
budget. Manual requests still use the overall service budget. There are no automatic
retries, paid fallbacks or Cloudflare billing changes. Connection secrets remain
encrypted, outside Git/APKs/exports. See [service contract](server/README.md).

Checks and limitations: [automatic artwork evidence](docs/automatic-artwork-evidence/README.md).
The new Android flow uses local HTTPS fixtures. Fixtures cannot establish live
Klein identity consistency or phone-to-deployed-Worker delivery. No live inference
is used for automated verification; physical-phone/visual-quality assessment remains
separate. The build environment still lacks access to the live workers.dev host.

## Build, signing and distribution

Use a full JDK 17 or 21, Android platform/build tools 35 and the pinned Gradle
8.11.1 wrapper. Select the original private key locally:

```sh
./gradlew -PvoidCanonDebugKeystore=<private-original-key-path> assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The distribution workspace retains the original key outside Git. The outgoing
regular APK certificate must match the published 0.4.3 certificate SHA-256:
`d2c8a958809475255d9fa24ead371f3225f19e3a5189db3b416217817de5f56a`.
Its app name remains **Void Canon**, package `org.voidcat.voidcanon`, version
**0.4.4 / build 9**. Install over the existing regular app to retain data.
A workspace's default debug key is not interchangeable with this identity.
No private signing key, provider credential, development image connection,
emulator database or user-created content belongs in public artifacts.

[Release evidence](docs/release-044-evidence/README.md) separates the original
source-workspace checks from the distribution rebuild, migration and Android
fixture rerun. The release source ZIP records its source revision and excludes
distribution binaries/archives and device data. Earlier published APKs remain
available; no new separate Test launcher is published.

## Limitations and next increment

Physical-phone verification of the regular update, existing image connection,
keyboard behavior and one live illustrated RP session remains separate from
emulator/TLS fixtures. A live assessment must judge identity, outfit/hand fidelity
and useful visual-change frequency; fixtures do not establish those qualities.
The unchanged Worker needs no new deployment for this release. No Cloudflare
billing settings or service budgets were changed. Campaign/ensemble depth,
background image generation, synchronization and automatic canon promotion
remain outside this increment; see [the roadmap](docs/PRODUCT-ROADMAP.md).
