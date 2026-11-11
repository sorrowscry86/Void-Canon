# Void Canon 0.5.0 — Freeform Story and compact Play

Regular **Void Canon**, package `org.voidcat.voidcanon`, version **0.5.0 / build 11**, Android 8.0+.

[Download APK](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.5.0/downloads/void-canon-0.5.0.apk) · [APK SHA-256](void-canon-0.5.0.apk.sha256) · [Source ZIP](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.5.0/downloads/void-canon-0.5.0-source.zip) · [Source SHA-256](void-canon-0.5.0-source.zip.sha256)

## Changes

Open **Play → sliders icon (Play mode)**. Classic RP retains the existing style; Freeform Story adds MLRPE-inspired Collaborative, Character focus and World exploration presets, pacing, and Short/Standard/Rich reply-length targets. Save applies the preferences to that scene. Context previews and recorded requests expose them; original messages, lore and continuity remain intact.

Play now has one row of icons for Mode, Context, Scene, AI and Artwork, with accessible names and long-press labels. Demo/Live selection moves into Play mode for the sample scene. The response can occupy more of the screen and still scrolls, while input keeps its full height. The icon row hides with the keyboard; input and Back remain accessible.

The existing selected OpenRouter model, consent, Free only and no automatic retries/fallback remain. Selecting a mode makes no provider request. Narrative length and portrayal remain dependent on the chosen model. This is the first narrative-preference increment, not a complete external MLRPE ruleset. AURPG dice, momentum and clocks, ensemble autonomy and cross-scene campaigns remain planned.

## Install and storage

Install over your current regular app; do not uninstall or clear data. The original signing certificate is retained. Existing private image connections and AI settings stay on the device. The private connection file is not part of this release.

Room migrates 4→5 by adding four scene preference columns with Classic defaults. Portable backups are version 5 and retain preferences/images/references; backups 1–4 remain readable. Invalid options and malformed archives fail before replacing work. Earlier app versions cannot read new version-5 backups.

Build, lint, **96 unit tests**, the signed upgrade and **three Android walkthroughs** pass. See the [mode contract](../docs/NARRATIVE-MODES.md), [implementation handback](../IMPLEMENTATION-HANDBACK.md) and [exact verification](../docs/release-050-evidence/README.md). Provider checks use local TLS fixtures. Physical Galaxy S26 behavior and live model adherence require device confirmation.
