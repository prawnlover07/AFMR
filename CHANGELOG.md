# AFMR release notes

These notes preserve the release descriptions previously recorded in README.
They describe changes at those releases; use the [user guide](docs/USER-GUIDE.md)
for current instructions and the [developer guide](docs/DEVELOPER-GUIDE.md) for the
current architecture. They do not imply Git history before the imported `v0.5.2`
baseline. No historical release dates have been inferred.

Pending changes belong under **Unreleased**. Keep the version unchanged while any
development tasks are in progress, including concurrent chats; increment it once
for the combined update only after all in-progress tasks are complete.

## Unreleased

New Minecraft/loader ports are planned; see the support matrix in
[PORTING.md](docs/PORTING.md). Planned builds are not released artifacts.

## 1.1.1 — October 29, 2026

Hotkey update for the existing NeoForge 26.3 / Java 25 and Fabric 1.20.1 / Java 17
builds, with matching optional companions. Additional platform ports follow after
individual verification.

- Adds an unbound **Show now playing widget/toast** key in Minecraft's AFMR
  controls category on both maintained targets. Reopen the current song for its
  usual display interval, skip its automatic popup delay and refresh the timer
  on repeat presses, including during manual music pause. Keeps playback, queue,
  history, cached artwork and visibility preferences intact. Full backups retain
  the binding; existing backups without it remain readable.

## 1.1 — October 28, 2026

Combined update for NeoForge 26.3 / Java 25 and Fabric 1.20.1 / Java 17.
Optional server companions are distributed separately from the client packages.


- Uses compact Play/Pause/Resume and immediate-next transport icons with tooltips
  and accessible labels. Keeps the separate Skip & wait action labelled.
- Adds hover/focus playlist-row actions, with a reserved ellipsis beside the
  playing marker, existing opaque context actions, and Shift+F10 access.

- Adds a song-position bar to the workspace and AFMR settings pages, with click/
  drag seeking, elapsed/total time and keyboard seeking. Paused songs stay paused;
  seeking preserves playlist, queue, history and notification identity.
- Fixes decoder buffer ownership during resource-song seeking, preventing a
  native allocator crash when sound buffers are released.
- Adds click-to-locate for the current song, affected song counts in context
  actions, and searchable settings with direct destinations and focus feedback.
  Uses checkboxes for boolean settings and arrows for details/navigation.

- Keeps song metadata clear of the hover actions button in a reserved right-edge
  space, including Fabric clipping and a small gap on both targets.

- Adds optional lightweight NeoForge 26.3 and Fabric 1.20.1 server companions for
  multiplayer structure conditions and mob-target combat retention. Clients on servers
  without a companion retain their existing behavior.

- Adds repeatable packaged import/repair/theme/widget audits, sustained playback
  and restart probes, live riding checks, and reliable muted-stream measurements
  for both maintained targets. Fabric smoke clients can select a Java executable.

- Marks the currently playing song/source playlist separately from editing
  selection, exposes song actions on hover/focus (also Shift+F10), and uses
  readable rule summaries plus concise waiting reasons/countdowns.
- Makes theme selection/editing reversible previews with Apply / Cancel. Paging
  and unrelated saves cannot commit previews; hiding suspends them. Apply commits
  the appearance and selected custom theme, while Graphite remains built in.

- Adds Display → Menus → Themes with named custom themes, color/opacity editing,
  rename/delete and .afmrt import/export between maintained builds. Shares menu
  colors/opacity and widget background color/opacity; saved themes join full
  backups. Graphite stays available as the built-in default.

- Makes Graphite the default menu palette: charcoal surfaces, blue selections and
  soft blue highlights. Reset colors and Reset menu appearance both use Graphite.

- Removes the Black / Gray / Blue / White background preset buttons from Widget
  settings on both targets. The color picker and RGB field remain available.

- Adds playlist-only Undo/Redo and drag-to-reorder priority, with insertion feedback
  and edge scrolling. Moves song Preview/Queue/Enable/Disable/Remove into an opaque
  right-click menu; volume balancing remains in playlist actions. Compact rules
  show enabled filters, with Show rules when none are enabled.
- Adds independent menu/widget fonts from imported TTF files or active resource
  packs, including imported fonts in full backups. Aligns shared navigation tabs
  across pages and restores custom backgrounds in the Fabric playlist workspace.
