# Developing AFMR

The added [Fabric 26.3 target](ports/fabric-26.3/README.md) uses its own Gradle
9.8.2 wrapper with Java 25. Run commands from that port directory. The root
wrapper remains 9.2.1; it is too old for Fabric 26.3 Loom. See [the matrix](docs/PORTING.md)
for all ten verified targets and their runtime requirements.

The added [Forge 1.20.1 target](ports/forge-1.20.1/README.md) uses its own build
and Java 17 runtime. Its source archive is independently buildable; tests also
provision Java 25. See its README for packaged client/server probes.

Minecraft 1.21.1 has independent [Fabric](ports/fabric-1.21.1/README.md) and
[NeoForge](ports/neoforge-1.21.1/README.md) builds using Java 21. Both also pass
packaged gameplay/network checks and extracted-source builds.

The four Fabric/Forge 1.18.2 / 1.19.2 builds also have independent source archives
and verified client/companion/fallback probes. Their production/runtime target is
Java 17; regression tests provision Java 25. Keep older hit attribution and
font/screen lifecycle changes local to those direct implementations. See
[their verification](docs/testing/2026-10-29-older-ports.md).

For the independent **Fabric 1.20.1** target, use
[`ports/fabric-1.20.1/README.md`](ports/fabric-1.20.1/README.md). Its sources,
Gradle build, JAR and disposable native test profile are separate from the
NeoForge target described below. Keep its mod version synchronized with the root
build; the current release is 1.1.1 and its shipped bytecode targets Java 17.

For a guided explanation of the implementation, read the
[developer guide](docs/DEVELOPER-GUIDE.md). For player-facing instructions, use the
[user guide](docs/USER-GUIDE.md). This file covers development setup and commands.

Work from this repository root. `src/` and the root Gradle build are authoritative.
The imported `work/` directory contains old checkouts, toolchains/caches, private
metadata snapshots and isolated game instances; `outputs/` contains earlier releases.
Both stay local and are ignored. Do not edit those archived source copies.

## Build and test

For release source/notices preparation, run
`python testing/prepare_dependency_sources.py` after building the NeoForge JAR.
It reads the exact nested-library coordinates, fetches original Maven Central
source JARs into ignored `outputs/<version>/third-party-sources`, and reproduces
their copyright/license comment blocks in `THIRD_PARTY_NOTICES.txt`. Include
the GNU LGPL texts and all notices in both runtime/source distributions; keep
the independently buildable Fabric copies synchronized. Network access belongs
only to this explicit release step, not gameplay or ordinary offline builds.

Install a 64-bit Java 25 JDK and make it available through `JAVA_HOME` or Gradle's
toolchain discovery. The wrapper uses Gradle 9.2.1 and NeoForge ModDevGradle.

```powershell
.\gradlew.bat build
.\gradlew.bat sourceZip
```

`build` compiles, packages the client and optional server companion JARs, and runs
the pure regression and `networkTest` packet component suites. The JAR is
in `build/libs/`; the source ZIP is in `build/distributions/`. Test reports are in
`build/reports/tests/test/`. Source ZIPs exclude local archives, Git metadata, caches,
and generated/native game data.

The imported local toolchain currently exists under `work/jdk/jdk-25.0.4.1+1`, and
the imported Gradle cache is `work/gradle-cache`. They are conveniences for this
computer, not dependencies committed to the project. A local PowerShell session can use:

```powershell
$env:JAVA_HOME = Join-Path $PWD 'work/jdk/jdk-25.0.4.1+1'
$env:GRADLE_USER_HOME = Join-Path $PWD 'work/gradle-cache'
.\gradlew.bat build
```

Fresh setups need dependency downloads. Gradle's Foojay resolver can provision the
Java 21 asset tool; Minecraft and AFMR themselves use Java 25.

## Writing descriptions and hover text

Edit [docs/UI-COPY.md](docs/UI-COPY.md), the single authoring source for both
targets. Replace only the prose beneath each **Text to edit:** marker, or clear
the paragraph to remove that explanation. Keep the entry's metadata. The
screen/control context, reference behavior and placeholder notes are there for
the author; only the editable prose ships. Utility labels and runtime messages
remain in their existing code, with shared key-binding translations in
`copy/utility-en_us.json`.

Both builds automatically compile the Markdown through `gradle/ui-copy.gradle`
into `build/generated/uiCopy/assets/afmr/lang/en_us.json`. Screens use native
`Component.translatable` calls; there is no runtime Markdown reader or custom
text loader. No Python or extra authoring dependency is needed. Do not edit or
commit the generated JSON. Rebuild and restart a test profile to review wording.

