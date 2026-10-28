# Optional AFMR server companion probes

Build both maintained targets before running these separate test-only mods. They
load the **packaged server companion**, bind only to loopback, create disposable
fixtures under their own ignored `run/` folders and stop automatically. They are
excluded from the AFMR client and server JARs. No personal worlds or music are used.

For the complete finite Windows batch, use `python testing/run_server_companions.py`
with Python 3 and the usual Java 25 / Gradle environment. It builds both targets,
runs the headless and actual connection checks against companion and unmodified
vanilla servers, cleans up its own processes, writes the focused verification
report and verifies source packaging. Generated logs/summary stay under
`work/server-companion-checks`. The local imported JDK/cache are fallback paths.

For individual runs from the repository root, with the usual Java 25 / cached Gradle environment:

```powershell
.\gradlew.bat -p testing/server-neoforge runServer --offline
.\gradlew.bat -p testing/server-fabric runServer --offline
```

Each performs 13 service checks against a real dedicated server: loaded structure
start bounds/catalog, sender isolation, actual mob targets/target switching/removal,
invalid dimension/entity/radius rejection, maximum radius and unchanged loaded-chunk
count during observation. The fixture explicitly retains its chunk and disables mob gravity so chunk lifetime
and falling cannot invalidate target-switch checks. Fake player packet sinks capture replies; these checks
alone do not test socket negotiation, real client state or audio. Results are
`run/server-companion-results.txt`; failures fail the Gradle task.

For actual client/server connections, start a server in one terminal and the
matching capped client in another, one target at a time:

```powershell
.\gradlew.bat -p testing/server-neoforge -Psocket runServer --offline
.\gradlew.bat -p testing/native -PserverCompanion runClient --offline

.\gradlew.bat -p testing/server-fabric -Psocket runServer --offline
.\gradlew.bat -p ports/fabric-1.20.1 -Psmoke -PpackagedSmoke -PserverCompanion runClient --offline
```

The loopback ports are 25585 (NeoForge) / 25586 (Fabric). The server grants operator
access only inside this disposable fixture, carves a small test room, inserts a
structure-start fixture and supplies a no-AI zombie target. It stops on the client's
`stop` command, with a three-minute server-tick limit as a fallback. Both clients
start at 60 FPS with VSync off in their own profiles. The fixture checks socket
negotiation, remote bounds/catalog, target-only peace, event-started retention past
the quiet period, separate PvP state, target loss/countdown, retained structures
and the FPS preferences. It writes `server-companion-network-results.txt` in the
selected client profile and closes. These are controlled context checks, not
subjective audio, physical input or broad modpack validation.
