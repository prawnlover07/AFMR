# Combined release validation — October 22, 2026

Development version remains **0.6**. This session tests the combined Unreleased
work; it does not finalize a new version or establish that other chats are done.

Later optional song-list sharing is covered by the
[song-sharing report](2026-10-22-song-sharing.md). Counts and the artifact hash below
identify this earlier validation snapshot, not the subsequently rebuilt JAR.

## Confirmed results

- **639 automated tests**, zero failures/errors/skips. The suite was explicitly
  rerun, then passed again after the shutdown fix and final startup logging change.
- **83 isolated integration checks**, zero failures. The helper loads the packaged
  AFMR JAR in `testing/native/run/integration`, with copied disposable world data
  and generated WAVs.
- **12 restart checks**, zero failures, in a second Minecraft process using the
  same profile. The world-linked setup, independent timing/volume, repaired paths,
  shared setup locations and listening history survive. Queue/pause/override are
  session state and clear. History is rendered, then cleared through its screen.
- **179 existing workspace checks**, zero failures, on the updated build. Actual
  screens/renderer, cached drafts, columns, tooltips, independent fades and local
  server jukebox behavior pass. Routine WAV metadata INFO output is absent from
  that run.
- **81 native playback regressions**, zero failures: native pause/resume, source
  ownership, shuffle/repeat avoidance, environment transitions, combat death,
  seven decoder fixtures, intentional decoder failure/retry and indexed access
  with 70,000 unrelated tracks. This is a new run, not the historical baseline.
- **15 fresh-install checks**, zero failures, using a new profile without an AFMR
  settings/library file. Six templates/priorities, intended Minecraft-music
  switches, 100% volumes, 600–1200-second timing, zero added fades/buffer, native
  widget, no local imports/history/links, unbound Next/Pause, actual vanilla audio
  and final shutdown flush pass.
- **39 vanilla-server checks**, zero failures, against an unmodified server on
  loopback. Actual reconnect selects the linked profile and its independent
  volume; Nether/Overworld travel retains it. Unlink preserves the running setup,
  logout returns to Default, and the next join uses Default with its original
  volume. Changing the server's display name does not change address identity.
  Existing online playback/context/combat/death checks also pass.
- Windows Explorer Save/Open actually exported and reimported one selected setup.
  The imported copy has no songs, retains 37% volume and appears directly above its
  source. Cancelling a second Save returns normally with a cancellation message.
  These actions used the computer-use plugin, not simulated SDL callbacks.

## Integration coverage

The real screens exercise volume slider/mute/reset, workspace queue actions,
FIFO order and normal silence, natural short-song completion, native pause/resume,
history replay/requeue/destination access and no pause/resume duplicates. A newer
held override retains pending requests; Use queue next resumes them without losing
that override. Automatic-off playlists still allow overrides and queued songs;
song-level off still excludes a song.

Profiles are copied through the screen, linked to the exact disposable world,
activated, and switched back. Their settings remain independent and music stays
shared. Use now clears queue/pause/override/audio. An import targeted at an inactive
profile completes in that originating setup after switching profiles.

Generated folders are moved, rescanned, reviewed, cancelled and repaired. A renamed
song is repaired individually. IDs and inactive-profile assignments survive rescan
and restart; the report clears, repaired music actually opens a native channel,
and both audio fixtures remain byte-identical to their originals.

Sharing checks include All/None, export/import work on the library worker,
preview/cancel/repeated review, priority insertion, retained playback flags/volume,
location rebinding, independent empty assignments, invalid input and extension
errors. Native Explorer interaction above supplements the helper-driven screens.

## Large-library measurements

A separate `run/performance` client used read-only metadata for the user's
**67,812 local tracks**, plus one generated playback fixture. Personal playlists,
profile links and preferences were excluded. All 67,812 assignments in the stress
playlist were populated; gameplay kept the small generated-music playlist active.
The large playlist was opened for UI measurement.

The corrected comparison disables Minecraft's AFK FPS cap only in the disposable
client. Each row retains 240 samples after 60 warmup ticks, in a stationary copied
world, using the same 120-FPS limit and window size:

| Case | Client Post median / p95 (ms) | Client tick median / p95 (ms) | FPS median |
| --- | --- | --- | --- |
| Small library, gameplay | 0.0950 / 0.1920 | 2.0153 / 2.7363 | 119 |
| Full catalog, gameplay | 0.0668 / 0.0957 | 1.7255 / 2.3040 | 119 |
| Full catalog and assignment data, gameplay | 0.0664 / 0.1147 | 1.5552 / 2.1493 | 119 |
| Large assigned playlist open | 0.0693 / 0.1034 | 1.6136 / 2.1358 | 119 |

