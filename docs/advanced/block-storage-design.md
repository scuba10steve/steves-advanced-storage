# Block Storage — Design

## Overview

**Block Storage** is a family of four tiered multiblock components for the advanced S3 storage system. Each tier is a high-density storage rack that holds storage box items in an internal GUI. Each occupied slot contributes that box tier's capacity to the multiblock total and draws a flat FE/t from the Advanced Storage Core's power budget.

Named as a family after AWS EBS (Elastic Block Storage).

| Block | ID | Slots | GUI layout | Power/slot (default) |
|---|---|---|---|---|
| Block Storage I | `block_storage_1` | 8 | 2×4 | 10 FE/t |
| Block Storage II | `block_storage_2` | 16 | 4×4 | 20 FE/t |
| Block Storage III | `block_storage_3` | 32 | 4×8 | 40 FE/t |
| Block Storage IV | `block_storage_4` | 64 | 8×8 | 80 FE/t |

Higher tiers are crafted using the previous tier as an ingredient. New tiers can be added in the future by adding a new block with a higher slot count — no changes to the scan or entity logic are required.

---

## Block Behavior (all tiers)

- Placed adjacent to the multiblock like any other storage component
- Requires an **Advanced Storage Core** in the multiblock to function; if the connected core is a plain `StorageCoreBlockEntity` (not advanced), the rack is inert — it contributes no capacity and its GUI is non-functional
- Right-click opens the slot GUI for that tier
- Storage box items (any tier: Basic through Ultimate) may be inserted into any slot
- Non-storage-box items are hard-rejected on insert — the slot will not accept them
- Each occupied slot contributes the inserted box tier's item capacity to the multiblock total
- Each occupied slot draws a flat, configurable FE/t from the Advanced Core's power budget (empty slots draw nothing)
- Multiple racks of any tier may exist in the same multiblock; each contributes independently
- Slot contents are NBT-persisted and survive chunk unload/reload

---

## GUI

- Slot grid sized to the tier (see table above)
- Only accepts storage box items (Basic, Condensed, Compressed, Super, Ultra, Hyper, Ultimate Storage Box)
- Invalid item inserts are rejected outright — no partial insert, no slot highlight
- Standard player inventory rows below the rack slots

---

## Power Model

- Flat FE/t cost per **occupied** slot, configurable per tier via `S3AdvancedConfig` (section `advanced_storage_core`)
- Empty slots cost nothing
- Power doubles each tier: Tier I base × 1, Tier II × 2, Tier III × 4, Tier IV × 8
- `AdvancedStorageCoreBlockEntity` introduces a `totalPowerDraw` field (int, FE/t) computed during each `scanMultiblock()` call. It starts at the existing base cost (`CORE_ENERGY_PER_TICK`) and accumulates occupied-slot costs from every Block Storage rack found. The `tick()` method consumes `totalPowerDraw` FE each tick instead of the fixed base value.
- `ContainerData` is expanded from 6 to 7 slots to expose `totalPowerDraw` with split-int encoding (same pattern as energy storage slots 0–3):
  - Slots 0–1: energy stored (low/high)
  - Slots 2–3: max energy (low/high)
  - Slots 4–5: `totalPowerDraw` (low/high)
  - Slot 6: `isPowered`
- The client-side screen must be updated to read `isPowered` from slot 6 instead of slot 5
- If the core loses power, the entire advanced system (including Block Storage capacity) stops functioning per existing Advanced Core behavior

### Config Values

| Key | Section | Default | Min | Max |
|-----|---------|---------|-----|-----|
| `block_storage_1_slot_energy_per_tick` | `advanced_storage_core` | `10` | `0` | `Integer.MAX_VALUE` |
| `block_storage_2_slot_energy_per_tick` | `advanced_storage_core` | `20` | `0` | `Integer.MAX_VALUE` |
| `block_storage_3_slot_energy_per_tick` | `advanced_storage_core` | `40` | `0` | `Integer.MAX_VALUE` |
| `block_storage_4_slot_energy_per_tick` | `advanced_storage_core` | `80` | `0` | `Integer.MAX_VALUE` |

---

## Architecture

All four tiers share a single `BlockStorageBlockEntity` class. The block class for each tier passes its slot count into the entity on construction via a constructor parameter. The entity exposes `getSlotCount()` which drives both the GUI slot grid and the scan loop iteration. No per-tier special casing exists in either the entity or the `scanMultiblock()` override — adding a future Tier V requires only a new block class and registration.

---

## Multiblock Integration

- `AdvancedStorageCoreBlockEntity` overrides `scanMultiblock()` to handle Block Storage racks of any tier
- For each rack found during the scan, the core:
  1. Calls `getSlotCount()` on the rack's block entity and iterates all slots
  2. Resolves each occupied slot's box item capacity by casting the `BlockItem`'s block to `BlockStorage` and calling `getCapacity()`. This cast is safe because slot validation guarantees only `BlockItem`-wrapping-`BlockStorage` items can occupy slots. `getCapacity()` is config-driven and may return a default fallback if config is unavailable.
  3. Adds the summed capacity to the multiblock total
  4. Accumulates `occupied slot count × <tier's slot energy config>` into `totalPowerDraw`
- When a box item is inserted or removed from the rack's inventory, the rack's block entity triggers a multiblock rescan by casting the inherited `core` field (from `MultiblockBlockEntity`) to `AdvancedStorageCoreBlockEntity` and calling `scanMultiblock()` on it. If `core` is null or not `instanceof AdvancedStorageCoreBlockEntity`, skip the rescan.
- Items are stored in the `StorageInventory` on the `StorageCoreBlockEntity` as normal — the rack slots only determine capacity, not item placement

---

## Recipes

Each tier is crafted using the previous tier as a central ingredient plus additional materials. Exact recipes are not specified here — they are a balancing decision made at implementation time.

---

## Out of Scope

- Tiered power cost per slot type (flat per-slot cost per rack tier only)
- Standalone power input on the rack itself (power flows through the Advanced Core)
- New storage media items — existing storage box items are used directly
- Block Storage functioning with a plain (non-advanced) `StorageCore`
