# Deferred features

The completed feature sections below shipped together in **1.1** on October 28,
2026. Earlier verification/version statements record their development checkpoints.
Open future requests and deeper testing remain deferred.

## Transport icons and playlist row menus

Implemented on both targets: primary Play/Pause/Resume and immediate-next icons,
labelled Skip & wait, hover/focus playlist actions, and Shift+F10. Both full builds pass; packaged clients each pass
49 native checks. See [the report](docs/testing/2026-10-25-transport-icons.md).
Version remains 1.0.1.

## Playback navigation pass

Implemented on both targets: seek bar, click-to-locate the playing song,
selection counts, settings search and consistent checkbox/detail controls.
Validation: 1,009 / 1,019 regression tests plus component checks and 51 native
checks per target pass. See [the report](docs/testing/2026-10-25-playback-navigation.md).
Version remains 1.0.1.

## Player clarity pass

Implemented under Unreleased on both targets: playing-source/song markers,
hover/focus song actions, readable rule summaries, concise waiting reasons and
countdowns, and isolated theme drafts with Apply / Cancel. Full builds pass
989/999 tests and packaged native runs pass 23 checks per target. See
[the report](docs/testing/2026-10-25-player-clarity.md). Version stays 1.0.1.

## Themes menu

Implemented under Unreleased on both maintained targets: named custom themes,
editing, rename/delete, and shared .afmrt files. Includes menu colors/opacity and
widget background color/opacity, with independent snapshots and full-backup
preservation. Fonts, images, blur, layout and music stay separate. See
[the report](docs/testing/2026-10-25-themes.md) for build-only verification.

## Playlist workspace follow-up

Implemented under Unreleased on both maintained targets: playlist-only session
Undo/Redo, drag priority, song context actions, active-rule filtering, opaque
popups, stable navigation/header geometry and the Fabric background-image fix.
Menu and widget independently use imported TTF or resource-pack fonts; full
backups include imported assets. Minecraft mute shows a warning while playback
continues silently; it does not pause/resume AFMR. The original mute-pause policy
was removed on October 25. Earlier verification is recorded in
[the follow-up report](docs/testing/2026-10-24-ui-followup.md). Version stays 1.0.1.

## Playlist import destination and replacement

Implemented on both maintained targets under Unreleased: default to an imported-only
new profile, with editable name and no automatic activation/binding. Existing-profile
mode replaces matching names while preserving identity/priority; settings-only keeps
songs and Import songs replaces assignments. Ambiguous duplicates are rejected.

## Content-based song sharing and repair

Implemented under Unreleased on both maintained targets: newly exported song
lists carry whole-file content identities; import offers linked-library searches,
additional folder searches and individual file choices with cancellation.
Fingerprinted repair supports reorganized/renamed copies and preserves existing
destination folder settings. Exact byte copies match; retagged/transcoded copies
and references without recorded identities are not automatically guessed.
See [the report](docs/testing/2026-10-24-content-matching.md).

## Riding animal

Implemented under Unreleased on both maintained targets: Any/Riding/Not riding,
using the player's direct Animal mount. Existing condition groups, transitions,
profiles, sharing and backups apply. Live mount/network checks are tracked in
[deferred testing](DEFERRED-TESTING.md).

## Player proximity volume

Implemented under Unreleased on both maintained targets: optional near/far
volume endpoints inside Nearby player, reversal and a separate trigger radius.
Native/automated verification is recorded in deferred testing and the feature report.

## Custom widget background

Implemented on both maintained targets under Unreleased. Display → Widget →
Custom background… imports an independent image for the AFMR card, including
GIF animation, sizing, opacity, preview and removal. Full configuration backups
include the asset. Native rendering/input checks are tracked in deferred testing.

## Playlist transition exceptions

Implemented on both maintained targets; 24 focused tests and 17 native checks per
target pass. See [the report](docs/testing/2026-10-23-playlist-transitions.md). Each automatic
source/destination pair can override finish/interruption, context buffer, fade-out,
silence range and fade-in, with inheritance per option. Rules follow actual song
ownership, remain profile-local and travel between jointly exported playlists.
The current development JAR remains 1.0.1 at the user's request; existing release
archives/tag are retained. More complex live combinations belong in deferred testing.

## 1.0.1: song groups and player themes

Implemented on both maintained targets; verification/release status is in the
current changelog and test report. Accepted decisions on October 23:

- Songs before silence has a global default and per-playlist override. Default one;
  switches start a fresh group. Only naturally completed scheduled/held-override
  tracks continue a group; Skip/Next, errors, previews and queues retain their
  explicit behavior. Count is 1-1000, kept with playback profiles.
- Add playing to playlist adds an assignment without removing the original or
  moving files. Captures the song when the destination picker opens and preserves
  an existing disabled assignment. Available in the workspace and Inspector.
- Now-playing delay is shared appearance, 0-60 seconds (default zero). Count only
  actual unpaused audio playback; keep the full ordinary display duration after
  appearing. All supported styles, including native toast metadata, obey it.
- Each playlist retains its Fisher–Yates cursor across switches. Avoid its own
  last drawn song at a new cycle even after another playlist played. State remains
  session-only and resets with the existing world/profile/resource lifecycle.
- Nearby player is an ordinary nested condition. Enter a name or pick an online
  profile, retaining a known UUID. Radius 1-1024 blocks (default 32); match only
  live, client-tracked players in the current dimension, excluding yourself.
  Existing entry/exit controls govern transitions. No server mod or remote lookup.
  Optional observation is cached at four updates/second and cleared on logout;
  no position snapshots are built when no enabled condition needs them.
