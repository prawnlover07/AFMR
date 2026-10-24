# Singleplayer-only option labels — October 23, 2026

Both maintained builds now show **Structures (singleplayer only)** in the
conditions list, including nested groups, and **Structure condition (singleplayer
only)** in the editor title. Multiplayer reports **Singleplayer only / unavailable
in multiplayer**; the title screen prompts joining a singleplayer world. In
singleplayer the existing status distinguishes pending from available data.
Structure search, Add current structures and radius controls have explicit hover
text. Manual configuration remains available for later singleplayer use.

Auditing the runtime guards found Structures to be the only wholly restricted
condition. Combat's mob-target retention is singleplayer only, so its condition
and quiet-period tooltips label that part specifically. Normal combat remains
usable in multiplayer. Normalization can also run at the title screen and retains
its existing wait message rather than receiving an inaccurate singleplayer badge.

Verification: ordinary builds pass **702 NeoForge / 712 Fabric tests**, zero
failures/errors/skips. The source diff was reviewed against the preceding source
packages: changes are option/title/status strings, hover text and a read-only
availability-status selection. No new tests were added for these wording changes.
No new native UI or multiplayer session was run; physical input, extreme scales
and final multiplayer label rendering remain deeper checks.

Runtime SHA-256:

- NeoForge: `82aa97ca29d4e7446c5f0e9704a9dcb79a2ed0fc951f5ae2d885f708ece60797`.
- Fabric: `c822088a7c1da050e2123ce306fd80a34496deafcc67337e6163f32da549071f`.

Version remains **0.6** and notes are under Unreleased.
