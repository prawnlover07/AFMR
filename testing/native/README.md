# Native AFMR fixture

This is the test-only mod used for the October 21, 2026 validation of AFMR 0.5.2,
relocated into the permanent repository. It loads the packaged AFMR JAR and measures
actual OpenAL source state, offsets and gain through Minecraft's sound executor.
It is a separate Gradle project and is never bundled into the AFMR runtime JAR.

The native suite changes the isolated game configuration, kills/respawns the test
player, teleports between dimensions, and builds a disposable room. Use a disposable
world and coordinate desktop testing with the user. Do not use a personal game profile.

## Prepare

All disposable clients run capped at **60 FPS**, with VSync off, at the owner's
request because higher rates cause desktop lag. `runClient` depends on
`capProbeFps`, which updates only the selected test profile's two video options
before Minecraft reads them. This covers title screens, first launches and every
native mode. `prepare_fixtures.py` also seeds new options with the cap. Fabric's
`-Psmoke` launch applies the same policy to `run-smoke`. Other saved options and
personal game profiles are preserved. Do not uncap performance probes without a
new explicit request. Earlier uncapped performance reports remain historical.

For warning-only Minecraft mute behavior, use `runClient -Pworkspace -PmuteWarning`.
Fabric uses `-Psmoke -PpackagedSmoke -PmuteWarning runClient`. These focused checks
expect silent position advancement, exact zero source/listener gain, retained mute
warning and independent manual pause; results go to `mute-warning-results.txt`.
The former automatic pause policy has been removed. The October 25 overnight pass
ran the warning/continuation checks on both targets; the focused mode also checks
natural completion, silence and new native streams while muted. Coordinate future
desktop runs with the owner.

For the recent import/repair/theme/widget/Controls audit, run
`runClient -Pworkspace -PovernightAudit`. Add `-PpickerAudit` only when an operator
can drive the actual Windows dialogs: the helper writes each requested generated
path to `run/workspace/audit-fixtures/picker-request.txt` and waits for the dialog
callback. It never automates the operating system itself. The final October 25
audits passed 70 checks on each target, including six real picker callbacks.

For additional live mounts, use `runClient -Pworkspace -PridingAudit`.
It checks the paged Riding animal control, horses/donkeys/mules/camels/pigs/striders,
boats/minecarts, dimension transfer and death/respawn. The same NeoForge mode can
use `-Pmultiplayer -PridingAudit` with the existing disposable loopback server.
Results use `riding-audit-results.txt` in singleplayer and
`riding-multiplayer-audit-results.txt` on the separate vanilla server.

For sustained native audio/resource ownership, use
`runClient -Pworkspace -PplaybackSoak`. It runs ten minutes of generated short
music, folder artwork and an animated widget image, with muted starts, ordinary
gaps, queue requests, manual pauses, profile swaps and two real resource reloads.
It records post-GC heap and direct-buffer observations, channel/texture bounds and
the 100-entry history limit. A short soak does not establish behavior in every
modpack or rule out every long-session leak. Immediately follow it with
`runClient -Pworkspace -PplaybackSoak -PsoakRestart` to verify actual restart
persistence and fresh native playback of the saved fixtures. Run restart before
another mode replaces the disposable configuration.

Fabric equivalents use `-Psmoke -PpackagedSmoke` instead of `-Pworkspace`.
`-PsmokeJava=/absolute/path/to/java.exe` selects the client runtime independently
of Gradle's Java; use Java 17 for the Fabric minimum-runtime check. The image-only
`artworkMemoryProbe` and `menuBackgroundProbe` tasks accept `-PartworkJava=...`.
Both targets retain the 60 FPS cap with these modes.

For player proximity volume, use `runClient -Pworkspace -PproximityVolume`.
This focused Feature101 mode uses generated audio and one synthetic client-side
RemotePlayer, checks editor input and independent trigger radius, and measures
actual OpenAL full/half/zero, reverse, untracked fallback and retained off/on gain
without restarting the song. Results are `proximity-volume-results.txt`; the
editor screenshot is `features-101-proximity-volume.png`. No second server player
is simulated by a network connection. Master gain affects the targets differently,
so endpoint checks compare against the measured full reference gain.

