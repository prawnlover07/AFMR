# Copy diagnostic summary — October 23, 2026

Implemented under Unreleased on both maintained targets; version remains 0.6.
Inspector's footer offers **Copy diagnostic summary**, with temporary success or
failure text. The selected priority row determines the detailed condition tree.
Capture uses Minecraft's clipboard API, with no sending, file export or monitoring.

## Scope and privacy

- Installed AFMR/Minecraft/loader versions, Java/OS/architecture, Java heap
  used/committed/max, music/master volume and basic runtime/session health.
- Playback phase and shared Inspector reason, selected/automatic/source/override
  priority numbers, queue/preview/jukebox/pause state, remaining timers and timing
  settings, current error category and failed-track count.
- Current dimension/biome/item resource IDs, time, block Y, cover/light, temperature,
  combat/PvP/health/death, weather and structure-data availability.
- First 64 priority rows and the inspected playlist's nested All/Any/Not results,
  selected ranges/flags and location match boolean. Inspected details remain
  available when that playlist is beyond the row cap. Clipboard text is limited
  to 48,000 characters with an explicit truncation marker.

The formatter never appends raw Inspector details or exception prose. User names,
profile/playlist/track IDs, song metadata, group labels, file paths, world/server
identifiers, X/Z coordinates and saved location data are omitted. Resource/version
fields accept bounded character sets; error text maps to fixed categories only.
Categories are inferred from the current engine message; this is not a persistent
error history or log export. A heap snapshot is not a native/GPU measurement or
memory-leak diagnosis.

## Automated validation

`build sourceZip --offline` passes on NeoForge (**747 tests**) and Fabric (**757
tests**), zero failures/errors/skips. The 12 new tests per target cover:

- Versions, volume/heap/scan counters, live playback reasons and context.
- Omission of private names, paths, metadata, nested saved locations and addresses;
  newline/control-character injection and invalid resource/version fields.
- Fixed error categories, title-screen handling without stale context, disabled
  runtime/scanning reports, and aliases for overrides with Automatic off.
- Nested condition modes/negation and unavailable structure data.
- No full traversal of a guarded 70,000-track library or playback mutation.
- Priority-row caps, inspected playlists beyond the cap and bounded large output.

`PlaybackInspector.reason` now supplies the common enum used by copied summaries
and the existing human-readable `waiting` text. The prior scheduler/Inspector tests
pass with this change. Report creation occurs only on the client-side button click;
there is no disk/log read, worker, owned library snapshot or steady-render work.

## Remaining checks

Actual in-game clipboard success/failure, physical keyboard navigation and small
window/reflow/hide behavior are in [DEFERRED-TESTING.md](../../DEFERRED-TESTING.md).
No Minecraft window was launched and the system clipboard was not read or changed
during this task. Compilation/component tests do not establish native UI validation.
