# Void Canon — Freeform Story and compact Play

Version **0.5.0 / build 11**, regular `org.voidcat.voidcanon`, Android 8.0+.
The prior release is preserved in [the 0.4.5 handback](docs/RELEASE-045-HANDBACK.md).

This increment implements the first MLRPE-inspired narrative mode: Classic RP or Freeform Story with per-scene preset, pacing and response-length preferences. It also addresses the request for less Play-screen chrome: five accessible icon controls replace wrapping text actions and source chips; the source/mode selector moves into Play mode. A weighted reading region reserves the input and controls first, then gives dialogue more space, including with the IME visible. The Android regression asserts at least 56 dp of input height with long dialogue.

See the [mode contract](docs/NARRATIVE-MODES.md) for controls, stable IDs, prompt behavior, persistence and boundaries. Settings are reserved in bounded context and inspectable in previews/recorded requests. Classic remains the default; existing transcripts, lore and continuity are not rewritten. Freeform uses the existing selected OpenRouter model and introduces no extra request, fallback or provider configuration.

Room is **5**, with explicit migration 4→5 adding four scene columns and retaining all prior records. Portable backup is **5** and still reads 1–4; invalid/unsupported IDs and legacy manifests with non-default preferences are rejected before replacing work. Device-local keys, image connection and artwork permissions retain their existing meaning. Older app versions cannot read new version-5 backups.

Build with JDK 17/21, SDK/build tools 35 and pinned Gradle 8.11.1:

```sh
./gradlew -PvoidCanonDebugKeystore=<private-original-key-path> assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
```

The package/signing identity must match existing regular installs. Certificate SHA-256: `d2c8a958809475255d9fa24ead371f3225f19e3a5189db3b416217817de5f56a`. Private signing/provider/connection files remain outside Git and artifacts.

Verification: **96 JVM tests**, lint and the signed 0.4.5→0.5.0 upgrade pass. Three Android walkthroughs cover navigation/retention, offline saved presets/cancel/recreation/keyboard, and actual TLS-fixture RP requests with narrative preferences plus artwork descriptions. The editor test caught canceled drafts resurfacing after recreation; the final editor opens from committed settings. Exact completed checks and screenshots are in [0.5.0 release evidence](docs/release-050-evidence/README.md). Android provider tests use local TLS fixtures, not live inference; emulator evidence does not verify physical Galaxy S26 behavior or model adherence.

Stop after review of this increment. AURPG dice/momentum/clocks, campaigns, cross-scene state and ensemble autonomy remain planned. No service/Worker, billing, token, synchronization or account changes are included.
