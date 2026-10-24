# Weather and condition matching: automated verification

The unreleased source adds Weather choices (Any weather, Rain, Snow, Thunderstorm,
Clear) and Conditions: match all / any. All is the default; no enabled filters
matches everywhere in either mode. Any weather means rain/snow/thunderstorms,
excluding clear. Turning the Weather filter off permits all weather states.

## Measured checks

- `gradlew.bat build --offline` passed with Java 25: **286 tests, 0 failures**.
- **11 weather tests** cover the combined weather option, rain/snow/storm overlap,
  dry biomes, independent cover, disabling, priority/fallthrough, snapshot and JSON
  persistence, defaults, Home-context copying, and inspector diagnostics.
- **7 matching tests** exercise all 12 filter types as independent ANY winners,
  ALL failures, no enabled filters, disabled filters, unavailable structure data,
  the global combat switch, summaries/inspector, priority/empty/manual playlists,
  and explicit overrides with Automatic off.
- **2 additional combat tests** verify that Combat OR Weather tracks started
  peacefully do not receive the combat death cut, while tracks started during
  combat retain the policy. Existing Combat/ALL regressions also pass.
- Main code and the weather selector compile; `git diff --check` passes.

## Implementation and limits

Weather observations use existing client rain/thunder flags and the already sampled
biome's precipitation at the player's block position and sea level. No new threads,
network requests, terrain scans, or disk reads are involved. The client flags gate
weather by dimension. Roof exposure is independent; Rain/Snow still match indoors.
Any weather and Thunderstorm use dimension-wide weather, including dry biomes;
Rain/Snow additionally require the corresponding local precipitation type.

The shared predicate combines checked/failed filter bits without per-filter
allocations. ALL preserves first-failure diagnostics; ANY reports when none of the
enabled filters match. Unavailable structures cannot match, but do not block another
matching filter in ANY. Playlist priority and disabled-song/override rules remain.

No native client/UI or server weather run was performed for these additions. The
earlier native reports do not validate them. Live synchronization, GUI scales/input,
weather changes across worlds/biomes/dimensions, and native transition/audio cases
are tracked in [deferred testing](../../DEFERRED-TESTING.md).
