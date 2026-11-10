# Automatic artwork verification

Historical source-workspace checks are preserved below. The subsequent [regular 0.4.4 distribution checks](../release-044-evidence/README.md) record the original signing identity, signed 0.4.3 upgrade and current Android rerun.

Source **0.4.4 / code 9**, SDK/build tools 35, JDK 21 and Gradle 8.11.1.
Device checks use an API 29 AOSP software emulator with disposable authored
fixtures. They do not establish physical-phone performance or live Klein quality.

## Build, domain and provider checks

- The [81-test regression run](build-and-unit.log) passed with
  [zero failures/errors](unit-tests.txt).
- After the final dispatch/freshness changes, all [22 focused tests](focused-tests.txt)
  passed; [final build](final-build.log) includes regular/debug and instrumentation
  APK assembly and lint. Coverage includes private-field exclusion, source-quoted
  cues, preserved source messages, reference ownership/hash rules, prompt bounds,
  meaningful-change fingerprints, coalescing, stale claims, restart interruption,
  daily/cooldown enforcement, allowance retention across restore, LRU eviction,
  unchanged Gallery originals and actual schema-3 migration.
- The delayed Android fixture exposed an existing 10-second default HTTP read
  timeout despite a 120-second overall image-call limit. The read limit now matches
  that overall limit. The [five client tests](client-tests.txt) and
  [client/build checks](timeout-client-check.log) passed afterward.
- [Final UI assembly/lint](ui-build.log) passed; [lint reports no issues](lint.txt).

## Android and retained data

[The fixture upgrade](fixture-upgrade.json) moved schema **3 → 4** in place and
compared every value/ID in all nine existing domain tables. Every row was unchanged.
It retained two worlds, four entries, four assets, two scenes, three messages,
one visual description and one chapter. Later fixture reinstall checks are recorded
separately; no library was wiped to obtain this result.

The [initial Android run](initial-android.log) passed the existing manual artwork
walkthrough. Its new concurrency assertion exposed the read-timeout mismatch;
the navigation check was obstructed by a [System UI ANR](initial-system-ui-anr.png).
Closing that system dialog restarted System UI without clearing app data.

The [intermediate run](intermediate-android.log) confirmed independent RP and
explicit retention, then exposed the artwork dialog's lifetime being tied to
a control row hidden by the keyboard. Its state now lives outside that row,
and opening artwork clears the underlying editor's focus. The final test also
edits a visual brief with the keyboard open. Navigation now scrolls to the
fourth Settings entry, and Backups opens expanded with its Close control fixed.

The [automatic artwork and navigation rerun](android-rerun.log) passed:
**OK (2 tests)** in 156.981 seconds. It verifies the corrected flow.
The automatic fixture holds the image response open while a real
UI-triggered RP request completes against another local HTTPS route. It checks
opt-in upload permission, one automatic image, independent RP completion, automatic
display, explicit Gallery retention, source/provenance preservation, a failed
explicit regeneration without retry, unchanged automatic allowance for that manual
request, pause/view toggles and exclusion of cache/jobs/credentials from backups.
Each rerun resets only its operational image fixtures and retains the consumed
device allowance; story/world records remain intact.

The generated-image response is a solid-color **local HTTPS fixture**, not a live
model output. No live AI requests or real provider credentials are used. Image
identity, clothing/hand fidelity and composition still require live visual evaluation.
The unchanged deployed Worker has not received a new phone-to-service verification
from this environment, whose policy blocks its workers.dev hostname.

## Signing and release

[Certificate verification](fixture-signing.txt) distinguishes the emulator signer
from the published regular 0.4.3 signer. The fixture APK retains the regular package
name for emulator upgrade checks, but its certificate cannot update the user's
regular installation. These source-workspace fixture artifacts were not published. The regular 0.4.4
release uses the original distribution identity, as recorded separately above;
no new Test launcher is published.
