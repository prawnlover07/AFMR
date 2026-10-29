# Minecraft and loader expansion

Agreed October 29, 2026: finalize 1.1.1 for the existing two targets first, then
add independently verified ports. Do not label a planned target as supported or
upload an unverified JAR. The list is a practical support selection, not a claimed
ranking of active players.

| Minecraft | Loader | Java | State |
| --- | --- | --- | --- |
| 26.3 | NeoForge | 25 | Existing target; tested 1.1.1 baseline |
| 26.3 | Fabric | 25 | Tested 1.1.1 port; see [report](testing/2026-10-29-fabric-26.3-port.md) |
| 1.21.1 | NeoForge | 21 | Tested 1.1.1 port; see [report](testing/2026-10-29-1.21.1-ports.md) |
| 1.21.1 | Fabric | 21 | Tested 1.1.1 port; see [report](testing/2026-10-29-1.21.1-ports.md) |
| 1.20.1 | Forge | 17 | Tested 1.1.1 port; see [report](testing/2026-10-29-forge-1.20.1-port.md) |
| 1.20.1 | Fabric | 17 | Existing target; tested 1.1.1 baseline |
| 1.19.2 | Forge | 17 | Implementation/testing in progress; not released |
| 1.19.2 | Fabric | 17 | Implementation/testing in progress; not released |
| 1.18.2 | Forge | 17 | Implementation/testing in progress; not released |
| 1.18.2 | Fabric | 17 | Implementation/testing in progress; not released |

Each client target includes its matching optional dedicated-server companion.
Forge is the selected Forge-family target through 1.20.1; NeoForge is selected
for 1.21.1 and 26.3. Supporting one does not assert compatibility with the other.

## Port and verification requirements

- Keep direct loader/Minecraft call sites for each target. Do not introduce
  runtime wrappers, shims or compatibility layers. Preserve indexed libraries,
  bounded decoder/artwork work and sound-executor ownership of native audio.
- Match playback, imports, profiles, repair/sharing, keybindings and custom UI
  behavior. Features absent from an older Minecraft version use the existing AFMR
  widget; do not promise unavailable vanilla toast/frequency/blur integration.
- Build and run the pure regressions, loader-specific packet/audio components and
  text checks with the appropriate Java target. Compilation alone is not a pass.
- Run packaged startup, local and Minecraft-resource playback, pause/Next/seek,
  the now-playing key, import/assignment, persistence and representative conditions
  in disposable profiles. Cap clients at 60 FPS with VSync off from startup.
- Verify the matching companion, actual companion-enabled multiplayer and fallback
  without it. Keep observations scoped/bounded and free of music/library data.
- Audit runtime metadata/dependencies/licenses, optional server exclusions and
  independently buildable source packages. Record exact artifact hashes and the
  measured scope; retain deeper field cases in DEFERRED-TESTING.md.

Public upload/account setup is separate from local packaging. Preserve `v1.1`,
the `v1.1.1` baseline and prior packages. New port commits record real
work; do not move a published release tag to later source.
