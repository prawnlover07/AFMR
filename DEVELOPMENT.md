# Developing AFMR

Work from this repository root. `src/` and the root Gradle build are authoritative.
The imported `work/` directory contains old checkouts, toolchains/caches, private
metadata snapshots and isolated game instances; `outputs/` contains earlier releases.
Both stay local and are ignored. Do not edit those archived source copies.

## Build and test

Install a 64-bit Java 25 JDK and make it available through `JAVA_HOME` or Gradle's
toolchain discovery. The wrapper uses Gradle 9.2.1 and NeoForge ModDevGradle.

```powershell
.\gradlew.bat build
.\gradlew.bat sourceZip
```

`build` compiles, packages the runtime JAR, and runs the regression tests. The JAR is
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
Pure unit tests cannot prove native pause position, device behavior, or real input.

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