For manual normalization during gameplay, prepare a profile and run
`runClient -Pworkspace -PgameplayNormalization` for singleplayer or
`runClient -Pmultiplayer -PgameplayNormalization` against the existing loopback vanilla
server. The focused helper tests actual Normalize/Back/Cancel controls, unpaused
completion, continued music, partial results, unchanged source bytes and saved
measurements. Results use `normalization-singleplayer-results.txt` and
`normalization-multiplayer-results.txt`. If a server was launched without an
interactive console, `-Pmultiplayer -PgameplayNormalization -PstopServer` joins solely to
send the vanilla operator stop command and closes the helper; it does not rerun
or overwrite the measured checks. Restrict this helper to the disposable server.

For per-playlist silence, build and prepare the workspace, then run
`runClient -Pworkspace -PplaylistSilence`. The focused mode creates a playlist from
an existing generated long WAV, checks range validation and retained drafts, and
measures a real three-second Skip gap. Results use `playlist-silence-results.txt`;
screenshots use `workspace-playlist-silence-*.png`. The ten-minute case verifies
its scheduled countdown without waiting ten minutes. Close the screen before
leaving the test world so profile activation cannot invalidate its fixture ID.

For the consolidated Display → Widget panel, build and prepare the workspace,
then run `runClient -Pworkspace -PwidgetSettings` in this test project. It checks
the actual tab/control layout, drafts, style restrictions, scrolling and drag,
picker/preview, resize and scrolled tooltip bounds. Results use
`widget-settings-results.txt`; screenshots use `workspace-widget-settings-*.png`.

For WebP/animated GIF checks, build and prepare the workspace profile, then run
`python testing/prepare_background_images.py --stage` from the repository root
(requires Pillow). Run `runClient -Pworkspace -PbackgroundImages` in this test
project. Results use `background-images-results.txt`; screenshots use
`workspace-background-*.png`. This exercises packaged decoding, real frame textures,
fast-import UI refresh, animation export, invalidation/reload, on/off and removal.
`menuBackgroundProbe` runs the large-PNG native decode/close stress case without a
game window, unpacking only the packaged nested libraries into the test build folder.

For focused visual picker checks, prepare the workspace profile, then run
`runClient -Pworkspace -PcolorPickers`. Results use `color-pickers-results.txt`
and `color-pickers-complete.txt`; screenshots use `workspace-color-picker-*.png`.
The helper opens every menu color and the widget picker, exercising actual pointer
dragging, arrows, hex/preset sync, Apply/Cancel, hide/reopen and style availability.

For focused menu-color UI/rendering checks, prepare the workspace profile, then
run `runClient -Pworkspace -PmenuColors`. This mode writes
`results/menu-colors-results.txt` and `menu-colors-complete.txt`, and captures
`workspace-menu-colors-*.png` screenshots. It checks actual screen controls,
invalid/hidden drafts, live custom RGB/alpha render states and palette reset.
See [the report](../../docs/testing/2026-10-22-menu-colors.md).

From the AFMR repository root, with Java 25 and Python 3 available:

```powershell
.\gradlew.bat build
python testing/native/prepare_fixtures.py --scan-files 5000
```

This generates quiet WAVs, a .fplite fixture and optional scan files, stages the root
build's JAR in `testing/native/run/mods`, and initializes missing test settings. Existing
test options/configuration are preserved. Other installed AFMR versions are reported
rather than removed. All generated files/worlds/results are ignored by Git.

Supply `--world <path>` to copy an existing disposable world to `run/saves/New World`
when that destination does not already exist. The original is only read. The imported
workspace's `work/overnight/run/saves/New World` is an available previously used test
world on this computer. Otherwise, create a new test world named **New World** in the
isolated client; the helper starts after joining a world and waiting for startup.

