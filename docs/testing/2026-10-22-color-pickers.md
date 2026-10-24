# Visual color pickers — October 22, 2026

Implemented under Unreleased on both targets; the development version remains
**0.6**. Every menu-color family and the AFMR widget background now has a clickable
swatch opening a visual picker. Existing hex entry and opacity controls remain.

## Measured results

- NeoForge passes **691 automated tests**; Fabric passes **701**, zero failures
  or errors. Seven new tests per target cover primary/secondary hue endpoints,
  all gray levels and 10,000 exact random RGB round trips, comparison with Java's
  independent HSB conversion, gray/black hue retention, saturation/value corners,
  non-finite/out-of-range clamping and exclusion of alpha bits.
- The packaged NeoForge JAR passes **25 focused in-game assertions**, zero failures.
  The helper opens all ten menu colors and the widget background. It checks
  initial color seeding, control bounds, hex preview, hue selection, saturation/
  brightness selection and synchronized hex output, out-of-bounds drag clamping,
  keyboard adjustment, invalid Apply blocking and Cancel restoring the original.
- Native checks also cover recovering an invalid parent hex field through the
  picker, unchanged panel opacity, the NeoForge widget-style availability guard,
  independent widget preview, hide/reopen retention, Apply and Escape rollback.
- The remapped/packaged Fabric JAR passes **75 smoke assertions**, zero failures.
  Twenty-four new picker checks cover the same applicable controls and all eleven
  color options; the existing 51 playback, codec, menu-color and UI checks still
  pass. Fabric has no unsupported widget style to gate.
- Menu and widget picker screenshots from both targets were inspected at 854×480.
  The SV square, hue strip, selection markers, Original/New swatches, presets,
  hex input and Apply/Cancel remain separate and readable. NeoForge retains menu
  blur; Fabric renders above the game without blur. Screenshots stay in ignored
  disposable profiles under `screenshots/*color-picker-*.png`.

Evidence: [NeoForge results](evidence/2026-10-22-color-pickers-neoforge-results.txt),
[Fabric results](evidence/2026-10-22-color-pickers-fabric-results.txt).

Tested runtime JAR SHA-256:

- NeoForge: `b1bca4e5b42ba9fe5f61f06d7412a04090cb0d472b4a0fe1e7f73be9937d6b48`.
- Fabric: `b5f65700c19840c4cc35f7d3c1bc80dfadd429f8c0165a9e1c403d916c2f4777`.

## Implementation and limits

The picker owns RGB/HSV state and its opening color. Valid drag/key/hex/preset
changes invoke the owning screen's direct setting callback; Apply retains them,
while Cancel/Escape invokes that callback with the opening color. Parent settings
retain validation and style availability. The picker can replace incomplete target
hex with a valid chosen color. Opacity is not part of the picker.

Rendering uses 64 cached vertical SV strips and six hue bands. Strip colors only
refresh when hue changes; no image decoding, texture allocation, file reads or
metadata work occurs in picker rendering. No new setting format/version, worker
thread or compatibility layer was introduced.

The helpers drive real native screen events, not physical input. Extreme scales,
incomplete-hex resize, disconnect/restart during previews, long repeated sessions
and third-party UI/render mods remain in [deferred testing](../../DEFERRED-TESTING.md).
All state-changing tests used generated music and disposable worlds/profiles;
personal music, images, foobar settings and game profiles were untouched. Helpers
are excluded from both runtime JARs.
