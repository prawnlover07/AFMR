# Song actions spacing — October 25, 2026

Song metadata and headers now stop four virtual pixels before the reserved
22-pixel actions button. The button stays at the right edge, before the vertical
scrollbar. Both targets use the metadata viewport for clipping; Fabric previously
clipped at the full row edge, allowing text underneath the button. Horizontal
scrolling retains access to clipped columns. No playback or version changes.

Both full builds pass: 999 NeoForge and 1,009 Fabric regression tests, zero failures,
plus their network and UI-copy checks. The packaged Fabric clarity run passes
23 in-game checks, including focus, Shift+F10, left-click, multi-selection and Undo.
Its song/menu screenshots were inspected. The disposable profile starts at 60 FPS
with VSync off. This pass did not repeat native NeoForge checks or exercise every
custom font/column combination.

Command: `gradlew.bat -Psmoke -PpackagedSmoke -Pclarity runClient` in the Fabric port.
Preserved result: [Fabric checks](evidence/2026-10-25-song-actions-spacing-fabric-results.txt).

JAR SHA-256:

- NeoForge: `6fc245e2b817961b0af12d0ddcfb704ccb8f1e58718e8b44fb4189bf5834354f`
- Fabric: `e4099c81698dd69ae6227645cb584768609337a71adea7495b85a6ba289b4666`
