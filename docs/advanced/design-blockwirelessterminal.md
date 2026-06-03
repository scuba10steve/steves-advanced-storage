# Wireless Terminal Component Design

**Goal:** Design a persistent block that acts as a transceiver for cross-dimensional, state-synchronizing wireless signals.

**Architecture:** The block extends `StorageMultiblock` (from the `s3` library). Its primary function is to host a `WirelessTerminalBlockEntity` which acts as **both a Transmitter (Tx) and Receiver (Rx)** in the `WirelessProtocol` layer. See `design-wirelessprotocol.md` for the full protocol design — the Tx/Rx split exists so other block types can participate in wireless signaling without implementing both roles.

**Tech Stack:** Java 21, NeoForge, `StorageMultiblock`, `WirelessProtocol` (new `network/` subpackage).

---
### Design Considerations

#### 1. Block Definition (`BlockWirelessTerminal`)
*   **Inheritance:** Must extend `StorageMultiblock` (from the `s3` library). This automatically triggers `scanMultiblock()` on the nearest core when placed or removed.
*   **Power Draw:** Must contribute a defined, flat FE/t draw accumulated into `totalPowerDraw` on `AdvancedStorageCoreBlockEntity`, configured via a key in the `advanced_storage_core` section of `S3AdvancedConfig`.
*   **Model/Texture:** Requires a block model JSON, texture in `assets/s3_advanced/textures/block/`, and a blockstate JSON in `data/s3_advanced/blockstates/`.

#### 2. Block Entity Logic (`WirelessTerminalBlockEntity`)
*   **Protocol Role:** Acts as both Tx and Rx. It generates `WirelessSignalPacket`s in response to local state changes (Tx role) and processes incoming packets from other nodes (Rx role). See `design-wirelessprotocol.md` for packet structure and routing.
*   **State Management:** Maintains the current terminal state: `IDLE`, `CONNECTING`, `SYNCING`, `TRANSMITTING`, or `ERROR`. Peer discovery uses a server-side registry keyed by dimension `ResourceKey<Level>` + `BlockPos`; `scanMultiblock()` registers this terminal and triggers the initial peer lookup. See `design-terminallogic.md` for the full state machine.
*   **State Synchronization:** When a packet is received, the Block Entity updates its internal state and emits a `WirelessTerminalEvent` to notify the multiblock core and GUI.
*   **Power:** Signaling power draw is tracked by this entity and aggregated into `totalPowerDraw` by the core during `scanMultiblock()`.

#### 3. Integration Points
*   **Networking:** `WirelessProtocol` and `WirelessSignalPacket` live in the `network/` subpackage: `io.github.scuba10steve.s3.advanced.network`. See `design-wirelessprotocol.md` for full packet and routing design.
*   **Multiblock:** Add discovery logic to `AdvancedStorageCoreBlockEntity.scanMultiblock()` — the terminal must be detected by the BFS and its FE/t accumulated into `totalPowerDraw`.
*   **Registration checklist** (per AGENTS.md):
    *   `ModBlocks` — register `BlockWirelessTerminal`
    *   `ModBlockEntities` — register `WirelessTerminalBlockEntity`
    *   `ModItems` — register `BlockItem` wrapper
    *   `ModCreativeTabs` — add to creative tab
    *   Blockstate JSON + block model JSON + texture
    *   Lang keys in both `en_us.json` and `es_es.json`
    *   Crafting recipe in `data/s3_advanced/recipe/`
    *   GameTest coverage using the `core_with_storage_box` template as a base