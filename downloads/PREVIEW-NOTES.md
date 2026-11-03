# Void Canon 0.4.1 preview

This preview adds persistent scene continuity: participant locations, positions, activities, relevant conditions/appearance, explicitly learned facts, held objects, and elapsed time. Live RP returns story and proposed changes in one response. Valid changes and the story save together; malformed or stale results preserve the prior state, history and draft. Current-state context is capped at 4,000 characters, with revision history stored locally.

The optional correction screen is at **Play → Scene → Review scene continuity**. Scene tracking is separate from world lore and approved chapter memory. Campaigns, cross-scene state, MLRPE play presets, and AURPG mechanics are future work.

## Installation and data

- App name: **Void Canon Test**.
- Android package: `org.voidcat.voidcanon.preview`.
- Version: `0.4.1-preview`, code 6; Android 8.0 or newer.
- Installs separately from the original `org.voidcat.voidcanon` app. Both can remain installed.
- Import a portable backup to test existing worlds, then configure OpenRouter again on-device. The preview accepts backup formats 1–4 and exports format 4. Older apps cannot import format 4.
- Signed with this environment's test/debug identity. An in-place update to the original app requires the original signing key, which is not included in Git.

## Validation

The shared implementation passed 52 JVM tests, Android lint, a continuity-editor test and the full API 29 emulator walkthrough. Preview-specific build/lint and installation checks are recorded in the current handback. Physical-device behavior, live OpenRouter response adherence, model quality and account quotas require testing with your own device/account.

This is an early test build. The model can still miss or misinterpret narrative facts; source-linked revisions and author corrections provide recovery. No credentials or user-created worlds are bundled.
