# Library repair — October 22, 2026

Development version remains 0.6; changes are under Unreleased.

The full build passes **616 automated tests**, zero failures/errors/skips. There
are **27 new health/repair regressions**:

- Seven health cases cover linked missing roots and descendant songs, standalone
  files without folders, missing songs inside present folders, vanilla exclusion,
  wrong source types, more than 100 missing entries, owned report/input snapshots,
  general scan messages and progress.
- Twenty repair cases cover relative paths with duplicate basenames, partial and
  zero-match plans, individually renamed files, nested links and all profile
  destinations, sibling path boundaries, empty folders, unsupported choices,
  linked-target conflicts, conservative duplicate merges, stale-plan validation
  before mutation, file-attribute loudness reuse, stable IDs on rescan, bounded
  queue/history remapping, save/reload, switching profiles during review and
  rejecting the same/nested replacement root.
- The repair tests also construct 70,000 unrelated entries, apply one repair and
  forbid full-catalog reads for 2,000 subsequent scheduler ticks. Generated source
  bytes are checked unchanged. No personal music or foobar data is used or modified.

An existing foobar-import test now expects a known ID to survive a fresh metadata
rescan, while still requiring fresh metadata objects; this is the intended stable
identity behavior needed by repair.

Tests use generated temporary files and fake audio. No game launch or desktop
interaction occurred. They do not prove native Explorer/SDL input, error-toast or
screen layout, real-device/UNC/permission latency, subjective audio quality or
runtime shutdown sequencing. Those cases are recorded in `DEFERRED-TESTING.md`.
