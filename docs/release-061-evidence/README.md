# Regular 0.6.1 — release evidence

## Trigger and change

The user reported “The model did not return a valid story/state response” on Freeform Story with direct DeepSeek. No raw provider output or live key was available, so the precise malformed field/wrapper cannot be determined from the screenshot. Code inspection found that roleplay relied only on prompt instructions for JSON, without the provider request's `response_format` option. The patch adds `json_object` for direct DeepSeek roleplay only, records it in request diagnostics and keeps summaries plain text. JSON syntax is insufficient by itself: existing strict protocol/revision/evidence/graph validation remains intact. No automatic repair, retry, provider/model switch or extra call is introduced.

## Build and unit checks

[Build](build.log): JDK 17, SDK/build tools 35, pinned Gradle 8.11.1. `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue --no-daemon '-Dorg.gradle.jvmargs=-Xmx1g -Dfile.encoding=UTF-8'` passes in **6m 24s**, 81 tasks (31 executed, 50 up-to-date). Lint reports no issues. **113 unit tests pass; zero failures/errors/skips** ([totals](unit-summary.tsv)). This is an incremental build; 0.6.0's clean-build evidence remains historical.

The added provider test verifies the JSON request flag, intact returned protocol text, absence of router-only options and exactly one request. The ordinary text test verifies no response-format constraint. Existing malformed/stale response tests prove rejected output preserves drafts, messages and state and retains local diagnostics. Existing mechanics, backup, storage, speech and routing tests remain in the full unit run.

## Signed upgrade

[Certificate](apk-verification.txt), [upgrade log](upgrade.log), [summary](upgrade-summary.json). Published regular 0.6.0 is installed on a disposable authored schema-6 fixture, then the exact outgoing 0.6.1 APK is installed with `adb install -r`. No uninstall/data clear is used. All nine domain tables are byte-value equivalent (8 worlds, 16 entries, 13 assets, 8 scenes, 21 messages, 4 descriptions, 1 chapter, 11 generation records, 9 continuity events). Private image hashes and every captured shared-preference file match. Database snapshots, provider preferences and signing keys stay outside Git/distribution artifacts. Room/backup remain 6.

## Android provider walkthrough

[Final Android walkthrough](deepseek-android.log): **passed, 117.257 seconds** on the exact outgoing signed APK after the upgrade check. The expanded test uses the real ViewModel and local HTTPS fixtures with fake keys; it checks separately encrypted key persistence, consent, catalog GET, AURPG JSON roleplay, a second Freeform JSON roleplay at the next continuity revision, unchanged native mechanics and a plain-text chapter summary with no JSON constraint. Catalog plus three text requests is exactly four calls, with no automatic retry. Switching back leaves OpenRouter settings unchanged.

## Limits

API 29 AOSP x86_64 software emulator in portrait; no physical S26 or live DeepSeek account. Fixtures establish request/state flow, not real-model syntax/semantic compliance, catalog access or pricing. The reported phone response was not inspected. Official DeepSeek documentation could not be fetched (proxy HTTP 403), so current individual-model guarantees were not verified. This request-format patch does not replay or salvage old rejected outputs; send explicitly after upgrading. Full semantic validation can still reject a JSON object with missing/wrong fields, stale revisions or unsupported changes. No UI, dice, speech or artwork implementation changes are included; [0.6.0 checks](../release-060-evidence/README.md) are historical and are not claimed as rerun on this patch.
