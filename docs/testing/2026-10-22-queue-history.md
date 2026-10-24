# Play-next queue and persistent listening history

Pending under Unreleased; the development version remains 0.6 while other work is
in progress. User choices: keep normal inter-song silence and persist the latest
100 history entries between launches.

## Implemented behavior

- Up to 100 queued requests, editable FIFO order, stable duplicate identities,
  remove/clear controls, no enqueue interruption and normal gaps. Next uses a ready
  queue first. Song switches, source membership and session failures are rechecked
  at the request's turn; invalid entries cannot block later requests indefinitely.
- Queue mode bypasses automatic conditions, context interruptions and time fitting.
  It respects pause, jukebox holds, death guards and fades. Existing held overrides
  resume afterward; newer overrides can take precedence without discarding requests.
- History retains the latest 100 successful scheduler starts across launches, with
  immutable metadata, source/origin, timestamp and selection explanation. Replay,
  re-queue, add to a chosen playlist and Clear history are provided. Queue resets on
  disconnect/resource reload; history is retained. No pause/resume duplicate entries.
- Separate small history snapshots share the existing coalescing save worker.
  Music files are read-only, original library saves retain their backup behavior,
  and song starts do not copy the full library or add worker threads.

## Automated verification

The shared-source root build passes **548 tests**, no failures/errors/skips,
including **39 new focused checks**: QueuedPlaybackTest (29), ListeningHistoryTest
(4), ListeningHistoryStoreTest (6). These cover the controls' underlying queue
ordering/capacity, normal silence, disabled/removed/failed requests, duplicate IDs,
automatic fallback, override hand-offs, previews, pause, jukebox, combat death,
time-fit bypass, Play on trigger, source/metadata/reason capture, ring bounds,
restart/clear persistence, interleaved coalescing, shutdown flush, corrupt/oversized
history and write failures. A guarded 70k-entry library forbids full-library reads
during enqueue/start and 2,000 steady ticks. This proves indexed access, not FPS.

Native desktop/game interaction was not used. The menu layout/input, native audio,
actual game-shutdown/restart and profile integration checks remain in
[DEFERRED-TESTING.md](../../DEFERRED-TESTING.md). A successful scheduler start may
still encounter an asynchronous decoder error later; these tests do not establish
audibility of every history entry. Personal music, foobar data and live game
configuration were not modified.
