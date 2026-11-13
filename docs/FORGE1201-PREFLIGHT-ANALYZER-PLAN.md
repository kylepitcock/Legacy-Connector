# Forge 1.20.1 Preflight Compatibility Analyzer

**Status:** Slice 1 implemented and reviewed; Slice 2 implementation committed on the working branch and awaiting local test/build validation  
**Working branch:** `feat/forge1201-preflight-analyzer`  
**Baseline:** `dde40275b9b3bd48ff811afa4c863e475e8a0cdf`  
**Target:** Forge 1.20.1-only mod JARs on NeoForge 1.21.1 / 21.1.x  
**Boundary:** Palladium + Alien Evolution are handled in a separately authorized parallel workstream on `fix/forge1201-palladium-runtime-adapters`. Do not edit, merge, or cherry-pick that work from this analyzer branch.

## Purpose

Build one deterministic, read-only preflight workflow that answers, before launching Minecraft:

- What is this JAR, which mod IDs does it contain, and which Minecraft/Forge range does it declare?
- Which dependencies are required, optional, already present, supplied by the connector, or unresolved?
- Which embedded/Jar-in-Jar mods and libraries are inside it?
- Which Minecraft, Forge, NeoForge, Mixin, and third-party types/members does its bytecode reference?
- Which references have a reviewed shared migration, package relocation, explicit replacement, adapter, shim, or no known mapping?
- Which mixin configs, targets, injection methods, accessors, invokers, and `@At` targets need review against the 1.21.1 target?
- Which resources/data namespaces and expected files exist, and which issues are only visible after translation or runtime?
- What can be verified structurally, what is only a heuristic, and what still requires an actual client/server test?

The analyzer is a diagnostic gate, not a loader allowlist. A clean static report must never be phrased as proof that a mod is gameplay-compatible.

## Architecture decision: orchestrate existing scanners; do not duplicate them

The paused source checkpoint already contains important Phase 2 building blocks:

| Existing component | Current responsibility | How the preflight analyzer should use it |
|---|---|---|
| `Forge1201JarInspector` / `Forge1201JarInfo` | Reads Forge 1.20.1 `mods.toml`, mod declarations, and dependency blocks | Reuse for identity; strengthen TOML coverage and error reporting |
| `Forge1201ApiUsageAnalyzer` | ASM inventory of referenced API classes, member signatures, caller locations, entrypoints, and manifest mixin/coremod metadata | Reuse and extend for nested JARs, richer reference kinds, and resource-declared mixin configs |
| `Forge1201DependencyResolver` | Compares declared dependencies with native NeoForge mod IDs in top-level mod directories | Reuse the status vocabulary; extend to nested archives, multiple roots, and version/evidence reporting |
| `Forge1201MappingCatalog` | Classifies reviewed package moves, explicit type substitutions, manual adapters, and unmapped Forge types | Make this the shared classification source; do not copy its rules into the report layer |
| `Forge1201TargetApiVerifier` | Read-only target class/member lookup with inherited-member support and incomplete-hierarchy distinction | Reuse as structural evidence, never semantic proof |
| `Gap1201` / Gradle `gap1201` task | Broad translated bytecode gap check against Minecraft 1.21.1, NeoForge, libraries, peers, and shim classes | Retain as the target-side structural regression engine; expose machine-readable results instead of building a competing gap scanner |
| `Mixin1201Triage` | Separate offline mixin-target triage against an indexed target archive | Refactor behind a result API and integrate; do not invoke its CLI or capture console output |
| `ResourceCheck` | Post-translation resource/model/texture checks | Reuse for translated-resource comparison; preflight should also inventory source resource namespaces |
| `ShimIndex` / `ShimIndexGenerator` | Runtime shim class/member inventory | Surface shim coverage as evidence, distinguishing implemented/adapted methods from generated no-op stubs |

The existing root Gradle tasks are `gap1201`, `verify1201`, and `checkLegacyMod`. The new workflow should have one documented entry point while preserving these focused tools for debugging and regression work.

## Findings from the initial source audit

