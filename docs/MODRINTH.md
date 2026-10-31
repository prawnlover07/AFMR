# Modrinth page fields

Copy-paste sheet for creating AFMR's Modrinth project, in the order Modrinth asks.
Values match the CurseForge page and the 1.1.1 files in `outputs/1.1.1/multi-version/`.
Checked against Modrinth's public API on October 30, 2026: the slug was free, and
`26.3`, `1.21.1`, `1.20.1`, `1.19.2` and `1.18.2` are all valid game versions.

## 1. Create project

| Field | Value |
| --- | --- |
| Name | `animal's final music replacer` |
| URL (slug) | `afmr` (same as CurseForge; free on Modrinth as of October 30, 2026) |
| Visibility | Public |
| Summary | `Turn your music into a soundtrack that reacts to the world` |

Modrinth search matches the summary, so a more literal version may find more
players, for example: `Plays your own music files based on biome, time of day,
combat and more`. Pick whichever sounds like you.

## 2. Project settings

| Field | Value |
| --- | --- |
| Icon | Same 400×400 PNG as CurseForge |
| Categories | **Utility** (Modrinth has no music or sound category) |
| Client-side | **Required** |
| Server-side | **Unsupported** |
| License | **MIT** |
| Issues | `https://github.com/prawnlover07/AFMR/issues` |
| Source | `https://github.com/prawnlover07/AFMR` |
| Wiki | `https://github.com/prawnlover07/AFMR/blob/main/docs/USER-GUIDE.md` |
| Discord | (leave empty) |
| Donation | Ko-fi: `https://ko-fi.com/animal12345` |

The client JAR stays **server-side unsupported** even though the optional companion
exists. If the client were marked "optional" on servers, server-pack tools could
install the client JAR on a server, and the client and companion must never share
an instance (they use the same mod ID).

## 3. Gallery

Upload the same screenshots as CurseForge. Mark your best one **Featured**; it
becomes the image in search results. Each image can have a title and a short
description (e.g. "The music menu", "Now playing widget").

## 4. Description

Copy all of [MODRINTH-DESCRIPTION.md](MODRINTH-DESCRIPTION.md) into the Description
tab (open it as plain text, select all, copy). It is the live CurseForge description
converted to Markdown, with the screenshots it already uses. The only change: bug
reports point to GitHub Issues, since Modrinth has no comment section.

## 5. Versions

Create one version per row. Every row uses the same settings:

- **Release channel:** the same one you chose on CurseForge (Release or Beta).
- **Primary file:** the plain client JAR from `outputs/1.1.1/multi-version/<folder>/`.
  Not the zips, `-source.zip` or the `AFMR-1.1.1-...` folders.
- **Loader and game version:** exactly one of each, as listed. Don't add versions or
  loaders that weren't tested (for example NeoForge on the Forge 1.20.1 file).
- **Dependencies:** Fabric rows add **Fabric API** (`fabric-api`) as *Required*.
  Forge and NeoForge rows have none.

| Folder | Version number | Version name | Game version | Loader | Fabric API |
| --- | --- | --- | --- | --- | --- |
| `NeoForge-26.3` | `1.1.1+26.3-neoforge` | `AFMR 1.1.1 (NeoForge 26.3)` | 26.3 | NeoForge | — |
| `Fabric-26.3` | `1.1.1+26.3-fabric` | `AFMR 1.1.1 (Fabric 26.3)` | 26.3 | Fabric | Required |
| `NeoForge-1.21.1` | `1.1.1+1.21.1-neoforge` | `AFMR 1.1.1 (NeoForge 1.21.1)` | 1.21.1 | NeoForge | — |
| `Fabric-1.21.1` | `1.1.1+1.21.1-fabric` | `AFMR 1.1.1 (Fabric 1.21.1)` | 1.21.1 | Fabric | Required |
| `Forge-1.20.1` | `1.1.1+1.20.1-forge` | `AFMR 1.1.1 (Forge 1.20.1)` | 1.20.1 | Forge | — |
| `Fabric-1.20.1` | `1.1.1+1.20.1-fabric` | `AFMR 1.1.1 (Fabric 1.20.1)` | 1.20.1 | Fabric | Required |
| `Forge-1.19.2` | `1.1.1+1.19.2-forge` | `AFMR 1.1.1 (Forge 1.19.2)` | 1.19.2 | Forge | — |
| `Fabric-1.19.2` | `1.1.1+1.19.2-fabric` | `AFMR 1.1.1 (Fabric 1.19.2)` | 1.19.2 | Fabric | Required |
| `Forge-1.18.2` | `1.1.1+1.18.2-forge` | `AFMR 1.1.1 (Forge 1.18.2)` | 1.18.2 | Forge | — |
| `Fabric-1.18.2` | `1.1.1+1.18.2-fabric` | `AFMR 1.1.1 (Fabric 1.18.2)` | 1.18.2 | Fabric | Required |

The `+26.3-neoforge` part keeps each version number unique, so launchers and update
checkers can tell the files apart.

### Server companions

In each version, after the primary client JAR, upload that folder's
`animals-final-music-replacer-server-<loader>-<version>-1.1.1.jar` as an
**additional file**. Make sure the client JAR stays marked **primary**: the Modrinth
app only installs the primary file, so players never get the companion by accident.
Server owners download the companion by hand from the version's page.

### Changelog (same text for every version)

```markdown
First release on Modrinth. Also available on CurseForge.

- Now available for Minecraft 26.3, 1.21.1, 1.20.1, 1.19.2 and 1.18.2, on Fabric and on NeoForge or Forge
- New **Show now playing** key (unbound by default, under Controls → AFMR) reopens the now playing widget for the current song
- Optional server companion for every version (structure detection and combat target tracking on multiplayer servers)
- New green music-note icon
```

## 6. Before submitting for review

- [ ] Each version has exactly one loader and one game version, and the right JAR
      (the file name says the loader and version).
- [ ] All five Fabric versions list Fabric API as required.
- [ ] Each version has the client JAR as primary and the matching `-server-` JAR
      as an additional file.
- [ ] Description screenshots show in the preview.
- [ ] Featured gallery image is set.
- [ ] After approval, add the Modrinth link to the README next to CurseForge.
