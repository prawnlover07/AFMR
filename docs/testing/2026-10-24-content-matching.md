# Cross-computer song matching — October 24, 2026

Unreleased on NeoForge 26.3 and Fabric 1.20.1; both development builds remain
1.0.1. The user chose identical complete file contents, including allowing
different names and directory layouts, rather than recognizing converted or
retagged recordings.

## Implemented workflow

- Export with Include song list records SHA-256 and byte length on the existing
  library worker. Portable references retain titles/relative paths but omit
  absolute source paths/IDs and audio. Successful export stores immutable
  SongIdentity values on unchanged live track references for future repair.
- Import provides Search linked libraries directly, enabling song import when
  pressed. It searches linked roots and known individual library files.
  Find song files offers Missing songs, All songs and Original folders views,
  title/path filtering, individual file selection, another recursive folder
  search, bounded error details, verified counts and Cancel search.
- Exact-path candidates with fingerprints must also match their file contents;
  a wrong file at an expected path never becomes a playable imported assignment.
  Unresolved signed references use AFMR-owned missing paths and retain identity.
- Content-verified file/folder repair recognizes renamed/reorganized copies.
  Existing destination folder settings survive link merging; nested links follow
  matched parent directories and collapse without leaving spurious missing roots.
  Partial signed repairs keep the remaining original references/root actionable.
  Stable IDs, disabled assignments, profiles and existing queue/history remapping
  continue through ordinary apply logic.

## Ownership and bounds

One worker-owned Reader reuses a 64-KiB buffer and digest. Searches first filter
by remaining byte sizes, avoid following directory symlinks, prefer known catalog
paths, avoid overlapping root walks, and retain only matching identities plus up
to 100 error messages. Files are checked for stable size/time/file key around the
hash read. No content/file operations occur during rendering or steady playback.
UI rows, search text and folder counts are cached at explicit screen transitions.

Inputs are captured snapshots; immutable identities travel through track copies,
saves, profiles and full backups. Export-only source paths/IDs are transient.
The import cancellation flag is checked between files and hash chunks; callbacks
check operation identity, cancellation and the pending import before publishing.
An OS read already in progress can delay acknowledgement. Unapplied/cancelled
searches do not alter the library, and music files are never written.

## Measured verification

- Both complete offline builds pass **915 NeoForge / 925 Fabric tests**, zero
  failures/errors, plus **20 UI-copy compiler checks each**.
- **27 new tests per target** use generated fixtures for byte identity across
  names/folders/timestamps, same-size wrong contents, invalid identities, cancelled
  reads with released handles, recursive/overlapping roots, known-path preference,
  unavailable roots, export privacy, changed/missing export sources, differently
  organized recipients, wrong expected-path files, explicit file selection,
  identical-copy assignment merging with Off winning, persisted missing identities,
  repair into linked roots, immutable/save/backup round trips, malformed imports,
  cancellation, individually imported catalog files, partial retries and nested
  root-setting preservation across profiles.
- Existing repair tests still cover late library/link changes, stable IDs,
  loudness invalidation and duplicate remapping. The loudness fixture now changes
  modification time while keeping bytes identical, as a known identity correctly
  rejects replacing the song with different bytes.
- Both source ZIP tasks succeed. Archive inspection confirms ContentFiles,
  SongIdentity and their tests are included, with no Git/cache/work/output/run
  directories. Final development JARs are rebuilt under the existing 1.0.1 names.

## Limits

No game window/native picker session was run. Component/build results do not prove
physical input, rendered layout, real Minecraft event-queue timing, network-drive
latency or native playback. See [deferred testing](../../DEFERRED-TESTING.md#content-based-sharing-and-repair-unreleased-both-targets).

Existing song lists without fingerprints retain their relative-path/manual
selection behavior; they need re-exporting to enable automatic content matching.
Absent local files without a recorded identity cannot be recognized by contents.
Exporting an identified song list reads file bytes and can take time for large
collections. Recorded identities survive full backups, but ordinary imports/full
backup creation do not hash every library file automatically. Retagging or
transcoding changes the identity; a modification-time change alone does not.
External edits after a reviewed search may require another check before applying.