1. **There is no unified preflight report yet.** Metadata inspection, API inventory, dependency checks, target API verification, gap analysis, mixin triage, and resource checks are separate tools/reports.
2. **The Forge 1.20.1 metadata parser is intentionally partial.** `Forge1201JarInspector` documents that arrays, inline tables, and multiline values are ignored. A preflight result must flag unsupported/ambiguous metadata instead of silently treating missing parsed values as facts.
3. **Dependency discovery is too shallow for a pack-level graph.** `Forge1201DependencyResolver` scans top-level `.jar` files and compares IDs only. An ID match explicitly does not prove the installed version exposes the API expected by the Forge mod. Nested archives and version ranges need first-class evidence.
4. **The bytecode scanner is a useful base, not a complete archive scan.** `Forge1201ApiUsageAnalyzer` scans classes in the outer JAR and currently captures direct method/field call sites. It does not provide a unified scan of nested JAR classes, and its mixin config inventory is based on the manifest `MixinConfigs` attribute rather than every resource/config declaration.
5. **The target verifier is structural.** It distinguishes missing classes/members, incomplete hierarchy, and descriptors requiring manual adapters. An exact signature match cannot validate changed event timing, gameplay semantics, rendering, networking, or lifecycle behavior.
6. **The mixin triage tool is isolated and heuristic.** `Mixin1201Triage` is a CLI with static process-wide indexes, uses a heuristic to identify mixin JSON files, checks many target/member names, and notes plugin presence. It does not yet form a reusable result model with complete handler-descriptor, slice, dynamic-plugin, and injection-semantics coverage. A missing Forge target must be explicitly classified as unverified/unsupported, not treated as a harmless warning.
7. **`Gap1201` already indexes nested `META-INF/jarjar/*.jar` classes for target/peer hierarchy lookup, but its per-mod reference report scans only top-level classes.** Its first pass should be extended carefully; do not assume nested library behavior is fully covered. It currently emits text plus TSV rather than a stable preflight JSON document.

These observations describe the initial source audit at the baseline above. Slice 1 has since been implemented, passed user-confirmed loader tests and a consolidated build at the source checkpoint recorded below, and produced a real-JAR JSON/text report for Symbiote 1.1.3. The first task invocation exposed a Groovy classpath-expression parsing bug (`FilteredFileCollection.positive()`); the expression was corrected in source commit `9f608a53d655f3c938a4a909da066f905f66e41e`. The resulting report is uploaded to the testbench and its JSON was parsed and reviewed.

## Result model and reporting rules

The unified result should be deterministic (stable sorting, normalized paths, no timestamps in the canonical content) and should contain:

- Analyzer schema/version, source JAR SHA-256, file name/size, and target profile.
- Source identity and all declared mod IDs/versions.
- Metadata parse status and any unsupported/ambiguous constructs.
- Dependency edges with declared range, required/optional flag, side, ordering, observed provider/version, and evidence source.
- Embedded archive inventory with path, digest, nesting depth, and discovered mod/library metadata.
- Bytecode inventory counts and references with caller class/method/descriptor and opcode where available.
- Per-type migration classification from `Forge1201MappingCatalog`.
- Structural verification status from `Forge1201TargetApiVerifier` / `Gap1201`.
- Mixin config and target findings, including unresolved/unsupported analysis cases.
- Resource/data namespace inventory and any source-vs-translated losses known to `ResourceCheck`.
- Explicit limitations and the next required validation stage.

Recommended finding states: `SUPPORTED_BY_SHARED_RULE`, `STRUCTURALLY_VERIFIED`, `MANUAL_ADAPTER_REQUIRED`, `UNMAPPED_API`, `DEPENDENCY_PRESENT_UNVERIFIED`, `REQUIRED_DEPENDENCY_MISSING`, `OPTIONAL_DEPENDENCY_MISSING`, `MIXIN_TARGET_UNRESOLVED`, `RESOURCE_MISSING`, `ANALYSIS_INCOMPLETE`, and `RUNTIME_VALIDATION_REQUIRED`.

Each finding needs severity, confidence/evidence level, source location, explanation, and whether the result is static, structural, heuristic, or runtime-confirmed. Never collapse “unknown” into “compatible”. Never mark an optional dependency as required just because a related class reference exists. Do not add connector-imposed prerequisite mods: the report may show a mod's actual declared dependency, a native provider candidate, or a connector-owned bridge, but must not invent a dependency.

