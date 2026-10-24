# Performance investigation — 0.2.2

## Combined Unreleased validation — October 22, 2026

In an isolated client using 67,812 real catalog entries, a small library, the full
catalog, 67k assignment data, and the large workspace all held a median 119 FPS
under the same 120-FPS cap. Gameplay Post-event median/p95 was 0.0668/0.0957 ms
with the full catalog versus 0.0950/0.1920 ms in the small baseline; these include
event/test overhead and order/JIT effects, not isolated AFMR time or an improvement
caused by catalog size. Large workspace opening was 567 ms in one measured pass.

A full linked-folder rescan completed in 436.78 s with zero missing-source issues.
That pass included hundreds of thousands of routine WAV chunk log messages. The
reader now suppresses third-party INFO messages while retaining warnings/errors;
scan-throughput improvement has not been measured. Initial scan-period FPS was
idle-capped and a 606-ms tick spike overlapped menu inspection, so neither proves
a steady gameplay regression or a scan-completion hitch. See the
[combined report](docs/testing/2026-10-22-release-validation.md) for methods,
limitations and preservation checks. Analysis-during-gameplay remains disabled:
this measures ordinary metadata scanning, not loudness decoding.

## Unreleased loudness scanner

Explicit playlist normalization uses at most two homogeneous worker lanes. Each
lane owns its decoder, 16-KiB PCM buffer and 160,016-byte histogram arrays; memory
does not grow with song duration. Files are dynamically assigned through one
atomic index, results use disjoint slots, and the last lane publishes a report.
Progress is published at most ten times per second per lane during decoding.
The client applies completed measurements at the batch boundary and saves an owned
snapshot. Report rows and summaries are formatted once, and only visible rows draw.
Playback checks tags or the captured cache for one opening file, never the whole library.

A warmed 120-s, 48-kHz stereo PCM meter benchmark measured 78.1 ms median thread CPU
(seven samples, three warmups), about 1,536x real time. This excludes decoding, I/O,
metadata, Minecraft, and saving. Independent FFmpeg comparisons and limitations are
in the [scanner test report](docs/testing/2026-10-21-loudness.md). Actual gameplay
impact has not been measured, so analysis suspends during active gameplay and runs
only at the title screen or with singleplayer paused.

The saved library used for the investigation contained **67,812 local tracks**, 70 vanilla tracks, and 250 explicit Day outside assignments. Its JSON occupied about 21.4 MiB. The local format counts were 63,327 WAV, 2,438 FLAC, 1,952 MP3, and 95 OGG entries. This is the indexed collection size, not a new recursive filesystem count.

## Causes and changes

- **Full-library scans on every tick and editor frame:** `selectPlayable` built a complete pool by streaming every track before checking whether any eligible song existed. It now checks only explicit playlist assignments and applicable vanilla IDs through a direct track index. The index is rebuilt at catalog changes. The editor reads the scheduler's selected playlist and cached status rather than resolving it again while drawing.
- **Repeated folder-tree construction:** search, sorting, assignment changes and folder expansion rebuilt all folder/file nodes. The tree now stays cached until imported metadata or linked folders change. Search computes matches once per query; expanding nodes only flattens visible branches. Song filtering/sorting and assignment edits update the song pane independently.
- **Repeated resource parsing:** vanilla music event expansion and Home world-key construction ran during each tick. Expanded vanilla pools are prepared during resource reload; the world key is retained for the current connection/save.
- **Synchronous library writes:** each edit serialized and wrote the full JSON on Minecraft's thread, and closing screens wrote it again. Edits now capture an independent snapshot and submit it to one background writer. The writer keeps one newest pending snapshot, serializes writes, preserves atomic replacement/backups, reports failures and flushes on normal shutdown. Screen navigation alone does not request a save.
- **Small render allocations:** column positions are calculated at layout time; duration rendering no longer invokes `String.format` for every visible song every frame. Ordinary row selection no longer enumerates the full selection range unless Shift is held.

## Measurements

Same machine, Java 25, 512 MiB initial / 1 GiB maximum heap, standalone packaged classes, the same private metadata snapshot, 100 warm-up calls followed by 300 timed calls. Only in-memory benchmark settings were changed to enable the day playlist and keep the scheduler in a long silent gap. No personal audio files were opened and no live user configuration was written.

| Operation | 0.2.1 median | 0.2.2 median | 0.2.1 p95 | 0.2.2 p95 |
| --- | ---: | ---: | ---: | ---: |
| Playlist resolution | 13.989 ms | 0.0022 ms | 18.399 ms | 0.0145 ms |
| Idle scheduler tick | 15.157 ms | 0.0021 ms | 17.429 ms | 0.0052 ms |
| Two legacy editor queries | 27.043 ms | 0.0037 ms | 33.721 ms | 0.0086 ms |

The editor-query row runs the same two library queries in both builds to compare the algorithms. The actual 0.2.2 render path no longer calls either query. Thread allocation accounting measured approximately 2,992 to 32 bytes per resolution and 5,984 to 64 bytes per query pair in these short runs.

A separate one-shot save comparison using the same metadata measured **338.4 ms** for synchronous JSON/file writing versus **9.4 ms** on the caller for copying and submitting a save snapshot. Serialization and disk work still occur, on the writer thread. This comparison includes cold serialization setup and is illustrative rather than a repeated latency distribution.

