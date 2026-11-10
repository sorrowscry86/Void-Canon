# Void Canon 0.4.5 release verification

Regular **Void Canon 0.4.5 / build 10**, `org.voidcat.voidcanon`, Android 8.0/API 26+, target/compile 35. This update fixes settings rejected by image-readiness checks, automatically populates captioned Gallery assets, and adds separately opted-in actual-image descriptions for RP.

## Build and original signing identity

JDK **17.0.17**, pinned Gradle **8.11.1**, Android SDK/build tools **35**. The original private signing key stayed outside Git and was selected through `-PvoidCanonDebugKeystore=<private-original-key-path>`.

[Build](build.log): **BUILD SUCCESSFUL in 4m 50s** for `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue`. [87 unit tests](unit-tests.txt): zero failures/errors. [Lint](lint.txt): no issues found. [APK verification](apk-verification.txt) confirms original package/name, version/build and certificate matching the published regular 0.4.4 APK:

`d2c8a958809475255d9fa24ead371f3225f19e3a5189db3b416217817de5f56a`

The subsequent [instrumentation build](instrumentation-build.log) and [manual-test build](manual-instrumentation-build.log) update test-only synchronization/assertions and pass lint. App behavior/APK bytes are unchanged by those test-only edits. An initial new JUnit setup returned a Job instead of Unit; its setup signature was corrected before the complete green 87-test run. Earlier intermediate logs are not represented as passing final checks.

The unit suite includes actual pixel-file/hash handling, one-time captioned publication after reopen, cache eviction and backup retention, restore fencing, scoped automatic descriptions, preserved author review, and Gallery/automatic-description completion during an in-flight RP turn. Existing backup/path/reference, provider pricing and continuity checks remain included.

## Signed in-place upgrade and data retention

The disposable API 29 AOSP x86_64 emulator retained its authored regular 0.4.4 fixture: one world, two entries, four assets, one scene, five messages, one approved visual description, one chapter, one generation record and two continuity events. No real user's device/library/credentials were read. [Preparation](prepare.txt).

The exact outgoing signed 0.4.5 APK installed with `adb install -r`, without uninstalling or clearing data. Every value/ID in all nine domain tables, image hashes, draft/history and non-secret model-preference bytes matched after launch/force-stop. Schema remains **4**. [Upgrade](upgrade-result.txt) · [summary](upgrade-summary.json).

After navigation/keyboard testing, the complete original library, image hashes and model-preference bytes matched again: [post-navigation comparison](after-navigation.txt). The later settings/artwork fixtures intentionally create another world/scene, synthetic turns, images and descriptions; those additions are separate from the unchanged-upgrade comparison. SQLite snapshots/private preferences remain outside Git.

## Android walkthroughs on the signed app

Each class ran using `adb shell am instrument -w -e class <class> org.voidcat.voidcanon.test/androidx.test.runner.AndroidJUnitRunner` after installing the fresh test APK with `adb install -r -t`.

- [NavigationUiTest](navigation.log): **OK (1 test), 179.008 seconds**. Installed identity, all tabs, Settings children and fixed exits, scrolling, keyboard-visible Back and draft preservation.
- [ArtworkSettingsUiTest](settings.log): **OK (1 test), 131.517 seconds**. A new scene without reference images or an image connection saves upload/description permissions and world style, reopens correctly, saves a visual brief with the IME, and retains fields after Activity/ViewModel recreation. The original world selection is restored afterward.
- [AutomaticArtworkUiTest](automatic.log): **OK (1 test), 201.242 seconds**. RP completes while the image response is held open; the image automatically displays and saves to Gallery with a caption. A separate TLS vision route receives the generated PNG, populates its hash-bound description, and the **next UI-triggered RP HTTP request is asserted to contain that description**. There is one vision call, two RP calls, and one successful image call; an explicit failed image regeneration is not retried. Original portraits/entries remain unchanged. View/pause toggles and backup exclusions are checked; fixture keys/connections and permissions are cleared afterward.
- [ImageGenerationUiTest](manual.log): **OK (1 test), 109.29 seconds**. Manual consent/preview/explicit save, caption population, preserved portrait/transcript and one image request. The [initial run](manual-initial-timeout.log) exceeded its 10-second local-save wait. Afterward the preview was cleared, SQLite integrity was ok and the captioned image was present, confirming persistence. The test now explicitly checks Save is enabled and allows 30 seconds for the emulator's local save; the rerun passes. No application timeout or provider behavior was changed to obtain that result.

Artwork and RP/vision responses are explicitly labeled **local HTTPS fixtures**. The vision fixture's description of a coat/clasp is synthetic; the supplied generated-image fixture is a solid-color PNG. These checks establish upload/description/context flow, not real visual recognition or live image quality.

## Screenshots and limits

[Keyboard-visible Play](18-play-keyboard-exit.png) and [automatic illustrated Play with a caption](21-automatic-artwork-fixture.png) are current signed-app test captures. The manual post-save capture still may show the form before drawing completes; it is not presented as live image output evidence.

The software emulator is 720×1280 portrait with the IME enabled and animations disabled. Its installed records/images are local; providers are local TLS fixtures. No physical Galaxy S26 was connected. S26-specific keyboard/layout behavior, current free-model availability/quotas, actual vision accuracy, and phone-to-deployed-Worker delivery require device confirmation. The environment blocks workers.dev and no live provider inference was performed.

The service/API/model, private connection, billing and shared image allowance were not changed. Automatic descriptions require a **separate explicit per-scene opt-in**, the existing OpenRouter key/consent and an image-capable selected model; they respect Free only and have no automatic retry/fallback. Descriptions stay labeled automatic, scoped to the matching illustrated scene and outside canon/identity promotion. Authors can review/correct/approve them.

## Public artifacts

[Release notes](../../downloads/RELEASE-0.4.5.md), [APK checksum](../../downloads/void-canon-0.4.5.apk.sha256) and [source checksum](../../downloads/void-canon-0.4.5-source.zip.sha256) accompany tag `v0.4.5`. The source ZIP records its source revision and excludes distribution binaries/archives, provider/signing keys, private connections and device snapshots. Delivery uses the existing direct GitHub file route; no formal GitHub Release object is claimed.
