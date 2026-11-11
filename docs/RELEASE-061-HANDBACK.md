# Void Canon — DeepSeek story/state response fix

Version **0.6.1 / build 14**, regular `org.voidcat.voidcanon`, Android 8.0+.
Previous release: [0.6.0 handback](RELEASE-060-HANDBACK.md).

The user reported a rejected story/state response during Freeform Story with direct DeepSeek. The screenshot establishes rejection, not the exact returned payload. Inspection found that the direct adapter used prompt instructions alone, without the API's JSON response mode. Direct roleplay now sends `response_format: {"type":"json_object"}`. This covers Classic, Freeform and AURPG; actual mode is recorded in generation context. Summaries remain plain text; OpenRouter/vision/artwork requests retain their existing format.

JSON mode constrains syntax, not semantic correctness. Revision, evidence, ownership, completeness and native mechanics validation remain enforced. Malformed/stale responses still cannot publish a partial turn or alter state. Existing failed responses remain in local Context diagnostics; no request is automatically retried, repaired or charged again. The user can explicitly send again after upgrading. No raw phone response or live DeepSeek key was available here.

Room and portable backup remain **6**; no migration or archive change is introduced. AURPG, speech, artwork and private credentials retain the [0.6.0 contract](AURPG-BACKUP-SPEECH.md). Package/signing identity are unchanged; install over the existing regular app without uninstalling or clearing data.

Build with JDK 17/21, SDK/build tools 35 and pinned Gradle 8.11.1:

```sh
./gradlew -PvoidCanonDebugKeystore=<private-original-key-path> assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue --no-daemon '-Dorg.gradle.jvmargs=-Xmx1g -Dfile.encoding=UTF-8'
```

[Verification](release-061-evidence/README.md) records completed checks and their limits. Local TLS fixtures exercise provider routing and publication; they cannot establish live provider compliance or physical S26 behavior. Official DeepSeek documentation was inaccessible from this environment (proxy HTTP 403); no claim is made about current catalog, prices or individual model guarantees.

Build, lint, **113 unit tests**, the expanded Android provider walkthrough and the signed 0.6.0→0.6.1 upgrade/data-retention check passed.
