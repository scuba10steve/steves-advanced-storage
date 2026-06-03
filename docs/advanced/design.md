# Steve's Advanced Storage — Design Document

## Overview

**Name:** Steve's Advanced Storage
**Mod ID:** `s3_advanced`
**Relationship:** Add-on mod requiring Steve's Simple Storage (S3)
**Description:** Advanced features for Steve's Simple Storage
**Output:** Separate JAR (`s3_advanced-<version>.jar`)

## Project Structure

This mod lives in its own repository (`steves-advanced-storage`), separate from Steve's Simple Storage.

```
steves-advanced-storage/
├── neoforge/          Main mod sources (Java 21, NeoForge 21.1.x, MC 1.21.1)
│   └── src/main/java/io/github/scuba10steve/s3/advanced/
│       ├── block/         Block classes (extend StorageMultiblock or BaseBlock)
│       ├── blockentity/   Block entity classes
│       ├── client/        Client-only event subscribers and screen registrations
│       ├── config/        S3AdvancedConfig
│       ├── crafting/      CraftingEngine, Coordinator, Patterns, etc.
│       ├── gui/
│       │   ├── server/    Menu classes (AbstractContainerMenu subclasses)
│       │   └── client/    Screen classes (AbstractContainerScreen subclasses)
│       ├── init/          DeferredRegister holders
│       ├── item/          Item classes
│       ├── network/       WirelessProtocol, WirelessSignalPacket (planned)
│       └── power/         PowerMatrix (planned)
├── gametest/          In-game integration tests (NeoForge GameTest framework)
├── docs/
│   ├── advanced/      Feature design documents
│   └── superpowers/
│       ├── specs/     AI-assisted brainstorming specs
│       └── plans/     Implementation plans (gitignored)
├── scripts/
│   └── copy.sh        Copies built jar to local modpack for testing
├── gradle.properties
└── AGENTS.md          Authoritative project conventions reference
```

## Convention Plugin (buildSrc)

`s3.neoforge-mod.gradle` applies shared config to all NeoForge subprojects:

- `net.neoforged.moddev` plugin
- Java 21 toolchain
- NeoForge version from `gradle.properties`
- Common run configs (client, server)
- `:core` dependency
- JEI `compileOnly` dependency
- JUnit test dependencies
- Core source set in `mods` block

Each subproject's `build.gradle` specifies only:
- `archivesName`
- Mod-specific dependencies (e.g. advanced depends on `:neoforge:s3`)
- Unique run configs (e.g. datagen for s3)
- `processResources` for its `neoforge.mods.toml`

## Dependency Chain

```
core  <──  neoforge/s3  <──  neoforge/advanced
              │                    │
              └── JEI (compileOnly)│
                                   └── JEI (compileOnly)
```

- `neoforge/s3` depends on `:core`
- `neoforge/advanced` depends on `:core` and `:neoforge:s3`
- S3 required at runtime via `neoforge.mods.toml` dependency declaration
- Advanced JAR contains only its own classes (does not bundle S3)

## JAR Outputs

- `neoforge/s3/build/libs/s3-<version>.jar` — includes core classes
- `neoforge/advanced/build/libs/s3_advanced-<version>.jar` — its own classes only

## Mod Identity

- **Mod ID:** `s3_advanced`
- **Package:** `io.github.scuba10steve.s3.advanced`
- **Entry point:** `StevesAdvancedStorage` with `@Mod("s3_advanced")`
- **Assets:** `assets/s3_advanced/`, `data/s3_advanced/`
- **Lang:** `assets/s3_advanced/lang/en_us.json`

## Docker / Local Server

`scripts/server.sh` copies both JARs to `server/mods/`:
- S3 JAR: required
- Advanced JAR: optional (skipped if not built, no error)

## Release Strategy

- Shared version number (`mod_version` in `gradle.properties`)
- Single GitHub release with both JARs as assets
- Separate Modrinth and CurseForge project listings
- Advanced lists S3 as a required dependency on both platforms

## Advanced Storage Core Block

The **Advanced Storage Core** is the core block of the companion mod. When placed in an S3 multiblock, it upgrades the system from basic to advanced, unlocking all advanced features.

- **Requires power** — the advanced system consumes Forge Energy (FE) to operate
- **Recipe:** Diamonds + Redstone + Iron + Storage Core
- **Multiblock role:** Acts as an upgrade component; all advanced features require its presence
- **Detection:** The Storage Core scans for an Advanced Storage Core during multiblock validation, similar to how it detects Sort Box or Search Box

See [planned-features.md](planned-features.md) for the full feature roadmap.

## Current Status

The mod is past the skeleton phase. Shipped features as of v0.5.0 include: Advanced Storage Core, Block Storage I, Config Block, Solar Generator, Coal Generator, Auto-Crafter, Recipe Memory Box, Machine Interface, and 16-color Storage Cables. See `docs/advanced/planned-features.md` for the full feature roadmap.
