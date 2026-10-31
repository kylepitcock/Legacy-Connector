# Legacy Connector: notes for Claude (and contributors)

Runs unmodified legacy Forge mods on NeoForge 1.21.1 by translating them at load time (like Sinytra Connector does for
Fabric). Read `README.md` for the overview and `docs/wiki/` (start at `Home.md`) for the detailed docs: architecture, bridges, world
generation, dimensions, entities, rendering, recipes, performance, development, addon API. **Keep `README.md` short** (landing page: what it is,
status table, install, config table, links); put technical detail in the matching `docs/wiki` page (relative links, wiki-compatible file names,
so the folder can be pushed to the GitHub wiki as is). (Architecture summary: loader = FML service that translates jars; runtime = NeoForge mod
holding the hand-written shim under `src/main/java/legacyconnector/v1_7_10`, bridges under
`src/main/java/org/kylepitcock/legacyconnector/v1_7_10/bridge`, synthesized API generator under `loader/.../synth`).

## Roadmap and rules

- **Do not stop to report.** In a long or unattended run keep going (test, fix, commit, next problem) until the user says stop; post status in GitHub issue
  comments, never by ending the turn.

- Order: finish 1.7.10, then 1.12.2, then 1.20.1 (with Sinytra Connector compatibility), then 1.16.5, then 1.2.5. Don't start a later
  version before the earlier one is done. Keep the README status table and `.gitignore` current.
- Legacy containers are exposed as NeoForge capabilities BY DEFAULT (items, fluids, energy: RF, IC2 EU, BuildCraft MJ; tiles and items; both versions), not left to addons. New legacy container/energy interfaces get an adapter in `CapabilityRules`/`BuiltinAdapters`/`ForeignEnergy1710`, rates in `LegacyConfig`.
- Outdated dependencies are bridged to modern equivalents instead of loaded (Baubles -> Curios, Waila -> Jade,
  NEI -> JEI, CoFH RF -> NeoForge energy). NEI is never loaded when JEI is present; everything it registers is mapped into JEI.
- Only 1.7.10-engine-only mods are skipped (performance/bug-fix patches, ID extenders, mixin loaders, profilers: list in
  `loader/.../LibraryBridges.java`, `OBSOLETE`). Client mods (Inventory Tweaks, JourneyMap, Custom Main Menu, ...) must load.
- **Fix generically first.** Make old mods work by implementing or synthesizing the legacy MC/Forge API (shim, bridges,
  synth). A mod-specific fix is allowed only when no umbrella fix works and the mod is popular; keep it minimal, isolated
  (own class, labelled, keyed by mod id) and easy to remove.
- The CurseForge/Modrinth description pages come from `.github/description/description.md` (`sync-description.yml` pushes it to Modrinth on
  master; CurseForge has no description API, so its rendering is a workflow artifact to paste). Whenever you update `README.md`, update that file too.
- Every report line the layer prints for a mod ends up on the compatibility site (`docs/`), so keep
  `docs/data/mods.json` complete: `tools/compat/from_corpus.py <report> <latest.log>` adds corpus results;
  `tools/compat/from_client.py <report> <latest.log> "<pack label>"` rates mods from a real client session or a server
  self-test run (works / partial / loads / fails, source "client", ranked between maintainer entries and the corpus).

## Build and test

- Use JDK 21 (`export JAVA_HOME=$(/usr/libexec/java_home -v 21)` on macOS); the default JDK 26 fails with
  "Unsupported class file major version 70".
- `./gradlew compileJava --rerun` (compileJava is non-incremental: the shim access widener rewrites class files), then
  `./gradlew generateShimIndex`. The latter catches "phantom field access" errors: shim code must read legacy fields
  that exist only as virtual fields through the `mcc$get$`/`mcc$set$` accessors. After moving source files, compare the
  count of `src/main/java/**/*.java` with the classes under `build/classes` (Gradle can silently drop moved classes).
- `./gradlew build` for everything, `./gradlew checkLegacyMod -PlegacyJar=a.jar,b.jar` for an offline link check of
  jars (generates and verifies the synthesized API).
- Server tests: `tools/dev/run-subset.sh <jar-name-fragments>` (a few mods, 1-2 min, prints the block/item/entity
  self tests), `tools/dev/run-corpus.sh [dir]` (a whole directory of jars). Test mods are user-supplied and
  gitignored (`test-mods/`, `test-mods2/`); never commit or redistribute mod jars. Run only one server at a time and never
  compile while one runs; kill stale ones with `pkill -9 -f "serverSubTestRunProgramArg[s]"` (the `[s]` stops the pattern matching itself).
- Translation results are cached per jar and mod set (`run-*/.legacyconnector/cache`). After changing the loader,
  linker, stubber or shim, delete the cache or the old translation is reused.
