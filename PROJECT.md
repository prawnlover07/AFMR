# animal's final music replacer (AFMR)

AFMR is a client-side Minecraft soundtrack mod. It combines local music and vanilla
tracks, selects playlists from the player's situation, and provides manual controls.

Current build: **1.0.1**, **Minecraft 26.3**, **NeoForge 26.3.0.90**, **Java 25**.
The imported validated Git baseline remains **0.5.2**.
The owner/author is **animal**. This folder is the source of truth for development.

An independently buildable **Fabric 1.20.1 / Java 17** backport is maintained in
[`ports/fabric-1.20.1`](ports/fabric-1.20.1/README.md). It retains AFMR's playback
and library features with the AFMR card as its sole now-playing style, without
newer vanilla music-toast/frequency integration or menu blur. Its validation is
separate from the NeoForge results below; both maintained builds are version 1.0.1.

Keep the version unchanged while any development tasks are in progress, including
concurrent chats. Increment it once for the combined update only after all
in-progress tasks are complete; collect pending changes under Unreleased.

The [user guide](docs/USER-GUIDE.md) explains current controls and workflows. The
[developer guide](docs/DEVELOPER-GUIDE.md) explains the implementation and how to
keep the documentation current alongside code changes.

## Accepted product decisions

- 1.0.1 adds song groups (global/default 1 plus per-playlist override, 1-1000),
  current-song assignment copying, delayed now-playing (shared 0-60s/default 0),
  per-playlist return-to-shuffle repeat protection, and a nearby-player condition.
  Switching playlists resets group progress. Only natural scheduled/held-override
  completions continue a group; queues/previews and manual Skip/Next retain their
  normal policies and reset groups. Notification delay counts actual unpaused
  audio time and does not shorten visible duration. Nearby player uses a selected
  online UUID when known, otherwise an entered name, and a 1-1024 block sphere
  (default 32) over live client-tracked players in the same dimension, excluding
  self. It follows existing finish/fade controls and nested conditions/priority.
  Profiles/copies/sharing/backups preserve new configuration. Player names/UUIDs
  intentionally travel in playlist files but never in diagnostic summaries.
  Retain source assignments and existing disabled destination entries when adding
  a playing track. No game-launch shuffle persistence or remote player lookup.


- AFMR's own code and documentation are MIT licensed, copyright 2026 animal.
  Include `LICENSE` in source and runtime distributions on both targets. Preserve
  the NeoForge template and bundled libraries' separate notices and licenses.
  AFMR and all functionality remain freely available. Any future creator tips
  are optional support, with no donor-only builds, early access or features.

- Target a private AFMR 1.0 release for friends on October 23, 2026. Friend testing
  is deferred until after release at Animal's request; it is not a release gate.
  Finish the author-written text and all concurrent development first, then the
  focused game pass and release housekeeping. Keep 0.6 until that work is complete;
  increment once for the combined release. Use
  [the private release checklist](docs/RELEASE-1.0-CHECKLIST.md), and walk Animal
  through final packaging, installation and the Git checkpoint/tag. No public
  publication or automatic sending is requested.

- Before 1.0, Animal writes AFMR's flavor text, feature descriptions and hover
  explanations personally. Use `docs/UI-COPY.md` as the shared authoring source
  for both targets, with screen/control context, reference behavior, explained
  placeholders and explicit target differences. Compile it at build time into
  Minecraft language entries. Keep utility labels/states, counters, errors,
  confirmations, live diagnostic output and metadata wording unchanged. Clearing
  an editable paragraph hides that explanation, with no empty hover box;
  preserve metadata and ordinary clipped-label tooltips so wording can be restored.

- Each playlist can override the global silence range, off by default, through
  **Playback → Silence** in its workspace. Min/max are 0–86400 seconds, max ≥ min;
  equal values give a fixed gap. Incoming selection changes during ordinary silence
  apply the new range counting elapsed unpaused silence. Unchanged ranges do not
  reroll. Queued songs use their source playlist, or global timing without a source.
  Explicit playback/trigger/join bypasses and decoder retry backoff remain intact;
  normal Skip, held overrides and jukebox New after silence use the effective range.
  Disabling retains custom values. Copies, profiles, playlist sharing and full
  backups preserve them; global frequency/default changes cannot override custom timing.

