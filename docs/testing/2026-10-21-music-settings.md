# Native music settings and selectable now-playing widgets

Current source adds Minecraft / AFMR widget styles and observes native Music
Frequency changes. The original AFMR card and artwork remain selectable.

## Automated verification

- Root `gradlew build --offline --console=plain` succeeded: **314 tests**, no failures,
  errors, or skipped tests. Five new tests cover style snapshot/storage/defaults,
  same-track playback identities, failed-start notification suppression, pending-gap
  rescheduling without interrupting music, and frequency changes while manually paused.
- Integration was implemented against the cached Minecraft 26.3.0.90 sources:
  `NowPlayingToast.getCurrentSongName` / `getNowPlayingString`, native toast-manager
  show/hide and pause-menu rendering, and `MusicManager.setMinutesBetweenSongs`.
- Normal GAME music timing presets follow that source: Default 600-1200 seconds,
  Frequent 600 seconds, Constant 5 seconds. This deliberately uses the normal-game
  presets for AFMR's global gaps, rather than reproducing every situational vanilla timer.

No Minecraft session was launched or controlled. Native mixin application, visual
layout, animation, live setting input, and audio/UI interaction remain on the
[deferred checklist](../../DEFERRED-TESTING.md). Personal music, foobar files, and
live game configuration were not changed.
