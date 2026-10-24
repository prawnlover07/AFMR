# Held-item condition: automated verification

The pending Held items filter selects exact item types in the main hand, off-hand,
or either hand (default). Its editor searches localized item names and registry IDs,
supports multiple selections/manual IDs and Add held items, and retains selections
when disabled. Empty hands are represented by empty IDs and cannot satisfy it.

## Measured checks

- `gradlew.bat build --offline` passed with Java 25: **386 tests, 0 failures**.
- **13 held-item regressions** cover both hands, exclusive hand selection, exact
  vanilla/modded IDs, empty hands/selections, disabled filters, All/Any/Not groups,
  item-change selection and ordinary priority/empty-song fallthrough, overrides and
  disabled songs, independent clones/save snapshots, JSON round trips, normalization,
  Inspector explanations, and indexed resolution with 70,000 unrelated tracks.
- The shared condition-combination regression now also exercises held items as an
  independent Any-mode match and All-mode failure.
- Runtime observation, the item editor and the updated Context call sites compile.
- `git diff --check` passes. Version stays **0.5.7**, with changes under Unreleased.

## Source review and limits

The client reads its two held stacks. Registry ID strings are cached by item object
identity and reset on disconnect; unchanged types, enchantments, names, durability
and count do not rebuild the IDs. Predicate evaluation uses at most two selected-set
lookups per enabled filter and does not enumerate the item registry or track library.
Catalog construction/default-stack names and searching are confined to editing.
No new thread, server request, file access, or configuration format version is added.

No native in-game/UI run was performed for this feature. Hotbar/off-hand swaps,
cast/reel, item-component variants, GUI input/scales, hidden menu drafts, reconnects
and native transitions remain listed in [deferred testing](../../DEFERRED-TESTING.md).
The earlier native reports do not validate these additions.