- Inspector offers **Copy diagnostic summary** on both targets. Capture on demand
  without disk/log reads, changing playback, copying library snapshots or walking
  the whole track catalog. Share safe versions, runtime/context/playback/volume
  state, timing, scan counts, heap snapshot and ranked playlist eligibility, plus
  the inspected playlist's nested condition results. Use priority numbers instead
  of user names/IDs. Omit metadata, music paths, world/server identifiers, X/Z,
  saved location data, group labels and raw exception strings; error categories
  are inferred from the current message. Keep output at most 48,000 characters
  and the first 64 priority rows, with inspected details available beyond that cap.
  Copy locally through Minecraft's clipboard API and show success/failure in the
  button; no automatic sending, file export or persistent diagnostic monitoring.

- Display settings has two tabs: **Widget** and **Menus**. Consolidate visibility,
  position, style/native toast option, duration, size, wrapping, information fields,
  text scrolling and card colors/opacity/presets/reset in one Widget panel. Use
  bounded whole-row scrolling on smaller windows, with wheel, drag and keyboard
  access; keep Preview and Save available in the footer. Retain numeric drafts and
  scroll across hiding and child pickers/preview; clamp scroll to resized bounds.
  Keep drafts across resizing. Hidden controls must not
  receive input; scrolled tooltips follow their current bounds. Menus holds only
  menu colors, opacity, image and available blur; preserve style availability rules.

- Timing offers **Play immediately on join**, off by default and saved per profile
  on both targets. Arm only at the actual world/server join after activating its
  bound profile, then use the first eligible automatic selection from fresh
  context. Skip initial silence and buffer once; keep Fade in, later gaps and
  context changes. Wait for eligibility if initially empty. Pause/death/time-fit
  guards remain effective; manual controls and enabled jukebox policies take
  precedence. Respawn, dimensions, profile changes and reloads do not arm a new
  join. Persist in snapshots, profile copies/switches and full backups; numeric
  Minecraft defaults preserves this independent toggle. Turning it on mid-session
  does not skip the current gap. Keep the Timing control directly visible.

- Mark options that require singleplayer in their visible option label and editor
  title on both targets. Structures is currently the only wholly restricted
  condition; explicitly identify multiplayer unavailability in its editor and
  tooltips. Keep configuration/editing available for later singleplayer use.
  Combat works in multiplayer, so qualify only mob-target retention in hover text.
  Normalization also works outside a world; its existing wait state remains.

- Full configuration transfer uses `.afmrb` archives through Import / export →
  Full configuration. Include all profiles/settings/playlists/assignments, library
  paths and cached metadata/loudness, world/server links, override slots, AFMR
  physical shortcuts, history and the managed background; never include music,
  foobar data or unrelated Minecraft options. Review before replacement and stage
  for next launch. Preserve prior managed files and roll back interrupted installs
  before loading the library. Reject unknown ZIP entries, traversal, oversized,
  incomplete, deeply nested or invalid data. Use the existing scanner with owned
  snapshots; startup installation precedes workers. Keep paths and use Library
  repair if folders moved. Unsupported platform shortcuts become unbound with a
  review warning; unavailable platform-specific display controls do not gain shims.

- List row backgrounds must cover exact viewport bounds outside the rounded text
  scissor. Paint the entire reserved horizontal scrollbar strip, including its
  pinned-column and right edges, once. Do not add overlapping alpha backgrounds.

- All ten menu-color families and the AFMR widget background offer clickable
  swatches with a visual HSV picker on both targets. Keep manual hex entry,
  synchronized previews, presets, Original/New swatches and keyboard arrows.
  Apply retains the choice; Cancel/Escape restores the opening RGB. Opacity is
  independent; hiding retains the active picker and draft. The widget style's
  existing availability rules still apply. Cache gradient strip colors outside
  steady rendering and keep the picker free of image/disk work.

- Both targets expose Display → Menus → Menu colors with ten shared RGB color
  families: background, panels, buttons, hover, selection, highlights, text,
  secondary text, borders and scrollbars. Related shades preserve their channel
  offsets; defaults reproduce the original palette. Valid edits preview live;
  invalid drafts block navigation. Colors persist globally across profiles.
  Reset colors preserves opacity/blur/image/widget settings; full menu reset
  restores colors as well. Text/highlights remain opaque and HUD styling stays
  independent. Keep Minecraft-native field/option/tooltip skins and error colors.

