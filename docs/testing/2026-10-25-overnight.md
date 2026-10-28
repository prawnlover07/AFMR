# Overnight combined-development verification — October 25, 2026

Animal authorized desktop testing for this session. Every client used a disposable
profile with a pre-launch 60 FPS cap and VSync off, one client at a time. Music,
artwork, share files and filesystem moves used generated fixtures. Personal game
profiles, music and foobar settings were not modified. This is verification of
the pre-companion 1.0.1 snapshot, not a numbered release or a universal compatibility
claim.

**Snapshot boundary:** while this pass was finishing, the separate server-companion
chat changed client condition observation/networking. Every native profile was
checked against the baseline hashes below; all used those earlier JARs. Final
unit builds also pass on the later in-progress sources, but that does not transfer
the native results to the new companion/client protocol. Validate that feature
separately after its implementation settles. No version bump or release freeze
is implied.

## Completed checks

| Check | NeoForge 26.3 | Fabric 1.20.1 |
| --- | ---: | ---: |
| Full regression build | 989 passed | 999 passed |
| UI-copy compiler | 20 passed | 20 passed |
| Recent-feature native audit, including actual Windows dialogs | 70 passed | 70 passed |
| Warning-only mute, natural ends/gaps/new starts | 22 passed | 24 passed |
| Native large-background decode/close stress | 5 passed | 5 passed |
| Historical artwork scratch-stack regression | — | 5 passed |
| Ten-minute playback/resource soak | 12 passed | 12 passed |
| Actual restart following soak | 12 passed | 12 passed |
| Expanded live riding audit | 27 passed | 27 passed |
| Song groups/player themes | 25 passed | 22 passed |
| Directional transition rules | 17 passed | 17 passed |
| Proximity volume | 22 passed | 22 passed |
| Normalization during gameplay | 18 passed | 18 passed |
| Player clarity/theme drafts | 23 passed | 23 passed |
| Inline settings | 30 passed | 29 passed |
| Queue/history/profile/repair/share integration | 83 passed | — |
| Full integration restart/shutdown | 17 passed | — |
| Separate vanilla-server lifecycle/playback | 39 passed | Not run |
| Separate vanilla-server riding | 28 passed | Not run |

All completed rows have zero failures. The native clients used the packaged runtime
JARs. The initial Fabric audit used Java 25; its sustained soak and the repeated
image-memory probes use Java 17.0.15. NeoForge uses Java 25.

## Recent-feature audit

The game tests exercised the current screens and asynchronous client callbacks:

- `.afmrp` exports with content signatures; default New profile and filename
  suggestion; name drafts across child screens, hide/reopen and screen
  reinitialization; cancellation and the profile limit. New imported-only
  profiles stay inactive and unbound.
- Existing-profile case/whitespace name replacement preserves the existing
  playlist object, ID and relative priority among existing playlists.
  Settings-only replacement retains enabled/disabled assignments; song-list
  replacement replaces them and reuses catalog identity. Ambiguous names are
  rejected. Importing while the target plays preserves its playback sequence
  in the measured settings-only cases.
- Wrong-content individual choices are rejected; renamed identical files are
  verified; linked-library search, Missing/All/Original folders views and filtering
  work. Import copies remain independent from the active setup.
- Cancelling a 5,000-entry health check retains the prior completed report and
  discards late results. Moving a generated song produces a missing-source issue;
  reviewing/cancelling preserves its original path, and Apply reconnects it while
  retaining the disabled assignment and clearing the missing issue.
- Theme right-click menu, opaque actions, keyboard navigation, Rename and
  confirmed Delete; independent/colliding repeated imports and export snapshots;
  Cancel releases the preview.
- Independent widget image upload, sizing and Preview; invalid replacement keeps
  the previous image; off releases textures, on lazily reloads, invalidation
  recreates textures, and Reset restores geometry/opacity/off.
- Actual Key Binds screen contains all 15 AFMR entries in one AFMR category.
  Both category screenshots were inspected. The generated widget Preview was
  also inspected.
- Server-synchronized horse mounting/dismounting selects the animal state;
  riding a boat does not.

Six additional checks per target used the real Windows dialogs through the
computer-use skill: theme Save, Open and Cancel; an individual song choice;
folder selection/content search; and widget image selection. All choices referred
to generated files under the disposable profile. The test helper itself does not
automate Windows.

## Native image ownership

Both background probes decoded a generated large wallpaper 100 times through
Minecraft's actual native image API. Dimensions stayed bounded, source bytes
were unchanged, closed pixels became inaccessible, and scratch-stack usage stayed
constant.

Fabric's artwork probe reproduced the old `Out of stack space` failure with the
superseded byte-array API as its intentional control. The current stream API then
completed 1,000 decodes and 100 invalid-image failures with stable scratch usage
and released pixels. Both Fabric image probes also pass on Java 17.

## Sustained playback

The NeoForge soak ran 600 seconds, with 240 song starts: 66 while Music was muted
and 80 while Master was muted. Generated folder artwork loaded repeatedly;
ordinary gaps, queue requests, manual pauses, profile swaps, animated widget
backgrounds and resource reloads at 180/420 seconds were exercised.

Maximum AFMR native music channels: **2**. Maximum AFMR textures: **8**. History
stayed at **100**. Post-GC heap was **558.6 MiB** at 62 seconds and **482.8 MiB** at
completion; observed buffers returned to about **1.69 MiB** after reloads. Reloads
caused temporary heap/frame-time spikes; these are not evidence of an AFMR leak.
The measurements include Minecraft's world/renderer and do not establish behavior
in a multi-hour modpack session.

