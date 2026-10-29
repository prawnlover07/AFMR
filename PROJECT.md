# animal's final music replacer (AFMR)

AFMR is a client-side Minecraft soundtrack mod with an optional server companion.
It combines local music and vanilla
tracks, selects playlists from the player's situation, and provides manual controls.

Current build: **1.1.1**, **Minecraft 26.3**, **NeoForge 26.3.0.90**, **Java 25**.
The imported validated Git baseline remains **0.5.2**.
The owner/author is **animal**. This folder is the source of truth for development.

An independently buildable **Fabric 1.20.1 / Java 17** backport is maintained in
[`ports/fabric-1.20.1`](ports/fabric-1.20.1/README.md). It retains AFMR's playback
and library features with the AFMR card as its sole now-playing style, without
newer vanilla music-toast/frequency integration or menu blur. Its validation is
separate from the NeoForge results below; both maintained builds are version 1.1.1.

An added [Fabric 26.3 target](ports/fabric-26.3/README.md) now passes its independent
build and packaged client/companion checks. It preserves the modern vanilla
toast/frequency/blur integrations. The added [Forge 1.20.1 target](ports/forge-1.20.1/README.md)
also passes its independent build, packaged playback and companion/fallback checks.
Both [Fabric](ports/fabric-1.21.1/README.md) and
[NeoForge](ports/neoforge-1.21.1/README.md) 1.21.1 targets pass those gates and their
independent source builds; see [verification](docs/testing/2026-10-29-1.21.1-ports.md).
Fabric and Forge 1.18.2 / 1.19.2 now pass their own client, companion/fallback
and independent source-build gates; see [their report](docs/testing/2026-10-29-older-ports.md).
All ten targets and runtime requirements are in [the support matrix](docs/PORTING.md).
The combined local 1.1.1 packages include the owner's green-note mod icon;
the original baseline tag and packages remain intact.

Keep the version unchanged while any development tasks are in progress, including
concurrent chats. Increment it once for the combined update only after all
in-progress tasks are complete; collect pending changes under Unreleased.

The [user guide](docs/USER-GUIDE.md) explains current controls and workflows. The
[developer guide](docs/DEVELOPER-GUIDE.md) explains the implementation and how to
keep the documentation current alongside code changes.

## Accepted product decisions

- For Minecraft 1.18.2 / 1.19.2 ports, clients without the optional companion
  start combat from local attacks only. Do not infer attackers from proximity or
  treat falls/fire as fights by default. The bundled integrated-server hooks and
  matching companions supply precise incoming and ranged-hit events, with no
  opponent identity/location or music data. Expose the availability limitation.
  Modern targets keep their native attributed-damage packets. This is a confirmed
  port decision, not a change to modern combat defaults.

- Animal authorized 1.1.1 for the existing NeoForge 26.3 and Fabric 1.20.1 builds
  first, then additional tested ports. The agreed five Minecraft targets are
  26.3, 1.21.1, 1.20.1, 1.19.2 and 1.18.2, each with Fabric plus NeoForge (26.3 /
  1.21.1) or Forge (1.20.1 / 1.19.2 / 1.18.2). Matching optional server companions
  are part of each port. A planned target is not supported until its own build,
  packaged gameplay/audio and companion/fallback checks pass. Preserve historical
  releases; public uploading is separate from local release preparation.

- Show now playing is a configurable client-side hotkey, unbound by default, on
  both targets. Reopen the current configured widget/toast immediately for its
  normal duration; bypass song-start delay, refresh on repeated presses, and
  allow manual music pause. Respect AFMR visibility and Minecraft toast/HUD
  settings. Do nothing without a current song or while a screen captures input.
  Preserve audio, scheduler/notification identity, queue/history and cached art.
  Include the key in full backups without invalidating existing 1.1 backups.

- Animal authorized the combined 1.1 release on October 28, 2026, after concurrent
  development completed. The version freezes recorded in the feature decisions
  below applied while those features were in progress; their combined update is
  now 1.1. Preserve the original 1.0.1 tag, packages and historical measurements.

- Workspace transport uses font-independent Play/Pause/Resume and immediate-next
  icons with state-specific tooltip/narration names. The primary action resumes
  a manual pause (including a silent gap); otherwise no song requests Play now. Keep Skip & wait labelled
  and disabled without a song; preserve its ordinary silence and the existing
  immediate-next pause/queue/override behavior. No scheduler/audio changes.
