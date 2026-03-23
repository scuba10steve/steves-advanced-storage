# Design: Scaffold steves-advanced-storage Standalone Repo

## Overview

Migrate `neoforge/s3-advanced` and `gametest/s3-advanced` out of the `steves-storage-system` monorepo into this standalone repository. Replace all in-tree project references to `:core` and `:neoforge:s3` with the published `steves-simple-storage` JAR from Modrinth Maven.

**Key decisions:**
- Min s3 version required: `0.11.0`
- Initial mod version: `0.1.0`
- `gametest/s3-advanced` source is included but no game-tests CI workflow (deferred)
- `buildSrc` convention plugins are copied and adapted (not inlined or dropped)

---

## Repository Structure

```
steves-advanced-storage/
├── buildSrc/
│   ├── build.gradle                        # copied as-is
│   └── src/main/groovy/
│       ├── s3.repositories.gradle          # copied as-is
│       ├── s3.conventions.gradle           # copied as-is
│       └── s3.neoforge-mod.gradle          # adapted (see Build Changes)
├── neoforge/s3-advanced/                   # copied from monorepo, adapted
├── gametest/s3-advanced/                   # copied from monorepo, adapted
├── .github/workflows/
│   ├── ci.yml                              # adapted
│   ├── release.yml                         # adapted
│   ├── bump-version.yml                    # adapted
│   ├── changelog.yml                       # copied as-is
│   └── openrewrite.yml                     # copied as-is
├── gradle/                                 # copied as-is
├── gradlew / gradlew.bat                   # copied as-is
├── settings.gradle                         # new
├── gradle.properties                       # adapted
├── cliff.toml                              # copied as-is
└── rewrite.yml                             # copied as-is
```

---

## Build File Changes

### `buildSrc/src/main/groovy/s3.neoforge-mod.gradle`

Remove `:core` and JEI dependencies from the `dependencies` block. Add the published s3 JAR as `compileOnly` and `localRuntime`:

```groovy
// Remove:
implementation project(':core')
compileOnly "mezz.jei:jei-1.21.1-common-api:19.27.0.340"
compileOnly "mezz.jei:jei-1.21.1-neoforge-api:19.27.0.340"

// Add:
compileOnly "maven.modrinth:steves-simple-storage:${s3_version}"
localRuntime "maven.modrinth:steves-simple-storage:${s3_version}"
```

`localRuntime` ensures s3 is present on the classpath for local dev runs (client/server) even though it is `compileOnly` for the build. Without it, the dev environment will fail to find the `s3` mod at runtime. The `localRuntime` configuration is provided by the `net.neoforged.moddev` plugin, which `s3.neoforge-mod.gradle` already applies.

Note: `s3.repositories.gradle` already includes the Modrinth maven repository, so no additional repository declaration is needed here. The Jared (BlameJared) maven entry in `s3.repositories.gradle` will become unused once JEI deps are removed — it can be left in place (harmless) or removed for cleanliness; the spec leaves it in.

### `neoforge/s3-advanced/build.gradle`

1. Change `version = advanced_mod_version` → `version = mod_version` (the monorepo used a separate `advanced_mod_version` key; the new repo uses a single `mod_version`)
2. In the `neoForge.mods.'s3-advanced' { }` block, remove `modSourceSets.add(providers.provider { project(':core').sourceSets.main })`
3. Remove the `dependencies { implementation project(':neoforge:s3') }` block (now provided by the convention plugin)

Note: The `id 'java'` plugin declaration and all other content in this file is unchanged. The `processResources` block already expands `mod_version` (not `advanced_mod_version`) — no change needed there.

### `neoforge/s3-advanced/src/main/resources/META-INF/neoforge.mods.toml`

Update the `s3` dependency version range to match `s3_version`:
```toml
[[dependencies.s3_advanced]]
    modId = "s3"
    type = "required"
    versionRange = "[0.11.0,)"   # was [0.10.0,)
    ordering = "BEFORE"
    side = "BOTH"
```

### `gametest/s3-advanced/build.gradle`

In the `dependencies {}` block:
```groovy
// Remove:
implementation project(':core')
implementation project(':neoforge:s3')

// Keep:
implementation project(':neoforge:s3-advanced')   // local module
testRuntimeOnly "org.junit.platform:junit-platform-launcher:1.14.3"  // keep unchanged

// Add (this module applies net.neoforged.moddev directly but not s3.neoforge-mod,
// so it does not inherit localRuntime from the convention plugin):
localRuntime "maven.modrinth:steves-simple-storage:${s3_version}"
```

