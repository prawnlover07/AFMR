# Repair-search cancellation — October 24, 2026

Unreleased on both maintained targets; development versions remain 1.0.1.
Check again changes to Cancel search during explicit missing-source checks and
repair-preview preparation. Cancellation keeps the previous completed health
report and saved file references; no partial repair is applied or preview opened.
The button shows Cancelling until the worker has finished.

The existing library worker receives owned source snapshots and a per-operation
AtomicBoolean predicate. Checks poll between folders/tracks and before completing;
planning also polls while selecting tracks and building destination/merge results.
Client callbacks verify operation identity and cancellation before accepting a
report or plan. This covers cancellation after worker completion but before its
queued client callback. The busy flag is cleared only on worker completion, so
another scan cannot race cancelled work or its progress updates. No new executor,
ordinary worker interruption or file-format compatibility layer was added.

## Measured verification

- Both full offline builds pass: **888 NeoForge / 898 Fabric tests**, zero
  failures/errors, plus **20 UI-copy compiler checks per target**.
- **Ten new cancellation tests per target** cover cancellation before the first
  folder, during folder checks, after three songs in a 5,000-song library, at the
  final song and with empty input; cross-thread signalling with deterministic
  latches and successful worker reuse; interruption on shutdown; cancellation
  before repair path validation, during repair planning and after its final file.
- Generated fixtures confirm source paths, assignments and metadata remain
  unchanged, and an uncancelled retry still completes normally.
- Both 1.0.1 development JARs are rebuilt; existing version/archive policy remains.

## Limits

No game window/native UI run was performed. Component tests validate worker
cancellation and read-only inputs, not physical clicks, screen reopening, the
real Minecraft callback queue or all slow-storage cases. A filesystem/metadata
call already in progress can delay acknowledgement; Cancel does not terminate an
operating-system call. Ordinary import/rescan jobs retain their existing behavior;
this control cancels explicit repair-menu checks and preview preparation.
See [deferred testing](../../DEFERRED-TESTING.md#repair-search-cancellation-unreleased-both-targets).
