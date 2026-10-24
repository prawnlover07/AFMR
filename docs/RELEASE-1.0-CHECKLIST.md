# AFMR 1.0 private release checklist

Plan agreed October 23, 2026: target a private 1.0 release for friends today.
Friend testing is deferred until after release, at Animal's request. This is a
readiness checklist; checked entries record completed work. Development and the
focused review are complete; version 1.0 passed final artifact verification.

## Finish the author's text

- [x] Animal's intended edits to [UI-COPY.md](UI-COPY.md) are reviewed and
  implemented for NeoForge 26.3 and Fabric 1.20.1, including the 26 cleared
  explanations. See [the review](testing/2026-10-23-author-copy.md).
- [x] Build both targets with the reviewed text and run regression/copy-compiler
  checks: 778 / 788 JUnit tests and 20 compiler checks per target pass.
- [x] Confirm no other development tasks remain active before freezing the release source.
- [x] Review representative changed guidance/tooltips in isolated game profiles for wrapping,
  truncation, dynamic values, small windows and target-specific behavior.

## Final focused game pass

- [x] On both targets, verify installation/import/assignment/first playback;
  day/night/cover and a custom condition; pause/Next, override, queue and effective
  global/per-playlist silence. Check playback and settings again after restarting.
  NeoForge has live day/night/cover/custom selection checks; Fabric's context
  matching uses the regression suite plus the author's gameplay feedback. See
  the [release report](testing/2026-10-23-release-1.0.md) for exact coverage.
- [x] Exercise Copy diagnostic summary through the real clipboard, inspecting
  useful state and private-data omission. Check the reorganized Timing/Combat/
  Library controls and the Widget/Menus pages with the final text.
- [x] Confirm world/server profile switching and reconnect behavior; moved-folder
  repair; playlist sharing; full configuration restore after restart, including an
  enabled animated background. Use generated assets and disposable profiles.
- [x] Record the exact builds and results. Fix reproduced release blockers and
  rerun affected checks. Retain other untested extremes and known limitations in
  [DEFERRED-TESTING.md](../DEFERRED-TESTING.md), without treating every open case as
  a mandatory 1.0 feature. Coordinate game windows with the author's computer use.

The author's few-hour smooth/stable Homestead session is already recorded as
successful manual endurance feedback. It did not measure memory; no instrumented
memory result or independent friend validation should be claimed.

## Release preparation

- [x] Animal chose MIT for AFMR's own code and documentation, credited to animal;
  retain the separate template and bundled-library notices. No public hosting,
  remote repository or automatic delivery to friends is required.
- [x] Prepare concise release notes and friend-facing install instructions for
  each loader/game/Java target, including one installed AFMR JAR, first playback,
  normal silence, backups and how to send a diagnostic summary. Document platform
  differences and the actual tested scope. Do not distribute personal configs,
  music, screenshots containing private data or complete personal `.afmrb` backups.

## Final numbered artifacts and Git

- [x] Once all development is complete, increment to 1.0 once across both targets:
  root and Fabric Gradle properties, NeoForge/Fabric metadata and current install/
  guide references. Move the combined Unreleased notes into the 1.0 entry while
  preserving historical version references and test reports.
- [x] Build both numbered runtime JARs and source ZIPs; rerun required checks and
  a packaged startup/smoke check against the actual final JARs. Verify metadata,
  bundled dependencies, Fabric Java 17 bytecode and archive contents/exclusions.
  Record final artifact hashes and observed results.
- [x] Create the Git release checkpoint and `v1.0` tag for the completed source;
  keep binaries/generated state out of Git. Prepare separate clearly labelled
  NeoForge and Fabric packages with the runtime JAR and installation guidance.
  Explain the checkpoint/tag and installation steps to Animal as they occur.

## After private release

- [ ] Send the chosen package to friends when they are available. Gather their
  fresh-install feedback and reproduction steps/diagnostic summaries, then make
  follow-up fixes as needed. No pre-release friend test is required for this plan.
