# Directional playlist transitions — October 23, 2026

This is an Unreleased addition to the working **1.0.1** source on NeoForge 26.3
and Fabric 1.20.1. The existing `v1.0.1` tag and friend ZIPs remain preserved.
Concurrent custom-widget-background development shares this working folder, so
combined-suite totals below describe particular runs rather than a frozen release.

## Automated validation

**24 new `TransitionRulesTest` cases per target** pass. They cover directional
finish/interruption, inheritance, actual source ownership through retained songs,
explicit interruption overriding both finish switches/shared-track protection,
independent buffer/out/gap/in timing, zero values, Play on trigger precedence,
subsequent-song timing after a short pair buffer, natural completion and elapsed
silence on destination changes. They also cover manual transport, queue/preview/
override precedence, pause clocks, combat death Fade/Keep, copied profile/snapshot
ownership, deletion cleanup, disk persistence, portable reference remapping,
omitted destinations and malformed data/unknown-field rejection.

The full-backup roundtrip includes a rule in both copied profiles. Full builds
and `sourceZip` pass: the latest reviewed runs report **832 NeoForge / 838 Fabric
JUnit tests**, zero failures/errors/skips, plus **20 UI-copy compiler checks each**.
There are **156 / 154 applicable UI passages** at this point. NeoForge's total
includes four concurrent background tests; the transition-only total would be
828. Generated source archives were checked for Git/cache/work/output/native-world
and private-backup exclusions. No format version or compatibility layer was added.

## Native validation

The user authorized disposable game windows. The focused helper passes **17 checks
per target** with generated WAVs and actual Minecraft screen events/SoundManager
playback. It opens the two-column menu and rule editor, rejects invalid timing,
saves a Finish rule and an independent Interrupt rule with buffer/out/gap/in,
then checks the actual Dark exploration song is retained when Underground wins.
Changing the winner to Day outside interrupts the actual Dark source despite
entry/exit protections, waits the custom buffer, fades, waits the custom gap and
starts native incoming audio using its custom fade-in. Inspector reports the
active rule. Next bypasses rules, and resetting one pair retains the other.

Sanitized helper outputs:
[NeoForge](2026-10-23-playlist-transitions-neoforge.txt) and
[Fabric](2026-10-23-playlist-transitions-fabric.txt).
Screenshots were inspected at **854×480** for both editors. They retain synthetic
bright menu/background fixtures from prior checks; screenshot capture may show
the preceding frame's draft while assertions verify the applied values.

Commands, with the local Java/Gradle cache configured:

```powershell
.\gradlew.bat build sourceZip --offline
.\gradlew.bat -p ports/fabric-1.20.1 build sourceZip --offline
python testing/native/prepare_fixtures.py --workspace
.\gradlew.bat -p testing/native runClient -Pworkspace -Pfeatures101 -Ptransitions --offline
.\gradlew.bat -p ports/fabric-1.20.1 -Psmoke -PpackagedSmoke -Pfeatures101 -Ptransitions runClient --offline
```

## Remaining scope

Physical input/narration, extreme scales, live server/combat/jukebox chains,
device changes, large libraries, subjective audio quality and prolonged modpack
sessions were not measured. Native checks assert actual sound activity and timing,
not microphone output or sample-accurate fade gain. The generated configuration
changes do not touch personal music, foobar data or personal game profiles.
See [deferred testing](../../DEFERRED-TESTING.md) for the deeper combinations.
