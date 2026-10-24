# WebP and animated GIF backgrounds — 2026-10-23

Scope: both maintained 0.6 development targets. Add still WebP import and animated
GIF backgrounds through the existing Custom background picker. No version bump.
All inputs are generated colored rectangles; personal images/music/configuration
are not modified. Game checks use the existing disposable profiles only.

## Implementation

- PNG/JPEG/static WebP and single-frame GIF normalize to the existing managed PNG.
  TwelveMonkeys ImageIO 3.15.3 and its five required modules are bundled unmodified
  with an accompanying BSD notice; their classes target Java 8. WebP is constructed
  directly, without global ImageIO provider registration or worker-loader assumptions.
- Animated GIFs copy to `config/afmr/menu-background.gif` after complete validation.
  They loop continuously with source delays (zero/missing delays use 100 ms), alpha,
  frame offsets, palette decoding and restore-background/restore-previous disposal.
  Full configuration backups include the GIF and restore/recover both managed
  filenames. Replacing formats removes the previous owned file. Originals stay read-only.
- Inputs remain bounded to 20 MiB / 64 megapixels. GIFs allow at most 512 frames
  and 256 million source patch pixels in total. The compositor scales all frames
  to at most 16,777,216 retained pixels / 64 MiB RGBA, with edges at most 2048.
  This is a pixel budget, not a claim about total application or driver memory.
- One worker composites/prepares frames. The client uploads a bounded frame set;
  a monotonic clock selects textures without file access, decoding, texture upload
  or frame allocations during drawing. Disable, replace, invalidation and close
  release every registered frame. Animated WebP is explicitly rejected with a GIF hint.
- Fast image imports can finish between screen ticks. The screen now observes the
  enabled state/name as well as busy state, so its on/off label and Remove control
  refresh even when it never sees the short busy interval.

## Automated verification

Both full builds pass: **735 NeoForge / 745 Fabric tests**, no failures/errors/skips.
Seventeen added cases per target cover lossy/lossless WebP, alpha and renamed
content, resizing, unsupported animated WebP preserving the prior image, exact GIF
composites compared with independent Pillow outputs, variable timing and looping,
memory resizing, excess frames, truncation, forged oversized GIF canvas, managed
format replacement/reload, invalid replacement preservation, and animated archive
export/review/install/recovery/conflict rejection.

The existing windowless native probes pass five cases each, including **100 large
PNG decode/close cycles per target**, stable scratch-stack usage and released native
pixels. The NeoForge probe extracts dependencies from the actual packaged JAR;
the Fabric windowless probe uses the development smoke classpath. These probes
establish native decoder lifetime behavior, not visual rendering.

## Packaged in-game verification

Commands after building and staging generated fixtures:

```powershell
python testing/prepare_background_images.py --stage
.\gradlew.bat -p testing/native runClient -Pworkspace -PbackgroundImages
.\gradlew.bat -p ports/fabric-1.20.1 -Psmoke -PpackagedSmoke -PbackgroundImages runClient
```

The focused game modes import WebP through the real worker, upload GIF textures,
inspect transparent/green native pixels, export the real managed GIF in a full
backup, invalidate and verify closed native pixel buffers, reload, disable/re-enable,
replace with a still and remove owned files. Original fixture inputs remain present;
unit tests also verify unchanged bytes. Fabric removes loose AFMR and external
image/audio dependency JARs from its smoke runtime, loading the packaged mod and
nested dependencies. NeoForge likewise stages the root runtime JAR separately
from its test-only helper.

Initial runs passed 14 NeoForge / 17 Fabric checks. Screenshot inspection then
caught the fast-import stale-label issue on Fabric; final runs add a direct check
for the enabled Image and Remove controls. Final results pass **15 NeoForge / 18
Fabric checks**, including the corrected controls. Preserved output:
[NeoForge](evidence/2026-10-23-background-images-neoforge-results.txt),
[Fabric](evidence/2026-10-23-background-images-fabric-results.txt).

Final runtime SHA-256 values (each matches the JAR staged in the tested profile):

- NeoForge: `233b6c4dea8f67c25433600f22100babdc0454ac49842c975a4a60c47129464c`
- Fabric: `ea2835d08ab54c68fbebf4377278fe3d2b3ac2b33cc59a21e708cae69c57d425`

Screenshots are inspected at 854×480. NeoForge shows animation below menu blur;
Fabric shows sharp frames and game through transparent pixels, without menu blur.
Successive captures show different colored frame patches behind the controls.

## Limits of verification

Physical Explorer interactions, every real-world GIF palette/profile/interlacing
variant, maximum frame-set GPU upload latency, device loss/disk failures, actual
resource-pack reload and process restart importing a GIF remain in
`DEFERRED-TESTING.md`. The tests call the same invalidation path and perform archive
installation in temporary directories, but do not claim those additional scenarios.

No test helpers, generated inputs, game data or user assets belong in runtime JARs.
Both source archives include the small owned image fixtures; the root archive also
includes their generator.