- Playlist exports/full backups retain selected player names and UUIDs as settings;
  copied diagnostics omit those identities. Importers can reselect a player.

Game-launch shuffle persistence, separate queue grouping policy, remote player
tracking and network lookup are outside this update.

## Private 1.0 release

The [release checklist](docs/RELEASE-1.0-CHECKLIST.md) records the October 23 plan.
Friend testing is deferred until after release. Animal's text, the representative
focused pass and development are complete; the source is frozen at 1.0. See the
checklist and release verification for numbered artifacts and delivery status.

## Before 1.0: author-written UI copy

The authoring workflow is implemented in [UI-COPY.md](docs/UI-COPY.md): 156
editable passages organized by screen/control, with reference behavior and
explicit target differences. Both builds compile this one Markdown source into
Minecraft language entries. Animal's writing pass is reviewed and applied on
both targets, including removal of the 26 cleared explanations. Empty paragraphs
now disable their explanation while preserving utility labels and clipped-label
tooltips. The representative final in-game wording/layout review passes on both
targets; physical accessibility and extreme layout cases remain deferred.

## Implemented in the current source

Optional per-playlist silence ranges are implemented under Unreleased on both
targets, with elapsed silence retained when the effective playlist changes.

Display settings now consolidates widget controls into one **Widget** tab beside
**Menus**, with a scrollable panel and retained settings/drafts on both targets.

Static WebP and animated GIF custom backgrounds are implemented on both targets
under Unreleased. GIFs loop with frame timing/compositing, resize to a bounded
frame budget, and travel in full configuration backups.

Timing's **Play immediately on join** is implemented under Unreleased on both
maintained targets, with a per-profile setting and one-time initial gap/buffer
skip. It retains normal playback guards and later song timing.

Full `.afmrb` configuration export/import is implemented on both targets under
Unreleased, including profiles/playlists/library references, physical AFMR keys,
history and the managed background. Review precedes replacement on restart;
transfers in both directions pass. Visual color pickers and the reported list
seams are also implemented and checked. Remaining extremes are in deferred testing.

Playlist setup import/export is implemented under Unreleased. `.afmrp` shares
settings for one or several playlists, optionally with relative-path song lists
and enabled states. Recipients locate their own files using folder choices and
Library repair, and bind
saved locations to their current world/server. Basic native workflows pass in the [combined validation](docs/testing/2026-10-22-release-validation.md); remaining combinations are in deferred testing.

Library repair/relinking is implemented under Unreleased. Rescans report missing
folders and songs; Explorer plus a reviewed relative-path plan reconnects moved
music while preserving profile assignments. Basic native workflows pass in the [combined validation](docs/testing/2026-10-22-release-validation.md); remaining combinations are in deferred testing.

Per-playlist volume and world/server profiles are implemented under Unreleased.
Profiles share the imported music library while keeping playlist/playback setups
independent. Native copy/switch, OpenAL volume and restart checks pass; remaining connection/picker/audio combinations are in deferred testing.

Play-next queue and persistent listening history are implemented under Unreleased.
The Queue / history page supports queue editing, replay, re-queue, destination
assignment and clearing history. Native queue/pause/history and restart checks pass; remaining interaction combinations are in deferred testing.

The optional playlist loudness scanner is implemented. **Normalize playlist** reuses
valid track tags, analyzes missing/invalid gain, caches measurements in AFMR-owned
data, and reports effective per-track dB adjustments. Vanilla tracks are excluded.
Source music and foobar files stay read-only. See the [user guide](docs/USER-GUIDE.md#volume-replaygain-and-artwork).

## Possible follow-up

For any future NeoForge/Forge backport, explicitly check the target Minecraft
version's artwork decoder. Minecraft 1.20.1's `NativeImage.read(byte[])` can overflow
LWJGL's small native scratch stack even for a 256px PNG; Minecraft 26.3 already
has a native-heap fallback. This depends on Minecraft's implementation, not the
mod loader. Carry the Fabric stream-loading fix into affected targets, preserve
image/texture cleanup, and repeat large-artwork and repeated-load/close checks on
the target runtime before release. See the
[reproduction and fix](docs/testing/2026-10-22-fabric-artwork-memory.md) and
[future-port check](DEFERRED-TESTING.md#future-minecraft-ports).

The Hybrid now-playing toast is implemented alongside Minecraft's native toast and
the AFMR card. Custom styles support artwork with music-note fallback, resizing,
information switches, single-line/word-wrap layouts, text scrolling and a settings preview. The AFMR card also
offers background color and opacity settings. Native visual checks
are tracked in [deferred testing](DEFERRED-TESTING.md).

Optional server companions now supply multiplayer structure boundaries and mob-target
retention on both maintained targets. Vanilla/client-only servers retain their
event timers and unavailable structure data. See DEFERRED-TESTING.md for remaining
networked validation.

Add multiplayer combat-target retention through optional server-provided target
data. Singleplayer uses actual mob targets after an attack/damage trigger;
ordinary multiplayer keeps its event timeout because generic target identity is
not synchronized. Do not infer targets from proximity or aggression poses.

Benchmark gameplay impact before allowing analysis during active gameplay. Until
then, jobs run at the title screen or while singleplayer is paused, and suspend
when play resumes. Native verification is tracked in [deferred testing](DEFERRED-TESTING.md).
