# AFMR: remaining deeper testing

The active private-release priorities are in
[the 1.0 checklist](docs/RELEASE-1.0-CHECKLIST.md). On October 23, Animal deferred
friend testing until after release; it is not a pre-release requirement. The list
below retains deeper cases and historical results, not a requirement to finish
every combination before 1.0. The author's reviewed text and representative
focused native review are complete. See the [1.0 verification](docs/testing/2026-10-23-release-1.0.md)
for current results; the deeper cases below remain available for later testing.

## Author-written copy workflow (Unreleased, both targets)

Both builds/source archives pass with **778 NeoForge / 788 Fabric JUnit tests**,
plus **20 copy compiler checks per target**. The author's edits are reviewed and
implemented, including removal of 26 cleared explanations. Windowless Minecraft
language probes reproduce all **150 / 148 applicable entries**, including empty
ones and dynamic placeholders. See [the author-copy review](docs/testing/2026-10-23-author-copy.md)
and [the initial workflow report](docs/testing/2026-10-23-ui-copy.md). This verifies
formatting and packaging, not on-screen rendering or physical input.

- [ ] Review the implemented author copy's changed tooltips and
  guidance line in an isolated game profile on both targets. Check wrapping,
  existing one-line truncation, narrow windows/GUI scales, dynamic values,
  keyboard focus and narration. Check cleared explanations leave no empty hover
  box while clipped-label tooltips remain available. Keep target-only differences accurate.

  The 1.0 pass inspected representative Timing/Combat/Inspector/Menus/widget
  screenshots on both targets and verifies actual author tooltip components and
  cleared explanations. Exhaustive wording review, physical focus/narration and
  extreme scales above remain open; they are not release blockers.

## Per-playlist silence (Unreleased, both targets)

Both builds pass **778 NeoForge / 788 Fabric tests**, with 31 focused cases each.
Packaged native runs pass **18 / 21 checks**, including a real three-second Skip
gap and native sound state. See [the report](docs/testing/2026-10-23-playlist-silence.md).

- [ ] Physical keyboard/narrator traversal and extreme GUI scales; a full ten-minute
  wall-clock gap; mixed live multiplayer/combat/death/jukebox transitions. Focused
  native checks use generated music, a normal gap and a scheduled ten-minute
  countdown. Pure scheduler tests cover range changes/guards without proving all
  corresponding native audio combinations.

## Copied diagnostic summary (Unreleased, both targets)

Both builds pass **747 NeoForge / 757 Fabric automated tests**, including 12 new
summary cases each. Privacy, bounded output, alias references, condition results,
title-screen behavior and indexed access with 70,000 tracks are covered. See
[the report](docs/testing/2026-10-23-diagnostic-summary.md).

- [x] Actual Minecraft clipboard copies pass on both targets for live playback,
  pause, override, an injected access-denied playback failure and retry, with
  success feedback, unchanged playback and private fixture details omitted.
  The test reads its own copied text through Minecraft's clipboard API rather
  than pasting it into a separate editor. See the 1.0 report.
- [ ] Repeat clipboard refresh during a long live scan and physical paste into
  another application; native clipboard denial/failure remains unforced.
- [ ] Physical keyboard navigation, smallest supported GUI size, resize/hide and
  reopen, title screen and actual clipboard failure handling on supported OSes.
  Initial feature validation was windowless; the later 1.0 pass exercised the
  actual game and replaced the system clipboard with generated diagnostic text.

## Settings organization (Unreleased, both targets)

October 23: both targets compile and assemble. Full regression runs pass
**746 / 747 NeoForge** and **756 / 757 Fabric** tests. Both fail the unrelated
`DiagnosticSummaryTest.manyDetailedGroupsKeepClipboardSizeBounded` assertion
expecting a "Report truncated" marker (line 114); this pass does not edit
diagnostics code or tests.

- [ ] In an isolated native profile, check the relocated **Library → Rescan on
  launch** switch, its saved state after restart and the adjacent Repair / missing
  control. Verify the compacted Timing controls and **Timing → Combat settings →
  Combat music** with mouse/keyboard input, retained timing drafts, folder paging
  and small window/GUI-scale layouts. This reorganization has no dedicated native
  UI run; automated regression results do not establish those visual/input checks.

  Superseded for basic controls by the 1.0 native review: Timing/Combat/Library
  routes, rescan toggle and restart persistence pass on both targets. The full
  automated suites also pass; the earlier unrelated truncation assertion was
  resolved in the diagnostic feature. Physical keyboard/folder-page extremes remain.

## Consolidated Widget tab (Unreleased, both targets)

Both builds pass **735 NeoForge / 745 Fabric tests**. Focused packaged game checks
pass **26 NeoForge / 25 Fabric cases**, including control retention, scrolling,
invalid drafts, picker/preview, style restrictions, layout bounds and menu/reset
independence. See [the report](docs/testing/2026-10-23-widget-settings.md).

