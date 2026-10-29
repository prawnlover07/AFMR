# Show now playing hotkey — October 29, 2026

Implemented under Unreleased on both maintained targets; version remains 1.1.
The existing published 1.1 packages and tag are preserved.

`Show now playing widget/toast` starts unbound in Minecraft's AFMR controls
category. Its gameplay handler directly reopens the overlay's current cached
content. It resets display age, bypasses the song-start delay and works during
manual music pause. NeoForge requests the real native toast again for Minecraft
style; AFMR/hybrid cards use their configured duration and existing fades. Repeat
presses refresh the display. Visibility/HUD/native-toast settings still apply,
and there is no action without a current song or when another screen captures keys.

Showing metadata does not restart audio or alter its playback sequence, queue,
history, shuffle, delay preferences or cached artwork. No server-owned state or
companion/protocol change is needed. Key capture/restore includes the new binding.
Backup validation allows the new key without rejecting existing 1.1 backups that
contain the fifteen original keys, and still requires every original binding.

Verification:

- Full builds: **1,011 NeoForge / 1,021 Fabric** pure regressions, **10 component
  checks** and **20 copy compiler checks** per target; zero failures/errors/skips.
  Two new backup cases per target cover round-tripping the new shortcut and
  rejecting a new shortcut substituted for a missing original binding. Existing
  backup cases exercise the original fifteen-key payloads.
- Packaged native clients: **19 NeoForge / 16 Fabric checks**, zero failures.
  Fabric ran on Java 17.0.15. Both used generated music/cover images and disposable
  profiles, one client at a time, capped at 60 FPS with VSync off before startup.
- The probe binds F8 through AFMR's real restore path and feeds Minecraft's
  `KeyMapping` click queue. Checks cover registration/category/default binding,
  no-song behavior, bypassing a 60-second automatic delay, reopening after the
  card's two-second test duration, repeat presses, paused native audio, retained
  stream identity, queue/history, retained cover generation and disabled visibility.
  NeoForge also checks hybrid/native styles and native toast SHOW state while
  music remains paused. This does not claim physical keyboard/Stream Deck testing
  or measurement of vanilla's entire toast expiration interval.
- Generated card/native-toast screenshots were inspected. Original music, foobar
  data and personal launcher profiles were not changed. Disposable clients exited.
  Raw logs/results remain in ignored `work/now-playing-key-*`,
  `testing/native/results/now-playing-key-results.txt` and Fabric's disposable
  `run-smoke/now-playing-key-results.txt`.
- Published `outputs/1.1/SHA256SUMS.txt` still matches every release artifact.

Reproduce with generated fixtures using `gradlew.bat -p testing/native -Pworkspace
-PnowPlayingKey runClient`, or the port's `gradlew.bat -Psmoke -PpackagedSmoke
-PnowPlayingKey runClient`. Coordinate desktop use first.

Native-tested development client SHA-256:

```text
NeoForge 10f49a5d1307a181f1301a019cf2d0584ee40f57cddff6c6867082e4016498a5
Fabric   247d3f9f77210d3bcd08e854a94f0e82aec1e7342c9621b262ff1fa18745992e
```
