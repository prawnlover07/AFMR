# Optional song columns verification — October 22, 2026

This feature remains under Unreleased in the current **0.6** development build.
The version is unchanged while concurrent development is in progress.

## Measured results

- The main build passed **509 automated tests**, zero failures/errors. Eighteen
  new checks cover metadata, ordering, saved preferences and table geometry.
- Generated tagged copies of the MP3, FLAC, OGG and WAV fixtures imported album
  artist, track/disc numbers, genre, date and ReplayGain track/album gain/peak.
  Byte comparisons and modification-time checks confirmed the import did not
  change these files. Peak-only/album-only and invalid tags were also checked;
  the existing playback ReplayGain regression tests still pass.
- Column preferences and all new metadata survived an owned library snapshot and
  disk save/load. Title/default normalization and missing fields were checked.
- Track/disc numbers and gains sort numerically, preserve displayed tag text and
  keep missing values last in either direction. Displayed ReplayGain values remain
  distinct from cached AFMR measurements. Geometry checks cover default fitting,
  overflow widths, scrolled hit testing, thumb endpoints and offset clamping.
- The packaged JAR passed **179 isolated in-game assertions**, zero failures.
  Seventeen new assertions supplement the earlier 162-check workspace run.
  Actual Minecraft screen events exercised the chooser, required Title, every
  optional field, bounded controls, hidden choices and return to the workspace.
- Generated track entries supplied known metadata for native cell-cache checks.
  Track-number ordering, reverse ordering and scrolled ReplayGain header selection
  passed. Synthetic horizontal scrolling and scrollbar dragging reached the final
  column. The On switch stayed pinned and metadata-row selection did not alter it.
  Choices applied across playlists; Defaults removed overflow and reset the offset.
- Captured chooser and horizontally scrolled workspace screens were inspected
  at an 854×480 window. Metadata stayed clipped to its viewport, the assignment
  column stayed visible, and controls remained separate. Screenshots are under
  ignored `testing/native/run/workspace/screenshots/workspace-columns-*.png`.

[Raw workspace results](evidence/2026-10-22-song-columns-results.txt).

Tested JAR SHA-256:
`fd79da4c492780826d62c85282b03aec0894c3d3188a93a66040527129af94c4`.

## Limits

Physical input, unusual GUI scales, long/Unicode tags, large playlist performance,
and live metadata changes while the table is open remain in
[deferred testing](../../DEFERRED-TESTING.md). Component tests establish file-tag
reading; the native checks do not exercise an Explorer import dialog. Neither
column visibility nor informational album gain changes playback normalization.
Existing music needs a rescan to populate newly cached tags. No personal music,
foobar settings or personal Minecraft profile was modified. The helper remains
test-only and is not bundled in AFMR.
