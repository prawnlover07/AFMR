# Play immediately on join — October 23, 2026

Implemented under Unreleased on both maintained targets. Version remains **0.6**.
Timing's new switch defaults off and belongs to the current profile. An actual
world/server join activates its bound profile before arming first playback. The
first eligible automatic selection skips initial silence and the context buffer,
then uses the existing song-start/fade path. Later gaps and context changes retain
normal timing. No eligible playlist means waiting for one, not forcing a song.

## Measured validation

- **718 NeoForge / 728 Fabric automated tests**, zero failures/errors/skips.
  Sixteen new tests per target cover the off default, fresh-context priority,
  initial gap/buffer bypass, normal fade-in, later gaps and non-retriggering ticks,
  reset versus explicit rejoin, ordinary context-change timing, delayed eligibility
  and disabled/manual-only/empty entries, pause/current context, death guards,
  each jukebox release policy, manual Skip/Next/queue/preview/override priority,
  frequency rescheduling, time-window fitting, decoder backoff and toggle lifecycle.
  Snapshots, disk persistence, profile copy/fresh/switch behavior and numeric
  defaults are covered; existing profile reflection and full-backup round trips
  now exercise the new setting at a non-default value.
- The packaged NeoForge build passes **10 focused native checks**. The generated
  fixture leaves Default off and binds a separate enabled profile to the disposable
  world, with 600 seconds of silence and a 60-second buffer. A real join starts the
  correct dimension-conditioned song exactly once during the startup observation.
  Timing's switch, direct defaults access, bounds/non-overlap, invalid numeric
  draft protection, live save, hide/reopen and later normal Skip all pass.
- The packaged/remapped Fabric build passes **14 focused native checks**, including
  initialization/title-screen checks, the same actual profile-before-playback join,
  UI/validation/retention checks and normal later Skip. The test samples a real
  OpenAL channel and verifies playing state, positive gain and no AL error.
  An initial helper check sampled before its asynchronous channel observation;
  adding a probe warm-up interval made the native measurement reliable without
  changing runtime code. The final run has no failures.
- Timing screenshots at 854×480 were inspected on both targets: the new left-hand
  switch and the defaults/Combat settings/Save & back footer remain separate,
  readable and inside the viewport. The generated fixtures and helpers are excluded
  from runtime JARs; personal music, settings and real game profiles were untouched.

Evidence: [NeoForge](evidence/2026-10-23-play-on-join-neoforge-results.txt),
[Fabric](evidence/2026-10-23-play-on-join-fabric-results.txt).

Tested runtime SHA-256:

- NeoForge: `9424beea22cc4634faeffc2a5c77dd5e974a7f7778e3343f0665b291b1be3a72`.
- Fabric: `edbf6310f98fa8c429f395723b539c5fed54f730117b3fbbaca8082730f636aa`.

## Lifecycle and limits

`PlaybackEngine.joinedWorld` arms one pending bit only at session entry. Profile
changes, respawn, dimension changes and resets do not arm it. Reset clears it;
turning the option on mid-session only changes the policy for the next join.
Explicit controls and enabled jukebox holds cancel a pending request. Ordinary
pause/death/fit guards still run; a failed attempted start keeps its normal
backoff. The existing numeric Minecraft defaults button preserves this independent
setting, as it preserves other playback switches.

Unit tests cover scheduler rules, not network lifecycle or audio quality. These
native runs join actual disposable singleplayer worlds; dedicated-server joins,
rapid server switches, real respawn/dimension/reload combinations, large libraries,
slow initial chunk/structure availability, unusual GUI scales and physical input
remain deeper checks in [deferred testing](../../DEFERRED-TESTING.md).