- Custom menu backgrounds are shared appearance settings on both maintained targets.
  Choose PNG/JPEG/static WebP/GIF in Explorer; preserve the original and store a
  bounded copy under `config/afmr/menu-background.png` or `menu-background.gif`.
  GIFs animate and continuously loop, preserving delays, transparency, offsets and
  disposal rules. Full backups carry the current asset. Provide on/off, Fill/Fit/Stretch, separate image
  opacity, removal and reset. On 26.3, submit the image in a GUI stratum before
  Minecraft's menu blur. Menu panels/text remain above it. Fabric has no menu blur.
  Keep decode/file work off rendering, cap inputs at 20 MiB / 64 megapixels and
  texture edges at 2048. GIFs accept up to 512 frames / 256 million total source
  patch pixels and resize to at most 16,777,216 retained pixels (64 MiB RGBA) across
  frames. Cache uploaded frame textures; select by a monotonic clock without
  decoding, I/O or allocations while drawing. Release all frames on
  replace/disable/reload/exit. Animated WebP is explicitly rejected; use GIF.

- `.afmrp` shares playlist settings, for one or several selected playlists
  in their relative priority order. Preserve all per-playlist flags, volume and
  condition parameters, including disabled/nested All/Any/Not groups. Do not include
  audio files, absolute music paths, global settings, profile bindings or original
  location world/server identifiers. Keep coordinates/radius/dimension; bind all
  configured locations to the importing player's current world/server at Apply.
  Those imports require being in a world; setups without configured locations
  can import at the title screen. Preview and choose destination profile/position;
  create new playlist IDs without overwriting existing playlists. Explorer open/
  save and one-file drop are supported. Up to 128 playlists and 16 MiB per file;
  reject unsupported fields and malformed/oversized/deep data without changes.
  Export and import each have an optional song-list switch, off by default. Song
  lists retain explicit on/off assignments, resource IDs and paths relative to
  named music roots (up to 100,000 distinct songs, within the same 16 MiB limit).
  Match unique linked root names or let the user choose roots in Explorer; use
  exact relative paths, never filename/title guesses. Ambiguous root names require
  explicit choices. Keep unfound songs assigned and persist unlocated roots as
  missing references; open ordinary Library repair after import so folder-relative
  relinking and individual renamed-file repair remain available. Music stays
  read-only. Both NeoForge 26.3 and Fabric 1.20.1 use the same portable references.

- Library repair checks linked folders and all local tracks on rescan, with a
  toast and searchable missing/unavailable-source report. Checks also work without
  any folder links. Preserve assignments and files; use Explorer to choose a new
  root or individual renamed file. Folder repair matches exact relative paths,
  previews results, then changes AFMR references only. Partial matches update
  expected paths while leaving unmatched songs listed for individual repair; zero
  matches reject a likely wrong root. Nested folder links and inactive profile
  destinations move together. Stable track IDs survive rescans, and duplicate
  target entries merge across profiles/legacy/queue/history with off winning
  assignment conflicts. Already linked folder destinations require unlinking the
  duplicate folder first to avoid overwriting its rescan settings. Apply stops an
  affected playing song, preserving other playback and pending requests; source
  checks and metadata reads stay on the existing library worker.

- Per-playlist Volume is 0-100%, default 100%, with live adjustment and reset.
  It multiplies the fade envelope and Minecraft/ReplayGain gain. Source ownership
  persists through finish protection, queue and overrides; standalone previews use
  normal volume. Muting a playlist does not change automatic eligibility.
- Profiles share imported music, metadata/loudness, folders, appearance, import
  preferences, physical keys, override slot assignments and listening history.
  Each owns playlists/order/conditions/assignments/volume, timing, combat, jukebox,
  normalization policy and folder rescan destinations. Up to 32 named setups can
  be created from templates or copied independently. Exact world folders/server
  addresses bind setups on join; unlinked worlds and the title screen use Default.
  Use now switches without changing links, stopping music and clearing session
  queue/override/pause state. Default and the active setup cannot be deleted.
  Imports keep the originating profile through picker callbacks and scan completion;
  deleted destinations leave imported tracks in the shared library only.

- Display → Menus controls background opacity for AFMR's workspace and settings,
  with immediate slider preview and Minecraft's pause-menu blur. Default: 80%
  opacity, blur enabled; blur strength follows Minecraft. Text, artwork and
  foreground indicators remain opaque. Widget appearance stays independent.

