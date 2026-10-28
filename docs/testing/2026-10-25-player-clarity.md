# Player clarity and reversible themes — October 25, 2026

Both maintained targets remain version 1.0.1. Verification used generated music
and disposable game profiles capped at 60 FPS, with VSync off. Personal music,
foobar data and real game profiles were unchanged.

## Result

| Target | Automated tests | Packaged native checks | Copy checks / passages |
| --- | ---: | ---: | ---: |
| NeoForge 26.3 / Java 25 | 989 passed | 23 passed | 20 / 167 |
| Fabric 1.20.1 / Java 17 | 999 passed | 23 passed | 20 / 165 |

No automated failures, errors or skips. Seventeen new tests per target cover
isolated theme drafts/ownership/commit, unrelated save snapshots, invalid/deleted
targets, readable condition summaries, transport priorities and countdowns. Song
table hit testing now accounts for the pinned marker space. Both full builds pass.

## Behavior and native checks

- The playing playlist marker follows the song's source ID, including when a
  different playlist is selected for editing. Song markers use the current track
  ID. Pause bars replace notes while paused; selection highlights remain separate.
- A shared song action button appears on the hovered/focused visible row. Its
  left-click and Shift+F10 open the existing context menu and preserve an existing
  multi-selection. Native checks disable both selected songs and undo that edit.
- Active condition summaries display Minecraft clock windows and configured
  radii. Native checks verify the night window and a 64-block playback area.
- The transport uses cached scheduler/client state, with no new catalog scans.
  Native checks exercise a live gap countdown and an empty matching playlist's
  reason. Automatic countdowns account for both gap and buffer deadlines; queue
  timing retains its existing buffer bypass. This pass does not change scheduling.
- Theme rows and valid editor values preview through an isolated ThemePreview.
  Settings and saved definitions stay untouched until Apply. Native checks verify
  named theme creation, paging without saving, editor Cancel, invalid-draft Cancel,
  menu hiding/suspension, reopening with the draft retained, and Apply returning
  directly to Display while committing both appearance and the named theme.
  A built-in preview followed by Cancel preserves the committed custom colors.

The first packaged pass had 22 checks per target. After tightening off-screen
focus handling and using the Fabric key event's Shift modifier, the final pass
adds Shift+F10 and passes all 23. Rendered source/song markers, opaque song menu
and empty-playlist feedback were inspected. The final packaged JARs exactly match
the copies staged in the native profiles.

## Evidence and reproduction

- [NeoForge results](evidence/2026-10-25-player-clarity-neoforge-results.txt)
- [Fabric results](evidence/2026-10-25-player-clarity-fabric-results.txt)

After the NeoForge runtime build:

```powershell
python testing/native/prepare_fixtures.py --workspace
.\gradlew.bat -p testing/native runClient -Pworkspace -Pclarity
```

From the Fabric project:

```powershell
.\gradlew.bat -Psmoke -PpackagedSmoke -Pclarity runClient
```

Screenshots remain in ignored profile directories under
`ui-followup-clarity-*.png`. Broader GUI-scale/font/controller/narration review,
actual Explorer theme sharing, theme rename/delete UI, hardware and multiplayer
combinations remain separate coverage. Older broad Workspace/FabricSmoke theme
traversals need their former autosave expectations updated before full reruns;
the dedicated clarity mode exercises the current Apply/Cancel behavior.

## Runtime JAR SHA-256

- NeoForge: `ca901c5b5a6cd72bf2b12d6611d9dc7f6cd6b0d0bdc507f73313702fc8d454ec`
- Fabric: `0ce4c4ee4ba7fe1f925627e8eabde3f82666c368bdd59c85ed57cfc1cbcf180c`
