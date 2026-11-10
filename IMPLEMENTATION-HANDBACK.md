# Void Canon — automatic scene artwork

Source version **0.4.4 / code 9**, regular package `org.voidcat.voidcanon`. The user
approved the automatic-artwork plan and requested implementation. The last
published regular APK remains 0.4.3; **no 0.4.4 APK has been distributed**.
The previous release is preserved in [its handback](docs/RELEASE-043-HANDBACK.md).

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

Regular release packaging must run with the original signing identity in the
original distribution workspace, as for 0.4.3. Do not publish this workspace's
fixture-signed APK as a regular update. Keep existing live downloads unchanged
until the signed 0.4.3 → 0.4.4 upgrade is verified. No new separate Test launcher
is requested or published.
