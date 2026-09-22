# JusCalc.

<img width="2148" height="1015" alt="JusCalc" src="https://github.com/user-attachments/assets/fae27a53-08da-4f9a-af8f-93bf79c3f26f" /><img width="1261" height="1015" alt="JusCalc2" src="https://github.com/user-attachments/assets/2bb1bed2-5e7a-4818-b430-09bea93cef07" />

A calculator with no buttons. Every input is a drag.

## The idea

Numbers live on two rotary dial strips — `0–4` and `5–9`. Drag a strip and release on the digit you want; the selection chases your finger with detent haptics and a spring settle. Everything else is a slide:

| Control | Drag left | Drag right | Tap |
|---|---|---|---|
| Top dial | lower digit (`5–9`) | higher digit | digit |
| Bottom dial | lower digit (`0–4`) | higher digit | digit |
| `( )` pad | `(` | `)` | smart paren |
| `X` pad | `×` | `÷` / `+` up, `−` down | `×` |
| `AC •` pill | clear all | decimal point | either half |
| `Del =` pill | delete | equals | either half |

Results are spelled out in words (`FIVE + TWO = SEVEN`). Long-press the display to cycle the six colorways: Olive, Cherry, Vanilla Haze, Violet Dust, Turquoise, Neon Lavender.

## Tech

- 100% Jetpack Compose, no fragments, no View system
- Geometry via [Kyant0/Shapes](https://github.com/Kyant0/Shapes) (`RoundedRectangle`, `UnevenRoundedRectangle`, `Capsule`) — used only where the primitives match the Figma panels
- Motion is built-in Compose animation only (`Animatable` drag tracking, critically-damped springs, `AnimatedContent`) — no animation library, no bounce, no particles
- Typeface is [Geist](https://github.com/vercel/geist-font) (OFL), bundled in `res/font`
- Expression evaluation is a hand-rolled shunting-yard over `BigDecimal` (`CalcEngine.kt`) — no scripting engine

## Build

```sh
./gradlew :app:assembleDebug      # APK in app/build/outputs/apk/debug/
./gradlew :app:installDebug       # install on the connected device
./gradlew :app:testDebugUnitTest  # unit tests
```

Requires Android SDK 37, Kotlin 2.4.10 (see `gradle/libs.versions.toml`).