Run `.\gradlew.bat validateUiCopy` for missing/unused entries, target restrictions
and placeholders in nonempty passages. Empty text disables only its explanation;
utility labels and overflow-label tooltips remain. `build` also runs `testUiCopy`, which exercises
the compiler's parsing and rejection cases. Keep reference behavior current when
a feature changes. The Fabric source ZIP includes the shared writing file,
compiler and utility translations so it remains independently buildable.

## Code and test layout

- `src/main/java/dev/animal/afmr/core`: library model, conditions, scheduler, combat,
  shuffle and inspector logic. Much of this is Minecraft-independent.
- `audio`, `mixin`, `library`, `ui`: native audio integration, decoding/import/storage,
  and screens. `ClientRuntime` owns the client-side integration.
- `src/test/java`: scheduler, conditions, persistence, import, decoding, ReplayGain,
  and indexed-library regressions. Audio resources are small generated test signals.
- `testing/native`: the separate native test mod and fixture generator. See its
  [instructions](testing/native/README.md); it is not bundled into AFMR.
- `docs/testing`: the preserved native/UI report, case list, and screenshots.

Running `runClient` opens Minecraft. Coordinate desktop testing with the user and
use an isolated profile; never point tests at personal game configuration or music.
All native/smoke clients must start capped at **60 FPS**, with VSync off, because
higher rates cause desktop lag on the owner's computer. Their Gradle `capProbeFps`
tasks enforce this before launch; preserve the cap in new probe setups as well.
Pure unit tests cannot prove native pause position, device behavior, or real input.
Optional companion service/socket checks have separate [test instructions](testing/SERVER-COMPANIONS.md).

## Maintaining server companions

Whenever a new feature or condition would otherwise work only in singleplayer
because it relies on server-owned state, include support in both maintained
server companions as part of that feature. Update the client observations,
companion packaging, relevant tests and user-facing availability text together.
Verify behavior with the companion and on a server without it. Keep observation
work bounded and music/library data local. Record any unavoidable limitation and
unfinished validation explicitly.

## Update versions

Keep the current version (1.1.1) unchanged while any development tasks are in
progress, including concurrent work in other chats. Do not increment it for an
individual fix, feature, or completed task. Record pending changes under
**Unreleased** in `CHANGELOG.md`; routine builds and tests keep the current version.

Only when all in-progress tasks are complete, increment the version once for the
combined update. A single task finishing does not establish that the other work
is complete. At that point, update `mod_version` in `gradle.properties`, the mod
versions in both client/server NeoForge metadata files, Fabric's Gradle properties
and both Fabric metadata files, current install/guide references, and the combined
changelog entry. Rebuild both numbered clients, optional companions and source
ZIPs. Keep historical test reports and Git baseline references at their
original versions. Documentation edits alone do not require a new mod version.

## Git checkpoints

Git is local. There is no remote configured. The first commit imports the validated
0.5.2 source; annotated tag `v0.5.2` identifies that checkpoint. The next commit records
the permanent project setup. Previous releases remain archived under ignored `outputs/`.

A commit is a saved checkpoint; a tag is a readable name for a particular checkpoint.
Use small commits for later features or fixes so changes are understandable and reversible.

```powershell
git status
git diff
git log --oneline --decorate
```

Repository-local author details are configured from the user's supplied name/email.
No global Git configuration was changed. Local Git history is not an off-computer backup;
remote hosting or another backup destination can be set up separately when requested.

## Full-backup and seam verification

`ConfigBackupTest` adds eleven generated-only transfer/rejection/recovery tests per
target. Run the ordinary builds for all 702 NeoForge / 712 Fabric tests. The native
workspace helper accepts `-PconfigBackup` to export/review/stage, then the same
flags plus `-PbackupRestart` to verify an actual restart. `-PbackupFromFabric`
reads a disposable staged Fabric export; `-PmenuSeams` repeats the 16 render-state
coverage checks. The Fabric packaged smoke run includes full backup review and
framebuffer seams; `-PbackupRestart` consumes a generated NeoForge export at title.
See the reports for exact fixtures and limits. Never stage personal configurations
in these test runs. Complete `.afmrb` files are ignored and excluded from source
packages because they contain private paths and world/server bindings.

## Join playback checks

The existing builds include sixteen `PlayImmediatelyOnJoinTest` cases per target,
plus profile and full-backup round trips for the setting. Native join-only fixtures
are staged with `testing/prepare_join_playback.py` (add `--fabric` for the port).
See `testing/native/README.md` for the mode flags and disposable-world requirement.
Only actual session entry calls `joinedWorld`; never rearm it from generic engine
reset, resource reload, profile switching or player-object replacement.
