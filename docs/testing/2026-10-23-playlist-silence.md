# Per-playlist silence — October 23, 2026

Both maintained targets remain version **0.6**. Custom silence is off by default;
playlists inherit global Timing until enabled through the workspace's **Silence**
button beside Volume. Minimum and maximum are seconds, 0–86400, maximum ≥ minimum.
Equal values produce fixed gaps (600 for ten minutes, 3 for three seconds).

## Automated checks

- NeoForge 26.3: **778 tests passed**, including **31 PlaylistSilenceTest cases**.
- Fabric 1.20.1: **788 tests passed**, including the same **31 cases**.
- Both complete `build` tasks passed, including UI copy generation/validation and
  the current copy compiler's 19 checks. Both source packaging tasks passed.

Focused coverage includes initial/default/custom gaps; fixed/random ranges and
no repeated rerolls; shorter/longer/repeated incoming selection changes with elapsed
silence; context buffer and fade/finish protection; pause and jukebox policies;
join/trigger/manual bypasses; held and one-song overrides; queue source/global
ranges, reordering, clearing and stale heads; decoder backoff; time-window fitting;
frequency changes; reset; finite bounds for active/inactive profiles; independent
clones/snapshots/profile copies; library persistence and strict playlist-file
validation. The existing full-backup round trip now includes a custom range in
both active and inactive profiles. Existing global Timing reset behavior remains.

## Packaged in-game checks

Disposable generated WAVs and test worlds only; no personal music or game profile
was changed. Both clients completed and exited successfully.

- NeoForge: **18 passed / 0 failed** via
  `runClient -Pworkspace -PplaylistSilence` in `testing/native`.
  [Preserved results](evidence/2026-10-23-playlist-silence-neoforge-results.txt).
- Fabric: **21 passed / 0 failed** (18 focused checks plus 3 startup checks) via
  `-Psmoke -PpackagedSmoke -PplaylistSilence runClient` in the port.
  [Preserved results](evidence/2026-10-23-playlist-silence-fabric-results.txt).

Actual controls verify inherited/custom labels, non-overlap, fixed ten-minute
entry, invalid/reversed/over-limit rejection, hide/reopen and resized drafts,
disabling without discarding values, and workspace refresh. A held override
starts generated music, normal Skip schedules three seconds, and a new playback
sequence starts after that real gap. NeoForge verifies Minecraft's active sound;
Fabric verifies actual OpenAL PLAYING state with no OpenAL error. The ten-minute
case verifies a scheduled countdown above 599 seconds without waiting ten minutes
in real time. Screenshots of both editors and workspace buttons were visually
reviewed; they remain in ignored disposable-profile screenshot folders.

An initial NeoForge run passed all 18 checks but rendered the fixture screen while
shutdown switched profiles, leaving its test-only playlist ID unavailable. The
helper now closes the screen before exit; the final packaged run exits cleanly.
This lifecycle correction is confined to the native helper.

## Package verification and limits

Runtime JARs contain the silence editor/model/scheduler and no native helpers or
test fixtures. Source ZIPs exclude personal data, Git metadata, caches, generated
game profiles/worlds and archives. UI strings use the concurrent UI-COPY pipeline.
Native-tested JAR SHA-256 values:

- NeoForge: `cc4ffaf10a5ee87300a5631b58d34e65dcdd40b4b716fa6afe019c298e4546e9`
- Fabric: `d775fb92e1c8e1766fa3936dedf8f8ab8ce6d64cfd640f3177078a4a6e48f58a`

Physical input/narrator traversal, extreme GUI scales, a full ten-minute wall-clock
wait, and every live multiplayer/combat/death/jukebox combination were not tested
in this focused native run. Automated scheduler checks are not native audio proof
for those combinations. No new threads or full track-catalog traversals were added.
