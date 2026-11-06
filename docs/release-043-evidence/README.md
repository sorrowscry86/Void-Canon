# Regular Void Canon 0.4.3 release verification

Artifact: **Void Canon 0.4.3 / build 8**, package `org.voidcat.voidcanon`, minimum Android 8.0/API 26, target/compile 35. Implementation: PR #3, tested head `da0a825ef02fe02bd2afe6ba463762f7dd003a2c`, merged as `e44249df74dc0b28fd3ab9ded73795d1afa0d542`. The merge has the same complete source tree as that tested head; subsequent release commits add documentation and distribution artifacts only.

## Build and signing

JDK **17.0.17**, Gradle **8.11.1**, Android platform/build tools **35**, API 29 AOSP x86_64 software emulator, 720×1280 portrait. The original distribution key remained outside the repository and was supplied through the existing `voidCanonDebugKeystore` Gradle property:

```sh
./gradlew -PvoidCanonDebugKeystore=<private-original-key-path> assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
```

[Build log](build.log): **BUILD SUCCESSFUL in 4m 3s**. [59 JVM tests](unit-tests.txt): zero failures/errors. [Lint](lint.txt): no issues found. [APK verification](apk-verification.txt) confirms app name, package, version/build and original signing certificate matching the published regular 0.4.1 APK:

`d2c8a958809475255d9fa24ead371f3225f19e3a5189db3b416217817de5f56a`

The APK in `downloads/void-canon-0.4.3.apk` is the same artifact installed below. Its checksum is published beside it. No new preview was produced. Signing keys, provider keys and private image-service connections are excluded from all public artifacts.

## In-place upgrade and process restart

A disposable emulator first installed the published regular `downloads/void-canon-0.4.1.apk`. The authored fixture retained from the earlier regular-release checks contained one world, two entries (including a renamed character), two image assets, one scene, three transcript messages, one approved chapter and one continuity event/revision. It also contained a draft, known facts, private author notes and non-secret model-preference fixtures. Visual-description/generation tables were empty. The fixture is synthetic; no user device data was read or published.

`adb install -r` installed the new APK **without uninstalling or clearing data**. The application was launched, force-stopped and its Room records compared. All nine domain tables and schema version 3 matched exactly: IDs, linked records, draft, transcript, chapter, private fields and saved continuity revisions. Each referenced private image matched its stored content hash. Model-preference XML bytes were identical. [Upgrade result](upgrade-result.txt).

After the navigation/keyboard exercise, a second force-stop/capture compared the entire fixture and model settings again: **unchanged**, including the original draft. [Post-navigation comparison](after-navigation.txt). Database snapshots, private preference files and the signing key remain outside Git. Room schema 3 and portable backup format 4 are unchanged.

## Android checks on the upgraded app

The freshly built test APK was installed with `adb install -r -t`, then each class ran with:

```sh
adb shell am instrument -w -e class org.voidcat.voidcanon.NavigationUiTest org.voidcat.voidcanon.test/androidx.test.runner.AndroidJUnitRunner
adb shell am instrument -w -e class org.voidcat.voidcanon.ImageGenerationUiTest org.voidcat.voidcanon.test/androidx.test.runner.AndroidJUnitRunner
```

- [Navigation](navigation.log): **OK (1 test), 176.421 seconds**. Settings from all four tabs, installed identity, child settings return paths, AI/continuity Close after scrolling, context/history exits, and Back with the IME visible. Returning to Play retains the unsent draft; the test restores the original draft. Entry/image/transcript/continuity records and model configuration remain unchanged.
- [Artwork](artwork.log): **OK (1 test), 78.635 seconds**. Imports a fixture-only connection, requires consent before enabling Generate, makes one local HTTPS prompt-only request, previews before save, and explicitly saves one exploratory image while retaining the original portrait and transcript. The fixture is a solid-color PNG, not generated Cloudflare artwork. It intentionally adds one asset after the complete pre/post-navigation equality check. Its local fixture connection is removed in test cleanup.

[Play with keyboard and visible Back](18-play-keyboard-exit.png) shows accessible input and recent dialogue. [Settings/version](19-settings.png) was captured manually during the test after drawing finished. The test's immediate Settings capture preceded the modal draw; it is retained as [Play with the restored draft](20-play-retained-draft.png), not presented as a Settings screenshot. [Artwork fixture capture](17-image-generation-fixture.png) was taken immediately after Save and Close; drawing still shows the generation form, so it is not presented as a saved-image preview. The automated record checks establish the fixture save.

## Environment and limits

The first cold boot briefly disconnected ADB during network setup; setup was retried after boot without altering application data. The software emulator also displayed a [System UI ANR](initial-system-ui-stall.png), whose native **Close app** action restarted System UI. Later navigation captures were inspected and are unobstructed. A warm-launch `am start -W` reported a display timeout, but the following artwork instrumentation completed successfully. No Void Canon crash is inferred from those emulator/system symptoms.

Airplane mode was enabled and Wi-Fi/cellular disabled for the upgrade/navigation checks. Installed artwork, editing/navigation, history and settings checks used local data. The artwork fixture used loopback HTTPS, not external networking. The repository tests retain backup validation, missing/corrupt asset and reference checks; this release does not introduce a new backup format or claim a new full Android backup/restore walkthrough.

No physical phone was connected. Physical-phone layout/performance, current OpenRouter quotas/model behavior, private development-connection import on the phone and phone → deployed Worker → AI delivery remain separate checks. This environment blocks the workers.dev hostname; no live artwork roundtrip or quality result is claimed. Earlier provider/service evidence remains in the artwork/navigation handbacks.

## Published files

[Release notes](../../downloads/RELEASE-0.4.3.md), [APK checksum](../../downloads/void-canon-0.4.3.apk.sha256), and [source checksum](../../downloads/void-canon-0.4.3-source.zip.sha256) accompany the direct GitHub files under tag `v0.4.3`. The source ZIP records its source revision and omits distribution binaries/archives, credentials and device snapshots. Publication uses the repository's existing Git-hosted download route; no formal GitHub Release object is claimed.
