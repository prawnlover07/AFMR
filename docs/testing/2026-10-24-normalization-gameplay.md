# Manual normalization during gameplay — October 24, 2026

Both maintained development builds remain **1.0.1**. Explicit **Normalize** jobs
now start and continue in singleplayer or multiplayer, independently of pause or
world presence. The normalization screen no longer pauses singleplayer. Back
leaves the batch running; Cancel retains completed measurements.

## Implementation

Removed the runtime's per-tick world/pause permission gate, the scanner's allowed
flag/setter/getter/start argument and its sleeping wait loop. Updated direct call
sites and removed the obsolete waiting status. Cancellation/interruption checks
still run between files and PCM chunks. The existing one-or-two low-priority
worker lanes retain their own decoder, fixed buffer and meter; client-side result
application and owned-snapshot saves are unchanged. Music and foobar data remain
read-only. No automatic library-wide scan, new executor, wrapper, compatibility
layer or version bump was added. Manual functionality is not conditional on a
future performance benchmark.

The repository's required Multi-Core By Default article was read in full before
editing the worker code. This change retains homogeneous lanes, dynamic file
assignment, disjoint results and a final immutable report.

## Automated verification

- NeoForge: **836 tests passed**.
- Fabric: **846 tests passed**.
- Both full `build` tasks pass, including the current UI-copy compiler's **20 checks**
  per target and passage validation.

Loudness regression cases preserve tag/cache precedence, all decoder formats,
source bytes/timestamps, invalid file isolation, partial cancellation, snapshots
and storage. Replaced the obsolete wait/resume case with immediate manual start,
concurrent-batch exclusion, cancellation and subsequent reuse; added owned-request
and empty-batch completion coverage. The historical broader multiplayer helper
now expects completion while connected rather than a waiting job.

## Packaged native checks

Generated audio and disposable profiles only. All three focused runs completed
and exited successfully, **18 checks each / 54 passed total**:

- [NeoForge unpaused singleplayer](evidence/2026-10-24-normalization-neoforge-singleplayer-results.txt):
  `runClient -Pworkspace -PgameplayNormalization` in `testing/native`.
- [NeoForge actual vanilla multiplayer](evidence/2026-10-24-normalization-neoforge-multiplayer-results.txt):
  `runClient -Pmultiplayer -PgameplayNormalization`, with an existing unmodified 26.3
  server bound only to `127.0.0.1:25575`. Confirmed no integrated server.
- [Fabric unpaused singleplayer](evidence/2026-10-24-normalization-fabric-singleplayer-results.txt):
  `-Psmoke -PpackagedSmoke -PgameplayNormalization runClient` in the port.

Actual screen input starts a two-track batch, checks a non-pausing progress page,
and observes SCANNED results and applied caches while the world stays active.
A second 42-track generated batch continues after Back; reopening progress reuses
the same job, Cancel produces a partial report, completed measurements remain and
unprocessed entries stay unmeasured. Minecraft reports the preview sound active
with an unchanged playback sequence across both jobs. Original fixture SHA-256 is
unchanged, and loading the normal library file confirms saved measurements.
The Minecraft active-sound check is not a listening test or direct OpenAL offset
measurement. No live FPS/latency benchmark or physical input/narrator pass is claimed.

Native-tested runtime SHA-256:

- NeoForge: `b00ca6993f12ee5ade5cccf703e8d883c75a9bb8e2102cd67a3dc78f2e2e2fe3`
- Fabric: `9ff90c46cdd289059ee700636fddfd2d2678bb7069ebd2420c3d88a5f37b988d`

Both `sourceZip` tasks and archive-content checks verify source delivery without
local profiles/worlds, caches, Git metadata, personal data or generated backups.
Runtime JARs exclude test helpers. Existing release archives remain untouched.

## Remaining deeper cases

Fabric against a remote vanilla server; physical controls/narration and extreme
GUI scales; disconnect/profile-switch/reload while analyzing; slow/network disks,
very large batches, damaged codecs and shutdown during blocked I/O; measured
frame-time/audio latency on low-core CPUs. These are diagnostic follow-ups and do
not gate the explicitly requested action.
