# AFMR manual checks

Current build: **1.0.1**. New widget, native Music Frequency, normalization and
condition-feature checks are listed in [DEFERRED-TESTING.md](DEFERRED-TESTING.md).
The sections below retain the earlier release checklists.

**October 21 update:** The user authorized deeper testing. Native/UI checks have now been performed; see the [session report](docs/testing/2026-10-21.md) and updated `DEFERRED-TESTING.md`. The checklist and release-time deferral notes below were originally written before that session.

User follow-up on 0.5.0: most manual checks worked to expectations. A focused list of complex interactions for a later requested test session is kept in `DEFERRED-TESTING.md`; exact individual cases from the user's testing have not been itemized.

Replace the previous AFMR JAR, restart Minecraft, and press **M**. Existing music and settings should remain.

For 0.5.6, press M again to hide the menu, then reopen and confirm your page,
playlist, filters, selections and scroll positions remain. M should type normally
inside text fields. See [deferred testing](DEFERRED-TESTING.md) for the full input checklist.

## Next and pause controls (0.5.2)

Bind **Next song (skip silence)** and **Pause/resume AFMR music** in Minecraft Controls, or use the matching Inspector buttons. Both keys start unbound; existing bindings should remain saved.

1. **Next:** Try during a song, a long silent gap, a context buffer and finish protection. It should immediately choose the next eligible shuffled song from the active override, or from the current automatic choice if no override is active. Its normal fade-in still applies. Check that both override modes remain active, including with Automatic off. A one-song override should return to automatic after its replacement ends. Disabled and failed tracks should stay excluded. The old Skip binding should still use normal silence.
2. **Real pause:** Pause a recognizable section of a long song, wait, then resume. It should continue from the same position rather than having advanced silently. Other game sounds should continue. Repeat during silence, fade-in, fade-out and a context buffer; their remaining time/progress should be preserved. Check the library's paused label and Inspector explanation.
3. **Queued controls:** While paused, press Next, request an override and select a preview in separate checks. Music should remain paused until explicitly resumed. Move between environments and verify automatic selection updates while playback is held. Resume and check normal transition rules.
4. **Audio lifecycle:** Pause immediately after a song request while decoding may still be starting. Open/close Minecraft's pause menu and confirm it does not cancel explicit AFMR pause. Try local and vanilla tracks. During forced Combat, check death Cut/Fade/Finish modes and respawn while paused. Sound reload/disconnect/restart intentionally clear pause. Verify the two new keys do not activate from chat or text fields, and test the user's Stream Deck mappings when available.

The scheduler/timer behavior is covered by automated tests. Native OpenAL position retention, delayed stream start, Minecraft pause-menu resume and actual keypress/audio behavior remain on the deferred in-game checklist; no desktop automation was performed for this update.

## Automatic switch and overrides (0.5.1)

Turn **Playlist settings → Automatic** off for a default and a custom playlist. Confirm they are excluded from automatic selection but selectable through **Play one song**, **Until released**, and their assigned keybinds. The override list should mark them **auto off** without disabling its playback buttons; the inspector should distinguish the override from disabled automatic selection.

Switch Automatic off while each override mode is playing. A one-song override should finish normally and return to the current automatic choice after silence. A held override should continue through later shuffled songs. Neither action should turn Automatic back on. Individually disabled songs must still be excluded; if all songs are disabled/unavailable, the override should release. Removing the playlist should also release it. Check saved switches and keybinds after restart.

## Inspector, overrides and shuffle

1. **Inspect first:** Open **Inspector** at the bottom of the music editor while in a world. Check Playing, Song source and Automatic choice. Select a playlist and open Details; scroll/hover long lines to read current biome/dimension/time/Y/light/temperature/cover and song counts. Change your environment and return to the main inspector for live updates. Check that empty, disabled, unmatched, manual-only and lower-priority playlists have distinct explanations. Try entry/exit finish protection and watch its reason. Without a world, the inspector should ask you to join instead of showing stale conditions.
2. **Manual playlist:** Create a custom playlist, leave its conditions unset, add some songs, and turn on **Playlist settings → Manual only**. It should stay out of automatic rotation while remaining available to imports, previews and Overrides.
3. **One-song override:** With ambient music playing and long silence/finish protection configured, open **Overrides...**, select your manual playlist and click **Play one song**. The old song should start its normal fade immediately; the requested song should start without another silent gap/context buffer. Move to another environment while it plays. After the song ends or is skipped, normal silence and the current automatic choice should resume. The inspector should identify the source and override correctly.
4. **Force combat:** Select Combat with **Until released** while not fighting. It should play even without combat triggers and remain selected as the environment changes. Normal gaps apply between its songs. The override menu's **Automatic** button should release it using normal transition rules. Turning the playlist's Automatic switch off should keep the override running; removing the playlist should release it. Combat's configured death policy and the new-music-while-dead switch should still apply.
5. **Stream Deck / keybind slots:** Open **Overrides → Keybind slots**, assign a playlist/mode, and bind its **Music override slot N** under Minecraft Options → Controls → Key Binds. All new physical keys start unbound. Slot 1's initial assignment is Combat/until released. Configure the Stream Deck to send that key. Press a held-mode slot again to release it; a one-song slot should request another shuffled song. Bind Return music to automatic selection and the inspector/override menus if desired. None of the music keys should activate while typing into chat/search/name fields. Restart: assignments should persist, while an active override should not.
6. **Shuffle:** Use a small playlist with three or four songs and temporarily short gaps. Each should play once before reshuffling, with no immediate repeat across the cycle boundary. Leave and return to that playlist; its unplayed order should continue. An override for it uses the same bag. Disable an unplayed song, add another, and verify disabled songs are excluded while additions join the remaining cycle. A single-song list can repeat; explicit Play selected previews are independent of shuffle history.
7. **Errors and scale:** If an audio file fails, inspect its failed count and try **Retry failed** after correcting the underlying issue. This clears quarantine and the old error without rescanning. Check inspector/override scrolling, lower editor buttons, binding pages and hover text at your preferred GUI scale, including after resizing the window.

