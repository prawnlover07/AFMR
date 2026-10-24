# Editable templates and UI follow-ups — October 21, 2026

These are Unreleased changes on the unchanged 0.5.7 development version. The
six initial playlists now use ordinary editable conditions. The runtime rule enum,
global Home area and specialized default-condition screens were removed. Existing
saved rules expand to condition data at load, preserving identities, order, song
switches, playback flags, captured areas and valid default thresholds. Deleted
templates are not reinserted.

All/Any/Not groups represent Day/Night's Underground exclusions and Dark
exploration's dimension branches. All playlists share the same editor. Clone
deep-copies conditions, areas, sets, child groups and assignments under a new ID;
library tracks, folder links and override bindings retain their existing identities.

Autoplay here requires a continuous one-second hold. Its red replacement warning
and progress bar appear while held. Completion replaces only the open playlist's
conditions with a 64-block world/dimension-scoped sphere, enables Automatic and
clears Manual only. Early release, pointer exit and screen changes cancel it.
Detailed location editing remains available in the condition menu.

The widget Corner selector supports Default and four corners for Minecraft,
Hybrid and AFMR styles. Bottom positions avoid the hotbar; the native pause-menu
song renderer and other notifications are not repositioned. The playback footer
now uses only the current track, clearing artwork and metadata in silence while
retaining selected-song Preview. The proposed respawn setting was removed at the
user's request; existing respawn/death playback policy remains unchanged.

## Measured verification

- The final shared-source build passed **352 automated tests**. Before concurrent
  condition work was incorporated, this update passed 348. Tests cover the original
  six predicates across time/dimension/Y/cover/depth/light/combat boundaries, full
  editability, clone independence, autoplay area boundaries/scope, saved-rule
  conversion and persistence, deleted templates, nested groups and unavailable
  structure data under Not, nested inspector details and widget anchors.
- The packaged JAR passed **80 isolated in-game checks** using generated quiet WAVs,
  a generated orange cover PNG and the disposable workspace world/profile. The
  helper drives actual screen events and observes native tooltip rendering.
- Existing assignments, priorities, transport, overrides, navigation/drafts, import,
  normalization access and create/delete regressions remained green.
- New checks edit a default's name and nested dimension branch, clone it, cancel
  a tap/short hold/pointer-exit/navigation hold, and complete a full hold. They check
  playlist-local replacement, automatic flags and assignment/source independence.
- The stale-footer regression loads a real generated cover, skips into silence,
  verifies track/art clearing, plays a no-cover song and verifies that Preview still
  uses the earlier selected song.
- Screenshots were inspected for the red warning/filling bar, cleared footer,
  no-cover song, all four AFMR corners, Hybrid top-right and Minecraft bottom-right.
  The native popup test explicitly enables Minecraft's toast visibility, disables
  focus-loss pausing during the fixture, verifies that no pause menu is open, and
  restores those test-profile options afterward.

The native results are preserved in
[the result log](evidence/2026-10-21-template-results.txt). Generated screenshots
remain under the ignored `testing/native/run/workspace/screenshots` directory.
The user's recording was read only; personal music/configuration was not changed.

## Remaining scope

The synthetic screen events do not establish physical mouse/keyboard behavior.
Keyboard holding, focus-loss cancellation, unusual window/GUI-scale combinations,
maximum-sized group trees, native file dialogs and live server respawn/audio cases
were not revalidated by this run. Existing deeper tests remain in
[DEFERRED-TESTING.md](../../DEFERRED-TESTING.md). Native playback's previous baseline
checks are historical evidence; the current automated suite does not replace them.
