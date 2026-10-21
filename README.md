# animal's final music replacer — 0.5.2

Client-side custom soundtrack for **Minecraft Java 26.3 / NeoForge 26.3.0.90+**.

Development now uses this repository root. Start with [project decisions](PROJECT.md),
[build and Git instructions](DEVELOPMENT.md), and [remaining complex tests](DEFERRED-TESTING.md).
The validated 0.5.2 baseline is tagged `v0.5.2`. Local workspace/release archives stay under ignored `work/` and `outputs/`.


## Install or upgrade

Replace the old AFMR JAR in the instance's `mods` folder with `animals-final-music-replacer-0.5.2.jar`. Keep exactly one AFMR version installed. Launch Minecraft and press **M**, or use the mod's Config button from the Mods menu.

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

## The six default playlists

Default priority, highest first:

1. **Combat** — triggered by attacking an entity or taking damage. The quiet period is configurable; combat music can be disabled globally.
2. **Home** — inside a spherical radius around your chosen position, in the captured world and dimension.
3. **Dark exploration** — under solid cover at light level 4 or lower by default, regardless of Y. Optional Nether/End switches enable it throughout those dimensions.
4. **Underground** — at or below the chosen Y (default 64), with at least five blocks of solid cover by default and no view of the sky.
5. **Night outside** — outside the underground condition during the night half of the world clock.
6. **Day outside** — outside the underground condition during the day half of the world clock.

There is no automatic per-biome or per-dimension playlist creation. **M → Playlists** opens the full priority list. Vanilla music is included automatically in most default playlists, alongside your custom tracks. Combat and Home override other contexts only when matching; disabling either lets lower-priority contexts apply. The six defaults cannot be deleted through the playlist manager.

## Custom playlists and priority

Open **M → Playlists → New playlist**, enter a name, and choose conditions. Each filter is optional. **All enabled filters must match**; with no filters enabled, a custom playlist allows any environment. Empty playlists fall through to lower-priority playlists. Automatic vanilla inclusion starts off for new custom playlists.

Available filters:

- **Location:** a spherical radius around a chosen center, scoped to the captured world/server and dimension. **Use current location** captures your position. This is independent of the default Home area.
- **Time:** inclusive world-clock ticks from **0–23999**. Presets cover Day, Night, or All day. Start greater than end crosses midnight; equal endpoints match only that tick. Uses the same Overworld clock as the existing day/night defaults, including in other dimensions.
- **Dimension:** one dimension ID, including modded dimensions. Overworld, Nether, End, and current-dimension buttons fill it in.
- **Elevation:** an inclusive minimum and maximum block Y, including negative Y. Current-Y buttons fill either boundary.
- **Cover:** any, solid cover, or open sky. Covered areas can require their own minimum depth; this does not use the default Underground Y cutoff. Water and leaves alone do not count as solid cover.
- **Biomes:** select one or more exact biome IDs. Any selected biome satisfies this filter; every other enabled condition must still match. Search the scrolling list, use **Add current biome**, or enter an ID and click **Add ID**. The list comes from the current world's biome registry, including modded biomes. Outside a world, saved selections and manually entered IDs remain available. Unavailable mod IDs are retained and marked rather than removed. Turn the filter off for any biome; an enabled filter needs at least one selection before saving. The six default playlists still ignore biome identity.
- **Biome temperature:** an inclusive range of Minecraft's biome base temperature. These values are not Celsius and do not change with altitude, weather, or time. The editor shows your current biome/value and offers cold/warm presets; elevation can be combined separately. Bounds use Minecraft's float precision so a displayed boundary such as 0.15 remains inclusive.
- **Light:** an inclusive range of local block/skylight levels **0–15**, independent of video brightness.
- **Combat:** any, in combat, or peaceful. This uses the configured combat triggers and global combat switch.
- **Water:** any, underwater, or out of water.

For example, a cold nighttime mountain playlist can require Night + minimum Y 80 + maximum biome temperature 0.15. A location and a separate dimension filter must refer to the same dimension to match together.

