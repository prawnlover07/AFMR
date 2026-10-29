# AFMR 1.1.1 — Forge 1.18.2

Independent client and optional dedicated-server companion for Minecraft **1.18.2**,
Java **17**, and **Forge 40.3.0**. Install only the full client JAR on clients;
press **M** in a world. Bind **Show now playing** in Minecraft's Controls menu.
The optional companion adds loaded-structure conditions and mob-target combat
retention. Do not install both AFMR JARs in one instance.

Playback, local file/folder and foobar imports, profiles, sharing/repair,
conditions, transition rules, queue/history, normalization, custom fonts/images
and the AFMR now-playing card are retained. This Minecraft version does not
provide the newer vanilla music toast, music-frequency setting or menu blur.

Incoming-hit and confirmed ranged-hit combat events work in singleplayer and on
servers with the matching companion. Without it, combat starts from direct local
attacks only. AFMR does not guess attackers from nearby entities or treat falls
and fire as fights by default. The Combat page explains this limitation.
The companion sends only the receiving player's dimension/entity scope, hit
direction and opponent category; it sends no opponent identity or music data.

## Build and verification

Run from this directory:

```powershell
.\gradlew.bat build
.\gradlew.bat sourceZip
```

Gradle provisions the target compiler and Java 25 test runtime as needed.
JARs are in `build/libs`; the independently buildable source ZIP is in
`build/distributions`. UI writing inputs are included in that archive.
Full builds, packaged UI/audio/integration/restart, actual companion and vanilla
fallback checks pass. This port's extracted source archive also builds on its own.
The root [support matrix](../../docs/PORTING.md) and
[measured report](../../docs/testing/2026-10-29-older-ports.md) record coverage and
remaining field cases. Root-relative documentation links refer to the full
repository; this standalone archive retains its own build inputs and instructions.

Disposable packaged-client probes use `-Pprobe -PnowPlayingKey runClient`,
`-Pnavigation`, `-PmuteWarning`, `-Pcodecs` or `-Pintegration` (then `-Prestart`).
Build first. Headless fixture checks use `-PserverProbe runServer`; `-Psocket`
leaves the fixture open for `-Pprobe -Pcompanion runClient`. Test profiles are
isolated under ignored `run-*` folders, capped at 60 FPS with VSync off.
Never direct a probe at a personal game profile or music library.

AFMR is MIT licensed; bundled libraries retain their own licenses. Preserve
their notices and corresponding sources as described in `THIRD_PARTY.md`.
Independent project; not an official Minecraft product or approved by Mojang or Microsoft.
