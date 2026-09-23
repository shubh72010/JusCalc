# ARCHITECTURE

Single-module Android app (`:app`), 100% Jetpack Compose. Two Kotlin files do nearly everything.

## Files

| File | Job |
|---|---|
| `app/src/main/java/com/jusdots/juscalc/MainActivity.kt` | All UI: `JusCalcApp` state root, `Display`, `NumberDialBar`, `DotsBar`, `DualPill`, `ParenPad`, `OpPad`, `HistoryPanel`, `SettingsSheet` |
| `app/src/main/java/com/jusdots/juscalc/CalcEngine.kt` | `evaluate()` (EvalEx wrapper) + `numberToCompact()` (magnitude words) |
| `app/src/test/.../CalcEngineTest.kt` | Arithmetic, error, and compact-format unit tests |
| `gradle/libs.versions.toml` | Version catalog: AGP 9.3.2, Kotlin 2.4.10, Compose BOM, EvalEx 3.7.0, Kyant Shapes 1.2.1 |

## State flow

`JusCalcApp` owns everything in `remember`: `expr` (typed expression), `resultPlain`/`resultErr`/`afterEquals` (last evaluation), `themeIdx`, `dotsMode`, `dotsCount`, `showSettings`, `showHistory`, `history` (a `mutableStateListOf`, hydrated from SharedPreferences on launch, saved on every change).

- Typing helpers (`append`, `appendOp`, `appendParen`, `smartParen`, `appendDot`) mutate `expr`; any commit after `=` starts fresh or chains the previous result.
- `=` runs `CalcEngine.evaluate(expr)` → `Ok` records `expr to result` at history head (cap 30) and persists; `Err` shows `ERROR`.
- Display renders token-per-line from `expr` plus the compact result; it never owns state.

## Key decisions

- **EvalEx, not hand-rolled parsing.** `BigDecimal` math with a 12-digit `HALF_UP` context; normalizes `× ÷ x X` before evaluating.
- **Compact magnitudes** (`numberToCompact`) for display only; history reloads the full plain result so precision is never lost.
- **Debug-signed release.** `app/build.gradle.kts` signs `release` with the debug key: installable, installs over debug builds, zero secret management. Swap in a real upload key before any store upload.
- **No R8/minify** (`optimization.enable = false`). APK is ~8 MB; enable when size matters.
- **Back handling is layered:** one `BackHandler` closes settings, then history, then lets the app exit.
- **CI releases on tags.** Push `v*` → `.github/workflows/release.yml` runs unit tests, `assembleRelease`, attests provenance, publishes the APK to a GitHub Release.
