# Fit songs to time window: automated verification

Implemented in the **0.6** development source without changing its version. The
Time condition has an opt-in Fit songs to time window switch. Automatic starts
filter the selected playlist's songs against the remaining matching interval at
normal 20-tick/second clock flow. If none fit, that playlist keeps priority and
stays silent. Overrides/previews bypass fitting; already playing songs keep their
existing transition and finish behavior.

## Measured checks

- `gradlew.bat build --offline` passed with Java 25: **432 tests, 0 failures**.
- **23 time-fitting tests** cover inclusive/exact boundaries, wrapped ranges,
  all-day/disabled options, All/Any/Not and overlapping alternatives, silence versus
  fallback, delayed starts after buffer/gap, unknown-duration requests/retry,
  overrides/previews/Next, pause/resume, shrinking windows, shuffle cycles, vanilla
  candidates, cloned/saved settings, Inspector explanations, frequency-gap changes,
  and indexed lookup with 70,000 unrelated library entries.
- The boundary algorithm also agrees with an independent tick-by-tick truth check
  across generated nested All/Any/Not trees and multiple clock positions.
- **11 duration-reader checks** cover all seven existing generated audio formats,
  unchanged source bytes/mtime, Vorbis granules compared to the independent WAV
  frame-count reference, unknown granule positions, and rejection of malformed,
  truncated, incomplete or chained Ogg resources and invalid local audio.
- Main code, background integration and the Time screen compile. `git diff --check`
  passes; Gradle and NeoForge metadata remain **0.6**.

## Reviewed behavior and limits

Budget evaluation happens at the actual scheduler start attempt after buffer/gap,
not at initial playlist selection. Opted-in Time branches alone are projected; other
observations stay fixed. A matching non-time alternative can make the budget
unlimited, and adjacent/overlapping time alternatives can extend it. Negated Time
branches close when their forbidden interval begins. Inclusive end blocks use the
end+1 tick. Clock acceleration/jumps, changing observations, decode startup delay,
or pausing an already playing song are not predicted by the initial duration check.

Duration requests use one existing library worker and immutable source observations;
the client owns results and save snapshots. Local metadata and resource Vorbis page
headers are read without decoding PCM or modifying originals. Cache saves are batched,
and pending resource results/cache are invalidated on reload. Failed duration reads
do not disable ordinary/manual audio playback. These integration properties were
reviewed in source; no live performance benchmark was performed.

Native UI/audio/inventory/server runs were not performed for this feature. The
Time-screen drafts/layout, real known/unknown playlists, resource-pack replacement,
pause/reconnect and native finish/transition cases remain tracked in
[deferred testing](../../DEFERRED-TESTING.md). Earlier native reports do not validate
these additions. No source packaging check was performed in this feature run.

The later [combined 0.6 run](2026-10-22-release-0.6.md) passed 450 automated tests
and 135 in-game assertions, including basic Time fitting UI, missing WAV duration
discovery, fitting playback, no-fit priority/silence and wider-window recovery.
The remaining broader cases above were not established by those basic checks.