Opening the large workspace took **567 ms** in this pass (475 ms in the earlier
pass). These are single opening observations, not a distribution. The Post span
includes AFMR and the test mod/event dispatch; the client-tick span excludes
rendering and does not capture every scheduled client task. These are not isolated
AFMR execution times or an AFMR-on versus AFMR-off benchmark. They show no catalog-
size FPS regression in this scene; ordering/JIT/GC explain why smaller measured
values must not be treated as an improvement caused by more tracks.

A complete read-only rescan of the linked music folder, followed by source health
checks, took **436.78 seconds** and reported **zero missing/unavailable sources**.
That first pass still had verbose third-party metadata logging. Its FPS readings
were affected by the default AFK cap. Its bounded timing array retained only the
first 4,096 scan-period samples, and brief menu inspection/background build work
occurred during it. A 606-ms tick spike therefore cannot be attributed specifically
to scan completion. The report corrects the earlier progress-message inference.
The probe now uses rolling samples plus whole-phase maxima for future long runs.
No throughput improvement is inferred for the logging fix without a controlled
comparison.

Checksums before and after this scan match for **20 personal files**: the original
AFMR library configuration, all 15 foobar `.fplite` files, and one sampled music file
in each of MP3/FLAC/OGG/WAV. This is sample preservation evidence, not a checksum of
every audio file. Generated repairs separately verify both fixture files byte for
byte. No personal music, foobar file or live-game configuration was written.

The later server-pass recheck still matches all 15 foobar files and four sampled
audio files. The personal AFMR configuration differs from the earlier snapshot;
its modification time is 15:09, before the final server pass began at 17:41. The
20/20 result above applies to the earlier scan interval, not the whole afternoon.

## Defect found and fixed

Minecraft's `GameShuttingDownEvent` can occur before player logout. AFMR closed
its save writer first; later logout returned the active profile to Default and
attempted a save against the closed writer. The native log reproduced
`Library store is closed` on normal exit with a linked profile.

Shutdown now saves the Default transition before the final writer flush, marks
the runtime closed and ignores later save/tick callbacks. Shutdown is idempotent.
The 83-check run explicitly exercises shutdown, late logout, a late screen save,
repeated shutdown, and reading the final saved profile. The subsequent interactive
client also exits normally without the old save error.

## Excessive metadata logging

The real-folder rescan emitted over 260,000 third-party WAV INFO records before
finishing. Every RIFF chunk was being logged, creating tens of megabytes of console
output. AFMR startup now retains the jaudiotagger logger at WARNING level, before
imports or playback tag reads. Warnings
and errors remain enabled. This is a log-volume fix; no scan-throughput or FPS
improvement is inferred from log size alone.

## Test-fixture corrections

Initial probes incorrectly treated streaming OpenAL buffer offsets as a monotonic
song position, allowed a 1.5-second random song to end during volume observations,
and tried renaming a currently open file. The corrected fixtures use the long
song for sustained checks, observe offset changes on the same native source and
stop playback before renaming. A direct test-only profile switch also left an
editor displaying a playlist absent from the new profile; the probe now opens
the correct editor, as the actual Use now UI already does. The fresh-install probe also
initially assumed every template includes vanilla music; Dark exploration
intentionally starts with that switch off, and the expectation was corrected. These failed preliminary
runs are retained locally and are not counted as product failures or passing cases.

The extended multiplayer probe initially left the player in Survival during an
additional Nether trip. The player died, so the return teleport and final native
playback assertion could not succeed. The corrected travel section uses Creative;
the earlier intentional death/respawn section still uses Survival. Startup also
respawns a dead disposable player before beginning a repeated run. The preliminary
37-pass/2-failure result is retained locally and excluded from the final totals.

## Final build and packaging

There are **409 passing live/helper checks** across integration (83), restart
(12), fresh installation (15), workspace (179), playback (81) and vanilla-server
testing (39), plus actual Explorer checks and the performance passes above.
The [sanitized case list](evidence/2026-10-22-combined-release-results.txt)
retains the passing assertions without personal paths or raw playback logs.

The final root `build sourceZip` succeeds and runs all 639 tests. The updated
helper compiles and the 39-check server run loads that packaged JAR. Its log has
zero jaudiotagger INFO records and no closed-writer save error, including actual
local playback and shutdown. The earlier approval-service usage-limit interruption
is resolved; it is no longer a pending validation step.

The JAR reports version **0.6**, excludes the native test helper and passes ZIP CRC
validation. SHA-256:
`9675bbb4a4c9e00769ab11594ea568398a9f5cc0d49182024e45d9713fa85afe`.
The source archive is rebuilt after these records are updated; checks exclude Git
metadata, caches, local archives, generated fixtures/results and game profiles.
No personal music/library snapshots are included. Both archives pass CRC checks.
Version remains unchanged under the repository's combined-update policy; this
validation does not publish a release or establish that concurrent work is done.

## Remaining optional coverage

The focused release pass is complete. Full
Stream Deck hardware input, audio-device changes, subjective listening quality,
network-drive/permission extremes and every cross-feature combination remain
separate checks in `DEFERRED-TESTING.md`.