- Shows **Music muted in Minecraft** when Minecraft Music or Master is muted.
  Playback and scheduling continue silently; changing volume does not pause or
  resume AFMR. Manual pause remains independent.
  Fabric retains its native channel through zero-volume settings changes.

- Reworks the playlist workspace around inline **Playlist Settings**. Keeps the
  area setup action visible, separates condition checkboxes from **Edit**, and
  puts volume, custom gaps and playback toggles in one scrolling column. Adds a
  discoverable playlist actions menu and right-click/F2 rename; moves duplicate,
  priority, export and manual playback actions there. Uses clearer action labels,
  retains all condition/group/playback controls, and silences unused list space.

- Playlist setup imports now default to a new named profile, created only on
  confirmation without switching or world binding. Existing-profile imports
  replace names ignoring capitalization/surrounding spaces while preserving
  identity and priority. Settings-only replacements keep songs; importing songs
  replaces assignments. Ambiguous duplicates stop the import; unmatched playlists
  follow the chosen insertion position.

- Groups every AFMR key binding under its own **AFMR** category in Minecraft's
  Key Binds menu on both targets, including all eight music override slots.
  Existing key assignments are preserved.

- Shared song lists now include file-content fingerprints. Import offers
  **Search linked libraries**, **Search a folder…** and individual file selection,
  with searchable missing/matched rows, verified counts, search details and Cancel.
  Identical files can use different filenames/folder layouts; filename/title
  guesses and retagged/converted copies are not treated as identical. Recorded
  identities survive saves/backups and enable content-based folder/file repair.
  Verified repairs can merge an already linked destination while preserving its
  rescan settings.

- Adds **Cancel search** to the repair menu while checking missing sources or
  preparing a repair preview. Cancellation stops work between files, preserves
  the previous report and saved paths, and discards cancelled results.

- Changes fresh playback defaults to a 3-second context buffer and 3-second
  fade-out on both targets and newly created profiles. Fade-in stays at zero.
  Saved timings are preserved; Minecraft defaults remains the vanilla-style preset.

- Adds **Riding animal** playlist conditions on both targets, with Any, Riding
  and Not riding choices. Horses and other Animal mounts use the client's live
  riding state in singleplayer and multiplayer. Supports nested conditions,
  normal transitions, profiles, playlist sharing and full backups.

- Adds **Nearby player → Proximity volume…** on both targets: independent near/far
  distances and volume percentages, live smoothing, reversal and retained settings.
  Scales the song-source playlist's volume while keeping trigger radius, playback
  fades and normalization independent. Includes owned copies, profiles, portable
  playlist sharing and full backups.

- Manual playlist normalization now starts and continues during gameplay and
  multiplayer. Removes the pause/title-screen gate and automatic singleplayer
  pause from its progress screen; Back keeps scanning, Cancel retains completed
  work, and original files remain untouched.

- Adds **Display → Widget → Custom background…** for the AFMR card on both
  targets: PNG/JPEG/static WebP and animated GIF import, on/off, Fill/Fit/Stretch,
  image opacity, preview and removal. Originals stay untouched; widget images and
  settings remain independent of menus and are included in full backups.

- Adds **Playlist → Transitions** with directional exceptions for finishing or
  interrupting the outgoing song, context buffer, fade-out, silence range and
  fade-in. Unset controls inherit existing settings; explicit rules override
  ordinary finish/matching protection and timing for that automatic pair.
  Manual controls, previews/queues and combat death handling retain precedence.
  Copies, profiles and backups retain rules; playlist exports reconnect rules
  between playlists included together.

## 1.0.1 — October 23, 2026

- Adds **Timing → Songs before silence**, with a per-playlist override under
  **Playback → Silence**. Play groups back-to-back, then use the normal gap.
  Default one preserves existing timing; switches start fresh groups.
- Adds **Add playing to playlist** to the workspace and Inspector. Choose a
  destination without removing the source assignment or changing files.
- Adds **Display → Widget → Delay (s)**. Wait 0-60 seconds of actual unpaused
  playback, then show for the ordinary full duration. Includes native toast.
- Preserves each playlist's shuffle position on switching and prevents its last
  song repeating immediately on return across a cycle boundary when alternatives exist.
- Adds **Conditions → Nearby player** with name/online-player selection, stable
  known UUID, configurable radius and normal transition settings. Uses only
  client-visible players; no server installation or network lookup. Shared files
  retain target identity; diagnostic summaries omit it.

## 1.0 — October 23, 2026

