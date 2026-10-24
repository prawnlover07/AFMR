# Consolidated widget settings — 2026-10-23

Scope: both maintained 0.6 development targets. Display settings now has **Widget**
and **Menus** tabs. No setting keys, stored values, defaults or version changed.

## Change

All now-playing controls share one Widget panel: visibility, corner, available
style/native-toast control, popup duration, scale, width, wrapping, seven information
switches, background color/picker, opacity, four presets and appearance reset.
Preview and Save remain fixed in the footer. Menus retains menu opacity, colors,
custom background and the NeoForge blur option.

Small viewports scroll complete control rows with wheel, draggable scrollbar,
Page Up/Page Down and Home/End. Scrolling moves existing widgets without applying
or recreating drafts. Hidden rows are excluded from rendering/input/focus; the
field labels follow their rows. Resizing clamps the offset and retains text via
the existing resize path. Hidden-screen and child-picker/preview behavior remains.
NeoForge button tooltip rectangles refresh only when their position/size changes,
so scrolling does not leave hover descriptions attached to the old position.

Minecraft style still disables custom widget controls and offers the native Music
Toast option. Hybrid enables custom size/information while retaining its native
frame; background colors remain specific to AFMR. Fabric retains its sole AFMR
card style. Both implementations use their native UI APIs directly.

## Verification

Both full builds pass **735 NeoForge / 745 Fabric regression tests**, with zero
failures/errors/skips. No implementation-mirroring unit tests were added for this
UI-only rearrangement.

Focused packaged game checks pass **26 NeoForge / 25 Fabric cases** using generated
music and disposable profiles. Checks cover:

- Only Widget/Menus sub-tabs; all five fields and seven switches remain available.
- Visible control bounds/non-overlap at the 854×480 game window and a larger layout.
- Wheel scrolling preserving an invalid draft, blocked navigation until corrected,
  and saving duration/scale/width together when changing tabs.
- Corner, information and text-layout changes; NeoForge Minecraft/Hybrid/AFMR guards.
- Color-picker Cancel/Apply, hide/reopen with drafts and scroll, and widget preview.
- Home/Page Down and actual scrollbar dragging; menu reset preserving card colors.
- NeoForge scrolled-picker hover rendering, placement and screen bounds.

An initial NeoForge drag check used the older mouse-button numbering in the helper.
It was corrected to `InputConstants.MOUSE_BUTTON_LEFT`; the production scrollbar
uses the target's native widget input. The final run passes the drag check.

The native helpers are separate test-only source sets. NeoForge stages the packaged
runtime JAR in its isolated profile; Fabric uses the remapped JAR and its nested
libraries without loose AFMR classes/external decoder dependencies. These UI runs
do not validate unrelated concurrent inspector work, which the combined builds may
also include.

Commands after building/preparing the disposable profiles:

```powershell
.\gradlew.bat -p testing/native runClient -Pworkspace -PwidgetSettings
.\gradlew.bat -p ports/fabric-1.20.1 -Psmoke -PpackagedSmoke -PwidgetSettings runClient
```

Results: [NeoForge](evidence/2026-10-23-widget-settings-neoforge-results.txt),
[Fabric](evidence/2026-10-23-widget-settings-fabric-results.txt).
Screenshots use `workspace-widget-settings-*.png` and `widget-settings-*.png` in
the disposable profiles. Inspect the top, lower controls, preview and hover views.

## Remaining checks

Physical mouse/keyboard use across unusual GUI scales, keyboard layouts, narrator
navigation, very narrow windows and rapid resize/hide sequences are deferred.
Native checks drive actual screen events but do not constitute those exhaustive
desktop/accessibility checks. Music, foobar settings and personal profiles stay
untouched. Runtime JARs exclude all helper classes and fixtures.
