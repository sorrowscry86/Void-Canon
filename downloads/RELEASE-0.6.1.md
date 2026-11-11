# Void Canon 0.6.1 — DeepSeek story/state response fix

Regular **0.6.1 / build 14**, `org.voidcat.voidcanon`, Android 8.0+.

[Download APK](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.6.1/downloads/void-canon-0.6.1.apk) · [APK SHA-256](void-canon-0.6.1.apk.sha256) · [Source ZIP](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.6.1/downloads/void-canon-0.6.1-source.zip) · [Source SHA-256](void-canon-0.6.1-source.zip.sha256)

Install over your current regular app; do not uninstall or clear data. Original signing identity and Room/backup 6 are retained.

Direct DeepSeek roleplay now enables JSON response mode rather than relying only on prompt instructions. It applies to Classic, Freeform and AURPG. Chapter summaries stay plain text; OpenRouter and image routes keep their existing behavior. Invalid or stale responses still leave transcripts, drafts and scene state intact. No automatic retry or extra provider call is added.

After updating, send the failed action explicitly again. An earlier failed output is retained in local Context diagnostics. JSON mode cannot guarantee that a model supplies every required field or valid state evidence; remaining invalid replies are rejected safely.

[Handback](../IMPLEMENTATION-HANDBACK.md) · [Verification](../docs/release-061-evidence/README.md). The user's raw provider output and live key were unavailable; local fixtures do not prove live DeepSeek compliance or physical-device behavior.

Build, lint, **113 unit tests**, the expanded Android provider walkthrough and the signed 0.6.0→0.6.1 upgrade/data-retention check passed.