First private release for friends, maintained for NeoForge 26.3 and Fabric 1.20.1.
The combined update retains editable contextual playlists, queue/history, manual
controls, profiles, library repair and sharing alongside the additions below.

Licenses AFMR's own code and documentation under MIT, copyright 2026 animal.
Both runtime JARs and source archives include the license; bundled components
and the development template retain their separate notices. Runtime/source
distributions reproduce upstream copyright notices and full LGPL texts. Private
packages also carry matching original sources for all thirteen bundled libraries.

Moves AFMR's descriptions, hover explanations and on-screen guidance into one
author-editable `docs/UI-COPY.md`, organized by screen/control with reference
behavior and explicit target differences. Both builds compile it into native
Minecraft language entries and validate missing/unused text and placeholders.
Applies Animal's reviewed descriptions and removes the 26 explanations cleared
in the writing file. Clearing a paragraph now hides its explanation without an
empty tooltip; utility labels and clipped-label tooltips stay unchanged. Fabric
source archives carry the shared authoring inputs.

Adds optional custom silence ranges per playlist on both targets. Set fixed or
random gaps beside Volume in the playlist workspace; off inherits global Timing.
Playlist changes during silence count elapsed waiting toward the incoming range.
Preserves pause/jukebox timing, explicit playback bypasses, copies, profiles,
playlist sharing and full configuration backups.

Adds **Inspector → Copy diagnostic summary** on both maintained targets. Copy
versions, playback reasons/priority references, the selected playlist's condition
results, timing, volume, scan/repair counts and a Java heap snapshot. Reports use
playlist numbers and omit names, song metadata, paths, world/server identifiers,
X/Z coordinates, saved locations and raw errors. Clipboard output is generated
only on request, bounded in size and never sent automatically.

Moves **Rescan on launch** from Timing to Library on both maintained targets.
Consolidates the Combat on/off switch in **Timing → Combat settings**, removing
its duplicate from the Timing page. Compacts the remaining playback controls
and keeps normalization on Timing.

Consolidates all now-playing controls under **Display → Widget**, beside **Menus**.
The widget panel includes visibility/position/style, timing, size, information,
text layout and card appearance, with scrolling for smaller windows and a fixed
Preview footer. Drafts, color pickers, style restrictions and existing saved
settings remain intact. Menus retains the interface's separate appearance options.

Adds static WebP and animated GIF menu backgrounds on both targets. GIFs loop with
their frame timing, transparency and disposal rules; large animations resize to a
bounded frame budget. Explorer filters and hover text include the formats. Full
configuration export/import preserves animations, and replacement/removal/reload
releases every cached frame. Originals remain untouched.

Adds **Timing → Play immediately on join**, off by default and saved per profile
on both maintained targets. Joining a world/server starts the first eligible
playlist without initial silence or a context buffer, using normal fade-in.
Later song gaps, playlist changes and playback guards retain their timing.
Minecraft defaults remains available in the Timing footer.

Marks the Structures condition and its editor **singleplayer only** on both
maintained targets, with an explicit multiplayer-unavailable message and hover
text on structure controls. Combat hover text identifies mob-target retention as
singleplayer only while preserving ordinary multiplayer combat behavior.

Adds **Import / export → Full configuration…** on both targets. Export one
`.afmrb` backup containing all settings, profiles, playlists, song assignments,
library references and metadata, AFMR physical keys, listening history and the
managed custom background. Import previews counts and an explicit replacement
warning, then applies on the next Minecraft launch with recovery copies. Music
files and unrelated Minecraft settings remain separate. Moved music folders can
use Library repair after restoring.

Fixes exposed image/world seams along list edges and the horizontal song scrollbar.
Row backgrounds now cover their exact viewport before foreground clipping, and
the whole reserved scrollbar strip is painted once, preserving transparency.

Adds visual color pickers to every menu color and the AFMR widget background on
both maintained targets. Click a swatch for a saturation/brightness square, hue
strip, presets and synchronized hex entry. Changes preview live; Apply keeps the
choice, while Cancel/Escape restores the opening color. Focused gradient controls
support arrow keys. Existing hex fields and opacity controls remain available.

Fixes Fabric 1.20.1 cover detection reading an unsynchronized heightmap. Cave roofs
now count as solid cover immediately after chunk loading, so Covered conditions
and Dark exploration can match correctly. Water and leaves alone remain excluded.

