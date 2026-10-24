# Source-qualified combat, PvP and health conditions

Pending changes under Unreleased; mod version remains 0.5.7 while concurrent
development is in progress.

## Implemented behavior

- Default combat requires an attack involving a recognized hostile/neutral mob or
  another player. Passive targets and environmental damage are excluded. An explicit
  broad-trigger option accepts any targeted entity or damage source.
- Incoming damage is observed from Minecraft's resolved client damage events.
  Projectile owners are used when available; outgoing attributed hits also refresh
  attack detection. Unknown/unloaded sources are not assumed to be combatants.
- Players only uses an independent PvP timer. Mob events extend general combat
  without extending PvP. It participates in existing condition groups/death policy.
- Health filters use red hearts, exclude absorption, and compare strictly above or
  below the configured threshold. Equality matches neither; decimal input is supported.

## Verification

The root `build --offline --console=plain` succeeded with **370 automated tests**
and no failures/errors/skips in the shared source at this check. This includes
**26 focused tests** across CombatTrackerTest (9), HealthConditionsTest (9), and
PvpConditionsTest (8). Coverage includes trigger scope, separate expiry, switches,
death/reset, health boundaries/precision, All/Any/Not, priority transitions,
manual overrides, combat death ownership, Inspector, and saved snapshots/settings.
The all-filter matching test also exercises Health in All and Any modes.

The isolated native helper compiles against the new API without launching Minecraft.
Its generic-damage check now distinguishes default rejection from broad-mode
acceptance; these changed native checks have not been executed.

The cached Minecraft 26.3.0.90 sources establish that ClientPacketListener resolves
ClientboundDamageEventPacket sources before LivingEntity.handleDamageEvent. Fully
blocked shield events use a different notification without this attacker data;
they do not refresh the timer. Native mixin application, species classification,
real projectile attribution and UI/audio checks remain in
[DEFERRED-TESTING.md](../../DEFERRED-TESTING.md). Personal music, foobar data and live
game configuration were not changed.
