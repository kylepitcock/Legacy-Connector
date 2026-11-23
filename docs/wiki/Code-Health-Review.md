# Code health review (2026-11-22)

A review of the whole code base after release 0.5.1: what is sound, what should be refactored, and the few places where the design itself
should be replaced. It was made from size and pattern counts over every source file, five read-only reviews of the main areas, and the
defects found on the release day. Effort figures are for one engineer and are rough.

**Verdict: no wholesale rewrite.** The architecture (translate the jar at load time, link it against a hand-written shim, bridge to the modern
game) holds up. Four designs should be replaced because they keep producing serious bugs or grow with every version; the rest is refactoring
in place, most of it mechanical.

## Size

| Area | Files | Lines |
|---|---|---|
| Runtime (`src/main/java`) | 5,580 | 503,000 |
| of which the 1.12.2 shim | 2,129 | 182,000 |
| of which the 1.7.10 shim | 1,581 | 137,000 |
| of which the bridges of 1.7.10 / 1.12.2 / Beta 1.7.3 / 1.20.1 | 203 / 175 / 234 / 157 | 41,000 / 32,000 / 26,000 / 17,000 |
| of which the shared `core` | 81 | 10,000 (2%) |
| Loader (`loader/src/main/java`) | 241 | 69,000 |
| Tests | 436 | 53,000 |

Patterns counted over the main sources: 1,788 catch-all blocks, 1,964 static mutable collections, 1,384 `synchronized`, 211 thread-locals,
about 148 distinct `legacyconnector.*` system properties.

## Replace the design (4)

### 1. How shim code reaches the modern level
The 1.7.10 `World` shim has five accessors for "the level". `mcc$fullLevel()` and `mcc$levelObject()` have the same body and differ in a
comment and in whether a guard logs; that guard (`RealLevelGuard`) is only on in development runs. `mcc$level()` is the safe generation region
in 1.7.10 and the real level in 1.12.2. Legacy code that runs while a chunk generates and reaches the real level deadlocks the server; this
shipped in 0.5.1 (`Block.getDrops`). There are 60 uses of `mcc$fullLevel()` in the 1.7.10 shim.

Replace with two types that the compiler enforces: a level handle that can give registries, the dimension key and entity construction but
has no block, chunk, height or light access, and a live level that is absent during generation. Give both versions the same names. Then
audit the 60 call sites. **3 to 5 days.**

### 2. One host implementation per version
Hosting a legacy entity, block entity, menu, fluid or capability on the modern side is the same idea in every version and is written once per
version: the entity and mob hosts of 1.7.10 and 1.12.2 share 55 to 65% of their lines, dimensions and menus 45 to 75%, fluid and recipe
bridges about 25%. Beta 1.7.3 has a third copy. Fixes drift: the 1.7.10 mob host has 22 commits and its 1.12.2 twin 8; the spawner-tag fix and
the modern-fluid mirror fix exist only on the 1.7.10 side. `core` is 2% of the runtime, and its only cross-version interface is the lifecycle
(`LegacyPlatform`).

Extract a shared kernel in `core/host` with a small interface per concern that each shim implements (entity operations, stack view, block
view), in this order: entity and mob hosts (2 weeks), fluid and capability mirrors (1 week), dimensions, menus and slots (3 days). Block and
item adapters stay per version (metadata and block states really differ); only their modern-side helpers move. About 9,400 lines of the shims
are near-verbatim copies (Forge config classes, the LWJGL vector and GLU classes): build them from one source (2 days).

### 3. The loader's pass orchestration
`LegacyJarTransformer.transform()` runs about twenty passes in a hand-written order inside one method; each pass decides for itself whether a
version concerns it (89 version tests in 26 loader files); 1.20.1 forks into a second pipeline at the top. Seven Ars Magica passes and one CoFH
pass sit in the generic loop, outside `compat/`, and the mod-name check cannot see them (the name is not in its list). Rename and redirect
tables are literals in 18 files.

Replace with an explicit pass list per version family (`Pass.appliesTo(version)`, `apply(context)`), a compat registry for mod-specific
passes, and tables as data files. First add a golden test that records the pass order per version. **8 to 12 days**; the 1.20.1 monoliths
(`Forge1201Transformer`, `Mc1201Migration`) after that, 6 to 10 days.

