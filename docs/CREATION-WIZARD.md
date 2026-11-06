# Creation wizard — 0.5.0 source increment

World → **Creation wizard · character or world**. Existing drafts show **Resume creation wizard**. Manual creation still works offline. The wizard needs no scene and does not require a new provider: it uses the selected OpenRouter RP/text model, consent, free-price restrictions, and explicit retry behavior already configured on the device.

## Flow

Choose Character or New world. Character targets the world where the draft began, even if the user later opens another world. Enter a starting idea, tone/constraints, and an interpretation: original, follow supplied source canon, or alternate. Unknown details can remain unresolved.

Optionally paste reference text, load one Fandom/Wikia article URL, or select an approved image description. The latter uses reviewed text, not the source image. Direct portrait upload inside the wizard is deferred; the existing Gallery provides image import and interpretation.

Generate sends the visible form plus up to 4,000 characters of the target world's public description for a character. It excludes transcripts, private author notes, unrelated entities, and source images. The model returns a bounded proposal. A world has at most two characters and two locations. Review names, descriptions, lore, and selected starter entries, then Save creation. Manual writing can skip generation entirely. Re-generation is explicit and replaces the working proposal only after a valid response.

## Persistence and checkpoints

The editable wizard draft saves atomically in app-private no-backup storage. Close and resume after restart. There is one working wizard draft at a time. A successful full-library restore clears that draft so it cannot accidentally target a replaced library. Discard only affects the draft.

Accepted creations use existing Room records and backup format 4; there is no schema migration. Saving uses stable draft-based IDs in one transaction, making duplicate acceptance harmless. Approved content includes proposal-model provenance and the reviewed reference excerpt/source metadata. Existing lore, transcripts, scenes, and images are not overwritten. New worlds become selected; characters join their original target world. Add artwork in Gallery and pair a character/location through the existing scene picker.

## Fandom / Wikia

One explicitly selected HTTPS article URL is fetched through that wiki's MediaWiki Action API. Language-prefixed paths and explicit `oldid` revisions are supported. Responses are bounded to 1 MiB and excerpts to 12,000 characters; truncation is disclosed. Returned HTML is converted to plain text and never executed. There is no crawling, authentication, automatic retry, redirect following, image download, or edit back to the wiki. Unsupported or inaccessible pages give a paste-text fallback.

Source links retain the reported revision where available. License metadata is explicitly marked unverified, not fabricated. Attribution/history and applicable terms need checking before redistribution; text and artwork may have different permissions. Imported references are not automatically world canon. Text inside a page is untrusted data, not instructions to the app or its model.

References consulted November 6, 2026:
- https://www.mediawiki.org/wiki/API:Query — general Action API and revision retrieval.
- https://www.mediawiki.org/wiki/API:Parsing_wikitext — selected-page parse API (verify actual wiki support).
- https://dev.fandom.com/wiki/Dev_Wiki:Sandbox/API — Fandom developer reference located through search; direct retrieval was unavailable in this workspace. Live Fandom access is not claimed on the basis of documentation or fixture tests.

## Delivery requirements

Run the project's Android compilation, unit tests, lint, and UI-test compilation. Validate generation parsing, bounds, wiki URL handling/text conversion, source retention, duplicate saves, world targeting, draft recovery, and portable export/restore. Record actual results in the implementation handback.

Regular APK updates must preserve `org.voidcat.voidcanon` and the existing certificate SHA-256 `d2c8a958809475255d9fa24ead371f3225f19e3a5189db3b416217817de5f56a`. The current workspace does not contain the original distribution key. Do not replace the published download with an APK signed by a different development key or ask the user to uninstall their existing app.
