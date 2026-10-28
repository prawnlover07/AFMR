# Player proximity volume — October 24, 2026

Both maintained builds remain **1.0.1**, with this feature under Unreleased.
**Nearby player → Proximity volume…** offers an independent on/off switch, near
and far distances, near and far volume percentages, and Reverse volumes.

Default endpoints are 4 blocks / 100% and 32 blocks / 0%, with the feature off.
Distances accept 0–1024 blocks with far strictly greater than near; percentages
accept 0–100% of the song-source playlist's existing volume. The trigger radius
remains independent. Outside the endpoints, clamp to the corresponding volume;
untracked/offline players use the far endpoint. Between them interpolate linearly
in actual block distance and smooth changed observations over 250 ms.

## Implementation and semantics

`PlayerProximityVolume` owns curve data/validation/normalization/copying. Each
condition node owns a curve. Only enabled nearby filters in enabled groups apply;
multiple curves use the quietest result, not compounded attenuation. Predicate
matching (All/Any/Not/trigger radius) stays independent from source gain.
`NearbyPlayers.distanceSquared` reuses the identity lookup; known UUIDs never fall
back to someone else's matching old name. Observations remain limited to living
players already tracked by the client in the current dimension, at the existing
maximum four updates per second. No server lookup or extra entity-scan rate.

The scheduler initializes gain at song start and keeps a separate proximity ramp
in playback time. Normal/death/transition/jukebox fades multiply the ramp with the
existing fade envelope and actual source playlist volume. Pause/record holds
freeze it. Retained songs, overrides and source-owned queue requests follow the
source; previews and unowned queue requests retain normal volume. Zero mutes
without stopping or advancing the song. Owned copies, clones, profiles, library
storage, portable playlist files and full backups retain the curve. Strict sharing
checks nested keys/types/ranges; diagnostics show settings without player identity.

## Automated verification

Full `build` tasks pass on both targets:

- **861 NeoForge tests / 871 Fabric tests**, including **25 focused proximity cases**
  per target, plus the existing backup round trip populated with curve values.
- UI-copy compiler **20 checks** per target; **163 / 161** passages validated.

Focused coverage includes off/default, near/far/midpoints/clamps, reverse/constant
volume, unavailable/invalid observations, strict UUID identity, separate trigger
radius, disabled/nested groups and quietest combination; initial/zero/live source
gain without restart, fade multiplication, pause, retained ownership, held override,
owned/unowned queues, previews, jukebox fade/resume and reset. Storage tests check
nested normalization, independent snapshots/clones/profiles, persistence, shared
round trips and rejection of bad curve types/unknown keys/ranges.

## Packaged native checks

**22 NeoForge / 22 Fabric checks pass**, with generated WAVs and a synthetic
client-side RemotePlayer in disposable singleplayer profiles. Both clients exit
successfully. These runs exercise the actual observation/runtime/audio path; they
do not represent two real players connected to a server.

- NeoForge: `runClient -Pworkspace -PproximityVolume` in `testing/native`.
  [Results](evidence/2026-10-24-proximity-volume-neoforge-results.txt).
- Fabric: `-Psmoke -PpackagedSmoke -PproximityVolume runClient` in the port.
  [Results](evidence/2026-10-24-proximity-volume-fabric-results.txt).

Native input checks access, default-off/four fields, invalid/equal distance and
over-limit percentage rejection, hidden invalid drafts, valid enable, small-window
bounds/no overlap, independent saved trigger radius and reversal retaining distances.
Visual screenshots of both endpoint editors were reviewed; screenshots remain in
ignored disposable-profile folders.

The helper moves the synthetic player to 4, 18 and 40 blocks, sampling actual
OpenAL source gain/state/error through Minecraft's sound executor. It verifies full,
half and zero gain relative to the near reference, then reverse full/zero/midpoint,
untracked far fallback, off/full gain and re-enabled retained settings. The same
playback sequence and active native sound persist throughout, including at zero.
The configured playlist gain is 50%; the native reference is **0.1 NeoForge** and
**0.5 Fabric** because the master volume is 20% and the targets apply it differently
in their native sound paths. Comparisons use the measured reference accordingly.

Early helper runs were rejected: Gradle's built-in `normalization` property
accidentally enabled an unrelated fixture; the manual-normalization launch flag
is now `gameplayNormalization`. A NeoForge assertion also incorrectly assumed
master gain was outside the source gain. The corrected focused runs above pass;
these corrections changed test helpers/launching, not the feature's production code.

Native-tested runtime SHA-256:

- NeoForge: `3065c337fcb8991dd2e181142839e4fdd7b5fcb225f3c7895a1f8c642e7e48f3`
- Fabric: `01366421ba014709f7edda8757fb4d2f8df70d6f88efc04d8a20add4b18b6f93`

Both source ZIP tasks and archive-content checks cover the new classes/tests and
exclude personal data, profiles/worlds, caches, Git metadata, backups and runtime
test helpers. Existing release outputs/tags are preserved; no version increment.

## Remaining deeper checks

Two real remote players, server tracking distance/chunk unload/death/rename,
rapid movement/teleports with a real network, physical keyboard/narration and
extreme GUI scales, restart transfer of curve configuration, and long native chains
mixing nested groups, queues, death and jukebox holds remain broader tests.
Automated scheduler tests do not prove every corresponding native interaction.
