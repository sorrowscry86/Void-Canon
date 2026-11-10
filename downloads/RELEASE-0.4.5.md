# Void Canon 0.4.5 — Saved artwork settings and captioned Gallery

[Download the regular APK](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.4.5/downloads/void-canon-0.4.5.apk) · [APK checksum](void-canon-0.4.5.apk.sha256) · [Source ZIP](https://raw.githubusercontent.com/sorrowscry86/Void-Canon/v0.4.5/downloads/void-canon-0.4.5-source.zip) · [Source checksum](void-canon-0.4.5-source.zip.sha256)

Install over your existing **Void Canon** app: package `org.voidcat.voidcanon`, version **0.4.5 / build 10**, Android 8.0+. The original signing identity is retained. Worlds, drafts, transcripts, images, chapters, scene continuity and private provider connections remain on the device. Room stays 4 and portable backup format stays 4.

## Settings that save before setup is complete

Open **Play → Artwork**, or **Settings → Automatic scene artwork**. Set upload permission, visual brief and world style, then choose **Save artwork settings**. These values now persist even when the connection/reference is not yet ready. Generation waits for the missing setup instead of rejecting the whole form. Input/storage errors appear inside the sheet.

The previous implementation validated an image prompt and connection before saving anything; its error snackbar could sit behind the sheet. Permission remains explicit, scene-scoped and initially disabled. World style remains scoped to its world.

## Automatic captioned images in Gallery and Play

Completed scene illustrations save to Gallery automatically, including late images for their original owners. Matching images display in Play with the same caption. Captions use visible scene context, are limited to 240 characters and require no extra model call. They are displayed text, not lettering embedded in the generated image or an approved interpretation of its pixels.

Saved images are exploratory references with stable job/asset IDs and provenance. They do not replace portraits, become identity references, approve visual descriptions, or alter lore/continuity/transcripts. The manual Gallery preview/save flow remains available; manual saves receive a prompt-based caption.

Recovery retries unfinished local Gallery publication without another image request. Duplicate saves keep one asset. Gallery copies survive operational cache eviction and are included in ordinary ZIP backups. Restoring a library clears old image jobs so they cannot republish into the restored library. Local save failures are shown in artwork status and offer **Retry Gallery save**.

## Automatic image descriptions used during roleplay

In **Play → Artwork**, enable **Automatically describe generated images for roleplay** and save. This separately authorizes sending the actual generated PNG to the selected OpenRouter image-capable model. It uses the existing OpenRouter key/consent, respects Free only, and makes one automatic vision attempt per image with at most 1,000 output tokens. Missing setup and failures are visible; roleplay continues, and failed/interrupted requests are not automatically replayed.

The visual-description field populates in Gallery. When ready and enabled, that source-hash-bound description enters the matching illustrated scene's next RP context as temporary visible observations. Captions stay as display text; they are not substituted for actual image understanding. Other scenes, stale images and hidden lore/private notes are excluded. Automatic observations are clearly labeled and do not approve world canon or replace portraits. Author edits are preserved; Gallery's existing review/propose controls provide explicit correction/retry.

## Verification and limits

See [release evidence](../docs/release-045-evidence/README.md) for exact build, unit, upgrade and Android results. Checks cover settings reopened after recreation, captions in both views, duplicate/recovered publication, cache eviction, backup retention and restore fencing. Android responses are local HTTPS fixtures. Physical Galaxy S26 behavior and live Cloudflare quality remain separate checks.

The existing service connection, model, 30 automatic attempts/device/UTC day, 30-second spacing, shared service allowance and no automatic network retry remain. The operational cache stays bounded; automatically saved Gallery images are durable library content. This remains an early debug build.

Tag: `v0.4.5`. The source ZIP records its source revision and excludes signing/provider keys, private service connections and device snapshots. Files use the established Git-hosted download route; no formal GitHub Release object is claimed.
