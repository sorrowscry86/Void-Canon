# Void Canon — artwork settings and automatic captioned Gallery

Version **0.4.5 / build 10**, regular `org.voidcat.voidcanon`, Android 8.0+.
This maintenance update addresses settings that appear not to save and the request
for generated scene images/captions to populate both Gallery and Play.
The previous release is preserved in [the 0.4.4 handback](RELEASE-044-HANDBACK.md).

## Settings fix

Previously, Save called the prompt planner and required a connected image service
before writing any preferences. Missing references, a missing connection, or a
currently unbuildable prompt rejected the entire form. Its snackbar was behind
the modal, making the failure look like a lost save.

Save now persists the scene permission/visual brief and world style independently
of generation readiness. Missing setup pauses generation and is shown as status;
it does not discard settings. Storage/input errors remain inside the form.
Permission remains explicit and initially off. Fields retain the existing scene
and world scope, and restore still clears device-local artwork preferences.

## Automatic images and captions

Every completed scene illustration is copied into Gallery under its stable job ID,
including late results for their original scene/owner. Matching images appear in
Play with the same caption. Local captions use owner/location and visible action,
appearance/environment; they are limited to 240 characters. No additional model
request is made, and captions are not burned into image pixels.

Gallery labels carry captions through the existing portable asset record. They are
scene-context captions, not approved image interpretations. Assets remain
exploratory: portraits, selected references, approved visual descriptions, lore,
continuity and source transcripts are not silently changed. Manual Gallery
previews retain their review/save flow and receive a prompt-based caption on save.

A completed job remains durable until its Gallery publication can be retried.
Recovery publishes completed cached images idempotently without another image
request. Repository locking checks that the job, scene and owner still exist;
a library restore cannot resurrect old images. Gallery copies survive cache
eviction and enter ordinary backups. Local save failures are visible and retryable.

## Automatic image descriptions for the RP model

The separate **Automatically describe generated images for roleplay** setting is
an explicit per-scene opt-in. It sends the actual generated PNG to the selected
OpenRouter image-capable model, using the existing key/consent and Free only
setting. It makes at most one automatic vision attempt per image, capped at
1,000 output tokens (and the selected model's limit), with no fallback/retry.
Settings can be saved before a key/model is ready; setup/failure status is visible.

Descriptions populate the Gallery visual-description field and the artwork prompt
panel. They remain labeled automatic observations, not author-approved canon.
When enabled, the matching illustrated scene's hash-bound description enters
Context preview and the next RP request as temporary visible observations. It
never becomes an identity reference or world lore. Other scenes and unmatched,
stale, missing or unreviewed manual descriptions are excluded. Authors can review,
correct or approve a description. An in-flight proposal never overwrites their
existing description. A restored library cannot receive a late description.

Vision requests are independent of RP/image jobs. Their stable generation IDs,
source hashes, request text, model/results/errors and usage are recorded in the
existing generation table. Interrupted attempts are not replayed automatically.
Gallery → Describe / review image provides the existing explicit retry/review path.
Background Gallery saves and automatic observations do not invalidate a concurrent
RP response; author edits and selected reference changes still do.

## Compatibility, verification and delivery

Room remains **4**, portable backup format **4**. No migration or data reset is
introduced. Existing image connections and OpenRouter settings remain device-local.
The private image service code is not included in source/APKs/exports. The S26's
replacement connection is maintained separately in its private Notion download.

See [0.4.5 release evidence](release-045-evidence/README.md) for exact build,
unit, upgraded-installation and Android fixture results. New checks cover offline
settings persistence/recreation, visible-context captions, duplicate publication,
restart recovery, cache eviction, portable retention and restore fencing. The
Android image/RP responses use local HTTPS fixtures; they are not live Cloudflare
quality evidence or physical Galaxy S26 verification.

Build with JDK 17/21, SDK/build tools 35 and Gradle 8.11.1:

```sh
./gradlew -PvoidCanonDebugKeystore=<private-original-key-path> assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue
```

Future regular updates must keep the original package and signing identity,
certificate SHA-256 `d2c8a958809475255d9fa24ead371f3225f19e3a5189db3b416217817de5f56a`.
The key stays outside Git. Install the regular update over the current app.
[Release notes](../downloads/RELEASE-0.4.5.md) and [downloads](../downloads/README.md)
include APK/source checksums and source revision information.

## Limits and next step

Image dispatch remains foreground-only, 512-square by default, at most 30 automatic
attempts/device/UTC day and at least 30 seconds apart. The existing shared service
budget and no-automatic-network-retry behavior remain. The operational cache is
bounded to 50 MiB; automatically retained Gallery artwork is durable library data.
Caption correctness is limited to the supplied visible scene context, not a new
inspection of the generated pixels. Physical S26 save/reopen behavior and live
artwork quality still require device confirmation. No Worker, model, billing,
provider-key, or synchronization changes are part of this update.
