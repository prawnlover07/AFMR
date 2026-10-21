# Validation — 0.5.2

Target: Minecraft Java 26.3, NeoForge 26.3.0.90, Java 25, Windows 11.

## Later native/UI session - October 21, 2026

The user authorized deeper interaction testing. The packaged 0.5.2 JAR passed 81 native playback checks and 6 persistence checks, plus real UI/keyboard checks. The full 227-test regression suite passed again. Native OpenAL state/position/gain measurements validate pause and resume, fades, delayed decoding, overrides, shuffle, environments and combat/death. Explorer/5,000-file scan/.fplite/custom-playlist/reset UI checks and saved-configuration comparisons pass. An invalid old-key-code assertion in the helper was corrected and the persistence checks rerun successfully; other helper timing/coordinate assumptions were also corrected before accepting results. No AFMR production change was needed. Personal music/foobar files were not modified; the test game was closed.

See the [detailed session report](docs/testing/2026-10-21.md) and `DEFERRED-TESTING.md` for scope and remaining native/hardware cases. The release-time notes below record what was deferred when each build was originally packaged.

## 0.5.2 checks

**227 tests pass**, including 22 new transport checks. Pause coverage includes retaining the current song, holding a silent gap/fade-in/fade-out/context buffer, observing death Cut/Fade while paused, queued overrides/previews, ordinary skip, asynchronous decode failure, idempotence and reset. Next coverage includes bypassing waits/finish protection, initial silence, choosing the current environment over a retained source, preserving both override modes with Automatic off, replacing a one-song request and later returning to automatic, paused queuing, preview exit, no eligible target, shuffle cycles and indexed behavior with 70,000 extra tracks. Inspector pause reporting is also exercised.

The native implementation pauses only AFMR's channel, using Minecraft's existing sound executor and an explicit per-sound flag. A NeoForge streaming-source event checks that flag after a delayed stream starts; the SoundEngine mixin reapplies it after Minecraft's global resume. The exact cached Minecraft 26.3 compiled class was checked with javap for the shadow field and play/tick/resume/stopAll injection targets. The registered mixin and new key translations are included in the release package.

Compilation, automated tests and packaging pass. Actual OpenAL song-position retention, delayed stream start, global menu-resume interaction and physical key/Stream Deck input remain for the user's later requested native tests. No desktop automation, live configuration changes or personal music writes were performed. The new cases are recorded in `DEFERRED-TESTING.md` and `TESTING.md`.

## 0.5.1 checks

**205 tests pass.** Seven new regressions cover starting both override modes with automatic selection off, a one-song return without enabling its source, turning Automatic off during a one-song override, individually disabled songs and fully disabled song pools, inspector reporting, and a saved keybind targeting an off playlist. The existing held-override test now verifies that turning Automatic off retains the override and its subsequent shuffled song, while deletion still releases it. Empty and missing target requests still leave current music alone.

The override menu no longer gates playback buttons on automatic eligibility. Playlist settings label the switch **Automatic**, override/import rows distinguish **auto off**, priority rows retain default/custom status, and the inspector reports **Automatic selection disabled** even when that playlist is being overridden. The switch is not changed by an override. No new persistence format or allocations in the playback path were added.

The full Java/NeoForge build and packaging checks pass. Native menu/keybind checks for this fix remain deferred at the user's request; no desktop automation or changes to live game configuration/music were performed. The user's broad successful manual report applies to 0.5.0. `DEFERRED-TESTING.md` retains the deeper-test list with the corrected switch behavior.

## 0.5.0 checks

User follow-up: most manual testing worked to expectations. This is a broad user report; individual scenario results were not supplied. `DEFERRED-TESTING.md` tracks complex interactions for a later requested in-game test session.

**198 tests pass**, adding 41 checks to the prior 157. Nine shuffle tests exercise full unique cycles, boundary avoidance, the Fisher-Yates decreasing-prefix random bounds, deterministic seeded output, additions/removals without replaying already drawn songs, fresh metadata, temporary empty eligibility, single/duplicate IDs and independent playlist history.

Eighteen override checks cover immediate start from silence, immediate normal-duration fade despite finish protection/context buffer, forced Combat without combat/global triggers, one-song return to the current environment with normal silence, held mode through context changes and shuffled next-song selection, skips/releases, disabled/empty/missing/deleted targets, synchronous and asynchronous failure with retry, previews, combat death policy and waiting for respawn, reset, zero-gap completion, shared automatic/override shuffle history and guarded 70,000-track lookup.

