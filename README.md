# JusCalc.

<img width="2148" height="1015" alt="JusCalc" src="https://github.com/user-attachments/assets/fae27a53-08da-4f9a-af8f-93bf79c3f26f" /><img width="1261" height="1015" alt="JusCalc2" src="https://github.com/user-attachments/assets/2bb1bed2-5e7a-4818-b430-09bea93cef07" />

A calculator with no buttons. Every input is a drag.

## The idea

Numbers live on two rotary dial strips — `0–4` and `5–9`. Drag a strip and release on the digit you want; the selection chases your finger with detent haptics and springs home. Or flip on **Number dots** and count instead: both bars become one `1–10` counter — drag from dot 1 through to bottom-row dot 2 and 7 dots light up, so `7` commits.

| Control | Drag left | Drag right | Tap |
|---|---|---|---|
| Top dial (`5–9`) | higher digit | lower digit | nearest third |
| Bottom dial (`0–4`) | higher digit | lower digit | nearest third |
| Dots (both bars) | slide across, lit count = digit (`10` → `0`) | continues into other row | dot's position |
| `( )` pad | pick from the `( )` row | — | smart paren |
| `X` pad | `×` | `÷` (or up through `÷ × + −`) | `×` |
| `AC •` pill | `•` | `AC` | either half |
| `Del =` pill | `=` | `Del` | either half |

**Display:** tap copies (result after `=`, expression while typing) · hold opens settings · pull down reveals history.

**Settings sheet:** six colorways (Olive, Cherry, Vanilla, Violet, Turquoise, Lavender), dots toggle, GitHub link. Back dismisses the sheet, then history, then the app.

**History:** every `=` lands in a pull-down panel under the display, survives restarts, tap a row to reload its result and keep calculating.

**Results** use compact magnitudes (`55 THOUSAND`, `1.2 MILLION`).

## Tech

- 100% Jetpack Compose, no fragments, no View system
- Geometry via [Kyant0/Shapes](https://github.com/Kyant0/Shapes) (`RoundedRectangle`, `UnevenRoundedRectangle`, `Capsule`) — used only where the primitives match the Figma panels
- Motion is built-in Compose animation only (`Animatable` drag tracking, critically-damped springs, `AnimatedContent`) — no animation library, no bounce, no particles
- Typeface is [Geist](https://github.com/vercel/geist-font) (OFL), bundled in `res/font`
- Expression evaluation is [EvalEx](https://github.com/ezylang/EvalEx) (Apache-2.0) over `BigDecimal`, `MathContext(12, HALF_UP)` — anything it rejects (trailing operator, div-by-zero, bad parens) surfaces as `ERROR`
- History persists in SharedPreferences as one tab-separated `expr⇥result` line per entry (30 cap)

## Build

```sh
./gradlew :app:assembleDebug       # APK in app/build/outputs/apk/debug/
./gradlew :app:assembleRelease     # debug-signed release APK, installs over debug
./gradlew :app:installRelease      # install the release on the connected device
./gradlew :app:testDebugUnitTest   # unit tests
./gradlew :app:lintRelease         # must be 0 errors
```

Requires Android SDK 37, JDK 17, Kotlin 2.4.10 (see `gradle/libs.versions.toml`).

Releases: push a `v*` tag and CI tests, builds the release APK, attaches it to a GitHub Release with build provenance. See [ARCHITECTURE.md](ARCHITECTURE.md).
