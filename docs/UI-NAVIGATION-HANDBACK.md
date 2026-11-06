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

## Distribution gate

**No installable 0.4.3 regular update has been published.** The original
certificate must match the regular 0.4.0/0.4.1 APK:

`d2c8a958809475255d9fa24ead371f3225f19e3a5189db3b416217817de5f56a`

The two local debug keys were checked using their public certificates. Neither
matches. Earlier release notes identify a separate distribution workspace as
the location of the original key. Native Codex history/workspace inspection
has not returned usable results; the connected desktop device is offline.
The local APK is an emulator fixture signed by the workspace debug key and is
not a compatible regular update. No workaround involving uninstall/data loss
or another preview package has been shipped.

After recovering the original key privately, build regular `assembleDebug`
with `-PvoidCanonDebugKeystore=<original-key-path>`, verify the certificate,
then check an in-place 0.4.1 upgrade with retained fixtures. Publish
`downloads/void-canon-0.4.3.apk`, its checksum and release notes, update the
primary download, and provide that regular download to the user. Keep the
private image connection JSON outside Git and supply it separately.

## Verification

Regular/debug and instrumentation builds pass. All 59 JVM tests pass and
Android lint reports no issues. The API 29 navigation rerun passed in 128.145
seconds, including Settings return paths, fixed Close controls, Back with the
IME visible and draft/data preservation. The Android HTTPS artwork fixture
also passed after the sheet changes.

Build and Android evidence is recorded in [navigation evidence](ui-navigation-evidence/README.md).
Physical-phone layout and a phone-to-deployed-Worker image request still need
device verification. The provider fixture does not establish live AI quality.