Nine inspector checks distinguish disabled/manual-only/unmatched/empty/winning/shadowed states, shared custom failure predicates, actual condition values, silence/buffer/death/finish protection/preview/fade/override reasons, automatic choice during overrides, per-assignment availability counts and guarded large-library diagnostics. Five settings checks cover manual-only exclusion, eight slot defaults, owned snapshots and JSON round trips, absent field defaults and malformed binding normalization. Inspector counts do not stat every audio file; saved entries are validated when opened for playback.

The Java/NeoForge build and release packaging checks pass. The inspector refreshes only while open, and shared predicate diagnostics add no string allocation to steady playback. No new threads or external dependencies were introduced. Physical keybinds, Stream Deck input, GUI layout/hover behavior and native gameplay remain for the user's manual testing. No desktop automation or writes to personal music, foobar files, or live game configuration were performed. See `TESTING.md` for the setup/checklist.

## 0.4.2 checks

**157 tests pass**, including 19 new transition checks. The four entry/exit combinations verify either toggle can let a song finish, while both off retain the normal buffer, fade and full silent gap. Other checks cover the unrelated toggle directions, original-source ownership through A → B → C, returning to the source, no matching playlist, removed sources, shared track assignments, manual skip/previews, source reset for the next song, priority changes, separate persisted choices and deep snapshots, existing entry settings with exit absent, and transitions with 70,000 guarded extra tracks.

The existing combat death-fade and cut tests now enable both playlist protections to verify that death policy overrides them. The timing-reset test also checks that the exit choice is preserved. The existing `finishCurrentSong` persisted field remains the entry setting; the new independent field is `finishCurrentSongOnExit`. No format migration or new schema version is needed.

Java/NeoForge compilation, the full regression suite and release packaging pass. Native UI, hover explanations and gameplay checks remain for the user, as requested. No desktop automation, live configuration edits, or personal music/foobar writes were performed. See `TESTING.md` for the four-case manual checklist.

## 0.4.1 checks

**138 tests pass**, including nine new biome-filter tests, five playback-default/settings tests and sixteen ReplayGain tests. Biome coverage includes exact vanilla/modded ID matches, OR within the selection and AND with other filters, disabled/empty behavior, default-playlist fallback, deep snapshots and JSON round trips, retained unavailable mod IDs, loading configurations without biome fields, normalization, and indexed selection with 70,000 guarded tracks. Playback-default coverage verifies fresh settings, a reset limited to the four timing values, existing timing retained on load, current-gap preservation and the 600–1200-second range for subsequent gaps. The snapshot-isolation fixture explicitly sets its custom fade instead of depending on the old three-second default.

ReplayGain checks cover dB-to-linear conversion, track peak limiting, missing/invalid peak behavior, missing/malformed gain, album-only exclusion, case-insensitive Vorbis fields, ID3v2.3/v2.4 TXXX fields, WAV ID3 despite INFO preference, signed PCM boost/attenuation/saturation, bit-exact unity and empty buffers, unreadable metadata, and persisted setting/snapshot isolation. Four generated on-disk fixtures (MP3, FLAC, OGG, WAV) are tagged in the temporary test directory, then read and decoded; the measured RMS amplitude halves as expected, and reading leaves bytes and modification times unchanged. No personal music is used for these tests. The PCM-loop microbenchmark is documented separately in `PERFORMANCE.md`; no gameplay FPS claim is made.

The preset was checked against the exact cached Minecraft 26.3 source (`Musics.GAME`, `Musics.CREATIVE`, and `MusicManager`). Normal game delays are 12000–24000 ticks (600–1200 seconds); AFMR adds no fade or context buffer in the preset. AFMR retains its existing scheduling semantics, including its initial gap and per-playlist transition settings, and does not imitate every special vanilla music delay.

The Java/NeoForge build and packaging checks pass. Per the user's preference to test manually, no desktop automation or native gameplay/UI checks were performed for 0.4.1. See `TESTING.md` for biome selection, layout, persistence and reset checks. No live game configuration, music, or foobar files were changed.

## Automated checks

**108 tests pass:** the previous 78 scheduler, context, storage, progress, performance-regression, audio and foobar checks plus 30 custom-playlist and combat/death checks.

