# animal's final music replacer (AFMR)

AFMR is a client-side Minecraft soundtrack mod. It combines local music and vanilla
tracks, selects playlists from the player's situation, and provides manual controls.

Current baseline: **0.5.2**, **Minecraft 26.3**, **NeoForge 26.3.0.90**, **Java 25**.
The owner/author is **animal**. This folder is the source of truth for development.

## Accepted product decisions

- Keep six defaults: Combat, Home, Dark exploration, Underground, Night outside,
  Day outside, in that initial priority order. Defaults do not depend on biome.
- Custom named playlists can combine optional location, time, dimension, elevation,
  cover, biome, temperature, light, combat, and water filters. All enabled filters
  must match; any selected exact biome satisfies the biome filter.
- The first eligible playlist in the editable priority list wins. Empty playlists
  fall through. Home uses a captured world/dimension-scoped spherical location.
- Underground requires both its Y cutoff and sufficient solid cover. Dark exploration
  requires cover and darkness, with independent optional whole-dimension Nether/End
  switches that ignore cover and light there.
- Music augments vanilla by default. Files/folders are imported through native file
  dialogs; launch rescans are optional. Import foobar `.fplite` into an existing
  chosen playlist. Never modify the original files or foobar data.
- Automatic off disables detection/selection only. Manual overrides still work.
  Individual disabled songs remain ineligible. Manual-only playlists are supported.
- Finish current song on entry and on exit are independent. Either the outgoing
  source's exit or incoming playlist's entry can protect a song. Ownership stays
  with the playlist that actually started it, including across multiple transitions.
- Normal transitions use the configured buffer, fade, then silence. Minecraft-like
  timing defaults are 600-1200 seconds, with no added fade or buffer.
- Combat detects attacks/damage and supports immediate death fade by default, cut,
  or finishing the current combat song. New music while dead is blocked by default.
- One-song and held overrides, eight configurable key slots, an inspector, per-playlist
  Fisher-Yates shuffle bags and repeat avoidance are implemented.
- Next bypasses waiting and keeps override state. Pause holds the actual audio position
  and playback timers while live context detection continues. Both keys start unbound.
- Existing ReplayGain track gain/peak tags are supported. A loudness scanner is deferred.
- Use the folder/column editor and bottom-left now-playing card with artwork.

## Current validation and next work

The regression suite has **227 passing tests**. The October 21 session validated
**81 native playback checks and 6 persistence checks**, plus UI scanning/import/settings
checks. See [the report](docs/testing/2026-10-21.md), [validation](VALIDATION.md), and
[remaining complex cases](DEFERRED-TESTING.md). No AFMR defect was reproduced in that run.

Future Minecraft downgrades should be deliberate ports after choosing a target;
the current code is not a multi-version compatibility layer. Requested future features
belong in [the backlog](BACKLOG.md). See [development instructions](DEVELOPMENT.md) for
the build, source layout, Git checkpoints, and native test fixture.
