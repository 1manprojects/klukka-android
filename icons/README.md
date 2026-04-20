# Klukka — Icon Export Pack

Monogram variant · glacier palette

## Contents

### `png/` — square PNGs
Use for Windows tiles, Linux `.desktop` icons, web touch icons, social.
Sizes: 16, 24, 32, 48, 64, 128, 256, 512, 1024

### `macos/` — rounded-rect PNGs
Source images for macOS icon set (already masked with 22% corner radius).
Sizes: 16, 32, 64, 128, 256, 512, 1024

### `android/` — adaptive icon layers (432×432)
- `klukka-android-foreground-432.png` — K mark on transparent
- `klukka-android-background-432.png` — solid glacier tile
Drop into `res/mipmap-anydpi-v26/ic_launcher.xml` as foreground/background.

### `klukka.ico`
Multi-size Windows icon (16/24/32/48/64/128/256). Use as `app.ico`.

### `favicon.ico`
Same file, renamed. Drop at web root.

### `klukka.icns`
macOS application icon bundle (16–1024 + retina variants).
Use as `Contents/Resources/AppIcon.icns` in your `.app`.

### `klukka-monogram.svg`
Vector source — scale to any size, embed in README or docs.

## Palette
- Background: `#1B2A2D` (glacier deep)
- Mark: `#E4EEEF` (glacier mist)
- Accent dot: `#8FA6A8` (glacier)

## Notes
- The ICO and ICNS files embed PNG data per entry (modern format, supported by Windows Vista+ and macOS 10.7+).
- For iOS, export a 1024×1024 non-transparent PNG from `macos/klukka-macos-1024.png` (rounding is applied by the OS — use the square `png/klukka-1024.png` instead for a flat square).