Provide both:
- Human-readable summary for mod developers and pack makers.
- Machine-readable JSON with a versioned schema so tests and future tooling can consume it without parsing console text.

## Implementation sequence

### Slice 1 — report contract and single-JAR inventory

- Add immutable result records and stable JSON/text writers.
- Reuse `Forge1201JarInspector`, `Forge1201ApiUsageAnalyzer`, `Forge1201MappingCatalog`, and `Forge1201DependencyResolver`.
- Inventory source resources, manifest attributes, mixin configuration declarations, Jar-in-Jar paths, and archive hashes.
- Make dependency status wording explicit: a native ID match is only a candidate, not API/version compatibility.
- Add a root Gradle `preflight1201` task with a straightforward single-JAR invocation and optional installed-mod directories/target classpath.
- Add fixture tests for standard metadata, multiple mod declarations, required vs optional deps, malformed/unsupported TOML, duplicate IDs, nested JARs, missing metadata, and deterministic JSON.

### Slice 2 — nested archives and dependency graph

- Recursively inspect Jar-in-Jar metadata and actual nested archive entries with bounded depth, size, and cycle protection.
- Record archive provenance and hashes; avoid extracting into persistent user directories.
- Discover nested mod IDs without confusing libraries, native NeoForge JARs, and Forge 1.20.1 JARs.
- Build dependency edges across parent and nested mods; retain side/order/range/optional semantics.
- Test missing/corrupt nested archives and path traversal entries.

### Slice 3 — complete bytecode reference coverage

- Extend the existing ASM inventory to scan nested mod/library bytecode separately from parent classes.
- Record types in class headers, signatures, annotations, descriptors, exceptions, instructions, invokedynamic bootstrap handles/arguments, method handles, and constant-dynamic values.
- Keep direct call sites distinct from symbolic references; include source method and bytecode offset when ASM data allows it.
- Optionally report reflection/resource string candidates as heuristics, never as confirmed type/member references.
- Test every reference kind with synthetic bytecode fixtures.

### Slice 4 — mixin analysis as a first-class result

- Refactor `Mixin1201Triage` into a state-free analyzer API with a dedicated result model and tests.
- Discover configs through manifest attributes, Forge metadata, and archive resource declarations; resolve config package/class names.
- Check target classes, mixin classes, shadow/accessor/invoker targets, injection target names and descriptors, `@At`, slice `from`/`to`, and handler descriptors.
- Report mixin plugins/refmaps/dynamic targets as requiring manual or runtime validation when static resolution is not authoritative.
- Compare target classes against the pinned NeoForge/Minecraft 1.21.1 index.

### Slice 5 — shared migration/bridge/shim coverage

- Join every finding to the migration tables, `Forge1201MappingCatalog`, bridge registry, shim index, adapter rules, and `Gap1201` output.
- Distinguish real implemented behavior from generated stub/no-op coverage.
- Make unsupported and soft-linked operations prominent; no “green” status from class linking alone.
- Add regression fixtures for known API changes and every new shared adapter.

### Slice 6 — pack-level preflight and runtime feedback

- Analyze a folder/modpack as one graph so peer APIs and native providers are considered without making unrelated mods mandatory.
- Add conflict/duplicate mod ID and incompatible dependency-range checks.
- Correlate static reports with runtime diagnostics and user-provided `latest.log`, `crash.txt`, `debug.log`, and `legacyconnector-report.txt`.
- Maintain isolated-mod, dependency-chain, client/server, and curated-interaction test profiles.
- Preserve a clear distinction between static, structural, client, gameplay, synchronization, and stability validation.

## Initial acceptance gate

A single candidate Forge 1.20.1 JAR must produce a repeatable text + JSON preflight report before translation. The report must:

1. Correctly identify all mod IDs and declared dependencies it can parse, and clearly disclose unsupported metadata.
2. Inventory top-level and nested archives without losing provenance.
3. Show referenced API types/members and their caller locations.
4. Classify known Forge APIs using the shared mapping catalog.
5. Separate exact structural matches from missing/incomplete/manual-adapter references.
6. Inventory and triage mixin declarations, with unsupported/dynamic cases explicitly called out.
7. Show resource namespaces and identify what still requires post-translation/runtime inspection.
8. Never report a mod as fully compatible solely because it parses, translates, links, or reaches the title screen.

