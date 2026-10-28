# Playback navigation — October 25, 2026

Both maintained clients retain version 1.0.1. This pass adds a song-position bar
in the playlist workspace and AFMR settings pages, a current-song link to its
source playlist, selection counts in song context actions, searchable settings
with direct destinations, and checkbox/detail-arrow consistency. Form layouts
reserve the bottom strip; Timing and Menus scroll on shorter windows.

Seeking previews a target until release, coalesces same-tick requests and keeps
manual/jukebox pause, source, queue, group/history and playback-notification
identity. Position and heard-time are separate clocks. Unknown durations use
the existing bounded duration worker. Local/resource decoders run on Minecraft's
existing decoder workers; canceled streams close there. Runtime audio streams
return GC-managed direct buffers, matching Minecraft's decoder contract.

Verification

- Full builds: 1,009 NeoForge / 1,019 Fabric pure regression tests, zero failures.
  Ten new tests per target cover seek identity/pause/loading/clamping/coalescing/
  queue preservation and the settings index. Network/component checks also pass,
  including three new buffer-ownership/frame/cancellation/failure tests per target.
  UI-copy compiler checks pass (20 per target), with 167 / 165 passages validated.
- Final packaged native batches: 51 passed per target, zero failures (102 total). Both use generated music/disposable worlds, capped
  at 60 FPS with VSync off. Personal music and foobar data remain read-only.
- Native cases include decoded PCM at the requested WAV offset, paused seeks,
  rapid and repeated seeks, resource music and forced buffer collection, source
  location with a cleared song query, retained queue/sequence, selection counts,
  precise search destinations, invalid-draft navigation guards, checkbox/detail
  actions, 420×240 menu scrolling, status-line hit separation, and the existing
  theme Apply/Cancel/paging/hide-reopen regression batch.
- Workspace and Menus screenshots were visually inspected. General codec,
  resource-pack, device-change and extended modpack certification is outside this
  controlled pass; see DEFERRED-TESTING.md.

Bugs caught during this implementation

- Rapid seeks could exhaust Minecraft's streaming pool. Pending initial sources
  needed explicit stop cleanup, canceled attachments needed ownership guards,
  and same-tick seeks now submit only their final target.
- A repeated resource seek exposed an invalid free of Minecraft-owned buffers
  in the new seek code (native allocator crash). Removed manual freeing and
  aligned local audio buffers with the GC-managed AudioStream contract. A
  decoder-owned direct-storage regression test and native forced collection
  exercise the corrected lifetime.
- Visual review found the workspace seek hit area sharing the status line.
  The bar now ends before the status text begins.

Reproduce

NeoForge: prepare generated fixtures with `testing/native/prepare_fixtures.py
--workspace`, then run `gradlew.bat -p testing/native runClient -Pworkspace
-Pnavigation -Pclarity`. Fabric: run `gradlew.bat -Psmoke -PpackagedSmoke
-Pnavigation -Pclarity runClient` in its port directory. Both require the ordinary
configured local toolchain/cache. Helpers remain outside runtime JARs.

Final client JAR SHA-256

- NeoForge: `041cceb7c038bf915943ab24ae1a090fe4e9237bc0d6268b18a7e65c4fc96abc`
- Fabric: `dc849746bd4de1870949c51da03116c7ad48db83a64af7b95d01621fe2ed5b08`

Preserved native results: [NeoForge](evidence/2026-10-25-playback-navigation-neoforge-results.txt),
[Fabric](evidence/2026-10-25-playback-navigation-fabric-results.txt).
Both source ZIPs were rebuilt and inspected for current sources and excluded
workspace data; runtime JARs exclude probe classes and test font fixtures.
