# Structure playlist filter: automated verification

The unreleased source adds searchable exact structure selections and a 0-128 block
radius outside generated structure bounds. Radius 0 matches inside the full box;
distance uses the player's block position, including height and diagonal distance.
Detection initially observes loaded singleplayer structures. Multiplayer structure
data needs a future server-side source; that filter cannot match without it.
The subsequent Match any option can still activate through another enabled filter;
the original regressions here exercise the default Match all behavior.

## Measured checks

- `gradlew.bat build --offline` passed with Java 25: **266 tests, 0 failures**.
- **13 new structure regressions** cover inclusive bounds and fractional block
  positions, spherical/vertical radius edges, exact vanilla/modded IDs, empty and
  unavailable observations, disabling/re-enabling, combined filters, ordinary
  priority/empty-playlist fallthrough, owned save snapshots and JSON round trips,
  normalization, immutable observations, inspector explanations, and indexed
  playback resolution with 70,000 unrelated tracks.
- Main code, including the native observation and new screen, compiled successfully.
- `git diff --check` passed.

The first test run exposed an incorrect time-window expectation in a new test:
12000-7000 wraps midnight and includes 6000. The corrected nighttime window is
12000-23999; the full suite then passed.

## Implementation review and limits

The client submits at most one observation at a time to the integrated server,
at most twice per second within a dimension/session. Registry and structure reads
stay on the server thread. Only immutable IDs and integer bounding boxes cross to
the client. Detection checks at most 17x17 loaded chunks and their loaded referenced
starts using `getChunkNow`; it does not request chunk loads, generation, or disk I/O.
Already observed boxes continue matching across chunk edges until the next refresh.
Dimension changes and logout discard observations and outstanding future handles.

Those ownership and bounded-work properties were reviewed in source; no gameplay
performance measurement or native detection/UI validation was performed. Real
generated villages, chunk loading, world/dimension changes, menu search/layout,
and multiplayer unavailable-data behavior remain listed in
[deferred testing](../../DEFERRED-TESTING.md). Historical native reports do not
validate this feature.