Then validate the report against synthetic fixtures and at least one real 1.20.1-exclusive mod that is not Palladium or Alien Evolution. Do not expand to another named mod chain until this milestone is complete.

## Slice 1 implementation checkpoint

Implemented on `feat/forge1201-preflight-analyzer`; code checkpoint: `c9abafc94708ae1bc97e6840cd3feb46628f5051`. The current branch also contains a follow-up documentation-only checkpoint; use the branch tip after `git pull --ff-only`.

Files:
- `loader/src/main/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201PreflightAnalyzer.java`
- `loader/src/test/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201PreflightAnalyzerTest.java`
- Root `build.gradle`: new `preflight1201` JavaExec task.

Slice 1 currently provides:
- Immutable result records, schema/analyzer versions, deterministic JSON (LF output) and human-readable text.
- Input JAR SHA-256, size, normalized archive entry inventory, source resource namespaces, manifest attributes, FMLCorePlugin and MixinConfigs values, mixin JSON path/content candidates, and Jar-in-Jar paths.
- Reuse of `Forge1201JarInspector`, `Forge1201ApiUsageAnalyzer`, `Forge1201DependencyResolver`, and `Forge1201MappingCatalog`.
- Parsed mod declarations, duplicate IDs, dependency required/optional/side/order/range data, host-bridge/native-ID-present/missing statuses, and Forge API type classifications plus bytecode member/call-site inventory.
- Explicit analysis limitations and conservative wording. A native mod ID match is not provider version/API verification; the scan runs only against the recognized outer JAR.

Synthetic tests cover multiple mod declarations, duplicate IDs, array/multiline TOML warnings, required and optional missing dependencies, host bridge and native ID candidate statuses, API classification/call sites, manifest and heuristic mixin candidates, resource namespaces, Jar-in-Jar path inventory, missing metadata, SHA-256 presence, and stable JSON/text output.

**Not implemented in Slice 1:** recursive nested JAR analysis, provider JAR/version provenance, full bytecode reference coverage, target API member verification, semantic mixin validation, shim/no-op coverage joining, and pack-level graph analysis. These remain later slices.

**Local validation state:** user-confirmed loader tests and the consolidated clean build passed at source HEAD `9e1a1e14b597dd8ae690e9ae8c350809567dd1fc` on 2026-11-14. The first `preflight1201` invocation failed during Gradle configuration at `build.gradle:346` because Groovy treated a leading-newline `+` as unary plus and attempted `FilteredFileCollection.positive()`. Fixed in source commit `9f608a53d655f3c938a4a909da066f905f66e41e`.

**Real-JAR report review:** the testbench contains `symbiote-1.1.3.jar.txt` and `symbiote-1.1.3.jar.json`, uploaded in testbench commit `73930aaf3ab76017ca0c9232c557856bf7e39dac`. The JSON parsed successfully. Its SHA-256 `e40f48a8a9bf1f39ea173a7c163285df199b7d2cf1dfcbf3efa721fe7f2f942c` matches the original Symbiote 1.1.3 input. The report targets Minecraft 1.21.1 / NeoForge 21.1.258 and identifies `symbiote` 1.1.3, one mod declaration, no duplicate IDs, two declared host dependencies (`forge`, `minecraft`), 515 archive entries, 278 outer-JAR classes, namespace `symbiote`, no Jar-in-Jar paths, no discovered mixin config candidates, and no coremod class.

The JSON records 442 referenced types: 322 outside Forge API, 77 package-relocation candidates, 28 manual-adapter-required types, and 15 explicit type replacements. It also records 1,110 distinct method references, 352 field references, 5,369 grouped method-call-site records, and 1,196 grouped field-call-site records. Among Forge-targeting bytecode calls, 403 grouped method call sites represent 693 instructions and 88 grouped field call sites represent 109 instructions.

Important concrete hotspots include 13 `SimpleChannel.messageBuilder` registrations, `sendToServer` and `send` usage, 118 `RegistryObject.get()` occurrences, `DeferredRegister` registration, legacy tick/damage/player events, GUI overlay and config-screen extension points, `ModLoadingContext` / `FMLJavaModLoadingContext`, and `DistExecutor`. The type classifier correctly leaves 77 package moves as candidates rather than claiming member or behavioral equivalence.

