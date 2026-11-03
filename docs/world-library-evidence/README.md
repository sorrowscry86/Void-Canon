# Personal world library — Android evidence (0.4.0)

Android 10/API 29 AOSP emulator, 720 × 1280 pixels at 320 dpi, software acceleration. Airplane mode enabled throughout the local walkthrough. These captures use authored test data; the Moon Garden/Kuro entries and model-catalog fixtures are not seeded into production installations.

The walkthrough retains the earlier navigation, artwork decode, keyboard/history, visual-description approval, chapter approval, context, free-model controls, and real Android Keystore fixture checks. It also creates a blank world through the UI, checks empty Gallery/Play states, creates a character/location, starts the selected scene, types a draft, switches to Glass Harbor and back, and asserts history/draft/context isolation.

- [New blank world](screenshots/11-own-blank-world.png)
- [Created people and places](screenshots/12-own-people-and-places.png)
- [Author-owned scene](screenshots/13-own-scene.png)
- [World library and switching](screenshots/14-world-library.png)
- [Keyboard-visible Play](screenshots/05-play-keyboard.png)

Result: **passed**, `OK (1 test)`, 238.458 seconds.

Separately, an in-place update from 0.3.1 to 0.4.0 retained every original database row/ID and Room version 2. APK signing continuity was verified. The 40-test JVM suite covers prior storage/context/provider behavior and the new world/scene/archive ownership cases. Android lint reports no issues. No real OpenRouter key or live-provider request is involved in these automated checks; physical-phone and current provider/account behavior require separate verification.