## Compile or run

```powershell
.\gradlew.bat -p testing/native compileJava
.\gradlew.bat -p testing/native runClient
```

`compileJava` does not open Minecraft. `runClient` opens the isolated profile and
quick-plays New World. The helper drives real runtime APIs and local server commands;
physical input, Explorer dialogs and visual checks are separate manual tests.

Results are `testing/native/results/native-results.txt` and `complete.txt`. A fresh
playback run has 81 feature checks. Extra persistence checks run if the UI Test Playlist
fixture is present. An intentionally invalid MP3 exercises error quarantine/recovery;
that expected decode error must not be mistaken for an unexpected failure.

## Saved UI checks

### Workspace overhaul

The `WorkspaceSuite` helper drives actual Minecraft screen events and asserts
the resulting library/settings/runtime state. It uses its own `run/workspace`
profile; the older native profile remains separate. Prepare it after building:

```powershell
python testing/native/prepare_fixtures.py --workspace --world "work/overnight/run/saves/New World"
.\gradlew.bat -p testing/native runClient -Pworkspace
```

Pass `--world` only on first preparation; the destination must not already exist.
Only the copied disposable world and generated WAV fixtures are used. This run
opens a Minecraft window and closes it on completion. Results go to
`results/workspace-results.txt` and `workspace-complete.txt`; renderer screenshots
go to `run/workspace/screenshots`. Checks include same-screen assignments, priority,
independent entry/exit, overrides/transport, navigation and drafts, all eight slots,
import destinations, normalization access, and create/delete. The checks also
observe rendered tooltips at the scaled workspace's right and bottom edges, on a
timing field, and on a disabled display button. They assert screen bounds and
mouse anchoring and save `workspace-tooltip-*.png` screenshots. These events do not
establish physical keyboard behavior or Windows Explorer interaction.

The current workspace run has 179 assertions. It also edits default templates and
groups, clones a playlist, exercises Autoplay here's tap/short-hold/pointer-exit/
navigation cancellation and completed hold, verifies cover clearing through silence
and a no-cover song, cycles every widget corner, and records all four AFMR corners,
Hybrid top-right and native bottom-right. Generated `fixtures/with-cover` audio and
a tiny orange PNG supply artwork without personal files. Native popup visibility
and focus-loss pausing are temporarily configured only in the isolated client and
restored afterward. These checks do not change death/respawn playback semantics.

Timing checks validate independent Fade in/Fade out fields, invalid bounds, hidden
drafts, saving and the four-state Jukebox selector. Generated WAVs and actual local
server jukebox placement/disc insertion/removal exercise all three enabled modes,
same-sequence resume, a full post-record gap, immediate restart despite a buffer,
overlapping records and muted Records volume. These workspace assertions inspect
GameAudio's pause intent, not OpenAL sample offsets; the older native suite's channel
measurements do not automatically validate this new hold path. See the
[fades/jukebox report](../../docs/testing/2026-10-22-fades-jukebox.md).

Song-column checks open the paged chooser, enable every optional field, retain
choices while hidden, inspect cached metadata cells, sort numeric track numbers
through horizontally scrolled headings, drag the horizontal scrollbar, toggle the
pinned On switch and preserve assignment state when selecting a scrolled row.
They verify global visibility across playlists and Defaults clearing overflow.
The generated fixture tracks receive metadata in memory; actual MP3/FLAC/OGG/WAV
tag parsing and read-only import behavior have separate component tests. See the
[song-columns report](../../docs/testing/2026-10-22-song-columns.md).

Display → Menus checks exercise the live opacity slider, blur toggle and reset,
independence from widget opacity, background alpha and fully opaque text in actual
native render states, and unchanged non-pausing behavior. Settings and workspace
screenshots cover 0%, 50%, 80% and 100%, plus blur on/off. Full opacity skips the
hidden blur pass. Physical dragging and unusual window/GUI-scale combinations
remain manual checks.

For the six persistence checks from the original session, use the isolated UI to:

