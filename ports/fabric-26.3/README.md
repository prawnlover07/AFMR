# AFMR 1.1.1 for Fabric 26.3

Independent Fabric target: Minecraft **26.3**, Java **25**, Fabric Loader
**0.20.1 or newer**, and Fabric API for **26.3**. Install the full client JAR
on the player's computer and press **M** in a world to open AFMR.
The optional small server companion is for server owners; do not install both
AFMR JARs in one instance. Ordinary playback works without a companion.

This target includes the same native music toast, music-frequency integration,
menu blur, AFMR widget, imports, contextual playlists, manual controls and
Show now playing shortcut as NeoForge 26.3. It does not require NeoForge.
Use Minecraft's Controls menu to assign the initially unbound popup shortcut.

## Build

```powershell
.\gradlew.bat build sourceZip
```

Use this directory's Gradle **9.8.2** wrapper and Java 25. Dependencies download
on the first build. The root project's older wrapper cannot build this target.
Client and companion JARs appear in `build/libs`; the independent source archive
appears in `build/distributions`. Root writing inputs are shared at build time;
the source archive includes those inputs so it builds on its own.

## Verification

The measured port report is `docs/testing/2026-10-29-fabric-26.3-port.md`
at the repository root (also included in the independent source archive).
The disposable native harness is under `src/probe`, with the dedicated-server
fixture in `src/serverProbe`. Neither is included in a runtime JAR.
All clients are capped at 60 FPS, VSync off, before startup. Never point these
harnesses at a personal game instance or music library.

Full build: 1,011 pure regressions, ten packet/audio component checks and 20 text
checks. Packaged gameplay: 19 hotkey, 49 navigation/transport, 22 mute, 83 feature
integration and 17 restart checks. Dedicated-server fixture: 13 checks; actual
companion connection: 12; unmodified-server fallback: eight.

Native runs use generated WAV/artwork plus Minecraft resource music; these are
controlled checks, not a claim of every codec/modpack or long-duration coverage.