- [ ] Physical input and narrator traversal, non-English keyboard layouts, extreme
  GUI scales/narrow windows, rapid resize/hide sequences and every mixed native/
  Hybrid/card preference. Automated native checks use a 854×480 game window and
  exercise a larger logical layout; they do not prove all accessibility paths.

## WebP and animated GIF backgrounds (Unreleased, both targets)

The complete suites pass **735 NeoForge / 745 Fabric tests**. Packaged native menu
checks pass **15 NeoForge / 18 Fabric cases** for WebP/GIF import, frame pixels,
fast-import control refresh, invalidation/reload, on/off,
replacement/removal and animation backup export. The windowless probes each pass
five cases including 100 large PNG native decode/close cycles. See
[the report](docs/testing/2026-10-23-background-formats.md).

- [ ] Physical Explorer selection/cancellation and upper-case extensions on each
  supported OS; real animated wallpapers with unusual palettes, ICC profiles,
  interlacing, damaged metadata, very rapid replacement and resource-pack reloads.
- [ ] Maximum 512-frame GPU upload latency and peak process/VRAM use; very slow
  disks, disk-full/permission failures during the two-format replacement, device
  loss and shutdown during preparation. The 64 MiB bound is retained frame pixels,
  not total process/driver memory; lossless WebP may require source-sized work data.
- [ ] Actual cross-version restart/restore of a GIF backup. The 1.0 pass now verifies
  a real reviewed GIF backup and restart restore independently on both targets,
  including all four uploaded textures. Cross-version GIF transfer remains open;
  earlier cross-version native backup tests and pure archive tests remain separate.

## Play immediately on join (Unreleased, both targets)

Both suites pass **718 NeoForge / 728 Fabric tests**. Actual disposable singleplayer
joins, bound-profile ordering, Timing UI and later normal Skip pass **10 NeoForge /
14 Fabric native checks**; Fabric also verifies real OpenAL playback. See
[the report](docs/testing/2026-10-23-play-on-join.md).

- [ ] Dedicated-server joins/reconnects, rapid server switches, real respawn and
  dimension transitions, resource reload while initially paused, and slow
  chunk/structure/duration availability. Unit checks cover policy and reset rules;
  the new native runs exercised singleplayer joins.
- [ ] Physical switch/keyboard input, unusual GUI scales/window sizes, very large
  profiles/libraries and live joins with jukebox/death/pause/time-fit combinations.
  The inspected layout and non-overlap check use 854×480.

## Singleplayer-only labels (Unreleased, both targets)

The wording update passes the existing **702 NeoForge / 712 Fabric tests** and
both builds. See [the label report](docs/testing/2026-10-23-singleplayer-labels.md).

- [ ] Visually check the visible Structures badge, title and unavailable message
  on an actual multiplayer server and at extreme GUI scales; exercise hover text
  on the disabled Add current structures button. No native run was added for the
  wording-only change.

## Full configuration transfer and list seams (Unreleased)

The complete suites pass **702 NeoForge / 712 Fabric tests**. Full export/review,
actual restart, transfers in both directions, 16 native seam cases and a packaged
Fabric framebuffer seam check pass. See the [backup report](docs/testing/2026-10-23-full-backup.md)
and [seam report](docs/testing/2026-10-22-menu-seams.md).

- [ ] Physical Explorer Open/Save/cancellation with `.afmrb`, drag/drop and keyboard
  review navigation; unusual keyboard layouts, modified/unsupported shortcuts and
  title-screen/hidden-menu lifecycle during transfer.
- [ ] Very large complete backups and peak memory/latency, disk-full/permission and
  actual process/power-loss fault injection at each install step. Tests simulate an
  interrupted journal and verify rollback/preflight preservation; they do not cut
  power or exhaust the real disk.
- [ ] Restore an enabled background and observe texture upload after restart;
  moved music/world folders, missing vanilla/modded resource IDs and every edited
  profile/condition combination across versions. Current transfer checks cover
  managed PNG bytes, all library/profile data and old-image removal.
- [ ] Physical window resize/GUI scale changes, other GPUs, third-party render/
  shader/UI mods and every image/opacity setting. Native reflow covers four canvas
  sizes and opacity 50/100%; Fabric framebuffer sampling covers 854×480 at 100%.

## Visual color pickers (Unreleased, both targets)

NeoForge passes **691 automated tests** and **25 focused native picker checks**.
Fabric passes **701 automated tests** and **75 packaged smoke checks**, including
24 new picker assertions. Seven new color-state tests per target cover RGB/HSV
conversion, independent JDK comparison, grayscale hue retention and clamping.
See [the picker report](docs/testing/2026-10-22-color-pickers.md).

- [ ] Physical mouse/keyboard and assistive input, very small/unusual GUI scales,
  resize with incomplete hex, rapid hide/reopen and disconnect while previewing,
  third-party rendering/UI mods and repeated preset/drag sessions.
