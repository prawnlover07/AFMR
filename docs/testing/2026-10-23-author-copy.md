# Reviewed author copy

October 23, 2026; NeoForge 26.3 and Fabric 1.20.1; version remains 0.6.

Animal edited 87 of the 156 writing-file passages: 61 revised explanations and
26 cleared ones. The remaining 69 keep their original wording by choice. Animal
confirmed that cleared paragraphs should remove the explanation.

## Review and implementation

Preserved the author's concise wording and made 17 small proofreading or
behavior corrections. The meaningful behavior corrections are:

| Passage | Correction |
| --- | --- |
| Combat state | Attack/damage starts combat in singleplayer; mob targeting sustains it. |
| Players only | "Use PvP instead of mob combat" also covers the not-in-PvP state. |
| Thunderstorm | Dimension-wide storm state, independent of biome or player height. |
| Fit songs to time window | Qualifies automatic songs; manual playback keeps its bypass. |
| Background image formats | Qualifies WebP as static and retains the GIF frame limit. |

Other corrections fix `inked-folder`, `a details`, a missing comma after
`playlists`, repeated `as well as`, `starttime`/`endtime`, sentence capitalization,
ReplayGain/GIF capitalization and the misplaced `Range:` prefix. The author's
reference metadata and utility wording are unchanged.

Blank paragraphs now compile to empty language entries. Their metadata and IDs
remain so text can be restored later. A fully cleared template can omit its
runtime placeholders; a nonempty template must retain them. Native tooltip call
sites check the resolved component before attaching an explanatory tooltip.
They keep the existing widget and its utility label, including the ordinary
tooltip that reveals a clipped label. On-screen blank guidance draws no text.
There is no runtime wrapper, Markdown reader or per-frame tooltip checking.

## Verification

- Both `build sourceZip` runs pass. JUnit reports: **778 NeoForge / 788 Fabric**,
  zero failures, errors or skips. Native-language probes run in those same builds.
- `testUiCopy` passes **20 checks per target**, including empty/whitespace passages
  and cleared templates with placeholder metadata. Missing IDs, target
  restrictions and nonempty-placeholder validation still apply.
- The real Minecraft `Language` and `Component` pipeline exactly reproduces all
  **150 NeoForge / 148 Fabric entries** from the reviewed file. **21 / 23** are
  cleared on those targets (26 distinct cleared passages). Unicode, percentages,
  quotes, paragraphs and dynamic arguments also pass.
- Every Java string literal and translation-key reference matches the captured
  source before blank-tooltip handling. Utility labels/messages are preserved.
- Both JARs contain exactly one generated English resource, matching the reviewed
  copy plus all 15 unchanged key-binding labels. Both source ZIPs contain the
  current writing file, compiler and utility input; excluded private/generated
  folders and backups remain absent.

Temporary review snapshots, expected-output manifests and a windowless probe live
under ignored `work/`. No personal profiles, music, foobar files, desktop windows,
native pickers, audio playback or system clipboard were used. On-screen wrapping,
narration and blank-tooltip visual checks remain for the focused final game pass
in [DEFERRED-TESTING.md](../../DEFERRED-TESTING.md).
