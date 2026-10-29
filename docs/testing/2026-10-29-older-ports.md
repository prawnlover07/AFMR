# Minecraft 1.18.2 / 1.19.2 ports — October 29, 2026

AFMR remains version 1.1.1. Targets are Fabric Loader 0.19.5 with Fabric API
0.77.0 for each game version, Forge 43.5.0 for 1.19.2, and Forge 40.3.0 for
1.18.2. Production bytecode and native runs use Java 17; pure tests use Java 25.

## Measured client checks

| Check | Fabric 1.19.2 | Forge 1.19.2 | Fabric 1.18.2 | Forge 1.18.2 |
| --- | ---: | ---: | ---: | ---: |
| Pure regressions | 1,023 | 1,013 | 1,023 | 1,013 |
| Packet/audio components | 14 | 14 | 14 | 14 |
| UI-copy compiler | 20 | 20 | 20 | 20 |
| Packaged now-playing shortcut | 16 | 16 | 16 | 16 |
| Transformed-game surface cover | In pure suite | 10 | In pure suite | 10 |
| Navigation/transport/draft retention | 52 | 52 | 52 | 52 |
| Mute warning/silent progression | 24 | 24 | 24 | 24 |
| Packaged PCM/compressed decoders | 7 | 7 | 7 | 7 |
| Full UI/fonts/backgrounds | 49 | 49 | 49 | 49 |
| Playback/library integration | 83 | 83 | 83 | 83 |
| Saved-state restart | 17 | 17 | 17 | 17 |

All final client runs above report zero failures. Decoder cases cover MP3,
Vorbis, FLAC, WAV, 24-bit FLAC/WAV and float WAV through actual nested production
libraries, producing signed 16-bit PCM. Native playback exercises local WAV and
Minecraft-resource streams, seeking, queue/history, pause/mute, fades, profiles,
repair, sharing, conditions and shutdown persistence. This is not a prolonged
modpack certification or a claim of physically listening to every compressed format.

Clients ran sequentially at 60 FPS with VSync off in disposable profiles. Only
generated fixtures were modified; personal music, foobar files, saves and launcher
preferences stayed untouched. Screenshots and raw results are retained locally
under `work/porting-2026-10-29` and ignored `run-*` folders.

## Direct port changes

- Older drawing/input uses native PoseStack, Font and GuiComponent APIs. Authored
  descriptions and placeholders belong to AFMR controls; no GuiGraphics/Tooltip
  facade was added. Minecraft 1.19's native rebuild boundary retains hidden draft
  widgets. Minecraft 1.18's native clear boundary handles its different lifecycle.
  Reopen checks verify controls, invalid drafts, focus and selected menu font.
- Minecraft's Gson 2.8.9 cannot restore Java records. The five stored immutable
  value types use ordinary final fields instead. Tests run against that actual
  Gson version, with explicit validation preserving malformed-identity rejection.
- Pre-1.20 fonts lack the reference provider. Generated font definitions copy the
  active default providers after the imported TTF, with bounded background reads.
  Provider changes invalidate the generated definition without a format version.
  Native tests verify distinct imported metrics and Greek/CJK fallback for both
  menu and widget fonts. The initial full UI pass caught this unsupported provider
  before release; corrected final passes are 49/0 on every target.
- Forge keeps the two shared-package audio SPI artifacts in one game-layer JAR
  with merged provider lists and original notices. Module-layer discovery is
  tested with the packaged decoders, not inferred from plain JVM results.

## Older combat attribution and companions

These Minecraft versions do not send the newer attributed damage event to clients.
The owner chose direct local attacks only on servers without a companion. Incoming
and confirmed ranged-hit events work in singleplayer and with the matching
companion. Default threat filtering excludes passive animals and falls/fire.

The server observes successful LivingEntity damage and sends only the recipient's
dimension/entity scope, direction and opponent category. It sends no opponent
identity, coordinates, music, library or settings. Pending hits merge per direction
and player; the server drains at most 64 clients per tick, expires pending entries
after 40 ticks, and clears disconnected/stopped sessions. Forge checks the receiving
connection before applying an event on the client.

Final repeated headless condition fixtures pass 13 cases per target. All four targets
pass 30 actual companion-connection checks each, including incoming hostile,
passive exclusion, fall exclusion, PvP, confirmed ranged hits and combat disabled.
The simulated-player fixture caught a null network connection; the server now
ignores sending to unconnected simulated players while still reporting their hit
to the real victim. All four vanilla-server connections pass eight fallback
checks each, including keeping the client at the required startup FPS cap.

Expanded coalescing/load, simultaneous real clients, disconnect/dimension/respawn
combinations and third-party combat implementations remain field cases in
`DEFERRED-TESTING.md`; code review is not counted as a measured network result.

## Startup observations

Several development launches exited with Windows `0xc0000005` before their checks
began. Application Error events identify Minecraft's `lwjgl.dll`: offsets `0x10ae`
with 3.2.2-SNAPSHOT and `0x10c5` with 3.3.2-snapshot. No JVM fatal-error report was
created. Unchanged retries completed the native cases above. No game-native,
driver or personal launcher overrides were applied. These events are retained as
an unresolved environment/library observation, not an AFMR crash fix or evidence
that startup issues are impossible on other machines. The
[older upstream native thread-detach code](https://github.com/LWJGL/lwjgl3/blob/3.2.2/modules/lwjgl/core/src/main/c/common_tools.c)
is an investigation reference; the exact cause has not been established.

## Source and distribution checks

Each older target passes a full build from the clean Git export and another
`build sourceZip` from its own extracted standalone source archive. Those archives
contain their wrapper, shared UI writing/compiler inputs, tests and icon without
requiring the parent repository. Source/privacy audits exclude Git data, caches,
personal configuration/music, worlds, logs and old workspaces.

Clean release client and companion class payloads match the production JARs used
by the native checks. Java 17 bytecode, metadata, original notices and exact icon
bytes are checked. Minimal companions exclude client code, music/library data and
nested audio/image libraries. Fabric distributions retain thirteen original
dependency source archives; Forge retains those plus the two MixinExtras archives.

The combined local packages are under `outputs/1.1.1/multi-version`, each with
direct JARs, client/companion distribution ZIPs, a source ZIP and SHA-256 manifest.
See [the combined report](2026-10-29-multi-version-release.md). Existing baseline
release tags and earlier packages remain unchanged. No public upload was made.