- [ ] Native restart with a chosen picker color and every opacity/custom-image
  combination. Existing storage tests cover settings persistence; these picker
  runs verify interaction/rendering and leave deeper lifecycle combinations open.

## Fabric cover detection (Unreleased)

The detector now uses the heightmap Minecraft 1.20.1 actually sends to clients.
Ten new windowless regressions exercise real block sections/client heightmap
initialization, roof/foliage/water cases, cache replacement and playlist selection.
See [the cover report](docs/testing/2026-10-22-fabric-cover.md).

- [ ] Recheck the reported snowy-plains cave in the user's modpack with the rebuilt
  Fabric JAR. Inspector should report Covered and the actual depth, then select
  Dark exploration at the configured light level/priority. Check nearby open sky,
  cave entrances, leaves/water overhead, teleports, chunk reloads and multiplayer.
  Windowless tests do not validate that modpack's live chunk/light behavior or audio.

## Menu color customization (Unreleased, both targets)

NeoForge passes **684 automated tests** and Fabric passes **694**, including
eight new palette/parser/snapshot checks each. The packaged NeoForge build passes
**29 focused native color checks**; Fabric passes **51 packaged smoke checks**,
including eleven color/reflow assertions. Editor/workspace screenshots were inspected. See
[the color report](docs/testing/2026-10-22-menu-colors.md).

- [ ] Physical RGB entry, keyboard-only navigation, resizing while drafts are
  invalid, unusual GUI scales/aspect ratios, very light/dark or low-contrast user
  palettes, and interaction with third-party rendering/UI mods.
- [ ] Exercise every color family with custom image opacity and blur combinations,
  restart the game with a custom palette and switch real world/server profiles.
  Component tests verify disk snapshots; this run does not measure every screen,
  background-image combination or native widget skin.
  A simulated native screen-height reflow verified that drafts remain attached
  to the correct color family and do not change palette values.

## Custom menu backgrounds (Unreleased, both targets)

Both targets pass **676 automated tests**, including **15 new layout/image/store
cases each**. Windowless native-decoder checks and packaging results are recorded
in [the background report](docs/testing/2026-10-22-custom-background.md).

- [ ] In a disposable native profile, choose/cancel PNG/JPEG via Explorer; confirm
  it works during a library scan/repair and after navigating away from the picker.
  Check long/Unicode filenames, invalid files and permission failures.
- [ ] Visually confirm 26.3 draws the image behind blur (blur On/Off and Minecraft
  blur strength 0/nonzero), with panel opacity 0/80/100%. Check title-screen panorama
  and in-world views, both workspace and child settings. Fabric should remain sharp.
- [ ] Fill/Fit/Stretch, image opacity, PNG transparency, portrait/ultrawide images,
  resizing/GUI scales, hide/reopen, disable/re-enable, reset/remove, resource reload,
  restart and profile changes through real controls. Original images remain intact.
- [ ] Repeat imports/reloads/removal with GPU texture observation and screenshot/
  render mods; shutdown or navigate away mid-import. Headless native decode tests
  cannot validate real GPU lifetime, dialog behavior or rendered blur pixels.

## Future Minecraft ports

- [ ] Before validating any future NeoForge/Forge backport, inspect its Minecraft
  `NativeImage.read(byte[])` implementation for the native scratch-stack limit.
  Use stream/native-heap loading on affected versions for both HUD and editor
  artwork, retaining texture/image cleanup. Repeat the generated cover larger
  than 64 KiB, repeated decode/close and invalid-image checks on that target's
  actual runtime, plus in-game replacement/reload checks. Do not infer safety
  from the mod loader or Minecraft 26.3's fallback. Reference the
  [1.20.1 crash report and probe](docs/testing/2026-10-22-fabric-artwork-memory.md).

## Fabric artwork stack-space crash

The supplied Homestead log and actual triggering cover reproduce
`OutOfMemoryError: Out of stack space` in Minecraft 1.20.1's byte-array image API.
The Fabric HUD/editor now use the native-heap stream decoder. **661 automated
tests pass**, plus **9 windowless native-image checks on each of Java 25 and the
user's Java 17 runtime**: old failure reproduction, the real cover decoding,
1,000 large image load/close cycles and 100 invalid-input failures with stable
scratch usage. See the [report](docs/testing/2026-10-22-fabric-artwork-memory.md).

- [x] User-reported Homestead endurance session, October 23: a few hours of smooth,
  stable play with no reported crash. Memory was not monitored; exact JAR and
  individual feature combinations were not recorded.
- [ ] Remaining focused lifecycle checks: repeated HUD/editor artwork changes
  and screen closure, texture registration failure/resource reload,
  rapid selection/skip, and sound/render mod interactions. The windowless probe
  validates native image decoding/cleanup, not full modpack GPU texture lifetime
  or the absence of every possible heap leak. Instrumented memory/VRAM observation
  remains unmeasured; the successful user session is separate evidence.