The playlist manager lists **#1 as highest priority** and shows each playlist's name, default/custom status and enabled state. Select a row and use **Move up**, **Move down**, **Move to top**, or **Move to bottom**. The first matching playlist with at least one enabled, available song wins. New custom playlists start above the broad Day/Night fallbacks, below the existing special playlists; move them higher when they should override Home, Combat, Dark exploration, or Underground.

Use **Settings → Name & conditions** to rename a custom playlist or edit filters. **Edit songs** opens its song table, where normal add/disable/remove controls work. Custom playlists also appear in the foobar import destination list. The main editor displays six tabs at a time, with arrows for additional playlists; **Playlists** always shows the full list. The ACTIVE line includes the winning priority number.

Deleting a custom playlist asks for confirmation and removes its assignments. Original music, library entries, and other playlist assignments remain. Any old linked-folder destination pointing to that playlist becomes library-only. Deletion is disabled while a scan is running. Reordering, per-playlist on/off controls, finish-current-song choices, and normal fades/silent gaps apply to custom and default playlists alike.

## Combat and death controls

Open **Combat → Playlist settings → Combat settings**, or **Settings → More settings → Combat settings**:

- Turn combat music on/off globally, and independently enable **On attack** and **On damage**. Attacking a targeted entity or taking damage refreshes the quiet period when that trigger is enabled. Health/absorption loss and a rising hurt timer count as damage.
- Set the **quiet period** from **1–600 seconds**, measured since the last enabled trigger. Death, respawn, player replacement, and disconnect clear combat state.
- **On death** defaults to **start fading immediately**, with a separate **1-second** death fade. It bypasses the normal context buffer and finish-current-song setting. Set the death fade to **0–60 seconds**; zero cuts immediately.
- Other death options are **cut immediately** or **finish combat song**. The latter lets the current combat song end naturally before normal scheduling resumes.
- **New music while dead** is paused by default. Existing ambient songs can finish; automatic new songs wait until respawn. You can allow new music while dead instead. The normal silent gap still applies after a combat fade, cut, or song ending; time spent dead can satisfy that gap.

Death handling follows the playlist that started the current song, including custom playlists whose Combat condition requires combat. It does not mistake an ambient song or an explicit manual preview for automatic combat music.

The underground heuristic uses Minecraft's synchronized surface heightmap without leaves, then inspects that local column to find solid terrain beneath any water. It does not query the server-only ocean-floor heightmap. This avoids treating ordinary tree canopies, shallow house roofs, or open water alone as underground. The local column result is cached for up to five game ticks (a quarter second) and refreshed immediately when you change columns or worlds. Large built structures can still count as cover. Open **Underground → Playlist settings → Underground conditions...** to set the inclusive maximum Y, capture your current block Y, and tune the minimum cover (1–64 blocks). Both the Y cutoff and cover requirement must pass. Home takes priority over Underground by default, including inside a deep or roofed base.

## Dark exploration setup

Open **Dark exploration → Playlist settings → Darkness conditions...**:

- Set the maximum light level from **0–15** (default **4**, inclusive). Lower values require darker surroundings. The screen reports your current light level and whether you have solid cover.
- In the Overworld and other ordinary dimensions, darkness requires solid cover overhead. Open-sky nighttime remains Night outside. Tree leaves alone do not count as solid cover. This condition ignores Y, biome, and video brightness/gamma.
- **Nether** and **End** are independent switches, off by default. On means Dark exploration applies anywhere in that dimension, regardless of light or cover. Off disables Dark exploration in that dimension. Combat and Home still retain their higher default priority.
- Darkness uses the world's local light, including torches and skylight adjusted for time of day. It is a local overhead-cover check rather than a full room-enclosure simulation.

Add songs from the left pane to the new playlist. Dark exploration starts with automatic vanilla inclusion off, so its empty playlist falls through to Underground or Day/Night. You can enable vanilla inclusion in Playlist settings. All playlists remain individually switchable and reorderable; their finish-current-song setting, normal context buffer, fades, and configured silence still apply.

## Home setup

Open **Home area...** from the editor:

- **Use current location** captures your exact X/Y/Z, world, and dimension.
- Set **Radius (blocks)**; the default is 64.
- Coordinates and dimension can also be entered manually.
- **Save & back** applies the settings. Manual setup in a world binds Home to that world.

The radius includes vertical distance, and points exactly on its boundary count as inside. In singleplayer, Home is scoped to the save folder; in multiplayer, to the server address. If you move a save to a different instance, capture its Home center again. No location is sent to a server or external service.

## Column editor

The editor uses a compact local canvas without changing Minecraft's GUI-scale setting. At the standard small test window it shows about twenty full song rows instead of the old three; larger windows can show more.

- **Top:** selected-song metadata and artwork, plus current playback/context information and import controls.
- **Left:** your linked music folders, nested subfolders, individual files, All local music, and the Minecraft soundtrack. This pane scrolls independently.
- **Middle:** playlist tabs (paged when there are more than six), a filter, and a scrolling song table with On, Title, Artist, Album, and Time columns. Click a column header to sort.

Select files on the left and choose **Add selected / folder →** to add them to the open playlist. Selecting a folder adds its songs recursively. Double-clicking a file adds it directly. **Ctrl-click**, **Shift-click**, and **Ctrl+A** support multiple selections. The two search boxes filter their own panes.

In a playlist, select rows and use **Enable**, **Disable**, or **Remove**. Disable retains the assignment; Remove removes an explicit assignment without deleting the file or library entry. Click a song's On cell, or press Space with songs selected, to toggle playback eligibility. Automatic vanilla rows must be disabled to exclude them; removing an explicit override allows automatic membership again.

**Play selected** previews the last selected song from either pane immediately, including songs in disabled playlists. The preview fades in, finishes without automatic context changes interrupting it, then returns to the normal silence and scheduling rules. **Skip / play** still controls automatic playback from the current context. The **ENV** line reports time, Outside/Underground, and Combat/Home; biome does not gate the six central playlists. If no music is available, the footer explains whether the matching playlist is off or has no enabled songs.

## Import and rescan

**Add files** and **Add folder** open the operating system's native file/folder dialogs (Windows Explorer dialogs on Windows). Files can be selected together. Folder imports include subfolders. **Folders...** lets you adjust recursive scanning, rescan, and unlink folders. Importing never moves, copies, uploads, or deletes your originals. Drag-and-drop onto the editor is also supported.

Supported formats: **MP3, OGG Vorbis, FLAC, and WAV**, mono or stereo, including 24-bit FLAC/WAV and floating-point WAV. DRM, OGG Opus, and surround audio are not supported. File tags provide title, artist, album, and duration when available; filenames are the fallback.

Use **Settings → More settings → Rescan folders on launch** for optional automatic updates. Manual rescan is always available. Existing off/on assignments survive rescans. Missing or undecodable files remain listed, are skipped for the session after a playback failure, and can be retried after a rescan.

During imports and rescans, the top information panel shows live scan progress. Finding music uses an animated bar and a count of supported files found; the total is still being discovered. Reading tags shows processed/total files, percentage, and the current filename. The indicator also appears on folder and settings screens, and clears when the library finishes updating. Scanning continues if you leave the editor. An overlapping folder request counts separately so its playlist assignment is preserved.

## Import foobar2000 playlists

1. Open **M → Import playlist** and choose the existing default or custom playlist that should receive the songs. The destination list scrolls and the currently open playlist is selected initially.
2. Press **Choose .fplite files...**. The native Windows Explorer dialog starts inside `%APPDATA%\foobar2000-v2\playlists-v2.0` when that folder exists. You can navigate elsewhere and select multiple files together.
3. The songs are appended to that destination. Existing library entries and metadata are reused; repeated entries are ignored. Existing song on/off choices and the destination playlist's enabled state are preserved.
4. Open **View last import report** for the source playlist names, added/already-assigned counts, duplicate count, and skipped-entry details. Hover a detail for its full message. foobar's GUID filenames are translated using the adjacent `index.txt` when available. Details are capped at 100 messages, while totals count all entries.