- Client/graphical tests must not open a window on the developer's desktop: use `tools/dev/docker-headless.sh`
  (Colima + Docker, Xvfb + Mesa). `tools/dev/session.sh start <jar fragments>` boots one long-lived headless client you
  drive with `session.sh send cmd "<slash command>"|info|wait n|mouse x y|click x y`, `session.sh shot <name>`
  and `tools/dev/survey.sh <namespace> <name>` (grid of a mod's blocks); screenshots land in
  `~/.cache/legacyconnector-docker/work/run-screenshots/control/out/`. Code changes need `session.sh stop` and a restart.
  Clear the docker volume `legacyconnector-cache` when pack-generation code changes.
- On a Windows desktop (no Docker) `tools/dev/quick-shots.sh` runs the graphical test directly in about a minute: superflat world, no terrain tour, render
  distance 4, 640x360, 12 blocks/items per mod (`EXTRA="-PshotMods=chisel -PdebugIcons=Stairs"` narrows it and logs icon lookups of block classes
  containing that text). Mods go in `run-screenshots/mods`; move `run-screenshots/.legacyconnector/cache` aside after code changes. The shot list comes from the
  mods' creative tabs, so subtypes a mod never lists (e.g. Chisel stairs `.3`-`.7`) show as missing textures in the log without being a defect.
- Live probing (fastest loop, no restart): start a session with `SCENARIO=control tools/dev/quick-shots.sh` (add `WORLD=minecraft:normal`), then
  `tools/dev/js.sh '<JavaScript>'` or `tools/dev/js.sh -f probe.js` evaluates in the running game (bindings `mc`, `server`, `log`; any class via
  `Java.type(...)`). Command files in `run-screenshots/control/in/*.cmd` also take player verbs: `walk forward|back|left|right [ticks] [sprint]`, `jump`,
  `sneak`, `attack`, `slot n`, `inv`, `creative`, `use`, `holduse ticks` (holds right-click: Thaumometer scans, bows), `click x y`, `shot name` (PNG in `control/out/`, view it with Read), `cmd /...`. The screenshot test
  and server self test log a per-level entity/chunk census and heap/direct memory every 30 s (`-Dlegacyconnector.memoryLog=true` elsewhere). For leaks:
  `jmap -histo:live <pid>` twice and diff, or `jcmd <pid> GC.heap_dump` + Eclipse MAT headless (`MemoryAnalyzerc.exe -consolelog -application
  org.eclipse.mat.api.parse dump.hprof org.eclipse.mat.api:suspects`).
- Gotchas learned the hard way: vanilla block tags are not bound while mods load (never use `BlockTags` when
  constructing shims); legacy code that runs during chunk generation must read blocks through the generation region, never
  the real `ServerLevel`; name lookups must stay O(1) (mods look up thousands of absent names); per-tick and per-chunk
  error logging must be throttled; mod dimensions' generators must never throw.

## 1.12.2 (started, server side only; issue #64)

- Shim: `src/main/java/legacyconnector/v1_12_2`; bridges and the platform: `src/main/java/org/kylepitcock/legacyconnector/v1_12_2` (`Platform1122`
  runs FML's lifecycle and posts `RegistryEvent.Register<T>` after preInit; `bridge/*1122` turn legacy registrations into modern ones).
- Design: a 1.12 block keeps its legacy state as ONE modern property `meta` (0-15, `Legacy1122Block.META`) mapped through the block's own
  `getStateFromMeta`/`getMetaFromState`; `Assets1122` rewrites each mod blockstate file to `meta=N` variants. Items with subtypes become one modern item per
  subtype (`mod:name`, `mod:name_1`, ...). Recipes registered in code become generated data pack recipes (`RecipeBridge1122`, JSON recipes of the jar
  are converted by the loader). Tile entities are hosted by one modern block entity type. `Item.ToolMaterial`, `ArmorMaterial`, `EnumRarity`... are classes
  with `values()` so `EnumHelper` can add constants.
- Static constants of vanilla classes the shim does not have (typed with synthesized classes) are produced on first read by `Vanilla1122.resolveStatic`
  (hooked into `Synth.getField`): `Blocks`/`Items` wrappers, enum constants, self-typed constants, state properties. `tools/gen_vanilla_1122.py` and
  `tools/gen_constants_1122.py` generate `init/Blocks|Items|SoundEvents|MobEffects|Enchantments` from the legacy field list.
- Test: put 1.12.2 jars in `run-subtest/mods` (keep the 1.7.10 ones in `mods-1710`), delete or move `run-subtest/.legacyconnector/cache` and `run-subtest/world`,
  `./gradlew runServerSubTest`; the log prints "1.12.2 content registered: ..." and "1.12.2 items/blocks per mod: ..."; failing mods are listed under
  "failed to load". A client smoke test is `./gradlew runClientTest` (mods in `run-clienttest/mods`).
- The bash tool of this project's sessions rejects some large heredocs (unmatched quote); write files with the Write tool or a Python script file.

## Addon API

- Public API for other mods: `src/main/java/org/kylepitcock/legacyconnector/api` (+ `api.event`); implementation and the only code touching shim types: `api/internal` (`Convert`, `ContentKeys`, `BuiltinAdapters`). Docs `docs/wiki/Addon-API.md`, example `examples/thaumcraft-modern-aspects` (standalone build, `./gradlew apiJar` first).
- Rule: API signatures never contain shim types (legacy objects are `Object`, reached through `LegacyAccess`); keep the API stable (PATCH-compatible), hook calls live in `LegacyLifecycle` (`AddonHost.enter`) and `LegacyConnector.registerCapabilities` (`CapabilityRules.apply`). Update the doc when adding API.

## Releases

Pre-1.0 versioning: bump the PATCH for every release (0.0.1, 0.0.2, ...); bump the MINOR only for a milestone (a new legacy
version supported); 1.0.0 is reserved for when the project is ready. Steps: set `mod_version` in `gradle.properties`
BEFORE building and committing, `./gradlew bundleJar`, copy `build/dist/legacyconnector-<ver>*.jar` to
`legacyconnector-<ver>.jar`, `gh release create v<ver> <jar>` with release notes. License: MIT.