## Optional song-list sharing (Unreleased, both targets)

Both maintained targets pass **661 automated tests**, including **22 new song-list
cases each**. These are component/build results, not new native UI/audio evidence.
See the [song-sharing report](docs/testing/2026-10-22-song-sharing.md).

- [ ] Actual Include song list / Import songs controls and disabled/no-song cases;
  resize, hide/reopen, root selection and long/Unicode names across GUI scales.
- [ ] Explorer folder selection/cancellation; automatic unique-root matches and
  ambiguous names; partial imports automatically opening Repair / missing; folder
  and individual renamed-file repair in the real UI, with native playback after repair.
- [ ] Export in one maintained target and import in the other through real screens;
  settings-only Off behavior, explicit Minecraft exclusions and unavailable resource
  packs, inactive profile targets, cancellation/profile changes during matching/pickers.
- [ ] A full large-library native import, slow/unavailable drives and permissions,
  external changes after preview, resource reload, shutdown mid-match, queue/history
  coexistence and a native restart of repaired imported references.

## Fabric 1.20.1 backport

The independent port passes **639 automated tests** and **40 disposable native
checks**, including testing the installed remapped JAR without loose AFMR classes
or external decoder JARs. These results are separate from the NeoForge counts
below. See [the port report](docs/testing/2026-10-22-fabric-1.20.1.md).

Remaining: physical M/key rebinding and scaled pointer/tooltip edge cases; actual
Explorer Open/Save/folder dialogs and cancellation; compressed-format native audio
quality, device changes and resource reload; deeper jukebox/combat/structure
interactions; 1.20.1 multiplayer and sound/render-mod compatibility; long sessions,
large libraries and uncommon drives/paths. Native tests ran on the development
JDK 25; Java 17 bytecode/API compatibility is checked by compilation and JAR
inspection, without a separate Java 17 game run yet. Existing core regression
coverage does not establish native results for those cases.

## NeoForge 26.3 validation

October 22 snapshot: **0.6**, with **639 passing automated tests** and **179 earlier passing
isolated workspace screen/control checks**. The combined Unreleased work now also passes **83 isolated integration checks and 12 real-restart checks**; see [the combined report](docs/testing/2026-10-22-release-validation.md). Historical 0.5.2 baseline: **227 passing
tests**. On October 21, 2026, the user authorized native/UI testing; **81 native
playback checks and 6 persistence checks were validated** against that baseline,
plus the UI checks below. No AFMR defect was reproduced in that session. Those
results do not validate every feature added since then.

See the [session report](docs/testing/2026-10-21.md) for methods, results, test-fixture corrections and performance limits. The current release-validation session is recorded separately below.

**Earlier hover-help run:** 331 automated tests passed. The extended workspace helper
passed 58 checks, including rendered tooltip bounds/anchoring at two scaled
workspace edges, a timing field and a disabled display button. Screenshots were
visually inspected. See [the hover-help report](docs/testing/2026-10-21-hover-help.md).

**Later multiplayer update:** The current source passed **27 native checks against
an unmodified vanilla server**, including paused disconnect/reconnect, server
damage/death/respawn, synchronized environment rules and normalization waiting online
then completing after logout. The session-state cleanup fix and exact scope are in
the [multiplayer report](docs/testing/2026-10-21-multiplayer.md). These results supplement
the historical baseline above; they do not validate every remaining UI/hardware case.

## Completed in the October 21 session

- [x] Native local/vanilla pause position retention, same-source resume, delayed stream start, frozen fade gains and silent gap, queued Next/override, preview paused at stream startup, global sound resume, actual Minecraft pause-menu interaction, other ambient audio, reload/restart reset.
- [x] Next and Inspector controls; both override modes with Automatic off; natural one-song return; empty/deleted/all-disabled targets; physical controls and chat/menu guards.
- [x] All four entry/exit combinations, source ownership through A-B-C, returning to the retained source; two four-song shuffle cycles and boundary repeat avoidance.
- [x] Live day/night, enclosed darkness, lit Underground and changed Y cutoff, captured Home, Nether/End toggles, and combined exact biome/time/dimension/Y/temperature/cover custom rules.
- [x] Combat trigger API and forced Combat with global combat off; death Cut, Fade and Finish, paused death fade, new-music blocking while dead and respawn.
- [x] Seven packaged audio-format variants, intentional asynchronous decoder failure and Retry failed, 70,000 unrelated library entries with indexed playback resolution.
- [x] Native Explorer pickers, 5,000-file scan with progress and zero reported issues, generated .fplite import into a manual playlist with duplicate/missing/reuse report, source checksum preservation.
- [x] UI playlist creation, condition pages/current biome, priority, separate entry/exit switches, reset despite invalid text, unchanged unrelated saved configuration, and persistence of these choices/scanned entries/Next-Pause keys.

## Completed in the combined October 22 release pass