Adds **Display → Menus → Menu colors…** on both maintained targets, with live
six-digit RGB editing for backgrounds, panels, buttons, hover/selection,
highlights, primary/secondary text, borders and scrollbars. Related shades follow
their chosen base color; defaults reproduce the existing palette exactly. Text
and highlights stay opaque while backgrounds retain their independent opacity.
Reset colors preserves other appearance settings; the full menu reset also resets
the palette. Colors are shared across profiles and independent of HUD styling.

Adds custom AFMR menu backgrounds on both maintained targets. Select a PNG/JPEG in
Explorer, with Fill/Fit/Stretch, image opacity, on/off and removal/reset controls.
AFMR preserves the original and stores a bounded copy shared across profiles.
On 26.3 the image sits before Minecraft's menu blur, below the menu panels.

Fixes the Fabric 1.20.1 album-art `OutOfMemoryError: Out of stack space` reproduced
from a Homestead log. The HUD and editor now decode PNGs through Minecraft's
stream API rather than copying the entire image onto LWJGL's small scratch stack.
Failed texture creation/registration also releases any newly owned image/texture.

Adds optional song lists to `.afmrp` sharing on NeoForge 26.3 and Fabric 1.20.1.
Export includes relative folder paths and song on/off states; import can match
linked roots or choose folders in Explorer, reuse existing library tracks and
retain missing assignments for ordinary folder/file repair. Audio files and
absolute music paths are never embedded. Both song switches default to Off.

Adds an independent Fabric 1.20.1 backport under `ports/fabric-1.20.1`, targeting
Java 17 bytecode. It uses the AFMR now-playing card, native file dialogs available
to the older game, and Minecraft 1.20.1's soundtrack. Newer native/Hybrid music
toasts, vanilla Music Frequency synchronization and menu blur are omitted. The
NeoForge implementation and version are retained. Port validation is recorded
separately from the existing NeoForge results.

Fixes shutdown ordering for world/server profiles: save the return to Default
before closing the writer, and ignore later logout/screen saves once AFMR is
closed. This prevents a spurious "Library store is closed" save error on exit.

Routine third-party metadata logging is reduced to warnings/errors. Large WAV
libraries no longer print a message for every RIFF chunk during imports/rescans.

Adds **Import / export** for `.afmrp` playlist setups. Export one or several
selected playlists in their priority order, retaining names, conditions/nested
groups, Automatic/Manual only, finish-on-entry/exit, Play on trigger, Minecraft
music and volume. Shared files contain settings only, without songs, music paths,
profile bindings, global settings or original world/server identifiers. Import
previews add independent playlists to a chosen profile/priority position; saved
coordinates, radius and dimension bind to the current world/server. Explorer
open/save dialogs and single-file drag-and-drop are supported.

Adds **Repair / missing**. Rescans check linked folders and every imported local
song, including individually added files; missing sources produce an error toast,
count and searchable report without removing assignments. Explorer can locate a
moved folder or renamed song. Review relative-path matches before applying;
track IDs, profiles, folder destinations, queue and history references survive.
Already imported duplicate tracks merge conservatively (off wins). No music or
foobar files are written. Checks/tag reads run on the existing library worker.

Adds per-playlist **Volume**, 0-100% with a 100% default and reset button. It
multiplies the fade envelope for automatic, override and queued songs. Retained
songs keep their source playlist's volume; standalone previews use normal volume.

Adds **Profiles** for independent world/server playlist and playback setups over
one shared music library. Create, copy, rename, select and link profiles; unlinked
worlds use Default. Playlist order, assignments, conditions, volume, timing, combat,
normalization policy and folder destinations travel with each setup. Appearance,
import preferences, physical keys, override slot assignments and history stay shared.
Switching stops music and clears pending queue/override state. Imports remember
their original profile through file-picker callbacks and scan completion.

Adds **Queue / history**. Queue selected enabled songs without interrupting playback;
reorder/remove requests or clear the queue. Normal silence remains between tracks,
and Next uses a ready queue first. Queued songs temporarily take priority over
automatic selection, then return to it or a retained override. New override commands
can take precedence without losing the queue. Pending requests clear on logout/reload.

Listening history saves the latest 100 starts across launches, including track
metadata, source playlist, origin and selection/context reasons. Replay, queue again,
add to another playlist, or Clear history. Saves use a separate bounded JSON snapshot
on the existing writer, without rewriting music files or copying the complete library.