Dropping only `.fplite` files onto the column editor imports them into its open playlist. Drop audio and playlist files in separate batches. Imported music uses AFMR's Fisher-Yates shuffle bags, environment priority, fades, and silence settings; foobar's playback order is not imported.

These imports read UTF-8 `.fplite` location lists. The playlist files, adjacent name index, music files, and foobar database are never modified. Music is linked at its original location, without copying or decoding entire songs during import. Foobar changes are not watched automatically; reimport after editing a foobar playlist. Reimport appends new songs and does not remove assignments that have since disappeared from the source playlist. Ordinary folder rescans still refresh audio tags.

Local MP3, FLAC, WAV, and OGG Vorbis references are supported. Missing files, archive-backed `unpack://` entries, subsong/chapter entries, network streams, and unsupported formats such as Opus/MP4/AIFF are skipped and reported. Archives are not extracted. Binary `.fpl` playlists are not supported. Playlist files are limited to 64 MiB each. Other foobar settings, cue segments, and decoder components are not imported.

## Playback inspector

Open **M → Inspector**, or bind **Open playback inspector** in Minecraft's Controls menu. Join a world for live diagnostics. The inspector refreshes once per second and separates the currently playing song, its source playlist, and the playlist that would win automatically. The current override, if any, is identified separately.

The explanation shows silent gaps, the context buffer, fades, manual previews, death handling and entry/exit finish protection. Hover a clipped explanation to read it in full. The ranked list distinguishes disabled playlists, manual-only playlists, unmet conditions, no playable songs, the automatic winner and lower-priority matches. **Details** opens a scrollable snapshot with configured conditions, biome/dimension/time/Y/light/temperature/cover/combat/water/Home data and song availability counts. Hover a clipped detail to read it. **Retry failed** clears the session's failed-track quarantine without rescanning.

Diagnostics use the runtime's existing context snapshot. The custom condition inspector shares the same comparison code as playback. Song counts are calculated only when opening a playlist's details, using its assignments and the track index.

Availability counts use saved library entries, not fresh filesystem scans. A missing file whose entry still exists is detected when playback opens it and is then marked failed for the session. Conditions shown in Details are a snapshot; return to the main inspector for updates.

## Overrides and keybinds

Open **M → Overrides...**, choose a playlist, then select:

- **Play one song:** immediately fade out the current music, bypass the initial gap and context buffer, and play one shuffled song from that playlist. When it ends or is skipped, normal silence and automatic selection resume using the environment you are in then.
- **Until released:** immediately switch in the same way, then keep selecting from that playlist regardless of context changes. Normal gaps apply between its songs. Use **Automatic**, the return-to-automatic key, or the same held-mode slot key again to release it. Release restores normal context-buffer and entry/exit behavior; a protected song may finish.

Overrides bypass contextual conditions, priority, the playlist's **Automatic** switch and the global combat-trigger switch. The target playlist must exist and have at least one enabled, available song. You can force Combat without attacking or taking damage, including with its Automatic switch off. Switching Automatic off leaves a current override and its later songs running. The normal combat-death policy still applies to a song started by Combat or a custom playlist requiring combat; preventing new music while dead also applies. Deleted, empty or fully failed override playlists release automatically. Individually disabled songs remain excluded, and bad songs are quarantined as before. **Play selected** remains an independent preview and cancels an override.

For a playlist that should never activate automatically, turn off **Playlist settings → Automatic**, or enable **Manual only**. This works for defaults and custom playlists. A custom playlist with no conditions otherwise matches every environment. Either choice keeps your dedicated manual collection out of automatic rotation while preserving overrides, imports and previews. Turning Automatic back on does not clear a separate Manual only choice.

For Stream Deck or keyboard control:

1. Open **Overrides... → Keybind slots...** and select one of eight slots.
2. Select a playlist and choose **Bind: one song** or **Bind: until released**. Slot 1 initially targets Combat in held mode; the other slots start unassigned.
3. In **Minecraft Options → Controls → Key Binds**, bind **Music override slot 1–8** to your chosen keys. These keys start unbound. You can also bind **Open playlist overrides**, **Open playback inspector**, and **Return music to automatic selection**.
4. Configure your Stream Deck to send those same keys while Minecraft has focus. A held-mode slot toggles its override off when pressed again; a one-song slot requests another shuffled song each time.

