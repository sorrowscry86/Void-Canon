# Demo artwork provenance

The bundled artwork is replaceable sample content, not product canon. Both images were newly generated with the built-in OpenAI image-generation tool during this task on November 1, 2026 (America/Chicago). They were not extracted from the supplied flattened UI mockup and were not downloaded from a third-party character library.

| Asset | Purpose | Properties |
| --- | --- | --- |
| `app/src/main/assets/demo/harbor.png` | World/location cover and reusable Play backdrop | 1024 × 1536 RGB PNG, no characters |
| `app/src/main/assets/demo/mira.png` | Character portrait and separately composited Play sprite | 1024 × 1536 RGBA PNG with actual transparent pixels |

These are development demo assets. This provenance record does not assert exclusive copyright, commercial clearance, or ownership of the supplied reference. Commercial distribution remains outside Stage 1.

## Generation prompts

Background: standalone portrait game background for The Glass Harbor; empty cobbled/wooden quay, dark harbor buildings, amber lanterns, blue reflective harbor and sailboats, distant hilltop castle, violet dusk. Refined detailed painterly anime editorial illustration, room for a separately composited character; no people, text, UI, or frames. Generated with `transparent_background: false`.

Mira: adult traveler with loosely tied dark brown hair, amber eyes, dark teal hooded coat, cream blouse, leather belts, satchel and brass compass, trousers and boots; relaxed three-quarter full-body stance. Refined painterly anime editorial style with amber lantern lighting and violet twilight. No backdrop, ground plane, text, or UI. Generated with `transparent_background: true`; PNG alpha was checked after generation.

Imported images receive an author-import provenance label. The author chooses their entity and use. Import does not imply a license grant or automatically change prose/lore.

The separate Kuro mascot PNGs requested in chat are delivered outside the application repository. They are not added as extra Stage 1 story characters.
