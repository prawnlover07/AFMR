# AFMR: your words

Edit the paragraph between **Text to edit:** and the next entry. Each paragraph
starts with the current in-game wording. Rewrite entries in any order and save
this file; unfinished entries keep their existing wording.

Keep the `afmr.copy...` ID and the quoted metadata lines intact. Reference behavior
records what the control currently does; it is background for your writing, not
required phrasing. Only your paragraph is packaged into the mod. Buttons, option
names, on/off states, counters, errors, diagnostics and confirmations stay in code.
Hover text that merely repeats a clipped label, song metadata or live diagnostic
output remains utility text as well. Minecraft-owned tooltips are unchanged.

Write plain text: quotes, apostrophes, Unicode and percent signs need no escaping.
Wrapping a paragraph across lines adds spaces. Blank lines add paragraph breaks.
Markdown styling is not rendered inside Minecraft. Tooltips wrap automatically;
on-screen guidance retains its existing single-line layout, so keep it concise.
Keep any `%1$s` / `%2$s` placeholders exactly as shown when the paragraph has text;
their meanings appear above it. You can move them within the sentence. Clearing
the entire paragraph removes its hover explanation or guidance line. Utility
labels and overflow-label tooltips remain. Keep the entry and its metadata so
you can add wording later. Target scopes describe UI capabilities across all supported
loaders; the author paragraphs are unchanged when a new loader uses the same controls.

## Build and preview

Run `.\gradlew.bat build` from the repository root for NeoForge, or from
the matching `ports/fabric-*` directory for Fabric. All builds read this same file automatically;
you do not need to edit Java or generated JSON. Run `.\gradlew.bat validateUiCopy`
in either directory to check wording without compiling the mod. A build reports
missing IDs, unused entries and broken placeholders in nonempty passages.

To see changes, install the rebuilt JAR in a test profile and restart Minecraft.
Review wrapping at your normal GUI scale. If you only want to write, save this
file and ask Codex to build and preview it later. The generated language files
under `build/generated/uiCopy` are disposable and should never be edited.

The Fabric source ZIP includes this file and its shared build support, so the
port can still be built independently after extraction.

## Jump to a menu