The packaged build was also exercised in Minecraft with **67,812 generated local entries**, including a playable generated tone. Editor opening, tree expansion, eligibility toggles, playback and persisted saves were checked. The regression suite includes a 70,000-track test that forbids subsequent full-library traversal during steady scheduling.

These are component timings, **not whole-game FPS measurements**. Initial JSON loading, initial editor tree construction, imports/rescans and snapshot capture still scale with collection size. Other mods, rendering settings, decoder cost and storage latency can independently affect Minecraft performance.

## Thread ownership

Version 0.4.0 evaluates custom conditions directly against the tick's world snapshot. Each playlist adds only a fixed set of scalar comparisons and optional location-distance math; eligibility continues using track assignments and the existing track index. A regression test evaluates 200 custom rules with a guarded 70,000-track library that rejects full-library traversal. The priority menu caches its rank lookup and condition summary when selection/order changes. Song tabs and import/priority lists show only their current page or viewport, rather than creating a widget for every imported song.

Version 0.4.1 adds exact biome membership using a hash set against the biome ID already sampled for the context. It performs no new world queries or music-library traversal. Biome registry enumeration and sorting happen when the condition screen initializes; searches run only when the search text changes. Only visible list rows are drawn. A regression test alternates matching and nonmatching biomes with a guarded 70,000-track library. Restoring playback timing changes four scalar settings and uses the existing save path.

ReplayGain reads only the next song's metadata on the existing audio worker when that stream opens. It does not walk the library, decode audio for loudness analysis, spawn another executor, or read tags per frame. A fixed gain is applied in place to each existing PCM16 byte buffer; unity gain returns immediately and the gain loop allocates nothing. The immutable local-source record captures the toggle before the worker opens the file, so the worker does not read mutable settings.

A warmed Java 25 microbenchmark on this machine processed 8,192-byte PCM blocks at approximately 4.3–4.5 ns/sample for attenuation/amplification, equivalent to about 0.04% of one CPU core at 44.1 kHz stereo. This measures only the scalar PCM adjustment loop, not decoding, metadata I/O, Minecraft, or frame rate. No gameplay performance measurement was performed for this update. Metadata latency depends on storage and tag size; disabling ReplayGain bypasses that additional read.

Version 0.4.2 records one source-playlist ID when an automatic song starts. Entry/exit decisions look up that playlist only at an accepted context transition; they add no music-library traversal or per-tick allocation. A guarded 70,000-extra-track regression exercises fade, gap and next-song selection with these controls.

Version 0.5.0's inspector builds diagnostic snapshots only while its screen is open, once per second. Eligibility checks short-circuit against playlist assignments and the existing track index. Full counts for a selected playlist's assignments are calculated only when opening its details. Custom failure reasons use an enum returned by the same scalar comparisons used for playback; ordinary mismatches allocate no strings. The inspector uses the existing context without additional Minecraft world queries.

Version 0.5.2 pauses one AFMR channel through Minecraft's existing sound executor. The integration retains the current AFMR sound and does a single indexed channel lookup per sound tick; it does not scan all sounds or add threads. Pause/resume commands are queued only when the flag changes or Minecraft globally resumes audio. A volatile per-sound flag covers a stream that starts decoding after a pause request. Sound-thread code owns OpenAL operations; the scheduler and timing state remain on the client thread.

Playback pause holds scalar timer origins and shifts them by the paused interval on resume. Environment detection and combat observations continue, with no shuffle draws or audio-health checks while paused. Next performs ordinary indexed eligibility resolution and takes a shuffle entry at the next song-start boundary. Guarded-library regressions cover pause/Next with 70,000 extra tracks. Native audio latency and gameplay FPS were not measured for this release.

Overrides store only a target ID, mode and lifecycle flags. Normal playback avoids even the override-ID lookup when no override is active. Held overrides validate through indexed assignments; no extra worker/executor or music-library traversal is introduced. The eight keybind mappings are registered once; their playlist/mode assignments are fixed-size saved data.

Shuffle work happens when choosing a new song. Each playlist retains a Fisher-Yates order and sets of cycle members/already drawn IDs. The eligible pool is indexed by ID to return current metadata; membership reconciliation and shuffling run only at song-selection boundaries. There is no shuffle or history scan in steady playing ticks. This trades bounded memory per visited playlist for preserving its unplayed order across context switches. Regressions guard 70,000-track libraries during overrides and diagnostics. Native gameplay FPS was not measured for this release.

The 0.3.2 environment fix samples only the player's column using a heightmap already synchronized by Minecraft. It skips fluids to find solid cover, caches the result for five game ticks, and refreshes on column/world changes. Ordinary solid surfaces require one block read per sample; water columns require a bounded downward scan. It neither primes an entire chunk's heightmaps nor iterates music entries, and allocates no per-block positions.

Minecraft continues to own world queries, sound scheduling, editor widgets and texture changes. Library changes and snapshot capture occur on that thread. The save worker receives its own data, and only one writer may replace a library file at a time. Scanning and decoding remain background I/O work. Removing repeated work makes steady scheduling small enough to execute directly without distributing library queries across threads.

The threading design was reviewed against [Multi-Core By Default](https://www.dgtlgrove.com/p/multi-core-by-default), as requested in the project instructions; independent file I/O remains a separate timeline, with explicit data ownership and serialized file replacement.
