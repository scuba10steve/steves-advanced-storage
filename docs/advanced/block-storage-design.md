# Block Storage — Design

## Overview

**Block Storage** (`block_storage`) is a high-density storage rack: a single multiblock component that holds up to 16 storage box items in an internal GUI. Each occupied slot contributes that box tier's capacity to the multiblock total and draws a flat FE/t from the Advanced Storage Core's power budget.

The name is an homage to AWS EBS (Elastic Block Storage).

---

## Block Behavior

- Placed adjacent to the multiblock like any other storage component
- Requires an **Advanced Storage Core** in the multiblock to function; if the connected core is a plain `StorageCoreBlockEntity` (not advanced), the rack is inert — it contributes no capacity and its GUI is non-functional
- Right-click opens the 16-slot GUI
- Storage box items (any tier: Basic through Ultimate) may be inserted into slots
- Non-storage-box items are hard-rejected on insert — the slot will not accept them
- Each occupied slot contributes the inserted box tier's item capacity to the multiblock total
- Each occupied slot draws a flat, configurable FE/t from the Advanced Core's power budget (empty slots draw nothing)
- Multiple Block Storage racks may exist in the same multiblock; each rack's occupied slots contribute independently to capacity and power draw
- Slot contents are NBT-persisted and survive chunk unload/reload

---

## GUI

- 4×4 grid of 16 slots
- Only accepts storage box items (Basic, Condensed, Compressed, Super, Ultra, Hyper, Ultimate Storage Box)
- Invalid item inserts are rejected outright — no partial insert, no slot highlight
- Standard player inventory rows below the rack slots

---

## Power Model

- Flat FE/t cost per **occupied** slot (configurable via `S3AdvancedConfig`, e.g. 10 FE/t per slot)
- Empty slots cost nothing; a fully-loaded rack of 16 Ultimate boxes draws 16× the per-slot cost
- `AdvancedStorageCoreBlockEntity` introduces a `totalPowerDraw` field (int, FE/t) computed during each `scanMultiblock()` call. It starts at the existing base cost (`CORE_ENERGY_PER_TICK`) and accumulates occupied-slot costs from every Block Storage rack found. The `tick()` method consumes `totalPowerDraw` FE each tick instead of the fixed base value.
- `ContainerData` is expanded from 6 to 7 slots to expose `totalPowerDraw` with split-int encoding (same pattern as energy storage slots 0–3):
  - Slots 0–1: energy stored (low/high)
  - Slots 2–3: max energy (low/high)
  - Slots 4–5: `totalPowerDraw` (low/high)
  - Slot 6: `isPowered`
- The client-side screen must be updated to read `isPowered` from slot 6 instead of slot 5
- If the core loses power, the entire advanced system (including Block Storage capacity) stops functioning per existing Advanced Core behavior

---

## Multiblock Integration

- `AdvancedStorageCoreBlockEntity` overrides `scanMultiblock()` to handle Block Storage racks in addition to the standard s3 component types
- For each rack found during the scan, the core:
  1. Iterates all 16 slots
  2. Resolves each occupied slot's box item capacity by casting the `BlockItem`'s block to `BlockStorage` and calling `getCapacity()`. This cast is safe because slot validation guarantees only `BlockItem`-wrapping-`BlockStorage` items can occupy slots. `getCapacity()` is config-driven and may return a default fallback if config is unavailable.
  3. Adds the summed capacity to the multiblock total
  4. Accumulates `occupied slot count × BLOCK_STORAGE_SLOT_ENERGY_PER_TICK` into `totalPowerDraw`
- When a box item is inserted or removed from the rack's inventory, the rack's block entity triggers a multiblock rescan by casting the inherited `core` field (from `MultiblockBlockEntity`) to `AdvancedStorageCoreBlockEntity` and calling `scanMultiblock()` on it. The `core` field is populated during `tick()` via `StorageMultiblock.attemptMultiblock()`; if `core` is null (not yet connected), no rescan is needed.
- Items are stored in the `StorageInventory` on the `StorageCoreBlockEntity` as normal — the rack slots only determine capacity, not item placement

---

## Out of Scope

- Tiered power cost per slot (flat cost only)
- Standalone power input on the rack itself (power flows through the Advanced Core)
- New storage media items — existing storage box items are used directly
- Block Storage functioning with a plain (non-advanced) `StorageCore`
