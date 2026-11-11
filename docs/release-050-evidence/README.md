# Regular 0.5.0 — verification

## Build and unit checks

The signed regular debug app and Android test APK build successfully. The final `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue --no-daemon '-Dorg.gradle.jvmargs=-Xmx1g -Dfile.encoding=UTF-8'` completed in **5m 32s**, using a fresh single-use 1 GiB daemon, after the editor and input-space fixes. [Final build](final-build.log), [earlier complete build](build.log), [unit totals](unit-test-summary.tsv), [public signing certificate](apk-verification.txt). There are **96 JVM tests, zero failures/errors/skips**, and lint reports no issues.

Coverage includes all preset/pacing/length combinations in bounded prompts, private-note exclusion, Classic behavior, fingerprint invalidation, scene-specific settings, database reopen, version-5 backup with images, legacy-version-4 defaults and invalid-option restore safety. Existing unsafe ZIP paths, broken references, corrupt/missing images, pricing, continuity and artwork checks remain included. Actual schema-1/2/3/4 database migrations are exercised, not just synthetic current records.

The [initial run](initial-build.log) had one new test failure: it expected restored image private filenames to remain unchanged. Restore intentionally stages under fresh names. The assertion now compares stable IDs, metadata and file hashes while allowing those private filenames to change. The complete rerun is green; no restore behavior was altered to satisfy the assertion.

## Signed upgrade

A disposable API 29 emulator retained the published regular 0.4.5 fixture: 2 worlds, 4 entries, 7 assets, 2 scenes, 9 messages, 2 descriptions, 1 chapter, 4 generation records and 4 continuity events. [Preparation](prepare.txt) and [summary](upgrade-summary.json). After the intermediate editor test, the original authored 0.4.5 disposable database fixture was restored byte-for-byte and regular 0.4.5 reinstalled. The final outgoing signed APK then installed with `adb install -r`; no uninstall or `pm clear` was used. [Final upgrade transcript](upgrade-final.txt). All original domain field values/IDs, image hashes and model-preference bytes matched, with schema 4→5 and four Classic-default columns added to each scene. The outgoing APK matches the final build bytes and original signing identity.

An initial fixed ten-second launch wait took its snapshot before the cold app initialized; that snapshot still showed schema 4. After warm launch, migration completed and the graph comparison passed. The final repeat used schema-readiness polling instead of that fixed wait. This early snapshot is not counted as a successful migration check. Device snapshots and private preferences stay outside Git.

The [initial new editor walkthrough](freeform-initial-editor-failure.log) caught canceled modal draft values resurfacing through Android saved state after Activity recreation, even though persisted settings were correct. The editor now uses transient form state consistent with its transient visibility. Save commits; Close/recreation discards the unsaved form, and a new opening seeds from committed data.

Visual review then caught a different layout defect: giving the dialogue a larger fixed limit could squeeze the input field until its text was clipped. Play now measures the input/controls first and allocates remaining space to a weighted reading region. The Android walkthrough asserts the input remains at least 56 dp tall with long dialogue, both before and while showing the IME. Final screenshots come from that checked layout, after modal/IME frames settle and transient save feedback is cleared.

A subsequent layout rebuild lost its long-lived Gradle daemon during the unit task after the environment slowed; the app source and fixture baseline were retained. The [interrupted run](interrupted-layout-build.log) is not counted as a passing build. The final build uses a fresh single-use daemon with `--no-daemon '-Dorg.gradle.jvmargs=-Xmx1g -Dfile.encoding=UTF-8'`. Only completed checks are reported as passing.

## Android checks

The signed regular package is used; no preview package substitutes for an upgrade.

- [NavigationUiTest](navigation.log): **OK (1 test), 182.428 seconds**. Version identity, Settings children, every tab, icon-based Context/Scene entry, visible modal exits, continuity editor access, keyboard Back and draft retention. [Prepared navigation baseline](before-navigation.txt) includes the newly authored Freeform fixture. After force-stop and a fresh instrumented process, the [post-navigation comparison](after-navigation.txt) confirms the entire schema-5 graph, including saved narrative settings/drafts, plus all images and model-preference bytes remain unchanged. [Restart result](process-restart.txt). The separate signed-upgrade check compares the untouched original schema-4 fixture with the four added defaults.

- [FreeformPlayUiTest](freeform.log): **OK (1 test), 176.857 seconds**. Offline scene setup with reused demo assets, five accessible icons on one row, long dialogue, preset/pacing/length selection, save/reopen, cancel followed by Activity recreation, saved preferences still selected, context application and unsent input with the software keyboard visible. No generation request is made by changing a mode. The canceled-draft regression passes on the final app, and the input remains at least 56 dp tall with long dialogue before/while the IME is visible.

- [AutomaticArtworkUiTest](automatic.log): **OK (1 test), 191.13 seconds**. The UI selects Freeform Story → Character focus → Brisk → Short. Both actual UI-triggered RP HTTP bodies contain the narrative-preference instructions, including the Short word target and character emphasis. RP continues while a fixture image is held open; the generated image is captioned and automatically retained in Gallery. One fixture vision request describes the generated PNG and the second RP body contains that description. Explicit image failure is not retried; baseline entries stay intact and existing privacy/backup checks pass. These are local TLS responses, not live provider/model-quality tests.

Screenshots: [long dialogue](01-compact-play-long-dialogue.png), [mode settings](02-freeform-settings.png), [keyboard](03-freeform-keyboard.png), [saved Freeform Play](04-freeform-saved-play.png), [navigation keyboard](18-play-keyboard-exit.png), [Settings](19-settings.png), [automatic artwork fixture](21-automatic-artwork-fixture.png). These are real emulator captures of labeled synthetic fixtures using the existing generated demo assets; no new production artwork was generated. Layout captures wait for UI frames and clear transient save feedback; the keyboard image shows the typed input, Send and current dialogue together. The settled Play image shows the compact toolbar and expanded reading panel.

## Limits

The emulator is API 29 AOSP x86_64 at 720×1280 portrait with a software keyboard. Offline authoring/layout checks run with airplane mode enabled. Provider-dependent checks use local HTTPS fixtures with fake keys and synthetic replies/images; they do not establish live OpenRouter response quality, current free-model availability or physical Galaxy S26 behavior. No live inference, Worker deployment, service-token rotation, billing or provider change is part of this increment.

This is the first MLRPE-inspired narrative-preference increment. AURPG rules, dice/momentum/clocks, named campaigns, cross-scene continuity and ensemble autonomy remain deferred. The [mode contract](../NARRATIVE-MODES.md) specifies the exact boundary.

## Distribution

The final regular APK is built with the original certificate and the checked layout/source. [Release notes](../../downloads/RELEASE-0.5.0.md) link the APK and source ZIP with SHA-256 files. The source ZIP records its exact source revision and excludes APKs/archives, private connections, signing/provider keys and device snapshots. Delivery uses the existing direct GitHub-file route under tag `v0.5.0`; no formal GitHub Release object is claimed.
