# AFMR 1.1.1 — Forge 1.20.1

Independent client and optional server companion for Minecraft **1.20.1**, Java
**17**, and Forge **47.4.0**. Install only the client JAR on clients; press **M**
in a world. Bind **Show now playing** in Minecraft's Controls menu if desired.
Servers can install the smaller companion for loaded-structure conditions and
mob-target combat retention. Do not install both AFMR JARs in one instance.

Playback, library management, sharing, profiles, conditions, queue/history,
ReplayGain tags, custom fonts/backgrounds and the AFMR now-playing card are
retained. Minecraft 1.20.1 does not provide the newer vanilla music toast,
music-frequency setting or menu blur integrations.

## Build

Use Java 17 or newer to start Gradle; Gradle can provision the Java 17 compiler
and Java 25 regression-test runtime. From this directory:

```powershell
.\gradlew.bat build
.\gradlew.bat sourceZip
```

Client and companion JARs are in `build/libs`; the independent source archive is
in `build/distributions`. The source archive includes the shared UI writing inputs.
See the root [support matrix](../../docs/PORTING.md) and
[verification report](../../docs/testing/2026-10-29-forge-1.20.1-port.md).

Native probes use generated fixtures and disposable `run-*` directories. Build
first, then use `-Pprobe -PnowPlayingKey runClient`, `-Pnavigation`,
`-PmuteWarning`, `-Pcodecs` or `-Pintegration` (optionally `-Prestart`). Headless
checks use `-PserverProbe runServer`; `-Psocket` leaves the fixture server open
for a matching `-Pprobe -Pcompanion runClient` connection. Clients are capped at
60 FPS from startup, with VSync and Forge's early startup window disabled.

AFMR is MIT licensed. Bundled libraries retain their own licenses; redistribute
their notices and corresponding sources as described in `THIRD_PARTY.md`.
The two audio SPI libraries share an identical package/class, so their classes
and provider registrations are packaged together in one game-layer library.
Original notices remain embedded. No Minecraft or Forge code is redistributed.

Independent project; not an official Minecraft product or approved by Mojang or
Microsoft.
