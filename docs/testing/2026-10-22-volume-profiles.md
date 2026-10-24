# Playlist volume and world/server profiles — October 22, 2026

Current development version remains 0.6; changes are under Unreleased.

The full build passes **589 automated tests**, zero failures/errors/skips. New tests:

- 12 playlist-volume regressions: live changes/mute, independent fades, retained
  source/Continue Playing on Match, automatic-off overrides, source and unowned
  queues, standalone preview, jukebox resume, death fade, pause, defaults/bounds,
  cloning and writer snapshots.
- 18 profile regressions: Default fallback, fresh templates, stable settings
  identity, independent conditions/assignments/priorities/playback, inactive copies,
  shared globals/library, per-profile folder destinations, original-source imports,
  deleted destinations, exact world/server keys, deletion/links, names/capacity,
  immutable save ownership, malformed data/bounds and complete playback-field copies.
  A 70,000-track guard prohibits full-library reads through profile activation and
  2,000 scheduler ticks with playlist volume applied.
- 5 folder-routing regressions: one rescan populates all linked setups, captured
  destination ownership, recursive/exact directory matching, overlapping roots and
  disabled assignments, deleted profile/playlist handling. Routing follows the
  discovered source path, preserving folder aliases without main-thread disk reads.
- 6 storage regressions: active/inactive round trips, existing root configurations,
  shared metadata stored once, coalesced saves across switches, malformed original
  preservation and inactive volume persistence.

These are component/scheduler/storage tests using fake audio. They do not prove
native gain, widget/input behavior, real connection lifecycle, SDL callback ordering
or listening quality. No game launch, desktop interaction, personal music write or
foobar write occurred. Native checks are recorded in `DEFERRED-TESTING.md`.
