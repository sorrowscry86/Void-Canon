# Navigation and regular-update evidence

Source: **0.4.3 / code 8**. Android API 29 software emulator, SDK/build tools 35,
JDK 21, Gradle 8.11.1. Device data consists only of disposable authored fixtures.

- `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` passed;
  see [build log](build.log), [59-test totals](unit-tests.txt), and
  [clean lint result](lint.txt).
- The final test-source build is recorded in
  [instrumentation-build.log](instrumentation-build.log).
- The Android HTTPS artwork test passed after the sheet changes. It checks
  private connection import, consent, one fixture request, preview before save,
  explicit Gallery save, original portrait/transcript preservation and the
  fixed Close button. The [initial combined run](artwork-and-initial-navigation.log)
  also contains a navigation-test selector failure: both the page behind the
  modal and the AI modal supplied a lazy list. That selector was narrowed to
  the list containing “Choose your models.”
- [The navigation rerun](navigation.log) passed, **OK (1 test)** in 128.145 seconds.
  It covers Settings from all four tabs,
  installed version/build identity, child settings return paths including
  Android Back, fixed Close controls after AI/continuity scrolling, context and
  history exits, and a visible Back control with the IME open. It verifies that
  the unsent draft survives and entries, assets, transcripts, state history and
  saved AI configuration are unchanged.

Captures: [Play with the IME and visible Back](18-play-keyboard-exit.png),
[central Settings and installed identity](19-settings.png).
The Settings capture was taken manually after the test, allowing the software
display to finish drawing; the test's immediate screenshot preceded that draw.

The software emulator initially displayed a System UI ANR; dismissing that
system dialog restarted System UI without clearing application data. The
initial fixture install also correctly rejected the workspace's default debug
certificate. Fixture APKs were signed with the emulator's existing debug
identity to retain its library. See [public certificate checks](fixture-apk-verification.txt).

**Neither emulator identity matches the published regular 0.4.1 certificate.**
These fixture APKs are not distributed. A regular 0.4.1 → 0.4.3 signed upgrade
still requires the original distribution key; this evidence does not claim
that upgrade or physical-phone verification. No live AI requests were made.
The image-test response is a solid-color local HTTPS fixture, not evidence of
Cloudflare output quality or the deployed endpoint's delivery.
