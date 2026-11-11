# Void Canon 0.6.0 — AURPG, direct DeepSeek and Read Aloud

Regular `org.voidcat.voidcanon`, **0.6.0 / build 13**, Android 8.0+.

[Download APK](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.6.0/downloads/void-canon-0.6.0.apk) · [APK SHA-256](void-canon-0.6.0.apk.sha256) · [Source ZIP](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.6.0/downloads/void-canon-0.6.0-source.zip) · [Source SHA-256](void-canon-0.6.0-source.zip.sha256)

Install over your current regular app; do not uninstall or clear data. Original package/signing identity and private connections are retained.

- **AURPG:** Play → sliders → AURPG → Save AURPG mode & open tools. Local dice, optional target checks, explicit momentum spend, manual progress clocks and saved roll history. Current mechanics enter RP context separately from prose/canon. This is the roadmap's first author-controlled rules-lite toolkit; full campaigns, character sheets, damage/initiative and autonomous mechanics are deferred.
- **DeepSeek:** AI settings → DeepSeek direct backup. Enter its separate API key on the phone, load model IDs or enter one manually, allow its story-sharing/API charges and select it for future text requests. Save does not send or retry a turn. OpenRouter's Free only remains specific to OpenRouter. Image descriptions stay on OpenRouter and artwork keeps its private connection.
- **Read Aloud:** speaker icon → latest scene message; Stop cancels. Uses Android TTS with an installed non-network voice for the device language. Playback stops on focus loss, message/scene changes, backgrounding and leaving the controls. No cloud voice account or microphone recording.

Room 5→6 adds the scene mechanical record with an empty default. Backup 6 retains rules, history, IDs and referenced images, validates arithmetic/graph/paths before replacement, and reads versions 1–5. Earlier apps cannot read new version-6 exports. Keys, provider selection and voice data remain device-local and outside backups.

[Contract](../docs/AURPG-BACKUP-SPEECH.md) · [Handback](../IMPLEMENTATION-HANDBACK.md) · [Exact verification and screenshots](../docs/release-060-evidence/README.md).

Provider checks use local TLS fixtures; no real DeepSeek key was supplied here. Live account/model access, current prices, model adherence and audible physical-S26 playback require device verification. No phone-specific local LLM, DeX layout or on-device image generation is included; the S26 discussion was informational.

Verification: clean signed build, lint, **112 unit tests**, **six Android walkthroughs**, and the regular 0.5.0→0.6.0 signed upgrade/data-retention check passed.
