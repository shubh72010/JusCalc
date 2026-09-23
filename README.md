<div align="center">
  <img src="https://github.com/shubh72010/JusCalc/raw/main/app/src/main/ic_launcher-playstore.png" width="160" alt="JusCalc icon">
  <h1>JusCalc.</h1>
  <p><strong>A calculator with no buttons. Every input is a drag.</strong></p>

[![Release](https://img.shields.io/github/v/release/shubh72010/JusCalc)](https://github.com/shubh72010/JusCalc/releases)
[![CI](https://github.com/shubh72010/JusCalc/actions/workflows/release.yml/badge.svg)](https://github.com/shubh72010/JusCalc/actions)
[![Downloads](https://img.shields.io/github/downloads/shubh72010/JusCalc/total)](https://github.com/shubh72010/JusCalc/releases)
[![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-brightgreen)](https://github.com/shubh72010/JusCalc/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF)](https://kotlinlang.org)

### [⬇ Download the latest APK](https://github.com/shubh72010/JusCalc/releases/latest)

</div>

## TL;DR

**The problem:** phone calculators are grids of tiny buttons designed for a mouse finger. One-handed, on the move, you mistype.

**The solution:** JusCalc replaces buttons with drag strips. Numbers live on two rotary dials — flick and release on your digit. Selections chase your finger, detent haptics tick, springs settle. No aiming, no mistypes.

### Why JusCalc?

| | What it does |
|---|---|
| **No buttons** | Every input is a drag or tap — usable one-handed, eyes half-open |
| **Two ways to count** | Rotary dials, or Number-dots mode: both bars become one 1–10 counter |
| **Speaks results** | `55000` → `55 THOUSAND`, `1200000` → `1.2 MILLION` |
| **Remembers** | Pull-down history survives restarts; tap a row to keep calculating |
| **Dresses up** | Six colorways: Olive, Cherry, Vanilla, Violet, Turquoise, Lavender |
| **Offline** | Zero network permission — your calculations never leave the phone |

<img width="2148" height="1015" alt="JusCalc in Olive" src="https://github.com/user-attachments/assets/fae27a53-08da-4f9a-af8f-93bf79c3f26f" /><img width="1261" height="1015" alt="JusCalc dials" src="https://github.com/user-attachments/assets/2bb1bed2-5e7a-4818-b430-09bea93cef07" />

## Gestures

| Control | Drag left | Drag right | Tap |
|---|---|---|---|
| Top dial (`5–9`) | higher digit | lower digit | nearest third |
| Bottom dial (`0–4`) | higher digit | lower digit | nearest third |
| Dots (both bars) | slide across — lit count = digit (`10` → `0`) | continues into the other row | dot's position |
| `( )` pad | pick from the `( )` row | — | smart paren |
| `X` pad | `×` | `÷` (or up through `÷ × + −`) | `×` |
| `AC •` pill | `•` | `AC` | either half |
| `Del =` pill | `=` | `Del` | either half |

**Display:** tap copies (result after `=`, expression while typing) · hold opens settings · pull down reveals history. Back closes settings, then history, then the app.

## How JusCalc compares

| | JusCalc | Stock calculator | Scientific apps |
|---|---|---|---|
| Button-free input | ✅ everything drags | ❌ | ❌ |
| One-handed use | ✅ | ⚠️ big phones, no | ⚠️ |
| Word-spelled results | ✅ `1.2 MILLION` | ❌ `1200000` | ❌ |
| Persistent history | ✅ on-device | ⚠️ varies | ✅ |
| Scientific functions | ❌ (basic + parens) | ❌ | ✅ |
| Price / ads / tracking | ✅ free, none, none | ✅ | ⚠️ often ad-supported |

**Use JusCalc** for everyday arithmetic with one thumb. **Use something else** for trig and logarithms.

## Installation

### The APK (recommended)

Grab it from [**Releases**](https://github.com/shubh72010/JusCalc/releases/latest) (Android 7.0+). Coming from v1.0? Uninstall it first — v1.0.1 switched signatures.

### From source

```sh
git clone git@github.com:shubh72010/JusCalc.git
cd JusCalc
./gradlew :app:installRelease   # or :app:installDebug
```

Requires Android SDK 37 + JDK 17.

## For developers

```sh
./gradlew :app:assembleDebug       # debug APK
./gradlew :app:assembleRelease     # upload-signed release APK
./gradlew :app:testDebugUnitTest   # unit tests (CalcEngineTest must stay green)
./gradlew :app:lintRelease         # must be 0 errors before any release
```

Push a `v*` tag and CI tests, builds, and publishes the APK to a GitHub Release with build provenance.

```
┌──────────────────────────────────────────────┐
│ MainActivity.kt — all UI + state             │
│  Display · Dials · Dots · Pills · Pads       │
│  HistoryPanel · SettingsSheet                │
└──────────────────────┬───────────────────────┘
                       │ expr / = 
                       ▼
┌──────────────────────────────────────────────┐
│ CalcEngine.kt — EvalEx (BigDecimal, 12-digit)│
│  evaluate() → Ok | Err · numberToCompact()   │
└──────────────────────┬───────────────────────┘
                       │ persist (30 cap)
                       ▼
              SharedPreferences
```

More: [ARCHITECTURE.md](ARCHITECTURE.md) · [DESIGN_LANGUAGE.md](DESIGN_LANGUAGE.md) · [AGENTS.md](AGENTS.md)

## Troubleshooting

### "App not installed" when updating from v1.0

v1.0 was debug-signed, v1.0.1 uses the upload key — Android treats them as different apps. Uninstall v1.0 first (its history goes with it).

### `adb: more than one device/emulator`

The network exposes the device twice. Pin every call:

```sh
export ANDROID_SERIAL=192.168.1.39:39585
```

### Build fails on missing SDK 37

Install Android SDK Platform 37 via SDK Manager (`sdkmanager "platforms;android-37"`), then rebuild.

## Limitations

- **Basic arithmetic only** — `+ − × ÷`, parens, decimals. No trig, logs, or matrices.
- **28-character expressions**, **30-entry history** — caps, not bugs.
- **Phone-first layout** — no dedicated landscape/tablet arrangement.
- **~8 MB APK** — R8/minify is off; size work hasn't mattered yet.

## FAQ

### Is it offline?

Fully. No network permission in the manifest — nothing leaves your phone.

### Where is my history stored?

On-device SharedPreferences, one line per entry. Uninstalling clears it; updates keep it.

### Will it come to the Play Store?

The upload key is ready — that's the main blocker gone. Store listing is still to do.

### Why EvalEx instead of hand-rolled parsing?

`BigDecimal` math with correct precedence for free, no scripting engine, and anything it rejects becomes a clean `ERROR`. Less code, fewer bugs.

## Credits

- Math: [EvalEx](https://github.com/ezylang/EvalEx) (Apache-2.0)
- Geometry: [Kyant0/Shapes](https://github.com/Kyant0/Shapes)
- Typeface: [Geist](https://github.com/vercel/geist-font) (OFL), bundled in `res/font`
