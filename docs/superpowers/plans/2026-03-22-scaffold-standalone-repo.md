# Scaffold steves-advanced-storage Standalone Repo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate `neoforge/s3-advanced` and `gametest/s3-advanced` from the `steves-storage-system` monorepo into this standalone repository, replacing all in-tree project references with the published Modrinth Maven JAR.

**Architecture:** Copy source modules and build infrastructure from the monorepo, then make targeted edits to replace `:core` / `:neoforge:s3` project dependencies with `maven.modrinth:steves-simple-storage:${s3_version}`. The `buildSrc` convention plugins are adapted rather than inlined or dropped. Verification at each step is `./gradlew build`.

**Tech Stack:** Gradle 8, NeoForge moddev plugin, NeoForge 21.1.218, Minecraft 1.21.1, Java 21, GitHub Actions

---

## File Map

| File | Action |
|---|---|
| `gradle/` | Copy from monorepo as-is |
| `gradlew` / `gradlew.bat` | Copy from monorepo as-is |
| `settings.gradle` | Create new |
| `gradle.properties` | Create adapted (single `mod_version`, add `s3_version`) |
| `cliff.toml` | Copy from monorepo as-is |
| `rewrite.yml` | Copy from monorepo as-is |
| `buildSrc/build.gradle` | Copy from monorepo as-is |
| `buildSrc/src/main/groovy/s3.repositories.gradle` | Copy from monorepo as-is |
| `buildSrc/src/main/groovy/s3.conventions.gradle` | Copy from monorepo as-is |
| `buildSrc/src/main/groovy/s3.neoforge-mod.gradle` | Copy then adapt: remove `:core`/JEI deps, add modrinth `compileOnly`+`localRuntime` |
| `neoforge/s3-advanced/` (all source) | Copy from monorepo as-is |
| `neoforge/s3-advanced/build.gradle` | Adapt: version key, remove `:core` modSourceSets, remove `:neoforge:s3` dep |
| `neoforge/s3-advanced/src/main/resources/META-INF/neoforge.mods.toml` | Adapt: update s3 `versionRange` to `[0.11.0,)` |
| `gametest/s3-advanced/` (all source) | Copy from monorepo as-is |
| `gametest/s3-advanced/build.gradle` | Adapt: replace project refs, add `localRuntime`, remove `s3` mods block |
| `.github/workflows/ci.yml` | Copy from monorepo as-is |
| `.github/workflows/release.yml` | Adapt: JAR paths, names, dependencies |
| `.github/workflows/bump-version.yml` | Copy from monorepo as-is |
| `.github/workflows/changelog.yml` | Copy from monorepo as-is |
| `.github/workflows/openrewrite.yml` | Copy from monorepo as-is |

---

## Task 1: Scaffold Root Build Files

**Files:**
- Create: `settings.gradle`
- Create: `gradle.properties`
- Copy: `gradle/`, `gradlew`, `gradlew.bat`, `cliff.toml`, `rewrite.yml`

- [ ] **Step 1.1: Copy the Gradle wrapper and config files from the monorepo**

  ```bash
  MONO=/home/steve/code/scuba10steve/steves-storage-system
  REPO=/home/steve/code/scuba10steve/steves-advanced-storage

  cp -r $MONO/gradle $REPO/
  cp $MONO/gradlew $REPO/
  cp $MONO/gradlew.bat $REPO/
  cp $MONO/cliff.toml $REPO/
  cp $MONO/rewrite.yml $REPO/
  chmod +x $REPO/gradlew
  ```

- [ ] **Step 1.2: Create `settings.gradle`**

  Create `/home/steve/code/scuba10steve/steves-advanced-storage/settings.gradle` with this exact content:

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

- [ ] **Step 1.3: Create `gradle.properties`**

  Create `/home/steve/code/scuba10steve/steves-advanced-storage/gradle.properties` with this exact content:

  ```properties
  org.gradle.jvmargs=-Xmx3G
  org.gradle.daemon=false
  org.gradle.parallel=true
  org.gradle.caching=true
  org.gradle.configuration-cache=true

  minecraft_version=1.21.1
  minecraft_version_range=[1.21.1,1.22)
  neoforge_version=21.1.218
  neoforge_version_range=[21.1,22)
  loader_version_range=[4,)

  mod_id=s3_advanced
  mod_name=Steve's Advanced Storage
  mod_license=MIT
  mod_version=0.1.0
  mod_authors=scuba10steve
  mod_description=Advanced features for Steve's Simple Storage.

  s3_version=0.11.0
  ```

