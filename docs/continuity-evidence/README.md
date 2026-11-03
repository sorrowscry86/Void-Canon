# Scene continuity verification

The final build, **52 JVM tests**, Android lint and instrumentation APK assembly passed. JVM coverage includes actual schema 1→3 and 2→3 database migrations, atomic state/story completion, rejection/failure preservation, context bounds, source references, corrections, scene isolation, and backup validation. See [JVM results](jvm-results.txt).

On the disposable API 29 AOSP software emulator (720 × 1280 at 320 dpi):

- [State-editor test](android-editor.txt): **passed**, `OK (1 test)`, 109.359 seconds standalone. It saves/reopens a correction, confirms its context inclusion, and preserves the transcript. A repeat also passed and produced the [clean state-editor capture](screenshots/15-scene-continuity.png).
- [Full regression walkthrough](android-regression.txt): **passed**, `OK (1 test)`, 223.493 seconds. Navigation/artwork/keyboard/history, descriptions, chapter approval, context, model picker/Keystore, world creation, scene selection and draft recovery remain covered. Wi-Fi and mobile data were disabled.

The [initial obstructed capture](screenshots/99-emulator-systemui-stall.png) records a System UI ANR under software emulation. After dismissing that dialog and disabling animations, the editor rendered cleanly. A chapter-label assertion was stabilized by waiting for its actual observed UI node. These are emulator results, not a physical-device performance claim.

No live OpenRouter requests were made. Structural response checks do not prove a model's semantic extraction quality. This environment's test APK has a different signing certificate from the published 0.4.0 download; it is not an in-place phone update.