- [x] **Focused release pass:** 409 passing live/helper checks and 639 passing
  automated tests. Final startup logging is rebuilt and verified; 39 actual
  vanilla-server checks include linked profile reconnect, dimension stability,
  unlink/Default fallback and independent volume. JAR/source packaging checks and
  sanitized evidence are recorded in the
  [combined report](docs/testing/2026-10-22-release-validation.md).

## Remaining complex cases

- [ ] **`.afmrp` playlist sharing — remaining combinations:** Basic selection,
  export/import, preview/cancel, top insertion, settings/empty-song retention,
  location rebinding and restart pass natively. Actual Explorer Save/Open and
  Save cancellation pass. Still check overwrite/extension behavior in Explorer,
  read/write permission failures, single/mixed file drops, Unicode/long names,
  very large setups and unusual GUI scales. Change profiles/worlds during picker
  or review, use inactive/deleted destinations and missing anchors, exercise all
  insertion modes, hide/reopen, title-screen imports and the configured-location
  guard outside a world. Native nested-group/modded-ID and audible rule changes
  remain separate from the passing component tests. See the
  [combined report](docs/testing/2026-10-22-release-validation.md).

- [ ] **Library repair — remaining combinations:** Generated folder moves,
  individual renames, missing reports, preview/Cancel/Apply, stable IDs/rescan,
  inactive assignments, byte preservation, repaired native playback and restart
  pass. Still exercise the specific Explorer repair folder/file callbacks,
  toast/count/search/hover with large lists, nested same-name/partial matches,
  duplicate merges, loudness reuse, queue/history remapping and affected versus
  unrelated playing songs with pause/jukebox/overrides. Check profile changes,
  stale plans, external changes, zero matches, already linked roots, shutdown
  during work, offline/permission/UNC/Unicode paths and directory aliases/links.
  These extremes remain unvalidated on actual devices; component tests cover
  their core logic. See the [combined report](docs/testing/2026-10-22-release-validation.md).

- [ ] **Playlist volume and profiles — remaining combinations:** Native volume
  slider/mute/reset and actual OpenAL gain, independent copied settings, Use now
  state cleanup, exact singleplayer binding, originating-profile import, restart
  and final shutdown flush pass. Still compare audible local/vanilla/ReplayGain
  volume, source retention through fades/queue/jukebox/death and physical dragging.
  Exercise fresh/copy/rename/delete/unlink across GUI scales and title-screen
  editing; join identically named worlds in different folders and server addresses
  differing by port. Actual server reconnect, dimension stability, unlink/Default
  fallback and display-name independence now pass. Change/delete destinations during Explorer/foobar imports and verify
  every profile's launch/manual folder routing. See the
  [combined report](docs/testing/2026-10-22-release-validation.md).

- [ ] **Queue/history — remaining combinations:** Native queue actions,
  reordering/removal/clear, natural FIFO completion with normal silence, held
  override/Use queue next, automatic-off sources, song-level exclusion, native
  pause/resume, history replay/requeue/destination access, restart and Clear pass.
  Still use physical multi-selection in sorted order, duplicates/vanilla/missing
  songs/deleted sources, history tooltips/destination assignment, contextual
  transitions/time fitting/Play on trigger and one-song overrides together.
  Exercise jukebox policies, death/respawn, resource reload, the full 100-entry
  bound, malformed-history recovery/writer failures and extreme GUI scales.
  Native restart currently verifies several actual starts, not a full 100-song
  session. See the [combined report](docs/testing/2026-10-22-release-validation.md).

- [ ] **Optional song columns (Unreleased):** 18 new metadata/ordering/storage/
  geometry tests pass, alongside the 179-check isolated workspace run. Native
  synthetic events checked all choices, hidden-page retention, cached cells,
  numeric sorting, horizontal wheel/header alignment, scrollbar dragging, pinned
  assignment controls, global choices and restoring defaults. Generated tagged
  MP3/FLAC/OGG/WAV imports were verified by component tests, not by a native import
  dialog. Still exercise physical mouse/keyboard input, unusual GUI scales,
  long/Unicode tags, large selected playlists, and rescan/loudness completion while
  the table is open. See [the report](docs/testing/2026-10-22-song-columns.md).

- [ ] **Independent fades and jukebox (Unreleased):** 23 new scheduler/storage
  tests and the 162-check workspace run passed. Actual nearby record insertion
  and removal exercised all three policies, overlapping records and muted Records
  volume; Timing saved asymmetric fade values. The helper checked GameAudio pause
  intent and retained playback sequence, not measured OpenAL sample offsets or
  subjective audio quality. Still measure native gain/position across jukebox
  holds, manual pause and queued controls; natural record completion, movement
  across audible range, chunk/dimension/disconnect lifecycle, sound/resource-pack
  reload, dedicated servers, unusual GUI scales and third-party sound mods.
  Live mob AI remains separately deferred below. See
  [the report](docs/testing/2026-10-22-fades-jukebox.md).