- [ ] **Step 1.4: Verify Gradle wrapper resolves**

  ```bash
  cd /home/steve/code/scuba10steve/steves-advanced-storage
  ./gradlew --version
  ```

  Expected: Gradle version printed with no errors. (It will warn about missing subprojects — that's fine at this stage.)

- [ ] **Step 1.5: Commit**

  ```bash
  cd /home/steve/code/scuba10steve/steves-advanced-storage
  git add gradle/ gradlew gradlew.bat settings.gradle gradle.properties cliff.toml rewrite.yml
  git commit -m "feat: scaffold root build files"
  ```

---

## Task 2: Set Up buildSrc Convention Plugins

**Files:**
- Copy: `buildSrc/build.gradle`, `buildSrc/src/main/groovy/s3.repositories.gradle`, `buildSrc/src/main/groovy/s3.conventions.gradle`
- Create adapted: `buildSrc/src/main/groovy/s3.neoforge-mod.gradle`

- [ ] **Step 2.1: Copy buildSrc as-is**

  ```bash
  MONO=/home/steve/code/scuba10steve/steves-storage-system
  REPO=/home/steve/code/scuba10steve/steves-advanced-storage

  mkdir -p $REPO/buildSrc/src/main/groovy
  cp $MONO/buildSrc/build.gradle $REPO/buildSrc/
  cp $MONO/buildSrc/src/main/groovy/s3.repositories.gradle $REPO/buildSrc/src/main/groovy/
  cp $MONO/buildSrc/src/main/groovy/s3.conventions.gradle $REPO/buildSrc/src/main/groovy/
  cp $MONO/buildSrc/src/main/groovy/s3.neoforge-mod.gradle $REPO/buildSrc/src/main/groovy/
  ```

- [ ] **Step 2.2: Adapt `buildSrc/src/main/groovy/s3.neoforge-mod.gradle`**

  The current content has `:core` and JEI deps. Replace the entire `dependencies {}` block.

  Open `buildSrc/src/main/groovy/s3.neoforge-mod.gradle`. The full adapted file should be:

  ```groovy
  plugins {
      id 'java'
      id 'net.neoforged.moddev'
      id 's3.conventions'
  }

  version = rootProject.mod_version
  group = 'io.github.scuba10steve.s3'

  java.toolchain.languageVersion = JavaLanguageVersion.of(21)

  neoForge {
      version = neoforge_version

      runs {
          client {
              client()
          }
          server {
              server()
          }
      }
  }

  dependencies {
      compileOnly "maven.modrinth:steves-simple-storage:${s3_version}"
      localRuntime "maven.modrinth:steves-simple-storage:${s3_version}"

      testImplementation 'org.junit.jupiter:junit-jupiter:5.14.3'
      testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
  }

  test {
      useJUnitPlatform()
  }
  ```

- [ ] **Step 2.3: Commit**

  ```bash
  cd /home/steve/code/scuba10steve/steves-advanced-storage
  git add buildSrc/
  git commit -m "feat: add adapted buildSrc convention plugins"
  ```

---

## Task 3: Copy and Adapt `neoforge/s3-advanced`

**Files:**
- Copy: entire `neoforge/s3-advanced/` directory from monorepo
- Modify: `neoforge/s3-advanced/build.gradle`
- Modify: `neoforge/s3-advanced/src/main/resources/META-INF/neoforge.mods.toml`

- [ ] **Step 3.1: Copy `neoforge/s3-advanced` from the monorepo**

  ```bash
  MONO=/home/steve/code/scuba10steve/steves-storage-system
  REPO=/home/steve/code/scuba10steve/steves-advanced-storage

  mkdir -p $REPO/neoforge
  cp -r $MONO/neoforge/s3-advanced $REPO/neoforge/s3-advanced
  ```

- [ ] **Step 3.2: Adapt `neoforge/s3-advanced/build.gradle`**

  The full adapted file should be:

  ```groovy
  plugins {
      id 's3.neoforge-mod'
  }

  version = mod_version

  base {
      archivesName = 's3-advanced'
  }

  neoForge {
      mods {
          's3-advanced' {
              sourceSet sourceSets.main
          }
      }
  }

  processResources {
      def replaceProperties = [mod_version: mod_version]
      inputs.properties replaceProperties
      filesMatching('META-INF/neoforge.mods.toml') {
          expand(replaceProperties)
      }
  }

  tasks.register('validateLang') {
      group = 'verification'
      description = 'Validates all lang files have the same key set as en_us.json'

      def langDir = file('src/main/resources/assets/s3_advanced/lang')
      if (langDir.exists()) { inputs.dir(langDir) }
      outputs.upToDateWhen { true }

      doLast {
          if (!langDir.exists()) {
              throw new GradleException("validateLang: lang directory not found: ${langDir}")
          }
          def referenceFile = new File(langDir, 'en_us.json')
          if (!referenceFile.exists()) {
              throw new GradleException("validateLang: reference file not found: ${referenceFile}")
          }
          def reference = new groovy.json.JsonSlurper().parse(referenceFile)
          def referenceKeys = reference.keySet()

          langDir.listFiles()
              ?.findAll { it.name.endsWith('.json') && it.name != 'en_us.json' }
              ?.each { langFile ->
                  def keys
                  try {
                      keys = new groovy.json.JsonSlurper().parse(langFile).keySet()
                  } catch (Exception e) {
                      throw new GradleException("validateLang: failed to parse ${langFile.name}: ${e.message}")
                  }
                  def missing = referenceKeys - keys
                  def extra = keys - referenceKeys
                  if (missing || extra) {
                      def msg = "${langFile.name} key mismatch:"
                      if (missing) msg += "\n  Missing: ${missing}"
                      if (extra) msg += "\n  Extra:   ${extra}"
                      throw new GradleException(msg)
                  }
              }
      }
  }

  tasks.named('check') { dependsOn('validateLang') }
  ```

  Key changes from the monorepo version:
  - `version = advanced_mod_version` → `version = mod_version`
  - Removed `modSourceSets.add(providers.provider { project(':core').sourceSets.main })` from the `neoForge.mods` block
  - Removed `dependencies { implementation project(':neoforge:s3') }` block entirely

- [ ] **Step 3.3: Update `neoforge/s3-advanced/src/main/resources/META-INF/neoforge.mods.toml`**

  Find this block:

  ```toml
  [[dependencies.s3_advanced]]
  modId="s3"
  type="required"
  versionRange="[0.10.0,)"
  ordering="BEFORE"
  side="BOTH"
  ```

  Change `versionRange="[0.10.0,)"` → `versionRange="[0.11.0,)"`.

- [ ] **Step 3.4: Verify the module builds**

  ```bash
  cd /home/steve/code/scuba10steve/steves-advanced-storage
  ./gradlew :neoforge:s3-advanced:build
  ```

  Expected: `BUILD SUCCESSFUL`. The JAR should appear at `neoforge/s3-advanced/build/libs/s3-advanced-0.1.0.jar`.

- [ ] **Step 3.5: Commit**

  ```bash
  cd /home/steve/code/scuba10steve/steves-advanced-storage
  git add neoforge/
  git commit -m "feat: add neoforge/s3-advanced module"
  ```

---

## Task 4: Copy and Adapt `gametest/s3-advanced`

**Files:**
- Copy: entire `gametest/s3-advanced/` directory from monorepo
- Modify: `gametest/s3-advanced/build.gradle`

- [ ] **Step 4.1: Copy `gametest/s3-advanced` from the monorepo**

  ```bash
  MONO=/home/steve/code/scuba10steve/steves-storage-system
  REPO=/home/steve/code/scuba10steve/steves-advanced-storage

  mkdir -p $REPO/gametest
  cp -r $MONO/gametest/s3-advanced $REPO/gametest/s3-advanced
  ```

- [ ] **Step 4.2: Adapt `gametest/s3-advanced/build.gradle`**

  The full adapted file should be:

  ```groovy
  plugins {
      id 'net.neoforged.moddev'
      id 's3.conventions'
  }

  version = rootProject.mod_version
  group = 'io.github.scuba10steve.s3.test'


  dependencies {
      implementation project(':neoforge:s3-advanced')
      localRuntime "maven.modrinth:steves-simple-storage:${s3_version}"

      testRuntimeOnly "org.junit.platform:junit-platform-launcher:1.14.3"
  }

  neoForge {
      version = neoforge_version

      runs {
          gameTestServer {
              type = "gameTestServer"
              systemProperty 'neoforge.enabledGameTestNamespaces', 's3_advanced'
          }
          data {
              data()
              programArguments.addAll(
                  '--mod', 's3_advanced',
                  '--all',
                  '--output', file('src/generated/resources/').getAbsolutePath(),
                  '--existing', file('src/main/resources/').getAbsolutePath(),
                  '--existing', rootProject.file('neoforge/s3-advanced/src/main/resources/').getAbsolutePath()
              )
          }
      }

      mods {
          's3_advanced' {
              // Keeps its own local sourceSets.main; only the cross-project call goes lazy
              sourceSet sourceSets.main
              modSourceSets.add(providers.provider { project(':neoforge:s3-advanced').sourceSets.main })
          }
      }
  }

  sourceSets.main.resources.srcDir 'src/generated/resources'

  tasks.withType(Test).configureEach {
      useJUnitPlatform()
  }
  ```

  Key changes from the monorepo version:
  - Removed `implementation project(':core')` and `implementation project(':neoforge:s3')` from `dependencies`
  - Added `localRuntime "maven.modrinth:steves-simple-storage:${s3_version}"` to `dependencies`
  - Removed the entire `s3 { ... }` named mod block from `neoForge.mods` (it only had `:core` and `:neoforge:s3` modSourceSets)
  - Kept the `'s3_advanced' { ... }` mod block unchanged

- [ ] **Step 4.3: Verify the full build passes**

  ```bash
  cd /home/steve/code/scuba10steve/steves-advanced-storage
  ./gradlew build
  ```

  Expected: `BUILD SUCCESSFUL`. Both `neoforge/s3-advanced` and `gametest/s3-advanced` should compile cleanly.

- [ ] **Step 4.4: Commit**

  ```bash
  cd /home/steve/code/scuba10steve/steves-advanced-storage
  git add gametest/
  git commit -m "feat: add gametest/s3-advanced module"
  ```

---

## Task 5: Set Up GitHub Actions Workflows

**Files:**
- Copy: `ci.yml`, `bump-version.yml`, `changelog.yml`, `openrewrite.yml`
- Create adapted: `release.yml`

- [ ] **Step 5.1: Copy workflows that need no changes**

  ```bash
  MONO=/home/steve/code/scuba10steve/steves-storage-system
  REPO=/home/steve/code/scuba10steve/steves-advanced-storage

  mkdir -p $REPO/.github/workflows
  cp $MONO/.github/workflows/ci.yml $REPO/.github/workflows/
  cp $MONO/.github/workflows/bump-version.yml $REPO/.github/workflows/
  cp $MONO/.github/workflows/changelog.yml $REPO/.github/workflows/
  cp $MONO/.github/workflows/openrewrite.yml $REPO/.github/workflows/
  ```

- [ ] **Step 5.2: Create the adapted `release.yml`**

  Create `.github/workflows/release.yml` with this exact content:

  ```yaml
  name: Release

  on:
    workflow_dispatch:
      inputs:
        version:
          description: 'Release version (defaults to mod_version from gradle.properties)'
          required: false
          type: string
        release_type:
          description: 'Release type'
          required: true
          type: choice
          default: beta
          options:
            - beta
            - release

  permissions:
    contents: write

  jobs:
    release:
      runs-on: ubuntu-latest

      steps:
        - name: Checkout
          uses: actions/checkout@v4
          with:
            fetch-depth: 0

        - name: Determine version
          id: version
          run: |
            if [ -n "${{ inputs.version }}" ]; then
              echo "version=${{ inputs.version }}" >> "$GITHUB_OUTPUT"
            else
              version=$(grep '^mod_version=' gradle.properties | cut -d'=' -f2)
              echo "version=$version" >> "$GITHUB_OUTPUT"
            fi

        - name: Setup Java 21
          uses: actions/setup-java@v4
          with:
            distribution: temurin
            java-version: 21

        - name: Setup Gradle
          uses: gradle/actions/setup-gradle@v4

        - name: Grant execute permission for gradlew
          run: chmod +x gradlew

        - name: Build and test
          run: ./gradlew build

        - name: Generate changelog
          uses: orhun/git-cliff-action@v4
          id: changelog
          with:
            config: cliff.toml
            args: --unreleased --tag v${{ steps.version.outputs.version }} --strip header
          env:
            GITHUB_REPO: ${{ github.repository }}

        - name: Create GitHub Release
          uses: softprops/action-gh-release@v2
          with:
            tag_name: v${{ steps.version.outputs.version }}
            target_commitish: ${{ github.sha }}
            name: v${{ steps.version.outputs.version }}
            body: ${{ steps.changelog.outputs.content }}
            files: neoforge/s3-advanced/build/libs/s3-advanced-${{ steps.version.outputs.version }}.jar
            prerelease: ${{ inputs.release_type == 'beta' }}

        - name: Publish to Modrinth
          uses: Kir-Antipov/mc-publish@v3.3
          with:
            modrinth-id: ${{ vars.MODRINTH_PROJECT_ID }}
            modrinth-token: ${{ secrets.MODRINTH_TOKEN }}
            files: neoforge/s3-advanced/build/libs/s3-advanced-${{ steps.version.outputs.version }}.jar
            name: Steve's Advanced Storage v${{ steps.version.outputs.version }}
            version: ${{ steps.version.outputs.version }}
            version-type: ${{ inputs.release_type }}
            loaders: neoforge
            game-versions: 1.21.1
            java: 21
            changelog: ${{ steps.changelog.outputs.content }}
            dependencies: |
              steves-simple-storage@0.11.0(required){modrinth:steves-simple-storage}

        - name: Publish to CurseForge
          uses: Kir-Antipov/mc-publish@v3.3
          with:
            curseforge-id: ${{ vars.CURSEFORGE_PROJECT_ID }}
            curseforge-token: ${{ secrets.CURSEFORGE_TOKEN }}
            files: neoforge/s3-advanced/build/libs/s3-advanced-${{ steps.version.outputs.version }}.jar
            name: Steve's Advanced Storage v${{ steps.version.outputs.version }}
            version: ${{ steps.version.outputs.version }}
            version-type: ${{ inputs.release_type }}
            loaders: neoforge
            game-versions: 1.21.1
            java: 21
            changelog: ${{ steps.changelog.outputs.content }}
            dependencies: |
              # TODO: add steves-simple-storage required dep once CurseForge project ID is known

        # TODO: Enable once Nexus Mods Upload API access is granted
        # - name: Publish to Nexus Mods
        #   uses: Nexus-Mods/upload-action@main
        #   with:
        #     api_key: ${{ secrets.NEXUSMODS_API_KEY }}
        #     file_id: ${{ vars.NEXUSMODS_FILE_ID }}
        #     game_domain_name: minecraft
        #     filename: neoforge/s3-advanced/build/libs/s3-advanced-${{ steps.version.outputs.version }}.jar
        #     version: ${{ steps.version.outputs.version }}
        #     display_name: Steve's Advanced Storage v${{ steps.version.outputs.version }}
  ```

- [ ] **Step 5.3: Commit**

  ```bash
  cd /home/steve/code/scuba10steve/steves-advanced-storage
  git add .github/
  git commit -m "feat: add GitHub Actions workflows"
  ```

---

## Verification Checklist

After all tasks are complete, run a final clean build to confirm everything is wired up correctly:

```bash
cd /home/steve/code/scuba10steve/steves-advanced-storage
./gradlew clean build
```

Expected: `BUILD SUCCESSFUL` with no warnings about missing projects or unresolved dependencies.

Confirm the output JAR exists:

```bash
ls neoforge/s3-advanced/build/libs/
```

Expected: `s3-advanced-0.1.0.jar`