- Playlist priority rows expose one shared hover/focus ellipsis in a reserved
  right-edge slot, separate from the playing marker and name. It opens the existing
  opaque playlist menu for the actual clicked row, including after scrolling.
  Preserve same-playlist song selection/search, invalid-draft guards, Undo/Redo,
  right-click, F2 and priority dragging; Shift+F10 also opens playlist actions.

- Both targets provide a bottom song-position bar in the playlist workspace and
  AFMR settings pages. Click/drag previews a target and commits on release;
  arrow keys seek five seconds and Home returns to the start. Keep pause,
  source/queue/history/group identity and notification listening-time separate
  from song position. Unknown lengths disable seeking until the existing duration
  worker resolves them. Decoder work uses existing workers; canceled seeks close
  their streams and repeated same-tick requests coalesce to the latest target.
- Clicking the current-song title locates its actual source playlist and song,
  clearing its song search without changing playback. Song menu actions show
  their affected selection count. Search settings opens and highlights controls,
  including global settings, fonts/themes and the selected playlist's rules.
  Search navigation respects invalid drafts. Boolean options use checkboxes;
  arrows identify details/navigation, while mode selectors retain cycling.
  Reserve transport space in page layouts and keep version 1.0.1 unchanged.

- Optional server companions for NeoForge 26.3 and Fabric 1.20.1 provide loaded
  structure boundaries/catalogs and sustain event-started combat while hostile or
  angered mobs target the player. No target-only combat starts, no PvP retention,
  and no terrain loading/generation. Clients without a companion keep ordinary
  multiplayer support and event timers. Server JARs exclude client/audio/library
  code; send only self-scoped observations, never music or playlist settings.
  Keep version 1.0.1 unchanged during development.
- New features or conditions that would otherwise be singleplayer-only because
  they need server-owned state must include matching companion support for both
  maintained targets in the same change. Update observations, availability text
  and tests together; verify multiplayer with and without the companion, and
  explicitly record any unavoidable limitation or unfinished validation.

- The workspace marks the current song and its source playlist separately from
  editing selection. Hover/focus song actions expose the existing opaque menu;
  Shift+F10 and right-click retain multi-selection. Compact rules use clock times
  and selected names. Waiting transport text gives reasons/countdowns without
  performing new catalog scans or changing scheduler policy.
- Theme previews own separate draft colors/opacity. Only Apply commits current
  appearance and the selected named theme. Cancel accepts invalid drafts, paging
  does not save, hiding suspends the preview and navigation cancels it. Explicit
  Save as/rename/delete remain collection operations. Never persist preview
  colors through unrelated saves, backups or color-picker callbacks.

- Themes include menu RGB colors/opacity and widget background RGB/opacity.
  Display → Menus → Themes owns named copies, editing, rename/delete and .afmrt
  sharing on both targets. Imported themes append with unique names; select to
  preview and explicitly Apply or Cancel. Saved themes are shared across profiles and included in full backups.
  Keep fonts, images, blur, layout, music and playlists outside theme files.

- Graphite is the default menu palette on both targets. Drawing shades, Reset
  colors and Reset menu appearance use the same charcoal/blue palette. Keep saved
  custom colors; users can reset the palette to opt in on existing installations.
  Widget background color uses its picker/RGB field; omit fixed color presets.

- All disposable Minecraft probes are capped at 60 FPS from startup, with VSync
  off in test profiles only. The owner reports substantial desktop lag above 60.
  Enforce this for native and Fabric smoke modes, including performance runs;
  retain other options and personal profiles. This is test setup, not an AFMR
  playback setting or a diagnosis of the underlying system lag.

- Playlist edits have session-only, per-profile Undo/Redo; global settings remain
  outside that history. Drag playlist rows to change priority. Song actions live
  in an opaque right-click menu and preserve an existing multi-selection. Compact
  When it plays lists enabled filters; Show rules/More rules reveals the others.
- Menu and widget fonts are separate shared appearance choices: imported TrueType
  (.ttf) files or loaded resource-pack font IDs. Imported assets join full backups;
  external resource packs remain the player's responsibility. Header/tab geometry
  stays consistent across AFMR pages; playlist views draw custom backgrounds too.
- Minecraft Music or Master at zero only silences playback and shows
  **Music muted in Minecraft** in the workspace. It does not pause/resume AFMR or
  its scheduling. Manual pause stays independent. The prior delayed mute-pause
  controller was removed at the user's request on October 25.

