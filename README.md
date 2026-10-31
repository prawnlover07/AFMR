<!-- Placeholder README assembled from animal's CurseForge text plus GitHub-specific sections. To be rewritten in animal's own words. -->

<p align="center">
  <img src="src/main/resources/icon.png" alt="AFMR icon" width="128">
</p>

<h1 align="center">animal's final music replacer</h1>

<p align="center"><b>Turn your music into a soundtrack that reacts to the world</b></p>

<p align="center">
  Minecraft 26.3 · 1.21.1 · 1.20.1 · 1.19.2 · 1.18.2 &nbsp;|&nbsp; Fabric · NeoForge · Forge &nbsp;|&nbsp; MIT license
</p>

---

**animal's final music replacer** (AFMR) automatically plays your own music files based on what's happening in the world. You can set music to play in a variety of situations, including current biome, time of day, health, combat status and more. Want *cozy music* that plays when you're close to home, *eerie music* for caving or even a *theme song* for when your best friends (or enemies...?) are near? **AFMR lets you do this, and much more.**

## 🎵 Features

- **17 conditions** to choose from (time, location, light level, held item...)
- **Combat and death music**
- **Mixes with Minecraft's music** or replaces it, your choice
- **Add songs or folders** from your computer, or import a playlist from **foobar2000**
- **Works in multiplayer**, including vanilla servers
- **Custom silence, fades and transitions** between songs and playlists
- Some songs louder than others? **Normalize volume** with a click
- **Different profiles** between worlds and servers
- **Share playlist setups** with a friend (they bring the music)
- **Now playing widget** shows song title and info in-game
- **Custom themes and colors**, including fonts, images and even GIFs
- Reorganize your music folders? **AFMR can find your songs again**

## 📥 Download

Download AFMR from **[CurseForge](https://www.curseforge.com/minecraft/mc-mods/afmr/files)**. Pick the file that matches your Minecraft version and loader. Each build only works on its own version.

| Minecraft | Loader | Also needs | Java |
| --- | --- | --- | --- |
| 26.3 | NeoForge 26.3.0.90+ | — | 25 |
| 26.3 | Fabric Loader 0.20.1+ | Fabric API | 25 |
| 1.21.1 | NeoForge 21.1.265+ | — | 21 |
| 1.21.1 | Fabric Loader 0.19.5+ | Fabric API | 21 |
| 1.20.1 | Forge 47.4.0+ | — | 17 |
| 1.20.1 | Fabric Loader 0.19.5+ | Fabric API | 17 |
| 1.19.2 | Forge 43.5.0+ | — | 17 |
| 1.19.2 | Fabric Loader 0.19.5+ | Fabric API | 17 |
| 1.18.2 | Forge 40.3.0+ | — | 17 |
| 1.18.2 | Fabric Loader 0.19.5+ | Fabric API | 17 |

- **Client-side mod:** servers and friends don't need it
- **Optional server companion:** structure detection and combat target tracking only work in single-player. Server owners can use the companion to track that data in multiplayer. Without the companion, combat is detected by a simple timer instead of hostile mob aggression.
  - On **1.18.2** and **1.19.2** servers without the companion, combat only starts when *you* attack something.
- **Tested on Windows.** Mac and Linux not tested.

## 🚀 Getting started

1. Install **Forge**, **NeoForge** or **Fabric** (see the table above)
2. Put the AFMR `.jar` file in your `mods` folder
3. Launch the game and join a world
4. Press **M**
5. Select a playlist & click **Add music**
6. To hear a song right away, right-click the selected song and click **Preview song**, or the skip button at the bottom

> **Note:** there are 10-20 minutes of silence after each song by default

Enjoy :)

For everything else, see the **[user guide](docs/USER-GUIDE.md)** and the **[installation checklist](docs/INSTALLATION.md)**.

## ❓ FAQ

**Does this mod include any music?**

Nope! Use your own files.

**Which audio formats work?**

MP3, OGG Vorbis, FLAC, WAV. No Opus files.

**Can I use Spotify/YouTube/streaming services?**

Nope! Local files only.

**Will it affect my files?**

AFMR only reads files; your music will never be changed or deleted.

**Will it break my world?**

AFMR only changes music; your world stays as it is.

**Does it work with other mods?**

I hope so! [Report a bug](https://github.com/prawnlover07/AFMR/issues/new/choose) if you run into any problems.

## 🐞 Bugs & ideas

Found a bug or have an idea? [Open an issue](https://github.com/prawnlover07/AFMR/issues/new/choose). For bugs, press **M → Playback status → Copy diagnostic summary** and paste it into the form; it leaves out your file paths and song names.

## 📚 Documentation

- **[User guide](docs/USER-GUIDE.md):** imports, playlists, conditions, controls, settings and troubleshooting
- **[Installation checklist](docs/INSTALLATION.md):** picking the right build and getting your first song playing
- **[Changelog](CHANGELOG.md):** what changed in each version
- **[Supported versions](docs/PORTING.md):** every Minecraft version and loader, with test reports

## 🛠️ For developers

AFMR is written in Java. The NeoForge 26.3 build lives at the repository root, and the other nine builds are under [`ports/`](ports). Each one builds on its own with Gradle.

- **[Build and development setup](DEVELOPMENT.md):** Java/Gradle requirements and commands
- **[Developer guide](docs/DEVELOPER-GUIDE.md):** a tour of the source, playback, audio and storage
- **Project records:** [product decisions](PROJECT.md) · [validation](VALIDATION.md) · [remaining tests](DEFERRED-TESTING.md) · [backlog](BACKLOG.md) · [performance](PERFORMANCE.md)

## 📄 License & credits

AFMR is free and open source under the **[MIT license](LICENSE)**. You're welcome to modify and share it, as long as you keep the license notice.

AFMR bundles these open-source libraries, under their own licenses (LGPL, BSD and MIT):

- **MP3SPI, JLayer, VorbisSPI, JOrbis and Tritonus Share:** MP3 and OGG playback
- **jFLAC:** FLAC playback
- **jaudiotagger:** song titles, artists and album art
- **TwelveMonkeys ImageIO:** WebP background images
- **MixinExtras:** Forge builds only

See [THIRD_PARTY.md](THIRD_PARTY.md) for details. Full license texts and notices are included in every download.