The report shows 850 distinct SRG-style Minecraft method references and 258 SRG-style field references (aggregate occurrence counts 7,392 and 1,364). Treat this as evidence that the SRG → 1.20.1 Mojang-name normalization stage must remain explicit before 1.20.1 → 1.21.1 migration. The report's 7 installed native IDs (`architectury`, `azurelib`, `geckolib`, `kubejs`, `pehkui`, `playeranimator`, `rhino`) are observed provider candidates only; Symbiote does not declare them as required dependencies.

Metadata warning: `mods.toml` line 10 starts a multiline string that the scalar parser skips. The declared Minecraft range is `[1.20.1,1.21)`, which does not include the target 1.21.1; the current report prints the range but does not yet produce an explicit target-range mismatch finding. Add that check early in Slice 2. The five Mixin API references belong to `com/scout/symbiote/util/ForeignHooks.scan`, which inspects globally registered Mixin configs; their presence does not by itself mean Symbiote ships its own mixin config. No `MixinConfigs` or mixin resource candidate was found.

Both report files are present and internally consistent; however, the Gradle console transcript was not uploaded, so do not record the final console line as `BUILD SUCCESSFUL`. Report generation and JSON validity are confirmed by the uploaded artifacts. No runtime or gameplay compatibility is established.

### Slice 2 implementation checkpoint — committed; local validation pending

**Last code-bearing Slice 2 checkpoint:** `fdb4e111fab45b0f41c7f15cad26cefbbcf92f13` (the later branch commits in this checkpoint are documentation-only boundary/continuation updates).  
**Working branch:** `feat/forge1201-preflight-analyzer`  
**Implementation scope:** bounded nested archive inspection, provider provenance, dependency graph, target-range evaluation.  
**Validation status:** code and regression fixtures are committed, but no local Gradle test/build has yet been run for Slice 2. Do not call Slice 2 validated until the user runs the focused test suite and real-JAR smoke test.

Files changed:
- `loader/src/main/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201ArchiveGraph.java` — new bounded archive graph analyzer.
- `loader/src/main/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201JarInspector.java` — added in-memory metadata parsing helpers for embedded Forge 1.20.1 and NeoForge metadata.
- `loader/src/main/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201PreflightAnalyzer.java` — schema/analyzer version bumped to 1.1 / 1.1.0; graph added to JSON/text result.
- `loader/src/test/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201ArchiveGraphTest.java` — new synthetic nested/provider/range/safety test suite.

Implemented behavior:
1. Reads real nested JAR entries in memory (no extraction to persistent folders) using both Jar-in-Jar metadata paths and actual `META-INF/jarjar/` / `META-INF/jars/` archive entries.
2. Records source-root, installed top-level, source-nested, and installed-nested archive provenance, parent path, depth, size, SHA-256, Jar-in-Jar group/artifact/version range/artifact version where declared, detected loader family, archive classification, and contained mod IDs/versions.
3. Distinguishes Forge 1.20.1 mods, NeoForge mods, library/unrecognized archives, corrupt ZIPs, unsafe paths, missing declared archive files, byte/entry/depth/archive-count limits, and duplicate content skipped by SHA-256 cycle protection.
4. Discovers installed top-level NeoForge and Forge 1.20.1 providers and nested providers. A provider record links mod ID and declared version to the actual archive path and hash. The report still does not claim that ID/version equality proves API or behavioral compatibility.
5. Builds dependency edges for source and nested/installed mods while preserving declaring mod/archive, dependency ID, range, required/optional flag, side, and ordering. It distinguishes host bridges, present providers, provider version mismatches, duplicate/ambiguous providers, required missing providers, and optional missing providers.
6. Evaluates numeric dotted Maven-style ranges conservatively. Unsupported/qualified version strings remain `UNPARSEABLE`; they are never silently treated as matching.
7. Adds explicit target Minecraft range findings. For Symbiote, `[1.20.1,1.21)` is correctly expected to exclude target `1.21.1`; the explanation calls this a possible metadata-range translation requirement, not an invented dependency or unconditional load-failure verdict.
8. Preserves deterministic ordering and exposes the archive graph in both JSON and text reports. Nested bytecode remains out of scope until Slice 3.

