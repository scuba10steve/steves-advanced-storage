# Steve's Advanced Storage — Planned Features

Features planned for Steve's Advanced Storage. These are tracked as GitHub issues on this [repository](https://github.com/scuba10steve/steves-advanced-storage/issues).

## Advanced Storage Core Block

The **Advanced Storage Core** is a new multiblock component that upgrades a basic S3 storage system to an advanced system. When placed in the multiblock, it unlocks all advanced features.

- **Requires power** — the advanced system must be powered (Forge Energy / FE) to function
- **Recipe:** Diamonds + Redstone + Iron + Storage Core
- **Effect:** Converts the multiblock from a basic storage system to an advanced storage system
- **Prerequisite:** All features below require an Advanced Storage Core in the multiblock

## New Blocks / Components

- **Smelting Box tiers** — tiered versions of the smelting box, similar to storage box tiers ([#1](https://github.com/scuba10steve/steves-advanced-storage/issues/1))
- **Recipe Memory Box (RMB) / Auto-Crafter** — the base `RecipeMemoryBox` and `AutoCrafter` blocks are implemented; tiered versions remain open as [#17](https://github.com/scuba10steve/steves-advanced-storage/issues/17) (Tiered Recipe Memory Box) and [#18](https://github.com/scuba10steve/steves-advanced-storage/issues/18) (Tiered Auto-Crafter). See `docs/advanced/rmb-autocrafter-1to1-pairing.md` for the approved pairing redesign.
- **Colored cables** — ✅ shipped in v0.5.0 (PR #35); 16 color variants with directional connection logic
- **Tank Block** — liquid/fluid storage integrated with the storage system ([#5](https://github.com/scuba10steve/steves-advanced-storage/issues/5))
- **Power Cell** — Forge Energy (FE/RF) storage integrated with the storage system, compatible with other redstone-flux mods


## Wireless Access

- **Wireless Terminal** — access the storage system remotely without physical connection to the multiblock
- **Multi-dimensional Upgrade** — upgrade for the Wireless Terminal enabling cross-dimension access

## Networked Storage

- **Multi-system networking** — connect multiple Storage Core systems via physical bridges, conduits, or wireless links ([#4](https://github.com/scuba10steve/steves-advanced-storage/issues/4))
