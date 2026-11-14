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

- Order (2026-11-06; rationale in `docs/wiki/Version-Roadmap.md`): finish 1.7.10, then 1.12.2 (beta since 2026-11-07), then 1.6.4 (beta since 2026-11-08; was: started: a thin forward layer over the 1.7.10 shim; `docs/wiki/Legacy-1.6.4-Estimate.md`), then 1.5.2 and 1.4.7 (small deltas over 1.6.4: same numeric-id model; 1.4.7 and older use flat `net.minecraft.src` names), then 1.20.1 (in-place remap onto NeoForge 1.21.1, not a shim; `docs/wiki/Legacy-1.20.1-Estimate.md`), then 1.19.2 and 1.18.2 (renames and diffs over the 1.20.1 pipeline), then 1.16.5, then 1.2.5 (ModLoader/`BaseMod` emulation, a different layer). 1.8 to 1.11.2 are out of scope; Beta/Alpha/1.0-1.1 jar-mod era is not feasible. Separately, keep the layer able to follow newer NeoForge/Minecraft host versions (1.21.4+). Don't start a later
  version before the earlier one is done (the user may explicitly pull one forward, as for 1.6.4). Keep the README status table and `.gitignore` current.
  Exception: Beta 1.7.3 (ModLoader / Forge 1.0) was started on request on 2026-11-06, outside that order (`docs/wiki/Legacy-b1.7.3.md`; loader:
  `BetaJars`, `transform/BetaDeobfuscator`, `mapping/BetaMappings`; runtime: `org/.../vb1_7_3` (`PlatformB173`, `BridgeB173`, `VanillaB173`), shim `legacyconnector/vb1_7_3`; test jars in gitignored `test-mods-b173/`, server test with them in `run-subtest/mods/b1.7.3/`).
  Contributor note (Zekken01, 2026-11-07; the maintainer has not set a separate rule for Beta): Beta fixes must implement shared legacy APIs and execute mods' original callbacks, never hardcode behavior by mod identity. The only mod-specific reimplementation considered is a mod's edits to base game classes. Actual-mod regression fixtures may name mods.
  Beta archive workflow (2026-11-07): keep the existing foundation. Audit the contributor's local 1000+ mod archive with `tools/dev/audit_b173_archive.py`; prioritize major mods whose selected payload has no detected vanilla-class replacements. Test one mod/family target at a time with only reviewed required dependencies, fresh configs/world/cache (`tools/dev/test_b173_one.py`, `-PsubTestDir`); never use the combined pack to establish initial compatibility. API references and successful linking are not proof of behavior. Run `tools/dev/check_b173_generic.py`; see the audit snapshot in `docs/data/b173-archive-audit-summary.json`.
  Beta ecology scope (explicit contributor instruction, 2026-11-08): placement, scheduling, terrain-population filtering and despawning changes apply only to hosted legacy mod entities. Preserve vanilla and modern mod entities' native rules; do not install global Beta fauna lists, caps or despawn behavior. Original mods' explicit ModLoader vanilla AddSpawn/RemoveSpawn requests still translate through biome modifiers, on the native vanilla lifecycle lane.
- Legacy containers are exposed as NeoForge capabilities BY DEFAULT (items, fluids, energy: RF, IC2 EU, BuildCraft MJ; tiles and items; both versions), not left to addons. New legacy container/energy interfaces get an adapter in `CapabilityRules`/`BuiltinAdapters`/`ForeignEnergy1710`, rates in `LegacyConfig`.
- Outdated dependencies are bridged to modern equivalents instead of loaded (Baubles -> Curios, Waila -> Jade,
  NEI -> JEI, CoFH RF -> NeoForge energy). NEI is never loaded when JEI is present; everything it registers is mapped into JEI.
- Only 1.7.10-engine-only mods are skipped (performance/bug-fix patches, ID extenders, mixin loaders, profilers: list in
  `loader/.../LibraryBridges.java`, `OBSOLETE`). Client mods (Inventory Tweaks, JourneyMap, Custom Main Menu, ...) must load.
