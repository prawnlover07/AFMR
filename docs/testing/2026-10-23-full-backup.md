# Full configuration transfer — October 23, 2026

Implemented under Unreleased on both maintained targets; version remains **0.6**.
Use **Import / export → Full configuration…** to export or review an `.afmrb`.
Replacement is explicit and applies after restarting Minecraft. Audio files stay
separate; library references and world/server links keep their original values.

## Measured results

- **702 NeoForge / 712 Fabric automated tests**, zero failures/errors/skips.
  Eleven new tests per target cover complete settings/profile/playlist/assignment/
  metadata/key/history round trips, background inclusion and source preservation,
  staging without current-session changes, actual startup installation, recovery
  copies and absent-image removal, simulated interrupted-journal rollback, failed
  install preflight, traversal/unknown members, incomplete/future/malformed JSON,
  decompressed entry/nesting bounds, non-AFMR/invalid keys, invalid/missing enabled
  images and preservation of an existing unsupported export filename.
- **10 focused NeoForge native export/review checks** pass. Real screens and
  synthetic mouse events navigate the new route, verify guarded Apply, export on
  the library worker, validate counts/keys/profiles, inspect bounds, cancel a review,
  reopen and stage replacement while preserving the current session.
- A separate actual Minecraft launch passes **6 restart assertions**: shared
  settings, independent playlist flags/volume, all profiles, a rebound physical
  shortcut, pending-file consumption and retention of previous managed files.
- **83 packaged Fabric smoke assertions** pass, including all prior 75 playback,
  codec/color-picker/UI checks, two seam assertions and six backup assertions.
  Full backup export, reviewed import, key inclusion and cancellation pass. One
  initial helper attempt missed a scaled workspace control; correcting the helper's
  coordinate transform made the run pass. No runtime fix was needed for that miss.
- An exported Fabric backup was installed by a real NeoForge startup: **7 transfer
  assertions** pass for shared appearance, complete playlists/assignments, profile
  links, physical keys, history, managed PNG bytes and pending-file consumption.
  That run also passes the separate 16 seam assertions (23 total).
- An exported NeoForge backup was installed by a real Fabric startup: **10 checks**
  pass including initialization, settings/playlist/profile/shortcut restoration,
  prior-file retention, pending consumption, removal of a previously managed image
  when the backup has none, and editor access after restoration.
- The compact replacement review screenshot was inspected at 854×480: counts,
  the red replacement warning, status and Apply/Back remain separate. Helpers and
  generated configurations/music/worlds stay outside runtime JARs and Git.

Evidence: [NeoForge review](evidence/2026-10-23-full-backup-neoforge-results.txt),
[NeoForge restart](evidence/2026-10-23-full-backup-neoforge-restart-results.txt),
[Fabric smoke](evidence/2026-10-23-full-backup-fabric-smoke-results.txt),
[Fabric → NeoForge](evidence/2026-10-23-full-backup-fabric-to-neoforge-results.txt),
[NeoForge → Fabric](evidence/2026-10-23-full-backup-neoforge-to-fabric-results.txt).

Final runtime JAR SHA-256 (the final cross-version runs load these exact JARs):

- NeoForge: `9f6224e03c99aa59b7e128a6fe2f3fd752a245d960b710924c1c9c3c75d6c444`.
- Fabric: `7cb6a30efc6c4d45ce48e1159423e192dc0a240bac06923a5f6e0b103b10281e`.

## Ownership and limits

The client captures owned library/history/key snapshots; the existing library
worker handles compression, decoding and validation. Startup installation runs
before loading library state or starting scanner work. Only library/history/keys
and the managed background are accepted; ZIP entries never become arbitrary paths.
A recovery journal and prior managed files allow interrupted installs to roll back.
Original music, foobar settings and the user's real installs were untouched.

Paths/world links are deliberately not guessed or rebound. Folder repair remains
available after transfer. Fabric has no NeoForge modifier shortcuts, menu blur or
native/hybrid toast styles; unsupported shortcuts are reported and unbound. These
checks establish transfer between the two current builds, not unknown future mod
formats, every historical release or every OS keyboard layout.

Native tests call actual widgets and runtime APIs; new-extension Explorer dialog
interaction, physical input, disk-full/power-loss fault injection, very large backup
latency and enabled-image texture upload after cross-version restore remain deeper
cases in [deferred testing](../../DEFERRED-TESTING.md).
