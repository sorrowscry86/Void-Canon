# Regular Void Canon 0.4.4 release verification

**Void Canon 0.4.4 / build 9**, `org.voidcat.voidcanon`, Android 8.0/API 26 minimum, target/compile 35. Implementation: [PR #5](https://github.com/sorrowscry86/Void-Canon/pull/5), head `f97cbae8ccaa49c11e1b34ead39f27f07cf277d3`. Merged as `8c40e21726c07d8018f17680fcf339e671587404`; its complete source tree matches the tested head. Subsequent release commits add documentation and distribution artifacts.

## Build and signing

Distribution workspace: JDK **17.0.17**, Gradle **8.11.1**, SDK/build tools **35**. The original key stays outside Git and was selected through the existing local signing property:

```sh
./gradlew -PvoidCanonDebugKeystore=<private-original-key-path> assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
```

[Build](build.log): **BUILD SUCCESSFUL in 6m 59s**. [Full unit suite](unit-tests.txt): **81 tests, zero failures/errors** after all PR fixes, including the aligned image read/overall timeout. [Lint](lint.txt): no issues found. The initial restricted-socket build attempt was rerun with network access for Gradle's local sockets; it did not require source changes.

[APK verification](apk-verification.txt) confirms package, launcher, version/build and signing certificate matching the published regular 0.4.3 APK:

`d2c8a958809475255d9fa24ead371f3225f19e3a5189db3b416217817de5f56a`

The exact outgoing APK in `downloads/void-canon-0.4.4.apk` was used for the upgrade below. Its SHA-256 is published beside it. No separate Test launcher was built for distribution. No signing key, provider key or private service connection is included.

## In-place 0.4.3 upgrade and restart

API 29 AOSP x86_64 software emulator, **720×1280 portrait**. It installed the published regular 0.4.3 APK and reopened an authored fixture before upgrading: one world, two entries (including a renamed character), two private image assets, one scene, three messages, one approved/hash-bound visual description, one approved chapter and one continuity event/revision. It also contained a draft, known facts, private author notes and non-secret model preferences. The generations table was empty. This is synthetic fixture content; no user's device data was read or published. [Old-app preparation](prepare.txt).

`adb install -r` upgraded to the signed regular 0.4.4 APK without uninstalling or clearing data. After launch, migration completion, force-stop and database capture:

- **All values and IDs in the nine narrative tables matched exactly.**
- Room schema migrated **3→4**, adding the three operational illustration tables. SQLite integrity check: **ok**.
- Every private image matched its recorded content hash.
- The non-secret model-preference XML bytes matched exactly.

[Upgrade result](upgrade-result.txt) · [migration summary](migration-summary.json).

The navigation test then exercised the same upgraded fixture. A second force-stop/capture verified the entire narrative graph and model-preference bytes again, including the original restored draft: [unchanged](after-navigation.txt). This comparison precedes the artwork fixtures, which intentionally append synthetic RP messages and exploratory assets. Private SQLite/preference files remain outside Git.

## Android checks on the signed installation

The fresh test APK was installed using `adb install -r -t`. Each class ran through `org.voidcat.voidcanon.test/androidx.test.runner.AndroidJUnitRunner` with `am instrument -w -e class <class>`:

- **NavigationUiTest:** [OK (1 test), 196.749 seconds](navigation.log). Settings from all tabs, version/build, child return paths, Close outside scrollers, context/history/continuity exits, keyboard-visible Back and unsent-draft preservation. The full fixture comparison afterward passed.
- **AutomaticArtworkUiTest:** [OK (1 test), 176.444 seconds](automatic-artwork.log). Per-scene permission and brief editing with the IME; an image response held open while UI-triggered RP completes over another local TLS route; automatic display; explicit exploratory Gallery retention; preserved original portrait/entries; prompt/provenance inspection; failed explicit regeneration without retry; unchanged automatic allowance for that manual retry; pause/view toggles; backup exclusion of image operations/cache/credentials. The test restores model configuration and removes fixture credentials/permissions in cleanup. It intentionally adds a synthetic RP turn and retained image.
- **ImageGenerationUiTest:** [OK (1 test), 117.73 seconds](manual-artwork.log). Existing manual Gallery connection/consent/preview/save flow, one local TLS request, no Gallery asset before explicit save, exploratory result and unchanged portrait/transcript. It intentionally adds another fixture image and clears its local connection afterward.

Artwork responses are solid-color **local HTTPS fixtures**, not live Cloudflare outputs. The RP response is also a labeled local fixture; there is no live provider inference or real account credential in these checks. See the screenshot descriptions below for which draw state each capture actually shows.

## Screenshots

- [Play with the keyboard and visible Back](18-play-keyboard-exit.png): accessible draft input and recent dialogue during the navigation test.
- [Layered Play with the restored original draft](20-play-retained-draft.png): the test's immediate Settings screenshot preceded modal drawing, so it is presented as the actual Play frame it shows.
- [Automatic illustration fixture](21-automatic-artwork-fixture.png): a solid-purple local response in Play with a labeled synthetic RP turn; it demonstrates display/composition, not AI image quality.
- [Manual artwork fixture capture](17-image-generation-fixture.png): the immediate post-save capture still shows the generation form before Close finishes drawing; it is not presented as a saved-image preview or live output-quality sample.

## Emulator environment and limits

Cold boot took about 392 seconds while the build was running. Early ADB disconnects and system network-service stalls occurred before app testing. The emulator displayed a [System UI ANR](initial-system-ui-stall.png); its native Close app action restarted System UI without clearing Void Canon data. Preparatory commands were bounded/retried after the system recovered. Later test captures were inspected separately; the system symptom is not reported as a Void Canon crash.

Airplane mode and disabled Wi-Fi/mobile-data settings were applied for local upgrade/navigation checks. Installed records/images remained accessible without a provider connection. Artwork/RP fixtures use loopback TLS only. The existing repository tests include backup/reference/hash/path validation, operational-state exclusion, allowance retention, restart recovery, coalescing and migration; this release does not claim a new full Android backup/restore walkthrough.

No physical phone was connected. Phone layout/performance, existing private service-connection use, current OpenRouter behavior, phone → deployed Worker delivery, and Klein identity/outfit/hand fidelity remain separate checks. The environment policy excludes the workers.dev hostname. The Worker API/model, shared 5,000-neuron budget, deployment and billing settings were unchanged.

## Publication

[Release notes](../../downloads/RELEASE-0.4.4.md), [APK checksum](../../downloads/void-canon-0.4.4.apk.sha256) and [source checksum](../../downloads/void-canon-0.4.4-source.zip.sha256) accompany tag `v0.4.4`. The source ZIP records its source revision, includes source/docs/verification evidence and excludes distribution binaries/archives, private keys/connections and device data. Publication uses the repository's existing direct GitHub download route; no formal GitHub Release object is claimed. Earlier downloads remain available.
