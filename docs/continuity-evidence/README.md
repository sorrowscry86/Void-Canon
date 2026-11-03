# Scene continuity verification

The final build, **52 JVM tests**, Android lint and instrumentation APK assembly passed. JVM coverage includes actual schema 1→3 and 2→3 database migrations, atomic state/story completion, rejection/failure preservation, context bounds, source references, corrections, scene isolation, and backup validation. See [JVM results](jvm-results.txt).

On the disposable API 29 AOSP software emulator (720 × 1280 at 320 dpi):

- [State-editor test](android-editor.txt): **passed**, `OK (1 test)`, 109.359 seconds standalone. It saves/reopens a correction, confirms its context inclusion, and preserves the transcript. A repeat also passed and produced the [clean state-editor capture](screenshots/15-scene-continuity.png).
- [Full regression walkthrough](android-regression.txt): **passed**, `OK (1 test)`, 223.493 seconds. Navigation/artwork/keyboard/history, descriptions, chapter approval, context, model picker/Keystore, world creation, scene selection and draft recovery remain covered. Wi-Fi and mobile data were disabled.

The [initial obstructed capture](screenshots/99-emulator-systemui-stall.png) records a System UI ANR under software emulation. After dismissing that dialog and disabling animations, the editor rendered cleanly. A chapter-label assertion was stabilized by waiting for its actual observed UI node. These are emulator results, not a physical-device performance claim.

No live OpenRouter requests were made. Structural response checks do not prove a model's semantic extraction quality. The downloadable preview uses the separate `org.voidcat.voidcanon.preview` package and launcher name **Void Canon Test**, allowing both apps to stay installed. Updating the original package still requires its original signing key.

## Downloadable preview

`assemblePreview lintPreview` passed with no lint issues. APK metadata identifies `org.voidcat.voidcanon.preview`, version `0.4.1-preview`, label **Void Canon Test**, minimum API 26. The APK signature was verified.

The [preview installation smoke check](preview-install.txt) passed on the API 29 emulator: installation beside the original app, launcher startup, initial world rendering, and opening the continuity editor. Both packages remained installed and the original app's database fingerprints were unchanged. The emulator produced another System UI stall during startup; it was dismissed before the check continued. The [preview capture](screenshots/16-preview-install.png) was reviewed after the stall cleared.
