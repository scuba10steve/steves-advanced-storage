# Wireless Protocol Design

**Goal:** Design a robust, cross-dimensional communication protocol (`WirelessProtocol`) capable of transmitting state updates between specialized components within the advanced storage network.

**Architecture:** The system utilizes a structured, serialized packet format. Signals are broadcast from specialized components (Transmitter and Receiver), which handle routing and ensuring state consistency across all involved terminals.

**Tech Stack:** Java 21, NeoForge Networking/Serialization, Custom Packet Structures.

---
### Protocol Structure and Design (`WirelessProtocol.java`)

#### 1. Component Specialization
The communication is managed by two specialized entities:
*   **WirelessTransmitterBlockEntity (Tx):** Responsible for generating and broadcasting `WirelessSignalPacket`s. It is triggered by local state changes and acts as the signal origin.
*   **WirelessReceiverBlockEntity (Rx):** Responsible for listening, validating, and applying incoming `WirelessSignalPacket`s. It acts as the signal destination and relay point.

#### 2. Packet Definition (`WirelessSignalPacket`)
*   **Structure:** The primary communication unit is a dedicated `WirelessSignalPacket`, a serializable object.
*   **Mandatory Fields:**
    *   `SenderID`: Unique identifier of the sending component — use `BlockPos` serialized as a long, or a UUID if cross-dimension identity is needed.
    *   `TargetID`: Identifier of the intended receiving component (or a network multicast address/ID).
    *   `ComponentType`: An enumeration indicating the role of the sender: `TX` (transmitter only), `RX` (receiver only), or `TRANSCEIVER` (both, as in `WirelessTerminalBlockEntity`).
    *   `SignalType`: An enumeration defining the intent: `STATE_UPDATE` or `ERROR`. No `QUERY` or `ACK` types — delivery is guaranteed by TCP and queries are resolved via direct registry lookup.
    *   `Payload`: The data specific to the signal type, structured using a dedicated Data Transfer Object (DTO).
    *   `GameTick`: `level.getGameTime()` tick counter for sequence checking and discarding outdated packets. Do not use wall-clock milliseconds — Minecraft runs at 20 TPS and wall-clock time is not deterministic across server restarts.
    *   `HopCount`: Integer tracking relay hops consumed. Discard any packet where `hopCount >= MAX_HOPS` (configurable via `S3AdvancedConfig`) to prevent infinite relay loops.
*   **Serialization:** Implementation must adhere to established NeoForge serialization patterns for cross-instance data transfer.

#### 3. Signal Propagation and Routing
*   **Transmission Trigger:** The Tx component triggers transmission based on local state changes.
*   **Signal Scope:** The effective range/power is defined by the Tx component and must be configurable via `S3AdvancedConfig`.
*   **Routing (Mesh):** The Rx component must implement a relay mechanism. If it receives a packet destined outside its local range, it must re-broadcast the packet with `hopCount` incremented, provided it is not the final target and `hopCount < MAX_HOPS`.
*   **Cross-dimensional routing:** To deliver a packet to a component in a different dimension, use `MinecraftServer.getLevel(ResourceKey<Level>)` to obtain the target `ServerLevel`, then look up the Rx block entity by `BlockPos`. The `SenderID`/`TargetID` must encode the dimension key alongside the block position to support this.
*   **Collision Handling:** Discard packets whose `GameTick` is older than the last applied tick for that `SenderID`. Track the last-applied tick per sender in a `Map<SenderID, Long>` on the Rx entity.

### State Synchronization Logic (`WirelessReceiverBlockEntity.java`)

*   **Packet Reception:** The Rx entity does not poll. Incoming packets are delivered via a NeoForge packet handler callback registered at startup — the handler is invoked by the networking layer when a `WirelessSignalPacket` arrives.
*   **State Update:** Upon successful packet deserialization and validation (SenderID, GameTick), the entity applies the payload data to the local state.
*   **Conflict Resolution:** Last-write-wins based on `GameTick`. If the incoming packet's `GameTick` is greater than the last-applied tick for that sender, apply it; otherwise discard. No majority-vote or consensus mechanism — that complexity is not warranted here.
*   **Delivery guarantee:** NeoForge networking uses TCP; application-level ACK packets are not needed. Remove `ACK` from `SignalType`.

### Multiblock Integration and Discovery

*   **Discovery Inclusion:** Both Tx and Rx components must implement a custom `Discovery` method called by the core's BFS (`AdvancedStorageCoreBlockEntity.scanMultiblock()`).
*   **Discovery Logic:** The logic checks the component type. If it is a Tx, it is registered as a `WirelessTxComponent`; if Rx, it is registered as a `WirelessRxComponent`.
*   **Network Topology Mapping:** The core uses these component registries to build a map of the wireless network topology, allowing the core to accurately calculate overall stability and power requirements of the multiblock structure based on wireless connectivity.