- M (or the rebound music-library key) opens and closes AFMR. Reopening remembers
  the page and editor position during the same world session. Text entry takes
  priority; hiding a settings page retains drafts for Save & back.
- Seed six editable templates: Combat, Home, Dark exploration, Underground, Night
  outside, Day outside, in that initial priority order. They use the same conditions
  and editor as all playlists, with no special runtime rule enum. Templates can be
  renamed, cloned and deleted. Defaults do not depend on biome. Clones own conditions,
  nested groups, assignment switches and playback flags independently; music stays shared.
- Every playlist can combine optional location, time, dimension, elevation,
  cover, biome, structure, temperature, light, combat, health, water, weather, held-item, and gamemode filters.
  Match all (default) requires every enabled filter; Match any requires at least one.
  Enabled nested groups use the same All/Any/Not logic; disabled groups are skipped.
  No enabled filters/groups matches everywhere before Not. Unavailable structure
  data does not become a match through negation. Four nested levels, eight children
  per node and 64 nodes bound evaluation and editing cost.
  Any selected exact biome
  or structure satisfies its individual filter. Weather offers Any weather
  (rain/snow/thunderstorms), Rain, Snow, Thunderstorm, and Clear; filter off permits all.
  Structure detection initially uses loaded generated singleplayer structure bounds,
  with an optional 0-128 block spherical radius outside the bounds (0 = inside only).
  Multiplayer structure synchronization is planned; unavailable structure data
  cannot satisfy that filter. Match any can still activate through another enabled
  filter; Match all falls through to normal eligible playlists.
- The first eligible playlist in the editable priority list wins. Empty playlists
  fall through. Each Location filter owns a world/dimension-scoped sphere. Home is
  an unconfigured Location template. Autoplay here requires a continuous one-second
  hold with red warning text and a filling bar; releasing/moving off/leaving cancels.
  Completion replaces the selected playlist's conditions with a 64-block sphere at
  the player, enables Automatic and clears Manual only, preserving music and flags.
- Time conditions offer **Fit songs to time window**, off by default. Before an
  automatic start, choose only songs whose known duration fits the remaining natural
  20-tick/second matching interval. Missing lengths are discovered in the background;
  if none fit, retain the selected playlist and stay silent. Wrapped and All/Any/Not
  windows preserve their logic. Clock jumps/rate changes are not predicted, and this
  option does not interrupt a playing song or restrict overrides/previews.
- Underground requires both its Y cutoff and sufficient solid cover. Dark exploration
  requires cover and darkness, with independent optional whole-dimension Nether/End
  branches that ignore cover and light there. These are ordinary editable groups;
  Day/Night carry independent negated Underground groups.
- Music augments vanilla by default. Files/folders are imported through native file
  dialogs; launch rescans are optional. Import foobar `.fplite` into an existing
  chosen playlist. Never modify the original files or foobar data.
- Automatic off disables detection/selection only. Manual overrides still work.
  Individual disabled songs remain ineligible. Manual-only playlists are supported.
- Finish current song on entry and on exit are independent. Either the outgoing
  source's exit or incoming playlist's entry can protect a song. Ownership stays
  with the playlist that actually started it, including across multiple transitions.
- Per-playlist Play on trigger is off by default. Automatic winning entry bypasses
  the buffer and initial silence while retaining normal fades, independent finish
  protection, Continue Playing on Match and time-window fitting. It does not restart
  on every matching tick or remove later inter-song silence. Manual controls,
  preview/override priority, pause and death guards stay effective.
- Global **Continue Playing on Match** (off by default) also protects a playing
  song when its library ID is eligible in the incoming automatic playlist. Every
  accepted automatic change checks again; a nonmatch uses the normal transition
  rules. Natural completion uses normal silence. Manual overrides retain priority
  and their existing immediate-switch behavior.
- Normal transitions use the configured buffer, Fade out, silence, then Fade in.
  Both fade durations are independent, default to zero and accept 0-60 seconds.
  Minecraft-like timing defaults are 600-1200 seconds, with no added fades or buffer.
- Timing's Jukebox selector defaults off. Audible nearby records can fade AFMR
  out and pause/resume the same song, end it and wait a full normal silence after
  the record ends, or end it and immediately start the next eligible song after
  the record ends without silence/buffer. Resume uses Fade in. Multiple records
  retain the hold until none are audible; muted records do not hold it. Manual
  pause owns an independent hold and death guards remain effective.
