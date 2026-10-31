# Public page notes (CurseForge + GitHub)

Working notes for writing AFMR's public pages yourself. Nothing here is meant to be
pasted as-is — the guide, outline and prompts are scaffolding, and the fact sheet is
raw material. Write your drafts in the **Your draft** spaces (or anywhere you like),
then ask Claude for feedback on accuracy, clarity or tone.

Prepared October 28, 2026 for version 1.1.

---

## 1. How software descriptions work (a crash course)

A mod page has one job: help the right player decide, quickly, "is this for me, and
will it work in my setup?" Everything below serves that.

**Readers skim, then commit.** Most people read the summary and first few lines, look
at screenshots, and scan headings. Only interested readers go deeper. So put the most
important thing first, and make the rest easy to scan.

**Lead with what the player gets, not how it works.** "Your music follows you into the
Nether" beats "evaluates dimension-based playlist conditions". Save mechanics for later.

**Be specific.** Vague claims are forgettable (and CurseForge moderators reject them):

| Vague | Specific |
| --- | --- |
| "Improves lighting" | "Torches flicker and cast moving shadows" |
| "Lots of options" | "Pick from 12 lantern colors, or enter your own" |
| "Works with other mods" | "Tested alongside Sodium and Iris" |

*(These examples are about an imaginary lighting mod on purpose — your words for AFMR
are yours.)*

**Show, don't tell.** One screenshot of the music menu says more than a paragraph. A
short GIF or video of music changing as you walk into a cave is even better.

**Answer the practical questions plainly.** Which Minecraft version? Which loader?
Client, server, or both? Does it need other mods? Will it work on my friend's server?
Players who can't find these answers leave.

**Be honest about limits.** "Tested on Windows; Mac/Linux untested" builds trust and
saves you bug reports. Players forgive limits they were told about.

**Sound like you.** Your voice is an advantage. Short sentences, plain words, a bit of
personality. You don't need to sound like a company.

**Structure beats length.** Headings, short paragraphs, bullet lists. Avoid walls of
text and ALL CAPS. A long feature list is fine if it's scannable.

**A simple writing process:**
1. Dump everything you want to say, messily, no editing.
2. Pick the single most important idea. That's your summary/opening.
3. Group the rest under headings (use the outline in §4).
4. Cut anything a player wouldn't miss.
5. Read it aloud. Fix anything you stumble over.
6. Ask Claude to check it against the fact sheet.

---

## 2. CurseForge's rules (what moderators check)

Every project gets an automated file scan plus a human review of the name, summary,
description and images. Outcomes: approved, changes requested, or rejected. A
**rejected** project can't be republished — you'd have to create a new one — so it's
worth getting right the first time. Description problems are the most common reason
moderators ask for changes.

- **Name:** English; unique; no game name, loader, or version numbers in it.
  "animal's final music replacer" is fine. Not "AFMR for NeoForge 1.21".
- **Summary:** shows in search results, not on the page. Ideally one sentence. Should
  say what the mod does or what makes it unique — not a personal statement, and not a
  copy of the description's first line.
- **Description:** must clearly explain what the mod adds or changes, with functional
  detail. Generic lines like "changes the game" aren't enough. English first. Don't
  rely on comparisons ("like X mod"). Links (GitHub, Discord, Ko-fi) go at the bottom,
  modest in size. No external download links.
