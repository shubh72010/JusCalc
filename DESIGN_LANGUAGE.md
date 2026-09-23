# DESIGN LANGUAGE

Black frame, olive soul. One physical language everywhere.

## Canvas

- **Frame:** near-black `Ink 0xFF1E1E1E` background; the display and key panels float on it.
- **Panels:** G2 rounded rectangles (Kyant `RoundedRectangle`), 20dp small cards, 45dp display and bottom outer corners, few-px gaps. Bottom pills are capsules.
- **Colorways** (whole UI re-skins, ink text stays): Olive `748067` · Cherry `E85D75` · Vanilla `FDF4D2` · Violet `A290B7` · Turquoise `2DEEE1` · Lavender `EA9EFF`.
- **Type:** Geist throughout; expression tokens 38sp medium, result 46sp semibold, dial center 46sp semibold, neighbors 22sp at 55%.

## Motion

Critically-damped springs only (`StiffnessMedium` settle, `StiffnessHigh` snap, `DampingRatioNoBouncy`). Selections chase the finger at reduced travel (0.15–0.25×), detent ticks on change, confirm buzz on commit, springs home on release. New display lines fade + slide in thirds; results crossfade. No bounce, no glow, no particles.

## Feedback

Haptics are the confirmation: `TextHandleMove` ticks while sliding, `LongPress` on commit, toast only for clipboard copies ("Copied"). Pressed pads scale to 0.92. Option popups (`( )` row, `÷ × + −` stack) use dark discs with accent rings at rest, accent fill when armed — legible over both olive cards and the black frame.

## Voice

Results speak in compact magnitudes (`5 THOUSAND`, `1.2 MILLION`, rollover-safe: `999999.9K` steps up, never prints `1000 THOUSAND`). History rows read `expr = result`. Errors say `ERROR`, nothing more.
