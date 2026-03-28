# Recipe Memory Box ↔ Auto-Crafter 1-to-1 Pairing

## Status
Proposed — pre-release refactor candidate

## Problem

The current design treats the Recipe Memory Box (RMB) as a shared recipe library within a multiblock. Auto-Crafters reference individual pattern slots via a `PatternKey(BlockPos rmbPos, int slotIndex)` cross-block pointer. This means:

- A single Auto-Crafter can hold assignments from *multiple* RMBs.
- A single RMB can be referenced by *multiple* Auto-Crafters.
- Assigning a pattern to a crafter requires: open RMB → open pattern slot → press Assign → pick crafter from a list — a non-obvious multi-step flow.
- The `PatternKey` abstraction (a cross-block `BlockPos + index` tuple) is an implementation detail that leaks into menus, packets, screens, and the crafting coordinator.
- There is no natural upgrade path: if tiers are added to both blocks independently, a T2 crafter paired with a T1 RMB produces undefined capacity expectations.

## Proposed Design: 1-to-1 Pairing

Each Auto-Crafter is explicitly paired with exactly one Recipe Memory Box. The crafter reads its recipes directly from its paired RMB. A given RMB can only be paired to one crafter at a time.

### Pairing Interaction

The player sneak+right-clicks an Auto-Crafter while holding a Recipe Memory Box item, or sneak+right-clicks an in-world RMB while holding an Auto-Crafter item, to establish the pair. The pairing is stored as a `BlockPos pairedRmbPos` on the `AutoCrafterBlockEntity`. Unpairing is done by sneak+right-clicking without a relevant item, or when either block is broken.

### Mental Model

> "This box controls this crafter."

The Auto-Crafter GUI shows the 4 (base tier) recipe slots from its paired RMB directly, with per-slot auto/buffer config alongside each recipe. No separate assignment step.

### Multiblock Membership Validation

Validation is performed at **craft time**, not at pairing time. The `AdvancedStorageCoreBlockEntity` tick loop skips any crafter whose paired RMB is not currently present in the same multiblock. This handles incremental building gracefully and is consistent with how the rest of the multiblock handles missing components.

---

## What Changes

### Deleted

| File / Type | Reason |
|---|---|
| `PatternKey.java` | No more cross-block slot references |
| `AssignPatternPacket.java` | Assignment UI removed |
| `UnassignPatternPacket.java` | Assignment UI removed |
| Assign button + crafter picker in `RecipePatternScreen` | Replaced by pairing interaction |
| `crafterPositions` field in `RecipeMemoryBoxMenu` | No longer needed by menus |
| `computeCrafterPositions()` in `RecipeMemoryBoxMenu` | No longer needed |
| `findCore()` BFS in `RecipeMemoryBoxMenu` | Only existed to populate crafter list |
| `BoxData` snapshot in `CraftingCoordinator` | Coordinator reads paired RMB directly |

### Modified

| File | Change |
|---|---|
| `AutoCrafterBlockEntity` | Replace `Map<PatternKey, PerPatternConfig>` with `PerPatternConfig[] configs` (one per slot) and `BlockPos pairedRmbPos`. Add `pair(BlockPos)` / `unpair()`. |
| `RecipeMemoryBoxBlockEntity` | Optionally add `BlockPos pairedCrafterPos` for reverse reference. |
| `UpdatePatternConfigPacket` | Change from `(crafterPos, PatternKey, ...)` to `(crafterPos, slotIndex, ...)`. |
| `AutoCrafterMenu` | Shows paired RMB's 4 pattern slots with inline `PerPatternConfig` per slot. Removes `assignments` map and `outputItems` resolution from RMB lookup. |
| `AutoCrafterScreen` | Renders per-slot recipe + auto/buffer config. Merges what was previously split across RMB assignment UI and auto-crafter list UI. |
| `BlockAutoCrafter` | Simpler `openMenu` buf — just pairing state, no assignments map serialisation. Add pairing interaction to `useWithoutItem` / `use`. |
| `CraftingCoordinator` | Iterate crafter list from core; for each crafter with a valid paired RMB in the multiblock, read patterns directly from `pairedRmb.getPattern(i)`. `BoxData` record removed. |
| `ModNetwork` | Remove assign/unassign handlers. Add `PairAutoCrafterPacket`. Update config handler. |

### Unchanged

| File | Reason |
|---|---|
| `AdvancedStorageCoreBlockEntity.scanMultiblock()` | BFS discovery of all multiblock members is unchanged |
| `RecipeMemoryBoxBlockEntity` pattern storage | Patterns are still stored per-slot as `RecipePattern` |
| `RecipePatternMenu` / `RecipePatternScreen` | Pattern editing flow unchanged; Assign button simply removed |
| `RecipeMemoryBoxScreen` | Pattern slot grid unchanged |

---

## Relationship to Cable Infrastructure (Issue #3)

The current shared-reference model was implicitly designed around a future where cables extend the multiblock and enable routing — one RMB serving multiple crafters across a network. The 1-to-1 pairing deliberately **defers** that flexibility to cables.

When cables land, the natural evolution is:
- A cable-connected RMB can serve as a *shared recipe library* for multiple crafters on the network.
- The crafter's `pairedRmbPos` could become a `pairedRmbChannel` or similar routing concept.
- The 1-to-1 pairing is the degenerate case (one cable segment, one pair) of the general routing model.

This means the 1-to-1 design doesn't close any doors — it just holds the simplest correct position until cables exist to justify the complexity.

---

## Why Before Release

- The current assignment flow is the most confusing part of the mod for new players.
- `PatternKey` cross-references touch nearly every layer (BE, menu, screen, packet, coordinator) — this technical debt compounds with every feature added on top of it.
- Tiers (#17, #18) are much cleaner to implement after this refactor, since tier = slot count on a single paired unit rather than a capacity mismatch between two independently-tiered blocks.
- The storage GUI integration and machine interfaces are not yet complete. Doing this refactor before those features are built avoids having to undo assignment wiring in new screens.
