# Transport icons and playlist actions — October 25, 2026

Both targets keep version 1.0.1. The primary workspace button draws Play/Pause
symbols with state-specific tooltip/narration labels (Play now, Pause, Resume).
The next-track icon retains immediate-next behavior and manual pause. Skip & wait
stays labelled and is disabled without a song. Resuming a manually paused gap
retains its remaining silence. The existing playback APIs are unchanged.
Symbols use bounded fill primitives, independent of menu-font glyph coverage.

Priority rows share one hover/focus ellipsis button, with permanent space beside
the name and actual-playing marker. Fresh click hit-testing runs before priority
selection/drag handling; a scrolled row opens its own existing opaque menu.
Same-playlist menus retain song search/selection. Invalid drafts, Rename/Undo,
priority dragging, right-click, F2 and Shift+F10 remain available.

Verification

- Full builds pass 1,009 NeoForge / 1,019 Fabric pure regressions and their
  component checks, with zero failures. UI-copy compiler checks pass (20 each),
  validating 167 / 165 passages. No extra mirrored unit tests for icon drawing.
- Actual packaged clients pass **49 native checks per target**, zero failures:
  hover target, reserved button edge, opaque menu, rename/Undo, keyboard menu,
  priority insertion, offscreen hiding, fresh scrolled-row hit testing, invalid
  playlist drafts, primary icon/label states, fresh-gap Play now, paused Next,
  Skip & wait, paused-gap Resume, plus the existing navigation/seek regressions.
- An initial Fabric run caught ordinary row selection intercepting the ellipsis.
  The action-slot hit test now precedes that selection branch. Initial drag and
  paused-Next test expectations were corrected to the existing insertion-line
  and scheduler behavior; no playback policy was changed to satisfy the tests.
- Transport and playlist-menu screenshots were inspected, including a long name
  beside the playing marker and action button. Probe profiles start at 60 FPS
  with VSync off. No personal music, foobar data or launcher profiles changed.
- Both source ZIPs were rebuilt and checked for current sources and excluded
  local data. Runtime JARs exclude native helpers and test font fixtures.

Reproduce with generated fixtures and `-Pworkspace -Pnavigation` in the NeoForge
native helper project, or `-Psmoke -PpackagedSmoke -Pnavigation` in Fabric.
Retained results: [NeoForge](evidence/2026-10-25-transport-icons-neoforge-results.txt),
[Fabric](evidence/2026-10-25-transport-icons-fabric-results.txt).

Final client JAR SHA-256

- NeoForge: `31ad11e830ea9043cba498fe0869c786366cd16d940bb2cc8a7a709ca6d01b45`
- Fabric: `e136c92deff28e6c05201fc2746f281c72087ed5c6ceadef8156b9a485a37f15`