In the `neoForge.mods {}` block: remove the entire `s3 { ... }` named mod block (it contained only `modSourceSets` calls to `:core` and `:neoforge:s3`, which are now provided by the published JAR at runtime). Keep the `'s3_advanced' { ... }` block unchanged — it retains its local `sourceSet sourceSets.main` and the lazy `modSourceSets` reference exactly as it appears in the monorepo:
```groovy
modSourceSets.add(providers.provider { project(':neoforge:s3-advanced').sourceSets.main })
```

The `data` run's `'--existing', rootProject.file('neoforge/s3-advanced/src/main/resources/...')` path is correct as-is — `neoforge:s3-advanced` is still a sibling subproject in the new repo.

### `gradle.properties`

The monorepo properties `advanced_mod_id`, `advanced_mod_name`, and `advanced_mod_version` are dropped entirely. They are only referenced in `gradle.properties` itself and in `neoforge/s3-advanced/build.gradle` (`advanced_mod_version` — already handled above). No build scripts or source files reference `advanced_mod_id` or `advanced_mod_name` by property name.

```properties
# Gradle config (unchanged)
org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=false
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

# Minecraft / NeoForge (unchanged)
minecraft_version=1.21.1
minecraft_version_range=[1.21.1,1.22)
neoforge_version=21.1.218
neoforge_version_range=[21.1,22)
loader_version_range=[4,)

# Mod
mod_id=s3_advanced
mod_name=Steve's Advanced Storage
mod_license=MIT
mod_version=0.1.0
mod_authors=scuba10steve
mod_description=Advanced features for Steve's Simple Storage.

# Dependency versions
s3_version=0.11.0
```

### `settings.gradle`

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = 'NeoForged'
            url = 'https://maven.neoforged.net/releases'
        }
    }
    plugins {
        id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention'
}

rootProject.name = 'steves-advanced-storage'
include 'neoforge:s3-advanced'
include 'gametest:s3-advanced'
```

---

## GitHub Actions Workflows

### `ci.yml`
No structural changes needed. `./gradlew build` works as-is with no `:s3` or `:core` module paths.

### `release.yml`
Four changes from the monorepo version:
1. JAR path: `neoforge/s3/build/libs/s3-*.jar` → `neoforge/s3-advanced/build/libs/s3-advanced-*.jar` (appears in `softprops/action-gh-release` `files:` and both mc-publish `files:` fields)
2. Release name in **both** mc-publish steps (Modrinth and CurseForge): `Steve's Simple Storage v...` → `Steve's Advanced Storage v...` (the `softprops/action-gh-release` `name: v${{ steps.version.outputs.version }}` is fine as-is)
3. Repo variables: `MODRINTH_PROJECT_ID` / `CURSEFORGE_PROJECT_ID` point to the new s3-advanced platform projects
4. mc-publish `dependencies`:
   - Modrinth step: replace JEI optional entry with `steves-simple-storage` as required:
     ```yaml
     dependencies: |
       steves-simple-storage@0.11.0(required){modrinth:steves-simple-storage}
     ```
   - CurseForge step: replace existing `dependencies:` block with a TODO placeholder until the CurseForge project numeric ID for s3 is known:
     ```yaml
     dependencies: |
       # TODO: add steves-simple-storage required dep once CurseForge project ID is known
     ```
5. The commented-out Nexus Mods publish step at the bottom of the file references the s3 JAR `filename:` path and `display_name` — update those to `s3-advanced` values to keep the comment accurate if/when it is enabled.

### `bump-version.yml`
Copied as-is. The monorepo workflow already targets `mod_version=` and no `advanced_mod_version` handling exists in the file — no changes needed.

### `changelog.yml`
Copied as-is.

### `openrewrite.yml`
Copied as-is.

### `game-tests.yml`
Not included. `gametest/s3-advanced` source is present but game-tests CI is deferred.

---

## Out of Scope

- Modrinth / CurseForge project creation (manual steps, done before first release)
- GitHub repo settings, branch protection, PR template (manual setup)
- Cleanup of the monorepo (removing `neoforge/s3-advanced` and `gametest/s3-advanced`) — done after first verified release from this repo
