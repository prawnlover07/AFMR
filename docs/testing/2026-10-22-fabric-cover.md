# Fabric covered-filter fix — October 22, 2026

The user's screenshots show a solid cave roof in snowy plains at Y 62, Dark
exploration above Night outside, and enabled Covered conditions. The Fabric
backport's cover sampler was reading `MOTION_BLOCKING_NO_LEAVES`, a heightmap
Minecraft 1.20.1 does not synchronize to clients.

## Cause and fix

Inspection of the mapped 1.20.1 Minecraft classes confirms that only
`MOTION_BLOCKING` and `WORLD_SURFACE` use `Heightmap.Usage.CLIENT`.
`LevelChunk` constructs all FULL heightmaps empty; chunk packets populate only
the transmitted maps. An empty no-leaves map returns the world-bottom height,
making the existing cover-depth check fail under a real roof. An absent map
would instead be lazily calculated, so regression fixtures must reproduce empty
initialized maps rather than simply omit them.

Fabric `SurfaceCover` now reads `MOTION_BLOCKING` and trims leaves, fluid-only
blocks and intervening air down to the first solid block in that column. It
retains the five-tick cache, keyed by chunk identity and column. The runtime
passes its current chunk and game tick directly. No server-side detection,
library scan, new background job or change to playlist logic is required.

The NeoForge 26.3 source was inspected separately: its no-leaves heightmap uses
`Usage.CLIENT`, so that implementation remains unchanged.

## Measured verification

- Fabric `gradlew.bat -p ports/fabric-1.20.1 build --offline` succeeds using JDK 25;
  the shipped classes continue to target Java 17. **694 tests pass**, zero
  failures/errors, including ten new `SurfaceCoverTest` cases.
- Fixtures use real 1.20.1 block sections and heightmaps, initialize FULL maps
  empty as `LevelChunk` does, and populate the two transmitted types. They verify
  the old no-leaves lookup returns -65 while a stone roof at Y 90 correctly yields
  a solid surface of 91 through the fixed sampler.
- Other checks cover water, leaves above air/ground, leaves over a solid roof,
  empty columns, negative coordinates, five-tick refresh, explicit clear,
  replacement chunks, changed columns, Dark exploration winning for a dark cave
  at night, and Night outside remaining eligible beneath foliage alone.
- The build produces the remapped
  `ports/fabric-1.20.1/build/libs/animals-final-music-replacer-fabric-1.20.1-0.6.jar`.
  The mod version remains 0.6 and pending notes are under Unreleased.

## Remaining verification

No game window or personal game profile was opened for this fix. Tests reproduce
the vanilla block/heightmap initialization and condition selection; they do not
establish live packet delivery, lighting or native audio in the user's modpack.
Recheck the reported cave and surrounding sky/foliage/water transitions with the
rebuilt Fabric JAR, including reloads and multiplayer. See
[deferred testing](../../DEFERRED-TESTING.md#fabric-cover-detection-unreleased).