- [Playlists and library workspace](#playlists-and-library-workspace)
- [Condition selection](#playlists-condition-selection)
- [Timing and playback](#timing-and-playback)
- [Combat settings](#timing-combat-settings)
- [Display settings](#display-settings)
- [Playlist sharing](#import-and-export-playlist-setups)
- [Full configuration](#import-and-export-full-configuration)
- [Profiles](#profiles)
- [Queue and history](#queue-and-history)
- [Song columns](#library-song-columns)

## Conditions: combat

### Combat state

> ID: afmr.copy.combatcondition.combat_state
> Targets: All targets
> Where: Conditions: combat — Hover text
> Placeholders: None
> Reference behavior: Any disables this condition. Qualifying attack/damage events start combat. Mob-target retention works in singleplayer and on servers with the AFMR companion: hostile or angered combat-capable mobs targeting you sustain an existing fight. Servers without the companion use the attack/damage timeout.

**Text to edit:**

Attack/damage starts combat. In singleplayer or on an AFMR companion server, mob targeting sustains it. Other servers use the attack/damage timer.

### Players only

> ID: afmr.copy.combatcondition.players_only
> Targets: All targets
> Where: Conditions: combat — Hover text
> Placeholders: None
> Reference behavior: Use a separate PvP timer, refreshed only by attacking or being attacked by another player, including projectiles. Mob attacks never extend it. With Peaceful selected, match when not in PvP.

**Text to edit:**

Use PvP instead of mob combat.

## Conditions: cover

### Minimum cover depth

> ID: afmr.copy.covercondition.minimum_cover_depth
> Targets: All targets
> Where: Conditions: cover — Hover text
> Placeholders: None
> Reference behavior: Required depth below solid cover when Covered is selected. Range: 0-4096 blocks; 0 requires cover without a depth limit. Water and leaves alone do not count.

**Text to edit:**

Play when below solid cover. Range: 0-4096 blocks; 0 means any cover w/ no depth limit.

## Conditions: gamemode

### Gamemode filter

> ID: afmr.copy.gamemodecondition.gamemode_filter
> Targets: All targets
> Where: Conditions: gamemode — Hover text
> Placeholders: None
> Reference behavior: Match any selected gamemode. An enabled filter with no selection cannot match.

**Text to edit:**



## Conditions: health

### Above or below

> ID: afmr.copy.healthcondition.above_or_below
> Targets: All targets
> Where: Conditions: health — Hover text
> Placeholders: None
> Reference behavior: Strictly above or below the threshold. Exactly equal does not match.

**Text to edit:**

Non-inclusive, health must be above or below.

### Heart threshold

> ID: afmr.copy.healthcondition.heart_threshold
> Targets: All targets
> Where: Conditions: health — Hover text
> Placeholders: None
> Reference behavior: Red hearts only; 1 heart = 2 health points. Decimals are allowed. Golden absorption hearts are excluded. Use a finite, non-negative number.

**Text to edit:**

Red hearts only! 1 heart is equal to 2 health points.

## Conditions: held items

### Hand selection

> ID: afmr.copy.helditemcondition.hand_selection
> Targets: All targets
> Where: Conditions: held items — Hover text
> Placeholders: None
> Reference behavior: Choose main hand, off-hand or either hand. Any selected item may match; empty hands do not. Item type ignores durability, names and enchantments.

**Text to edit:**

Ignores durability, item name & enchantments

## Conditions: nested groups

### Add group

> ID: afmr.copy.conditiongroups.add_group
> Targets: All targets
> Where: Conditions: nested groups — Hover text
> Placeholders: None
> Reference behavior: Add an All/Any/Not branch. Limits: four nested levels, eight children per group, 64 total condition nodes per playlist.

**Text to edit:**

Add an All/Any/Not branch. Limits: four nested levels, eight children per group, 64
total condition nodes per playlist.

## Conditions: structures

### Add current structures

> ID: afmr.copy.structurecondition.add_current_structures
> Targets: All targets
> Where: Conditions: structures — Hover text
> Placeholders: None
> Reference behavior: Singleplayer or an AFMR companion server. Select the loaded generated structures you are currently inside.

**Text to edit:**

Requires singleplayer or the AFMR server companion.

### Structure search

> ID: afmr.copy.structurecondition.structure_search
> Targets: All targets
> Where: Conditions: structures — Hover text
> Placeholders: None
> Reference behavior: Singleplayer or an AFMR companion server. Search the current world's structures. IDs can be entered manually for later use.

**Text to edit:**

Requires singleplayer or the AFMR server companion. Use structure name or ID

### Structure radius

> ID: afmr.copy.structurecondition.structure_radius
> Targets: All targets
> Where: Conditions: structures — Hover text
> Placeholders: None
> Reference behavior: Singleplayer or an AFMR companion server. Extra spherical distance outside the structure's full bounding box, including vertical distance. 0 = inside only; maximum 128 blocks.

**Text to edit:**

Requires singleplayer or the AFMR server companion. Play when near a structure. 0 = within bounds, max 128 blocks.

## Conditions: time and numeric ranges

### Time range

> ID: afmr.copy.rangecondition.time_range
> Targets: All targets
> Where: Conditions: time and numeric ranges — Hover text
> Placeholders: None
> Reference behavior: Inclusive clock ticks, 0-23999; 0 is dawn. Start greater than end wraps across the day boundary.

**Text to edit:**

0-23999, 0 is dawn. If start time > end time, wrap around day boundary.

### Elevation range

> ID: afmr.copy.rangecondition.elevation_range
> Targets: All targets
> Where: Conditions: time and numeric ranges — Hover text
> Placeholders: None
> Reference behavior: Inclusive player block Y range. Solid cover is a separate condition.

**Text to edit:**



### Temperature range

> ID: afmr.copy.rangecondition.temperature_range
> Targets: All targets
> Where: Conditions: time and numeric ranges — Hover text
> Placeholders: None
> Reference behavior: Biome base temperature, using Minecraft values rather than degrees. Inclusive minimum and maximum.

**Text to edit:**

Uses Minecraft biome temp values.

### Light range

> ID: afmr.copy.rangecondition.light_range
> Targets: All targets
> Where: Conditions: time and numeric ranges — Hover text
> Placeholders: None
> Reference behavior: Inclusive local light level, 0-15. Unaffected by the video brightness setting.

**Text to edit:**

Unaffected by video brightness, 0-15.

### Fit songs to time window

> ID: afmr.copy.rangecondition.fit_songs_to_time_window
> Targets: All targets
> Where: Conditions: time and numeric ranges — Hover text
> Placeholders: None
> Reference behavior: Only start automatic songs that can finish before the matching time window ends, at normal 20-tick/second time flow. Unknown lengths are checked in the background. If none fit, stay silent. Manual overrides and previews bypass this option.

**Text to edit:**

Only play automatic songs that fit matching time window. If none apply, stay silent.

## Conditions: weather

### Any Weather

> ID: afmr.copy.weathercondition.any_weather
> Targets: All targets
> Where: Conditions: weather — Hover text
> Placeholders: None
> Reference behavior: Rain, snow or thunderstorms in this dimension. Clear weather does not match.

**Text to edit:**

Rain, snow or thunderstorms

### Rain

> ID: afmr.copy.weathercondition.rain
> Targets: All targets
> Where: Conditions: weather — Hover text
> Placeholders: None
> Reference behavior: Raining in a rain-capable biome at your height, including thunderstorms. Roofs do not change this.

**Text to edit:**

Activate while raining in biome at current height.

### Snow

> ID: afmr.copy.weathercondition.snow
> Targets: All targets
> Where: Conditions: weather — Hover text
> Placeholders: None
> Reference behavior: Snowing according to the biome and your height, including thunderstorms. Roofs do not change this.

**Text to edit:**

Activate while snowing in biome at current height.

### Thunderstorm

> ID: afmr.copy.weathercondition.thunderstorm
> Targets: All targets
> Where: Conditions: weather — Hover text
> Placeholders: None
> Reference behavior: A thunderstorm in this dimension, including dry or snowy biomes.

**Text to edit:**

Activate while storming in this dimension.

### Clear

> ID: afmr.copy.weathercondition.clear
> Targets: All targets
> Where: Conditions: weather — Hover text
> Placeholders: None
> Reference behavior: Neither rain/snow weather nor a thunderstorm in this dimension.

**Text to edit:**

Activate during clear weather.

## Display settings

### Scroll long text

> ID: afmr.copy.nowplayingsettings.scroll_long_text
> Targets: All targets
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: Scroll clipped text inside the chosen width, pausing at both ends. Off uses an ellipsis. Increase popup duration for very long titles.

**Text to edit:**

Keep text bound within widget by scrolling rather than cutting off. Off uses ellipsis.

### Information field

> ID: afmr.copy.nowplayingsettings.information_field
> Targets: All targets
> Where: Display settings — Hover text
> Placeholders: %1$s = field label; %2$s = on/off
> Reference behavior: %1$s: %2$s. Empty metadata is omitted.

**Text to edit:**

%1$s: %2$s. Omitted if empty.

### Menu opacity

> ID: afmr.copy.nowplayingsettings.menu_opacity
> Targets: All targets
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: AFMR menu backgrounds, 0-100%. Changes preview immediately. Text, artwork and selection indicators stay opaque.

**Text to edit:**



### Menu blur

> ID: afmr.copy.nowplayingsettings.menu_blur
> Targets: Minecraft 26.3 builds
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: Blur the custom image or game behind AFMR menus. Strength follows Minecraft's Menu Background Blur setting.

**Text to edit:**

Blur game/image behind windows. Controlled by Minecraft's Menu Background Blur setting.

### Preview widget

> ID: afmr.copy.nowplayingsettings.preview_widget
> Targets: Minecraft 26.3 builds
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: Preview the current song or sample metadata without starting music. Available for Hybrid and AFMR styles.

**Text to edit:**



### Widget corner

> ID: afmr.copy.nowplayingsettings.widget_corner
> Targets: Minecraft 26.3 builds
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: Choose any screen corner. Default uses top left for Minecraft/Hybrid and bottom left for AFMR. Bottom placement keeps clear of the hotbar.

**Text to edit:**


### Widget style

> ID: afmr.copy.nowplayingsettings.widget_style
> Targets: Minecraft 26.3 builds
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: Hybrid combines Minecraft's toast frame with AFMR artwork, duration, size and information options.

**Text to edit:**

Hybrid combines Minecraft toast style with AFMR info.

### Widget duration

> ID: afmr.copy.nowplayingsettings.widget_duration
> Targets: Minecraft 26.3 builds
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: Visible time, 1-60 seconds. Applies to Hybrid and AFMR; Minecraft toast uses its own option.

**Text to edit:**

1-60sec. Applies to Hybrid and AFMR styles.

### Widget scale

> ID: afmr.copy.nowplayingsettings.widget_scale
> Targets: Minecraft 26.3 builds
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: Widget size including text and artwork, 50-200%. Applies to Hybrid and AFMR.

**Text to edit:**

50-200%. Applies to Hybrid and AFMR styles.

### Widget width

> ID: afmr.copy.nowplayingsettings.widget_width
> Targets: Minecraft 26.3 builds
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: Width before scaling, 140-600 GUI pixels. Applies to Hybrid and AFMR.

**Text to edit:**

140-600 GUI pixels. Applies to Hybrid and AFMR styles.

### Text layout

> ID: afmr.copy.nowplayingsettings.text_layout
> Targets: All targets
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: Single line keeps each field on one row. Word wrap moves whole words to additional rows within the chosen width. Overwide words scroll if enabled, otherwise use an ellipsis.

**Text to edit:**

Word wrap moves words to additional rows within chosen width. Overwide words scroll if enabled.

### Card background color

> ID: afmr.copy.nowplayingsettings.card_background_color
> Targets: Minecraft 26.3 builds
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: AFMR card background color: six RGB hex digits, with optional #. Hybrid keeps Minecraft's frame.

**Text to edit:**



### Card opacity

> ID: afmr.copy.nowplayingsettings.card_opacity
> Targets: All targets
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: AFMR card background: 0% transparent, 100% solid. Text and artwork keep their own opacity.

**Text to edit:**



### Preview widget

> ID: afmr.copy.nowplayingsettings.preview_widget.card
> Targets: Pre-26.3 builds
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: Preview the current song or sample metadata without starting music.

**Text to edit:**



### Widget corner

> ID: afmr.copy.nowplayingsettings.widget_corner.card
> Targets: Pre-26.3 builds
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: Choose any screen corner. Default uses bottom left. Bottom placement keeps clear of the hotbar.

**Text to edit:**



### Widget duration

> ID: afmr.copy.nowplayingsettings.widget_duration.card
> Targets: Pre-26.3 builds
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: Visible time, 1-60 seconds. Applies to the AFMR card.

**Text to edit:**



### Widget scale

> ID: afmr.copy.nowplayingsettings.widget_scale.card
> Targets: Pre-26.3 builds
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: Widget size including text and artwork, 50-200%. Applies to the AFMR card.

**Text to edit:**


### Widget width

> ID: afmr.copy.nowplayingsettings.widget_width.card
> Targets: Pre-26.3 builds
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: Width before scaling, 140-600 GUI pixels. Applies to the AFMR card.

**Text to edit:**

140-600 GUI pixels.

### Card background color

> ID: afmr.copy.nowplayingsettings.card_background_color.card
> Targets: Pre-26.3 builds
> Where: Display settings — Hover text
> Placeholders: None
> Reference behavior: AFMR card background color: six RGB hex digits, with optional #.

**Text to edit:**



## Display: color picker

### Hue

> ID: afmr.copy.colorpickerarea.hue
> Targets: All targets
> Where: Display: color picker — Hover text
> Placeholders: None
> Reference behavior: Drag to choose a hue. Arrow keys adjust it when focused.

**Text to edit:**



### Saturation and brightness

> ID: afmr.copy.colorpickerarea.saturation_and_brightness
> Targets: All targets
> Where: Display: color picker — Hover text
> Placeholders: None
> Reference behavior: Drag to choose saturation and brightness. Left/Right changes saturation; Up/Down changes brightness.

**Text to edit:**



## Display: color swatches

### Open color picker

> ID: afmr.copy.colorswatch.open_color_picker
> Targets: All targets
> Where: Display: color swatches — Hover text
> Placeholders: %1$s = swatch name or hex color
> Reference behavior: %1$s. Open the color picker.

**Text to edit:**



## Display: custom menu or widget background

### Choose image

> ID: afmr.copy.menubackground.choose_image
> Targets: All targets
> Where: Display: custom background — Hover text
> Placeholders: None
> Reference behavior: PNG, JPEG, static WebP or GIF. GIFs animate and loop. Your original stays untouched. Maximum 20 MiB / 64 megapixels; 512 GIF frames. Animations resize to fit memory.

**Text to edit:**

PNG/JPEG/WEBP (static)/GIF. Max 20MiB & 512 GIF frames.

### Image sizing

> ID: afmr.copy.menubackground.image_sizing
> Targets: All targets
> Where: Display: custom background — Hover text
> Placeholders: None
> Reference behavior: Fill: centered crop. Fit: show the whole image. Stretch: fill the target screen/card without cropping. Widget transparent pixels and Fit gaps show the game.

**Text to edit:**



### Image opacity

> ID: afmr.copy.menubackground.image_opacity
> Targets: All targets
> Where: Display: custom background — Hover text
> Placeholders: None
> Reference behavior: Image transparency for the selected menu or widget image. Menu panel opacity and solid widget background opacity are separate. Widget text/artwork remain visible; the popup fade multiplies image opacity.

**Text to edit:**



### Reset background

> ID: afmr.copy.menubackground.reset_background
> Targets: All targets
> Where: Display: custom background — Hover text
> Placeholders: None
> Reference behavior: Turn off the background and restore Fill at 100% opacity. Keep the selected image for later.

**Text to edit:**



## Display: menu colors

### Hex color field

> ID: afmr.copy.menucolors.hex_color_field
> Targets: All targets
> Where: Display: Themes → Edit colors — Hover text
> Placeholders: None
> Reference behavior: Six-digit RGB color; # is optional. Valid edits preview immediately. Shades follow this color. Text and highlights stay opaque.

**Text to edit:**



### Reset menu colors

> ID: afmr.copy.menucolors.reset_menu_colors
> Targets: All targets
> Where: Display: Themes → Edit colors — Hover text
> Placeholders: None
> Reference behavior: Restore AFMR's original menu colors. Keeps background opacity, blur, image and widget settings.

**Text to edit:**

Restore original menu colors only

## Display: widget preview

### Guidance

> ID: afmr.copy.nowplayingpreview.guidance
> Targets: All targets
> Where: Display: widget preview — On-screen guidance (existing single-line layout)
> Placeholders: None
> Reference behavior: Current song, or sample metadata when idle. Preview fits this screen.

**Text to edit:**



## Display: widget scrolling

### Widget scrollbar

> ID: afmr.copy.settingsscrollbar.widget_scrollbar
> Targets: All targets
> Where: Display: widget scrolling — Hover text
> Placeholders: None
> Reference behavior: Scroll widget settings. Use the mouse wheel, drag, or Page Up / Page Down.

**Text to edit:**



## Import and export: full configuration

### Export full backup

> ID: afmr.copy.configbackup.export_full_backup
> Targets: All targets
> Where: Import and export: full configuration — Hover text
> Placeholders: None
> Reference behavior: Export all settings, profiles, playlists, song assignments, library paths, AFMR keys, history and the custom background. Music files stay separate.

**Text to edit:**

Export settings, profiles, playlists, song assignments, library paths, AFMR keys, history and background.

### Import full backup

> ID: afmr.copy.configbackup.import_full_backup
> Targets: All targets
> Where: Import and export: full configuration — Hover text
> Placeholders: None
> Reference behavior: Review a .afmrb backup before replacing this install's AFMR configuration. Apply on restart keeps a recovery copy. Music paths and world/server links are preserved; use Library repair for moved folders.

**Text to edit:**

Use .afmrb. Apply on restart keeps a recovery copy. Use Library Repair to find missing/moved folders.

### Replace on restart

> ID: afmr.copy.configbackup.replace_on_restart
> Targets: All targets
> Where: Import and export: full configuration — Hover text
> Placeholders: None
> Reference behavior: Replace all AFMR settings and library data after restarting Minecraft. The current session stays as it is; the previous files are kept under config/afmr/restore-previous.

**Text to edit:**

Replace all settings and library data after restarting Minecraft. Previous files are kept under config/afmr/restore-previous.

## Import and export: playlist setups

### Import setup

> ID: afmr.copy.playlistsharing.import_setup
> Targets: All targets
> Where: Import and export: playlist setups — Hover text
> Placeholders: None
> Reference behavior: Read one shared setup and review its playlists before adding them to a profile. Music files and global settings stay separate.

**Text to edit:**

Read shared setup and view playlists before adding to profile.

### Export setup

> ID: afmr.copy.playlistsharing.export_setup
> Targets: All targets
> Where: Import and export: playlist setups — Hover text
> Placeholders: None
> Reference behavior: Save selected playlists in priority order with every condition and playback option. An optional song list includes relative folder paths and on/off states. Audio files and original world/server links stay separate.

**Text to edit:**

Export selected playlists, their priorities and conditions, as well as song assignments if desired.

### Guidance

> ID: afmr.copy.playlistsharing.guidance
> Targets: All targets
> Where: Import and export: playlist setups — On-screen guidance (existing single-line layout)
> Placeholders: None
> Reference behavior: Includes a song list, without audio files or absolute music paths.

**Text to edit:**

Include song list

### Settings only

> ID: afmr.copy.playlistsharing.settings_only
> Targets: All targets
> Where: Import and export: playlist setups — On-screen guidance (existing single-line layout)
> Placeholders: None
> Reference behavior: Settings only. Locations bind to the recipient's world/server.

**Text to edit:**

Do not include song list

## Import: locate music folders

### Missing or renamed songs can also be repaired after import

> ID: afmr.copy.presetsongfolders.missing_or_renamed_songs_can_also_be_repaired_after_import
> Targets: All targets
> Where: Import: locate music folders — On-screen guidance (existing single-line layout)
> Placeholders: None
> Reference behavior: Missing or renamed songs can also be repaired after import.

**Text to edit:**

Can also repair after import.

### Choose a folder containing the saved relative paths

> ID: afmr.copy.presetsongfolders.choose_a_folder_containing_the_saved_relative_paths
> Targets: All targets
> Where: Import: locate music folders — On-screen guidance (existing single-line layout)
> Placeholders: None
> Reference behavior: Choose a folder containing the saved relative paths.

**Text to edit:**

Choose folder containing saved relative paths.

### Additional guidance

> ID: afmr.copy.presetsongfolders.additional_guidance
> Targets: All targets
> Where: Import: locate music folders — On-screen guidance (existing single-line layout)
> Placeholders: None
> Reference behavior: Not located: choose the folder containing the saved relative paths

**Text to edit:**

Not located: Choose folder containing saved relative paths.

## Import: music destination

### Import to library only

> ID: afmr.copy.playlistimport.import_to_library_only
> Targets: All targets
> Where: Import: music destination — Hover text
> Placeholders: None
> Reference behavior: Import files or folders without assigning songs to a playlist. Foobar playlist import needs a playlist destination.

**Text to edit:**

Import files or folders without assigning to a playlist. Foobar import requires playlist assignment.

## Import: music report

### Guidance

> ID: afmr.copy.playlistimportreport.guidance
> Targets: All targets
> Where: Import: music report — On-screen guidance (existing single-line layout)
> Placeholders: None
> Reference behavior: Details limited to the first 100 messages; skipped count includes all entries.

**Text to edit:**

Details limited to the first 100 messages; skipped count includes all entries.

### Hover a detail to read its full message

> ID: afmr.copy.playlistimportreport.hover_a_detail_to_read_its_full_message
> Targets: All targets
> Where: Import: music report — On-screen guidance (existing single-line layout)
> Placeholders: None
> Reference behavior: Hover a detail to read its full message.

**Text to edit:**

Hover over details to read full message.

## Inspector

### Copy diagnostic summary

> ID: afmr.copy.playbackinspector.copy_diagnostic_summary
> Targets: All targets
> Where: Inspector — Hover text
> Placeholders: None
> Reference behavior: Copy versions, playback/condition state, volume, scan status and a heap snapshot. Uses playlist priority numbers; omits names, song metadata, paths, world/server identifiers, X/Z coordinates, saved locations and raw errors. Clipboard only; nothing is sent.

**Text to edit:**

Copy versions, playback/condition state, volume, scan status and heap snapshot to clipboard. Uses priority numbers & omits sensitive info.

### Join a world to inspect live playback

> ID: afmr.copy.playbackinspector.join_a_world_to_inspect_live_playback
> Targets: All targets
> Where: Inspector — On-screen guidance (existing single-line layout)
> Placeholders: None
> Reference behavior: Join a world to inspect live playback

**Text to edit:**

Join a world to inspect live playback

## Keys: override assignments

### Override slot assignment

> ID: afmr.copy.overridebindings.override_slot_assignment
> Targets: All targets
> Where: Keys: override assignments — Hover text
> Placeholders: None
> Reference behavior: Choose the target playlist and override mode. Set the physical key separately in Minecraft key binds.

**Text to edit:**

Choose target playlist and override mode. Keys can be bound in Minecraft keybind settings.

## Library: linked folder

### Unlink folder

> ID: afmr.copy.folder.unlink_folder
> Targets: All targets
> Where: Library: linked folder — Hover text
> Placeholders: None
> Reference behavior: Stop future linked-folder scans in every profile. Keeps imported songs, playlist assignments and files.

**Text to edit:**

Stop future linked-folder scans in every profile.

## Library: missing sources

### Run Check again or Rescan all to find missing folders and songs

> ID: afmr.copy.libraryrepair.run_check_again_or_rescan_all_to_find_missing_folders_and_songs
> Targets: All targets
> Where: Library: missing sources — On-screen guidance (existing single-line layout)
> Placeholders: None
> Reference behavior: Run Check again or Rescan all to find missing folders and songs.

**Text to edit:**

Run Check again or Rescan all to find missing folders and songs.

## Library: repair preview

### Guidance

> ID: afmr.copy.repairpreview.guidance
> Targets: All targets
> Where: Library: repair preview — On-screen guidance (existing single-line layout)
> Placeholders: None
> Reference behavior: Only AFMR references change. Playlists stay assigned; no music files are moved.

**Text to edit:**



### Details

> ID: afmr.copy.repairpreview.details
> Targets: All targets
> Where: Library: repair preview — On-screen guidance (existing single-line layout)
> Placeholders: None
> Reference behavior: References only. Duplicate assignment conflicts keep songs off; music files stay unchanged.

**Text to edit:**



## Library: scan messages

### Guidance

> ID: afmr.copy.scanmessages.guidance
> Targets: All targets
> Where: Library: scan messages — On-screen guidance (existing single-line layout)
> Placeholders: None
> Reference behavior: Up to 100 scan details. Missing sources are listed separately without this limit.

**Text to edit:**

Up to 100 scan details.

## Library: song columns

### RG_TRACK_GAIN

> ID: afmr.copy.songcolumns.rg_track_gain
> Targets: All targets
> Where: Library: song columns — Hover text
> Placeholders: None
> Reference behavior: ReplayGain track gain tag, before peak protection. Playback prefers track tags over AFMR measurements.

**Text to edit:**

Playback prefers ReplayGain track tags over AFMR measurements.

### RG_TRACK_PEAK

> ID: afmr.copy.songcolumns.rg_track_peak
> Targets: All targets
> Where: Library: song columns — Hover text
> Placeholders: None
> Reference behavior: ReplayGain track peak tag. 1.0 is full scale; playback uses this value for peak protection.

**Text to edit:**

ReplayGain track peak tag. 1.0 is full scale; playback uses this value for peak
protection.

### RG_ALBUM_GAIN / RG_ALBUM_PEAK

> ID: afmr.copy.songcolumns.rg_album_gain_rg_album_peak
> Targets: All targets
> Where: Library: song columns — Hover text
> Placeholders: None
> Reference behavior: ReplayGain album tag. Display only; AFMR playback uses track gain.

**Text to edit:**

ReplayGain album tag. Display only; AFMR playback uses track gain.

### AFMR_GAIN

> ID: afmr.copy.songcolumns.afmr_gain
> Targets: All targets
> Where: Library: song columns — Hover text
> Placeholders: None
> Reference behavior: Cached AFMR loudness measurement, before peak protection. Existing track ReplayGain tags take precedence during playback.

**Text to edit:**

Playback prefers ReplayGain track tags over AFMR measurements.

### TITLE

> ID: afmr.copy.songcolumns.title
> Targets: All targets
> Where: Library: song columns — Hover text
> Placeholders: None
> Reference behavior: Title and the On switch stay visible in the column selection.

**Text to edit:**



### Other columns

> ID: afmr.copy.songcolumns.other_columns
> Targets: All targets
> Where: Library: song columns — Hover text
> Placeholders: %1$s = column label in lower case
> Reference behavior: Show %1$s in the song list. Missing tags leave an empty cell.

**Text to edit:**



## Library: sources

### Rescan on launch

> ID: afmr.copy.sources.rescan_on_launch
> Targets: All targets
> Where: Library: sources — Hover text
> Placeholders: None
> Reference behavior: Scan linked folders at startup for new songs and refreshed metadata.

**Text to edit:**

Scan linked folders at startup for new songs and metadata.

## Playlist overrides

### Play one song

> ID: afmr.copy.overrides.play_one_song
> Targets: All targets
> Where: Playlist overrides — Hover text
> Placeholders: None
> Reference behavior: Override conditions for one enabled song, then return to automatic selection.

**Text to edit:**

Override conditions for one enabled song, then return to automatic selection.

### Until released

> ID: afmr.copy.overrides.until_released
> Targets: All targets
> Where: Playlist overrides — Hover text
> Placeholders: None
> Reference behavior: Keep this playlist active until released. The bound key toggles the override; it does not need to stay held.

**Text to edit:**

Keep this playlist active until released. The bound key toggles the override.

## Playlists and library workspace

### Duplicate playlist

> ID: afmr.copy.library.clone_playlist
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Copy this playlist's conditions, assignments and playback switches independently. Music files are shared.

**Text to edit:**

Copy this playlist's conditions, assignments and playback switches independently. Music
files are shared.

### Import into library

> ID: afmr.copy.library.import_into_library
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Import music into the library without assigning it to a playlist.

**Text to edit:**

Import music into the library without assigning it to a playlist.

### Add selected or folder

> ID: afmr.copy.library.add_selected_or_folder
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Assign selected songs to this playlist. A selected folder includes its subfolders.

**Text to edit:**

Assign selected songs to this playlist. A selected folder includes its subfolders.

### Play one song

> ID: afmr.copy.library.play_one_song
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Override conditions for one enabled song, then return to automatic selection. Bypasses the initial silence and context buffer.

**Text to edit:**

Override conditions for one enabled song, then return to automatic selection. Bypasses
the initial silence and context buffer.

### Until released

> ID: afmr.copy.library.until_released
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Keep this playlist active regardless of conditions until you return to Automatic. No need to hold a key.

**Text to edit:**

Keep this playlist active regardless of conditions until you return to Automatic.

### Song columns

> ID: afmr.copy.library.song_columns
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Choose song information columns for all playlists. Extra columns scroll horizontally; drag the bar or use Left/Right with the song list focused. Rescan existing music to read new tags.

**Text to edit:**

Choose song information columns for all playlists. Extra columns scroll horizontally;
drag the bar or use Left/Right with the song list focused. Rescan existing music to read
new tags.

### Preview

> ID: afmr.copy.library.preview
> Targets: All targets
> Where: Playlist song right-click menu — Hover text
> Placeholders: None
> Reference behavior: Play the last selected song, including disabled songs. Cancels any playlist override.

**Text to edit:**

Play the last selected song, including disabled songs. Cancels any playlist override.

### Queue

> ID: afmr.copy.library.queue
> Targets: All targets
> Where: Playlist song right-click menu — Hover text
> Placeholders: None
> Reference behavior: Append selected enabled songs in table order without interrupting playback. Normal silence applies. Manage requests in Queue / history.

**Text to edit:**

Append selected enabled songs without interrupting playback. Normal silence applies. Manage requests in Queue / history.

### Disable songs

> ID: afmr.copy.library.disable_songs
> Targets: All targets
> Where: Playlist song right-click menu — Hover text
> Placeholders: None
> Reference behavior: Exclude selected songs from this playlist, including manual overrides. Preview remains available.

**Text to edit:**

Exclude selected songs from this playlist, including manual overrides. Preview remains
available.

### Remove assignments

> ID: afmr.copy.library.remove_assignments
> Targets: All targets
> Where: Playlist song right-click menu — Hover text
> Placeholders: None
> Reference behavior: Remove selected assignments from this playlist. Keeps library entries and music files; Minecraft songs may remain included automatically.

**Text to edit:**

Remove selected assignments from this playlist. Keeps library entries and music files;
Minecraft songs may remain included automatically.

### Normalize playlist

> ID: afmr.copy.library.normalize_playlist
> Targets: All targets
> Where: Playlist actions / right-click menu — Balance song volumes hover text
> Placeholders: None
> Reference behavior: Measure loudness for local songs in this playlist. Results are cached separately; audio files are unchanged.

**Text to edit:**

Measure loudness for local songs in this playlist. Results are cached in AFMR only.

### Pause and resume

> ID: afmr.copy.library.pause_and_resume
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Pause or resume AFMR playback and its timers.

**Text to edit:**

Pause or resume AFMR playback and its timers.

### Next

> ID: afmr.copy.library.next
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Play the next queued song, or the effective playlist if no queue is ready. Skip silence and fade-out; fade-in still applies.

**Text to edit:**

Play the next queued song, or effective playlist if no queue is ready. Skip silence
and fade-out; fade-in still applies.

### Skip & wait

> ID: afmr.copy.library.skip_wait
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Stop the current song and wait the normal silence. Disabled when no song is playing; Play now belongs to the primary transport button.

**Text to edit:**

Stop and wait the normal silence.

### Return to Automatic

> ID: afmr.copy.library.return_to_automatic
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Release the playlist override. Pending queued songs remain; clear them in Queue / history to return fully to automatic music.

**Text to edit:**

Release the playlist override. Pending queued songs remain, clear them in Queue / history to fully return to automatic music.

### Automatic switch

> ID: afmr.copy.library.automatic_switch
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Allow automatic selection of this playlist. Off still permits overrides and previews.

**Text to edit:**

Allow automatic selection of this playlist. Off still permits overrides and previews.

### Manual only

> ID: afmr.copy.library.manual_only
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Reserve this playlist for overrides and previews, even when Automatic is on.

**Text to edit:**

Reserve this playlist for overrides and previews, even when Automatic is on.

### Match all or any

> ID: afmr.copy.library.match_all_or_any
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: All requires every enabled filter and group; Any requires at least one. Not inverts their combined result.

**Text to edit:**

All requires every enabled filter and group; Any requires at least one. Not inverts
their combined result.

### Playlist volume

> ID: afmr.copy.library.playlist_volume
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Balance this playlist from 0-100%. Combines with Minecraft volume and normalization; retained songs keep their source playlist's volume.

**Text to edit:**


### Playlist silence

> ID: afmr.copy.library.playlist_silence
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Set a silence range for this playlist, or use Timing's global range. Elapsed silence counts toward a new playlist's range.

**Text to edit:**

Set a custom range of silence for this playlist. Elapsed silence counts towards new playlist range.

### Minecraft music

> ID: afmr.copy.library.minecraft_music
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Include the Minecraft music applicable to the current situation. Explicitly disabled songs stay excluded.

**Text to edit:**

Include the Minecraft music applicable to the current situation. Explicitly disabled
songs stay excluded.

### Finish song on entry

> ID: afmr.copy.library.finish_song_on_entry
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: When switching into this playlist, let the current song finish. Manual overrides, skips and combat death rules take precedence.

**Text to edit:**

When switching into this playlist, let the current song finish. Manual overrides, skips
and combat death rules take precedence.

### Finish song on exit

> ID: afmr.copy.library.finish_song_on_exit
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: Let a song started by this playlist finish after leaving it. Independent of entry protection; manual controls and combat death rules take precedence.

**Text to edit:**

Let a song started by this playlist finish after leaving it. Independent of entry
protection; manual controls and combat death rules take precedence.

### Play on trigger

> ID: afmr.copy.library.play_on_trigger
> Targets: All targets
> Where: Playlists and library workspace — Hover text
> Placeholders: None
> Reference behavior: On automatic playlist entry, skip the context buffer and silence. Current music fades normally; finish-song controls still apply. Later songs use normal timing.

**Text to edit:**

On automatic playlist entry, skip the context buffer and silence. Current music fades
normally; finish-song controls still apply. Later songs use normal timing.

## Playlists: Set playback area here

### Set playback area here

> ID: afmr.copy.autoplayhere.autoplay_here
> Targets: All targets
> Where: Playlists: Set playback area here — Hover text
> Placeholders: None
> Reference behavior: Hold for one second to replace this playlist's conditions with a 64-block zone here and enable automatic selection. Songs and playback switches are kept.

**Text to edit:**

Quick option: hold for one second to replace this playlist's conditions with a 64-block radius here & enable automatic selection.

## Playlists: condition selection

### Match all or any

> ID: afmr.copy.playlistconditions.match_all_or_any
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: All: every enabled filter must match. Any: at least one enabled filter must match. With no enabled filters, the playlist matches everywhere.

**Text to edit:**

All: every enabled filter must match. Any: at least one enabled filter must match. With
no enabled filters, the playlist matches everywhere.

### Not

> ID: afmr.copy.playlistconditions.not
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Invert the result of this entire condition group. Unavailable structure data remains unavailable.

**Text to edit:**

Invert the result of this entire condition group. Unavailable structure data remains
unavailable.

### Group enabled

> ID: afmr.copy.playlistconditions.group_enabled
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Off removes this group from its parent's All/Any comparison without deleting its settings.

**Text to edit:**

Off removes this group from its parent's All/Any comparison without deleting its
settings.

### Location

> ID: afmr.copy.playlistconditions.location
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: A spherical area in one world and dimension, including vertical distance.

**Text to edit:**

A spherical area in one world and dimension, including vertical distance.

### Time

> ID: afmr.copy.playlistconditions.time
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: An inclusive time range in Minecraft clock ticks. Ranges can cross the day boundary.

**Text to edit:**

An inclusive time range in Minecraft clock ticks. Ranges can cross the day boundary.

### Dimension

> ID: afmr.copy.playlistconditions.dimension
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Match a dimension ID, independently of biome and elevation.

**Text to edit:**

Match a dimension ID, independently of biome and elevation.

### Elevation

> ID: afmr.copy.playlistconditions.elevation
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Player block Y range. Solid cover is a separate filter.

**Text to edit:**

Player block Y range. Solid cover is a separate filter.

### Cover

> ID: afmr.copy.playlistconditions.cover
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Solid cover or open sky. Water and leaves alone do not count as cover.

**Text to edit:**

Solid cover or open sky. Water and leaves alone do not count as cover.

### Biome temperature

> ID: afmr.copy.playlistconditions.biome_temperature
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Minecraft biome base temperature, rather than degrees.

**Text to edit:**

Minecraft biome base temperature.

### Light

> ID: afmr.copy.playlistconditions.light
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Local light level, 0-15. Independent of video brightness.

**Text to edit:**

Local light level, 0-15.

### Combat

> ID: afmr.copy.playlistconditions.combat
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Choose combat or peaceful, with an optional Players only filter for PvP. Uses the enabled attack/damage triggers and quiet period.

**Text to edit:**

Choose combat or peaceful, with an optional Players only filter for PvP. Uses the
enabled attack/damage triggers and quiet period.

### Water

> ID: afmr.copy.playlistconditions.water
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Underwater requires the player to be submerged, rather than just standing in water. Any disables this filter.

**Text to edit:**

Underwater requires the player to be submerged, rather than just standing in water. Any
disables this filter.

### Riding animal

> ID: afmr.copy.playlistconditions.riding_animal
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Any disables this condition. Riding matches while the player's direct vehicle is an animal, including horses, donkeys, mules, llamas, camels, pigs and striders; modded mounts inheriting Minecraft's Animal class also qualify. Not riding matches on foot or in non-animal vehicles such as boats and minecarts. Multiplayer uses the client's synchronized riding state. Nested All/Any/Not groups and normal playlist priority and transitions apply.

**Text to edit:**

### Biomes

> ID: afmr.copy.playlistconditions.biomes
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Match any selected biome. An enabled filter needs at least one selection.

**Text to edit:**

Match any selected biome. An enabled filter needs at least one selection.

### Structures

> ID: afmr.copy.playlistconditions.structures
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Requires singleplayer or the AFMR server companion. Match loaded generated structure bounds, including height and the configured radius. Servers without the companion cannot provide structure data.

**Text to edit:**

Requires singleplayer or the AFMR server companion. Match loaded generated structure bounds, including height and the
configured radius. Servers without the companion cannot provide structure data.

### Weather

> ID: afmr.copy.playlistconditions.weather
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Match weather and biome precipitation. Solid cover does not change this filter.

**Text to edit:**

Match weather and biome precipitation. Solid cover does not change this filter.

### Health

> ID: afmr.copy.playlistconditions.health
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Above or below a red-heart threshold. Golden absorption hearts are excluded; exact equality does not match.

**Text to edit:**

Above or below a red-heart threshold. Golden absorption hearts are excluded; non-inclusive.

### Held items

> ID: afmr.copy.playlistconditions.held_items
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Match a selected item in the main hand, off-hand or either hand. Item names, durability and enchantments do not change its type.

**Text to edit:**

Match a selected item in the main hand, off-hand or either hand. Ignores item names, durability and enchants.

### Gamemode

> ID: afmr.copy.playlistconditions.gamemode
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Match any selected player gamemode: Survival, Creative, Adventure or Spectator.

**Text to edit:**

Match any selected player gamemode.

### Nested groups

> ID: afmr.copy.playlistconditions.nested_groups
> Targets: All targets
> Where: Playlists: condition selection — Hover text
> Placeholders: None
> Reference behavior: Combine nested All/Any/Not groups with the filters on this page.

**Text to edit:**

Combine nested All/Any/Not groups with the filters on this page.

## Playlists: silence

### Custom silence switch

> ID: afmr.copy.playlistsilence.custom_silence_switch
> Targets: All targets
> Where: Playlist Settings: inline Playback gaps — Hover text
> Placeholders: None
> Reference behavior: Override Timing's silence range for this playlist. Off uses global timing and keeps the custom values. Changing playlists during silence counts time already waited toward the incoming range.

**Text to edit:**

Override Timing's silence range for this playlist. Off uses global timing and keeps the
custom values. Changing playlists during silence counts time already waited toward the
incoming range.

### Minimum silence

> ID: afmr.copy.playlistsilence.minimum_silence
> Targets: All targets
> Where: Playlist Settings: inline Playback gaps — Hover text
> Placeholders: None
> Reference behavior: Shortest gap, 0-86400 seconds. Use the same minimum and maximum for a fixed gap.

**Text to edit:**

Shortest gap, 0-86400 seconds. Use the same minimum and maximum for a fixed gap.

### Maximum silence

> ID: afmr.copy.playlistsilence.maximum_silence
> Targets: All targets
> Where: Playlist Settings: inline Playback gaps — Hover text
> Placeholders: None
> Reference behavior: Longest gap, at least the minimum and up to 86400 seconds. Manual play controls, Play on trigger and Play immediately on join still skip silence.

**Text to edit:**

Longest gap, at least the minimum and up to 86400 seconds. Manual play controls, Play on
trigger and Play immediately on join still skip silence.

## Playlists: volume

### Inline volume

> ID: afmr.copy.playlistvolume.live_volume_slider
> Targets: All targets
> Where: Playlist Settings: inline Playback volume — Hover text
> Placeholders: None
> Reference behavior: Changes the source playlist's playing song live, including queues and overrides. 0% is silent; 100% is normal volume. Library previews use normal volume.

**Text to edit:**

Changes the source playlist's playing song live, including queues and overrides. 0% is
silent; 100% is normal volume. Library previews use normal volume.

## Profiles

### New profile

> ID: afmr.copy.profiles.new_profile
> Targets: All targets
> Where: Profiles — Hover text
> Placeholders: None
> Reference behavior: Create a named setup with the six default playlist templates and Minecraft-like playback defaults. Shares your music library.

**Text to edit:**

Create a named setup with the six default playlist templates and Minecraft-like playback
defaults. Shares your music library.

### Copy selected profile

> ID: afmr.copy.profiles.copy_selected_profile
> Targets: All targets
> Where: Profiles — Hover text
> Placeholders: None
> Reference behavior: Copy selected playlists, conditions, volume, assignments, priorities, timing and combat settings. The name above is used for the copy.

**Text to edit:**

Copy selected playlists, conditions, volume, assignments, priorities, timing and combat
settings. The name above is used for the copy.

### Use now

> ID: afmr.copy.profiles.use_now
> Targets: All targets
> Where: Profiles — Hover text
> Placeholders: None
> Reference behavior: Stop music and clear the queue, override and pause state, then use this setup now. Does not change links; unlinked worlds use Default.

**Text to edit:**

Stop music and clear the queue, override and pause state, then use this setup now. Does
not change links; unlinked worlds use Default.

### Link current world or server

> ID: afmr.copy.profiles.link_current_world_or_server
> Targets: All targets
> Where: Profiles — Hover text
> Placeholders: None
> Reference behavior: Select this setup automatically next time you join the current world or server. Use now switches immediately.

**Text to edit:**

Select this setup automatically next time you join the current world or server. Use now
switches immediately.

### Unlink current world or server

> ID: afmr.copy.profiles.unlink_current_world_or_server
> Targets: All targets
> Where: Profiles — Hover text
> Placeholders: None
> Reference behavior: Remove the current world/server link. Next join will use Default; current playback is unchanged.

**Text to edit:**

Remove the current world/server link. Next join will use Default; current playback is
unchanged.

### Join a world or server to link it

> ID: afmr.copy.profiles.join_a_world_or_server_to_link_it
> Targets: All targets
> Where: Profiles — On-screen guidance (existing single-line layout)
> Placeholders: None
> Reference behavior: Join a world or server to link it.

**Text to edit:**

Join a world or server to link it.

## Queue and history

### History: Play now

> ID: afmr.copy.listening.history_play_now
> Targets: All targets
> Where: Queue and history — Hover text
> Placeholders: None
> Reference behavior: Replay as a manual preview, interrupting current music. Pending queued songs remain.

**Text to edit:**

Replay as a manual preview, interrupting current music. Pending queued songs remain.

### History: Queue

> ID: afmr.copy.listening.history_queue
> Targets: All targets
> Where: Queue and history — Hover text
> Placeholders: None
> Reference behavior: Append this song to the queue. Its original playlist's song switch is respected when that playlist still exists.

**Text to edit:**

Append this song to the queue. Its original playlist's song switch is respected when
that playlist still exists.

### History: Add to playlist

> ID: afmr.copy.listening.history_add_to_playlist
> Targets: All targets
> Where: Queue and history — Hover text
> Placeholders: None
> Reference behavior: Choose a playlist to add this library song to. Existing enabled/disabled assignments are preserved.

**Text to edit:**

Choose a playlist to add this library song to. Existing enabled/disabled assignments are
preserved.

### Clear queue

> ID: afmr.copy.listening.clear_queue
> Targets: All targets
> Where: Queue and history — Hover text
> Placeholders: None
> Reference behavior: Remove pending requests. The current song continues until it ends or you skip it.

**Text to edit:**

Remove pending requests. The current song continues until it ends or you skip it.

### Use queue next

> ID: afmr.copy.listening.use_queue_next
> Targets: All targets
> Where: Queue and history — Hover text
> Placeholders: None
> Reference behavior: Give the queue priority again after a new playlist override. Does not interrupt the current song or skip normal silence.

**Text to edit:**

Give the queue priority again after a new playlist override. Does not interrupt the
current song or skip normal silence.

## Timing and playback

### Play immediately on join

> ID: afmr.copy.settings.play_immediately_on_join
> Targets: All targets
> Where: Timing and playback — Hover text
> Placeholders: None
> Reference behavior: Start the first eligible playlist when joining a world or server, skipping initial silence and the context buffer. Normal fade-in and later song gaps still apply. Saved per profile.

**Text to edit:**

Start the first eligible playlist when joining a world or server, skipping initial
silence and the context buffer. Normal fade-in and later song gaps still apply. Saved
per profile.

### Normalization

> ID: afmr.copy.settings.normalization
> Targets: All targets
> Where: Timing and playback — Hover text
> Placeholders: None
> Reference behavior: Apply ReplayGain tags or cached loudness results to newly started local songs. Music files and Minecraft music are unchanged.

**Text to edit:**

Apply ReplayGain tags or cached loudness results to newly started local songs. Music
files and Minecraft music are unchanged.

### Continue playing on match

> ID: afmr.copy.settings.continue_playing_on_match
> Targets: All targets
> Where: Timing and playback — Hover text
> Placeholders: None
> Reference behavior: Let the current song finish if it is also eligible in the incoming automatic playlist. Manual overrides still switch music.

**Text to edit:**

Let the current song finish if it is also eligible in the incoming automatic playlist.
Manual overrides still switch music.

### Jukebox policy

> ID: afmr.copy.settings.jukebox_policy
> Targets: All targets
> Where: Timing and playback — Hover text
> Placeholders: None
> Reference behavior: Fade AFMR out for audible jukebox music. Resume pauses the current song; new after silence waits a full gap after the record ends; new immediately skips that gap. Uses Fade out and Fade in.

**Text to edit:**

Fade AFMR out for audible jukebox music. Resume pauses the current song; new after
silence waits a full gap after the record ends; new immediately skips that gap. Uses
Fade out and Fade in.

### Combat settings

> ID: afmr.copy.settings.combat_settings
> Targets: All targets
> Where: Timing and playback — Hover text
> Placeholders: None
> Reference behavior: Configure automatic combat detection, triggers, quiet period and death behavior.

**Text to edit:**

Configure automatic combat detection, triggers, quiet period and death behavior.

### Minecraft defaults

> ID: afmr.copy.settings.minecraft_defaults
> Targets: All targets
> Where: Timing and playback — Hover text
> Placeholders: None
> Reference behavior: Restore 600-1200 seconds of silence, with both fades and the context buffer at zero. Replaces unsaved timing values.

**Text to edit:**

Restore 600-1200 seconds of silence, with both fades and the context buffer at zero.
Replaces unsaved timing values.

### Min silence (sec)

> ID: afmr.copy.settings.min_silence_sec
> Targets: All targets
> Where: Timing and playback — Hover text
> Placeholders: None
> Reference behavior: Shortest random gap before the next scheduled song. Seconds, 0-86400.

**Text to edit:**

Shortest random gap before the next scheduled song. Seconds, 0-86400.

### Max silence (sec)

> ID: afmr.copy.settings.max_silence_sec
> Targets: All targets
> Where: Timing and playback — Hover text
> Placeholders: None
> Reference behavior: Longest random gap. Must be at least the minimum; up to 86400 seconds.

**Text to edit:**

Longest random gap. Must be at least the minimum; up to 86400 seconds.

### Fade in (sec)

> ID: afmr.copy.settings.fade_in_sec
> Targets: All targets
> Where: Timing and playback — Hover text
> Placeholders: None
> Reference behavior: Fade in a newly started song. Seconds, 0-60; 0 starts at full volume.

**Text to edit:**

Fade in a newly started song. Seconds, 0-60; 0 starts at full volume.

### Fade out (sec)

> ID: afmr.copy.settings.fade_out_sec
> Targets: All targets
> Where: Timing and playback — Hover text
> Placeholders: None
> Reference behavior: Fade out a normal interruption or override transition. Seconds, 0-60; 0 cuts immediately. Combat death keeps its own fade setting.

**Text to edit:**

Fade out a normal interruption or override transition. Seconds, 0-60; 0 cuts
immediately. Combat death keeps its own fade setting.

### Context buffer (sec)

> ID: afmr.copy.settings.context_buffer_sec
> Targets: All targets
> Where: Timing and playback — Hover text
> Placeholders: None
> Reference behavior: A new playlist selection must stay stable this long before taking effect. Seconds, 0-60; separate from silence.

**Text to edit:**

A new playlist selection must stay stable this long before taking effect. Seconds, 0-60;
separate from silence.

## Timing: combat settings

### Combat music

> ID: afmr.copy.combatsettings.combat_music
> Targets: All targets
> Where: Timing: combat settings — Hover text
> Placeholders: None
> Reference behavior: Enable automatic combat detection. Manual playlist overrides remain available when off.

**Text to edit:**

Enable automatic combat detection. Manual playlist overrides remain available when off.

### Combat triggers

> ID: afmr.copy.combatsettings.combat_triggers
> Targets: All targets
> Where: Timing: combat settings — Hover text
> Placeholders: None
> Reference behavior: Combatants only: hostile/neutral mobs and other players, including their projectiles. Ignore passive targets and environmental damage. Any permits any targeted entity or damage source. Players-only playlist conditions still require another player.

**Text to edit:**

Combatants only: hostile/neutral mobs and other players, including their projectiles.
Ignore passive targets and environmental damage. Any permits any targeted entity or
damage source. Players-only playlist conditions still require another player.

### Quiet period

> ID: afmr.copy.combatsettings.quiet_period
> Targets: All targets
> Where: Timing: combat settings — Hover text
> Placeholders: None
> Reference behavior: Leave combat this many seconds after the last qualifying attack/damage event. Mob-target retention works in singleplayer and on servers with the AFMR companion: the countdown starts after the last target is lost. Targeting cannot start combat. PvP and servers without the companion keep their event timeout. Range: 1-600.

**Text to edit:**

Leave combat this many seconds after the last qualifying attack/damage event. Mob-target
retention works in singleplayer or with the AFMR server companion: the countdown starts after the last target is lost.
Targeting cannot start combat. PvP and servers without the companion keep their event timeout. Range:
1-600.

### Death fade

> ID: afmr.copy.combatsettings.death_fade
> Targets: All targets
> Where: Timing: combat settings — Hover text
> Placeholders: None
> Reference behavior: Used only for Fade on death, independently of the normal fade setting. Seconds, 0-60; 0 cuts immediately.

**Text to edit:**

Used only for Fade on death, independently of the normal fade setting. Seconds, 0-60; 0
cuts immediately.

### New music while dead

> ID: afmr.copy.combatsettings.new_music_while_dead
> Targets: All targets
> Where: Timing: combat settings — Hover text
> Placeholders: None
> Reference behavior: Prevent new songs from starting until respawn. A song allowed to finish by the death policy can keep playing.

**Text to edit:**

Prevent new songs from starting until respawn. A song allowed to finish by the death
policy can keep playing.

## Update: song groups and player themes

### Nearby player

> ID: afmr.copy.playlistconditions.nearby_player
> Targets: All targets
> Where: Conditions: nearby player — Hover text
> Placeholders: None
> Reference behavior: Match when the selected player is present within the chosen radius in your dimension. Uses client-visible players; no server installation or remote lookup. Excludes yourself. Ordinary entry and exit settings apply.

**Text to edit:**

Match when the selected player is present within the chosen radius in your dimension. Uses client-visible players; no server installation or remote lookup. Excludes yourself. Ordinary entry and exit settings apply.

### Notification delay

> ID: afmr.copy.nowplayingsettings.delay
> Targets: All targets
> Where: Display: Widget — Hover text
> Placeholders: None
> Reference behavior: Wait this many seconds of unpaused playback before showing now playing. Zero shows immediately. The full display duration follows the delay; changing songs cancels a pending notification.

**Text to edit:**

Wait this many seconds of unpaused playback before showing now playing. Zero shows immediately. The full display duration follows the delay; changing songs cancels a pending notification.

### Songs before silence

> ID: afmr.copy.songcount.count
> Targets: All targets
> Where: Timing / playlist silence — Hover text
> Placeholders: None
> Reference behavior: Play this many automatic or held-override songs back-to-back, then wait the normal silence. One preserves ordinary playback. Switching playlists, Skip/Next and manual playback start a new group. Queued songs and previews keep their normal silence.

**Text to edit:**

Play this many automatic or held-override songs back-to-back, then wait the normal silence. One preserves ordinary playback. Switching playlists, Skip/Next and manual playback start a new group. Queued songs and previews keep their normal silence.

### Nearby player selection guidance

> ID: afmr.copy.nearbyplayer.guidance
> Targets: All targets
> Where: Conditions: nearby player — On-screen guidance
> Placeholders: None
> Reference behavior: Enter a name and radius, or choose an online player to retain their UUID. Matching uses only live players in the client's dimension and tracked entities; the local player is excluded.

**Text to edit:**

Enter a player name and radius in blocks, or select an online player below.

### Playlist transition rules

> ID: afmr.copy.transitions.rules
> Targets: All targets
> Where: Playlist transitions — How rules work hover text
> Placeholders: None
> Reference behavior: Directional automatic transitions use the playlist that started the song. Explicit options override ordinary entry/exit and matching protection, Play on trigger timing and global timing only for this pair. Manual controls, queue/preview and combat death policies take precedence. Export both playlists to share their rule.

**Text to edit:**

Choose the playlist the song comes from, then its destination. Rules apply only in that direction during automatic playback. Unset options inherit normal settings. Manual controls and combat death settings keep precedence. Export both playlists to share their rule.

### Transition timing fields

> ID: afmr.copy.transitions.numeric
> Targets: All targets
> Where: Transition rule — Numeric field hover text
> Placeholders: None
> Reference behavior: Blank fields inherit existing timing; zero is an explicit override. Buffer and fades accept 0–60 seconds. Silence accepts 0–86400 seconds with both fields filled or both blank and maximum at least minimum. Only the first incoming song uses transition fades and silence; later songs use normal settings.

**Text to edit:**

Leave blank to inherit normal timing. Enter 0 for an immediate change. Buffer and fades allow 0–60 seconds; silence allows 0–86400 with both fields filled or both blank. These timings affect this transition, not later songs.

## Player proximity volume

### Open player proximity volume

> ID: afmr.copy.nearbyplayer.proximity_volume
> Targets: All targets
> Where: Nearby player condition — Hover text
> Placeholders: None
> Reference behavior: Adjust this playlist's volume with the selected player's distance. The trigger radius remains independent. Nearby player must be on for the volume adjustment to apply.

**Text to edit:**

Adjust this playlist's volume with the selected player's distance. The trigger radius remains independent. Nearby player must be on for the volume adjustment to apply.

### Proximity volume switch

> ID: afmr.copy.proximityvolume.enabled
> Targets: All targets
> Where: Player proximity volume — Hover text
> Placeholders: None
> Reference behavior: Scale the source playlist's volume by the selected player's distance. Off keeps the values. Enabled nearby rules in nested groups use the quietest result; disabled groups are ignored.

**Text to edit:**

Scale the source playlist's volume by the selected player's distance. Off keeps the values. Enabled nearby rules in nested groups use the quietest result; disabled groups are ignored.

### Near distance

> ID: afmr.copy.proximityvolume.near_distance
> Targets: All targets
> Where: Player proximity volume — Hover text
> Placeholders: None
> Reference behavior: At or inside this distance, use Near volume. Blocks, 0-1024; must be smaller than Far distance. Volume blends between the two distances.

**Text to edit:**

At or inside this distance, use Near volume. Blocks, 0-1024; must be smaller than Far distance. Volume blends between the two distances.

### Far distance

> ID: afmr.copy.proximityvolume.far_distance
> Targets: All targets
> Where: Player proximity volume — Hover text
> Placeholders: None
> Reference behavior: At or outside this distance, use Far volume. Blocks, up to 1024; must exceed Near distance. An offline or untracked player also uses Far volume. Only players tracked by your client can be measured.

**Text to edit:**

At or outside this distance, use Far volume. Blocks, up to 1024; must exceed Near distance. An offline or untracked player also uses Far volume. Only players tracked by your client can be measured.

### Near volume

> ID: afmr.copy.proximityvolume.near_volume
> Targets: All targets
> Where: Player proximity volume — Hover text
> Placeholders: None
> Reference behavior: Percentage of the source playlist's existing volume at the near distance, 0-100. Set this below Far volume to get quieter as the player approaches. Fade and normalization still apply.

**Text to edit:**

Percentage of the source playlist's existing volume at the near distance, 0-100. Set this below Far volume to get quieter as the player approaches. Fade and normalization still apply.

### Far volume

> ID: afmr.copy.proximityvolume.far_volume
> Targets: All targets
> Where: Player proximity volume — Hover text
> Placeholders: None
> Reference behavior: Percentage of the source playlist's existing volume at the far distance, 0-100. A zero endpoint mutes the song without stopping it or selecting another song.

**Text to edit:**

Percentage of the source playlist's existing volume at the far distance, 0-100. A zero endpoint mutes the song without stopping it or selecting another song.

### Reverse volumes

> ID: afmr.copy.proximityvolume.reverse
> Targets: All targets
> Where: Player proximity volume — Hover text
> Placeholders: None
> Reference behavior: Swap Near and Far volume while keeping both distances. Changes are applied smoothly to the source playlist's playing song, including playlist overrides and queued songs. Library previews use normal volume.

**Text to edit:**

Swap Near and Far volume while keeping both distances. Changes are applied smoothly to the source playlist's playing song, including playlist overrides and queued songs. Library previews use normal volume.

### Playlist import destination

> ID: afmr.copy.presetimport.destination
> Targets: All targets
> Where: Import playlist setup — Destination hover text
> Placeholders: None
> Reference behavior: Default to a new named profile containing only imported playlists, created on confirmation without activation or world binding. Existing-profile mode replaces names ignoring capitalization and surrounding spaces, retains IDs/priority, and stops ambiguous duplicates. Settings-only replacement keeps songs; importing songs replaces assignments.

**Text to edit:**

New profile keeps the import separate and lets you choose its name. Select an existing profile to replace matching playlist names and add unmatched ones. Matching ignores capitalization and surrounding spaces; ambiguous duplicates stop the import. Settings-only imports keep existing songs. Turning Import songs on replaces their assignments.

## Display: themes

### Shared theme settings

> ID: afmr.copy.themes.scope
> Targets: All targets
> Where: Display: Themes — Theme list hover text
> Placeholders: None
> Reference behavior: Select a theme to preview its ten menu colors, menu opacity, widget background color and widget opacity. Apply commits it; Cancel discards the preview. Themes do not contain images, fonts, layout, music or playlists. Save as creates an independent named copy. Imported names are made unique without replacing saved themes.

**Text to edit:**

Themes include menu colors and opacity, plus widget background color and opacity. Select to preview; Apply keeps the changes.

### Song actions

> ID: afmr.copy.library.song_actions
> Targets: All targets
> Where: Playlist workspace — Hovered/focused song action button
> Placeholders: None
> Reference behavior: Opens the existing song context menu. A selected row retains its multi-selection. Right-click and Shift+F10 also open the menu.

**Text to edit:**

Song actions. Right-click or press Shift+F10 with the song list focused.
