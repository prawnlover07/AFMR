# animal's final music replacer — 1.0

AFMR is a client-side custom soundtrack mod for **Minecraft Java 26.3**,
**NeoForge 26.3.0.90+**, and **Java 25**. It combines local music and Minecraft's
music, chooses playlists from your surroundings, and gives you manual controls.
Install it on your client; no server installation is needed.
The playback implementation passed [27 checks on an unmodified vanilla server](docs/testing/2026-10-21-multiplayer.md).
See the [multiplayer guide](docs/USER-GUIDE.md#multiplayer) for behavior and scope.

## Start here

Version **1.0** is the first private release for friends, with contextual playlists,
profiles, queue/history, moved-library repair, playlist sharing, full backups,
custom backgrounds and Animal's reviewed UI text on both maintained targets.
See the [release notes](CHANGELOG.md#10--october-23-2026).

- **[User guide and quick reference](docs/USER-GUIDE.md)** — installation, your first
  song, imports, playlists, controls, settings, and troubleshooting. Share this with friends.
- **[Installation checklist](docs/INSTALLATION.md)** — choose the correct loader,
  hear the first song, and gather a diagnostic summary if something goes wrong.
- **[Developer guide](docs/DEVELOPER-GUIDE.md)** — a guided tour of the source,
  playback state, audio integration, persistence, and how to make changes.
- **[Build and development setup](DEVELOPMENT.md)** — Java/Gradle requirements,
  commands, source packaging, and repository workflow.

To install or upgrade, close Minecraft and put
`animals-final-music-replacer-1.0.jar` in the instance's `mods` folder, replacing
any older AFMR JAR. Keep one version installed. Launch, join a world, and press **M**,
or open AFMR's Config button from the Mods menu.

The editor uses a playlist workspace: priority and a music
library on the left, songs in the center, rules on the right, and persistent playback
controls below. **Add music** can import straight into the selected playlist;
the lower-left library also assigns existing songs or folders without leaving the screen.
**Timing**, **Display**, **Keys**, and **Inspector** are directly accessible.

Fresh settings use **10–20 minute gaps**. **Next** skips the wait; **Preview** plays
the selected song. M still opens/closes AFMR and retains your page and place.

The AFMR card has a plain black background with adjustable color
and opacity, and removes the redundant Now playing label. A **Hybrid toast** combines
Minecraft's frame with AFMR album art.
Open **Display** for style, scale, width,
displayed information, single-line/word-wrap layouts, scrolling and AFMR appearance
options. Hybrid and AFMR include a preview.

AFMR reads original music and foobar playlist data without modifying them. Settings
and absolute music paths are saved in the instance's `config/afmr/library.json`.
Each friend should import their own music rather than copy your configuration.

## License

AFMR's code and documentation are available under the [MIT license](LICENSE),
copyright 2026 animal. You may modify and redistribute AFMR while retaining the
copyright and license notice. [Bundled libraries](THIRD_PARTY.md) retain their own
licenses; user-supplied music and artwork are separate.

## Project references

[Accepted product decisions](PROJECT.md) · [Release notes](CHANGELOG.md) ·
[Validation](VALIDATION.md) · [Remaining tests](DEFERRED-TESTING.md) ·
[Feature backlog](BACKLOG.md) · [Performance measurements](PERFORMANCE.md) ·
[Bundled libraries](THIRD_PARTY.md)

The guides are editable Markdown in `docs/`. Update them alongside features using
[the documentation checklist](docs/DEVELOPER-GUIDE.md#keeping-documentation-current).
