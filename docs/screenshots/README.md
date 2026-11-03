# Stage 1 Android evidence

Captured on an Android 10 / API 29 AOSP emulator at 720 × 1280 pixels, 320 dpi (360 × 640 dp), with airplane mode enabled. These are native app screenshots, not mockups. The character is renamed during the smoke test to demonstrate editing and stable references.

- [World](01-world.png)
- [Character](02-character.png)
- [Gallery](03-gallery.png)
- [Play](04-play.png)
- [Play with the software keyboard](05-play-keyboard.png)
- [Preserved history](06-history.png)

The test decodes the actual bundled image files before capture because Compose idleness does not wait for asynchronous image loading. Physical-device rendering and performance remain unverified.
