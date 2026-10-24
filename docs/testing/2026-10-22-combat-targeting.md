# Singleplayer combat targeting — October 22, 2026

Unreleased change on version 0.6. A qualifying attack/damage event starts combat;
actual targeting by loaded hostile/angered combat-capable mobs can sustain that
fight in singleplayer. Losing the last target starts the full quiet countdown.
Targeting alone cannot start or revive combat. Players only and multiplayer keep
their event-driven expiry.

## Measured verification

- `gradlew.bat build --offline` passed **468 tests**, zero failures/errors, and
  rebuilt `build/libs/animals-final-music-replacer-0.6.jar` using Java 25.
- Eighteen new tracker tests cover attack/damage entry, targeting-only rejection,
  sustained and switching targets, target loss/countdown, brief holds,
  reacquisition before/after expiry, disabled triggers, passive/environmental
  exclusions and broad mode, independent PvP expiry, global off, death/respawn,
  reset, unavailable observations and event-only multiplayer behavior.
- Main compilation validates the current NeoForge event and Minecraft API usage.
  Inspection confirms the server owns its mutable loaded-mob index, observations
  are scoped to immutable session/player requests, scanning is capped at 256 mobs
  per server tick, and no chunks are loaded or remote requests made.

## Remaining verification

No desktop or native gameplay/audio checks were run for this change. The tracker
tests do not prove real entity join/leave delivery, mob AI target behavior,
cross-thread lifecycle timing, rendered tooltips, or native playback transitions.
Live hostile/neutral mobs, large-population scan latency, world/dimension/player
replacement, pause/death transitions and vanilla multiplayer timeout checks remain
in [DEFERRED-TESTING.md](../../DEFERRED-TESTING.md). Prior native reports cover their
recorded builds and do not establish this new targeting integration.
