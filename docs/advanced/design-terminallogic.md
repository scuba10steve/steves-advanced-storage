# Terminal Block Entity Logic and Multiblock Discovery Design

**Goal:** Design the internal state machine and event handling for `WirelessTerminalBlockEntity`, ensuring it correctly manages wireless signals and participates in the advanced storage multiblock structure.

**Architecture:** The Block Entity acts as the local intelligence. It utilizes the `WirelessProtocol` to receive external state updates, maintains a local copy of the network state, and feeds its status into the `StorageCoreBlockEntity` to maintain the integrity of the entire multiblock structure.

**Tech Stack:** Java 21, NeoForge Events, State Machines, Multiblock Discovery Logic.

---
### Block Entity State Machine Design (`WirelessTerminalBlockEntity.java`)

*   **States:** Define clear, enumerated states for the terminal:
    *   `IDLE`: Confirmed peer in registry; no active send or receive in progress.
    *   `CONNECTING`: Entered when `scanMultiblock()` registers this terminal but no peer is found in the server-side wireless registry (keyed by dimension `ResourceKey<Level>` + `BlockPos`). The terminal re-checks the registry each game tick via `tick()`. Transitions to `IDLE` when a peer is confirmed, or to `ERROR` after a configurable number of ticks without finding one.
    *   `SYNCING`: A `STATE_UPDATE` packet is being processed and applied to local state.
    *   `TRANSMITTING`: A `STATE_UPDATE` packet has been dispatched; returns to `IDLE` on the next tick (TCP delivery guaranteed — no wait required).
    *   `ERROR`: Entered when: the multiblock core is unpowered, no peer is found within `max_range` ticks in `CONNECTING`, an incoming packet references an unrecognized `SenderID`, or the target dimension's `ServerLevel` is unavailable. Clears back to `CONNECTING` on the next successful `scanMultiblock()`.
*   **State Transition Logic:** Transitions are triggered by:
    1.  **Game tick** (`tick()`): drives time-based transitions such as the `CONNECTING` retry counter and the `ERROR` recovery check.
    2.  **Packet receipt:** NeoForge packet handler callback delivers a `WirelessSignalPacket` — not polled.
    3.  **Internal events:** Local state changes (e.g., power state change, `scanMultiblock()` result).
*   **Event Emission:** When a state transition occurs or a critical state change is registered, the Block Entity must emit a custom NeoForge event (`WirelessTerminalEvent`) to notify the rest of the mod, particularly the multiblock core.

### Wireless Packet Processing Logic

*   **Deserialization:** The entity must implement a reliable mechanism to deserialize the `WirelessSignalPacket` received from the network stack.
*   **Validation:** Upon receiving a `WirelessSignalPacket`, validate the `SenderID` and `GameTick` fields before applying.
*   **Conflict Resolution:** Last-write-wins on `GameTick`. If the packet's `GameTick` is greater than the last-applied tick recorded for that sender, apply the payload; otherwise discard. No majority-vote or consensus mechanism.
*   **Delivery guarantee:** NeoForge networking uses TCP — no ACK packets are required or sent.

### Multiblock Integration and Discovery

*   **Discovery Inclusion:** The `WirelessTerminalBlockEntity` must implement a custom `Discovery` method called by the core's BFS (`AdvancedStorageCoreBlockEntity.scanMultiblock()`).
*   **Discovery Logic:** The logic should check if the block entity possesses a wireless transceiver component. If so, it should return itself along with the necessary component data (e.g., `FE/t` draw, type identifier).
*   **Multiblock Status Reporting:** The entity needs a method to report its health/connectivity status to the core. This allows the core to correctly calculate the overall stability and power requirements of the multiblock structure, which includes the wireless network's status.

