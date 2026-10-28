# Playlist follow-up, custom fonts and Minecraft mute — October 24, 2026

Both maintained targets remain version 1.0.1. Testing used isolated game profiles,
a disposable world, generated WAV/PNG files and an original generated TrueType
font. Personal music, foobar data and real game configurations were unchanged.

## Results

| Target | Automated tests | Packaged native checks |
| --- | ---: | ---: |
| NeoForge 26.3 / Java 25 | 964 passed | 38 passed |
| Fabric 1.20.1 / Java 17 | 974 passed | 40 passed |

No failures, errors or skipped automated tests. Both full builds also pass 20
copy-compiler checks each, 165/163 language passages and source packaging.
The runtime JARs match those staged in the isolated profiles; neither contains
probe/smoke helpers or the test font. Source archives exclude Git metadata,
workspace copies, caches, run profiles, music and personal configuration.

The new regression cases cover playlist identity, nested metadata ownership,
coalescing, absent/off/on song assignments, priority, creation/deletion, incoming
links, separate profile timelines, bounds and conflicting edits. A throwing
assignment map verifies that settings checkpoints never traverse song assignments.
Font tests cover bounded TrueType validation, ownership/collision protection,
independent managed assets and full-backup preservation. Mute controller tests
cover delay, interruption of the delay, automatic ownership and manual pauses.

## Native input and rendering

Actual screen input checks enabled/disabled rule visibility, Undo/Redo, drag
reorder and reversal, song multi-selection/context actions and opaque controls.
All eight navigation tabs retain the workspace's physical bounds on Timing.
Rendered popup and Timing screenshots were inspected; small-window heading
scaling was corrected to avoid overlap. A generated green image is imported and
its pixels are verified behind playlist surfaces, covering the Fabric draw-path fix.

Both menu and widget accept loaded resource-pack fonts independently. The
original fixture TTF is imported into each target through the real asynchronous
file/resource-reload path; selected IDs and measured glyph widths verify use of
the loaded font. Full backups preserve both font files and settings in regression
checks. These native checks call ingestion directly; they do not open Explorer.

## Native audio

The helper samples actual OpenAL state and offsets on Minecraft's sound executor.
Music mute waits at least 0.95 seconds in the measurement, preserves the playing
sequence/channel and holds its offset while paused. Raising Music resumes that
same song and position. Master mute also pauses/resumes; a manually paused song
stays paused after mute/unmute. The workspace detects and displays Minecraft mute.

Fabric 1.20.1 ordinarily destroys channels at zero Music/Master volume. The AFMR
path retains only its stream at exact zero gain. An additional ordinary sound
check verifies Master still stops other sounds and they remain stopped on unmute.
Development runs caught the initial Master channel discard and stale bookkeeping
for stopped ordinary sounds; the final packaged checks pass both cases.

## Evidence and reproduction

- [NeoForge results](evidence/2026-10-24-ui-followup-neoforge-results.txt)
- [Fabric results](evidence/2026-10-24-ui-followup-fabric-results.txt)

After the root build:

```powershell
python testing/native/prepare_fixtures.py --workspace
.\gradlew.bat -p testing/native runClient -Pworkspace -PuiFollowup
```

From the Fabric project:

```powershell
.\gradlew.bat -Psmoke -PpackagedSmoke -PuiFollowup runClient
```

Screenshots remain in ignored profile screenshot directories as
`ui-followup-*.png`. The TTF generator is `testing/generate_font_fixture.py`.
Broader physical input/narration, third-party font/Unicode coverage, multiplayer,
modpacks, device-loss runs and friend usability remain separate testing. Older
deep helper traversals need updates for the removed toolbar actions; they were
not claimed as rerun by this focused pass. See DEFERRED-TESTING.md.

## Packaged SHA-256

- NeoForge: `1b0c73dd5b21daeb9b4617cff4991869cc330ebd1fc951bac2b1c36241b18c6d`
- Fabric: `af289249a0a4dbfd5f1b63d2a3f91593cedc41da59787ea01a562422b4ca3499`