Slot assignments/modes are saved with AFMR settings; physical keys use Minecraft's normal keybind storage. Keybind actions do not activate while another screen/text field is open. Active overrides are temporary and clear on disconnect, sound reload or restart. No commands, server installation or simulated combat events are needed.

## Shuffle and repeat avoidance

Automatic playback and overrides share an independent **Fisher-Yates shuffle bag per playlist**. Every eligible song is drawn once before that playlist reshuffles. At a reshuffle boundary, the last song is avoided when another choice exists. Leaving a playlist and returning later preserves its remaining order, and overrides use that same order. Skipped or failed selections count as drawn; disabled, missing and quarantined songs are excluded.

When eligible membership changes, new songs join the unplayed part of the cycle and removed songs disappear without resetting songs already drawn. Updated metadata is used on the next draw. A one-song playlist can repeat. Explicit **Play selected** previews are outside these bags. History lasts for the current runtime session and clears with disconnect/sound reload/restart; it is not written into the music files or a growing history log.

## Next song and pause

Bind **Next song (skip silence)** and **Pause/resume AFMR music** under **Minecraft Options → Controls → Key Binds**. Both start unbound and can be triggered by the same keys from a Stream Deck. **M → Inspector** also provides Next song and Pause/Resume buttons. Music keybinds remain inactive while typing or another screen is open; the Inspector buttons work within its screen.

**Next song** stops the current song and starts the next shuffled choice without waiting for fade-out, silence, finish protection or the context buffer. The new song still uses the configured fade-in. It uses an active override's playlist and preserves either override mode; for a one-song override, the replacement song becomes the one that completes before returning to automatic. Otherwise, Next uses the current automatic choice, including after an explicit preview or while an older playlist's protected song is finishing. It also works during a silent gap. If no eligible playlist is available, it reports that and leaves the current song alone. Individually disabled/failed songs remain excluded.

**Pause/resume AFMR music** pauses only AFMR's music channel at its current position. It holds the silent-gap countdown, fade progress and context buffer, while game sounds and live environment/combat detection continue normally. Press it again to resume. Next while paused queues a replacement but does not start it until resumed. Override requests stay queued; a selected preview opens paused. Combat death policy is still observed while paused: Cut discards the combat song, Fade begins when resumed, and Finish song retains it. New-music-while-dead settings still apply.

Pause is temporary and clears on disconnect, sound reload or restart. Returning from Minecraft's pause menu preserves an explicit AFMR pause. Main-menu music and music discs remain under Minecraft's normal control. The existing **Skip song (with normal silence)** binding still ends the current song with the usual gap; skipping a one-song override that way still returns to automatic. The existing editor Skip / play button retains that behavior.

## Playback behavior

Open **M → Settings → Restore Minecraft defaults** to restore a random **600–1200 second (10–20 minute)** gap, **0-second fade**, and **0-second context buffer**. Fresh configurations use these values too. The reset replaces the four timing fields, including any unsaved values, and saves immediately. Existing installations retain their saved timing until you change or reset it. Timing changes affect future gaps; a gap already counting down is retained.

This preset follows Minecraft 26.3's normal GAME/CREATIVE music delays and adds no AFMR fade or buffer. It approximates normal gameplay timing rather than reproducing Minecraft's initial five-second delay, special dimension/biome delays, or music-frequency options. It preserves music, playlists, priority, per-playlist **Finish current song**, combat/death options, folder rescanning and now-playing settings. For smoother context changes, set Fade and Context buffer back to **3 seconds** each: stable context → fade out → configured silence → fade in.

Each default or custom playlist has two independent toggles under **Playlist settings**:

- **Finish current song on entry:** when this playlist becomes active, let the song already playing from another playlist finish. Existing **Finish current song** choices keep this behavior.
- **Finish current song on exit:** let a song started by this playlist finish when another playlist takes over, or when none matches. This new toggle defaults off.

