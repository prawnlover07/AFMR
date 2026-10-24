# Menu color customization — October 22, 2026

Implemented under Unreleased on NeoForge 26.3 and Fabric 1.20.1. Both retain
version **0.6** while development is in progress.

## Measured results

- NeoForge passes **684 automated tests** and Fabric passes **694**, zero
  failures/errors. Eight new
  palette tests cover exact original shade preservation, independent family edits,
  channel clamping, independent RGB/alpha, ASCII hex validation, missing/invalid
  settings, owned snapshots and disk persistence, plus reset independence.
- The packaged NeoForge JAR passes **29 focused in-game checks**, zero failures.
  The helper opens Display → Menus → Menu colors and drives actual screen events
  in the disposable workspace profile. It checks field bounds, invalid input
  blocking Back/page changes, immediate previews, all ten editable families,
  retained hidden drafts and recovery/reset without changing other appearance.
  A simulated screen-height reflow verifies keyed draft retention and unchanged
  palette values when the number of fields per page changes.
- NeoForge GUI render-state inspection confirms the chosen purple background,
  panels and buttons carry 50% alpha, the selected tab uses its custom selection
  color, and the highlight/text remain fully opaque. The checks cover the color
  editor, workspace and Timing screen; these screens remain non-pausing.
- The packaged/remapped Fabric JAR passes **51 smoke checks**, zero failures,
  including eleven new color/reflow checks. These cover opening the actual editor,
  invalid RGB, live preview, retained hidden drafts, appearance independence,
  keyed drafts during screen-height reflow, non-pausing workspace and both reset paths. The existing generated playback,
  decoder, pause/volume/queue/import and screen checks also pass in that run.
- Color-editor and themed workspace screenshots from both targets were visually
  inspected at 854×480. The paged field layout stays separate from navigation;
  backgrounds, buttons, selection and labels follow the palette. NeoForge retains
  its configured blur; Fabric has no blur. Native widget skins and warnings remain
  separate. Screenshots remain in each ignored disposable test profile.

The first NeoForge helper run completed its render checks but attempted to click
an already-selected Menus tab during reset navigation. The helper was corrected
to recognize that retained page; the repeat run passed its assertions. No
production change was needed for that fixture correction.

Final review additionally found a real resize issue in the first editor draft:
the shared screen restored text fields by position when page capacity changed.
The color editor now owns drafts keyed by color family and handles reflow directly;
validation also reveals an invalid draft that moved off the current page. The new
native reflow assertions pass. A concurrently added Fabric cave-heightmap fixture
also failed during a shared-source build; its corrected fixture subsequently
passed, accounting for the ten additional Fabric tests in the final suite.

Evidence: [NeoForge results](evidence/2026-10-22-menu-colors-neoforge-results.txt),
[Fabric results](evidence/2026-10-22-menu-colors-fabric-results.txt).

Tested runtime JAR SHA-256:

- NeoForge: `77a39d7a21cd2669fd024729afb35d1ecc9c96624c3c083cd3f514372adc78ea`.
- Fabric: `2886e0a90869215cd1b06cb35b31d5584afa237aecaf8f5e0271eef46002ce51`.

## Implementation and limits

`MenuColor` stores ten configurable bases; rendering shifts original shades by
their family's channel offsets and clamps them. Default values reproduce the old
palette exactly. Explicit families are stored in cached surface geometry and used
at direct drawing call sites. Per-frame resolution performs fixed arithmetic and
map lookup, without file work or allocations. Palette snapshots own their map;
appearance is global across profiles and excluded from playlist exchange.

RGB values preview when valid; incomplete text stays a draft. Text/highlight alpha
is independent of panel alpha. Reset colors preserves opacity/blur/image/HUD
settings; full menu reset restores the palette too. Native Minecraft text fields,
option widgets and tooltips retain their own skins, and errors retain warning
colors. No new format version or compatibility layer was added.

Physical input, unusual scales, low-contrast user palettes, real restart/profile
switches and custom-image/render-mod combinations remain in
[deferred testing](../../DEFERRED-TESTING.md). The new NeoForge run focuses on menu
rendering; it does not repeat the entire historical playback/workspace suite.
Fabric's smoke run does not measure every pixel's alpha. All state-changing checks
used generated fixtures and disposable worlds/profiles; personal music, images,
foobar settings and game profiles were untouched. Test helpers are excluded from
both runtime JARs.
