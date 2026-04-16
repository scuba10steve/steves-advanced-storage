# Steve's Advanced Storage — Agent Instructions

A NeoForge mod that adds advanced features on top of [Steve's Simple Storage](https://github.com/scuba10steve/steves-storage-system). Requires `s3` (Steve's Simple Storage) as a dependency.

---

## Project Structure

```
neoforge/          Main mod sources (Java 21, NeoForge 21.1.x, MC 1.21.1)
  src/main/java/io/github/scuba10steve/s3/advanced/
    block/         Block classes (extend StorageMultiblock or BaseBlock)
    blockentity/   Block entity classes (extend BaseBlockEntity)
    client/        Client-only event subscribers and screen registrations
    config/        S3AdvancedConfig — all configurable values
    crafting/      Crafting data classes (CraftingEngine, Coordinator, Patterns, etc.)
    gui/
      server/      Menu classes (AbstractContainerMenu subclasses)
      client/      Screen classes (AbstractContainerScreen subclasses)
    init/          DeferredRegister holders (ModBlocks, ModBlockEntities, ModMenuTypes, ModItems)
    item/          Item classes
  src/main/resources/
    assets/s3_advanced/lang/   en_us.json and es_es.json (both must stay in sync)
gametest/          In-game integration tests (NeoForge GameTest framework)
  src/main/java/io/github/scuba10steve/s3/advanced/gametest/
docs/
  superpowers/
    specs/         Design documents written during brainstorming
    plans/         Implementation plans (gitignored — do not commit)
scripts/
  copy.sh          Copies built jar to local GDLauncher modpack for in-game testing
```

---

## Build Commands

```bash
# Build the mod
./gradlew :neoforge:build

# Build the gametest module
./gradlew :gametest:build

# Run in-game integration tests (requires full game server startup)
./gradlew :gametest:runGameTestServer

# Copy built jar to local modpack for manual in-game testing
bash scripts/copy.sh
```

**Always run `./gradlew :neoforge:build` to verify compilation after any change.**

---

## Key Conventions

### Multiblock Components

All new blocks that participate in the multiblock extend `StorageMultiblock` (from the `s3` library). This base class automatically triggers `scanMultiblock()` on the nearest core when the block is placed or removed — no manual wiring needed.

`AdvancedStorageCoreBlockEntity.scanMultiblock()` uses a BFS from `worldPosition` gated by `isPartOfMultiblock(BlockRef)` to discover component block entities. When adding a new multiblock block type, add its discovery logic to the BFS and accumulate its FE/t into `totalPowerDraw`.

### Power

All component blocks draw a flat FE/t from the core's power budget. Config keys live in `S3AdvancedConfig` under the `advanced_storage_core` section. The core tracks total draw in `totalPowerDraw` (reset each `scanMultiblock()` call). `tick()` consumes `totalPowerDraw` per tick; `isPowered()` checks against `totalPowerDraw`.

### New Block Checklist

When adding a new block, ensure all of the following are registered:

1. **ModBlocks** — add the block to `init/ModBlocks.java`
2. **ModBlockEntities** — add the block entity to `init/ModBlockEntities.java` (if applicable)
3. **ModItems** — add a `BlockItem` wrapper in `init/ModItems.java`
4. **ModCreativeTabs** — add the item to the creative tab's `displayItems` in `init/ModCreativeTabs.java`
5. **Texture** — add a block model JSON and texture in `assets/s3_advanced/textures/block/`
6. **Lang keys** — add translation keys to both `en_us.json` and `es_es.json`
7. **Recipe** — add a crafting recipe JSON in `data/s3_advanced/recipe/`

### GUI Pattern

Menus use a three-constructor pattern:
- Public client constructor `(int containerId, Inventory, FriendlyByteBuf)` — reads block pos (and any extra data) from buf
- Public server constructor `(int containerId, Inventory, BlockEntity)` — called from `BlockEntity.createMenu()`
- Private canonical constructor shared by both

Register in `ModMenuTypes` using `IMenuTypeExtension.create(...)`. Register the screen in `ClientEvents.registerScreens(...)`.

### Lang Files

`en_us.json` and `es_es.json` must stay in sync. The build task `validateLang` enforces this — the build will fail if a key exists in one file but not the other. Add `[TRANSLATE]` prefix to new `es_es.json` entries.

### GameTests

Tests use `@GameTestHolder("s3_advanced")` and `@PrefixGameTestTemplate(false)`. The existing `core_with_storage_box` template (3×3 structure with an Advanced Storage Core at local pos (1,1,1) and an adjacent Storage Box) is available for reuse. Add new test classes to the `gametest` module.

### Dependency: Steve's Simple Storage (`s3`)

The `s3` library is the upstream dependency. Notable public APIs used here:
- `StorageMultiblock` — base class for all multiblock component blocks
- `StorageCoreBlockEntity` — base class for the core; `isPartOfMultiblock(BlockRef)` is public
- `BaseBlockEntity`, `BaseItem` — base classes for mod block entities and items
- `BlockRef` — `new BlockRef(Block block, BlockPos pos)`, import from `io.github.scuba10steve.s3.util`

Do not modify the `s3` library to add missing accessors — work around private fields using available public API.

---

## Current mod_version

See `gradle.properties` — `mod_version` and `s3_version` (dependency on Steve's Simple Storage).

---

## Design Docs

Active design documents are in `docs/superpowers/specs/`. Read the relevant spec before implementing a feature. Implementation plans go in `docs/superpowers/plans/` (gitignored).
