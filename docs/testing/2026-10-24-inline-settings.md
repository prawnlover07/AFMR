# Inline playlist settings — October 24, 2026

Version remains 1.0.1 on NeoForge 26.3 / Java 25 and Fabric 1.20.1 / Java 17.
The isolated profiles and generated fixtures were used throughout. No personal
music, foobar data, real game profile or external account was modified.

## Result

- NeoForge: `build` passed 930 tests, 20 copy-compiler checks and 165 validated
  passages. The packaged native UI run passed **30 checks / 0 failures**.
- Fabric: `build` passed 940 tests, 20 copy-compiler checks and 163 validated
  passages. The packaged native UI run passed **29 checks / 0 failures**.
- Both `sourceZip` tasks passed. Runtime JARs exclude test helpers and the removed
  volume/silence screens. The isolated profiles used the exact packaged JARs.

The UI runs exercised effective automatic selection, independent toggles and
Edit targets, negative sky-condition restoration, inline area capture, radius
validation, invalid drafts through hide/resize, inline custom gaps and live
volume settings, independent entry/exit finish flags, trigger bypass, dedicated
rename, actual right-click actions, duplication with disabled song assignments,
tap cancellation and successful one-second condition replacement scoped to one
playlist. Empty-list hit testing rejects clicks before native widget sound handling.
NeoForge's container itself reports a hit as consumed even when its child rejects
it; its additional check tests the empty child's input directly.

Rendered default, area and playback screenshots were inspected on both targets.
Full condition selectors and nested groups remain available, with group names
separate from playlist renaming. The workspace retains song tables/columns,
transport, normalization, imports, repair, profiles and the lower-left library.

Development checks caught and fixed NeoForge's SDL right-button numbering and a
hold-completion refresh invalidating an iterator. The final hold checks exercise
that refresh path successfully. The UI changes do not alter scheduler predicates
or playback policy; this pass does not claim a fresh full audio-device, modpack,
real multiplayer or friend usability study. The older deep native scenarios
remain follow-up coverage as recorded in DEFERRED-TESTING.md.

## Evidence and reproduction

- [NeoForge native results](evidence/2026-10-24-inline-settings-neoforge-results.txt)
- [Fabric native results](evidence/2026-10-24-inline-settings-fabric-results.txt)

After building NeoForge and staging its packaged JAR:

```powershell
python testing/native/prepare_fixtures.py --workspace
.\gradlew.bat -p testing/native runClient -Pworkspace -PinlineSettings
```

From the Fabric project:

```powershell
.\gradlew.bat -Psmoke -PpackagedSmoke -PinlineSettings runClient
```

The separate helper is absent from production JARs. Screenshots and world/profile
files remain in ignored test directories.

## Packaged JAR SHA-256

- NeoForge: `076dadada23b48cfaee7eb50e30335ce84edb90a9aa2e0a74691caf3dea7c672`
- Fabric: `3fd2d406bc976570c66e2ce4e980ce1e8cdfa839ba611518995393f455f9c814`