Adds a **Columns** chooser beside Add music in the playlist workspace. Column
choices are saved across playlists; Title and On remain required. Optional fields
include album artist, track/disc numbers, genre, date, ReplayGain track/album gain
and peak, cached AFMR gain and file path. Extra columns scroll horizontally with
a draggable bar, horizontal wheel input or Left/Right while the song list is
focused. Headings sort their own fields numerically where appropriate, with
missing values last. Import/rescan caches new tags without changing music files;
existing entries need rescanning to fill newly available metadata.

Timing now has independent **Fade in** and **Fade out** durations. Both default
to zero and accept 0–60 seconds; combat death retains its separate fade control.

Adds a **Jukebox** selector under Timing, off by default. Audible nearby records
can fade AFMR out and pause/resume the current song, end it and wait a full normal
silence after the record, or end it and start a new song immediately afterward.
Resume uses Fade in; the other modes retain normal new-song fades. Manual pause
remains independent, overlapping records keep AFMR held, and muted records do not
suppress AFMR.

Combat now stays active in singleplayer while a loaded hostile/angered combat-capable
mob targets the player, after a qualifying attack or received hit starts the fight.
Losing the last target starts the configured quiet countdown. Targeting alone cannot
start combat; Players only and multiplayer retain their independent event timeouts.

## 0.6 — October 22, 2026

Adds **Fit songs to time window** inside Time conditions. Automatic starts use
cached lengths and normal 20-tick/second remaining time, pick fitting songs without
consuming oversized shuffle entries, and stay silent when none fit. Missing local
and vanilla lengths are checked one at a time on the library worker. Wrapped,
All/Any/Not and overlapping windows retain their logical meaning; overrides and
previews bypass fitting. Existing playing songs are not rechecked or cut by this option.

Adds **Play on trigger** to playlist Playback, off by default. A new automatic
winning entry skips the context buffer and initial silence, keeps normal fades,
and honors independent finish-song controls and matching-song retention. Later
songs use normal silence; manual controls, pause, death guards and time fitting
retain their behavior.

Adds **Gamemode** conditions with any combination of Survival, Creative, Adventure
and Spectator. Uses the client’s current server-synchronized mode, with the same
All/Any/Not groups, playlist priority, transitions, cloning, saving and Inspector
support as other conditions.

Adds **Held items** conditions with searchable item names/IDs, multiple selections,
Add held items, and main-hand/off-hand/either-hand selection. Item type matches
regardless of names, enchantments, durability or count. Integrates with All/Any/Not
groups, snapshots/clones, priority and Inspector; uses existing client inventory.

Adds **Display → Menus** with a live 0–100% background-opacity slider and an
optional Minecraft pause-menu blur. Defaults to 80% opacity with blur enabled;
blur strength follows Minecraft's own setting. Workspace and settings backgrounds
share the choice, while text, artwork and foreground indicators remain opaque.
Menu appearance is independent of the now-playing widget's appearance.

Combat now defaults to hostile/neutral mobs and other players, using source-attributed
damage rather than generic health loss. Passive targets and environmental damage
do not trigger combat. A broad any-entity/any-damage option remains available.
Adds **Players only** inside each playlist's Combat condition, with a separate PvP
timer that mob fights cannot extend. Projectile owners are resolved when available.

Adds **Health** conditions: strictly above/below a configurable red-heart threshold,
excluding golden absorption hearts. Supports decimals, existing All/Any/Not groups,
saved settings, priority, and Inspector explanations.

Adds concise hover descriptions to less obvious playlist, playback, timing,
condition, import and display controls. Help stays out of the permanent layout,
supports keyboard focus and disabled buttons, and uses screen coordinates so
workspace scaling keeps tooltips beside their controls. Buttons pause briefly
before showing help; clipped labels remain available on hover.

The six default playlists are now editable condition templates, using the same
name/filter/group editor as every other playlist. Nested All/Any/Not groups express
the existing Underground exclusions and dimension-specific darkness behavior;
initial matching, priority and Minecraft inclusion are preserved. Saved rule presets
expand to editable data on load, retaining settings, assignments and locations.
Deleted templates stay deleted. Clone creates an independent condition/assignment
copy directly below its source, with shared library music and unchanged key/folder links.

Autoplay here replaces the open playlist's conditions with a world/dimension-scoped
64-block sphere at the player, enables Automatic and clears Manual only. A continuous
one-second hold shows red warning text and a filling bar; release, pointer exit or
screen changes cancel. Detailed locations remain in the conditions editor.

