# Regular 0.6.0 — release evidence

## Build and unit checks

The final clean signed build uses JDK 17, SDK/build tools 35 and pinned Gradle 8.11.1. `clean assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue --no-daemon '-Dorg.gradle.jvmargs=-Xmx1g -Dfile.encoding=UTF-8'` completes in **7m 13s**. [Build](build.log), [unit totals](unit-summary.tsv), [public certificate](apk-verification.txt). **112 unit tests pass with zero failures/errors/skips; lint reports no issues.**

Coverage includes direct-provider payload separation, sanitized errors, no retry, blocked free-only/image requests on the direct route, authenticated model discovery without story upload, long speech chunking/surrogate pairs, native dice arithmetic/explicit spending, unopposed checks/no automatic critical, clock/record validation, scene isolation/reopen/rename, backup retention and failed-restore preservation, and mechanics changes invalidating pending RP. Real schema-1/2/3/4/5 database migrations are tested, including non-default Freeform preferences surviving 5→6. Existing pricing, image hashing, archive path/graph, continuity and artwork tests remain included.

## Signed upgrade and data retention

The [final upgrade](upgrade.log) uses the exact outgoing signed APK and a disposable authored regular-0.5.0 fixture. The archived pre-AURPG schema-5 database is restored byte-for-byte, published regular 0.5.0 is reinstalled, and the outgoing 0.6.0 is installed with `adb install -r`. No uninstall or `pm clear` is used. [Summary](upgrade-summary.json).

Baseline: 4 worlds, 8 entries, 10 assets, 4 scenes, 14 messages, 3 descriptions, 1 chapter, 7 generation records and 6 continuity events. After schema-readiness polling, all nine domain tables retain every original field/ID, with only the new empty `aurpgJson` column added. Referenced image hashes and both OpenRouter/DeepSeek preference-file bytes match. Device databases/preferences remain outside Git; fixtures do not represent a physical S26 or a real provider account.

## Android checks

All six final exact-APK walkthroughs pass sequentially after the signed upgrade; see the [run summary](android-summary.log). Transcripts and screenshots are recorded below. The normal signed package is used, not a preview package. Providers use local TLS fixtures with fake keys/models and synthetic prose/images. They establish request/state flow, not live provider quality or model availability.

| Android walkthrough | Result | Time |
| --- | --- | --- |
| [AURPG and speech](rules-speech-final.log) | Passed | 165.551 s |
| [Direct DeepSeek](deepseek-final.log) | Passed | 95.133 s |
| [Navigation and exits](navigation-final.log) | Passed | 167.567 s |
| [Freeform and keyboard](freeform-final.log) | Passed | 140.267 s |
| [Automatic captioned artwork](automatic-final.log) | Passed | 190.062 s |
| [Artwork settings persistence](settings-final.log) | Passed | 114.638 s |

Portrait screenshots: [AURPG clock controls](screenshots/01-aurpg-tools.png), [Read Aloud toolbar](screenshots/02-read-aloud-play.png), [keyboard and retained draft](screenshots/18-play-keyboard-exit.png), [Settings](screenshots/19-settings.png). [Long Freeform dialogue](screenshots/01-compact-play-long-dialogue.png), [Freeform preferences](screenshots/02-freeform-settings.png), [Freeform keyboard](screenshots/03-freeform-keyboard.png), [saved Freeform](screenshots/04-freeform-saved-play.png), [captioned artwork](screenshots/21-automatic-artwork-fixture.png). Screens show synthetic local fixtures.

## Issues caught before release

The [initial AURPG walkthrough](rules-initial-timeout.log) timed out on Compose idling after the mode saved, before a roll was recorded. The library remained usable. A phase-traced [rerun](rules-initial-success.log) completed the local controls, speech handling and recreation in 196.65 seconds. The final run below repeats on the clean outgoing build.

An [early provider test](provider-initial-send-timeout.log) did not publish a turn. The direct route now uses the full bounded application context budget rather than an overly small context allowance; test submission waits for queued draft persistence and invokes the enabled Send action. An [additional provider run](provider-initial-crash.log) exposed a Compose slot-table failure during Activity destruction. Provider forms are now separated into keyed composable branches, and the app/test artifacts are rebuilt cleanly. [The clean provider walkthrough](provider-clean-success.log) passes in 128.901 seconds, including encrypted-key isolation, model GET, one AURPG-grounded RP POST, unchanged native mechanics, switching back and closing the activity. The final run below repeats after the signed upgrade.

Speech initialization was also hardened for engines that report failure synchronously: handling is posted until the constructor assignment is complete, canceled/closed readers cannot auto-start, and failed initialization can be retried explicitly. There is no automatic voice/model fallback.

## Limits

Android checks use an API 29 AOSP x86_64 software emulator, 720×1280 portrait with the software keyboard; local authoring is exercised with airplane mode enabled. No physical Galaxy S26 was connected. TTS start/stop or missing-voice guidance does not prove audible output, voice quality, headphone behavior or that a suitable offline voice is installed on the user's phone. No live DeepSeek key was supplied here; actual account balance, model access/prices, protocol adherence and narrative quality require phone/account confirmation. There are no new Worker deployments, service-token rotations, live model charges or S26-specific inference/DeX integrations in this increment.

This is the first rules-lite toolkit with per-scene state. Full character sheets, damage/initiative, automatic clock/momentum changes, cross-scene campaigns and ensemble autonomy remain deferred. The [feature/storage contract](../AURPG-BACKUP-SPEECH.md) defines the exact rules and limits.
