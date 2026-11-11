# Scene continuity — first increment

**Current compatibility update (0.6.2):** [Story response handling](MODEL-RESPONSE-COMPATIBILITY.md) supersedes the original all-or-nothing publication rule below. Usable prose can be saved without state updates; state changes retain the full validation contract. This document records the first increment.

The user authorized state tracking before adding MLRPE-inspired play presets or AURPG mechanics. This increment adds a scene-local continuity ledger to the existing 0.4.0 app. It does not create campaigns, simulate offscreen actors, introduce dice rules, or change world canon.

## Contract

- Track the player and focus character, optionally up to eight participants total, with stable character IDs. Track location, present/offscreen status, position, activity, conditions, appearance changes, and explicitly learned facts. Unknown details remain blank. Up to sixteen objects have stable IDs, location, holder, and optional occupied hands.
- Time consists of an optional author-entered anchor and elapsed seconds since tracking began. The app accumulates explicit per-turn elapsed time; it does not infer a fictional calendar or silently advance offscreen actors.
- Current state is provisional scene observation. Character lore, private notes, approved chapter memory, and original transcripts retain their existing meanings. Initial state uses the selected scene's player, character and location; earlier transcripts are not retroactively extracted.
- `Play → Scene → Review scene continuity` is an optional author correction surface. It can add existing world characters and objects, correct facts, and use the snapshot before the latest update. A correction creates another revision; it never rewrites a source message.

## Context

`Continuity.project` reserves at most 4,000 characters for complete current-state records. The player and focus character are always included. Other present participants are selected before objects; carried objects are prioritized. Offscreen actors other than the focus/player, offscreen objects, and records that do not fit are omitted with diagnostics. No state history is sent. Additional characters receive state and identity here, not full ensemble orchestration or independent private model calls.

This state block and its location identity map are allocated before history and lore, within the existing 24,000-character total context ceiling. If core state plus instructions/input cannot fit, generation fails locally rather than clipping core facts. Character counts approximate tokens. Author-only fields remain excluded. State fields are model-visible reference material, not a place to put author secrets.

The current location map names the scene and supplied participants' locations. This increment does not automatically select a new backdrop, navigate scenes, or retrieve all locations in a large world when travel is mentioned.

## Generation and publication

Live RP requests ask for protocol-1 JSON containing a narrative, base revision, elapsed time, and changed actor/object records. The request audit retains the protocol marker and exact prompt. No provider-specific assistant prefill or JSON-schema capability is required; models must follow the text contract.

A change must quote a nonempty exact passage from the current user input or completed narrative. Changed records must explicitly include every field so omitted keys cannot silently clear conditions or possessions. The app validates protocol/base revision, lengths, IDs, world ownership, duplicate updates, holders, co-location, hand occupancy and the next core context projection. Omitted existing records cannot be modified by the model. New objects need fresh IDs; additional actors are introduced through the correction editor.

A successful completion atomically commits one user/reply pair, the new current snapshot, one before/after event, and the completed generation record. Only narrative text enters the transcript; the raw response and usage remain in request diagnostics. Duplicate completions do nothing. Corrections invalidate pending fingerprints. Completion targets its recorded source scene independently of current UI selection.

Malformed or invalid responses retain diagnostics but publish neither the narrative nor state. Cancellation/failure preserves the draft and previous history/state. No automatic retry or paid fallback is added. RP output permits up to 4,096 tokens, reduced to model/context limits; at least 1,024 output tokens must be available. Image requests stay at 1,000 and chapter-summary behavior remains bounded as before. One request per RP turn is used.

## Persistence and limitations

Room schema 3 adds `scenes.continuityJson` (blank by default) and `continuity_events`. Explicit migration 2→3 supports the existing 1→2 path. Backup format 4 includes state and events; formats 1–3 remain importable and must not smuggle new state into a legacy manifest. Restore validates ownership, revision chains, source references, and agreement between the latest event and current state before replacement.

Validation is structural. An exact quote establishes traceability, not semantic entailment: the model can still miss a detail, misread a failed attempt, invent an unsupported consequence, or misinterpret a time expression. Corrections and source history are deliberate recovery mechanisms. The app does not read private reasoning, infer motivations, or guarantee NPC knowledge isolation within a shared model context. Scripted Demo remains the old fixed exchange; automatic continuity extraction is for Live RP only.

Named campaigns, cross-scene continuity, offscreen event scheduling, relationships, player presets, and AURPG mechanics remain future increments.
