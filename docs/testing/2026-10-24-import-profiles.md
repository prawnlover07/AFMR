# Profile-first playlist imports — October 24, 2026

Implemented on both maintained targets under Unreleased; version remains 1.0.1.
The default is a new editable filename-named profile, created only on successful
confirmation and containing only the imported playlists. It stays inactive and
unbound. Existing-profile mode replaces matching names ignoring case/surrounding
spaces, preserving object identity, IDs, relative priority and references.
Settings-only replacement retains songs; enabling song import replaces assignments.
Ambiguous duplicates are rejected before mutation; unmatched playlists follow
insertion settings. Cancel creates no profile or playlist changes.

## Measured checks

Both full builds pass **928 NeoForge / 938 Fabric JUnit tests**, including **13 new
`PresetImportProfilesTest` cases each**, with zero failures/errors/skips, plus
**20 UI-copy compiler checks per target**. The current writing source validates
**165 / 163 applicable passages**. Core checks cover imported-only creation,
independent identities, no activation/world binding, profile limits, invalid names,
location validation without empty profiles, settings/song replacement, shared
catalog retention, stable objects/IDs/priority/references, ambiguous source/target
names, mixed replacements/additions, transition remapping, inactive profiles and
owned save/reload. Existing regression expectations were updated for intentional
same-name replacement; unrelated-playlist reuse tests now use unrelated names.

The 70,000-track guarded regression confirms settings-only import neither walks
the full catalog nor reads existing assignment entries. Retaining the assignment
map avoids copying it. Test reports were directed into ignored
`work/import-profile-checks/{AFMR,fabric-1.20.1}` to avoid concurrent-build locks.
Ordinary build/test commands remain supported; the local report override is not
part of the shipped source or runtime.

## Scope

These are component/model/build results, not native UI or Explorer validation.
The reusable NeoForge integration helper now expects the New profile default,
existing-profile insertion choice and inactive imported profile, but was not run
for this addition. Native drafts/navigation/feedback and playback during import
remain in [deferred testing](../../DEFERRED-TESTING.md). No personal music, foobar
data, game profile or release package was modified. Concurrent unrelated working
changes were preserved; no version bump or release tag was made.
