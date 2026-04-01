# Steve's Advanced Storage - Integration Matrix

This document tracks:
- integrations that are already implemented
- compatibility surfaces that already exist
- high-value integration options to consider next

## Current integrations (implemented)

### Required mod dependencies

- `s3` (Steve's Simple Storage) is a hard dependency in `neoforge.mods.toml` and is required on both client and server.
- `neoforge` and `minecraft` are required with explicit version ranges.

### JEI integration

- JEI dependencies are declared in `neoforge/build.gradle` (`compileOnly` API + `runtimeOnly` NeoForge runtime for dev).
- `S3AdvancedJEIPlugin` is registered via `@JeiPlugin`.
- `RecipePatternTransferHandler` supports ghost-filling `RecipePatternMenu` from JEI crafting recipes via `GhostSlotFillPacket`.

### Capability-based compatibility surfaces

These are not "named mod integrations", but they are the main interoperability points with other mods:

- Forge Energy capability is exposed on:
  - `AdvancedStorageCoreBlockEntity`
  - `SolarGeneratorBlockEntity`
  - `CoalGeneratorBlockEntity`
- Item handler capability is exposed on:
  - `CoalGeneratorBlockEntity` fuel handler

Any mod that uses standard NeoForge energy/item capabilities can interact with these block entities.

## Documentation status

- No prior dedicated integration/compatibility doc was present.
- This file is now the source of truth for integration tracking.

## Candidate integrations (recommended)

Prioritized by expected player value vs implementation effort.

### 1) Jade / WTHIT overlay support (high value, low-medium effort)

What players get:
- at-a-glance HUD info for advanced blocks (power, active state, queue/progress, linked core)

Implementation idea:
- add optional provider registrations for key block entities
- show compact and configurable tooltip lines

### 2) The One Probe support (high value, low-medium effort)

What players get:
- in-world probe details similar to Jade/WTHIT for players using TOP packs

Implementation idea:
- register probe info providers for the same key block entities
- reuse shared tooltip-building logic to avoid duplication

### 3) EMI/REI recipe transfer parity (medium-high value, medium effort)

What players get:
- non-JEI recipe viewers can transfer recipes into pattern GUIs

Implementation idea:
- mirror the JEI transfer flow with equivalent API hooks
- keep packet and slot mapping logic shared where possible

### 4) Curios slot support for wireless tools (future-facing, medium effort)

What players get:
- optional Curios slot for wireless terminal-like items

Implementation idea:
- only relevant once wireless terminal items are implemented
- gate behind soft dependency checks

### 5) ComputerCraft / CC:Tweaked bridge for automation (high value, higher effort)

What players get:
- programmable automation access to storage/crafting states and actions

Implementation idea:
- expose a constrained peripheral API (read status, trigger requests, query inventory)
- define strict permissions/rate limits to prevent abuse

## Suggested rollout

1. Add an `IntegrationManager` pattern with soft-dependency checks and per-mod registration classes.
2. Implement Jade + TOP first using shared tooltip data builders.
3. Implement EMI/REI transfer support by adapting current JEI slot mapping code.
4. Revisit Curios and CC:Tweaked after wireless and automation APIs stabilize.

## Maintenance notes

- Keep all optional integrations soft-dependent: mod absent must never crash startup.
- Prefer shared adapter/helper layers so each integration is thin.
- Update this file whenever an integration is added, removed, or materially changed.
