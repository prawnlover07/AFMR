# Fabric artwork stack-space crash — October 22, 2026

Development version remains **0.6**. The change is recorded under Unreleased.

## Evidence and cause

The user supplied a Homestead 1.3.7 archive and Prism launch log. The archive
targets Minecraft 1.20.1; the running instance uses Java 17.0.15 with a 12,000 MiB
maximum heap. The log's failure is `OutOfMemoryError: Out of stack space`, at
`MemoryStack.nmalloc` → `NativeImage.read(byte[])` → AFMR `NowPlaying.loadCover`.
It does not report `Java heap space` or show cumulative heap growth.

Read-only inspection of the artwork on the last playing track produced a
**139,885-byte, 256×256 PNG**. Minecraft 1.20.1's byte-array overload tries to copy
all those bytes onto LWJGL's native scratch stack. The standalone reproduction
has **65,536 bytes** available, so that particular image cannot fit. A delayed
failure when that track plays can resemble a session memory leak, but the exact
failure reproduces on the first decode of that cover without the modpack.

The cached Minecraft bytecode confirms that the InputStream overload instead
reads into a native heap buffer and frees it on success and failure. Minecraft
26.3's byte-array overload already has a native-heap fallback for large inputs;
the NeoForge code is therefore left unchanged. This investigation does not infer
that no other modpack memory problem can exist.

## Change

The Fabric HUD and editor use `NativeImage.read(InputStream)` directly. Images and
textures are also closed if creation/registration fails before ownership transfers
to Minecraft's texture manager. Normal texture release already removes the map
entry and closes the dynamic texture/pixels; that lifecycle is retained.

There is no stack-size/heap-limit increase, music-file rewrite or texture wrapper.
The music widget, artwork dimensions and playback semantics are retained.

## Verification

- Fabric `build` passes **661 automated tests**, zero failures/errors/skips.
- The windowless `ArtworkMemoryProbe` runs Minecraft's actual native image/STB
  decoder and passes **9 checks** on Java 25, then **the same 9 checks** using the
  Homestead instance's Java 17 executable. These repeated checks are not 18 distinct
  test cases. The standalone Loom runtime uses LWJGL 3.3.2; the supplied game log
  uses 3.3.1. Both refer to the same 1.20.1 image-loader implementation.
- A generated **225,230-byte, 256×256 PNG** and the actual **139,885-byte** cover
  reproduce the exact old `Out of stack space` failure.
- **1,000** large-image stream decodes/close cycles preserve dimensions, allow
  pixel reads while open, reject pixel reads after close, and leave the native
  scratch pointer/frame unchanged after every iteration.
- **100** invalid-image stream attempts throw I/O errors without scratch growth.
- The real triggering artwork succeeds through the stream decoder; its scratch
  usage also returns to the starting value.
- The probe creates no window/world/Minecraft singleton. Only the explicitly
  selected real music file is read. Personal music, the modpack archive, foobar
  and live-game configuration are not written. Generated inputs and private raw
  logs/path files remain in ignored local storage.

The [sanitized Java 17 probe output](evidence/2026-10-22-fabric-artwork-memory-results.txt)
retains numeric results without personal track names or filesystem paths.

## Limits

This proves the reported native scratch-capacity failure and successful native
decoding with the replacement API. It is not a full Homestead session/GPU texture
soak test or a measurement of all native/heap allocations in the modpack. Those
remaining checks are in `DEFERRED-TESTING.md`. The personal instance was not launched
or modified; install the rebuilt Fabric JAR there for the follow-up gameplay test.