- **Native extension points.** Legacy content enters the game the way a 1.21.1 NeoForge mod would add it; the layer only
  translates the legacy API into that: village buildings -> extra pieces of the vanilla village structure (vanilla's own layout and pools untouched), structure chests -> loot
  tables (+ global loot modifiers), ores/features -> placed features + biome modifiers, biomes -> the biome-placement API the
  pack uses, spawns -> biome modifiers, recipes/tags -> data packs, containers -> capabilities. No post-generation decorators,
  result-overriding mixins or direct fills when a native hook exists: those ignore the pack's other mods.
- **Fix generically first.** Make old mods work by implementing or synthesizing the legacy MC/Forge API (shim, bridges,
  synth). A mod-specific fix is allowed only when no umbrella fix works and the mod is popular; keep it minimal, isolated
  (own class, labelled, keyed by mod id) and easy to remove.
  **Enforced:** `tools/dev/check_mod_specific.py` (part of `./gradlew check`) fails when a specific mod's name (`tools/dev/mod-names.txt`) shows up in the
  core outside a `compat/` package or the shim of another mod's API; existing mentions are frozen in `tools/dev/mod-specific-baseline.txt` and can only
  go down (`--update` lowers it). Put mod-specific code under a `compat/` package.
- The CurseForge/Modrinth description pages come from `.github/description/description.md` (`sync-description.yml` pushes it to Modrinth on
  master; CurseForge has no description API, so its rendering is a workflow artifact to paste). Whenever you update `README.md`, update that file too.
- Every report line the layer prints for a mod ends up on the compatibility site (`docs/`), so keep
  `docs/data/mods.json` complete: `tools/compat/probe.py <jar-dir> [--mc 1.12.2]` is the offline link probe (translates the jars as one mod set via `checkLegacyMod -PcheckJson`, no game; source "probe", the lowest tier, never overwrites better results; "loads" = all references link, "fails" = unresolved references; also appends the run to `docs/data/probe.json`, shown as a pie chart on `docs/probe.html`; `tools/compat/fetch_probe_mods.py <mc> <dir>` downloads the top Forge mods from Modrinth; `.github/workflows/probe.yml` runs both weekly and per release; the probe also runs `ResourceCheck`, an offline rendering smoke test (missing textures/models, layer vs mod's own), see the wiki Development page); `tools/compat/from_corpus.py <report> <latest.log>` adds corpus results;
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
- Regression suites: `tools/dev/verify.sh [--update] [suite ...]` runs named jar sets (`tools/dev/verify/suites/*.txt`) through the headless server self test and compares the metrics with `tools/dev/verify/baseline.json` (wiki Development, "Regression suites"); run it before merging shim/loader changes.
- Server tests: `tools/dev/run-subset.sh <jar-name-fragments>` (a few mods, 1-2 min, prints the block/item/entity
  self tests), `tools/dev/run-corpus.sh [dir]` (a whole directory of jars). Test mods are user-supplied and
  gitignored (`test-mods/`, `test-mods2/`); never commit or redistribute mod jars. Run only one server at a time and never
  compile while one runs; kill stale ones with `pkill -9 -f "serverSubTestRunProgramArg[s]"` (the `[s]` stops the pattern matching itself).
- Translation results are cached per jar and mod set (`run-*/.legacyconnector/cache`). After changing the loader,
  linker, stubber or shim, delete the cache or the old translation is reused.
- Scripted client scenarios (real window, unattended): `tools/dev/verify-client.sh [scenario ...]`, scenarios in `tools/dev/verify/scenarios/*.txt` (verbs, `expect <regex> :: <JavaScript>`, `shot`, `logabsent`); results and screenshots in `build/devtools/verify-client/`; see wiki Development, "Client scenarios".
- Client/graphical tests must not open a window on the developer's desktop: use `tools/dev/docker-headless.sh`
  (Colima + Docker, Xvfb + Mesa). `tools/dev/session.sh start <jar fragments>` boots one long-lived headless client you
  drive with `session.sh send cmd "<slash command>"|info|wait n|mouse x y|click x y`, `session.sh shot <name>`
  and `tools/dev/survey.sh <namespace> <name>` (grid of a mod's blocks); screenshots land in
  `~/.cache/legacyconnector-docker/work/run-screenshots/control/out/`. Code changes need `session.sh stop` and a restart.
  Clear the docker volume `legacyconnector-cache` when pack-generation code changes.
- Headless graphical tests must be silent. Use OpenAL Soft's null backend (`ALSOFT_DRIVERS=null`) and master volume zero
  in the disposable test profile. Keep the user's Prism audio settings intact.
- On a Windows desktop (no Docker) `tools/dev/quick-shots.sh` runs the graphical test directly in about a minute: superflat world, no terrain tour, render
  distance 4, 640x360, 12 blocks/items per mod (`EXTRA="-PshotMods=chisel -PdebugIcons=Stairs"` narrows it and logs icon lookups of block classes
  containing that text). Mods go in `run-screenshots/mods`; move `run-screenshots/.legacyconnector/cache` aside after code changes. The shot list comes from the
  mods' creative tabs, so subtypes a mod never lists (e.g. Chisel stairs `.3`-`.7`) show as missing textures in the log without being a defect.
- **Performance mods are standard in graphical tests.** `tools/dev/quick-shots.sh` copies the jars in `run-perfmods/` (Sodium, Sodium Extra, Iris, ImmediatelyFast, FerriteCore, ModernFix, EntityCulling, MoreCulling, BadOptimizations, C2ME, Clumps, Cloth Config, and the Iris add-ons Colorwheel + Colorwheel Patcher (Create/Flywheel under shaders), EuphoriaPatcher, Iris Veil Compat (Veil shader effects under packs) and Bad Horse Fix; Iris Flywheel Compat is left out (Colorwheel declares it incompatible); taken from a real pack, gitignored) into the game's mods folder; `NOPERF=1` tests without them. Defects such as the Sodium meshing thread race (ChromatiCraft see-through blocks) and C2ME's off-thread generation only show with them.
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

## 1.12.2 (beta since 2026-11-07; issue #64)

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
- Test: put 1.12.2 jars in `run-subtest/mods/1.12.2/` (not directly in `mods`: jars that bundle Mixin fail module resolution there; keep the 1.7.10 ones in `mods-1710`), delete or move `run-subtest/.legacyconnector/cache` and `run-subtest/world`,
  `./gradlew runServerSubTest`; the log prints "1.12.2 content registered: ..." and "1.12.2 items/blocks per mod: ..."; failing mods are listed under
  "failed to load". The server test also logs "1.12.2 blockstates: N translated, M fell back" (client translation run on the server; `-PassetsDump=<dir>` writes the
  generated assets, `-PclientProxies=<class,...>` runs those mods' client proxies first so their state mappers exist, e.g. `mekanism.client.ClientProxy`) and
  "1.12.2 item placement: N of M placed" (a fake player places every ItemBlock; `-Dlegacyconnector.placementDiag=1` adds the legacy checks per failure). A client smoke test is `./gradlew runClientTest` (mods in `run-clienttest/mods`).
- The bash tool of this project's sessions rejects some large heredocs (unmatched quote); write files with the Write tool or a Python script file.

## Addon API

- Public API for other mods: `src/main/java/org/kylepitcock/legacyconnector/api` (+ `api.event`); implementation and the only code touching shim types: `api/internal` (`Convert`, `ContentKeys`, `BuiltinAdapters`). Docs `docs/wiki/Addon-API.md`, example `examples/thaumcraft-modern-aspects` (standalone build, `./gradlew apiJar` first).
- Rule: API signatures never contain shim types (legacy objects are `Object`, reached through `LegacyAccess`); keep the API stable (PATCH-compatible), hook calls live in `LegacyLifecycle` (`AddonHost.enter`) and `LegacyConnector.registerCapabilities` (`CapabilityRules.apply`). Update the doc when adding API. Covers every supported version (API 0.3: Beta 1.7.3, `versionOf`, failure event, fluids, tabs); `LegacySupport` is the per-version matrix and `LegacySupportTest` keeps the doc table in sync (`API_DOCS_UPDATE=1` rewrites it), `ApiSignatureTest` rejects shim types in signatures.

## Releases

Two channels (since 2026-11-13):

- **Nightly** (automatic): `.github/workflows/nightly.yml` builds `bundleJar` after every push to master (docs-only pushes are skipped) and
  replaces the rolling GitHub pre-release `nightly` (version `<latest release>-nightly.<run>.<sha>`, never sent to Modrinth/CurseForge). So a
  fix reaches players by pushing to master; point reporters to the nightly when their problem is fixed there. Do not cut a release for a
  single fix.
- **Stable** (less frequent, bigger): only when the maintainer asks or a batch of verified work has accumulated. Pre-1.0 versioning: bump
  the PATCH per release; bump the MINOR only for a milestone (a new legacy version supported); 1.0.0 is reserved for when the project is
  ready. Cut it from GitHub: Actions > **Release** > Run workflow (`.github/workflows/release.yml`; or
  `gh workflow run release.yml -f version=0.5.0 -f notes="..."`; empty version = next patch). It builds `bundleJar` of master with
  `-Pmod_version=<ver>` (nothing is committed: master is protected against pushes without a pull request, so `mod_version` in
  `gradle.properties` is only the local default; bump it by hand after a release), creates tag `v<ver>` and the GitHub release (notes + commit list since the last release) and starts
  `publish.yml` (Modrinth, CurseForge; skips pre-releases) and `probe.yml`.
  By hand, if ever needed: set `mod_version` BEFORE building and committing, `./gradlew bundleJar`, copy
  `build/dist/legacyconnector-<ver>*.jar` to `legacyconnector-<ver>.jar`, `gh release create v<ver> <jar>`.

License: MIT.