All native UI, keypress/Stream Deck and gameplay checks above are left to the user. This release was compiled and tested automatically without desktop automation or changes to live game settings/music.

## Entry / exit controls

Open a default and a custom playlist's **Playlist settings**. Both **Finish current song on entry** and **Finish current song on exit** should be visible, with explanations on hover. Existing finish-song choices should appear under Entry; Exit starts off. Check the lower condition/priority buttons at your usual GUI scale.

For a song playing from A while B becomes active, test these combinations (use short temporary silence/buffer/fade values if desired):

| A: on exit | B: on entry | Expected |
| --- | --- | --- |
| Off | Off | Fade out after the context buffer, then normal silence |
| On | Off | Finish A's song, then normal silence |
| Off | On | Finish A's song, then normal silence |
| On | On | Finish A's song, then normal silence |

Also check A → B → C while A's song is retained: A remains its source; B's exit toggle should not protect a song B never started. Returning to A should let that song continue. When no playlist matches, A's exit toggle still permits finishing. Manual skip and combat-death fade/cut should override protection. Restart, then use the timing-reset button and confirm both choices are retained.

No desktop input or in-game testing was automated for this release; automated checks cover the scheduler and persistence behavior above.

## Other feature checks

1. **Create and rank:** Open **Playlists → New playlist**, give it a name, and choose conditions. The six defaults remain. Move the new playlist up/down in the numbered list, then use **Edit songs** to add music or import a `.fplite` into it. Higher matching playlists take precedence; an empty or disabled playlist falls through.
2. **Combine filters:** Try Night + a temperature range that includes the live value shown in the temperature editor. All enabled filters must match. Change one bound so it excludes your current environment, then restore it. Location capture, Y, cover, light, dimension and water filters can be enabled separately. A captured location already has its own world and dimension.
3. **Timing:** Existing silence and context-buffer settings still apply. For a quicker check, temporarily use a short silence and buffer, or use **Skip / play** while idle to bypass the remaining gap. Restore your preferred timing afterward.
4. **Combat/death:** Put songs in Combat and rank it above other matching playlists. Open **Combat → Playlist settings → Combat settings**. Check attack/damage switches independently. When combat music is playing, death should immediately begin the default 1-second fade; change On death to Cut for an immediate stop, or Finish combat song to let it end. By default, no automatic new music starts until respawn. A custom playlist requiring combat uses the same death policy.
5. **Persistence:** Restart and check names, priority, filter values, on/off choices and song assignments. Test the seventh-and-later tab arrows and the scrolling import destination list if you add more playlists.

6. **Exact biomes:** Open a custom playlist's **Name & conditions → Biomes** (use the condition-page arrows). Search for your current biome, or use **Add current biome**. Select a second biome. With the playlist ranked high enough and assigned music, either selected biome may activate it, but an unselected biome should fall through. Combine with Night to confirm both filters must match. Turn the biome filter off to allow all biomes while retaining selections. With Filter on and every selection removed, Save should ask you to choose a biome or turn the filter off.
7. **Modded/offline editing:** Check the biome list in a modded world. Outside a world, saved IDs remain visible and new IDs can be entered with **Add ID**. Missing modded IDs should be retained with a note. Restart and confirm selections/filter state persist. **Clear / any** removes selections and turns the biome filter off.
8. **Playback reset:** In **M → Settings**, change all four timing fields, then click **Restore Minecraft defaults**. Check minimum 600, maximum 1200, fade 0 and buffer 0. It should work even if an unsaved timing field contains invalid text. Playlists, assignments, priority, Finish current song and combat/other options should remain as configured after restart. A currently pending silent gap retains its existing countdown; later gaps use the restored range. Restore your preferred fade/buffer afterward if desired.

9. **ReplayGain:** Preview songs whose files already contain foobar ReplayGain track tags, and compare with **Settings → More settings → ReplayGain track tags** off/on. Restart each preview after changing the toggle. Tagged loud songs should attenuate, and quiet songs with a valid peak tag can be boosted within that peak limit. Untagged songs and vanilla music should sound unchanged. No folder rescan is needed. Confirm music/master volume and fades still work; check MP3, FLAC, OGG and ID3-tagged WAV if available. Changes within a song should remain natural. ReplayGain state should survive restart and the timing-reset button.

The existing features remain covered by automated checks. Native UI and gameplay checks are left to the user; no desktop input was automated for this update.

Biome temperature is Minecraft's base biome value, not Celsius, and is separate from elevation. Equal time endpoints match one tick; a start greater than the end crosses midnight. Manual previews and already-playing ambient music do not receive the combat-specific death fade.

If playback differs from the conditions you chose, include the **ACTIVE / ENV** lines, the custom playlist's filters and priority, and any error in `logs/latest.log` when reporting it.
