# Regular Void Canon 0.4.1 release evidence

The regular APK contains the continuity implementation in the original `org.voidcat.voidcanon` package, version **0.4.1/code 6**, launcher name **Void Canon**, minimum Android API 26. `assembleDebug assembleDebugAndroidTest` succeeded. [APK verification](apk-verification.txt) records matching signing certificates with published 0.4.0 and verifies that the APK contains the continuity implementation and editor.

The earlier source checks passed **52 JVM tests**, Android lint, the API 29 continuity editor and full regression walkthrough; see [source evidence](../continuity-evidence/README.md). Those results are distinct from installation of this regular package.

The regular package upgraded over the published 0.4.0 APK using `adb install -r`, without uninstalling or clearing data. A disposable API 29 fixture contained a renamed character, three transcript messages, an approved chapter, an unsent draft, private notes, artwork references, and device-local model configuration. Room migrated schema 2→3; every original record, ID, and existing field was unchanged, original images matched their stored hashes, and the model-preference file was retained byte-for-byte. The **continuity-editor test passed** (`OK (1 test)`, 78.045 seconds) inside the regular app and saved a new state revision without changing source messages. See [upgrade results](upgrade-result.txt), [editor result](android-editor.txt), and [initial emulator capture](initial-system-ui-stall.png).

The emulator contains only disposable authored fixtures. No live OpenRouter requests, provider keys, real user worlds, database archives, or signing keys are included in this evidence. Emulator results do not establish physical-phone performance.

The initial capture was obstructed by a software-emulator **System UI** ANR. The editor assertions passed and the saved state/data were verified independently. The capture is retained as a limitation, not a claim of physical-device performance.
