# Optional song-list sharing — October 22, 2026

Implemented for NeoForge 26.3 / Java 25 and Fabric 1.20.1 / Java 17, retaining
development version **0.6** and collecting changes under Unreleased.

## Behavior

Export has **Include song list: Off/On**; import has **Import songs: Off/On**.
Both start off. Song-bearing `.afmrp` files carry relative paths under named music
roots, explicit on/off assignments and resource IDs. Audio files, absolute music
paths, original track IDs, profile links and source world/server IDs are excluded.
Playlist settings/priority/location rebinding retain their existing behavior.

Unique linked root names are matched automatically. Ambiguous source or recipient
names require explicit Explorer root selection. A background review checks exact
relative paths, reports found/missing counts and reuses existing library entries.
Unfound assignments remain in imported playlists. Unlocated roots receive expected
paths under AFMR's configuration directory, without creating folders/files. The
ordinary repair report then supports relative-folder and individual-file repair.
Files under a chosen root that were not listed in the setup are not imported.

## Automated verification

Both targets pass **661 tests**, with **zero failures/errors/skips**, including
**22 new `PresetSongsTest` cases each**. Checks cover:

- Portable references, relative-path privacy, shared-song deduplication and on/off states.
- Individually imported files and nested root selection; exact matching of identical
  filenames in separate albums, with no loose filename guessing.
- Missing-root imports followed by the existing folder repair; partial matches,
  individual renamed-file repair and actual library save/load afterward.
- Reuse of stable recipient IDs, repeated import without duplicate tracks, inactive
  profile insertion, settings-only bypass and cancelled/replaced plans without changes.
- Ambiguous names on either side; deliberately mapping roots to the same file,
  with disabled winning conflicting assignments.
- Explicit resource exclusions and unavailable resource identifiers; deleted profiles
  and mismatched plans failing without modifying the library.
- Absolute/UNC/drive/traversal/unsupported paths, malformed references and coerced booleans.
- Bounded progress, source-byte preservation and indexed selected-song export with
  70,000 unrelated entries; actionable handling of removed assignments.

Builds compile the target-specific screens/pickers. Fabric production sources target
Java 17; the test runner uses the local Java 25 development JDK. Test data is generated
under temporary directories. No personal music, foobar or live Minecraft state is used.

## Limits

No Minecraft or Explorer window was opened for this feature. The earlier combined
release checks predate it. Actual new controls, file dialogs, cross-target screen
workflows, native playback and restart of this extension remain recorded in
`DEFERRED-TESTING.md`; component tests do not establish those native results.
