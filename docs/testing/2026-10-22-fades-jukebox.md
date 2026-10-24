# Independent fades and jukebox verification — October 22, 2026

These changes remain under Unreleased in the current **0.6** development build.
The shared source also includes the separately reported combat-targeting change;
the workspace run does not validate live hostile-mob targeting.

## Measured results

- The main build passed **491 automated tests**, zero failures/errors. Eight new
  independent-fade tests cover asymmetric gains, overrides/previews, either fade
  at zero, trigger transitions, the separate combat-death policy, bounds, defaults,
  snapshots and disk persistence.
- Fifteen new jukebox tests cover Off and all three policies, a full gap after
  the record, immediate buffer bypass, Play on trigger interaction, early record
  stop, independent manual pause ownership, blocked starts, disabling/resetting
  the option, finish protection, queued Next/preview, death guards and persistence.
- The packaged JAR passed **162 isolated in-game assertions**, zero failures.
  The helper drove actual Minecraft screens and integrated-server commands in the
  disposable workspace profile using generated WAVs/artwork. The 27 new assertions
  supplement the earlier 135-check workspace run.
- Timing exposed both fade fields, blocked invalid values, retained hidden drafts
  and saved Fade in **0.4 seconds** independently of Fade out **1 second**. The
  Jukebox selector exposed Off and all three behaviors.
- Actual jukebox blocks received actual music-disc items. Each enabled policy
  detected record playback and requested audio pause after fade-out. Resume kept
  the playback sequence; after-silence began a full five-second gap after record
  removal; immediate started another song despite a five-second context buffer.
- Two overlapping records retained the hold after the first was removed and
  released it after the second. Muting Minecraft's Records category prevented
  suppression. The helper restored its isolated volume/focus settings afterward.
- The Timing screenshot was visually inspected at an 854×480 window. Labels,
  independent fields and the Jukebox selector fit without overlap. Screenshots
  remain under ignored `testing/native/run/workspace/screenshots/`.

[Raw workspace results](evidence/2026-10-22-fades-jukebox-results.txt).

Tested JAR SHA-256:
`8e9b16b4632aff214281db619b3fd187b07f62e149b750d8a89d9e5380eb5ed8`.

## Limits

The new workspace checks inspect GameAudio pause intent and retained playback
identity. They do not measure native OpenAL sample offsets, physical input or
subjective listening quality. Natural record completion, range crossings, lifecycle
and resource reload combinations, dedicated servers and third-party sound mods
remain in [deferred testing](../../DEFERRED-TESTING.md). No personal music, foobar
settings or personal Minecraft profile was modified. The helper is test-only and
is not bundled in AFMR.