- Combat defaults to attacking or receiving source-attributed damage from hostile/
  neutral mobs and other players. Passive targets and environmental damage do not
  refresh it; a global broad-trigger option remains available. Attack/damage switches
  are independent. In singleplayer, once an event starts combat, actual targeting
  by loaded hostile/angered combat-capable mobs sustains it; losing the last target
  starts the quiet countdown. Targeting alone cannot start or revive combat.
  Multiplayer retains the attack/damage timeout until server target data is available.
  A playlist Combat condition can restrict itself to Players only,
  using an independent PvP timer that mob attacks cannot extend. Combat supports
  immediate death fade by default, cut, or finishing the song. New music while dead
  is blocked by default.
- Health conditions compare red hearts only, excluding absorption, strictly above
  or below a configurable threshold. Exact equality does not match. Health and PvP
  work in the same All/Any/Not groups and priority rules as other filters.
- Held items matches any selected exact item type in the main hand, off-hand, or
  either hand (default). Search names/IDs or add current held items. Empty hands
  do not match; item names, enchantments, durability and stack counts do not affect
  the type. Works in the existing All/Any/Not groups without server support.
- Gamemode matches any selected Survival/Creative/Adventure/Spectator mode,
  observed from Minecraft's client player-mode state. Filter off by default;
  Survival initially selected. Enabled with no selections does not match. It
  participates in All/Any/Not groups, clones, persistence and normal transitions.
- One-song and held overrides, eight configurable key slots, an inspector, per-playlist
  Fisher-Yates shuffle bags and repeat avoidance are implemented.
- Next bypasses waiting and keeps override state. Pause holds the actual audio position
  and playback timers while live context detection continues. Both keys start unbound.
- Play-next queue holds up to 100 explicit requests in editable FIFO order. Enqueue
  does not interrupt the current song or bypass normal silence. Queued songs bypass
  automatic conditions/time fitting and context interruptions, while respecting their
  source playlist's song switches, failures, pause, jukebox and death guards. Next
  uses a ready queue first. A new playlist override takes precedence without deleting
  requests; Use queue next or another enqueue restores queue priority. Once drained,
  normal automatic selection or a retained held override resumes. Reset/logout/reload
  clears pending requests; clearing the queue leaves the current song playing.
- Listening history retains the latest 100 successful starts across launches, with
  metadata, source playlist, origin and a selection/context explanation. It offers
  replay, queue, add to a playlist and Clear history. Bounded snapshots save separately
  in `config/afmr/history.json` on the existing writer, without copying the whole music
  library on song starts. Pauses/resumes do not add entries; original music stays read-only.
- Existing ReplayGain track gain/peak tags take precedence over cached loudness scans.
  An explicit, cancellable **Normalize playlist** action analyzes imported tracks
  at a shared -18 LUFS target, saves results only in AFMR, and reports each track's
  effective dB adjustment. Vanilla music stays unchanged. Scanning suspends during
  active gameplay pending gameplay-impact measurements.
- Use the playlist workspace: priority above the library on the left, songs in
  the center, conditions/playback switches on the right, transport below. Keep labels
  concise; no welcome flow, tutorials, or flavor text. Preserve folder/column editing,
  multi-selection, sorting, previews, all import paths, and independent scrolling.
  Columns beside Add music saves global song-column visibility. Title and On remain
  required; optional album artist, track/disc numbers, genre, date, ReplayGain
  track/album gain and peak, cached AFMR gain and path scroll horizontally at
  readable widths. On stays pinned. Numeric fields sort numerically, with missing
  values last. Metadata is read only on import/rescan and cached for display;
  existing songs require a rescan to populate newly added fields.
  Now-playing style is selectable: Minecraft's native
  music toast (default), a Hybrid toast with Minecraft's frame and AFMR artwork, or
  the bottom-left AFMR card. Hybrid/AFMR offer custom duration, scale, width, individual
  metadata/icon switches, single-line/word-wrap layouts and optional clipped text scrolling.
  Word wrap preserves whole words and adds height without expanding the chosen width.
  Missing art uses animated
  music notes. The AFMR card has no accent bar, defaults to black at 92% opacity,
  and offers background color/opacity controls. Widgets omit the redundant Now playing
  label. All three styles have a corner selector; Default preserves their existing
  positions and bottom corners avoid the hotbar. The playback footer clears track
  information/artwork in silence while retaining selection for Preview.
  Native style respects Minecraft's music-toast visibility and notification
  duration settings, including the pause menu.
