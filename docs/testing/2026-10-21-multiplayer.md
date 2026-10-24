# Vanilla multiplayer validation — October 21, 2026

The current AFMR build passed **27 native multiplayer checks** against an unmodified
Minecraft Java **26.3** dedicated server. AFMR and the separate test helper were
installed only in the **NeoForge 26.3.0.90 / Java 25** client. The server had no mods.
The full build passed **253 regression tests** with no failures or errors.

The user authorized launching the isolated client/server and accepted the Minecraft
EULA for the disposable server. Authentication was offline and the server bound only
to `127.0.0.1:25575`. Tests used a generated flat world and quiet generated WAV audio.
Personal game profiles, music, and foobar data were not used.

## Results

| Area | Verified against the vanilla server |
| --- | --- |
| Connection | Join and reconnect, with no integrated server or server-side AFMR |
| Local and vanilla music | Native OpenAL streaming, local Next/repeat avoidance, vanilla resource preview |
| Pause | Native source position retained, server continues, night selection updates while music is paused |
| Overrides | Automatic-off playlist plays through a held override; logout clears it |
| Default context | Day/night, synchronized solid cover, enclosed darkness, lit Underground |
| Home | Captured server address and location, correct priority, stable connection identity across dimensions |
| Custom conditions | Combined biome, temperature, time, dimension, Y, and open-sky filter from synchronized data |
| Water and dimensions | Underwater observation, Nether/End whole-dimension darkness switches |
| Combat and death | Actual server damage, quiet-period expiry, client attack-observation API, death Cut, new-song blocking, respawn |
| Logout | Playback, pause, override, and world context clear immediately; automatic music returns after reconnect |
| Normalization | No decoding while connected; waiting scan completes after disconnect and publishes its measurement |

The [raw check results](evidence/2026-10-21-multiplayer-results.txt) preserve native
source state/position and observed context for every assertion. The final test client
closed itself, and the disposable server process was stopped afterward.

Tested runtime JAR SHA-256:

```text
697381bc4366b84ddb36803488585385aeca69f3f064ddb41029bd325a9fda3a
```

The staged test JAR and `build/libs/animals-final-music-replacer-0.5.2.jar` matched.
This is an unreleased build of the current source, including the loudness scanner;
it does not replace the historical validation record for the imported 0.5.2 baseline.

## Changes and fixture corrections

AFMR already had a client-only entrypoint, client-only event subscribers and sound
mixins, no custom network payloads, and no synchronized gameplay registrations.
The audit confirmed that biome temperature, clocks, light, cover heightmaps, player
health, and the other rule inputs are available through vanilla client data.

Logout cleanup previously depended on a later client tick observing a missing world,
and left the last context/vanilla pool cached. AFMR now handles NeoForge's client
logout event immediately and clears the full session state, with the null-world
tick path retained as a fallback. Saved settings, music, and captured areas remain.

The first fixture disconnected the client level incorrectly: it cleared the client
world without first closing its connection. Reconnect then encountered the test
server's one-player limit. The fixture was corrected to follow vanilla's disconnect
sequence; this was a test error, not an AFMR connection defect. Immediate session
cleanup assertions failed before the production change and passed afterward.
The fixed 25-check run passed; the final run added the two normalization assertions
and passed **27 / 27**.

## Scope and performance decision

This was a real network/client/audio integration test on loopback, not just scheduler
unit coverage. It does not establish online account authentication, proxy/Realms
behavior, modded-server compatibility, physical attack-key input, UI layout, or every
timing combination. The earlier [native/UI session](2026-10-21.md) and
[remaining test list](../../DEFERRED-TESTING.md) retain their separate scope.

The user prioritized gameplay performance. Loudness analysis therefore still waits
while connected: its background decoding consumes local CPU and disk bandwidth,
and this run did not benchmark that workload during active gameplay. Existing
ReplayGain and cached normalization adjustments remain available online. Analysis
can finish at the title screen, or while singleplayer is paused.

Use the [reusable multiplayer fixture](../../testing/native/README.md#vanilla-multiplayer-suite)
to repeat the checks. The runtime JAR excludes the test helper; source packaging
excludes generated profiles, server worlds, logs, caches, and personal data.
