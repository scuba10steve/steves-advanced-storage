# Config Block — Design

## Overview

**Config Block** (`config_block`) is a multiblock component that consolidates the five s3 feature blocks into a single block. Instead of placing individual CraftingBox, SearchBox, SortBox, SecurityBox, and StatisticsBox blocks in the multiblock, the player inserts those items into the Config Block's 5-slot GUI to enable those features.

Named after AWS Config, which tracks and stores configuration items for resources.

**Prerequisite:** The Block Storage feature must be implemented first. That feature introduces:
- The `totalPowerDraw` field on `AdvancedStorageCoreBlockEntity` and the 7-slot `ContainerData` layout that Config Block accumulates costs into
- The `scanMultiblock()` override on `AdvancedStorageCoreBlockEntity` that Config Block's scan logic extends

---

## Block Behavior

- Placed adjacent to the multiblock like any other component
- Requires an **Advanced Storage Core** to function; if the connected core is a plain `StorageCoreBlockEntity` (not advanced), the block is inert — it contributes no features and its GUI is non-functional
- Right-click opens the 5-slot GUI (1×5 horizontal grid)
- Accepts any of the five s3 feature block items in any slot: CraftingBox, SearchBox, SortBox, SecurityBox, StatisticsBox
- No slot-type restriction; order does not matter
- Duplicate feature types across slots are allowed and harmless — the multiblock scan sets each feature flag once regardless of how many times it appears
- Each occupied slot draws a flat, configurable FE/t from the Advanced Core's power budget (empty slots draw nothing)
- Multiple Config Blocks may exist in the same multiblock; each contributes independently
- Inserting or removing a feature block item triggers an immediate multiblock rescan
- Slot contents are NBT-persisted and survive chunk unload/reload

---

## GUI

- 1×5 horizontal grid of 5 slots
- Accepts only the five s3 feature block items (CraftingBox, SearchBox, SortBox, SecurityBox, StatisticsBox)
- Invalid item inserts are hard-rejected — no partial insert, no slot highlight
- Standard player inventory rows below the slots

---

## Power Model

- Flat FE/t cost per **occupied** slot (configurable via `S3AdvancedConfig`)
- Empty slots cost nothing
- Cost is accumulated into `totalPowerDraw` on `AdvancedStorageCoreBlockEntity` during `scanMultiblock()`, alongside Block Storage rack costs
- If the core loses power, the entire advanced system stops functioning per existing Advanced Core behavior

### Config Values

| Key | Section | Default | Min | Max |
|-----|---------|---------|-----|-----|
| `config_block_slot_energy_per_tick` | `advanced_storage_core` | `10` | `0` | `Integer.MAX_VALUE` |

---

## Multiblock Integration

- `BlockConfigBlock` must extend `StorageMultiblock` so that `scanMultiblock()`'s `getValidNeighbors()` recursion includes it in the multiblock set. All existing s3 feature block classes follow this pattern.
- `AdvancedStorageCoreBlockEntity`'s `scanMultiblock()` override (introduced by Block Storage) is extended to detect Config Blocks alongside Block Storage racks
- For each Config Block found during the scan, the core:
  1. Iterates all 5 slots
  2. Maps each occupied slot's item to its corresponding feature flag: CraftingBox → `hasCraftingBox`, SearchBox → `hasSearchBox`, SortBox → `hasSortBox`, SecurityBox → `hasSecurityBox`, StatisticsBox → `hasStatisticsBox`
  3. Sets the corresponding flag(s) on the core
  4. Accumulates `occupied slot count × CONFIG_BLOCK_SLOT_ENERGY_PER_TICK` into `totalPowerDraw`
- The scan must reset `totalPowerDraw` to the base core cost before accumulating per-slot costs, consistent with the reset-then-accumulate pattern of the base `scanMultiblock()`
- When a feature block item is inserted or removed, the block entity triggers a rescan by casting the inherited `core` field to `AdvancedStorageCoreBlockEntity` and calling `scanMultiblock()`. If `core` is null or not `instanceof AdvancedStorageCoreBlockEntity`, skip the rescan — the block is inert in that configuration.
- A Config Block and standalone feature blocks (e.g. a CraftingBox placed directly in the multiblock) may coexist — flags are set additively during the scan with no conflict

---

## Out of Scope

- Per-feature-type power costs (flat per-slot cost only)
- Slot-type restrictions (any feature item in any slot)
- Preventing duplicate feature types (harmless, not enforced)
- Config Block functioning with a plain (non-advanced) `StorageCore`
