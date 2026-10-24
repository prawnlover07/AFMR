# Playlist setup sharing — October 22, 2026

Current development version remains 0.6, with `.afmrp` changes under Unreleased.

The full build passes **639 automated tests**, zero failures/errors/skips,
including **23 new playlist-preset regressions**. They cover:

- Every playlist boolean option and volume, Unicode names, all condition types,
  nested All/Any/Not groups and ordinary library save/reload.
- Removal of songs/assignments, music paths, profile bindings/global preferences,
  original singleplayer save-folder paths and server addresses.
- Nested configured location rebinding, saved coordinates/radii/dimensions,
  disabled filters/groups, requiring a world only when locations are configured.
- Multi-selection priority order, three group insertion positions, a deleted
  anchor, inactive/deleted profiles, independent repeated imports and snapshots
  retained through later edits/profile switches.
- UTF-8/uppercase extension, complete overwrites/temp cleanup and preserving a
  previous file on invalid export. Rejection of song-bearing files, original world
  IDs, unsupported fields/enums, invalid field types, duplicate keys, missing
  marker, malformed UTF-8, size/count and condition nesting limits.
- A guarded 70,000-track catalog and assignment map: core capture/insertion cannot
  get/iterate/stream the catalog or copy the source song assignments. The existing runtime save snapshot still costs work at the edit
  boundary; these tests do not benchmark native UI or filesystem latency.

No Minecraft launch, desktop interaction or personal music/foobar write occurred.
SDL Save/Open and overwrite/cancel behavior, physical input/rendered layout, live
world binding, cross-screen lifecycle and audible rule transitions are deferred
in `DEFERRED-TESTING.md`. Pure model/storage tests do not establish those results.