The 16 custom-playlist checks cover optional filters and all-condition matching, wrapped/inclusive time windows, day/night consistency, inclusive negative block-Y ranges, float-precision biome-temperature boundaries, world/dimension-scoped spherical areas, cover depth independent of the default Underground cutoff, water/light/combat filters, creation/naming/default preservation, priority and empty/disabled fallback, protected default deletion, folder unlinking, deep save snapshots, loading existing settings, normalization, and 200 custom rules with a guarded 70,000-track index.

Four combat-tracker checks cover independent attack/damage triggers, quiet-period refreshes, health/absorption and hurt-time observations, global disabling, death, respawn and reset. Ten playback checks cover immediate death-fade start independent of context buffer/finish toggles, instant cut, zero-duration fades, normal silence, preventing or allowing new music while dead, finishing the combat song, custom combat playlists, ambient/preview isolation, and death during an existing fade without a volume jump.

The 0.3.2 cover-sampling fix also passes all 78 checks. The client-heightmap regression is validated in packaged Minecraft below; the existing pure environment tests do not simulate Minecraft chunk packet loading.

The new import checks cover UTF-8 BOMs, Unicode and literal percent/plus/hash characters in native Windows paths, encoded file URIs, relative paths, friendly names from an optional index, duplicate entries, empty playlists, binary `.fpl` and malformed UTF-8 rejection, archive/stream/subsong/invalid-location skips, missing and unsupported audio, progress completion, metadata reuse, fresh metadata during ordinary folder rescans, repeated imports preserving disabled assignments, overlapping destinations, and accurate totals with bounded error details. Tests use generated data, and verify source/index bytes and playlist modification time remain unchanged.

The 14 added checks cover inclusive and negative Underground Y cutoffs, required cover and minimum cover depth, open-sky exclusion from Dark exploration, inclusive light thresholds independent of Y, default/custom priority and disable behavior, empty-playlist fallback, condition snapshots and normalization, normal buffer/fade/full-gap transitions, independent Nether/End overrides that ignore light and cover, insertion of the missing playlist into existing libraries, and condition/priority persistence.

Four index regressions exercise 70,000 tracks with a guarded list that rejects any full-library iteration after indexing, disabled/missing/duplicate vanilla assignments, import and metadata index updates, and an empty active pool after a silent gap. Three save regressions check snapshot isolation from later edits, rapid saves retaining the newest state after shutdown flush, and background write failure preserving an existing file.

The four scan-progress tests cover discovery before metadata reading, per-file completion and filenames, a file removed during scanning, empty/missing sources, and overlapping requests retaining their separate playlist assignments. Progress snapshots are immutable and published directly from the worker; no client task is queued for each file.

Six current-playback checks cover custom playlist eligibility across different biomes/dimensions while in water, disabled/empty playlist explanations, manual previews bypassing disabled playlists and context changes, returning to normal scheduling after a preview and full silent gap, and explicit retries after preview errors.

New checks cover the five-playlist default, exclusion of underground areas from day/night outside, Combat/Home priority and disabling, editable priority, spherical Home boundary and vertical distance, isolation between worlds and dimensions, non-destructive v1 migration with a pre-upgrade backup, library-only imports, and preservation of disabled songs during migrated-folder rescans.

Existing tests cover grace periods, fade-out and full silent gaps, finish-current-song behavior, rechecking context during silence, individual vanilla overrides, failed-track quarantine, skip/reset behavior, JSON and backups, and MP3/OGG/FLAC/WAV decoding. Format fixtures include 24-bit FLAC/WAV and floating-point WAV. Decoded waveform amplitude, length, format, and metadata are checked.

## Packaged Minecraft checks for 0.4.0

Loaded the first packaged 0.4.0 JAR in the isolated Minecraft host with the existing test configuration. The six default playlists retained their order and choices. Verified through the UI:

- The Playlists toolbar entry opens a ranked list with enabled/default/custom status and priority controls. Default deletion is disabled.
- Created and named a custom playlist, captured an independent 64-block location radius, applied the Night time preset, configured a biome-temperature range, and required underwater playback. The temperature screen reported `minecraft:cold_ocean` with base value 0.500, and the preset enabled its filter.
- Moved the custom playlist to the top, then down below Combat; rank numbers updated correctly. Edit songs opened its empty table, with arrows for the seventh playlist tab.
- The scrolling foobar destination selector included the custom playlist. A generated `.fplite` selected through native Explorer added two enabled assignments and reported the fixture's three skipped entries.
- Read-only inspection of the saved JSON confirmed seven playlists, custom rank #2, the captured world/dimension/center/radius, all four configured filters, and the two enabled imported assignments.

