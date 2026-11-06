# Regular-app update and navigation

Source version **0.4.3 / code 8**. The user's regular launcher is **Void Canon**,
package `org.voidcat.voidcanon`. The artwork feature had been delivered to the
separate preview package, explaining why it did not appear in that launcher.
The user has requested future work in the regular app.

## Changes

- Always-visible app header shows the installed app name, version and build.
  Settings is available across World, Gallery, Play, Chapters and entry details.
- Settings links to OpenRouter AI/models, the Cloudflare artwork connection,
  and backup export/restore. Closing a child returns to Settings. Closing AI
  settings without Save discards its uncommitted form edits.
- Every bottom sheet has an explicit Close control outside its scroller:
  Settings, AI/models, artwork connection/generation, world library, context,
  continuity, scene history, and backups.
- Entry details and non-World tabs have a visible Back action that remains
  available while the IME hides the bottom navigation. Leaving Play clears
  focus and hides the keyboard while retaining the unsent draft.
- The artwork connection controls are shared by Settings and Gallery.

No Room schema, backup format, provider protocol, lore, transcript or stable
record IDs changed. No new Worker deployment is needed.

## Regular distribution

The signing gate is resolved in the original distribution workspace. The
regular **0.4.3 / build 8** APK uses the original package and certificate:

`d2c8a958809475255d9fa24ead371f3225f19e3a5189db3b416217817de5f56a`

The source workspace's two fixture keys did not match regular 0.4.1; its earlier
checks are preserved as historical evidence. The distribution workspace used
the original key privately, verified its APK certificate against published
regular 0.4.1, and checked an in-place upgrade with retained library/image/model
fixtures. No user installation was uninstalled or cleared. No new preview is
needed. See [the regular release](../downloads/RELEASE-0.4.3.md) and
[distribution evidence](release-043-evidence/README.md).

Private signing keys and the private image connection JSON remain outside Git
and public downloads. The APK has no bundled service access code; import the
connection through **Settings → Artwork connection** on the device.

## Verification

Regular/debug and instrumentation builds pass. All 59 JVM tests pass and
Android lint reports no issues. The API 29 navigation rerun passed in 128.145
seconds, including Settings return paths, fixed Close controls, Back with the
IME visible and draft/data preservation. The Android HTTPS artwork fixture
also passed after the sheet changes.

The source workspace’s build and Android evidence is recorded in [navigation evidence](ui-navigation-evidence/README.md). Current signed-artifact checks and the upgraded-installation rerun are recorded in [release evidence](release-043-evidence/README.md).
Physical-phone layout and a phone-to-deployed-Worker image request still need
device verification. The provider fixture does not establish live AI quality.
