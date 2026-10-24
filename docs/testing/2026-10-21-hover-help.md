# Hover descriptions — October 21, 2026

Unreleased changes on the 0.5.7 development version add concise help for playlist
rules, overrides, transport, assignment actions, timing, advanced conditions,
folder unlinking, import destinations, override slots and display fields.
No permanent explanatory text or tutorial was added to the game screens.

Buttons use Minecraft's tooltip holder, wrapping, narration and positioning.
The workspace supplies actual screen coordinates for its scaled controls;
tooltips remain at the normal Minecraft GUI text size. Button help has a 350 ms
delay, also supports keyboard focus, and remains available on disabled buttons.
Clipped labels retain a hover fallback when no explanatory tooltip replaces it.

Validation:

- `gradlew build`: 331 tests, no failures or skipped tests.
- The isolated workspace helper passed 58 checks, extending the previous 48 with
  scaled-render and disabled-control fixtures plus tooltip render/bounds checks.
- NeoForge's tooltip render event observed the actual deferred render path. The
  helper verified mouse anchoring and screen bounds at the workspace right edge,
  bottom edge, a timing edit field, and the disabled native-style Preview button.
- Renderer screenshots of the right edge, bottom edge and disabled control were
  visually inspected: readable text, wrapped lines, and no clipping at the edges.
- The complete workspace control/navigation regression still passed. Generated
  WAVs and the disposable `testing/native/run/workspace` profile were used.

The renderer checks preceded final copy refinements and the clipped-label fallback;
the final source was rebuilt afterward. This run does not establish physical
mouse behavior, screen-reader output, every field's wording, or every GUI scale
and window size. Keyboard-focus visibility uses Minecraft's native holder but
was not separately exercised. Existing deeper tests remain in DEFERRED-TESTING.md.