Display now offers Default and all four screen corners for Minecraft, Hybrid and
AFMR popups. Defaults preserve existing placement; bottom corners avoid the hotbar.
Native toast visibility and pause-menu placement remain Minecraft-controlled.
The playback footer clears title, artist and artwork in silence instead of reverting
to an old selection; selected-song Preview remains available. Respawn behavior is unchanged.

## 0.5.7 — October 21, 2026

Replaces the main editor with the selected playlist workspace. The left column
contains scrolling playlist priority above the searchable music library; the center
keeps the sortable title/artist/album/time table and bulk actions; the right panel
exposes Automatic, Manual only, Minecraft inclusion, independent entry/exit finish
controls, and condition editing. Priority movement, new/custom deletion, library
multi-selection, recursive folder assignment, double-click assignment and previews
remain available. No welcome flow, tutorials or flavor text is added.

Pause/resume, Next, normal Skip/play, both override modes and return to Automatic
are directly accessible. Navigation opens Library, Timing, Display, Keys and Inspector;
primary settings pages validate drafts before navigation, and M still retains drafts
and editor state. Timing no longer hides global options behind More settings. The
complete display controls, all eight override slots, physical Minecraft key binding,
inspector details/retry, Home, and specialized conditions remain available.

The Add music dialog accepts files, folders or foobar playlists with an explicit
destination. Files/folders can go to Library only or directly into a playlist;
foobar import still preserves existing assignment switches and retains its report.
Linked-folder rescans and normalization progress/cancellation/reports are preserved.
Playback policy, audio ownership, saved configuration format and defaults are unchanged.

Verification: 331 automated tests and 48 isolated in-game screen/control checks.
See [the UI report](docs/testing/2026-10-21-workspace.md) for exact scope and limits.

## 0.5.6 — October 21, 2026

The music-library key (M by default) now closes AFMR as well as opening it.
Reopening restores the current page, playlist, searches, expanded folders,
selections, sort order and scroll positions within the current world session.
Text fields keep accepting the bound key as text. M temporarily hides settings
pages without discarding their draft fields; Save & back still applies them.
Done/Escape from the main editor also retains its place. Disconnect clears the
remembered menu. The control is labeled **Open/close music library**.

## 0.5.5 — October 21, 2026

Removes the AFMR card's left accent bar and changes its default panel to black at
92% opacity. Adds an **Appearance** tab with arbitrary RGB background colors,
Black/Gray/Blue/White presets, a saved-color swatch, 0–100% background opacity and
an appearance reset. Background opacity is independent of text/artwork; the note
fallback has no separate colored tile. Hybrid retains Minecraft's textured frame.

Removes the redundant **Now playing** heading and its setting from custom widgets.
The native toast already shows only song information. Existing size, information,
scrolling and preview controls remain available.

Adds **Text layout** on the Size tab for Hybrid and AFMR: single line per field,
or word-wrapped multiple lines. Wrapping preserves whole words within the chosen
width and increases widget height. Words too wide for one row retain the existing
scroll/ellipsis behavior. Wrapping and measurements are cached at content/layout changes.

## 0.5.4 — October 21, 2026

Adds a selectable **Hybrid toast** with Minecraft's toast frame and AFMR's artwork
and popup controls. Album art replaces the music notes when available; missing art
uses animated colored notes. Minecraft's native toast and the AFMR card remain selectable.

Hybrid and AFMR now offer 50–200% scale, 140–600 GUI-pixel width, 1–60 second duration,
and independent artwork, heading, title, artist, album, song length and source-playlist
switches. Optional scrolling keeps long text within the chosen width and pauses at
both ends; off uses an ellipsis. Settings include a current-song/sample preview.
Small viewports cap the effective scale. Artwork requests coalesce during rapid skips
and settings changes, with stale results discarded.

## 0.5.3 — October 21, 2026

This build collects the updates made since the imported 0.5.2 baseline. Earlier
development builds reused the 0.5.2 filename; those changes are described together
here.

Adds selectable now-playing styles: Minecraft's native music toast (default),
including the pause-menu track display, or the retained AFMR album-art card.
Native style respects Minecraft's Music Toast and notification-duration settings;
AFMR style keeps its adjustable card duration. HUD artwork is loaded only in AFMR mode.