The user stopped Computer Use to use their computer and chose to perform the remaining gameplay checks themselves. No further UI automation was performed. Final menu refinements provide six visible priority rows at the small test size and return Edit songs to the existing editor instead of opening another nested editor; these refinements compile and pass the automated suite but were not rechecked visually. Native gameplay/death-screen timing, a restart with the custom playlist, and these final layout/navigation changes remain for manual testing. See `TESTING.md` for a short checklist.

All generated songs, test configuration edits, world activity and screenshots used the isolated workspace instance. No personal Minecraft configuration, saves, foobar playlists or music were modified. The isolated test client's settings are retained for follow-up testing; its pre-test configuration is backed up privately in the workspace and excluded from the source archive.

## Packaged Minecraft checks for 0.3.2

The user's recording shows a dark cave around Y -22 while AFMR 0.3.1 reports Night / Outside. Inspection of Minecraft 26.3 sources confirmed that `OCEAN_FLOOR` is not included in client chunk packets. Client chunks still allocate an empty map for it, and later block updates can partially populate it. Taking the lower of that empty map and the synchronized no-leaves surface map produced a surface at the world's minimum Y, incorrectly rejecting both Underground and Dark exploration. Earlier tests built roofs after joining, which populated the map in those test columns and missed this cold-load failure.

Reproduced with the packaged 0.3.1 JAR in an isolated world: a sealed, unlit chamber at (128, -22, 128) beneath existing terrain was labeled Day / Outside with Day outside active. After saving and closing, the packaged 0.3.2 JAR loaded the same saved chamber without modifying any overhead blocks. It immediately reported Day / Underground / Dark, selected Dark exploration, and played the generated test song.

Placing a torch beside the player switched the active playlist to Underground, confirming the light threshold and underground fallback. An open-sky water column at nighttime, with the player below the configured Underground Y cutoff and more than five water blocks overhead, correctly selected Night outside. Water alone did not activate either enclosed-area playlist.

The replacement uses only the synchronized `MOTION_BLOCKING_NO_LEAVES` heightmap and samples the same local column to remove fluids from the solid-cover height. The result is cached for five game ticks and invalidated when columns or worlds change; the cached world reference is released on disconnect. The scan is bounded by world height, reuses its mutable block position, and does not build full-chunk heightmaps or traverse the music library. The 0.3.2 JAR and source archive contain no recording frames, private settings, or personal music.

All regression commands, generated music, world changes, configuration edits and screenshots used the workspace test instance. The user's Minecraft instance, saves, recording and music were only read.

## foobar2000 sample checks for 0.3.1

Read-only validation against 15 user-supplied `.fplite` files parsed 72,067 entries: 69,950 local references and 2,117 archive-backed entries. All 15 friendly playlist names resolved through the adjacent index. The largest playlist contained 70,910 entries; a scan using the existing 67,882-track metadata snapshot found 66,779 usable tracks, reused metadata for all of them, and reported 4,131 missing/unsupported/archive entries. The scan took approximately 32 seconds on this machine, including existence checks on the user's music drive. This is a sample measurement, not a performance guarantee.

The 15 `.fplite` files and `index.txt` retained their SHA-256 checksums, sizes, and modification times. The importer never opens foobar SQLite databases. foobar2000 was running during validation and some of its own SQLite properties files changed; those files were not written or restored by this work. No personal sample data is included in the source archive.

## Packaged Minecraft checks for 0.3.1

The packaged JAR was loaded in the isolated Minecraft host. **Import playlist** displayed all six existing destinations, including a disabled Night outside playlist. The native Explorer picker selected a generated `.fplite` fixture. Importing added its new linked WAV, retained an existing disabled song, and left the destination disabled. The report resolved the generated friendly playlist name and displayed one added song, one existing assignment, one duplicate, three skipped entries (missing/unsupported/archive), and reused metadata. Saved JSON retained exactly six environment playlists and the expected on/off assignments.

The corrected Explorer dialog starts inside foobar's playlist folder with only `.fplite` entries visible. Repeating the generated import added zero songs, reported two existing assignments and two reused metadata entries, and retained the disabled choices. Report pagination worked at the small test window size. Hovering a truncated missing-file detail displayed its full path and error in a wrapped tooltip within the window.

## Packaged Minecraft checks for 0.3.0