- Changing vanilla Music Frequency applies AFMR gap presets: Default 600-1200
  seconds, Frequent 600 seconds, Constant 5 seconds. It restarts a pending gap but
  preserves a playing song, fades, buffer and saved widget preferences. Custom gaps
  remain editable and are not overwritten on startup.

## Current validation and next work

**Play immediately on join** passes **718 NeoForge / 728 Fabric automated tests**,
plus **10 NeoForge / 14 Fabric focused native checks**. Actual joins use the bound
profile and skip a 600-second gap / 60-second buffer. The Timing layout was checked
and screenshots inspected. See [the join report](docs/testing/2026-10-23-play-on-join.md).

Full configuration transfer and list seam fixes pass **702 automated NeoForge**
and **712 Fabric tests**, zero failures. Native export/review and restart checks,
transfers in both directions between the maintained builds, 16 NeoForge seam
cases and the 83-check packaged Fabric run pass. See the [backup report](docs/testing/2026-10-23-full-backup.md)
and [seam report](docs/testing/2026-10-22-menu-seams.md).

Visual color pickers pass **691 automated NeoForge tests** and **701 Fabric tests**,
plus **25 focused NeoForge native checks** and **75 packaged Fabric smoke checks**.
All menu/widget swatches and picker controls were exercised; see
[the report](docs/testing/2026-10-22-color-pickers.md).

The Fabric cover fix passes **694 automated tests**, including ten new real-block
heightmap/cache/playlist regressions. Newly received 1.20.1 chunks now use their
synchronized motion-blocking map; the user's modpack recheck remains pending.
See [the cover report](docs/testing/2026-10-22-fabric-cover.md).

Menu color customization passes **684 automated NeoForge tests** and **694 Fabric
tests**, plus **29 focused NeoForge native checks** and **51 packaged Fabric smoke
checks**.
Live theme previews, validation, reset, selected accents and text/surface alpha
were checked; see [the report](docs/testing/2026-10-22-menu-colors.md).

The Fabric artwork crash reported with Homestead is reproduced and fixed: a
139,885-byte cover PNG exceeded the old decoder's 65,536-byte native scratch
stack. Both artwork displays now use the stream decoder. The port passes 661
automated tests plus nine windowless native-image checks on Java 17 and Java 25;
full modpack soak testing remains separate. See the
[artwork memory report](docs/testing/2026-10-22-fabric-artwork-memory.md).

Optional song-list sharing passes **661 automated tests per target**, including
22 new export/matching/repair cases on NeoForge 26.3 and Fabric 1.20.1. Native
Explorer/UI and playback checks for this extension remain deferred; see the
[song-sharing report](docs/testing/2026-10-22-song-sharing.md).

The earlier combined Unreleased setup passed **409 live/helper checks**, including
83 integration, 12 actual restart and 39 vanilla-server checks, alongside
**639 automated tests**. Native OpenAL volume,
queue/pause/history, copied profiles, moved/renamed library repair, sharing and
shutdown were checked in a disposable profile. Actual Explorer export/import and
Save cancellation also passed. Shutdown ordering and excessive WAV metadata logging
were corrected during validation. See [the combined report](docs/testing/2026-10-22-release-validation.md)
for remaining work and precise limits.

Playlist setup sharing passes **639 automated tests**, including 23 new `.afmrp`
round-trip, privacy, location, profile, ordering and validation regressions. Core
capture/import avoid reading a guarded 70k music catalog. Live Explorer/save,
screen and world-binding checks remain deferred; see
[the report](docs/testing/2026-10-22-playlist-presets.md).

Library repair passes **616 automated tests**, including 27 new health/repair
regressions, generated-fixture preservation and a 70k steady-playback guard.
Explorer interaction, real drives and live audio remain deferred; see
[the report](docs/testing/2026-10-22-library-repair.md).

Playlist volume and world/server profiles pass **589 automated tests**, including
41 new volume, ownership, persistence and indexed-library regressions. Native
UI/audio and connection lifecycle checks remain deferred; see
[the report](docs/testing/2026-10-22-volume-profiles.md).

