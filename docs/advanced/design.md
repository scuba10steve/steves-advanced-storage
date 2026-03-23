# Steve's Advanced Storage — Design Document

## Overview

**Name:** Steve's Advanced Storage
**Mod ID:** `s3-advanced`
**Relationship:** Add-on mod requiring Steve's Simple Storage (S3)
**Description:** Advanced features for Steve's Simple Storage
**Output:** Separate JAR (`s3-advanced-<version>.jar`)

## Project Structure

```
steves-storage-system/
├── buildSrc/
│   ├── build.gradle
│   └── src/main/groovy/
│       └── s3.neoforge-mod.gradle         # convention plugin
├── core/                                   # platform-agnostic code (unchanged)
│   ├── build.gradle
│   └── src/
├── neoforge/
│   ├── s3/                                # S3 mod (current neoforge source)
│   │   ├── build.gradle
│   │   └── src/
│   └── advanced/                          # Steve's Advanced Storage
│       ├── build.gradle
│       └── src/main/
│           ├── java/io/github/scuba10steve/s3/advanced/
│           │   └── StevesAdvancedStorage.java
│           └── resources/
│               └── META-INF/neoforge.mods.toml
├── gametest/
│   ├── s3/                                # game tests for S3
│   │   ├── build.gradle
│   │   └── src/
│   └── advanced/                          # game tests for Advanced (skeleton)
│       ├── build.gradle
│       └── src/
├── settings.gradle
├── gradle.properties
└── docker-compose.yml
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
- `neoforge/advanced/build/libs/s3-advanced-<version>.jar` — its own classes only

## Mod Identity

- **Mod ID:** `s3-advanced`
- **Package:** `io.github.scuba10steve.s3.advanced`
- **Entry point:** `StevesAdvancedStorage` with `@Mod("s3-advanced")`
- **Assets:** `assets/s3-advanced/`, `data/s3-advanced/`
- **Lang:** `assets/s3-advanced/lang/en_us.json`

### gradle.properties additions

```properties
advanced_mod_id=s3-advanced
advanced_mod_name=Steve's Advanced Storage
```

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

## Initial Scope

Skeleton only — no features. Just the module structure, build config, entry point, and mod metadata.