- [ ] **Singleplayer combat targeting (Unreleased):** In an isolated world, let
  a zombie target the player without attacking; combat must remain peaceful. Start
  a fight with an enabled attack or received hit, then block further hits while
  the mob targets the player beyond the quiet period. Verify retention, multiple
  targets, neutral-mob provocation, target loss/death/unload, full quiet countdown,
  reacquisition during that countdown and no restart after expiry. Exercise
  death/respawn, dimension changes, pause/resume, global off, logout and another
  world; ensure no stale hold. Repeat with a very large loaded mob population to
  measure bounded-scan latency and server cost. On a vanilla server verify the
  original event timeout and independent Players only timer. The 18 new tracker
  tests pass; they do not validate live server events, mob AI or native audio.
  See [the report](docs/testing/2026-10-22-combat-targeting.md).

- [ ] **Gamemode and Play on trigger (0.6):** Integrated-server changes through
  all four modes, editor drafts/selections, trigger fade/no entry gap and manual
  Skip passed in the combined 135-check run. Physical input, unusual GUI scales,
  dedicated-server mode updates/reconnects, rapid live boundary changes and
  protected/death/paused transitions with real native audio remain deeper cases.
  See [the combined report](docs/testing/2026-10-22-release-0.6.md).

- [ ] **Fit songs to time window (0.6):** In the isolated profile, toggle
  Time's fit option with draft ranges/presets, resize/hide/reopen/save, and test
  known short/long generated tracks near a natural window end. Verify silence with
  no fitting tracks, priority retention, wrapped/overlapping/All/Any/Not windows,
  unknown local/vanilla duration discovery, resource-pack reloads, persisted local
  lengths, disabled/failed songs, Next versus overrides/previews, pause/resume,
  pending gaps and independent entry/exit/Continue Playing on Match behavior.
  Confirm no synchronous file reads or native audio regressions and measure the
  background metadata/header-read impact. Manual time jumps are outside prediction.
  Automated scheduler/header tests do not establish native UI/audio timing.
  The combined 0.6 run subsequently verified the basic Time toggle/save, generated
  WAV duration discovery, fitting playback, no-fit priority/silence and wider-window
  recovery. Those checks use a paused disposable clock; the deeper cases above remain.

- [ ] **Held items (0.6):** In the isolated profile, search fishing rods by
  name/ID, add current held items, select multiple types, and save/reopen. Swap
  hotbar items, move a rod between main/off-hand, empty both hands, cast/reel, and
  use renamed/enchanted/damaged rods. Check hand selection and nested All/Any/Not
  groups, priority/fades/finish behavior, hidden-menu drafts, resize/GUI scales,
  persistence and modded IDs. Repeat item switching on an unmodified server and
  after disconnect/reconnect. Automated conditions/storage/playback tests do not
  establish live inventory observation, input, layout, or native audio transitions.

- [ ] **Menu transparency (0.6):** 373 automated tests and 105 isolated
  in-game workspace checks passed. Native screen events exercise the live opacity
  slider, blur toggle/reset, alpha at 0/50/100%, opaque text, widget independence,
  and non-pausing behavior. Screenshots confirm the corrected 80% workspace and
  blur on/off. Physical dragging, unusual GUI scales/window shapes and third-party
  shader/rendering combinations remain unmeasured. See
  [the report](docs/testing/2026-10-21-menu-transparency.md).

- [ ] **Editable templates and UI follow-ups (0.6):** The shared source
  passed 352 automated tests and 80 isolated in-game checks. Default/group editing,
  cloning, autoplay hold/release/pointer-exit/navigation cancellation, completed
  playlist-local replacement, artwork clearing through silence/no-cover tracks,
  the corner selector and native popup anchors were checked. Screenshots cover
  the hold bar, footer states, four AFMR corners, Hybrid top-right and native
  bottom-right. Still exercise physical mouse/keyboard holding, keyboard focus
  cancellation, extreme group trees and resizing/scaling, plus live server/audio
  transitions with edited templates. See [the report](docs/testing/2026-10-21-editable-templates.md).

- [ ] **Source-qualified combat, PvP and health (0.6):** In an isolated
  profile, test attacking and receiving hits from players, hostile mobs and neutral
  mobs (including wolves, bees, llamas, goats and iron golems), passive animals and
  environmental damage. Check the broad-trigger toggle and independent attack/damage
  switches. Test arrows/tridents and other owned projectiles, self damage, unknown/
  unloaded attackers, death/respawn and disconnect. Fully blocked shield events do
  not provide the damage-source packet used for detection. Verify Players only
  expires after the last player event even if mobs keep attacking, including PvP
  playlist priority, Any/All/Not groups and combat death policies. Verify red-heart
  thresholds with fractional values, equality, healing, absorption and extra maximum
  health; inspect menu drafts/tooltips, save/reopen and Inspector at several GUI scales.
  The automated predicates/timers/storage/scheduler checks and helper compilation
  do not prove native attacker attribution, UI input or audio behavior. No desktop
  interaction has been performed for this feature.

