# Void Canon — provider-independent story compatibility

Version **0.6.2 / build 15**, regular `org.voidcat.voidcanon`, Android 8.0+.
Previous release: [0.6.1 handback](docs/RELEASE-061-HANDBACK.md).

The user still encountered the story/state rejection on DeepSeek and reported similar trouble on OpenRouter. This release separates usable story publication from optional structured continuity updates for both providers, across Classic, Freeform and AURPG. Plain prose and common JSON story wrappers are accepted. A valid complete state protocol still commits its validated state; invalid/missing state metadata leaves every tracked state field, revision, event and native mechanical record untouched while saving the story and action once.

[Response compatibility contract](docs/MODEL-RESPONSE-COMPATIBILITY.md) describes supported wrappers and failure limits. A small persistent Play footer and local request diagnostics record “Story saved; tracked state unchanged.” The next request identifies the state/transcript gap to avoid undoing recent prose to match an older snapshot. No automatic retry, repair request, provider/model switch or extra paid call is added. DeepSeek no longer forces the API's JSON response mode; prompts prefer JSON but explicitly permit prose.

Changed-scene fingerprints still reject stale responses. Empty, truncated or unusable structured output without story text is not published, and transport/output-budget failures remain failures. Model thinking blocks are never treated as narrative. Raw output/usage stay in diagnostics. Existing failed outputs are not automatically replayed; send the action explicitly after installing.

Room and portable backup remain **6**. The completed-generation error field stores the nonfatal warning; no new table, column or archive type is introduced. Backups retain story and warning, while provider keys/settings remain device-local. Original package/signing identity are retained. Install over the regular app without uninstalling or clearing data.

Build with JDK 17/21, SDK/build tools 35 and pinned Gradle 8.11.1:

```sh
./gradlew -PvoidCanonDebugKeystore=<private-original-key-path> assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue --no-daemon '-Dorg.gradle.jvmargs=-Xmx1g -Dfile.encoding=UTF-8'
```

[Verification](docs/release-062-evidence/README.md) records completed build/test/upgrade checks and limits. Providers use local HTTPS fixtures, not a real account. The user's raw phone outputs were not available; no physical Galaxy S26 was attached. No claim is made about a live model's semantic accuracy, current availability or audio quality.

Final build, lint, **121 unit tests**, both-provider Android compatibility (including restart/keyboard/no-popup notice), and signed 0.6.1→0.6.2 upgrade/data-retention verification passed. The earlier direct-provider walkthrough also passed before the display-only notice adjustment; see evidence for exact scope.
