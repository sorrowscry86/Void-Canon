# Void Canon 0.4.1 — Scene continuity

The regular Void Canon update now includes persistent scene continuity. Participant positions, locations, activities, relevant conditions, learned facts, held objects, and explicit elapsed time remain available to subsequent Live RP turns. The app validates changes and saves accepted story/state updates together. The optional correction editor is at **Play → Scene → Review scene continuity**.

## Install / upgrade

Download `void-canon-0.4.1.apk` and install it over your existing Void Canon app. It uses package `org.voidcat.voidcanon`, app name **Void Canon**, version 0.4.1/code 6, and the signing certificate used by the published 0.4.0 APK. Android 8.0 or newer is required. Do not uninstall or clear data: this update retains the app's local worlds, images, drafts, transcripts, chapters, and AI configuration. An optional portable backup provides an additional copy before upgrading.

This is the regular app update. The older **Void Canon Test** preview is a separate package with separate data. Preview work is not automatically merged into the regular app; portable backups can transfer it, and restoring replaces the destination library. Both 0.4.1 variants support backup format 4; older 0.4.0 cannot read that format. Provider keys/settings are excluded from portable backups.

## Included changes

- Scene-local current state with source-linked revision history, separate from world lore and chapter memory.
- A bounded current-state block in model context, with explicit omissions.
- Atomic story/state publication and failure recovery; malformed or stale replies do not rewrite history or state.
- Author corrections and revision recovery through the optional continuity editor.
- Room schema 3 with explicit 1→2→3 and 2→3 migration paths. Backup formats 1–3 remain importable.

The source passed 52 JVM tests and Android lint; earlier API 29 editor/regression walkthroughs are documented separately. This regular artifact passed build, package/version, signing-continuity, and packaged-feature checks. The regular package upgraded over 0.4.0 on the API 29 emulator with records/images/settings intact, and its continuity editor passed after upgrade. Exact results are recorded in [release evidence](../docs/release-041-evidence/README.md).

This remains an early debug build. Live provider/model adherence and physical-phone behavior require separate testing. Model extraction can miss or misinterpret story details; corrections preserve author control. Campaign-wide state, play-mode presets, AURPG mechanics, and ensemble automation remain future increments.