### 4. Legacy item stack identity (1.7.10)
`ItemStack.mcc$wrap` hands out wrappers from a 64-slot table per thread, so the wrapper of a modern stack depends on hash collisions and on
the thread. Mods compare stacks by identity (the Flux Capacitor energy loss was this class on 1.12.2, which uses yet another model). Store the
view on the modern stack, as `World` and `Entity` already do. **2 to 3 days.**

## Do first: cheap and protective

| What | Why | Effort |
|---|---|---|
| Run the gate (compile, shim index, `check` with the unit tests) on every pull request and push | No workflow runs a test today; the mod-specific check stayed unwired for weeks | 1 day |
| A required suite set per release, baselines for the 25 suites without one, a full run before a stable release | A regression that froze world generation shipped because a subset was run | 2 days |
| Remove `TRANSFORMER_VERSION` from the cache key | The key already has a content hash of the loader and digests of the runtime and shim index; the hand-bumped number collided between branches. Check first that development runs have those digests | 1 to 2 days |
| One reporting channel for "degraded" behaviour | Asset caps, block render types without a handler, GL calls that are no-ops or read real state, mixins that did not apply and swallowed exceptions each fail silently in their own way | 3 to 5 days |
| A registry of the system-property switches (name, type, default, description), listed at start and in the wiki | About 148 switches parsed at their call sites, nearly all undocumented | 2 to 3 days |

## Refactor in place

- **World generation state in thread-locals** (current region, deep pass, shift, ore split): make it one explicit context per region, entered with try-with-resources. 1 week.
- **Block shape queries lock the legacy block** (`synchronized (legacy)` in `LegacyBlockAdapter`, shared by render, server and generation threads): cache shapes per state, lock only on a miss. 2 to 4 days.
- **Rendering**: a registry of vanilla block render types with a start-up list of unhandled ones (today types 7, 18 and 41 are a hand-written chain and the rest draw as cubes); a table for GL state queries (today only listed values come from the emulation, the rest read the real context); one facade for Sodium and Iris (special cases in 21 files). 1 to 2 weeks.
- **`LegacyModLocator` and `LegacyJarInspector`** do discovery, caching, synth jar building, ordering and reporting: extract the cache, the synth jar provider, the mod-set planner and the report writer. 5 to 8 days.
- **Exception policy**: one helper for calls into legacy code (site, fallback, throttled log) in place of ad hoc catch blocks (36 in the block adapter alone). 2 days for the core classes.
- **`World`, `Entity`, `Block`** (1,500 to 2,700 lines each): legacy code needs the methods on these classes, but they can delegate to components (light, heights, block access, entities, sound). Do it as each concern is touched; `checkShimMembers` proves no signature changed.

## Tidy

- Move the 1.20.1 analyzer tools (34 files, 17,000 lines) and the Beta self tests (about 5,600 lines) out of the shipped jars; the tools also feed the loader's content hash, so editing one invalidates every cache. 2 to 5 days.
- Rename the 19 `RoundNCompat` classes of 1.20.1 by API area, and sort the 163 mixins into `hook/`, `feature/`, `access/`, `compat/`, `perf/`. 4 to 5 days; the loader names some of these classes as strings.
- Make `compileJava` incremental again by widening access into a separate output directory instead of rewriting class files in place. 1 day.

## Keep as it is

- The overall split: loader translation, shim, bridges; the forward chain for older versions (1.6.4 down to 1.2.5 ride the 1.7.10 shim); the in-place remap for 1.20.1.
- Virtual fields (`mcc$get$`/`mcc$set$`) with the build-time check, and the `checkShimMembers` gate.
- View caches stored on the modern object (`LegacySlot`) for worlds and entities.
- Per-thread render state, old block renderers captured as baked quads, vanilla render types (so shader packs work), display lists.
- The content-hash plumbing of the translation cache, `SideBySidePlanner`, `SplitPackageResolver`, `TranslationPool`.
- Per version on purpose: event bridges, metadata and block state mapping, coremod sites, the asset pipelines.
- `gate.sh`, the suite runner and its picker, `MixinSelfCheck`, the user-facing `LegacyConfig`.

## Order

1. The "do first" table (about two weeks).
2. The level-access types (design 1) and stack identity (design 4): they stop the classes of bug that shipped.
3. The shared host kernel (design 2) before another shim-based version is added; each new version is otherwise another copy.
4. The loader pass lists (design 3) before 1.19.2, 1.18.2 and 1.16.5 are threaded through the 1.20.1 pipeline.
