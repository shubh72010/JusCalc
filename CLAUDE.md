# CLAUDE.md

Instructions for Claude working in this repo. See also ARCHITECTURE.md (structure) and DESIGN_LANGUAGE.md (visual system).

## Commands

```sh
./gradlew :app:assembleDebug       # debug APK
./gradlew :app:assembleRelease     # debug-signed release APK (installs over debug)
./gradlew :app:testDebugUnitTest   # unit tests (CalcEngineTest must stay green)
./gradlew :app:lintRelease         # must be 0 errors before any release
```

JDK 17, Android SDK 37. Releases ship via `v*` git tags (CI builds + publishes the APK).

## Device loop

Wireless ADB. The network often exposes the device twice, so pin every adb call:

```sh
export ANDROID_SERIAL=192.168.1.39:39585
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.jusdots.juscalc/.MainActivity
adb shell screencap -p /sdcard/x.png && adb pull /sdcard/x.png /tmp/x.png
```

`screencap` is harmless; `input`/`am start` steal the user's screen — install silently, but only inject input when the user is off the phone.

## Conventions

- Almost everything lives in `MainActivity.kt`; `CalcEngine.kt` is pure math (EvalEx + compact formatting). Keep it that way.
- Minimal diffs. No new dependencies, abstractions, or screens without being asked. Stdlib and already-present APIs first.
- Gesture rule: sub-slop jitter resolves by release position, never by center — taps must survive finger wobble.
- Dots mode is one unified 1–10 counter (top row 1–5, bottom 6–10); commit is `count % 10`.
- Back order: settings → history → exit. Copy rule: result after `=`, expression while typing.
- Never commit keystores, tokens, `.env`, or APKs. Release is upload-signed locally (`juscalc-upload.jks` + `keystore.properties`, both untracked — back them up); CI falls back to debug signing.