When a song from A is playing and B becomes active, **either A's exit toggle or B's entry toggle keeps the song playing**. If both are off, the normal context buffer, fade-out and silence apply. The entry setting of A and exit setting of B do not decide that transition. For example, allowing Day outside songs to finish on exit also delays Combat, even with Combat's entry toggle off; turn both relevant toggles off when Combat should interrupt ambient music.

A retained song keeps its original source playlist through further transitions. A playlist that becomes active without starting a song does not inherit ownership of that song. Returning to the source playlist keeps the song playing. Normal silence follows a natural ending; the current context is checked again when the next song starts. Combat's death-fade/cut setting and manual skipping take precedence over both toggles. Manual previews still play through independently.

**Skip / play** skips a current song with normal silence, or bypasses a pending silent gap when idle. The Controls menu also offers a rebindable skip action. The timing-reset button preserves both per-playlist finish choices.

Custom and vanilla background music use one scheduler and Minecraft's sound engine, music/master volume, output device, and pause handling. Discs and other sound categories keep their normal behavior. Main-menu music stays under Minecraft's control.

The bottom-left now-playing card fades in and out. Artwork comes from embedded tags, then `cover.jpg`, `folder.jpg`, `cover.png`, or `folder.png` alongside the song. It moves above the hotbar on small GUI layouts. Card visibility and duration are configurable.

## ReplayGain

**M → Settings → More settings → ReplayGain track tags** is on by default. AFMR reads `REPLAYGAIN_TRACK_GAIN` and `REPLAYGAIN_TRACK_PEAK` from the song's existing metadata when it opens for playback. Supported tag storage is ID3v2.3/v2.4 TXXX fields in MP3 and WAV, and Vorbis comments in FLAC and OGG Vorbis. Field names are case-insensitive. Album gain is not used; this mode balances individual songs for shuffled playlists.

Tag reading happens on the existing audio worker, once per song opening. There is no full-library rescan or loudness analysis, and already-imported songs work immediately if their audio files contain the tags. Changes to tags are picked up the next time that song opens. The toggle also applies from the next song, including manual previews. Files without a valid track-gain tag, unsupported tag storage, or unreadable metadata play at their original level. Vanilla/resource-pack music is unchanged.

Gain scales the decoded PCM before Minecraft's music/master volume and AFMR's fades. Peak protection limits the adjustment so the tagged peak will not exceed full scale. Without a valid positive peak tag, attenuation still works, but amplification is capped at the original level. A final sample clamp prevents integer wraparound if the stored peak is inaccurate; it cannot repair existing clipping or guarantee against intersample peaks. Normalization preserves changes in volume within a song.

AFMR never writes or changes your audio files or foobar settings. Tags must be stored in the audio files; foobar's component-specific settings and playlist/database-only information are not read. A built-in loudness scanner is deferred in `BACKLOG.md`.

Implementation references: [ReplayGain amplitude scaling](https://replaygain.hydrogenaudio.org/player_scale.html), [tag mappings](https://picard-docs.musicbrainz.org/en/v2.13/appendices/tag_mapping.html#replaygain-track-gain), and the user's [foobar ReplayGain components](https://www.foobar2000.org/components/tag/ReplayGain). No foobar component code or binaries are bundled.

## Data and build

Data lives in `config/afmr/library.json`, with a rolling `.bak` copy. Edits take effect immediately; a background writer saves an owned snapshot and keeps only the newest waiting snapshot during rapid edits. Normal game shutdown waits for pending writes. Invalid or newer-schema configurations are preserved and not overwritten. Paths are absolute; moving a music collection requires reimporting or updating paths. Version 0.2's schema is not readable by 0.1; use the pre-upgrade backup if reverting.

Build with a 64-bit **Java 25 JDK**:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

The wrapper provisions Gradle/NeoForge dependencies, with the Foojay resolver available for the Java 21 asset tool. Minecraft and the mod use Java 25. Build outputs are under `build/libs`. Other Minecraft versions need a separate port. See `VALIDATION.md` for the checks performed on this release and `THIRD_PARTY.md` for bundled decoder details.
