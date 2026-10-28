# Riding-animal condition — October 24, 2026

Implemented under Unreleased on NeoForge 26.3 and Fabric 1.20.1. Development
versions remain **1.0.1**; existing numbered release archives/tag are unchanged.

## Behavior

Name & conditions includes a paged **Riding animal** row cycling Any, Riding and
Not riding. Any is the default and does not participate in condition matching.
Riding checks whether the player's direct vehicle is an instance of Minecraft's
Animal class. Horses, donkeys, mules, llamas, camels, pigs, striders and modded
Animal subclasses qualify, including idle riders and passengers. Boats/minecarts
and standing near an animal do not qualify.

Both runtimes read the client's existing vehicle reference into the immutable
context snapshot. There is no entity traversal, registry lookup, server query or
new background work. Reset/disconnect contexts explicitly report false.
Multiplayer uses the vehicle state already synchronized by Minecraft.

The ordinary condition bitset, All/Any/Not groups, enabled branches, playlist
priority and transition controls apply. Copies, profiles, disk saves, portable
playlist sharing and full backups retain the configured state. Inspector and
copied diagnostics include the current boolean observation and condition state.
An empty author-editable hover entry was added to UI-COPY.md; existing authored
prose remains intact and no new explanatory paragraph ships by default.

## Measured verification

- Both full offline builds succeed: **878 NeoForge / 888 Fabric tests**, zero
  failures/errors, plus **20 UI-copy compiler checks per build**.
- **17 new tests per target** cover default/Any, Riding and Not riding, All/Any,
  nested Not/disabled branches, null normalization, independent clones/snapshots,
  disk saves, profile copies/switches, portable import/export with nested rules,
  unknown enum rejection, complete active/inactive profile backup round trips,
  priority/empty/disabled-song fallback, manual overrides, ordinary
  buffer/fade/silence on dismount, independent entry/exit finish protection, and
  Inspector/summary explanations.
- Existing Context construction sites were updated directly; no obsolete
  constructor or compatibility layer was retained.
- The initial backup test fixture omitted the required keybinding set. Adding
  the complete generated fixture fixed that test; no production backup validation
  was weakened.
- Rebuilt development JARs:
  `build/libs/animals-final-music-replacer-1.0.1.jar` and
  `ports/fabric-1.20.1/build/libs/animals-final-music-replacer-fabric-1.20.1-1.0.1.jar`.

## Remaining verification

No desktop/native gameplay run was performed. The automated tests supply the
mounted-state observation and exercise conditions, storage and scheduling; they
do not establish live vehicle packets, physical controls, real mount lifecycle,
modded entity hierarchies or native audio transitions. Check real animal mounts,
both camel seats, non-animal vehicles, mount/death/dimension/logout transitions,
UI paging and vanilla multiplayer in isolated profiles. Details are in
[deferred testing](../../DEFERRED-TESTING.md#riding-animal-unreleased-both-targets).
