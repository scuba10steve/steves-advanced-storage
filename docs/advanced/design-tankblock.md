# Tank Block Design (Fluid Storage Component)

**Goal:** Design a block that functions as a fluid storage unit, capable of holding, measuring, and managing different types of liquids within the advanced storage multiblock network.

**Architecture:** `BlockTank` extends `StorageMultiblock` (from the `s3` library), automatically participating in the multiblock BFS. Its core functionality relies on a `BlockTankEntity` which manages the fluid level using NeoForge's `IFluidHandler` capability and `FluidStack`. Fluid volumes are measured in millibuckets (mB); one bucket = 1000 mB.

**Tech Stack:** Java 21, NeoForge Fluid API (`IFluidHandler` capability + `FluidStack`), `StorageMultiblock` Integration.

---
### Block Entity Logic (`BlockTankEntity.java`)

*   **Fluid State:** The `BlockTankEntity` must track the current `FluidStack` (type + amount in mB) and the maximum capacity in mB (configurable via `S3AdvancedConfig`).
*   **Fluid Management:**
    *   **Input/Output:** Fluid insertion and extraction are handled entirely via the `IFluidHandler` capability. External mods (pipes, pumps) interact through that capability automatically — no custom fluid source interface is required.
    *   **Level Maintenance:** Implement logic to handle continuous input/output rates and update the stored fluid level dynamically during the block's `tick()` cycle.
    *   **Overflow/Underflow:** Implement safeguards to prevent fluid levels from exceeding the tank capacity or dropping below zero, potentially emitting an `ERROR` event if limits are breached.
*   **Storage Integration:** When a fluid is stored, the `BlockTankEntity` must report its current state (e.g., "Tank X holds 75% of Water") to the core for holistic storage tracking.

### Multiblock Integration

*   **Component Discovery:** The `BlockTankEntity` must implement a custom `Discovery` method for `AdvancedStorageCoreBlockEntity.scanMultiblock()`.
*   **Multiblock Role:** The tank acts as a *storage component*, contributing to the overall capacity and status of the multiblock.
*   **State Contribution:** It must report its current fluid state (type and level) to the core. This allows the core to aggregate the total usable volume and status of the entire multiblock.

### Data and Interfaces

*   **Fluid Types:** Use `FluidStack` from the NeoForge/Minecraft fluid registry — do not define a custom fluid enum. Any fluid registered in the game (Water, Lava, or modded fluids) is automatically supported.
*   **`IFluidHandler` capability:** The block entity must register and expose an `IFluidHandler` capability. This is the standard NeoForge interoperability surface — any mod using standard fluid pipes or pumps will interact via this capability automatically.
*   **Power draw:** Must contribute a flat FE/t draw accumulated into `totalPowerDraw`, configured via `tank_block_energy_per_tick` in the `advanced_storage_core` section of `S3AdvancedConfig`.
*   **Capacity:** Maximum fluid volume in mB is configurable via `tank_block_capacity_mb` in the `advanced_storage_core` section of `S3AdvancedConfig`.
### GUI and Client Sync

*   **GUI:** Right-click opens a screen showing the current fluid type (icon + name), fill level as a progress bar (current mB / max mB), and a slot for bucket insertion/extraction. Standard player inventory rows below.
*   **Client sync:** Fluid type and level must be synced to the client for GUI rendering. Use `ContainerData` for the numeric level (split-int encoding for mB values exceeding `Short.MAX_VALUE`) and a separate custom sync packet or `BlockEntity.getUpdateTag()` for the `FluidStack` type, since `FluidStack` cannot be encoded directly in `ContainerData`.

### Registration checklist (per AGENTS.md)
    *   `ModBlocks` — register `BlockTank`
    *   `ModBlockEntities` — register `BlockTankEntity`
    *   `ModItems` — register `BlockItem` wrapper
    *   `ModCreativeTabs` — add to creative tab
    *   Blockstate JSON + block model JSON + texture
    *   Lang keys in both `en_us.json` and `es_es.json`
    *   Crafting recipe in `data/s3_advanced/recipe/`
    *   GameTest coverage