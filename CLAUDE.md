# Legacy Connector: notes for Claude (and contributors)

Runs unmodified legacy Forge mods on NeoForge 1.21.1 by translating them at load time (like Sinytra Connector does for
Fabric). Read `README.md` first for the architecture (loader = FML service that translates jars; runtime = NeoForge mod
holding the hand-written shim under `src/main/java/legacyconnector/v1_7_10`, bridges under
`src/main/java/org/kylepitcock/legacyconnector/v1_7_10/bridge`, synthesized API generator under `loader/.../synth`).

## Roadmap and rules

- Order: finish 1.7.10, then 1.12.2, then 1.20.1 (with Sinytra Connector compatibility), then 1.16.5. Don't start a later
  version before the earlier one is done. Keep the README status table and `.gitignore` current.
- Outdated dependencies are bridged to modern equivalents instead of loaded (Baubles -> Curios, Waila -> Jade,
  NEI -> JEI, CoFH RF -> NeoForge energy). NEI is never loaded when JEI is present; everything it registers is mapped into JEI.
- Only 1.7.10-engine-only mods are skipped (performance/bug-fix patches, ID extenders, mixin loaders, profilers: list in
  `loader/.../LibraryBridges.java`, `OBSOLETE`). Client mods (Inventory Tweaks, JourneyMap, Custom Main Menu, ...) must load.
- **Fix generically first.** Make old mods work by implementing or synthesizing the legacy MC/Forge API (shim, bridges,
  synth). A mod-specific fix is allowed only when no umbrella fix works and the mod is popular; keep it minimal, isolated
  (own class, labelled, keyed by mod id) and easy to remove.
- Every report line the layer prints for a mod ends up on the compatibility site (`docs/`), so keep
  `docs/data/mods.json` complete: `tools/compat/from_corpus.py <report> <latest.log>` adds corpus results.

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
- Gotchas learned the hard way: vanilla block tags are not bound while mods load (never use `BlockTags` when
  constructing shims); legacy code that runs during chunk generation must read blocks through the generation region, never
  the real `ServerLevel`; name lookups must stay O(1) (mods look up thousands of absent names); per-tick and per-chunk
  error logging must be throttled; mod dimensions' generators must never throw.

## Releases

Pre-1.0 versioning: bump the PATCH for every release (0.0.1, 0.0.2, ...); bump the MINOR only for a milestone (a new legacy
version supported); 1.0.0 is reserved for when the project is ready. Steps: set `mod_version` in `gradle.properties`
BEFORE building and committing, `./gradlew bundleJar`, copy `build/dist/legacyconnector-<ver>*.jar` to
`legacyconnector-<ver>.jar`, `gh release create v<ver> <jar>` with release notes. License: MIT.
