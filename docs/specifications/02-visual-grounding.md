# Stage 2 — images become narrative references

## Context to supply

Send with `PRODUCT-BRIEF.md` and the Stage 1 implementation handback. Stage 1 should provide an Android-first local world/character/gallery/play prototype with separate character/background assets, persistent data, and portable backup. Inspect the real implementation before changing it.

The core requirement: a character or setting may be physically described only in an image, yet later RP/prose should use those visible details. This stage prepares trustworthy context; live RP comes in Stage 3.

## Outcome and work

- Associate each asset with a stable entity ID and a simple role: primary appearance, alternate appearance, location reference, or exploratory/noncanon reference.
- Store a source-linked, editable visual description with status (draft/approved), generation provenance where applicable, and the source asset revision or identity. Replacing an image must not leave an old description falsely attached to the new image.
- Allow an author to write/approve a description manually, with no network required.
- Add one optional image-understanding adapter after provider/configuration is resolved. If no provider is configured, manual descriptions remain usable; label automatic interpretation unavailable rather than faking it.
- Ask image interpretation to report observable attributes and uncertainty, not invented personality, motives, ancestry, or history. Provide review/edit before approval.
- Let the user select the character appearance and setting references for a scene. Do not mix every outfit into one description.
- Produce an inspectable scene-context preview from selected approved descriptions. Preserve source links, exclude exploratory/noncanon assets by default, and make missing descriptions visible.
- Keep provider secrets outside client-delivered source and exports; use an approach appropriate to the chosen Android packaging. Disclose what images/text are transmitted and do not silently incur paid fallback calls.

## Acceptance

- Import a portrait with no author-written appearance text. If a vision provider is configured, propose observable details, review them, and see approved details in scene context.
- Repeat for a location image.
- Correct a proposed eye color; subsequent context uses the approved correction.
- Switch to an alternate outfit; scene context uses the chosen outfit without rewriting baseline lore.
- Replace a source image; stale description approval is visibly invalidated or retained only against the old source.
- Unsupported inference such as “this person is evil” does not become an approved fact automatically.
- Manual descriptions and local context preview work offline.
- Export/restore retains sources, descriptions, selections, and approval state without exporting secrets.

## Stop boundary

Do not build artwork generation or a provider marketplace. No automatic canon updates. Report provider decisions, cost/connection limitations, checks performed, and remaining gaps in `IMPLEMENTATION-HANDBACK.md`; stop for review.
