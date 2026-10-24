# Deferred features

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

Add multiplayer structure detection through an optional server-side source of
structure boundaries. Vanilla servers do not synchronize these. Preserve ordinary
client-only multiplayer use; an unavailable structure filter cannot match, though
another enabled filter may satisfy Match any. Singleplayer selection/search and
radius are implemented.

Add multiplayer combat-target retention through optional server-provided target
data. Singleplayer uses actual mob targets after an attack/damage trigger;
ordinary multiplayer keeps its event timeout because generic target identity is
not synchronized. Do not infer targets from proximity or aggression poses.

Benchmark gameplay impact before allowing analysis during active gameplay. Until
then, jobs run at the title screen or while singleplayer is paused, and suspend
when play resumes. Native verification is tracked in [deferred testing](DEFERRED-TESTING.md).