- The playlist workspace uses inline **Playlist Settings**, keeping the library
  lower left and **Set playback area here** visible. The held one-second action
  still replaces only the selected playlist's conditions with a 64-block zone.
  Condition rows toggle; separate Edit controls open details. Area/time numeric
  details, volume and custom song gaps stay inline, with independent panel scrolling
  and draft validation. Rename is a playlist right-click/visible actions menu or F2
  action, never part of the conditions editor. Keep nested group naming, all rules,
  priority, duplication, sharing, overrides and independent finish flags available.
  Blank list space is silent; handled controls retain click feedback. No version bump.

- `.afmrp` imports default to a new filename-named, editable profile containing
  only imported playlists. Create it on successful confirmation, keep it inactive
  and unbound, and retain the shared music library. Existing-profile mode replaces
  names ignoring case and surrounding spaces; keep playlist object/ID, relative
  priority, key/transition/folder references. Settings-only keeps existing song
  assignments without traversing them; with Import songs on, replace assignments
  using the reviewed song plan. Block ambiguous duplicate matches before mutation;
  add unmatched playlists using insertion controls. Respect the profile limit
  without silently falling back. Keep the current working version unchanged.

- Repair / missing offers Cancel search during explicit source checks and repair
  preview preparation on both targets. Preserve the previous completed report
  and saved references; discard cancelled results even if work finished before
  the client processes the result. Stop between filesystem operations and keep
  the busy state until the existing worker acknowledges cancellation. Version
  remains 1.0.1.

- Riding animal is an ordinary Any/Riding/Not riding condition on both targets,
  off by default (Any). Observe the player's direct vehicle using Minecraft's
  Animal class; horses, other vanilla animal mounts and modded subclasses count.
  Standing near an animal, boats and minecarts do not count. Use synchronized
  client riding state in singleplayer and multiplayer, without entity scans or
  server requests. Keep All/Any/Not groups, priority, normal transition controls,
  copies/profiles, portable sharing and full backups. Version stays 1.0.1.

- Nearby player offers optional **Proximity volume**, off by default: two
  distance/volume endpoints (default near 4 blocks/100%, far 32 blocks/0%).
  Distances are 0–1024 blocks with far > near; endpoint volumes are 0–100% of
  the actual song-source playlist's volume. Reverse volumes swaps percentages.
  The existing trigger radius stays independent. Clamp outside endpoints; use
  Far volume for offline/untracked players. Use existing client-visible player
  observations, smoothly blending over 250 ms without restarting music. Apply to
  retained songs, overrides and source-owned queue entries; source-free previews
  retain normal volume. Enabled nearby rules in enabled condition groups use the
  quietest result; turning the filter/group off disables its curve. Keep fade,
  jukebox and normalization behavior and deep copies/profiles/sharing/backups;
  diagnostics omit identities. Version remains 1.0.1 during development.

- Explicit player actions such as **Normalize** must not be blocked behind a
  performance precaution requiring title-screen or paused-singleplayer use.
  Manual normalization starts immediately in gameplay and multiplayer and
  continues when its screen is closed. Its screen does not pause singleplayer.
  Preserve worker-owned decoding, bounded parallel lanes, cancellation with
  partial results, read-only music and normal saves. Keep version 1.0.1 unchanged.

- The AFMR now-playing card accepts its own custom background under **Display →
  Widget → Custom background…** on both targets. Reuse the bounded menu image
  decoder for PNG/JPEG/static WebP and animated GIF, storing an independent
  `config/afmr/widget-background.png` or `.gif`. Provide on/off, Fill/Fit/Stretch,
  separate image opacity, preview, removal and reset. Replace the colored panel;
  transparent pixels/Fit gaps show the game. Missing/failed loads use the existing
  card color. Text/artwork and popup fades remain intact. Appearance is shared
  across profiles and included in full backups/recovery. Hybrid/native toast
  frames keep their existing styles. Keep the development version at 1.0.1.

