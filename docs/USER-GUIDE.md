# AFMR user guide

For animal's final music replacer **1.0.1**. Reviewed against the source on
**October 23, 2026**.

AFMR gives Minecraft a soundtrack built from your own music and Minecraft's music.
It chooses a playlist based on where you are and what you are doing. You can also
choose music yourself, skip songs, and pause just the music.

This guide describes the NeoForge 26.3 build. The maintained Fabric 1.20.1 build
shares the playlist/library features, uses AFMR's own now-playing card, and lacks
the newer native music-toast/frequency and menu-blur integration. See the
[installation checklist](INSTALLATION.md) for both targets' requirements.

Start with [your first song](#install-and-hear-your-first-song), keep the
[quick reference](#quick-reference) handy, or jump to:

- [Library and imports](#your-library-and-imports)
- [Repair moved music](#repairing-moved-or-missing-music)
- [Multiplayer](#multiplayer)
- [Playlists and priority](#playlists-and-priority)
- [World and server profiles](#world-and-server-profiles)
- [Share playlist setups](#sharing-playlist-setups-afmrp)
- [Custom conditions](#playlists-and-conditions)
- [Home, Underground, and Dark exploration](#autoplay-areas-and-editable-templates)
- [Timing and transitions](#timing-and-transitions)
- [Overrides and keys](#manual-overrides-and-keybinds)
- [Queue and listening history](#queue-and-listening-history)
- [Combat and death](#combat-and-death)
- [Volume and artwork](#volume-replaygain-and-artwork)
- [Troubleshooting](#troubleshooting)
- [Saving and sharing](#saving-upgrading-and-sharing)

## Install and hear your first song

1. Use **Minecraft Java 26.3**, **NeoForge 26.3.0.90 or later**, and **Java 25**.
   Install AFMR on your own Minecraft client; a server installation is unnecessary.
   Fabric, Forge, and other Minecraft versions need a separate port.
2. Close Minecraft. Put `animals-final-music-replacer-1.0.1.jar` in your game
   instance's `mods` folder. Keep exactly one AFMR JAR there.
3. Launch that instance, join a world, and press **M**. If M is already assigned
   to another mod, change AFMR's binding in **Options → Controls → Key Binds**.
   You can also open AFMR with its **Config** button in the Mods menu.
4. Select **Day outside** in the left priority list. Click **Add music**, select
   that destination, then **Files** or **Folder**. Importing directly into a playlist
   adds its songs to the library and assigns them in the same operation.
5. To assign existing music, select songs or a folder in the lower-left **Music
   library**, then **Add selected / folder →**. Double-click a file to add it directly.
6. Select a song in either pane and click **Preview** to hear it immediately.
7. **Next** in the bottom transport skips the normal wait. Fresh settings use
   **10–20 minutes of silence between songs**.

For local music only, switch **Minecraft music** off in the selected playlist's
right-side Playback panel, and remove or disable any explicit Minecraft assignments.

AFMR reads music at its original location. It does not copy or modify your music,
playlist files, or foobar settings. You can close its screens while a scan runs.
**AFMR screens do not pause the game**, so open them somewhere safe.

## Quick reference

Paths beginning with **M** start in the main music editor. M reopens the page you
last hid; use Back / Save & back to return to the editor if needed.

| I want to… | Where / what to do |
| --- | --- |
| Open / close AFMR | **M**, or Mods menu → AFMR → Config to open |
| Import audio | **M → Add music → Files / Folder** |
| Assign imported songs | Select a playlist in Priority, select songs/folder in Music library, **Add selected / folder →** |
| Hear one specific song | Select it in either pane → **Preview** |
| Queue specific songs | Select songs in the playlist table → **Queue** |
| Edit the queue or find recently played songs | **M → Queue / history** |
| Set gaps for one playlist | Select playlist → **Silence** beside Volume → **Custom silence: on** |
| Balance a playlist's volume | Select playlist → **Volume** in the right panel |
| Keep different world/server setups | **M → Profiles** |
| Skip and get another song immediately | **M → Next**, or bind **Next song (skip silence)** |
| Skip but keep the usual gap | Editor **Skip / play** while playing, or bind **Skip song (with normal silence)** |
| End a gap | Editor **Skip / play** while idle; context rules still apply |
| Pause / resume only AFMR music | **M → Pause/Resume**, or bind **Pause/resume AFMR music** |
| Force a playlist | **M → Play one song / Until released** |
| Release an override | **M → Automatic**, or bind **Return music to automatic selection** |
| Stop a playlist activating automatically | **Conditions → Automatic: off**, or **Manual only: on** |
| Exclude a song from that playlist | Select its row → **Disable**, or click its **On** cell |
| Change playlist priority | **M → Priority**; #1 wins first |
| Make a custom playlist | **M → New** |
| Change silence / fades | **M → Timing** |
| Start music when joining a world/server | **M → Timing → Play immediately on join** |
| Choose song information columns | Playlist workspace → **Columns**, beside **Add music** |
| Fade music out for nearby jukebox records | **M → Timing → Jukebox** |
| Choose a widget style, size, information, or scrolling | **M → Display** |
| Keep shared songs playing through automatic playlist changes | **M → Timing → Continue playing on match** |
| Play a playlist near your location | Select it → hold **Autoplay here** for one second |
| Diagnose silence or unexpected selection | **M → Inspector**, select a playlist → **Details** |
| Find linked folders | **M → Library** |
| Refresh linked folders | **M → Library → Rescan all** |
| Refresh linked folders automatically at startup | **M → Library → Rescan on launch** |
| Configure combat detection and death behavior | **M → Timing → Combat settings** |
| Find or reconnect moved music | **M → Repair / missing**, or **Library → Repair / missing** |
| Transfer a complete AFMR install | **M → Import / export → Full configuration…** |
| Share playlist conditions/options | **M → Import / export → Export selected...** |
| Add a shared playlist setup | **M → Import / export → Import .afmrp...**, or drop one `.afmrp` file onto the workspace |

Only **M** is bound initially. Skip, Next, Pause, Inspector, Overrides, Return to
automatic, and all eight override slot keys start unbound. Gameplay music keys
do not activate while chat or another screen is open; use the screen buttons there.

**M** (the **Open/close music library** binding) also hides AFMR from its subpages.
Reopening restores the page, selected playlist, filters, expanded folders,
selections, sort order and scroll positions. Workspace **Done** / Escape also
keeps your place. This memory lasts until you leave the world or restart Minecraft.
While a text field is focused, M types normally; click a button or list first to
use it to hide the menu. Hiding a settings page keeps draft text without applying
it; use **Save & back** to validate and save those fields.

## Multiplayer

Install AFMR and NeoForge on your own client, then join a vanilla **Minecraft 26.3**
server normally. AFMR needs no server installation, custom packets, or operator
permissions. Music files, playlists, imports, controls, artwork, and settings stay
on your computer; other players hear their own soundtrack.

Automatic playlists use the position, time, dimension, biome, temperature, light,
cover, water, and player health data the server already sends. Home and custom
location areas are scoped to the connection's server address and dimension.
Joining through a different address requires capturing the area again; replacing
the world behind the same address also requires recapturing it.

The **Structures (singleplayer only)** filter currently detects structures in
singleplayer only. Its editor title and unavailable status make that restriction
visible; hover text on its controls repeats the limitation. Vanilla servers do not
send structure boundaries. In multiplayer, playlists
cannot satisfy that filter. **Match all** playlists fall through to other eligible
playlists; **Match any** can still activate through another enabled filter. Overrides
remain available. Multiplayer structure support is planned.

Multiplayer menus cannot pause the server. AFMR's own Pause still holds just your
music, while context detection continues. Disconnecting clears playback, pause,
overrides, combat, and the previous world context; saved playlists and areas remain.

Loudness analysis waits while connected, to protect gameplay performance. Leave
the server to let a waiting **Normalize** job finish; saved normalization
adjustments and existing ReplayGain tags work during multiplayer playback.

## Your library and imports

The **library** is the list of music AFMR knows about. A **playlist** is a collection
of assignments from that library. A song can belong to several playlists, with a
separate on/off choice in each.

### Finding, selecting, and editing songs

The lower-left Music library shows All local music, linked folders, nested folders, files, and
the Minecraft soundtrack. The right pane shows the selected playlist. Each pane
has its own search box and scroll position. Click column headings to sort songs.
The upper-left Priority list scrolls independently and shows every playlist.

**Columns**, beside **Add music**, chooses which song fields appear in every
playlist. Defaults are Title, Artist, Album and Time, plus the required On switch.
Title stays enabled. Optional columns include Album artist, Track no., Disc no.,
Genre, Date, ReplayGain track/album gain and peak, AFMR gain and File path. Choices
save immediately; **Defaults** restores the original set.

When the columns exceed the table width, drag the horizontal bar below the songs,
scroll horizontally, or focus the song list and press Left/Right. The On switch
stays pinned at the left. Column headings sort the field currently under the
pointer, including after scrolling. Track/disc numbers, duration and gain/peak
values sort numerically; missing values stay at the bottom in either direction.

New tag fields are read during import/rescan. Rescan linked folders or re-add
individual files to fill them for existing entries. Missing tags leave empty cells;
Minecraft tracks generally have no album artist, track/disc or ReplayGain tags.
**RG** columns show the file's ReplayGain tags, before peak protection. Album tags
are information only; playback continues to prefer track gain. **AFMR gain** shows
the cached loudness measurement, before peak protection, and does not override an
existing track gain tag. Showing columns does not change playback or write tags.

Hover less obvious controls for short explanations of their behavior and ranges.
Button help appears after a brief pause and is also available through keyboard
focus. Disabled controls can still show their hover help.

- Click a folder to select and expand/collapse it. **Add selected / folder →**
  adds its music recursively to the open playlist, including subfolders.
- Double-click a file on the left to add it directly.
- **Ctrl-click** selects separate songs; **Shift-click** selects a range.
  **Ctrl+A** selects listed songs in the focused list when you are not typing.
- In the right pane, **Enable** and **Disable** change the selected assignments.
  Clicking the **On** cell toggles one song. **Space** toggles selected songs.
- **Remove** removes an explicit playlist assignment. It does not delete the
  audio file or its library entry, or affect other playlists.
- Automatically included vanilla songs can return after **Remove**. **Disable**
  them to keep them excluded, or turn off automatic vanilla inclusion.

**Preview** previews the last song selected in either pane. It bypasses
playlist conditions and automatic eligibility, cancels an override, and normally
finishes before automatic scheduling resumes. The playback footer shows only the
current song; during silence it clears title/artist/artwork and shows the wait timer.
Selection remains available for Preview. Use Disable to exclude a song from
scheduled playback; an explicit preview can still play it.

### Supported audio and folder links

Use **MP3, OGG Vorbis, FLAC, or WAV**, in mono or stereo. Supported variants include
24-bit FLAC/WAV and floating-point WAV. DRM, OGG Opus, and surround audio are not
supported. Tags supply title, artist, album, and duration when available; the
filename supplies a fallback title. A successful import does not guarantee a file
will decode successfully when played.

**Add music → Folder** scans subfolders and keeps a folder link for later rescans.
**Library** lets you change whether that link includes subfolders, rescan just
that folder, or **Unlink folder (keep songs)**. A folder link remembers the destination chosen during import. Library-only links
leave assignment to you; a playlist destination also assigns newly discovered songs there.

**Rescan** refreshes linked folders and metadata without resetting existing
assignment switches. **Library → Rescan on launch**
enables optional launch scanning; it starts off. Files added individually can be
selected again with **Add music → Files** to refresh their metadata. Missing files are not
automatically removed from the library.

During discovery, the progress display counts files while the total is still
unknown. During tag reading, it shows processed/total and the current file. It
continues after leaving the editor. Drag audio files or folders onto the editor
as an alternative to the file dialogs.

### Repairing moved or missing music

**Rescan all**, a folder rescan and optional launch rescans check linked folders
and all imported local songs, including files outside linked folders. Missing or
unreadable sources produce an error toast and red **Missing: N** button in the
workspace. They stay in the library and playlists. Open **Repair / missing** to
search the report; hover a row for its full path and explanation. **Check again**
checks availability without reading every song's tags. **Scan messages** shows
other scan errors separately. Reports describe the last check, not a continuous
drive monitor; restoring a drive/file may require another check or rescan.

1. Select the missing **Folder** row and choose **Locate selected...**. Explorer
   opens a folder picker. Select the folder's new root, preserving the layout below
   it. For example, `OldMusic/Albums/Track.mp3` maps to `NewMusic/Albums/Track.mp3`.
   You can also open a linked folder's settings and use **Locate moved folder...**.
2. Review the located/still-missing counts and hover rows to compare old/new paths.
   **Apply repair** updates AFMR's paths; **Cancel** leaves them unchanged. Folder
   links nested under the moved root and each profile's rescan destinations follow.
3. Unmatched songs keep their IDs with the expected path under the new root and
   remain listed. Select a **Song** row and **Locate selected...** to choose an
   individually renamed/moved file. AFMR does not guess between matching filenames
   elsewhere. A folder with no song matches is rejected; pick the correct root or
   repair songs individually.

Repair preserves playlist assignments, enabled/off switches, priorities, volume,
conditions, queue order and history. Rescanning the repaired location keeps each
song's original ID. Existing duplicate track entries at that path are merged;
if their switches disagree in one playlist, **off wins**. If the destination
folder itself is already linked, unlink that duplicate link first (keep songs),
then retry, so its folder settings are not silently overwritten.

Only AFMR references change: repair never moves, renames or writes music or
foobar files. Located songs refresh their metadata; cached loudness survives only
when size and modification time still match. Applying stops a currently playing
song affected by the repair; other music and pending queue requests remain. A new
availability check runs afterward. Finish or cancel the pending repair before
starting another import/scan. M can hide a preview; **Review repair** reopens it.

### Importing foobar2000 playlists

1. Open **M → Add music** and select an existing destination playlist.
2. Click **Foobar playlist** and select one or more files. On Windows,
   the dialog starts in `%APPDATA%\foobar2000-v2\playlists-v2.0` when available.
3. Wait, then use **View last import report** for added/already-assigned songs,
   duplicates, skipped entries, and reasons. Hover clipped details for full text.

Imports append songs and preserve existing enabled/disabled assignments. Reimport
after editing the foobar playlist; AFMR does not watch it or remove songs deleted
from it. Existing library metadata is reused; use an ordinary audio/folder rescan
to refresh tags. Playback uses AFMR shuffle rather than foobar's order.

You can drop a batch containing only `.fplite` files onto the editor to import
into its open playlist. Drop audio and playlist files in separate batches.

Only UTF-8 `.fplite` location lists are supported, up to 64 MiB each. Binary `.fpl`,
network streams, archive-backed `unpack://` entries, subsongs/chapters, and formats
such as Opus/MP4/AIFF are skipped and reported. AFMR reads the adjacent `index.txt`
for playlist names when available. Reports retain up to 100 detail messages while
their totals cover all entries. No original foobar data is changed.

## Playlists and priority

AFMR checks the list from top to bottom. The **first matching automatic playlist
with at least one eligible song wins**. Empty playlists fall through. Automatic
vanilla songs can make a playlist playable even without local assignments.

The initial priority order is:

| Rank | Playlist | When it matches |
| --- | --- | --- |
| 1 | Combat | A qualifying attack/damage trigger started combat; the quiet period is initially 15 seconds, with targeting retention in singleplayer |
| 2 | Home | Inside the captured world/dimension and spherical Home radius; initially 64 blocks |
| 3 | Dark exploration | Covered and light ≤4 outside Nether/End; optional dimension groups start off |
| 4 | Underground | At or below block Y 64, under solid cover, with at least 5 blocks of cover |
| 5 | Night outside | Not classified Underground, world-clock ticks 12000–23999 |
| 6 | Day outside | Not classified Underground, world-clock ticks 0–11999 |

The default playlists do not filter by biome. Most include applicable Minecraft
music automatically; **Dark exploration starts without automatic vanilla music**.
Which vanilla songs are applicable follows Minecraft's current situation and
loaded sound resources.

In **Priority**, select a row and use **Top**, **Up**, **Down**, or **Last**. The six
defaults are editable templates: rename them, change any conditions, clone them, or
delete them just like other playlists. New playlists start just above the Day/Night
fallbacks; move them higher to outrank other eligible playlists. Template conditions
are independent copies, including the Underground exclusions inside Day/Night.

**Automatic: off** and **Manual only: on** both exclude a playlist from automatic
selection while allowing overrides and previews. They are independent settings:
turning Automatic back on does not turn Manual only off. A song's own off switch
still excludes it from automatic playback and overrides.

## World and server profiles

Open **M → Profiles**. Your existing setup is **Default**. Enter a name and choose
**New** for the six fresh templates, or **Copy selected** to duplicate a setup.
Copies have independent playlists, priorities, conditions, song switches, volume,
timing, combat, jukebox and normalization settings. Up to 32 profiles are supported.

Select a profile and **Use now** to switch immediately. This stops music and clears
the queue, override and pause state. **Link current** makes that profile load next
time you join the current world/server; it does not switch immediately. One profile
can serve several worlds/servers. Unlinked worlds use Default. **Unlink current**
removes that association; **Linked worlds** lets you remove older associations.
Links use exact world folders or server addresses/ports, so identical display names
do not collide. Moving a save folder or changing the address requires linking again.
Changing dimensions keeps the current profile. Logout/title-screen editing uses Default.

Imported music, metadata, loudness results and linked folders stay shared, as do
appearance, launch-rescan preference, song columns, keys, override slot destinations
and listening history. An override slot pointing to a playlist absent from the
active profile is unavailable until reassigned or that playlist exists there.
Folder rescan destinations are remembered per profile. **Rescan all** and launch
rescans discover shared folders once and apply the captured destinations across
all profiles, even when inactive. A folder not configured in a setup adds no
assignments there. Rescanning one folder from its settings uses the current setup. A pending import still assigns to the
profile where you opened the picker, even after switching. Deleted destinations
leave the imported songs in the shared library. Copied location filters retain
their original world scope; capture a new location when needed.

**Rename** changes a setup's name without changing its links. Default and the active
profile cannot be deleted; switch away before deleting another setup. Deleting an
inactive profile removes its links and setup, preserving shared music and history.

## Playlists and conditions

Open **New**, enter a name, and click **Create**. The new playlist opens in the workspace;
assign music from the lower-left library and open **Name & conditions** as needed. New playlists start without automatic vanilla
inclusion. **Name & conditions** opens the same complete editor for every playlist.

**Clone** asks for a name and inserts the copy immediately below its source. It
copies all conditions and nested groups, enabled/disabled song assignments, Automatic,
Manual only, Minecraft inclusion, Play on trigger and both finish switches. Edits to either playlist
are independent. Files and library tracks remain shared; folder links and key-slot
bindings stay with their existing targets.

Choose **Conditions: match all / any** above the filters. **Match all** is the
default: every enabled filter must match. **Match any** activates when at least
one enabled filter matches. Disabled/unset filters do not count as matches. With
no enabled filters, the playlist matches everywhere in either mode. Each filter
counts once: multiple selected biome IDs are alternatives within the Biomes filter,
and structure selections work the same way. For example, Biomes + Weather with
Match any means a selected biome **or** the chosen weather; Match all requires both.
Use Automatic off or Manual only for a collection you want to trigger yourself.

**Groups** adds nested condition branches. Every group has the same filters, All/Any
mode and **Not** switch as the playlist. All requires every enabled filter/group;
Any requires at least one. Not inverts their combined result. **Group: off** skips
a branch without deleting it. Disabled branches do not count as matches. No enabled
filters or groups matches everywhere before Not is applied. Unavailable structure
data cannot become a match through Not. Groups support four nested levels, eight
children per level and 64 total condition nodes per playlist.

| Filter | Meaning |
| --- | --- |
| Location | This playlist/group's sphere in one world/server and dimension |
| Time | Inclusive ticks 0–23999; Day/Night/All day presets; start > end crosses midnight, equal ends match one tick; optional Fit songs to time window |
| Dimension | One exact ID, such as `minecraft:the_nether`; modded IDs work too |
| Elevation | Inclusive minimum/maximum block Y, including negative values |
| Cover | Any, solid cover with a minimum depth (0–4096), or open sky; 0 requires cover without a depth limit; water/leaves alone do not count |
| Biomes | Any one of the selected exact biome IDs |
| Structures | Any selected generated structure's full bounding box, plus an optional 0-128 block radius outside it; singleplayer only initially |
| Biome temperature | Inclusive Minecraft biome base value, not Celsius; independent of weather, height, and time |
| Light | Inclusive local block/skylight range 0–15; unrelated to video brightness |
| Combat | Any, in combat, or peaceful; optional Players only restricts the timer to PvP; obeys the global combat switch |
| Health | Strictly above/below a configurable number of red hearts; excludes absorption; equality does not match |
| Held items | Any selected item type in the main hand, off-hand, or either hand; empty hands do not match |
| Gamemode | Any selected mode: Survival, Creative, Adventure or Spectator |
| Water | Any, underwater, or out of water |
| Weather | Any weather (rain/snow/thunderstorms), Rain, Snow, Thunderstorm, or Clear; off allows every state |

**Gamemode:** open **Name & conditions → Gamemode**, enable **Filter**, select
one or more modes and **Save & back**. No selections with the filter enabled
cannot match. The filter starts off, with Survival selected. Changes to your
current mode use the usual playlist priority, buffer, fade and finish-song rules;
manual overrides still bypass conditions. It works in singleplayer and multiplayer
using the mode Minecraft reports for your player.

**Time → Fit songs to time window** is off by default. Turn it on to start only
automatic songs whose full length fits the remaining matching time. AFMR uses
normal time flow (20 game-clock ticks per second); for example, 6000 ticks left
allows a song up to five minutes long. The check happens after the applicable buffer
and silence, when the song is about to start. Oversized songs are skipped without
being counted as played. If none fit, the selected playlist stays active and silent
until a fitting song becomes available or its conditions stop matching; lower
priority playlists do not fill that silence.

Cached imported lengths are reused. Missing local/vanilla lengths are checked in
the background; unknown/invalid lengths wait until known, and **Inspector → Retry
failed** allows another attempt if a check failed. Source audio files stay read-only.
Discovered local lengths are saved; Minecraft/resource-pack lengths are rechecked
after sound/resource reloads because packs can replace songs.

Fitting follows All/Any/Not groups: Time OR a currently matching non-time condition
has no time limit from that Time branch. Overlapping time alternatives can extend
the matching interval; negated time groups fit until the forbidden interval begins.
All day has no closing boundary. Other observations are assumed to stay as they are
when choosing the song. Manual clock jumps, accelerated time and later condition
changes are not predicted. Existing playing songs keep their normal finish/transition
rules; **Next** still checks fitting, while overrides and explicit previews bypass it.

**Held items:** open **Name & conditions → Held items**, search an item name or ID
(for example, `fishing rod` or `minecraft:fishing_rod`), and click its row. You can
select multiple item types, or use **Add held items** to add what you currently hold
in the chosen hand(s). **Hand** cycles through Either hand (default), Main hand and
Off-hand. Save & back applies the filter. Clear / any disables it; an enabled
filter needs at least one selection. Modded item IDs can be entered manually and
remain saved when unavailable.

To play a fishing playlist while holding a rod, assign songs, enable its Held items
filter, select Fishing Rod, and leave Hand on Either hand. Make sure Automatic is
on, Manual only is off, and its priority is above other playlists that would win.
Existing silence, buffer and finish-song settings still apply when switching items.
This checks item type, including damaged, renamed and enchanted rods; casting is
not required. Items in your inventory but outside the chosen hand(s) do not count.
The filter works with All/Any/Not groups in singleplayer and multiplayer.

**Nearby player:** open **Name & conditions → Nearby player**, enter a Minecraft
player name or select an online player, set a **1–1024 block** radius (default 32),
and turn the condition on. A selected/known online profile keeps its UUID, so a
later name change does not target someone else. Otherwise AFMR matches the entered
name without case sensitivity. You cannot target yourself.

Distance includes height and the exact radius boundary. Only living players
already tracked by your client in the current dimension count; the online list
alone does not mean someone is nearby. Server tracking limits can therefore make
a large configured radius ineffective. No server mod is required. Combine the
filter with ordinary All/Any/Not groups and adjust playlist priority as usual.
Entry and exit obey the playlist's existing finish/fade settings. Player names
and UUIDs are retained in playlist exports and full backups; copied diagnostic
summaries omit them.

**Weather → Any weather** includes rain, snow and thunderstorms, and excludes clear
conditions. It uses the dimension's weather state, so it also matches a rainy world
while you are in a dry biome. **Rain** and **Snow** additionally check which type the
biome and your height produce; dry biomes match neither. Both include thunderstorms
with that precipitation type. **Thunderstorm** matches storms in dry or snowy biomes
too. **Clear** requires neither rain/snow weather nor thunder. Nether and End have
clear conditions under vanilla rules. Weather continues matching indoors; combine
it with **Cover** using Match all to require exposure or shelter. Detection uses
client weather data in singleplayer and multiplayer; no server mod is needed.

Time uses the Overworld clock even in other dimensions. For biome selection, search
the current world's list, **Add current biome**, or enter an ID with **Add ID**.
Saved IDs remain when their mod is unavailable. Outside a world, you can still
edit saved/manual IDs. An enabled biome filter needs at least one selection.

For **Structures**, join singleplayer and search names or IDs (searching `village`
lists the village variants), use **Add current structures** while inside one, or
enter an exact ID such as `minecraft:village_plains`. Any selected type can satisfy
the structure filter. Selections persist when their mod is
absent and can be edited outside a world using **Add ID**.

**Radius outside bounds** defaults to **0**, meaning inside the structure's full
generated bounding box. Set it to **32**, for example, to also match within 32
blocks of the nearest boundary. Distance uses your block position and includes
height and diagonal distance; it is a spherical margin, not a sphere around the
structure's center. The maximum is **128 blocks**. Detection refreshes about twice
per second from already loaded chunks and loaded structure starts; it never loads
or generates terrain to find structures. Player-built villages and structures
placed without generated structure records do not supply these boundaries.

Example: create **Cold mountain nights**, enable Night, minimum Y 80, and maximum
biome temperature 0.15. Assign songs and move it above other playlists that would
otherwise win there. The Inspector explains any filter that is preventing selection.

A captured Location already fixes its dimension; a conflicting Dimension filter
prevents matching in Match all, while Match any treats them as alternatives.
**Delete** asks for confirmation and removes
only that playlist/assignments. Library entries, originals, and other playlists
remain. Deletion is unavailable during scans.

## Autoplay areas and editable templates

Select any playlist and hold **Autoplay here** for one continuous second. While
held, the button shows a red replacement warning and a filling bar. Releasing early,
moving off the button, or leaving the screen cancels it. A completed hold replaces
all its conditions with a 64-block sphere around your current position, scoped to
this world/server and dimension. It enables Automatic and turns Manual only off;
song assignments, Minecraft inclusion and finish switches are retained. Priority
still determines which eligible playlist wins.

For a different radius, coordinates, dimension or combined conditions, use
**Name & conditions → Location**. Each playlist and group owns its own area.
Spheres include height and the boundary. Moving a singleplayer save to a different
instance may require recapturing an area because its scope includes the save path.

**Home** starts as an unconfigured Location template. It has no global area or
special playback behavior. Set its location through the conditions editor or use
Autoplay here, and clone it for additional places if desired.

**Underground** starts with Elevation from the minimum integer Y to 64 and Covered
with a five-block minimum depth. Both must match. Edit those ordinary filters in
Name & conditions. Day/Night each contain their own negated copy of that group;
changing one template does not silently rewrite another.

**Dark exploration** uses Any across three groups. Covered darkness requires solid
cover and light 0–4, plus Not Nether and Not End dimension groups. The separate
Nether and End groups start off; enable either under Groups to match that entire
dimension regardless of cover/light. Every branch and filter is editable.

## Timing and transitions

**Songs before silence** in Timing sets how many naturally completed songs play
consecutively before the usual silent gap. It starts at **1**; whole numbers from
**1 to 1000** are allowed. Set **3** for three songs, a gap, then another three.
For an individual playlist, open **Playback → Silence → Songs before silence**
and enable **Override global count**. Song count and custom silence are independent.
Both global counts and overrides belong to the current world/server profile.

Switching playlists starts a fresh group. Initial playback keeps its usual wait;
each song still uses the normal fade-in. Groups apply to automatic playback and
held playlist overrides. Queued songs, previews and one-song overrides retain
their normal silence behavior. Skip ends the group and waits normally; Next ends
the group and bypasses the wait. Minecraft defaults restores the global count to
one, preserving playlist overrides.

**Timing** controls:

| Setting | What it does | Fresh default / range |
| --- | --- | --- |
| Minimum / maximum silence | Random gap before the next scheduled song | 600–1200 seconds; each 0–86400, maximum ≥ minimum |
| Fade in | Fade in a new song or resume it after a jukebox record | 0 seconds; 0–60 |
| Fade out | Fade out a normal interruption, override transition or jukebox hold | 0 seconds; 0–60 |
| Context buffer | Require a selection to stay stable before accepting a context change | 0 seconds; 0–60 |
| Play immediately on join | Skip initial silence and context buffer for the first eligible playlist after joining | Off; saved per profile |
| Continue Playing on Match (under Timing) | Let the current song finish when it is eligible in the incoming automatic playlist | Off |
| Jukebox | Choose what AFMR does while an audible nearby record plays | Off |

For more frequent music, lower both silence values. For gentle changes, try a
3-second Fade in, Fade out and context buffer. A normal interrupted transition is:
stable new selection → fade out → silence → fade in.

For a playlist-specific gap, select the playlist and open **Silence** beside
**Volume** in the Playback panel. Enable **Custom silence** and enter minimum and
maximum seconds. Set both to **600** for fixed 10-minute gaps, or both to **3**
for a 3-second combat gap. Off uses global Timing and retains your custom values.
The same 0–86400-second bounds apply; the maximum must be at least the minimum.

During an ordinary silent gap, changing the selected playlist applies its range
and counts time already spent in silence. Entering a 3-second playlist after
waiting 100 seconds can start music immediately, once its context buffer allows.
Pause and jukebox holds do not count as elapsed silence. The range is sampled
once when it changes; an unchanged range keeps its existing random countdown.
Queued songs use their source playlist's range, or global Timing for library-only
requests. Initial automatic playback and later songs use the effective range;
Next, play now, initial manual overrides, Play on trigger and Play immediately on
join keep their silence bypasses. Playlist copies, profiles, playlist exports and
full backups include these settings.

**Play immediately on join** uses the profile linked to the world or server and
its current matching playlist. Fade in still applies. Later songs use normal gaps;
respawning, changing dimensions, switching profiles and reloading sounds do not
trigger another join start. Turning the option on while already in a world takes
effect on the next join. If no playlist is eligible yet, AFMR waits for one.
Manual controls, pause, death guards, time-window fitting and enabled jukebox
policies keep their usual behavior.

**Jukebox** cycles through four choices:

- **Off:** records do not affect AFMR playback.
- **Resume song:** fade out, pause the current song, then fade it back in from the
  held position when no audible jukebox remains.
- **New after silence:** fade out and end the current song. After the record ends,
  wait a full normal silence before scheduling another song.
- **New immediately:** fade out and end the current song. When the record ends,
  start the next eligible song without silence or a context buffer.

All enabled modes block new AFMR music while a record is audible. They use the
independent Fade out and Fade in values; combat death keeps its own fade setting.
Overlapping jukeboxes keep the hold until the last audible record stops. Muted or
out-of-range records do not hold AFMR. Ending a record never clears a manual Pause,
and the usual rule preventing new music while dead still applies.

**Minecraft defaults** immediately saves 600–1200 second gaps and zero
fade/buffer and one song before silence, replacing even unsaved timing text.
It preserves your other settings.
This approximates normal gameplay music delays; it does not reproduce every special
Minecraft music timer. Changed global silence settings apply to future gaps; an existing
countdown remains. Playlist custom ranges remain independent of these global values. Next song bypasses that countdown.

Changing Minecraft's **Options → Music & Sounds → Music Frequency** also sets
AFMR's global minimum and maximum silence (playlist custom ranges take precedence):

| Music Frequency | AFMR silence |
| --- | --- |
| Default | 10–20 minutes |
| Frequent | 10 minutes |
| Constant | 5 seconds |

These presets follow Minecraft's normal gameplay timings. A frequency change
restarts a pending silent countdown with the new range; it never cuts a playing
song. A manually paused countdown remains paused. Fades and context buffer stay
as configured. You can edit custom gaps afterward; saved custom timing is preserved
on startup, and AFMR timing edits do not change the native option's selected preset.

### Letting a song finish

Each playlist's **Playback** panel also has **Play on trigger**, off by default.
When it becomes the automatic winner, this starts the current song's normal fade
immediately, skips the context buffer and silent gap, then fades in the new song.
Entry/exit finish protection and Continue Playing on Match still apply; a protected
song finishes before the new playlist starts without a gap. Later songs in the
same playlist use normal silence. Priority, time-window fitting, manual overrides,
previews, pause and death rules remain in effect. Manual **Skip / play** still
uses the normal gap.

For an 8pm Minecraft cue, start its Time range at **14000**, put it above other
eligible playlists, enable **Play on trigger**, and enable **Finish song on exit**
to protect its song from later automatic changes. Manual controls can still stop it.

Every playlist has independent **Finish song on entry** and **Finish song on exit**
switches, initially off. Suppose a Day outside song is playing and Combat wins:

- **Day outside's exit** on lets its song finish.
- **Combat's entry** on also lets that existing song finish.
- With Continue Playing on Match off, both must be off for normal interruption.

The playing song continues to belong to the playlist that started it, even through
several context changes. A playlist that becomes selected without starting a song
does not take ownership. After the song ends, the normal gap applies unless the
incoming playlist has Play on trigger enabled, and AFMR checks
the current situation again. Manual skip/Next and combat death fade/cut take
precedence. If combat should interrupt ambient songs, check both relevant switches
and Continue Playing on Match: a shared eligible song can also be protected by it.

**Continue Playing on Match**, in **Timing**, adds conditional
finish protection to automatic playlist changes. After the context buffer, AFMR
checks the playing song's library ID against the incoming playlist's eligible
music. A shared song continues from its current position without restarting or
fading out. Disabled assignments do not match; automatic vanilla inclusion counts
only when that vanilla track is currently applicable. Matching uses library
identity, so two files with the same title do not match.

Every later accepted automatic playlist change checks again. If the new playlist
does not include the song, normal entry/exit and fade rules apply. The song keeps
its original source playlist throughout. When it ends, the normal silence follows,
and the next song comes from the situation then. Manual overrides still switch
music as usual and take priority over automatic selection; manual skip/Next and
combat death fade/cut still take precedence. This global option starts off.

### Shuffle

Each playlist has its own shuffled cycle: eligible songs are drawn once before
reshuffling, with immediate repeats avoided when alternatives exist. Leaving and
returning preserves the remaining cycle. Automatic play and overrides share it;
explicit previews are separate. Skipped/failed selections count as drawn. A playlist
with one song can repeat. Changing membership preserves already-drawn songs where
possible. Cycles reset on disconnect, sound reload, or restart.

AFMR also remembers each playlist's own last drawn song across swaps, so returning
after another playlist played does not immediately repeat that song at a new
shuffle cycle when alternatives exist. Switching profiles resets the session state.

Use **Add playing to playlist** in the workspace or Inspector to assign the
current song to another playlist. The picker captures the song that was playing
when opened. The original assignment and playback remain intact; an already-off
destination assignment stays off. Files are never moved or copied.

## Manual overrides and keybinds

Select a playlist in **Priority**, then use the workspace controls:

- **Play one song:** fade out the current song, bypass the initial gap/buffer,
  and play one shuffled song. Its ending or normal Skip returns to automatic
  selection and normal silence.
- **Until released:** make the same initial switch, then keep choosing from that
  playlist through context changes. Normal gaps apply between songs. This is a
  toggle mode, not a requirement to physically hold a key.

Overrides ignore conditions, priority, Automatic off, Manual only, and the global
combat-trigger switch. They require an existing playlist with eligible songs;
song-level off and failed songs stay excluded. Turning Automatic off during an
override does not stop it. An empty, deleted, or entirely failed target releases
automatically. Death handling still applies.

**Automatic** releases the override and restores normal transition/finish rules;
it need not cut the song immediately. **Preview** cancels it for a preview.

To use keyboard shortcuts or Stream Deck:

1. Open **Keys** and select one of eight override slots.
2. Choose a playlist and **Bind: one song** or **Bind: until released**.
   Slot 1 initially targets Combat in held mode; others start unassigned.
3. In **Options → Controls → Key Binds**, bind **Music override slot 1–8**.
   Also bind Next, Pause, or Return to automatic if useful.
4. Have Stream Deck send those keys while Minecraft has focus. Press the same
   held-mode slot again to release; a one-song slot requests another song each time.

Slot targets/modes are saved by AFMR; physical keys are saved by Minecraft.
Overrides clear on disconnect, sound reload, or restart.

### Next, normal Skip, and Pause

**Next song** immediately stops the current song and requests the first ready queued
song, or the next shuffled choice when no queue is ready, bypassing silence, buffer, fade-out, and finish protection. Fade-in still
applies. It preserves overrides, including one-song mode; without an override it
uses the current automatic winner, even after a preview or during a retained song.
With no eligible target, it leaves the current song alone.

**Skip song (with normal silence)** ends the song and keeps the usual gap. The
editor's **Skip / play** does this while playing; while idle it ends the silent
wait without bypassing context rules.

**Pause/resume AFMR music** holds the audio position, silence, fades, and buffer.
Game sounds and live environment/combat detection continue. Next or override while
paused queues the request until resume; a preview opens paused. Death Cut still
discards a combat song; death Fade waits for resume; Finish retains it. Returning
from Minecraft's pause menu preserves an explicit AFMR pause. AFMR pause clears
on disconnect, sound reload, or restart.

## Queue and listening history

Select one or more enabled songs in the playlist table and click **Queue** to append
them in the table's current order. The queue holds up to 100 requests; deliberately
queuing the same song again is allowed. Adding requests leaves the current song and
existing silence countdown alone. Songs then play in queue order with the **normal
silence between them**. Use **Next** or **Queue / history → Next song now** to skip
the current song/wait when desired.

In **Queue / history → Queue**, select a request to move it Up/Down or Remove it.
**Clear queue** removes pending requests without cutting the song already playing.
Queue mode bypasses automatic conditions, context interruptions and time-window
fitting. Automatic off and Manual only are allowed; the source playlist's disabled
song switches remain respected. Removed/disabled/failed entries or deleted source
playlists are skipped rather than blocking later requests. Audio-open failures are
reported and quarantined for the session; repair the file and use Inspector's Retry
failed before queuing it again. Pause, jukebox holds, fades and death guards still apply.

After the queue empties, automatic selection resumes, or an existing held override
continues. A **new** playlist override takes precedence and leaves requests waiting:
one-song mode releases them after its song, while held mode waits for release.
**Use queue next**, **Automatic**, or adding another request gives the queue priority
again without interrupting the current song. Preview interrupts normally and leaves
pending requests intact. Pending requests are session-only and clear on disconnect,
sound reload, or restart.

**Queue / history → History** shows the most recent 100 successful starts, newest
first, saved between game launches. Hover a row for artist/album, the playlist name
at playback time, automatic/override/queue/preview origin, and the selection reason
and captured context. Pause/resume does not add duplicate entries. History records
starts, including songs later skipped, rather than counting only completed listens.

Select a row to **Play now** as a preview, **Queue** it again, or **Add to playlist**
and choose a destination. Existing enabled/disabled assignments stay as they were.
Songs removed from the library remain visible in history but cannot be replayed or
assigned until reimported. Re-queue respects the original playlist's song switch
when that playlist still exists; otherwise it is a direct library request.
**Clear history** empties saved history without interrupting music. History is saved
separately in `config/afmr/history.json`, with metadata and track IDs rather than
absolute music paths. No music files or foobar data are rewritten.

## Combat and death

Open **Timing → Combat settings** for the global detection switch, triggers,
quiet period and death options. A playlist's **Name & conditions → Combat**
controls its combat filter separately.

**Combat music** enables automatic combat detection globally. **On attack** and
**On damage** are independent. **Triggers: combatants only** is the default:
attacking a targeted hostile/neutral mob or another player, or receiving damage
attributed to one, refreshes the **quiet period** (1–600 seconds, initially 15).
Passive animals, mining, empty swings, and environmental damage such as falling,
fire, or drowning do not refresh it. Melee attack attempts count; ranged attacks
count when an attributed hit is observed. Projectiles use their owner when known.
Merely losing health or absorption is no longer a combat trigger. The global
**any entity / any damage** option permits passive targets and environmental damage.
Death, respawn/player replacement, and disconnect clear the combat state. An
override can still force Combat when global detection is off.

In **singleplayer**, once a qualifying attack or hit starts combat, hostile or
angered combat-capable mobs that are still targeting you keep it active even
between hits. Losing the last target starts the full quiet countdown. A mob merely
noticing you cannot start combat, and targeting cannot restart an expired fight.
Only loaded mobs are checked; AFMR does not load terrain to find enemies.
In **multiplayer**, combat uses the attack/damage timeout: vanilla servers do not
send a general mob target field to clients.

For a PvP playlist, open its **Edit conditions → Combat**, select **in combat**,
and turn **Players only** on. Only attacks involving you and another player refresh
its separate timer; fighting mobs or being targeted by them does not extend it.
With **peaceful** selected, Players only means not currently in PvP, even if fighting mobs. **Any**
disables the Combat filter. Combine this filter with other conditions using the
playlist/group's usual All/Any/Not controls.

**Edit conditions → Health** enables a threshold in red hearts (1 heart = 2 health
points). Choose **below** or **above**; decimals are allowed. At exactly the threshold,
neither direction matches. Golden absorption hearts are excluded. For example,
**below 3 hearts** matches at 2.5 red hearts even if absorption is present. Health
does not require combat detection and works with the existing priority, transition,
finish-song, and manual-override rules. Death's new-music restriction still applies.

On **Death options**, choose **start fading immediately** (default), **cut immediately**,
or **finish combat song**. The separate death fade defaults to 1 second (0–60).
It bypasses the ordinary context buffer and entry/exit protection. These policies
apply to songs started by Combat or a custom playlist requiring combat; an explicit
preview is independent.

**New music while dead: paused** is the default and prevents new scheduled songs,
including override songs, until respawn. **Allowed** permits them. This setting
does not mean AFMR's manual Pause has been activated.

## Volume, ReplayGain, and artwork

AFMR uses Minecraft's **Music** and **Master** volume and selected output device.
It handles vanilla and local background music together while in a world. Main-menu
music, discs, and other sound categories retain Minecraft's normal behavior.

Select a playlist, then **Volume** in the right Playback panel. The slider adjusts
it from **0-100%**, with **Reset to 100%**. Changes apply to its playing song on the
next playback tick. Playlist volume combines with Minecraft volume and normalization;
it does not rewrite tracks. Retained songs keep the volume of the playlist that
started them, including through automatic context changes. Queues and overrides
use their source playlist's volume; standalone previews and source-free queue/history
requests use normal volume. While paused, changes take effect on resume. A 0% playlist
still plays silently and does not fall through to another playlist.

**Normalize**, below the playlist song table, starts a one-click loudness
job for that playlist's imported music, including assigned songs currently disabled.
Search filters do not limit the scan. It opens a progress screen with **Cancel**;
completed measurements are kept if cancelled. Vanilla tracks stay unchanged.

Valid existing ReplayGain **track** gain takes precedence, including a 0 dB tag.
Otherwise AFMR reuses a matching cached scan or analyzes the decoded audio toward
a shared **-18 LUFS** target. A song uses the same adjustment in every playlist.
Normalization balances overall loudness while preserving the song's internal dynamics;
sample-peak protection can limit boosts, leaving very dynamic tracks quieter.

The finished report lists each processed song, its source (**Scanned**, **Cached**,
or **Existing tag**), and its signed **Applied gain**, such as `-8.65 dB` or `+2.10 dB`.
Select a row to see requested versus applied gain and peak-protection limits, or the
reason a file failed. Silent/below-gate audio and clips shorter than 400 ms are reported
as issues rather than given an extreme boost. Unprocessed songs are counted when cancelled.

The progress screen pauses singleplayer automatically. Analysis runs at the title
screen or while singleplayer is paused; it suspends when play resumes. In multiplayer,
leave the world to let a waiting scan continue. No launch-time or automatic whole-library
loudness scan runs. Original music and foobar files are never rewritten; cached
measurements live in `config/afmr/library.json`. A change in file size or modification
time invalidates a cached measurement; click **Normalize** again to update it.

**Timing → Normalization** starts on and controls both tags
and scanned adjustments. **Normalize** turns it on if needed. Changes apply
when the next local song opens; a playing song is not abruptly changed. Untagged,
unscanned, or changed files play at their original level until normalized again.
Album gain is not used. Supported tag storage is MP3/WAV ID3v2.3/v2.4 TXXX and
FLAC/OGG Vorbis comments. Tags must be in the audio file, not just a foobar database.
Without a valid peak tag, existing gain can reduce volume but will not boost it.

Open **Display** to enable/disable
now-playing information and choose a style:

- **Minecraft toast** (default) uses the real native music-note toast, including
  song information in the pause menu. It shows local artist/title metadata as
  literal text and uses Minecraft's translated titles for vanilla tracks.
  The native Music Toast control on this page is the same setting as in
  **Options → Music & Sounds**: Never, Pause menu, or Pause menu and toast.
  Choose **Pause menu and toast** for an in-game popup. Minecraft's own setting is
  preserved; AFMR does not turn it on automatically. Native timing is five seconds
  multiplied by Minecraft's notification display-time setting; there is no album art.
- **Hybrid toast** uses Minecraft's native toast frame, initially at the top left, with AFMR's
  artwork and custom controls. Album art replaces the animated music notes when
  available; missing art uses Minecraft's colored music-note sprite.
- **AFMR album-art card** initially uses a bottom-left popup with a black background and no
  accent bar. Both custom styles use
  embedded artwork first,
  then adjacent `cover.jpg`, `folder.jpg`, `cover.png`, or `folder.png`. Missing
  artwork does not prevent playback. Both styles are independent of Minecraft's
  Music Toast setting; the native display is suppressed for AFMR playback.

**Display settings** has two tabs: **Widget** and **Menus**. Widget contains all
now-playing controls in one panel. Use the mouse wheel, scrollbar, Page Up / Page
Down or Home / End to reach more rows on smaller windows. Home / End keep their
normal editing behavior while a text field is focused. Preview and Save stay in
the footer; scrolling keeps uncommitted text intact.

The **Widget** tab includes **Corner**: Default, Top left, Top right, Bottom left
and Bottom right. This applies to all three styles. Default preserves top-left
Minecraft/Hybrid and bottom-left AFMR placement. Bottom popups stay above the hotbar
when they cannot fit beside it; native pause-menu song information retains its normal
position. Minecraft's Music Toast visibility option still governs native popups.

**Delay (s)** delays the notification by **0–60 seconds** (default 0).
The timer starts when audio actually begins and pauses with AFMR playback. A song
that ends before the delay never displays a notification. Once shown, the widget
keeps its full ordinary display duration. This setting applies to every supported
style, including Minecraft's native toast and pause-menu song metadata on 26.3.
It is shared across profiles.

The same panel sets style, visibility and custom popup duration (initially
7 seconds; 1–60 allowed). **Scale (%)** sets scale (50–200%, including text and
artwork); **Width** sets 140–600 GUI pixels before scaling. A widget that cannot fit
the viewport shrinks automatically; the AFMR card stays above the hotbar on narrow
screens. Height follows the enabled information rows.

**Text** chooses **single line** (default) or **word wrap**. Word wrap moves whole
words to additional rows within the chosen width; long titles grow vertically
instead of widening the widget.
Artist, album and playlist information wrap the same way. Wrapping never splits a
word. A word too wide for an entire row stays intact and uses the scrolling option,
or an ellipsis if scrolling is off. The small-viewport scale limit also accounts
for the added rows. These layout controls apply to Hybrid and AFMR.

The information switches independently select artwork/music notes, song title,
artist, album, song length and original source playlist. Widgets show song information
without a redundant "Now playing" label. Empty
metadata and unknown lengths are omitted. Previews show **Preview** as their source.
**Scroll long text** moves overflowing text within the chosen width at 24 GUI pixels
per second, pausing for 1.5 seconds at each end. Off (the default) clips long text
with an ellipsis. Increase popup duration to allow very long titles to scroll fully.
Disabling artwork removes the entire icon column. Disabling every field hides the
custom widget.

The background controls in **Widget** customize the AFMR card. Choose Black, Gray,
Blue or White, or enter any six-digit RGB color (`#RRGGBB`, with an optional `#`).
The color swatch shows the saved color. **Opacity (%)** accepts 0–100%:
0 is transparent and 100 is solid. Text, artwork and music notes retain their own
opacity. **Reset appearance** restores black at 92% opacity. These background
controls apply to the AFMR card; Hybrid keeps Minecraft's textured frame.

The **Menus** tab controls AFMR's workspace and settings backgrounds. Drag
**Background opacity** from 0–100% to preview the change immediately; the default
is 80%. Text, artwork and foreground indicators stay opaque. **Blur background**
uses Minecraft's pause-menu blur on the custom image or game, with strength controlled
by Minecraft's **Menu Background Blur** option. It starts enabled. Fully opaque
menu panels skip hidden blur work. These choices do not change widget appearance
or pause gameplay. Fabric 1.20.1 has panel opacity and custom images, without blur.

Open **Menu colors…** to change Background, Panels, Buttons, Hover, Selection,
Highlights, Text, Secondary text, Borders and Scrollbars. Enter six RGB hex digits,
with an optional `#`; each field has a color swatch and valid values preview live.
Use the page arrows for more colors. Related shades follow the chosen base color
so alternating rows and headings keep their differences. Invalid values block
page changes and Save & back; hiding the page retains drafts.

Click any color swatch to open a visual picker. Drag inside the square for
saturation/brightness and along the hue strip to change hue. Presets, Original/New
swatches and a hex field are provided. The controls stay synchronized and preview
the setting live. Focus a gradient and use arrow keys for small adjustments.
**Apply** keeps the color; **Cancel** or Escape restores the color from when the
picker opened. Hiding/reopening the picker retains its draft. The same picker is
available from the widget background swatch under **Display → Widget**; on
NeoForge, that widget option requires the AFMR card style. Opacity stays separate.

Colors apply to AFMR's workspace and child screens across every profile. Text and
highlights stay fully opaque; panel transparency, blur, custom images and HUD
appearance retain their separate settings. Minecraft-native text boxes, option
buttons and tooltips keep their native skins; errors retain their warning colors.
**Reset colors** restores the original palette and can recover from invalid input
without changing other appearance settings. **Reset menu appearance** resets the
palette together with panel opacity, image settings and available blur.

Open **Custom background… → Choose image…** to select a PNG, JPEG, static WebP or
GIF in Explorer. Animated GIFs loop with their frame timing and transparency.
The image appears behind all AFMR workspace/settings panels; on 26.3 it is drawn
**before** the menu blur. Reduce panel opacity in Menus to show more of it.
**Image opacity** controls the wallpaper separately. **Fill** crops the center to
cover the screen; **Fit** shows the whole image with gaps; **Stretch** fills the
screen without cropping. Transparent pixels and Fit gaps show the game/panorama.

AFMR keeps a copy at `config/afmr/menu-background.png` (stills) or
`config/afmr/menu-background.gif` (animations); moving/deleting your
original afterward does not break it. Originals are read-only. Imports accept up
to 20 MiB / 64 megapixels and resize the longest edge to at most 2048 pixels.
GIFs accept up to 512 frames and 256 million source patch pixels in total, and resize
further to keep retained frames within 64 MiB of pixel data. Animated WebP is not
supported; use GIF for animation. Full configuration backups include the animation.
Preparation runs in the background, with cached frame textures for rendering. These
appearance settings are shared by every world/server profile and are not included
in playlist exports. Invalid images leave the previous copy intact.

**Image: off** keeps the selected image for later. **Remove image** deletes only
AFMR's copy. **Reset image settings** disables it and restores Fill / 100% image
opacity while keeping the copy. **Reset menu appearance** also restores 80% panel
opacity, the original menu colors and, on 26.3, enabled blur.

**Preview widget** shows the current song or sample metadata while idle, without
starting playback. These custom controls apply to Hybrid and AFMR; Minecraft style
keeps its native appearance, pause-menu display and timing. Switching styles or
changing custom settings refreshes the current song's notification. Only custom
styles with artwork enabled load HUD artwork; the editor's art remains available.

## Troubleshooting

The **Inspector** separates the song playing, its original source playlist, the
automatic winner, and any override. Its main view updates once a second. Select a
playlist and open **Details** for conditions and song counts. Details is a snapshot;
return to the main view for updates. Hover clipped explanations to read them fully.

| Symptom | Check |
| --- | --- |
| Nothing plays after launch | Join a world; check Music/Master volume, AFMR Pause, and Inspector's silence timer. Fresh gaps are 10–20 minutes. Try a preview or Next. |
| Imported songs do not play automatically | Assign them from the left library pane to a matching playlist. Check song switches, Automatic, Manual only, conditions, and priority. |
| A different playlist wins | Inspect the ranked list. Move the intended playlist higher or adjust its filters. |
| Inspector winner differs from the song's source | A buffer, fade, finish protection, preview, or override may retain earlier music. Read the explanation. |
| Vanilla music still plays | Turn automatic vanilla inclusion off in applicable playlists and disable/remove explicit vanilla assignments. |
| Home never matches | Capture it in the correct world and dimension; radius includes height. |
| Low Y or nighttime does not select exploration music | Underground also requires cover/depth. Darkness requires cover/light; Nether/End use their own switches. |
| A key does nothing | Assign it in Minecraft Controls, check conflicts, focus Minecraft, and close chat/other screens. Slot keys also need a target assignment. |
| A song fails or moved files remain listed | Restore/reimport the file at its correct path. Missing/failed songs can stay listed. Use **Inspector → Retry failed** or rescan after repairing them. |
| Playback says no available songs despite a count | Counts use saved entries, not a fresh disk check; a missing file may fail only when opened. |
| Settings will not close/save | Read the field error and enter valid values. Check the game's `logs/latest.log` for load/save errors. |
| Library could not load / save blocked | Close Minecraft and preserve `config/afmr/library.json` and its backup before investigating. AFMR keeps unreadable/unsupported JSON and blocks overwriting it; it does not automatically restore the backup. |

For troubleshooting, open **Inspector → Copy diagnostic summary**. The button
briefly says **Summary copied!**; paste the text into your message when asking for
help. It copies locally and sends nothing automatically. The report contains
installed versions, playback reason/source/override state, timers, volume, scan
counts, a Java heap snapshot, priority rows and the inspected playlist's nested
condition settings/results. Select the relevant playlist before copying.

Playlist numbers match the current priority list. Names/IDs, song metadata, music
paths, world/server identifiers, X/Z coordinates, saved locations, group labels
and raw errors are omitted. Biome/dimension/item resource IDs, current block Y and
condition ranges remain useful for diagnosis. Playback errors use a brief inferred
category; detailed exceptions remain in your local log. The report includes up to
64 priority rows plus the inspected playlist, and caps clipboard text at 48,000
characters. Heap figures are a single Java-memory snapshot, not native/GPU memory
measurements or a memory-leak test. Copying does not scan files or change playback.

Failed songs are excluded for the session to avoid repeated errors. **Retry failed**
clears that exclusion without scanning or repairing files. A completed rescan also
clears it. For a bug report, include the diagnostic summary, reproduction steps
and relevant log lines if needed. Paths in logs
or configuration can reveal personal folder names.

## Sharing playlist setups (.afmrp)

Open **M → Import / export**. Select one or more playlists in the list, or use
**All / None**, then **Export selected...**. Explorer opens a save dialog; use a
filename ending in **`.afmrp`**. The selected playlists are saved in their current
priority order, even if you selected them in a different order. The export snapshot
is taken before opening Explorer, so later profile changes do not alter it.

Shared files retain playlist names, Automatic/Manual only, both finish-song
switches, Play on trigger, Minecraft music, volume and every condition parameter,
including disabled settings and nested All/Any/Not groups. By default they contain
**settings only**. Switch **Include song list: On** before export to also retain
song assignments and their on/off states. The list contains folder names and
relative paths, plus resource identifiers for explicit Minecraft/resource-pack
songs. **Audio files and absolute music paths are never embedded.** Global
timing/combat/widget settings, keybinds and profile links stay separate. The saved
Minecraft music option still controls normal vanilla inclusion.

Choose **Import .afmrp...** or drag **one** `.afmrp` onto the main workspace.
Review the playlists and hover a row for its condition summary/playback flags.
Choose a destination profile and **Insert** above the workspace's selected playlist,
at top priority or at bottom priority. Relative order within the imported group
is preserved. A missing selected anchor falls back to bottom. **Add playlists**
creates independent new playlists; existing ones remain. **Cancel** changes nothing.
Names may repeat; rename imported copies in Name & conditions when needed.

If the file includes songs, turn **Import songs: On** to restore those assignments.
AFMR checks files on the background library worker and shows progress and
found/missing counts. It automatically tries linked music folders with a unique
matching folder name. If either side has ambiguous names, choose explicitly.
Open **Locate music folders...**, select a root and use **Locate selected folder...**
to choose it in Explorer. Choose the folder containing the displayed relative path:
for `Albums/Track.wav`, choose the folder containing `Albums`. AFMR checks that
exact path; it does not guess between songs with the same filename or title.
Existing library entries are reused rather than duplicated. Selecting a root
imports only the listed songs, not every other file in that folder.

You can import with files still missing. Their assignments and switches are
preserved, and AFMR opens **Library repair / missing sources**. An unlocated root
appears as a missing folder named after the exported root; use **Locate selected...**
to reconnect its relative paths. Missing/renamed songs can be located individually.
Unlocated references use paths under `config/afmr/missing-imports` until repaired;
no music files or placeholder folders are created there. Repairs survive restart
and retain playlist assignments. Unavailable resource songs require their resource
pack/content to be installed; they cannot be repaired by choosing a local file.
Keep **Import songs: Off** to import settings without any library or song changes.
Folder choices stay in the preview until **Add playlists**; Cancel saves nothing.

Location filters keep coordinates, radius and dimension but omit the sender's
original save-folder paths and server addresses. **At Apply, all configured areas
bind to the world/server you are currently in**, including configured locations
whose filters/groups are disabled. Their coordinates do not move to your player.
Join the intended world before importing a setup containing configured locations;
the Add button stays disabled outside a world. A setup without configured locations
can import from the title screen. Modded biome/item/structure/dimension IDs remain
as saved and require the corresponding content to match.

Each file supports up to **128 playlists**, **100,000 distinct song references** and
**16 MiB**. Malformed files and
unsupported fields report an error without adding playlists. M can hide a preview
and reopen it later. Imports/exports use the existing background library worker;
they do not modify music or foobar files.

## Transferring your full configuration

Open **M → Import / export → Full configuration… → Export full backup...** and save
one **`.afmrb`** file. In the new install, choose **Import full backup...** from the
same screen, or drop one backup onto the workspace. Review its counts, then choose
**Replace on restart** and restart Minecraft.

The backup includes every profile, playlist and condition, song on/off assignments,
settings, library paths and cached metadata/loudness, folder destinations,
world/server links, override slots, AFMR shortcuts, listening history and the
custom background image. It does not include audio files or other Minecraft
settings. Import replaces the destination's AFMR configuration; Back cancels a
review that has not been applied. The current session keeps its existing setup
until restart. Prior managed files remain in `config/afmr/restore-previous`.

Music paths and world/server links retain their exact original values. If music
moved, use **Repair / missing** to relink its folders. Review profile links for a
renamed or moved world. Unsupported shortcuts are shown in the review and become
unbound; rebind them in the destination install. Fabric 1.20.1 retains its supported
AFMR card settings and has no NeoForge key modifiers, menu blur or native/hybrid
music-toast styles. Shared playlist, playback, library and appearance controls
transfer between both maintained builds.

Use `.afmrp` when sharing selected playlist templates with someone else. Full
backups contain your complete setup, including absolute paths and world/server
links.

## Saving, upgrading, and sharing

AFMR saves its library, profiles, playlists, settings, areas, and slot assignments in the game
instance's `config/afmr/library.json`, with `library.json.bak` as a rolling previous
copy. Edits take effect in memory immediately; saves run in the background and
normal shutdown waits for pending saves. Use **Save & back** on screens with fields.
Do not edit the JSON while Minecraft is running.

Audio paths are absolute. Copying this JSON to a friend does not supply your music
or make your paths valid on their computer. Playlist areas also include world/server
scope. Use **`.afmrp`** to share playlist settings instead. Give friends the **runtime JAR and this guide**, let each import their own
music, and use the [quick reference](#quick-reference) for day-to-day controls.
The guide can be sent by itself; no developer tools are needed to read it.

For upgrades, close Minecraft, back up `config/afmr`, and replace the AFMR JAR,
keeping one version. Existing settings retain their saved values; they do not
automatically adopt fresh defaults. Historical migration details are in the
[release notes](../CHANGELOG.md). Follow-up work is tracked in the [backlog](../BACKLOG.md).
