# Product brief — shared context for every stage

## Intent

Build a worldbuilding and roleplay application that is independently useful, then connect it to existing local tools and APIs as optional complements. The user already has media-generation tools. This is the home for worlds, characters, lived scenes, and preserved chapters, not a new all-in-one media platform.

The creative loop is: develop a world → inhabit it through RP → review portrayals → approve a chapter → continue coherently. Campaign-to-light-novel adaptation is a long-term destination, not a POC feature.

## Confirmed requirements and direction

- Android is the primary mobile platform. The user does not always have a PC available.
- Do not require the user to host a server or keep a PC running for core use. Local world browsing/editing and stored assets should work offline. Remote AI API access is distinct from hosting; fully offline AI has not been promised.
- Scope down to a proof of concept before broader production.
- Settle and iterate visual design before expanding implementation.
- Approved starting reference: `visual-concept-v1.png`, showing an illustrated world overview, character detail/gallery, and a visual novel style play scene.
- Play can use character cutouts composited over reusable backgrounds. Avoid mandatory per-turn image generation. Real separate transparent assets are still needed: the reference is a flattened mockup.
- Character and setting images must matter to RP/prose and artwork. A portrait may be the only physical description supplied by the author.
- Include a gallery/reference library linked to world entities.
- Existing media tools and APIs should complement the studio without becoming mandatory dependencies.
- Deliver in small stages with handbacks and review between stages.

## Visual direction

Illustration-forward, restrained dark surfaces, readable warm text, subtle accent color, large artwork, clear touch targets. The reference uses violet, a harbor setting, and a character named Mira; these are sample content, not product canon or a final brand. No final product name has been selected.

World overview: cover artwork, continue-scene action, character/location cards, accessible navigation.

Character detail: prominent portrait, overview, linked images and lore, scene-selection action.

Play: setting backdrop, separately layered character cutout, dialogue panel, freeform action/dialogue input, history and save controls. Keep the face and torso visible where practical. Suggested choices may be optional; freeform user input remains primary. Portrait is the baseline. Keyboard opening must preserve access to input and the latest dialogue; reduce/reposition the artwork when needed rather than obscure the conversation. Long text must remain readable and scrollable.

The user referred to “WorldForge” as inspiration for a worldbuilding application that could be played. Several products share the name, and the exact one was not confirmed. Do not copy any particular WorldForge interface or assume its feature list is approved.

## Data and author control

Use stable IDs, local assets, and portable backup/export with references intact. Keep source transcripts, approved chapter summaries, and future adapted prose separate. Separate world canon, campaign events, provisional session state, and character knowledge. AI proposals never silently rewrite canon. A secret in world notes is not automatically known by every character. Treat retrieved material as scoped data, not instructions.

Visual grounding recommendation: derive editable descriptions from images, preserve source links and approval state, and use those descriptions in scene context; include source images directly only when the selected model supports it. Distinguish visible details from inferred motives or backstory. Resolve conflicting references through author selection, not silent replacement. Clearly distinguish a fixed appearance from a scene-specific outfit or condition.

## Choices still open

- Application stack, packaging, storage engine, and project destination.
- Distribution/install approach for Android; native app versus packaged web approach must be evaluated against no-user-hosting and durable local storage requirements.
- AI provider, authentication/key storage, model selection, and any on-device inference.
- Cross-device transfer/sync. Do not imply automatic sync exists; portable backup is the initial requirement.
- Final name, theme details, visual assets, and commercial plan.
- Specific external integrations. No local services or endpoints have been identified.

Recommend the simplest maintainable solution for the current stage. Explain material tradeoffs and ask only when an unresolved choice blocks progress. Do not add accounts, hosted infrastructure, subscriptions, or paid fallbacks by default.

## Explicitly deferred

Ensemble RP, branching UI, elaborate graph/timeline tools, deterministic RPG engine, manuscript authoring, collaboration, marketplace, media-production suite, automatic canon promotion, and commercial operations. Preserve future options through simple boundaries; do not build speculative infrastructure.