- Directional playlist transition rules are under each playlist's **Transitions**
  button. Choose the actual song-source playlist and its destination. Finish mode
  inherits, forces finish, or interrupts; independent optional buffer/fade-out/
  fade-in (0–60s) and silence min/max (0–86400s) override only this automatic pair.
  Blank timing inherits; zero is explicit. Forced interruption overrides both
  finish switches and Continue Playing on Match. Explicit buffer/silence takes
  precedence over Play on trigger for that component. Manual controls, previews,
  queues, death policies and time-fit guards retain precedence. Switching during
  a gap retains elapsed silence and evaluates from the last actual automatic source.
  Once a fade starts it completes; a later destination updates pending gap/fade-in.
  Later songs use ordinary timing. Retain deep owned copies in profiles/backups;
  portable sharing includes only links between playlists exported together and
  remaps both ends. Deleting a playlist clears incoming references. Maximum 128
  destination rules per source. Keep current development version 1.0.1 as requested;
  preserve the already tagged/packaged 1.0.1 checkpoint and collect this addition
  under Unreleased until the next release decision.

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

- Structure controls explain that observations require singleplayer or the optional
  AFMR server companion. Keep configuration/manual IDs editable without a connected
  data source. Combat works on all servers; qualify mob-target retention as requiring
  singleplayer or the companion. Manual normalization remains available in gameplay.

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
  Export whole-file SHA-256/size identities on the worker; no audio is embedded.
  Import offers explicit Search linked libraries (including known individual
  songs), additional folder searches and individual file choices. Verify identical
  bytes across different names/layouts; never guess from filenames/titles. Keep
  exact-relative-path matching for references without content information.
  Searches are cancellable and report verified/missing counts. Keep unfound songs
  assigned and persist unlocated roots as
  missing references; open ordinary Library repair after import so folder-relative
  relinking and individual renamed-file repair remain available. Music stays
  read-only. Both NeoForge 26.3 and Fabric 1.20.1 use the same portable references.

- Library repair checks linked folders and all local tracks on rescan, with a
  toast and searchable missing/unavailable-source report. Checks also work without
  any folder links. Preserve assignments and files; use Explorer to choose a new
  root or individual renamed file. Fingerprinted songs match identical contents
  anywhere under the chosen folder; unsigned references use exact relative paths.
  previews results, then changes AFMR references only. Partial matches update
  expected paths while leaving unmatched songs listed for individual repair; zero
  matches reject a likely wrong root. Nested folder links and inactive profile
  destinations move together. Stable track IDs survive rescans, and duplicate
  target entries merge across profiles/legacy/queue/history with off winning
  assignment conflicts. Verified folder repair can merge an already linked
  destination, preserving its rescan settings. Unsigned duplicate roots still
  require an explicit unlink. Apply stops an
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
  Structure detection uses loaded generated structure bounds in singleplayer or
  companion-enabled multiplayer,
  with an optional 0-128 block spherical radius outside the bounds (0 = inside only).
  Servers without the optional companion provide unavailable structure data, which
  cannot satisfy that filter. Match any can still activate through another enabled
  filter; Match all falls through to normal eligible playlists.
- The first eligible playlist in the editable priority list wins. Empty playlists
  fall through. Each Location filter owns a world/dimension-scoped sphere. Home is
  an unconfigured Location template. Set playback area here requires a continuous one-second
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
  Both fade durations are independent and accept 0-60 seconds. Fresh defaults are
  fade-in 0, fade-out 3 and context buffer 3 seconds, with 600-1200 second gaps.
  New profiles use these defaults; saved timings are preserved. The separate
  Minecraft defaults preset restores zero added fades/buffer.
- Timing's Jukebox selector defaults off. Audible nearby records can fade AFMR
  out and pause/resume the same song, end it and wait a full normal silence after
  the record ends, or end it and immediately start the next eligible song after
  the record ends without silence/buffer. Resume uses Fade in. Multiple records
  retain the hold until none are audible; muted records do not hold it. Manual
  pause owns an independent hold and death guards remain effective.
- Combat defaults to attacking or receiving source-attributed damage from hostile/
  neutral mobs and other players. Passive targets and environmental damage do not
  refresh it; a global broad-trigger option remains available. Attack/damage switches
  are independent. In singleplayer or companion-enabled multiplayer, once an event
  starts combat, actual targeting
  by loaded hostile/angered combat-capable mobs sustains it; losing the last target
  starts the quiet countdown. Targeting alone cannot start or revive combat.
  Servers without the companion retain the attack/damage timeout.
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
  effective dB adjustment. Vanilla music stays unchanged. Explicit scans run during active gameplay and
  multiplayer without a pause requirement.
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