An actual subsequent game launch passed 12 checks for restored history, catalog,
profiles, assignments, group timing and widget image settings, followed by fresh
native music/artwork/GIF playback. Session queue state was empty as intended.

Fabric's Java 17 soak also ran 600 seconds, with **242 starts**, including **67**
with Music muted and **80** with Master muted. Maximum channels/textures were
**2 / 8**, and history stayed at **100**. Post-GC heap was **378.4 MiB** at 62
seconds and **345.0 MiB** at completion; observed buffers ended around **4.50 MiB**.
Both resource reloads completed. Its actual subsequent launch passed the same
12 persistence/playback checks. These are per-runtime observations, not a memory
comparison between Minecraft versions.

The extended mute mode on both targets separately verifies actual new native streams at
zero effective gain, natural completion, the ordinary gap and a subsequent song
under both Music and Master mute. Unmuting restores gain while retaining the
independent manual-pause behavior tested earlier.

The expanded riding mode passes 27 checks per target: paged Riding animal controls
and Any/Yes/No choices; actual horses, donkeys, mules, camels, pigs and striders
through mounting/dismounting and fallback music; nearby animals alone; boats and
minecarts; a real Nether transfer/return; and death/respawn. Both camel seats with
two networked players remain outside this run.

## Broader integration and catalog scale

The NeoForge integration pass verifies live per-playlist gain, preview gain,
FIFO queue/gaps/pause/resume, override/queue interaction, history actions, profile
copy/switch/link ownership, imports finishing in their originating profile,
folder and individual repair with queue/history references, portable settings
and location rebinding, malformed inputs, and ordered shutdown/writer flush.
Its actual subsequent launch verifies linked profile selection, repaired playback,
imported-only profile persistence, history, settings and session-state reset.

The performance run used a **staged 67,812-track metadata catalog** and generated
audio; it skipped the real-folder rescan. Across the small-library, full-metadata,
full-assignment and large-workspace phases, median FPS was **60**, with fifth
percentile **59**. The large workspace opened in **405.8 ms**. The measured client
post-tick callback median stayed between **0.057 and 0.099 ms**; this includes other
callbacks and is not a pure AFMR profiler measurement. The large assigned playlist
was disabled for automatic playback, so this does not benchmark decoding/shuffling
all of its real songs or performance in Homestead. Heap observations in this short
benchmark were not normalized with GC and are not leak measurements.

The dedicated-server checks used the existing unmodified Minecraft 26.3 server,
bound to loopback only. The 39-check run covers day/night, cover/light/underground,
Home/server identity, combined biome/temperature/time/Y rules, water, Nether/End,
manual pause/Next/overrides, explicit normalization, environmental versus broad
combat damage, attack observation, combat death cut/respawn, vanilla resource
audio, disconnect cleanup, linked profile reconnect, dimension stability and
unlinked Default fallback. The 28-check riding run repeats the live mount,
dimension and death/respawn checks over a real server connection. This is one
player on a vanilla server; it does not validate PvP, remote proximity tracking,
both camel seats or a Fabric remote server.

## Test corrections

Intermediate failures were isolated test assumptions and were corrected before
the final passing runs: streaming `AL_SEC_OFFSET` wraps when processed buffers
are removed; 26.3 applies Master gain at the source; inserted playlists can shift
existing indices without changing their relative priority; 26.3 uses `oak_boat`;
Fabric's picker guard is a boolean; and an already verified song belongs in All
songs rather than Missing songs. The older transition helper also needed the
current `Playlist switching` label and panel scrolling; the inline helper needed
`Show rules` after disabling its last active filter. Integration setup must change
the visible volume field rather than directly changing the model underneath a
stale draft. Its restart expectations now reflect imported-only profiles rather
than the superseded Default-profile duplication behavior. These intermediate
results do not establish product bugs.

The sound executor publishes native observations to the client. Mute progression
uses accumulated observed forward movement instead of comparing two wrapping
stream offsets. Exact mute checks use effective source × listener gain.

## Evidence and limits

Reusable helpers are under `testing/native` and Fabric's `src/smoke`; raw logs,
generated paths and intermediate results are in ignored
`work/overnight-2026-10-25`. The helpers are excluded from the runtime JARs.
`DEFERRED-TESTING.md` retains the combinations not measured by this pass, including
real two-player tracking, third-party modpacks/devices, blocked external storage,
physical controller/Stream Deck/narration and extreme UI/font combinations.

Baseline runtime SHA-256:

```text
NeoForge ca901c5b5a6cd72bf2b12d6611d9dc7f6cd6b0d0bdc507f73313702fc8d454ec
Fabric   0ce4c4ee4ba7fe1f925627e8eabde3f82666c368bdd59c85ed57cfc1cbcf180c
```

Fresh final regression runs pass **989 / 999 tests**, with zero failed, errored or
skipped, plus **20 copy checks each**. Source packaging also passes its path audit:
no Git metadata, caches, test worlds, logs, old workspaces or private library
snapshots. The inspected source archives include wrappers, build files and
licenses. The later archives include
the in-progress server work and are not the native-tested snapshot. Runtime JARs
contain no probe/smoke classes; client bytecode targets Java 25 / Java 17.

Final-build client hashes, distinct from the native-tested baseline:

```text
NeoForge b82c2507982c67fccf555a663f91c31c0c72fd4312884f6d203fa6d96a914ea8
Fabric   ab84eae8ea0645f960ce5bf15257a92650180619569c19b3f5d650d3e7ac613a
```

These hashes identify the builds at the final-check point; continued companion
development can change them again. The disposable clients and vanilla server
were shut down after testing.

No product defect was established by this pass's final native runs. This chat's
changes are test helpers, test launch options and verification documentation.
The version stays 1.0.1; release archives/tags have not been published or changed.
