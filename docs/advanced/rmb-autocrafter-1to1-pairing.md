# Recipe Memory Box ↔ Auto-Crafter 1-to-1 Pairing

## Status
Approved — see `docs/superpowers/specs/2026-04-09-rmb-crafter-pairing-and-naming-design.md` for the full design spec.

## Summary

`BlockRecipeMemoryBox` becomes a directional block with a `FACING` block state. The face the RMB points at is its paired crafter (Auto-Crafter or Machine Interface). Pairing is resolved at `scanMultiblock()` time — nothing is stored on the crafter side. The `PatternKey` cross-block reference system and the Assign button / crafter picker are removed entirely.

## Problem

The current design uses `PatternKey(BlockPos rmbPos, int slotIndex)` cross-block pointers threaded through block entities, menus, screens, packets, and the crafting coordinator. Assigning a pattern requires: open RMB → open pattern slot → press Assign → pick crafter from a coordinate list. This is the most confusing part of the mod and compounds with every feature built on top.

## Solution: Face-Based Pairing

Each RMB has a distinct "output" face (visually obvious texture). Whatever Auto-Crafter or Machine Interface is on that face (and in the same multiblock) is its paired crafter. No picker, no stored position reference, no packets for pairing — the block layout is the configuration.

Pairing for a typical 3-RMB / 3-crafter layout:
```
[RMB→][RMB→][RMB→]
[AC  ][AC  ][AC  ]
```

Cables (future) will extend the connection along the facing direction without changing this model.

## Key Deletions

- `PatternKey` record
- `AssignPatternPacket` / `UnassignPatternPacket`
- Assign button + crafter picker in `RecipePatternScreen`
- `crafterPositions` threading through `RecipeMemoryBoxMenu`
- `BoxData` / `CrafterData` snapshots in `CraftingCoordinator`

## Also Included

Auto-Crafter gains a `customName` field (NBT-persisted, editable via GUI text field) for use in the future Statistics Block.
