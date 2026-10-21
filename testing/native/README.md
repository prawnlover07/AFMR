# Native AFMR fixture

This is the test-only mod used for the October 21, 2026 validation of AFMR 0.5.2,
relocated into the permanent repository. It loads the packaged AFMR JAR and measures
actual OpenAL source state, offsets and gain through Minecraft's sound executor.
It is a separate Gradle project and is never bundled into the AFMR runtime JAR.

The native suite changes the isolated game configuration, kills/respawns the test
player, teleports between dimensions, and builds a disposable room. Use a disposable
world and coordinate desktop testing with the user. Do not use a personal game profile.

## Prepare

From the AFMR repository root, with Java 25 and Python 3 available:

```powershell
.\gradlew.bat build
python testing/native/prepare_fixtures.py --scan-files 5000
```

This generates quiet WAVs, a .fplite fixture and optional scan files, stages the root
build's JAR in `testing/native/run/mods`, and initializes missing test settings. Existing
test options/configuration are preserved. Other installed AFMR versions are reported
rather than removed. All generated files/worlds/results are ignored by Git.

Supply `--world <path>` to copy an existing disposable world to `run/saves/New World`
when that destination does not already exist. The original is only read. The imported
workspace's `work/overnight/run/saves/New World` is an available previously used test
world on this computer. Otherwise, create a new test world named **New World** in the
isolated client; the helper starts after joining a world and waiting for startup.

## Compile or run

```powershell
.\gradlew.bat -p testing/native compileJava
.\gradlew.bat -p testing/native runClient
```

`compileJava` does not open Minecraft. `runClient` opens the isolated profile and
quick-plays New World. The helper drives real runtime APIs and local server commands;
physical input, Explorer dialogs and visual checks are separate manual tests.

Results are `testing/native/results/native-results.txt` and `complete.txt`. A fresh
playback run has 81 feature checks. Extra persistence checks run if the UI Test Playlist
fixture is present. An intentionally invalid MP3 exercises error quarantine/recovery;
that expected decode error must not be mistaken for an unexpected failure.

## Saved UI checks

For the six persistence checks from the original session, use the isolated UI to:

1. Import all 5,000 scan fixtures into the library.
2. Create **UI Test Playlist**, capture **minecraft:dappled_forest** in its biome filter,
   enable both entry/exit finish switches and move it to priority #1.
3. Restore Minecraft playback defaults. Keep test Next/Pause mappings **N/P**.
4. Pause AFMR and close the client normally.

Then run `.\gradlew.bat -p testing/native runClient -PkeysOnly`. This checks those six
saved behaviors plus fixture presence, without resetting the configuration or running
the playback suite. The exact biome expectation reflects the original disposable world;
adapt it explicitly if using a different world. Saved key names are compared rather
than old GLFW key codes, because Minecraft 26.3 uses SDL input.

Full playback runs reset custom test playlists and music before starting. Preserve
the UI fixture if you intend to run the separate persistence test afterward. See the
[session report](../../docs/testing/2026-10-21.md) and [remaining cases](../../DEFERRED-TESTING.md).

The helper and fixture preparation compile/check after relocation. The historical
native/UI results are the recorded October 21 session; relocation alone is not a new
native test run. Screenshots and validated case names are tracked with the report.