Changing vanilla **Music Frequency** applies AFMR silence presets: Default 10-20
minutes, Frequent 10 minutes, Constant 5 seconds. Pending gaps restart; playing songs,
fades, buffer, and saved custom timing at startup are preserved.

Adds global **Continue Playing on Match** in **Settings → More settings**, off by
default. Automatic playlist changes let a shared eligible library track finish
without interruption, followed by normal silence. Later automatic changes check
again; manual overrides retain their existing behavior and priority.

Adds **Weather** conditions: Any weather (rain/snow/thunderstorms), Rain, Snow,
Thunderstorm, and Clear. Filter off allows every weather state. Rain and Snow
follow biome/height precipitation and also match during storms; Cover is independent.
Adds **Conditions: match all / any** to custom playlists. Match all remains the
default; Match any accepts one enabled filter, and no filters still matches everywhere.

Adds a searchable **Structures** filter to custom playlists. Select any generated
structure type, including village variants and modded IDs, with an optional 0-128
block radius outside its full bounding box. Detection initially uses loaded
singleplayer data; that filter cannot match in multiplayer until server support is
added. Match any may activate via another filter. Existing playlist priority applies.

Clears playback, pause, overrides, combat, and cached world context immediately on
logout. Adds a reusable vanilla-server integration suite; the current build passes
[27 multiplayer checks](docs/testing/2026-10-21-multiplayer.md).

Adds **Normalize playlist** for imported music, with existing ReplayGain tag reuse,
cached -18 LUFS measurements, progress/cancellation, and a per-track report showing
signed effective dB adjustments and peak limits. Original music and foobar data stay
read-only; vanilla tracks are unchanged. Analysis suspends during active gameplay.

## Releases through 0.5.2

Version 0.2.2 fixes repeated full-library scans during playback and editor rendering. Playlist resolution uses a track index; the editor reuses the scheduler's result and caches its folder tree. Saves serialize and write in the background, with pending saves flushed on normal shutdown. See `PERFORMANCE.md` for measurements against a 67,812-file library. No reimport or settings reset is needed.

Upgrading from 0.1 preserves your imported tracks and settings. Before upgrading the configuration, the mod saves `config/afmr/library-v1-before-upgrade.json`. Old biome, dimension, and underwater playlists are retained in the JSON's `legacyPlaylists` archive, and no longer appear in the editor. Old General assignments are copied to Day outside, Night outside, and Underground; explicit old Day/Night assignments take precedence. Individual disabled-song choices are preserved. Old General folder links continue adding new songs to those three ambient playlists during rescans. New folder imports go into the library first so you can assign their songs yourself.

Version 0.3.0 adds Underground Y/cover controls, Dark exploration, and optional Nether/End overrides. Existing imported music and playlist choices are retained.

Version 0.3.1 adds read-only imports from foobar2000 `.fplite` playlists into any of the six existing environment playlists, with duplicate handling, progress, metadata reuse, and an import report.

Version 0.3.2 fixes Underground and Dark exploration being classified as Outside on newly loaded chunks. The cover check now uses surface data Minecraft actually synchronizes to clients, with a local column check to exclude water alone. No settings reset or music reimport is required.

Version 0.4.0 adds named custom playlists with optional conditions, a ranked playlist manager, paged playlist tabs, and dedicated combat/death controls. Existing music, defaults, settings and priority order are retained.

Version 0.4.1 adds exact biome selection for custom playlists, a Minecraft playback-timing reset, and playback of existing ReplayGain track tags. Existing timing settings stay as saved; fresh configurations use the timing defaults described below.

Version 0.4.2 separates each playlist's finish-song behavior into **on entry** and **on exit**. Existing finish-song choices retain their entry behavior; the new exit toggle starts off. No reimport is required.

Version 0.5.0 adds a live playback inspector, manual-only playlists, one-song and held overrides, eight assignable override keybind slots, and independent Fisher-Yates shuffle bags for each playlist. Existing music, conditions, priorities and playback settings remain intact.

Version 0.5.1 makes a playlist's on/off switch control automatic selection only. It is labeled **Automatic** in Playlist settings. Off playlists remain playable through both override modes and keybinds; switching Automatic off does not stop an active override. Individual disabled songs stay excluded from overrides. Existing saved switches retain their values.

Version 0.5.2 adds separate **Next song (skip silence)** and **Pause/resume AFMR music** keybinds, plus matching Inspector buttons. Existing skip and override bindings remain unchanged.
