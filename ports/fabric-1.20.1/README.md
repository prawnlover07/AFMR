# AFMR for Fabric 1.20.1

This is the actively maintained Fabric 1.20.1 backport of AFMR 1.1.
The repository's
root `src/` remains the separate NeoForge 26.3 implementation. Edit this port's
sources directly when changing 1.20.1 behavior; it has no runtime compatibility
layer and does not load the NeoForge build.

`.afmrp` export/import supports optional song lists shared with the NeoForge build.
Turn on **Include song list** when exporting. On import, **Search linked libraries**
finds identical song files in your linked folders/catalog across renamed files
and reorganized layouts. **Find song files...** also offers an extra folder search,
individual file selection, filtering, match details and cancellation. Per-song
on/off states are preserved, with missing files retained for
**Library repair / missing sources**. Audio files and absolute music paths are
excluded. Both switches start off. See the root [user guide](../../docs/USER-GUIDE.md#sharing-playlist-setups-afmrp).

The playlist workspace supports Undo/Redo, drag priority and opaque song context
menus with selection counts. The bottom strip supports seeking; click a playing
song title to locate its source playlist. Search settings opens global controls
and the selected playlist's rules. Display provides independent imported
TTF/resource-pack fonts for menus and the widget. Minecraft Music/Master mute
silences playback and shows a warning without pausing the song or scheduling.

## Install

Use a **Minecraft Java 1.20.1** instance with **Fabric Loader 0.19.5 or newer**, a
matching **Fabric API for 1.20.1**, and **Java 17 or newer**. Put
`animals-final-music-replacer-fabric-1.20.1-1.1.jar` from `build/libs/` in that
instance's `mods` folder. The audio decoders are included in the JAR.

Press **M** to open the music workspace. Import files or folders with **Add music**;
dragging supported files onto the workspace also works. A manual playlist
override bypasses the default initial silence. Normal automatic playback keeps
the existing Minecraft-like 600–1200 second gaps unless changed in Timing.

Use a separate game instance/configuration for this target. Do not open a world
created in a newer Minecraft release in 1.20.1. Music and foobar data stay read-only.

## Scope

**Playlist Settings → When it plays → Show rules → Riding animal** supports horseback/other animal-mount
playlists with Any/Riding/Not riding choices. It uses the ordinary client riding
state and works in multiplayer without a server mod.

The playlist workspace, filters/nested groups, priorities, manual overrides,
entry/exit finish controls, independent fades, volume, queue/history, profiles,
library repair, `.afmrp` sharing, metadata, ReplayGain and loudness analysis are
retained. Structures and combat targeting use owned, bounded server observations. The optional
`animals-final-music-replacer-server-fabric-1.20.1-1.1.jar` adds these observations
to Fabric servers (with Fabric API). Install one AFMR JAR per server; clients keep
the ordinary client JAR. Vanilla servers remain supported with unavailable structure
data and attack/damage combat timers.

The sole now-playing style is the **AFMR album-art card**. Minecraft's newer native
toast/pause-menu song display, Hybrid style, Music Frequency synchronization and
menu blur are removed. Background opacity, card position, size, information and
text scrolling remain available together under **Display → Widget**, with scrolling
on smaller windows. Menu appearance stays under **Menus**.
**Display → Widget → Custom background…** imports an independent PNG/JPEG/static
WebP or animated GIF for the card, with on/off, Fill/Fit/Stretch, image opacity,
preview and removal. Transparent pixels show the game; unavailable images fall
back to the card color. Full backups include both widget and menu images.
**Inspector → Copy diagnostic summary** copies versions, safe playback/context
state and numeric diagnostics locally, using priority numbers instead of names.
It omits private paths, world/server identifiers, saved locations and raw errors.
**Display → Menus → Custom background…** imports
PNG/JPEG/static WebP and animated GIF through Explorer, with independent image opacity and
Fill/Fit/Stretch sizing. The port renders them below AFMR panels without blur.
Missing artwork uses a static music note.
**Display → Menus → Menu colors…** customizes menu backgrounds, panels, buttons,
hover/selection, highlights, text, borders and scrollbars with live RGB previews.
The palette is shared across profiles. Reset colors keeps opacity and image/HUD
settings; the full menu reset restores all menu appearance defaults.
GIFs loop with their timing/transparency, resize to a bounded frame budget, and are included in
full configuration backups. Original files remain untouched.
Click any menu-color or widget-background swatch for a visual hue and
saturation/brightness picker, presets and synchronized hex entry. Apply keeps
the live preview; Cancel/Escape restores the opening color.
Vanilla music and selectable content reflect the installed 1.20.1 resources/mods.
Scroll song columns with their horizontal bar, Left/Right, or Shift + wheel.

Native open/save/folder dialogs use Tiny File Dialogs bundled with Minecraft's
LWJGL, on an AFMR-owned picker worker. There is no SDL dependency.

## Build and verification

Author-written descriptions and hover explanations come from the repository's
shared `docs/UI-COPY.md`. Edit only each **Text to edit:** paragraph; the Fabric
build packages the applicable shared and Fabric-specific entries automatically.
Run `.\gradlew.bat validateUiCopy` here for wording checks. The standalone source
ZIP includes `docs/UI-COPY.md`, `gradle/ui-copy.gradle` and
`copy/utility-en_us.json` at its own root, with the same build commands.

Covered conditions now use Minecraft 1.20.1's synchronized motion-blocking
heightmap. The previous no-leaves map was empty in newly received client chunks,
making caves count as open sky. Water/leaves are still trimmed from the sampled
column. See the [cover regression report](../../docs/testing/2026-10-22-fabric-cover.md).

The Fabric album-art stack-space crash is fixed in the current source. See the
[investigation and measurements](../../docs/testing/2026-10-22-fabric-artwork-memory.md).
`-Psmoke artworkMemoryProbe` runs the real Minecraft/STB image decoder without a
game window, reproduces the old API failure, and stress-checks the replacement
stream API. Optional `-PartworkJava=<java executable>` selects a runtime such as
Java 17. `-PartworkPathFile=<private text file>` reads a local music path for
read-only artwork verification; keep that file and raw logs out of Git.

Build using **JDK 25** and the included Gradle wrapper. The shipped classes target
Java 17; the copied regression tests use the development JDK's collection APIs.
From this directory:

```powershell
.\gradlew.bat build
.\gradlew.bat sourceZip
```

Or from the repository root, pass `-p ports/fabric-1.20.1` to the root wrapper.
Builds need official Fabric/Minecraft and Maven Central dependencies on first use.
The tests use Minecraft 1.20.1's Gson 2.10 to check the actual JSON API behavior.

Run `python testing/prepare_smoke.py` to prepare generated fixtures.
`-Psmoke runClient` additionally loads the **test-only** `src/smoke` mod, creates
`run-smoke/`, generates a disposable world, opens screens and measures native
audio. Add `-PpackagedSmoke` to test the actual remapped JAR from `run-smoke/mods`,
without loose AFMR classes or external decoder dependencies on its runtime path.
Never point it at personal profiles. It writes `smoke-results.txt` and screenshots
inside that disposable profile and exits automatically. This test mod is not
included in the release JAR.

For focused background checks, stage generated images from the repository root
with `python testing/prepare_background_images.py --stage` (Pillow), then use
`-Psmoke -PpackagedSmoke -PbackgroundImages runClient`. It writes
`background-images-results.txt` and `background-*.png` screenshots in `run-smoke`.
The mode is independent of specific default playlist IDs. `-Psmoke menuBackgroundProbe`
runs 100 large-PNG native decode/close cycles without a game window.

`-Psmoke -PpackagedSmoke -PwidgetSettings runClient` focuses on the consolidated
Widget/Menus tabs, row scrolling/drag, drafts, color picker, preview and layout.
It writes `widget-settings-results.txt` and `widget-settings-*.png` in `run-smoke`.

See [the port validation report](../../docs/testing/2026-10-22-fabric-1.20.1.md)
for measured results and remaining checks.

Full configuration transfer is available under **Import / export → Full configuration…**.
`.afmrb` backups include all AFMR configuration, profiles/playlists, library references,
AFMR keys, history and the custom background. Review before replacing on restart;
use Library repair if music paths changed. Music files stay separate. The same
archive works across both maintained builds; unavailable native-toast/blur controls
remain unavailable on this target, and modifier shortcuts are reported and unbound.

**Timing → Play immediately on join** defaults off, is saved per profile, and skips
only the first eligible song's initial silence/context buffer after a world/server
join. Normal fade-in and later song timing remain. The final source passes 788
automated tests; see the [release verification](../../docs/testing/2026-10-23-release-1.0.md)
for native coverage and limitations.

`-Psmoke -PpackagedSmoke -PplaylistSilence runClient` checks per-playlist silence
with a generated WAV: controls, validation, hide/resize, saved ranges, a real
three-second Skip gap and actual OpenAL playback. Results are
`run-smoke/playlist-silence-results.txt`. Only the disposable profile is changed.

AFMR's code and documentation use the [MIT license](LICENSE), copyright 2026 animal.
[Bundled components](THIRD_PARTY.md) retain their separate licenses.

`-Psmoke -PpackagedSmoke -PgameplayNormalization runClient` verifies manually requested
normalization in the unpaused disposable singleplayer world, including progress
screen/Back/Cancel, partial results, normal saves and continued music. Results are
`run-smoke/normalization-singleplayer-results.txt`. Analysis also runs during
multiplayer; its scanner no longer takes a world/pause gate.

`-Psmoke -PpackagedSmoke -PproximityVolume runClient` exercises the distance-volume
editor and actual OpenAL gains with generated music and a synthetic client-visible
player. Results use `run-smoke/proximity-volume-results.txt`; this is not a real
two-player remote-server test.
