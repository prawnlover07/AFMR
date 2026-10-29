# AFMR 1.1.1 multi-version release — October 29, 2026

The agreed five Minecraft versions now have ten verified client builds and ten
matching optional server companions. The original two-target `v1.1.1` baseline
was released first and remains unchanged. Added ports stay at 1.1.1; no public
upload or remote push was performed.

## Measured verification

| Target | Java | Pure regressions | Packet/audio components | UI-copy checks |
| --- | ---: | ---: | ---: | ---: |
| NeoForge 26.3 | 25 | 1,011 | 10 | 20 |
| Fabric 26.3 | 25 | 1,011 | 10 | 20 |
| NeoForge 1.21.1 | 21 | 1,011 | 10 | 20 |
| Fabric 1.21.1 | 21 | 1,021 | 10 | 20 |
| Forge 1.20.1 | 17 | 1,011 | 10 | 20 |
| Fabric 1.20.1 | 17 | 1,021 | 10 | 20 |
| Forge 1.19.2 | 17 | 1,013 | 14 | 20 |
| Fabric 1.19.2 | 17 | 1,023 | 14 | 20 |
| Forge 1.18.2 | 17 | 1,013 | 14 | 20 |
| Fabric 1.18.2 | 17 | 1,023 | 14 | 20 |

These are fresh full builds from a clean Git export. Every target passes its
own scoped packaged client and companion gates; exact native counts and test
boundaries are recorded in the individual reports:

- [Original NeoForge 26.3 / Fabric 1.20.1 baseline](2026-10-29-release-1.1.1.md)
- [Fabric 26.3](2026-10-29-fabric-26.3-port.md)
- [Forge 1.20.1](2026-10-29-forge-1.20.1-port.md)
- [Fabric / NeoForge 1.21.1](2026-10-29-1.21.1-ports.md)
- [Fabric / Forge 1.18.2 and 1.19.2](2026-10-29-older-ports.md)
- [Green-note mod icon across all twenty runtime JARs](2026-10-29-mod-icon.md)

Native clients ran one at a time, capped at 60 FPS with VSync off from startup.
Only generated fixtures and disposable profiles were modified. Tests use actual
Minecraft/OpenAL state and registered input queues; they do not claim physical
keyboard input, human listening to every format or prolonged modpack coverage.
Older native startup failures and unchanged successful retries are recorded in
the older-port report; their precise cause is unresolved.

## Source and package audit

All ten clean-export builds pass `build sourceZip`. Each newly added port also
passes an actual build/sourceZip from its own extracted standalone archive;
the four oldest ports were checked again after their final production changes.
Final documentation updates regenerate archives without changing tested classes.
Archives retain wrappers, tests and shared UI writing/compiler inputs and exclude
Git metadata, caches, private configuration/music, logs, worlds and old workspaces.
Unrelated uncommitted page notes are excluded by packaging committed sources.

For every target, both clean-build class payloads equal the locally tested client
and companion classes. Bytecode targets match the table. Runtime archives exclude
probe/smoke classes. Metadata identifies the correct game, loader and 1.1.1
version; icon references resolve to the owner's exact image. Minimal companions
contain only their server observations, packets and required pure context types,
with no nested audio/image libraries or client assets beyond the branding icon.

Client distributions retain original third-party notices and corresponding source
archives: thirteen for Fabric and NeoForge 26.3, twelve for NeoForge 1.21.1, and
fifteen for Forge. Forge's merged SPI container preserves original notices and
both original source artifacts; MixinExtras Forge/common sources and MIT notices
are included separately. NeoForge 1.21.1 uses Minecraft's supplied JOrbis module.

The combined output is `outputs/1.1.1/multi-version`. Each target contains five
release artifacts: a client JAR, companion JAR, independently buildable source
ZIP, client distribution ZIP and companion distribution ZIP. That is twenty
runtime JARs, twenty distribution ZIPs and ten source ZIPs. Target manifests and
an aggregate manifest record SHA-256 for all fifty. Client distribution folders
retain dependency sources and their own source checksums. Original baseline and
earlier port packages remain intact.

## Player-visible boundaries

Only 26.3 provides Minecraft-native music toast, Music Frequency and menu blur;
older versions retain the AFMR card and its own timing controls. On 1.18.2 /
1.19.2, incoming and confirmed ranged-hit combat attribution requires singleplayer
or the matching companion. Without it, direct local attacks start combat; AFMR
does not guess attackers or count falls/fire as combat by default.

All targets keep music, library and playlist configuration client-local. Server
companions are optional, loader/game-specific alternatives to the full client
JAR, never an additional AFMR JAR in the same instance. Broader multiplayer scale,
third-party combat/audio/input mods, hardware/device changes and prolonged play
remain field cases in [DEFERRED-TESTING.md](../../DEFERRED-TESTING.md).
