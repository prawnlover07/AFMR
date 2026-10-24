# List-edge seam repair — October 22–23, 2026

The reported strips of game/custom image came from two missing pieces of coverage:
rounded foreground scissoring trimmed row backgrounds at fractional editor scales,
and the horizontal scrollbar painted only its movable range while its parent
cutout reserved the complete table width.

Both targets now draw row backgrounds once over exact viewport geometry before
foreground clipping, clamp the final partial row to the bottom, and paint the
entire horizontal scrollbar strip. No extra base layer compounds transparency.

## Measured results

- The old packaged NeoForge build reproduced **14 failures / 2 passes** across four
  canvas sizes (427×240, 500×271, 853×479, 1280×681), opacity 100%/50% and default/all
  song columns. The native helper projects actual colored render-state rectangles,
  including their scissors, and requires exactly one background layer along all
  three list perimeters and the reserved horizontal strip.
- The corrected build passes **16/16** cases. The final runtime JAR repeats all
  sixteen successfully during the cross-version backup run. Alpha checks preserve
  the chosen opacity. Canvas changes use actual screen reflow/extraction, not
  physical window resizing.
- The packaged Fabric run passes two seam checks: a contrasting 1×1 magenta custom
  background is installed behind opaque panels; direct framebuffer samples find
  **zero exposed magenta pixels** at priority/library/song-list perimeters or across
  the entire reserved horizontal scrollbar strip. The fixture has 100 additional
  local entries, 20 playlists and all columns visible at 854×480.
- Workspace screenshots from both targets were inspected. The Fabric screenshot
  retains the deliberately red background selected during preceding picker checks;
  it contains no magenta leaks. The full Fabric run passes 83 assertions.

Evidence: [before](evidence/2026-10-22-menu-seams-before-results.txt),
[initial correction](evidence/2026-10-22-menu-seams-neoforge-results.txt),
[final NeoForge](evidence/2026-10-23-menu-seams-final-neoforge-results.txt),
[Fabric framebuffer and smoke](evidence/2026-10-23-full-backup-fabric-smoke-results.txt).
Final JAR hashes are recorded in [the backup report](2026-10-23-full-backup.md).

Physical resize/GUI scale changes, third-party shader/render mods and every
background/opacity combination remain deeper checks. These were generated-only
isolated profiles; the supplied screenshot was used as the bug reference.
