# AFMR: remaining deeper testing

Current release: **0.5.2**. Automated baseline: **227 passing tests**. On October 21, 2026, the user authorized native/UI testing; **81 native playback checks and 6 persistence checks were validated**, plus the UI checks below. No AFMR defect was reproduced. The existing release JAR is unchanged.

See the [session report](docs/testing/2026-10-21.md) for methods, results, test-fixture corrections and performance limits. No further test run is scheduled.

## Completed in the October 21 session

- [x] Native local/vanilla pause position retention, same-source resume, delayed stream start, frozen fade gains and silent gap, queued Next/override, preview paused at stream startup, global sound resume, actual Minecraft pause-menu interaction, other ambient audio, reload/restart reset.
- [x] Next and Inspector controls; both override modes with Automatic off; natural one-song return; empty/deleted/all-disabled targets; physical controls and chat/menu guards.
- [x] All four entry/exit combinations, source ownership through A-B-C, returning to the retained source; two four-song shuffle cycles and boundary repeat avoidance.
- [x] Live day/night, enclosed darkness, lit Underground and changed Y cutoff, captured Home, Nether/End toggles, and combined exact biome/time/dimension/Y/temperature/cover custom rules.
- [x] Combat trigger API and forced Combat with global combat off; death Cut, Fade and Finish, paused death fade, new-music blocking while dead and respawn.
- [x] Seven packaged audio-format variants, intentional asynchronous decoder failure and Retry failed, 70,000 unrelated library entries with indexed playback resolution.
- [x] Native Explorer pickers, 5,000-file scan with progress and zero reported issues, generated .fplite import into a manual playlist with duplicate/missing/reuse report, source checksum preservation.
- [x] UI playlist creation, condition pages/current biome, priority, separate entry/exit switches, reset despite invalid text, unchanged unrelated saved configuration, and persistence of these choices/scanned entries/Next-Pause keys.

## Remaining complex cases

These are additional scenarios, not failures found in the session. Pure tests cover many of their scheduler/storage/filter rules; the items below retain the deeper native/hardware combinations.

- [ ] **Transition and paused-context combinations:** Pause during a live context buffer while moving between environments; verify inspector selection and remaining buffer together. Exercise queued controls with repeated context changes, A-B-C where B's exit cannot protect A's song, no matching playlist, and manual skip/death interrupting an existing transition. Test explicit disconnect/reconnect while paused (reload and restart already checked).
- [ ] **Override mutations and every slot:** Turn Automatic off during an active held/one-song override, release/cancel mid-fade, retrigger/switch slots rapidly, and remove the last available song during decoding. Exercise all eight physical slots, later binding pages, missing bindings, Return to automatic, and actual Stream Deck hardware input.
- [ ] **Live shuffle membership changes:** Add/disable/remove songs partway through a cycle; shared songs across playlists; leave/return continuity; one-song pools; failed files repaired on disk and retried. Check those mutations together with overrides and explicit previews.
- [ ] **Combat input variants:** Actual attack-key interaction against entities and real damage/absorption/hurt-time triggers; attack/damage switches independently; custom combat-driven manual playlists; new-music-while-dead enabled; death during an existing fade or partially completed fade-in.
- [ ] **Condition boundaries/worlds:** Wrapped custom time ranges, inclusive negative/exact Y and temperature edges, water, conflicting filters, Home world/dimension/radius boundaries, modded biome registration/offline IDs and multiplayer synchronization. The combined positive custom-rule case and both dimension darkness switches already pass.
- [ ] **Full library and audio lifecycle:** Longer sessions with the user's real library and unusually tagged files, many songs assigned to active playlists, import/rescan during gameplay, launch rescans and inaccessible folders, sound-device changes, volume interactions and subjective ReplayGain comparisons. Existing ReplayGain decoding/tag regressions pass. The 70k check measured only indexed scheduling, not overall FPS.
- [ ] **UI/import extremes:** Resize/GUI-scale combinations beyond the tested small window, long names/tooltips, many custom playlists, later keybind pages, repeated .fplite import preserving disabled assignments, cancelled/native picker edge cases. Basic picker, scan, import reporting, custom conditions and reset/persistence already pass.

Continue using the isolated workspace profile and generated fixtures for state-changing checks. Personal music and foobar files remain read-only. The test-only helper, detailed logs and fixtures are retained under `work/overnight`; it is not included in the release mod.

## Earlier user report

The user reported that most manual checks of 0.5.0 worked to expectations, without itemizing cases. The October 21 results above provide the later concrete checks.
