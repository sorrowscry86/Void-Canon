# Stage 3 Android verification

Android 10 / API 29 AOSP emulator, 720 × 1280 pixels at 320 dpi (360 × 640 dp), software acceleration, airplane mode enabled for local workflows. No physical-phone or live-account inference result is implied.

- [Play and its Demo / Live RP controls](screenshots/04-play.png)
- [Play with the keyboard](screenshots/05-play-keyboard.png)
- [Approved visual description](screenshots/07-approved-description.png)
- [Approved chapter with preserved transcript references](screenshots/08-approved-chapter.png)
- [Scene context inspection](screenshots/09-context.png)
- [Free-model picker with labeled test fixtures](screenshots/10-free-model-picker-fixture.png)

The 0.3.1 Android test passed (`OK (1 test)`, 167.81 seconds), including summary setup failure/retry feedback that remains visible after its snackbar is dismissed, plus all of these flows and verified Android Keystore encryption using a non-secret fixture. It checked that the fixture was absent from preference plaintext and exported world data, then removed it. Model-picker fixtures are test-only; production options are loaded from OpenRouter's current catalog. Live provider authentication, image interpretations, model responses, account quotas, and real-time free availability require the user's OpenRouter key and device verification.

Separately, an in-place upgrade from Stage 1 retained the edited character and three transcript messages and migrated Room schema 1 to 2. The signing certificate matches Stage 1. Thirty-three JVM tests cover data/context/migration and the OpenRouter HTTP contract; Android lint reports no issues.
