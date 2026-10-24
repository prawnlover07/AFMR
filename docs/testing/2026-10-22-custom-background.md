# Custom menu backgrounds — October 22, 2026

Implemented in NeoForge 26.3 and Fabric 1.20.1, with the development version kept
at 0.6. Open **Display → Menus → Custom background…**. PNG/JPEG selection uses the
existing native Explorer dialogs; the source stays read-only. AFMR atomically
stores a normalized PNG under its config folder. Image on/off, Fill/Fit/Stretch,
separate image opacity, remove and reset controls apply across world/server profiles.

## Automated results

Both targets pass **676 JUnit tests**, zero failures/errors/skips. The 15 added
cases per target cover:

- Centered Fill source cropping, Fit gaps, Stretch, extreme aspect ratios,
  tiny viewports and invalid dimensions.
- JPEG import/PNG normalization, read-only originals, aspect-preserving reduction
  from 4096×1024 to 2048×512, transparency and content-based format detection.
- Missing, unsupported, oversized, corrupt/truncated images and declared pixel
  bombs; failed import preserves the previous managed image without temp files.
- Snapshot/disk persistence, global appearance across profile switches, defaults
  and invalid setting bounds.

`MenuBackgroundProbe` passes **5 checks per target**, with generated images only:
source preservation, capped dimensions, managed-copy reload, a processed wallpaper
larger than native scratch space, and 100 actual native stream decode/close cycles.
Each cycle verifies that pixel access fails after close and LWJGL scratch pointer/
frame values remain stable. NeoForge used Java 25; Fabric used the installed
Minecraft Java 17 runtime. No game window, world or personal image was opened.

Full `build sourceZip --offline` succeeds for both targets. The native probe is a
test helper, excluded from the shipped mod JAR. Fabric main classes target Java 17.
Source packaging also excludes nested logging folders initialized by the probes;
generated logs remain ignored, alongside caches, worlds and private library state.

## Rendering and ownership

Minecraft 26.3's `GuiRenderState.blurBeforeThisStratum()` marks the current stratum
as the first one after blur. `MusicScreen` and `LibraryScreen` draw the image, advance
to a new stratum, then request the existing topmost-screen blur. Panels and controls
are extracted afterward. The existing fully opaque panel optimization is retained.
This order was checked against the local Minecraft implementation; it is not a
claim of screenshot/pixel verification.

Fabric draws before menu panels and before the workspace coordinate transform.
Image work is one asynchronous job at a time; sources are limited to 20 MiB / 64
megapixels, subsampled before full allocation and reduced to a 2048-pixel longest
edge. The client owns one cached texture and cached placement. Replacement, disable,
resource reload and shutdown release the texture. Missing images are attempted
once, not every frame. Re-enable or choose again to retry. Neither target uses
the stack-backed byte-array native decoder for these images.

## Remaining native UI verification

Explorer behavior, actual blur pixels, GUI scaling, live resource reload and GPU
texture lifetime with render mods remain in [DEFERRED-TESTING.md](../../DEFERRED-TESTING.md).
The headless probe validates native decode/close only. It does not prove GPU cleanup
under a running modpack, menu layout correctness or a general memory-leak absence.
