# Stage 3 — one complete playable loop

## Context to supply

Send with `PRODUCT-BRIEF.md` and the latest implementation handback. Earlier stages should provide the Android local visual app, gallery, source-linked approved visual descriptions, and inspectable scene context. The goal is freeform RP presented as a visual novel, not a scripted choice-only game. Remote AI may use a direct API without a user-hosted server; fully offline inference is not a prerequisite or promise.

## Outcome

Choose a character and location → play a scene informed by their lore and selected visual references → approve a chapter → restart and continue coherently.

## Work

- Connect one configurable model adapter. Settle provider and credentials based on the real project; do not hard-code an assumed service. Keep offline browsing/editing usable when generation is unavailable.
- Assemble bounded context from author direction, selected lore, approved visual descriptions, character knowledge, campaign state, approved chapter memory, and recent messages. Record what was actually supplied, model/settings when available, and omitted/truncated material.
- Support one user-controlled role and one AI-controlled character, with optional narration in the same response. Do not dictate the user's character's actions or internal thoughts without permission.
- Keep scene prose informed by selected visual details; send source images only if supported and explicitly configured. Reuse backgrounds/cutouts without automatic image-generation jobs.
- Distinguish narrator information from what the character knows. A basic explicit author-only/character-known distinction is sufficient; do not build a full knowledge graph.
- Persist the source transcript. Allow a proposed chapter summary to be edited, approved, saved, and used for subsequent context. Preserve source message references. Editing the summary never rewrites the original messages.
- Store provisional session state separately from approved chapter memory. Model inventions remain provisional; world-lore changes require explicit review.
- Provide cancel, clear generation/error status, bounded retries, and protection against duplicate turns. No silent paid fallback. Retain the latest unsent text where practical.

## Acceptance

- With physical appearance provided only through a portrait and its approved description, RP uses visible features accurately; likewise for the selected setting.
- A deliberately author-only secret is excluded from the character's context. Identify the limits of model compliance; do not claim deterministic secrecy merely because a prompt requests it.
- User types a freeform action, receives a response, and can inspect its supplied context.
- Save a chapter, revise its summary, and confirm the source transcript is unchanged.
- Restart and continue using approved chapter memory and the preserved scene state.
- A generation failure or cancellation does not erase history or append a duplicate completed turn.
- Disconnect networking: existing worlds, art, transcripts, and chapters remain accessible; generation clearly reports unavailable.
- Backup/restore includes all approved memory and source references, excluding credentials.

## Stop boundary

No ensemble autonomy, branching UI, manuscript generation, advanced rules engine, or external tool actions. Provide Android evidence and honest test limitations. Update `IMPLEMENTATION-HANDBACK.md` and stop for user evaluation of the complete POC.
