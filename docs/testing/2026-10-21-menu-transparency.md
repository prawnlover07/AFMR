# Menu transparency — October 21, 2026

Unreleased changes on the unchanged 0.5.7 development version. Display → Menus
adds a live 0–100% background-opacity slider, Minecraft blur toggle and reset.
Defaults: 80% opacity, blur enabled. Minecraft controls blur strength. Text,
artwork and foreground indicators retain their own opacity; menu and now-playing
widget settings are independent. Native Minecraft input/slider textures keep
their normal appearance. Menus retain their existing gameplay pause behavior.

Panel rectangles are cached at setup and exclude list viewports. Lists paint each
visible row or empty region once, preventing overlapping backgrounds from
compounding opacity. Geometry stays cached during rendering; no library traversal
or metadata work was added. Blur uses Minecraft's native background pass while
topmost, and is skipped at 100% opacity.

## Measured verification

- Final shared-source build: **373 automated tests, zero failures/errors**. New
  cases cover menu/widget persistence independence through save snapshots and
  disk, opacity bounds/non-finite inputs, and non-overlapping background cuts.
- Final packaged JAR: **105 isolated in-game checks, zero failures**. The existing
  workspace/template/transport/tooltip/artwork/corner checks remained green.
- New screen-event checks exercise the live slider, blur toggle and reset, and
  verify settings/workspace background alpha at 0/50/100%, opaque text in native
  render states, widget-opacity independence and non-pausing behavior.
- Native renderer screenshots inspected at 0%, 50%, 80% and 100%; blur on/off
  comparisons confirm the game blurs behind clear UI text. The isolated profile
  uses Minecraft blur strength 5. The 80% workspace shows the game through panels
  and song/library viewports after the background-layering correction.
- Only generated audio/artwork and the disposable workspace profile/world were
  used. No personal music, foobar state or personal Minecraft profile was changed.

The native results are preserved in
[the result log](evidence/2026-10-21-menu-transparency-results.txt). Screenshots
remain under ignored `testing/native/run/workspace/screenshots/workspace-menu*.png`.
The test-only helper is not bundled in the mod.

Validated JAR SHA-256:
`d4b391c00dff552b4c23a1a7be15723bca6ced8c68fe29351eca2dad6fd651e2`.

Physical dragging, unusual window shapes/GUI scales and third-party shader/render
combinations remain unmeasured. The native helper drives actual screen events,
not physical mouse input. No new performance benchmark is claimed.
