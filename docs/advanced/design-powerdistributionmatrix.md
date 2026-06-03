# Advanced Power Distribution Matrix

> **Note:** This feature does not correspond to an existing open issue. Issue #27 is "Wireless Terminal." Create a dedicated GitHub issue for the Power Distribution Matrix before starting implementation.

## Goal
To transition the power system from a monolithic `totalPowerDraw` to a granular, localized, and prioritized `PowerMatrix`, enabling dynamic power routing and isolated component failure within the multiblock structure.

## Architecture Overview
The system introduces a two-layered power model:
1.  **Global Power Generation:** The `StorageMultiblock` core still tracks the aggregated power draw (`totalPowerDraw`).
2.  **Localized Power Routing:** A new component, `PowerMatrix`, manages power status keyed by `DimensionalCoordinate`. This allows individual component blocks to query the matrix for available power at their specific location/dimension, rather than relying on a simple system-wide boolean.

This decoupling ensures that components can have defined power priorities, allowing the system to allocate power intelligently during periods of high demand.

## Key Components & Data Structures

### 1. `DimensionalCoordinate`
(Reused from Issue #29) This class defines the abstract location of a component within the multiblock, crucial for localizing power queries.

### 2. `PowerMatrix` (New Class: `neoforge/src/main/java/io/github/scuba10steve/s3/advanced/power/PowerMatrix.java`)
This class serves as the core of the new power system.
*   **Storage:** It maintains a map (`Map<DimensionalCoordinate, Map<String, Integer>>`) tracking current usage by component ID at specific coordinates.
*   **Capacity:** It holds a `maxCapacity` configured via `S3AdvancedConfig`.
*   **Methods:**
    *   `reservePower()`: Attempts to allocate power for a component, returning `true` only if capacity allows.
    *   `releasePower()`: Frees up power after a component's tick cycle.
    *   `isComponentPowered()`: Allows components to check their current power status based on their coordinate and priority.

### 3. `StorageMultiblock` Core Integration
The core is responsible for maintaining the `PowerMatrix` instance and integrating the dimensional mapping (from Issue #29). During `scanMultiblock()`, it initializes and populates the necessary structures for the matrix.

## Implementation Details & Flow

### Power Allocation Flow
1.  **Component Registration:** When a block is registered, it must specify its `requiredPowerDraw` and `ComponentPriority` (e.g., 1=Highest, 3=Lowest).
2.  **Tick Cycle:** During a block's `tick()`:
    *   The block retrieves its `DimensionalCoordinate` via the core.
    *   The block queries `StorageMultiblock.getPowerMatrix().isComponentPowered(coord, id, draw)`.
    *   The `PowerMatrix` checks if the component can be powered, applying priority logic if the local coordinate capacity is exceeded.
    *   If powered, the block executes its logic.
    *   Regardless of success, the block's power draw is released (`releasePower`) after the tick.

### Configuration
New configuration fields in `S3AdvancedConfig` (`advanced_power_matrix`) allow external tuning of:
*   `globalMaxCapacity`: The maximum total power the multiblock can support.
*   `defaultPriorityLevel`: Default priority for newly registered components.

## Dependencies

`DimensionalCoordinate` must be designed and implemented before this feature. It is a foundational type used throughout the power matrix and wireless systems — create a dedicated `design-dimensionalcoordinate.md` spec before starting this work. While `DimensionalCoordinate` will be introduced via Issue #29 (Multi-dimensional Upgrade), the power matrix should be designed so it does not require the wireless terminal feature to be complete first.

## Performance Note

The `reservePower()` / `releasePower()` cycle described above runs once per block entity per tick. At scale this becomes a `Map<DimensionalCoordinate, Map<String, Integer>>` lookup on every tick for every powered component. Benchmark against the existing `scanMultiblock()` accumulation model before committing to this approach — if the performance cost is measurable, consider batching power allocation in `scanMultiblock()` rather than per-tick.