The final packaged JAR was loaded in the isolated client using the previous five-playlist test configuration. It displayed six tabs, retaining existing assignments and adding an empty Dark exploration playlist with automatic vanilla inclusion off. A generated music file was added to the new playlist through the editor.

Verified in the disposable world:

- The new Underground conditions screen displays the inclusive Y cutoff (initially 64), minimum cover, and a Use current Y button. Capturing the player at block Y 70 updated the field to 70 and persisted it.
- An enclosed unlit stone room at Y 70 activated Dark exploration while the Underground cutoff was still 64. After raising the cutoff to 70, Dark exploration retained its higher priority.
- Adding a nearby torch switched playback to Underground with the updated Y cutoff and required cover.
- Removing overhead cover at nighttime selected Night outside, with Dark exploration no longer matching.
- The Darkness conditions screen displays the light limit, live local light and cover information, and independent Nether/End switches. Both start off; enabling them persists separately.
- With the switches on, Dark exploration activated above the Nether roof and in an open End location. Neither location required overhead cover.
- Normal playback, UI navigation, metadata/artwork, and shutdown continued working. Final saved settings retained the captured Y, light limit, dimension switches, and playlist assignment.

All world commands, generated audio, configuration edits and screenshots used the isolated workspace test instance. No personal Minecraft saves or live user configuration were modified.

## Packaged Minecraft checks for 0.2.2

The packaged JAR loaded in the isolated Minecraft host with 67,812 generated local entries. The editor opened, expanded All local music, displayed and played the generated test song, disabled and re-enabled its assignment, and shut down successfully. The saved JSON retained all library entries and the final enabled assignment. Folder expansion and playlist toggles reuse the existing tree; per-frame active/status information uses the scheduler's tick result.

Same-library standalone benchmarks compared the 0.2.1 and 0.2.2 packaged classes, using a private read-only copy of the user's saved metadata (67,882 total tracks). See `PERFORMANCE.md` for timings and limits. Personal music was not decoded or changed by these benchmarks; no private library data is included in the release.

## Packaged Minecraft checks for 0.2.1

The packaged update was loaded in the isolated client. A collection of 12,000 generated MP3 files plus the existing 48 test songs was rescanned: discovery displayed a live found count and animated bar, then metadata processing displayed an advancing processed/total percentage and filename. The editor stayed usable and the scan completed with 12,000 new tracks and no issues. The library-only folder was not assigned to a playlist.

With all automatic playlists disabled, the editor showed `ENV Day / Outside / Home`, `ACTIVE None`, and the disabled Day outside explanation. Selecting a song in the disabled Day outside playlist and pressing **Play selected** started the packaged local audio stream and changed the information panel to Preview. No personal music files or live user configuration were modified.

## Packaged Minecraft checks from 0.2.0

An independent Minecraft development host loaded only the built mod JAR from its `mods` folder, including the nested decoder libraries. It used a disposable test world and generated audio; no user Minecraft installation or personal music collection was changed.

Verified:

- Existing schema-1 configuration upgrades to schema 2, displays exactly five playlists, and creates the pre-upgrade backup.
- M opens the redesigned editor. The left folder tree and center table display compact rows, titles, artist/album data, durations, and album artwork.
- The table displays roughly twenty rows at the standard small client window size; a forty-song generated collection was imported for scrolling and density checks.
- Playlist tabs, folder expansion, whole-folder assignment, row selection, individual disable, and independent table scrolling work with the editor's scaled coordinates.
- Ctrl+A selects the full playlist; bulk disable retains all 48 test assignments while setting their eligibility off.
- Home capture records the player's actual position and radius, persists the result, and activates Home music in the captured area.
- Add folder opens the native Windows Explorer folder-selection dialog and imports its selected directory. The picker can be opened again after completion.
- Add files opens the native multi-file dialog with the MP3/FLAC/OGG/WAV filter, and cancelling returns cleanly to Minecraft.
- Music and the now-playing overlay continue working in the packaged build.

## Scope

This release was checked on Windows. The SDL dialog APIs should use native dialogs on other supported systems, but other operating systems were not tested. Multiplayer Home scope is covered by world/dimension matching tests; a live multiplayer server was not exercised. Scan progress was exercised on 12,000 generated files; larger real-world collections, unusual tagging conventions, and interactions with other audio/GUI mods remain untested. The underground detector is a configurable surface-cover heuristic, not a cave-volume classifier.
