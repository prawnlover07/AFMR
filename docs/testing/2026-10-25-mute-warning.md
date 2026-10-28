# Minecraft mute warning without automatic pause — October 25, 2026

On both maintained targets, Minecraft Music/Master volume no longer controls
AFMR pause state. The workspace's **Music muted in Minecraft** warning remains.
Playback/scheduling continue silently; manual pause stays independent. The former
one-second mute controller, reset/ownership hooks and its six tests per target
were removed. There is no persisted setting migration or version bump.

## Build-only verification

The user explicitly chose **Finish builds only; leave game checks for later**.
No Minecraft/Explorer windows were opened for this change.

- Full regression builds pass **958 NeoForge / 968 Fabric JUnit tests**, with zero
  failures/errors/skips and **20 UI-copy compiler checks per target**. Reports
  use ignored `work/mute-warning-checks` to avoid concurrent test-output locks.
- Both `sourceZip` tasks pass. Runtime/source ZIP inspection finds no removed
  `MutedMusicPause` class or tests and no Git/cache/work/output/game-profile files.
- NeoForge `testing/native:compileJava` and Fabric `compileSmokeJava -Psmoke`
  pass for the revised actual-audio checks. Compilation is not native validation.
- The warning predicate and both workspace render call sites remain intact. The
  Fabric stream-retention hooks remain necessary to continue at exact zero
  source/listener gain without stopping AFMR's stream. Their comment now describes
  silent continuation instead of delayed pause. Normal sounds retain their stop
  behavior; OpenAL work remains on Minecraft's sound executor.

## Deferred native checks

`-PmuteWarning` selects a focused sequence in the existing UI follow-up helper.
It checks Music and Master mute beyond the former one-second delay, native
position advancement, exact zero source/listener gain, unchanged song identity,
warning visibility, unmute continuity and manual pause/resume. Fabric additionally
checks ordinary sounds still stop on Master mute and remain stopped on unmute.
This sequence remains **unrun**. Earlier October 24 native mute results describe
the superseded automatic-pause policy and are preserved as historical evidence.

Later authorized runs must retain the **60 FPS cap**. Song completion, gaps/new
starts while muted, device/reload changes and longer modpack sessions remain in
[deferred testing](../../DEFERRED-TESTING.md). Existing release packages/tag and
personal music, foobar data and game profiles were not modified.