1. Import all 5,000 scan fixtures into the library.
2. Create **UI Test Playlist**, capture **minecraft:dappled_forest** in its biome filter,
   enable both entry/exit finish switches and move it to priority #1.
3. Restore Minecraft playback defaults. Keep test Next/Pause mappings **N/P**.
4. Pause AFMR and close the client normally.

Then run `.\gradlew.bat -p testing/native runClient -PkeysOnly`. This checks those six
saved behaviors plus fixture presence, without resetting the configuration or running
the playback suite. The exact biome expectation reflects the original disposable world;
adapt it explicitly if using a different world. Saved key names are compared rather
than old GLFW key codes, because Minecraft 26.3 uses SDL input.

Full playback runs reset custom test playlists and music before starting. Preserve
the UI fixture if you intend to run the separate persistence test afterward. See the
[session report](../../docs/testing/2026-10-21.md) and [remaining cases](../../DEFERRED-TESTING.md).

The helper and fixture preparation compile/check after relocation. The historical
native/UI results are the recorded October 21 session; relocation alone is not a new
native test run. Screenshots and validated case names are tracked with the report.

## Combined release checks

The newer `IntegrationSuite` uses its own `run/integration` profile. Its first
pass expects fresh AFMR test settings and an unused `integration-fixtures` folder;
it creates/moves generated WAVs, drives queue/history/profile/repair/sharing
screens, samples OpenAL on Minecraft's existing sound executor, and validates
shutdown ordering. Never point it at personal configuration. Prepare after building:

```powershell
python testing/native/prepare_fixtures.py --integration --world "work/overnight/run/saves/New World"
.\gradlew.bat -p testing/native runClient -Pintegration
.\gradlew.bat -p testing/native runClient -Pintegration -Prestart
```

Use `--world` only when the copied destination does not exist. The restart pass
requires a successfully completed first pass. `-Pinteractive` leaves the client
open for actual Explorer/input checks afterward. Results are
`results/integration-results.txt` and `integration-restart-results.txt`. Source
music is generated, checks include byte preservation, and no personal game is used.

`--fresh` / `-Pfresh` use `run/fresh`, deliberately without a prewritten AFMR
library file or test key assignments, to check the actual first-install defaults
and vanilla playback. Preparation refuses an existing AFMR config in that profile.

`--performance --library <read-only-library.json>` / `-Pperformance` use
`run/performance`. Preparation stages only the local track metadata and linked
folder paths, omitting playlists, world/server links, appearance and history.
The probe compares small/full catalogs, 67k-scale assignment data, workspace
opening, a read-only rescan and post-scan gameplay. It retains bounded timing
arrays, measures client-tick/Post event spans and Minecraft FPS, and does not
claim those spans are isolated AFMR-only execution time. Minecraft's AFK frame
limiter is disabled only in this test client. `-PskipScan` repeats the catalog/UI
comparison without doing another filesystem scan. Results contain aggregate
measurements; raw game logs and staged metadata remain ignored/private.

The existing original playback run accepts `-PautoExit` to close its disposable
client once complete; its default behavior still leaves it open for manual checks.

See the [combined report](../../docs/testing/2026-10-22-release-validation.md)
for measured results, fixture corrections and remaining limits.

## Vanilla multiplayer suite

`MultiplayerSuite` runs against an **unmodified Minecraft 26.3 server**, with AFMR
and the probe installed only on the client. It uses another isolated profile,
`run/multiplayer`, and quiet generated audio. The server must be disposable: tests
change time, blocks, dimensions, game mode, and kill/respawn the test player.
Coordinate the client launch with the user. The fixture sends ordinary vanilla
commands as an operator; AFMR itself needs no commands, permissions, or networking.

Prepare the client after building AFMR:

```powershell
python testing/native/prepare_fixtures.py --multiplayer
```