Safety limits currently encoded:
- Maximum nested depth: 4.
- Maximum nested archive candidates inspected: 128.
- Maximum individual nested JAR payload: 32 MiB.
- Maximum inflated content per nested archive: 64 MiB.
- Maximum aggregate nested inflated bytes: 256 MiB.
- Maximum metadata document read: 1 MiB.
- Maximum archive entry names considered: 100,000 at the root and 100,000 within a nested archive.
- No filesystem extraction; absolute, drive-qualified, backslash, empty, dot, and parent-traversal entry paths are rejected.

Synthetic coverage added:
- Nested Forge mod vs ordinary library classification and Jar-in-Jar coordinate/hash provenance.
- Installed top-level NeoForge and Forge 1.20.1 provider discovery plus installed nested NeoForge provider discovery.
- Provider version match, out-of-range mismatch, duplicate/ambiguous mod IDs, required/optional missing dependencies.
- Minecraft target mismatch, exact/numeric/union ranges, and unparseable qualified versions.
- Missing declared JAR, corrupt archive, traversal path, duplicate archive content, maximum depth, per-archive size, and archive-count limits.

The test suite is not yet confirmed. Potential test or implementation defects must be corrected from the actual Gradle output; do not treat a successful GitHub write or source inspection as a passing compile.

### Exact next action — validate Slice 2 in the Universal worktree

First synchronize only the Universal worktree, after verifying it is clean and on the expected branch:

```powershell
$repo = 'F:\\Program\\intellij-idea-portable\\Legacy-Connector-1.20.1'
$branch = 'feat/forge1201-preflight-analyzer'
$actual = (git -C $repo branch --show-current).Trim()
if ($actual -ne $branch) { throw "Wrong branch: expected $branch, found $actual" }
$dirty = @(git -C $repo status --porcelain)
if ($dirty.Count -gt 0) { throw "Universal worktree is dirty. Inspect local changes before syncing." }
git -C $repo fetch origin "$branch"
if ($LASTEXITCODE -ne 0) { throw "Fetch failed." }
git -C $repo pull --ff-only origin "$branch"
if ($LASTEXITCODE -ne 0) { throw "Pull failed." }
git -C $repo rev-parse HEAD
```

Expected source HEAD: `fdb4e111fab45b0f41c7f15cad26cefbbcf92f13`.

Then, still in that exact Universal checkout, run:

```powershell
.\\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain
```

If the focused loader tests pass, run the `preflight1201` task against the original Symbiote 1.1.3 Forge JAR and the same installed NeoForge mods directory used for the previous report. Save both JSON and text reports, then inspect the JSON for archive graph provenance, dependency resolution, and the `[1.20.1,1.21)` target-range result. Keep the original JAR SHA-256 `e40f48a8a9bf1f39ea173a7c163285df199b7d2cf1dfcbf3efa721fe7f2f942c` as the input identity check. If the report is correct, rerun the consolidated clean build before marking Slice 2 validated.

Do not change the Palladium worktree or branch. Do not merge anything into `master`. Do not claim runtime compatibility from preflight output.

Later slices remain unchanged: Slice 3 nested and complete bytecode references; Slice 4 first-class mixin validation; Slice 5 migration/bridge/shim coverage; Slice 6 pack-level graph and runtime feedback. Keep the SRG → 1.20.1 Mojang-name normalization stage separate from 1.20.1 → 1.21.1 migration.


## Validation boundary

User-confirmed validation on 2026-11-14 at source HEAD `9e1a1e14b597dd8ae690e9ae8c350809567dd1fc`:

- `:loader:clean :loader:test --no-daemon --console=plain` — BUILD SUCCESSFUL; 12 actionable tasks, 1m 30s.
- `:clean :loader:clean :testmods:clean :build :loader:build :testmods:assemble --no-daemon --console=plain` — BUILD SUCCESSFUL; 33 actionable tasks, 5m 32s.
- Deprecation/removal warnings and Gradle 9.0 compatibility warnings were present but non-fatal.

The analyzer code is compile/test/build validated at that exact source commit. The corrected real-JAR smoke artifacts are present and reviewed as described above; the final Gradle console result was not uploaded. This is static preflight evidence only and establishes no Minecraft runtime compatibility.
