# Void Canon 0.6.2 — story compatibility for OpenRouter and DeepSeek

Regular **0.6.2 / build 15**, `org.voidcat.voidcanon`, Android 8.0+.

[Download APK](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.6.2/downloads/void-canon-0.6.2.apk) · [APK SHA-256](void-canon-0.6.2.apk.sha256) · [Source ZIP](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.6.2/downloads/void-canon-0.6.2-source.zip) · [Source SHA-256](void-canon-0.6.2-source.zip.sha256)

Install over your current regular app to retain data and private connections; do not uninstall or clear data. Original signing identity and Room/backup 6 are retained.

Both providers can now save usable story prose and common JSON wrappers without requiring valid state metadata. If metadata is invalid/missing, the story and action are saved once, the matching draft is consumed, and tracked state remains unchanged. A small persistent notice identifies that state gap; future context preserves the recent narrative rather than silently undoing it to match older tracked facts. Valid complete state replies still update continuity normally.

Forced DeepSeek JSON mode is removed. Prompts prefer JSON but explicitly permit prose. Empty or unusable output, provider/output-limit errors and stale-scene responses remain failures. No extra model call, repair/resend, automatic retry or provider switch is added. Existing failed outputs are not replayed; send the action explicitly after updating.

[Compatibility contract](../docs/MODEL-RESPONSE-COMPATIBILITY.md) · [Handback](../IMPLEMENTATION-HANDBACK.md) · [Verification](../docs/release-062-evidence/README.md). Provider fixtures do not prove live model quality or physical S26 behavior; the user's raw phone responses were unavailable.

Final build, lint, **121 unit tests**, both-provider Android compatibility (including restart/keyboard/no-popup notice), and signed 0.6.1→0.6.2 upgrade/data-retention verification passed. The earlier direct-provider walkthrough also passed before the display-only notice adjustment; see evidence for exact scope.