- [ ] **Workspace physical input and extremes (0.5.7):** The new helper passed
  48 actual-screen checks using synthetic mouse events, including normal library
  assignment/folders, drafts, navigation, independent switches, overrides/transport,
  all slot pages, import destinations and create/delete. Still exercise physical
  M/rebound keys and repeats, Ctrl/Shift/Space with the three lists, scrollbar
  dragging, many playlists, native Explorer file/folder/.fplite dialogs/cancellation,
  import progress and reports while switching pages, and resizing/GUI scale during
  retained drafts. Compare current/inspected artwork and long metadata, current
  vanilla applicability, and pending scans at title/in-world transitions. The geometry
  tests span small/large/tall windows; rendered native QA does not cover every
  scale/window combination. See [the report](docs/testing/2026-10-21-workspace.md).

- [ ] **Menu toggle and retained place (0.5.6):** In an isolated profile, use M
  to hide/reopen the editor and settings/condition subpages. Check playlist/tab
  page, both searches, expanded folders, multi-selection, sort and both scroll
  positions; repeat using main-editor Done/Escape and Mods → Config. Verify M
  types normally in focused fields, draft settings survive hiding (including
  invalid text), Save & back still validates, and rebound keyboard/modifier keys
  toggle correctly. Hold M through key repeat, resize/change GUI scale while
  hidden, finish a scan while hidden, and disconnect/rejoin; no immediate reopen,
  stale return screen or old-world draft should survive. Build/regression tests
  do not establish physical input or live menu state.

- [ ] **AFMR appearance controls (0.5.5):** In an isolated profile, check the
  Appearance tab and preview at small/large GUI scales. Verify black/no accent
  bar by default, arbitrary RGB colors and presets, opacity 0/35/92/100%, invalid
  color/opacity input and reset recovery. Confirm text/artwork/notes remain visible
  at zero panel opacity, popup fades still apply, and Hybrid/native styling is
  unaffected. Save/restart and verify appearance persistence. Both custom widgets
  must omit the Now playing heading, including while scrolling and in preview.
  Switch single-line/word-wrap layouts in Hybrid and AFMR; exercise long multiword
  titles, artists/albums/playlists, Unicode, and overwide single words with scrolling
  both on and off. Check whole-word boundaries, fixed width, added height, artwork
  alignment, hotbar clearance and small-viewport fitting, then save/restart.
  Automated storage/normalization/metadata/wrapping tests do not establish native rendering/input.

- [ ] **Hybrid toast / custom widget controls (0.5.4):** In an isolated profile,
  switch all three styles during local/vanilla playback and preview. Confirm the
  native toast is suppressed in Hybrid/AFMR; compare Hybrid's frame/position with
  Minecraft, and check artwork with embedded/folder art, missing/corrupt art,
  rapid skips, style changes, disconnect and resource reload. Try 50/100/200% scale,
  widths 140/260/600, short/tall viewports and GUI scales; verify the AFMR hotbar
  clearance and preview. Toggle every information field, including artwork-only,
  text-only and all-off; test empty metadata, unknown length, translated vanilla
  titles, Unicode/slashes and long artist/album/playlist names. Check original
  source ownership during retained playback. Toggle scrolling and observe endpoint
  pauses, clipping, duration, restart/pause behavior and ellipsis fallback. Save,
  restart, reopen all tabs, and verify numeric validation and preferences.
  Automated layout/text/storage checks do not establish native rendering or input.

- [ ] **Native music settings and selectable widgets (0.6):** In an isolated
  profile, switch Minecraft / AFMR styles during local, vanilla, and preview playback.
  Check native Never / Pause menu / Pause menu and toast, notification duration,
  native translated titles, literal local titles with slashes/Unicode, rapid skips,
  same-track restarts, muted-then-unmuted playback, F1, and small/large GUI scales.
  Confirm only one style appears, the AFMR card still loads artwork/duration, and
  changing style or logging out/reloading discards pending art and stale song names.
  Verify Music Frequency presets via the vanilla menu, pending and manually paused
  gaps, uninterrupted playing songs, saved settings after restart, custom timing
  preservation on startup, and ordinary title-screen vanilla music. Compilation and
  scheduler/storage tests do not prove mixin application or live toast/UI behavior.

- [ ] **Continue Playing on Match (0.6):** In the isolated profile, enable
  the global switch under Settings → More settings, save/reopen, and check the
  button/tooltip at small and large GUI scales. Listen through Day → Night with a
  shared generated track, then a third playlist without it; confirm uninterrupted
  position/gain for a match, buffered normal fade for a nonmatch, and normal silence
  after natural completion. Exercise both override modes during retention and
  verify Inspector's explanation. Automated scheduler/storage tests do not prove
  live UI input or native audio continuity. Desktop access has not been requested
  or used for this feature.

