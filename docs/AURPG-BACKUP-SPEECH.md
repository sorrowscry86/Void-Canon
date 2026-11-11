# AURPG toolkit, direct DeepSeek backup and Read Aloud

## Scope

0.6.0 adds the roadmap's first AURPG increment (dice, momentum, progress clocks), a direct DeepSeek text connection with a separate key, and native Android Read Aloud. It retains the compact Play toolbar, existing Freeform/Classic modes, local library, continuity, artwork, image descriptions and chapters. This is a rules-lite author toolkit, not an implementation of a separate unpublished AURPG rulebook or a complete campaign simulator.

## AURPG

Open Play → sliders icon → AURPG → Save AURPG mode & open tools. Dice and counters work offline without an AI account. The app generates each die with SecureRandom; the model never rolls or mutates mechanics. Before sending an action for portrayal, roll/check the applicable mechanics in the tools.

- Roll 1–10 dice with 2–100 sides each. Supply an optional label, modifier from −100 to 100, optional target from 1 to 1000, and explicit momentum spend.
- Total = sum of dice + modifier + spent momentum. Each spent point adds +1 and is deducted atomically. A target check passes when total >= target. A blank target is an unopposed roll. There are no automatic criticals, damage, character stats or loot rules.
- Momentum is a manually adjusted bank from 0 to 10. It is never automatically awarded by the model.
- Create up to 16 named progress clocks, each with 2–12 segments. Explicit +1/−1 actions adjust their fill within bounds; narration cannot advance them.
- Rolls are immutable and have stable IDs, raw dice, modifier, target, spent momentum, total and timestamp. The tool shows the latest 20; backups retain the full journal, up to 1000 rolls per scene. Clock IDs remain stable. Names and checks are author-supplied reference data, not executable instructions.

Mechanics belong to the saved character/location scene pairing. Switching to Classic/Freeform retains dormant mechanics; switching to another scene does not transfer them. There is no cross-scene campaign bank or offscreen scheduler. World lore, character entries and source transcripts are not rewritten by rolls or clock edits.

AURPG RP context reserves the complete current mechanical projection before history/lore: momentum, all clocks and the latest three checks, including IDs and arithmetic. The selected mode/preferences and exact request context are inspectable. Changes participate in the scene fingerprint; UI actions are blocked during text generation and repository completion rejects stale portrayals. Model compliance is still semantic, not guaranteed: inspect and correct prose if it contradicts the recorded outcomes. No new per-turn model call is introduced.

## Direct DeepSeek backup

Open AI settings → DeepSeek direct backup. Enter the separate API key on the phone. Load DeepSeek model IDs to query its current authenticated catalog, or enter a valid text-model ID manually. Discovery sends the key to the fixed DeepSeek API endpoint and no story text. Model IDs are not a hardcoded list; the API's catalog does not provide the app with verified current pricing/context capabilities.

Enable the separate story-sharing/API-charge permission and Use DeepSeek for future text requests, then Save. This explicitly selects DeepSeek for RP and summaries. It never silently retries a failed OpenRouter call or resends a turn. Send explicitly after switching. Turn it off to return to the original OpenRouter configuration. OpenRouter's Free only applies to OpenRouter; it is not a zero-cost guarantee for this separately authorized API account.

The adapter uses DeepSeek's OpenAI-compatible `/v1/chat/completions` and authenticated `/v1/models`. It omits OpenRouter routing/pricing fields and headers and leaves temperature at the direct provider's default. The app bounds story context to 24,000 characters and output to at most 4096 tokens (subject to provider limits); these are application budgets, not an advertised model maximum. Errors are sanitized; there is no automatic retry, redirect or alternate model/provider attempt. Usage and provider/model identity are retained in the request audit.

Direct DeepSeek is text-only in this increment. Manual/automatic image understanding stays on the selected OpenRouter vision model and permissions; generated artwork keeps its existing private connection. Approved visual descriptions and scoped generated-image observations can still inform a DeepSeek text request.

The key is AES-GCM encrypted with a separate Android Keystore alias in `deepseek-private` preferences. OpenRouter's encrypted namespace/key/configuration remain independent. Provider selection, keys, model cache, account consent and voice data are device-local and excluded from ZIP backups. Remove DeepSeek key first deactivates the direct text route. No key is supplied in source/APK/docs; do not paste it into chat or lore.

## Read Aloud

The speaker icon reads the latest visible scene message. Tap its Stop icon to cancel. No automatic playback or cloud voice API is added. Long text is queued in engine-sized chunks without splitting UTF-16 surrogate pairs or dropping text. The app requests an installed non-network voice matching the device language. If no suitable engine/voice is available, it reports setup guidance rather than falling back to a network voice.

Android controls voice installation, language and audio output. Playback uses speech/media audio attributes and transient audio focus, stopping on focus loss, scene/message changes, backgrounding and leaving the controls. Opening the keyboard hides/disposes those controls and stops playback. No microphone permission, audio recording or generated audio file is introduced. Audible quality and installed voices require physical-device verification; an emulator start/stop/error-path check cannot prove S26 sound output.

## Storage and compatibility

Room schema 6 adds only `scenes.aurpgJson` through explicit migration 5→6 with an empty default. The version-1 nested mechanical record is independent of UI controls. Older database migrations remain registered. Existing narrative preferences and every original record/ID are retained.

Portable format 6 includes mechanics, ordinary structured data and referenced images. Versions 1–5 remain importable; legacy manifests cannot smuggle AURPG mode/state into a version that does not support them. Restore validates versions, IDs, ownership, clock ranges, dice ranges, arithmetic, journal limits (including a 10,000-record total for clocks/checks) and archive/file references before replacement. Corrupt results or unsupported versions must leave the current library usable. Earlier apps cannot import a new version-6 archive.

No device-specific local LLM, Samsung Galaxy AI integration, DeX layout or on-device image generation is included. The S26 discussion was informational; those opportunities require separate implementation and device benchmarking.

See [release evidence](release-060-evidence/README.md) for completed checks and limitations.
