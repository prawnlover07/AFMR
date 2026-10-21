# AFMR development guidance

This repository root is the authoritative AFMR source. Start with `PROJECT.md`,
`DEVELOPMENT.md`, `BACKLOG.md`, and `DEFERRED-TESTING.md`. Historical copies under
ignored `work/` and `outputs/` are archives, not editing targets.

## Collaboration

- If a request misunderstands the actual code or asks for behavior that does not
  make sense, explain the issue and clarify before editing. Raise relevant concerns.
- Follow the user's accepted playback semantics in `PROJECT.md` and `README.md`.
  In particular, Automatic off must still allow overrides; song-level off excludes
  the song; entry and exit finish controls are independent.
- Preserve music files and foobar playlists/settings as read-only. Use generated
  fixtures and an isolated game profile for state-changing tests.
- Coordinate native/desktop testing with the user's current authorization and
  computer use. Routine builds and unit tests do not need desktop interaction.
- Record complicated unfinished tests in `DEFERRED-TESTING.md`, and requested
  future features in `BACKLOG.md`. Distinguish measured results from assumptions.

## Code and performance

- Use Java and NeoForge; the current target is Minecraft 26.3 and Java 25.
- Avoid wrappers, shims, adapters, and compatibility layers. Update call sites
  directly. Remove dead or deprecated code when replacing it.
- Split code by responsibility; aim for source files below 2,000 lines. Avoid
  monolithic files and unnecessary abstractions.
- Performance comes first. Do not traverse the complete track library in steady
  gameplay ticks or rendering. Retain indexed lookups, cached UI data, bounded
  background work, and explicit ownership of save snapshots.
- Keep disk I/O and metadata/decoding work off the game thread where practical.
  Minecraft's sound executor owns OpenAL calls; the client owns scheduler state.
- Before writing threaded code, read the complete article and follow its paradigm:
  https://www.dgtlgrove.com/p/multi-core-by-default
- Do not add format versioning or old-format support without a concrete need.
  Existing shipped behavior should not be changed incidentally during cleanup.

## Verification and Git

- Run checks appropriate to the change. `./gradlew build` runs the regression suite;
  do not claim native audio/UI validation from pure scheduler tests.
- Use `./gradlew sourceZip` when verifying source packaging. Archives must not
  contain Git metadata, personal data, caches, test worlds, or old workspace copies.
- Keep generated files, local music/library snapshots, and published binaries out
  of Git. Small generated decoder test fixtures in `src/test/resources` are intended.
- Do not fabricate pre-Git development history. Tag `v0.5.2` marks the imported
  validated baseline; subsequent commits record actual project changes.