- [ ] **New weather / Match any options (0.6):** In an isolated profile,
  test clear/rain/thunder transitions in singleplayer and on an unmodified server,
  including snowy/high-altitude and dry biomes, roofs, Nether/End, and world changes.
  Any weather includes storms/rain/snow but excludes clear. Verify the weather
  selector and condition-mode button at small/large GUI scales, saved settings,
  ALL/ANY combinations with biome/location/structures/weather, disabled filters,
  and Match any activation through another filter when structure data is unavailable.
  Check Combat OR Weather playback started peacefully versus during combat, together
  with death policy. Automated predicates/storage/scheduler tests cannot establish
  live weather synchronization, UI input, or native transition/audio behavior.

- [ ] **New structure filter (0.6):** In the isolated singleplayer profile,
  search village variants and modded IDs, toggle several selections, use Add current
  structures, and save/reopen. Walk across an actual generated village's full bounding
  box, then test radius 0 and 32, including vertical and diagonal boundaries. Check
  chunk changes, teleportation, dimension changes, world switches, and newly loaded
  starts; confirm no chunks are loaded/generated by observation. Inspect small/large
  GUI scales and invalid radius/empty selection errors. On an unmodified server,
  verify unavailable-data messaging, fallback playlists and manual overrides; after
  singleplayer logout, no structure observations may survive into multiplayer.
  Automated geometry/persistence/scheduler tests do not validate native discovery or UI.

- [ ] **New loudness scanner (0.6):** In the isolated profile, verify the
  Normalize playlist button/progress/results at small and large GUI scales, signed
  dB rows and peak-limit details, Cancel and reopening reports. Confirm singleplayer
  pauses while scanning, resuming gameplay suspends analysis, and completed measurements
  persist after cancellation/restart. Multiplayer waiting/resume now passes separately.
  Listen
  to normalized generated quiet/loud fixtures through native audio, verify the
  normalization switch and next-song timing, and verify vanilla gain is unchanged.
  Benchmark actual gameplay impact before enabling scans during active play. The
  [automated/component checks](docs/testing/2026-10-21-loudness.md) do not establish
  these native/UI behaviors.

These are additional scenarios, not failures found in the session. Pure tests cover many of their scheduler/storage/filter rules; the items below retain the deeper native/hardware combinations.

- [ ] **Transition and paused-context combinations:** Pause during a live context buffer while moving between environments; verify inspector selection and remaining buffer together. Exercise queued controls with repeated context changes, A-B-C where B's exit cannot protect A's song, no matching playlist, and manual skip/death interrupting an existing transition. Explicit paused disconnect/reconnect now passes on a vanilla server; reload and restart were already checked.
- [ ] **Override mutations and every slot:** Turn Automatic off during an active held/one-song override, release/cancel mid-fade, retrigger/switch slots rapidly, and remove the last available song during decoding. Exercise all eight physical slots, later binding pages, missing bindings, Return to automatic, and actual Stream Deck hardware input.
- [ ] **Live shuffle membership changes:** Add/disable/remove songs partway through a cycle; shared songs across playlists; leave/return continuity; one-song pools; failed files repaired on disk and retried. Check those mutations together with overrides and explicit previews.
- [ ] **Combat input variants:** Actual attack-key interaction against entities and real damage/absorption/hurt-time triggers; attack/damage switches independently; custom combat-driven manual playlists; new-music-while-dead enabled; death during an existing fade or partially completed fade-in.
- [ ] **Condition boundaries/worlds:** Wrapped custom time ranges, inclusive negative/exact Y and temperature edges, water boundaries, conflicting filters, Home world/dimension/radius boundaries, modded biome registration/offline IDs and modded multiplayer synchronization. Vanilla multiplayer synchronization of the combined custom rule, water state, Home, and both dimension darkness switches now passes.
- [ ] **Full library and audio lifecycle:** Longer sessions with the user's real library and unusually tagged files, many songs assigned to active playlists, import/rescan during gameplay, launch rescans and inaccessible folders, sound-device changes, volume interactions and subjective ReplayGain comparisons. Existing ReplayGain decoding/tag regressions pass. The 70k check measured only indexed scheduling, not overall FPS.
- [ ] **UI/import extremes:** Resize/GUI-scale combinations beyond the tested small window, long names/tooltips, many custom playlists, later keybind pages, repeated .fplite import preserving disabled assignments, cancelled/native picker edge cases. Basic picker, scan, import reporting, custom conditions and reset/persistence already pass.

Continue using the isolated workspace profile and generated fixtures for state-changing checks. Personal music and foobar files remain read-only. The reusable test-only helper is tracked under `testing/native`; full session logs and generated fixtures remain in ignored `work/overnight`. The helper is not included in the release mod.

## Earlier user report

The user reported that most manual checks of 0.5.0 worked to expectations, without itemizing cases. The October 21 results above provide the later concrete checks.