Queue and listening history pass **548 automated tests**, including 39 focused
queue/history/storage checks and a guard against traversing 70,000 library entries.
Native UI/audio validation remains deferred; see
[the report](docs/testing/2026-10-22-queue-history.md).

Optional song columns pass **509 automated tests** and **179 isolated in-game
checks**. Generated tagged MP3/FLAC/OGG/WAV imports, saved column preferences,
numeric sorting, horizontal scrolling and pinned assignment controls passed.
See [the report](docs/testing/2026-10-22-song-columns.md) for scope and limits.

Independent fades and jukebox policies pass **491 automated tests** and **162
isolated in-game checks**. Actual record insertion/removal, all three policies,
overlapping/muted records and saved timing controls passed. Native sample-offset
and subjective listening checks remain deferred; see
[the report](docs/testing/2026-10-22-fades-jukebox.md).

The Unreleased singleplayer combat-targeting update passes **468 automated tests**,
including 18 new tracker checks. Live mob AI, server lifecycle and native audio
validation remain deferred; see [the report](docs/testing/2026-10-22-combat-targeting.md).

The earlier combined **0.6** build passed **450 automated tests** and **135 isolated
in-game checks**. Gamemode switching, trigger fades/no entry silence, missing WAV
duration discovery, time-fitting silence/priority and the workspace regressions
were checked. See [the report](docs/testing/2026-10-22-release-0.6.md).

Fit songs to time window passes the **432-test** automated suite, including 23
fitting tests and 11 duration-reader checks. Native UI/audio and gameplay-impact
checks remain deferred; see the [time-fit report](docs/testing/2026-10-22-time-window-fit.md).

The held-item condition compiles and passes the **386-test** automated suite,
including 13 held-item regressions. Live inventory/UI/audio checks remain deferred;
see the [held-item report](docs/testing/2026-10-21-held-items.md).

The 0.5.7 playlist workspace passes **331 automated tests** and **48 isolated
in-game screen/control checks**. See [the UI report](docs/testing/2026-10-21-workspace.md)
for the feature mapping, renderer checks and physical-input/native-dialog limits.

The validated 0.5.2 baseline had **227 passing tests**. The October 21 session validated
**81 native playback checks and 6 persistence checks**, plus UI scanning/import/settings
checks. See [the report](docs/testing/2026-10-21.md), [validation](VALIDATION.md), and
[remaining complex cases](DEFERRED-TESTING.md). No AFMR defect was reproduced in that run.

The unreleased loudness scanner's automated and independent measurement checks
are recorded [separately](docs/testing/2026-10-21-loudness.md); the baseline native
checks do not validate its new UI or native playback integration.

The unreleased structure filter compiled and passed its automated checks within
the **266-test** suite. Native structure discovery and menu checks remain deferred;
see the [structure verification report](docs/testing/2026-10-21-structures.md).

Weather filters and Match all / any now pass the **286-test** automated suite.
Live weather/UI validation remains deferred; see the
[weather and matching report](docs/testing/2026-10-21-weather-matching.md).

Continue Playing on Match now passes the **309-test** automated suite, including
23 new feature checks. Native audio continuity and settings UI checks remain
deferred; see [validation](VALIDATION.md) and [deferred testing](DEFERRED-TESTING.md).

The earlier playback implementation also passed **27 native checks against an unmodified vanilla
26.3 server**, including synchronized conditions, combat/death, audio and paused
disconnect/reconnect. Logout now clears session state immediately. See the
[multiplayer report](docs/testing/2026-10-21-multiplayer.md). Loudness analysis still
waits while connected, following the user's preference to protect gameplay performance.

The Hybrid toast, custom widget size/information controls and optional text scrolling
and AFMR appearance/word-wrap settings now pass the **329-test** automated suite. In-game visual/input checks remain in
[deferred testing](DEFERRED-TESTING.md); see [validation](VALIDATION.md) for scope.

The menu toggle/place-retention update passes the same **329-test** suite;
physical input and live menu restoration remain in [deferred testing](DEFERRED-TESTING.md).

Future Minecraft downgrades should be deliberate ports after choosing a target;
the current code is not a multi-version compatibility layer. Requested future features
belong in [the backlog](BACKLOG.md). See [development instructions](DEVELOPMENT.md) for
the build, source layout, Git checkpoints, and native test fixture.