- **Avatar (project picture):** PNG, 400×400, square. Not a solid color or plain
  gradient, nothing copyrighted (no Minecraft logo, no album art you don't own). Avoid
  WebP.
- **Files:** mods must be `.jar`. Each file needs a changelog.
- **Music:** only allowed if you own it or have rights. AFMR ships no music — say so
  clearly, it helps reviewers.
- **Images:** required for visual projects; strongly recommended here since AFMR has a
  big UI. AI-edited showcase images need a visible disclaimer.

Sources: CurseForge support article 9000197279 (project guidelines), 9000197241
(creating a project), and the CurseForge blog post "How to pass moderation review".

---

## 3. Fact sheet (features verified against the 1.1 source; identity and requirements updated for 1.1.1)

Use as raw material. Pick what matters to *your* reader; you don't need all of it.

### Identity
- Name: **animal's final music replacer** (AFMR) · Author: animal · License: **MIT**
- Mod ID: `afmr` · Version: **1.1.1** (October 29, 2026)
- Source: https://github.com/prawnlover07/AFMR (public)
- CurseForge: project ID 1773357, `afmr` (https://www.curseforge.com/minecraft/mc-mods/afmr; files, screenshots
  and third-party distribution set up; public and approved by October 30, 2026)
- Bug reports: GitHub Issues with forms in `.github/ISSUE_TEMPLATE/`

### Requirements
Tested loader versions from `docs/INSTALLATION.md`. "or newer" is only stated there for
NeoForge 26.3, Fabric 26.3 and Fabric 1.20.1.

| Minecraft | Loader | Also needs | Java | Client file (`outputs/1.1.1/multi-version/`) |
| --- | --- | --- | --- | --- |
| 26.3 | NeoForge 26.3.0.90 or newer | — | 25 | `NeoForge-26.3/animals-final-music-replacer-1.1.1.jar` |
| 26.3 | Fabric Loader 0.20.1 or newer | Fabric API | 25 | `Fabric-26.3/animals-final-music-replacer-fabric-26.3-1.1.1.jar` |
| 1.21.1 | NeoForge 21.1.265 | — | 21 | `NeoForge-1.21.1/animals-final-music-replacer-neoforge-1.21.1-1.1.1.jar` |
| 1.21.1 | Fabric Loader 0.19.5 | Fabric API | 21 | `Fabric-1.21.1/animals-final-music-replacer-fabric-1.21.1-1.1.1.jar` |
| 1.20.1 | Forge 47.4.0 | — | 17 | `Forge-1.20.1/animals-final-music-replacer-forge-1.20.1-1.1.1.jar` |
| 1.20.1 | Fabric Loader 0.19.5 or newer | Fabric API | 17 or newer | `Fabric-1.20.1/animals-final-music-replacer-fabric-1.20.1-1.1.1.jar` |
| 1.19.2 | Forge 43.5.0 | — | 17 | `Forge-1.19.2/animals-final-music-replacer-forge-1.19.2-1.1.1.jar` |
| 1.19.2 | Fabric Loader 0.19.5 | Fabric API 0.77.0+1.19.2 | 17 | `Fabric-1.19.2/animals-final-music-replacer-fabric-1.19.2-1.1.1.jar` |
| 1.18.2 | Forge 40.3.0 | — | 17 | `Forge-1.18.2/animals-final-music-replacer-forge-1.18.2-1.1.1.jar` |
| 1.18.2 | Fabric Loader 0.19.5 | Fabric API 0.77.0+1.18.2 | 17 | `Fabric-1.18.2/animals-final-music-replacer-fabric-1.18.2-1.1.1.jar` |

- **Client-side.** Install on your own game. Works on ordinary vanilla servers; other
  players don't need it and each player hears their own soundtrack.
- **Optional server companions** (`animals-final-music-replacer-server-<loader>-<version>-1.1.1.jar`,
  one per target, in the same folders): only for server owners. Add multiplayer support
  for the **Structures** condition and mob-target combat tracking. Without them,
  everything else still works. On 1.18.2 / 1.19.2 servers without the companion, combat
  only starts when you attack something.
- Never install the client and the companion in the same instance.
- All decoding libraries are bundled inside the JAR; no extra downloads.
- **Tested on Windows 11.** macOS and Linux are untested (file pickers and foobar2000
  import were built around Windows).
- Only NeoForge 26.3 has the native music-toast/frequency and menu-blur integration;
  every other target uses AFMR's own now-playing card.

### What it does (grouped)
**Your own music in Minecraft**
- Plays MP3, OGG Vorbis, FLAC and WAV (incl. 24-bit and float WAV). Not supported:
  Opus, surround, DRM.
- Import files or whole folders (or drag and drop); linked folders can rescan.
- Reads music where it already lives — never copies or modifies your files.
- Imports foobar2000 playlists.
- Repairs moved/missing music, including matching by file content.
- Can mix in Minecraft's own soundtrack, or replace it entirely.
- **AFMR does not include any music.**

**Music that follows what you're doing**
- Playlists with a priority order; the highest-priority matching playlist plays.
- 17 condition types: location, time of day, dimension, elevation, cover (under a roof
  or open sky), biomes, structures, biome temperature, light level, combat, health,
  held items, game mode, riding an animal, underwater, weather, and nearby players.
- Conditions can be combined and nested into groups.
- Six ready-made starting playlists: Combat, Home, Dark exploration, Underground,
  Night and Day outside.
- "Set playback area here" to tie a playlist to a spot (like your base).

**Feels natural, not jarring**
- Silence between songs like vanilla (default 10–20 minutes, adjustable).
- Fade in/out, a short "context buffer" so walking past a cave mouth doesn't flip the
  music, and optional "let the current song finish".
- Per-playlist transition rules (e.g. how to go from Overworld → Combat).
- Songs before silence (play a few back-to-back), fit songs to a time window.
- Combat and death music handling; jukebox records can fade AFMR out.

**You're in control**
- Press **M** for the music menu. Next, skip, pause (music only), seek, queue, history.
- Eight bindable override slots to force a playlist instantly.
- Playback status screen explains why something is (or isn't) playing.
- Undo/redo for playlist edits, drag to reorder priority, settings search.

**Sound and looks**
- ReplayGain support and loudness normalization; per-playlist volume.
- Proximity volume (music changes volume near other players).
- Now-playing card with album art; style, size and info are configurable.
- Custom themes, fonts, and menu/card backgrounds (PNG, JPEG, WebP, animated GIF).

**Save and share**
- Profiles per world/server.
- Share playlist setups (`.afmrp`) and themes (`.afmrt`) with friends — each person
  links their own music files.
- Full configuration backup/transfer (`.afmrb`).

### Numbers you might use
- 1,000+ automated tests per build; multiplayer checked on an unmodified vanilla server.
- JAR size about 2.9 MB.

---

## 4. CurseForge description outline (with prompts)

Answer the prompts in your own words; the answers *become* the page. Skip any section
that doesn't feel useful.

### 4a. Summary (search-results line)
Prompt: *If a friend asked "what does it do?" and you had one breath, what would you
say?* Try writing 5 different versions, then pick the best. Avoid starting with "This
mod…" or "A mod that…".

> Your draft:
> Turn your music into a soundtrack that reacts to the world
>

### 4b. Opening (first 2–4 sentences on the page)
Prompts: Why did you make this? What's the moment in-game that made you want it? What
does playing with AFMR *feel* like compared to vanilla?

> Your draft:
> animal's final music replacer automatically plays your own music files based on what's happening in the world. You can set music to play in a variety of situations, including current biome, time of day, health, combat status and more. Want cozy music that plays when you're close to home, eerie music for caving or even a theme song for when your best friends (or enemies...?) are near? AFMR lets you do this, and much more.
>

### 4c. Screenshot / GIF
Prompt: Which single image shows AFMR best? (Ideas: the playlist workspace; the
now-playing card in a scenic spot; a before/after of entering a biome.)

### 4d. Features
Prompt: Pick the 4–6 groups from the fact sheet that matter most to players. For each,
one heading + 2–4 bullets. Lead each bullet with the benefit.

> Your draft:
> 17 conditions to choose from (Time, location, light level, held item...)
> Combat and death music
> Mixes with Minecraft’s music or replaces it, your choice
> Add songs or folders from your computer, or import a playlist from foobar2000
> Works in multiplayer, including vanilla servers
> Custom silence, fades and transitions between songs and playlists
> Some songs louder than others? Normalize volume with a click
> Different profiles between worlds and servers
> Share playlist setups with a friend (they bring the music)
> Now playing widget shows song title and info in-game
> Custom themes and colors, including fonts, images and even GIFs
> Reorganize your music folders? AFMR can find your songs again
>

### 4e. Getting started
Prompt: The shortest path from install to hearing a song. (Facts: put the JAR in
`mods`, join a world, press M, select a playlist, Add music.)

> Your draft:
>Install Forge or NeoForge or Fabric
>Put the AFMR .jar file in mods folder
>Launch game and join a world
>Press M
>Select a playlist & click Add music
>To hear a song right away, right-click the selected song and click "Preview song" or the skip button at the bottom 
>Note: 10-20 minutes of silence after each song by default
>Enjoy :)
>

### 4f. Requirements & compatibility
Use the table in §3. Be explicit about: versions, loader, Fabric API for Fabric,
client-side, vanilla servers OK, optional companion, Windows-tested.
>Client-side mod, servers and friends don't need it
>Optional server companion: structure detection and combat target tracking only work in single-player. Server owners can use the companion to track that data in multiplayer. Without the companion, combat is detected by a simple timer instead of hostile mob aggression.
>Tested on Windows. Mac and Linux not tested.
>More versions on the way

### 4g. Known limitations / FAQ
Prompts: What questions will people ask first? ("Does it include music?" "Will it work
on my server?" "Mac?" "Opus files?" "Other Minecraft versions?")

> Your draft:
>Does this mod include any music?
>Nope! Use your own files.

>Which audio formats work?
>MP3, OGG Vorbis, FLAC, WAV. No Opus files.

>Can I use Spotify/Youtube/Streaming services?
>Nope! Local files only.

>Will it affect my files?
>AFMR only reads files; your music will never be changed or deleted.

>Will it break my world?
>AFMR only changes music; your world stays as it is.

>Does it work with other mods?
>I hope so! Reach out through CurseForge comments or GitHub if you run into any problems.
>

### 4h. Links & credits (bottom, small)
User guide link (if the GitHub repo is public), issue tracker/Discord if you have one,
bundled-library credits (see `THIRD_PARTY.md`), license.

---

## 5. GitHub README

The README is the front page of the repo. On GitHub, readers are a mix of players and
other modders, so it can be a bit more technical than CurseForge.

Suggested structure:
1. Name + one-line summary (can match CurseForge)
2. A screenshot
3. Short "what it does" (can reuse your CurseForge features, trimmed)
4. Download → link to the CurseForge page (once it exists)
5. Requirements table
6. Links to the user guide and installation checklist (already written in `docs/`)
7. Building from source → link to `DEVELOPMENT.md`
8. License + third-party credits

Notes on the current README:
- It reads like a changelog/developer note ("first private release for friends",
  Codex/test-report links up top). For the public, lead with what the mod is.
- `docs/USER-GUIDE.md` line ~171 tells players to get the companion from
  `build/libs/` — should say "download it from the CurseForge page" once that exists.
- Only matters once the repo is public. While it's private, nobody else sees it.

---

## 6. Upload checklist

**Before creating the project**
- [x] Project picture: 400×400 PNG, original art, not a solid color.
- [x] 1–4 screenshots (in-game, real, no AI edits).
- [x] Summary + description drafted and reviewed.
- [ ] Decide release type for the first upload. Suggestion: **Beta** (only Windows-tested,
      no modpack testing yet). You can upload a "Release" file later.
- [ ] Decide where server companions go: as additional files on this project, or a
      separate small project (clearer for server owners).

**Creating the project** (you do this — Claude can't create accounts or sign in)
- [x] Project type: Mods. Pick one main category + up to a few extras that fit.
      (Utility & QoL, Cosmetic)
- [x] License: MIT.
- [x] Optionally link the source (only if the GitHub repo is public).
- [x] Environment: set per file (Client on client JARs; Server on any companion JARs).

**Uploading files** — use the plain client `.jar` in each folder of
`outputs/1.1.1/multi-version/`, one per target. Not the JARs directly in `outputs/1.1.1/`
(an earlier 1.1.1 build), `build/libs/`, the zips, `-source.zip` files or
`third-party-sources`. License and third-party notices are already inside each JAR.
- [ ] `NeoForge-26.3/animals-final-music-replacer-1.1.1.jar` → 26.3 · NeoForge · Client ·
      Java 25.
- [ ] `Fabric-26.3/animals-final-music-replacer-fabric-26.3-1.1.1.jar` → 26.3 · Fabric ·
      Client · Java 25.
- [ ] `NeoForge-1.21.1/animals-final-music-replacer-neoforge-1.21.1-1.1.1.jar` → 1.21.1 ·
      NeoForge · Client · Java 21.
- [ ] `Fabric-1.21.1/animals-final-music-replacer-fabric-1.21.1-1.1.1.jar` → 1.21.1 ·
      Fabric · Client · Java 21.
- [ ] `Forge-1.20.1/animals-final-music-replacer-forge-1.20.1-1.1.1.jar` → 1.20.1 · Forge ·
      Client · Java 17.
- [ ] `Fabric-1.20.1/animals-final-music-replacer-fabric-1.20.1-1.1.1.jar` → 1.20.1 ·
      Fabric · Client · Java 17.
- [ ] `Forge-1.19.2/animals-final-music-replacer-forge-1.19.2-1.1.1.jar` → 1.19.2 · Forge ·
      Client · Java 17.
- [ ] `Fabric-1.19.2/animals-final-music-replacer-fabric-1.19.2-1.1.1.jar` → 1.19.2 ·
      Fabric · Client · Java 17.
- [ ] `Forge-1.18.2/animals-final-music-replacer-forge-1.18.2-1.1.1.jar` → 1.18.2 · Forge ·
      Client · Java 17.
- [ ] `Fabric-1.18.2/animals-final-music-replacer-fabric-1.18.2-1.1.1.jar` → 1.18.2 ·
      Fabric · Client · Java 17.
- [ ] All five Fabric files: add **Fabric API** as a *required dependency*.
- [ ] Server companions (if on this project): the
      `animals-final-music-replacer-server-<loader>-<version>-1.1.1.jar` in the same
      folders → tag Server; give a display name that clearly says "server companion —
      optional".
- [ ] Changelog for each file (trim the 1.1.1 section of `CHANGELOG.md` to the highlights).
- [ ] Check a file against `outputs/1.1.1/multi-version/SHA256SUMS.txt` if unsure it's
      the released build.

**After approval**
- [ ] On the public Files tab, check each JAR's Minecraft version, loader, Client/Server
      tag and Fabric API dependency.
- [x] Add the CurseForge link to the README, user guide and installation checklist
      (uses the `afmr` slug; checked live on October 30, 2026).
- [x] Turn on third-party distribution.
- [x] Set up bug reporting: GitHub issue forms (bug report, idea) in `.github/ISSUE_TEMPLATE/`.
- [ ] Next update: homepage/issue links in the mod metadata.

---

## 7. Decisions only you can make
- Release type: Beta or Release?
- ~~Make the GitHub repo public?~~ Done: public and linked as Source.
- Companion as extra files or its own project?
- ~~Where should players report bugs?~~ GitHub Issues (bug report and idea forms);
  CurseForge comments stay open for general feedback. Paste the updated Help &
  feedback and FAQ lines from `CURSEFORGE-DESCRIPTION.md` into the live page.
- Any credits or thanks you want to include?
