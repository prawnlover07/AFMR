# Optional AFMR server companions — October 25, 2026

Development version remains 1.0.1. Both maintained targets now support loaded structure
boundaries/catalogs and event-started mob-target retention through optional server
companions. Server JARs exclude client, UI, playback/library and decoder classes.

## Measured verification

| Target | Pure regressions | Packet tests | Headless server checks | Companion connection | Vanilla fallback | Server JAR bytes |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| neoforge | 999 | 7 | 13 | 12 | 8 | 22053 |
| fabric | 1009 | 7 | 13 | 12 | 8 | 21456 |

Both full builds also pass 20 copy compiler checks per target. Headless probes load
the packaged companions and use explicitly retained chunks, loaded structure-start
fixtures, no-AI/no-gravity mobs and fake-player packet sinks. They verify per-player
targets, target switching/removal, invalid sender scope/radius rejection, catalog
and bounds, maximum radius and unchanged loaded-chunk count during observation.

Separate actual socket connections use packaged clients in disposable profiles,
one client at a time, capped at 60 FPS with VSync off from startup. They verify
channel negotiation, remote bounds/catalog, target-only peace, event-started combat
retention past a one-second quiet period, separate PvP state, target loss/countdown
and retained structure data. Unmodified vanilla servers verify connection without
channels, unavailable structures and the original event timeout. No personal worlds,
music or launcher profiles are changed.

Repeatable commands: [server companion fixtures](../../testing/SERVER-COMPANIONS.md).
Generated full logs and result files stay under ignored work/run directories.

## Additional field coverage

These controlled fixtures do not establish natural generation/discovery across
modpacks, large-population/many-client latency or broad reconnect/dimension/death
combinations with native audio. Physical UI input, subjective audio transitions
and malicious wire traffic beyond the packet decoder bounds remain deeper tests.
