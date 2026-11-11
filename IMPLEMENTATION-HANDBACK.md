# Void Canon — AURPG, direct DeepSeek backup and Read Aloud

Version **0.6.0 / build 13**, regular `org.voidcat.voidcanon`, Android 8.0+.
Previous release: [0.5.0 handback](docs/RELEASE-050-HANDBACK.md).

The user requested a direct DeepSeek connection with its own key, then added the roadmap's first AURPG increment (dice/momentum/clocks) and Read Aloud. The S26 hardware question was informational; no new phone-specific inference or DeX work is included.

See [the feature/storage contract](docs/AURPG-BACKUP-SPEECH.md). AURPG uses local native dice and author-controlled counters, preserved separately from prose/canon. The selected RP provider receives recorded mechanics, never authority to change them. Classic and Freeform remain available; Play keeps a compact icon row, now including Read Aloud, and reserves full input height before allocating dialogue space.

DeepSeek uses its own encrypted device-local key/configuration and an explicit paid/story-sharing permission. Provider switching is manual, text-only and never resends a failed turn automatically. OpenRouter's existing Free only, vision and encrypted key remain independent; artwork keeps its existing service. Model discovery is authenticated and sends no story. Read Aloud uses Android TTS with installed non-network voice selection, explicit Play/Stop and lifecycle cleanup.

Room **6** adds the scene mechanical JSON through migration 5→6. Backup **6** includes the journal/clocks and images; versions 1–5 remain readable. Native arithmetic and graph validation precede restore. Keys/provider selection/voice data are excluded. Existing work and source transcripts are preserved.

Build with JDK 17/21, SDK/build tools 35 and pinned Gradle 8.11.1:

```sh
./gradlew -PvoidCanonDebugKeystore=<private-original-key-path> assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue --no-daemon '-Dorg.gradle.jvmargs=-Xmx1g -Dfile.encoding=UTF-8'
```

Original signing identity/package are retained. Certificate SHA-256: `d2c8a958809475255d9fa24ead371f3225f19e3a5189db3b416217817de5f56a`. Never include signing keys, API keys, private image connections or device snapshots in distribution artifacts.

Exact completed results, screenshots, initial test failures and final limits are recorded in [0.6.0 release evidence](docs/release-060-evidence/README.md). Provider tests use local TLS fixtures, not live account inference. Physical S26 playback, model quality, live DeepSeek account/model access and current pricing require device/account verification. No real DeepSeek key was supplied to this environment; enter it on the phone.

Stop after this increment for review. Full character sheets, damage/initiative, automated clock/momentum changes, named campaigns, cross-scene mechanics, branching and ensemble autonomy remain deferred.

Verification: clean signed build, lint, **112 unit tests**, **six Android walkthroughs**, and the regular 0.5.0→0.6.0 signed upgrade/data-retention check passed.
