# Playlist workspace verification — October 21, 2026

AFMR **0.5.7**, Minecraft **26.3**, NeoForge **26.3.0.90**, Java **25**.

## Implementation coverage

| Existing function | Workspace location |
| --- | --- |
| All playlists, priority, new and protected default/custom deletion | Upper-left Priority, Top/Up/Down/Last, New playlist/Delete |
| Folder tree, two searches, Ctrl/Shift selection, recursive folder assignment, double-click add | Lower-left Music library and center song table |
| Sortable On/title/artist/album/time, enable/disable/remove, Space, previews | Center table and action row; On remains a toggle column |
| Automatic, Manual only, vanilla inclusion, separate entry and exit finish switches | Right Conditions/Playback panel |
| Custom name, all twelve condition filters, Match all/any | Name & conditions and inline Match control |
| Home, Underground, Darkness, Combat/death | Edit conditions; Home area remains directly available |
| File/folder imports and foobar .fplite imports/reports | Add music, with library-only or playlist destination |
| Linked sources, rescans, recursive/unlink controls | Library; launch rescan toggle in Timing |
| Normalize, cancellable scan and signed-gain reports | Center Normalize action and retained normalization screen |
| Normal Skip/play, immediate Next, pause/resume | Bottom transport |
| One-song/held overrides, release, all eight slots and physical bindings | Workspace controls, Overrides, Keys, Minecraft key binds |
| Native/Hybrid/AFMR display, size/wrap/information/scroll/appearance and preview | Display, retaining every existing option |
| Inspector reason/details/retry | Inspector navigation |
| Reopen page, drafts, searches, selections, sort and list scroll | Retained MusicMenu state and cached primary pages |

The old playlist manager and rules screens were removed, with their actions moved
into the workspace. No scheduler, decoding, normalization implementation, file
format or default changes are part of this overhaul. Existing files/folders and
foobar data remain read-only. UI labels are concise; onboarding/tutorial/flavor
copy has been removed from the affected screens.

## Automated checks

`gradlew build` passes **331 tests**, zero failures: the existing 329 regressions
plus geometry checks covering small/large/tall game windows, metadata/action space,
independent list regions, and separation from playback controls. `sourceZip` builds
the numbered source package; its contents are inspected separately for excluded data.

## Isolated native screen checks

The user authorized an isolated Minecraft window. The separate test mod
`WorkspaceSuite` runs only with `-Pworkspace`, uses `testing/native/run/workspace`,
a copy of the earlier disposable world, and generated quiet WAVs. It sends actual
Minecraft screen/widget mouse events with SDL button identifiers and converts the
workspace's virtual coordinates. It checks resulting model/runtime state and captures
screenshots through Minecraft's renderer. It does not automate the desktop mouse.

**48 passed, zero failed.** Checks cover direct navigation, library placement,
double-click and selected-track assignment, bulk on/off and removal, whole-folder
assignment, independent entry/exit, priority top/down/last, both override modes with
Automatic off, a generated song actually starting from its expected source,
pause/next/resume/release, invalid timing blocking navigation, hidden draft/page
retention, save-on-navigation, complete display metadata controls, native key-binding
access and all eight slot pages, import destinations, normalization progress access,
custom conditions/Match access, Inspector/details/retry, create/delete, and searches
and editor identity surviving reopening.

Two fixture issues were corrected during development: the helper initially used an
old mouse button identifier instead of Minecraft 26.3's SDL identifier, and hand-built
track IDs contained characters invalid for AFMR's resource identifiers. Corrected
fixtures use SDL's left-button constant and valid `file:` IDs. These were helper
defects; the failed runs are not counted as validation. A later check explicitly
requires a generated song to start without an engine error.

Runtime output is in ignored `testing/native/results/workspace-results.txt` and
`workspace-complete.txt`; screenshots are in the isolated profile's `screenshots`.

## Limits

Synthetic screen events do not prove physical keyboard repeat/rebinding, desktop
file-picker behavior, all window/GUI-scale combinations, or all music-library sizes.
Normalization-screen access does not revalidate every decoding/gain/report case.
The new helper does not measure OpenAL state/offset/gain; those remain covered only
by earlier, separately scoped native playback reports. Physical input, picker,
progress, long-name and larger-library combinations remain in `DEFERRED-TESTING.md`.