Obtain the official vanilla 26.3 server JAR. This machine's Gradle cache already has
one at `work/gradle-cache/caches/neoformruntime/artifacts/minecraft_26.3_server.jar`.
Read the [Minecraft EULA](https://aka.ms/MinecraftEULA); use `--accept-eula` below
only if you agree. The preparation script never starts the server:

```powershell
python testing/native/prepare_multiplayer_server.py --server-jar <path-to-vanilla-26.3-server.jar> --accept-eula
```

It creates `work/multiplayer/server`, binds to **127.0.0.1:25575**, disables remote
administration, and grants the offline **AFMRProbe** test player operator access.
Keep this offline test server restricted to loopback. Existing server properties
are preserved and checked; existing worlds are reused rather than reset.

In that server directory, start it using Java 25:

```powershell
java -Xmx1G -jar server.jar --nogui
```

After the server reports `Done`, run from the repository root:

```powershell
.\gradlew.bat -p testing/native runClient -Pmultiplayer
```

The client joins automatically, checks native audio and synchronized context, then
disconnects/reconnects and closes itself. Results are `results/multiplayer-results.txt`
and `results/multiplayer-complete.txt`. Stop the server normally with `stop` afterward.
The current suite has 39 checks; its attack case calls AFMR's observation API, whereas damage
and death come from real server updates. Physical input and UI layout remain separate
checks. The normalization case verifies manual analysis completes online and its
measurements survive logout. It does not benchmark gameplay performance.

The extended profile checks bind an inactive copy, reconnect to activate it,
verify independent playlist volume, travel between dimensions without switching
profiles, unlink and reconnect to Default, and retain server-address identity
despite a changed display name. These extra travel checks use Creative mode;
the intentional death/respawn checks use Survival. A dead test player is respawned
before starting a repeated run. See the
[combined release report](../../docs/testing/2026-10-22-release-validation.md).

The 0.6 run also edits all four gamemode choices, retains hidden drafts, observes
actual integrated-server mode updates and automatic source changes, and checks
Play on trigger with a one-second fade against a five-second buffer/ten-second gap.
Manual skip retains its gap. Time-screen checks save fitting, discover a generated
90-second WAV's missing duration on the library worker, keep priority/silence when
none fit, and resume after widening the range. The disposable clock is paused to
stabilize the range budget; no natural-clock end-to-end timing benchmark is claimed.


### Windowless wallpaper check

After building AFMR, `./gradlew -p testing/native menuBackgroundProbe --offline`
generates a detailed 4096×1024 wallpaper, imports/reloads its managed copy and runs
100 actual Minecraft native stream decode/close cycles without a game window.
It verifies source preservation, bounded dimensions, released native pixels and
stable LWJGL scratch usage. It does not validate GPU registration or rendered blur.
The Fabric counterpart is `./gradlew -Psmoke menuBackgroundProbe --offline` from
that port; `-PartworkJava=<Java 17 executable>` runs it on a selected game runtime.

## Whole-configuration transfer and seam checks

With the workspace fixture prepared, use `runClient -Pworkspace -PconfigBackup`
for ten real-screen export/review/cancel/stage checks. Restart with those flags
plus `-PbackupRestart` for six startup checks. `-PbackupFromFabric` expects the
generated `fabric-import.afmrb` to have been copied into the isolated profile and
staged as its `config/afmr/pending-restore.afmrb`; it verifies seven transfer cases.
Adding `-PmenuSeams` repeats sixteen exact render-state coverage/alpha cases at
four canvas sizes, with and without table overflow. Results are separated by mode
under ignored `results/`. Use only generated exports; never stage the user's setup.

## First song on join

Prepare ordinary generated fixtures/worlds first, then run
`python testing/prepare_join_playback.py` from the repository root to replace only
the disposable workspace configuration. It binds a profile with the join switch
on to `New World`, leaves Default off, and uses a 600-second gap / 60-second buffer.
Run `runClient -Pworkspace -PplayOnJoin` for actual first-join selection, Timing
controls/validation/retention and later normal Skip assertions. Use `--fabric` on
the fixture script and `-Psmoke -PpackagedSmoke -PplayOnJoin` on the port build for
its separate actual-join/OpenAL checks. Fixture staging does not open personal
music or touch a real install. Join-mode logs remain in ignored result folders.

## Final copy, clipboard and animated restore review

After staging the workspace fixtures, place the generated test GIF from
`src/test/resources/backgrounds/disposal.gif` in that disposable instance's
`background-fixtures/` folder. Run `runClient -Pworkspace -PfinalReview`, then
restart with the same flags plus `-PreviewRestart`. This mode owns its generated
playlist and clipboard report, checks the actual screens/native sound and stages
a reviewed `.afmrb` restore. It replaces the system clipboard with AFMR's generated
test report. Coordinate desktop use first. Results are `final-review-results.txt`
and `final-review-restart-results.txt` under the ignored results directory.

The independent Fabric helper uses `-Psmoke -PpackagedSmoke -PfinalReview runClient`
and adds `-PreviewRestart` for the second process. Its fixture belongs in
`ports/fabric-1.20.1/run-smoke/background-fixtures/`; it uses the disposable
`AFMR Smoke` world and stores results/screenshots inside `run-smoke`.

## Song groups and player themes

For directional playlist transitions, add `-Ptransitions` to the commands below.
That selects a focused generated-audio/menu sequence and writes separate
`transitions-results.txt` evidence. It retains the disposable-profile requirement
and needs current authorization to take desktop focus.

After staging the current packaged AFMR JAR in the disposable workspace profile,
run `runClient -Pworkspace -Pfeatures101` from this test project. The generated
fixture helper checks grouped real audio, delayed notifications, assignment/count
editors and a synthetic client-visible player. Fabric's independent equivalent is
`-Psmoke -PpackagedSmoke -Pfeatures101 runClient` from its port project. Coordinate
desktop use first. See the 1.0.1 feature report for results and untested network cases.

## Inline playlist settings

Use the disposable workspace profile and generated fixtures:

```powershell
python testing/native/prepare_fixtures.py --workspace
.\gradlew.bat -p testing/native runClient -Pworkspace -PinlineSettings
```

The separate `InlineSettingsSuite` checks real screen input, checkbox/edit
separation, negative-condition restoration, area capture, invalid numeric drafts,
hide/resize retention, inline timing/volume, independent finish flags, right-click
rename/duplicate and the one-second replacement hold. Results are in
`testing/native/results/inline-settings-results.txt`; screenshots stay in the
ignored workspace profile. It does not use personal music or the desktop pointer
API. For Fabric, use `-Psmoke -PpackagedSmoke -PinlineSettings runClient` from its
project directory; results are in `run-smoke/inline-settings-results.txt`.

## Playlist follow-up, fonts and mute

After building/staging the runtime JAR, `runClient -Pworkspace -PuiFollowup`
selects the dedicated UiFollowupSuite. It uses generated WAV/PNG/TTF fixtures,
checks current workspace input and native font-resource reloads, then samples
actual OpenAL pause state and offsets on Minecraft's sound executor. Fabric's
matching mode is `runClient -Psmoke -PpackagedSmoke -PuiFollowup` from the port.
It excludes the ordinary workspace/smoke driver to avoid competing inputs.
Results use `ui-followup-results.txt`; screenshots use `ui-followup-*.png`.
The fixture font is original, generated by `testing/generate_font_fixture.py`.
See the October 24 follow-up report for measured results and coverage limits.

## Player clarity and reversible themes

`runClient -Pworkspace -Pclarity` selects the focused clarity checks inside
UiFollowupSuite. Fabric uses `runClient -Psmoke -PpackagedSmoke -Pclarity`.
The mode uses the generated long WAV and a disposable profile capped at 60 FPS.
It checks song actions, source/editing distinction, readable rules, waiting labels
and the current ThemePreview flow. Results use `clarity-results.txt`; rendered
screenshots use `ui-followup-clarity-*.png`. It does not rerun the older broad
workspace or mute-warning scenarios.
