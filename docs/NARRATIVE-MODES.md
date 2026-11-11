# Narrative modes — first MLRPE-inspired increment

The authorized increment adds Freeform Story narrative presets and a compact Play toolbar. It builds on one user-controlled role, one focus character and optional environmental narration. It does not implement a complete external MLRPE ruleset, ensemble orchestration or AURPG mechanics.

## Using the controls

Open Play → the sliders icon (Play mode). Icon controls have accessible names and long-press labels: Play mode, Scene context, Scene details, AI settings and Artwork. All five occupy one row with 48 dp targets. The mode/source label remains in the scene header. History and Save remain in the header. The toolbar hides while the keyboard is open; existing Back and input behavior remains.

Choose Classic RP for the existing prompt, or Freeform Story for explicit narrative preferences:

| Setting | Options |
| --- | --- |
| Preset | Collaborative; Character focus; World exploration |
| Pacing | Measured; Balanced; Brisk |
| Response length | Short (60–120 words); Standard (120–220); Rich (220–350) |

Save play settings commits to that scene only. Close discards unsaved changes. Recreating the activity also closes this transient editor; reopening always starts from the committed settings. Choosing Freeform selects Live RP for the sample scene. Scripted demo remains accessible inside Play mode for the harbor sample; it never uses the narrative preferences. Other scenes use Live RP. The sample's temporary demo/live toggle is a UI choice, as before; the narrative settings are portable persisted data.

Classic is the default for existing and newly created scenes. It retains dormant freeform preferences so switching back to Freeform does not erase them. These preferences do not replace author direction, selected lore, user role or known facts.

The dialogue viewport can now occupy up to 54% of the available Play height, or 46% while the keyboard is open, rather than the former 34%, subject to the space remaining after input and controls. A weighted reading region reserves the full input height first, so long replies cannot squeeze text entry. Longer text scrolls within the panel. Short replies use their natural height.

## Context and author control

The Freeform instruction asks for the selected emphasis, pace and narrative length while retaining the continuity JSON protocol. It protects the user's decisions/speech/thoughts, respects supplied knowledge, keeps discoveries provisional, and prohibits invented rules results or independent offscreen simulation. It allocates the complete instruction before lore/history within the existing context budget. Models may still disregard requests; length targets are not a local guarantee.

Context preview shows playMode, narrativePreset, narrativePacing and narrativeLength. Exact request diagnostics record those stable IDs alongside the actual prompt/provider settings. No extra model call is added. Selecting a mode does not generate text, summarize, reinterpret images or change lore. Existing OpenRouter consent, model picker, Free only, output budgets and no automatic retry/fallback remain in force.

Settings changes participate in the scene fingerprint. The UI disables saving during an RP/summary/manual-vision request, and repository completion rejects responses built against changed settings. Original messages, drafts and continuity remain available after rejection. Image/description work retains its existing independent lifecycle.

## Storage and compatibility

Room schema 5 adds four non-null TEXT columns to scenes through MIGRATION_4_5: narrative_mode, narrative_preset, narrative_pacing and narrative_length. Defaults are classic/collaborative/balanced/standard. Kotlin/portable records use a nested NarrativeSettings object with stable string IDs; IDs are independent of translated labels and names. Scene IDs determine ownership, so renaming cannot break a preference link.

Portable backup format 5 includes settings under scenes[].narrative. Versions 1–4 remain readable and acquire Classic defaults. Legacy manifests cannot smuggle non-default narrative settings; unsupported mode/preset/pacing/length IDs fail validation before replacement. The existing image staging, graph/path/hash checks and atomic replacement remain. App versions through 0.4.5 cannot import a version-5 backup; keep an older export if returning to an older installation. No credential or artwork-upload-permission policy changes.

## Next increment

AURPG is displayed as planned only. Dice, momentum, clocks, campaign identity, cross-scene continuity, offscreen scheduling, branches and ensemble autonomy remain deferred. Agree the AURPG resolution contract before implementing its engine; narrative prompts alone do not constitute rules enforcement.

Verification and device limits are recorded in [release evidence](release-050-evidence/README.md).
