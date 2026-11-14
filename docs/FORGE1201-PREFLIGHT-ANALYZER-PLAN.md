**Current checkpoint — 2026-11-15 (Intermediary report reviewed; descriptor classification refined, test pending):** The user supplied the generated report. It records MCPConfig joined.tsrg SHA-256 `19ae000175bfb58e5b26406c31a0e501bfb40000b078fee2f00ea60a8c7ed453`, source Intermediary 1.20.1 SHA-256 `5a808bd1ebc4a9ee7d1c5900dbd2bf391586b536c3d86fbf2f972047faa2d165`, and target Intermediary 1.21.1 SHA-256 `83071b5b17866658463255f5f21430ade2a49ce2dd089dc3c14de1e93b99211c`. Counts: 7,413/7,436 source class joins; 7,011 source class IDs shared with target; 34,390/48,575 source method joins; 31,003/31,003 source field joins; 188 reused SRG method IDs across distinct Intermediary symbols; zero reported source/target ambiguities. The report counted 20,131 exact methods and 22,813 exact fields, but its descriptor-unresolved totals (11,330 method rows and 5,722 field rows at source; 10,076 method and 5,262 field rows at target) are **not reliable measures of missing Minecraft types**: code review found the parser marked every descriptor object type absent from Tiny as unresolved, including ordinary external/JDK types such as `java/lang/String` shown in the Tiny v1 format examples.

 A follow-up source-counter refinement in commit `0dbd61d72d9d246c73b7734e67b669c5efa6413d` records package-qualified external type occurrences even when the same descriptor also contains a separate unresolved type. This keeps the source external/unresolved counters independent.
A corrective implementation now distinguishes package-qualified external descriptor candidates from known source Minecraft classes missing a Tiny identity. External pass-through rows are reported separately and are not counted as fully mapped exact. Regression tests cover both external Java/library types and known missing Minecraft descriptor classes. Code is committed in `2b6f64feee1d557647ea11b2275d237514761b64`, with test/counter commits through `43e02d86b4290e5325faa0b487ad975cf2b7c5eb` and `0d65007b9dd73c8d3069dd691fc028c257c56766`. The design review is recorded in the Intermediary bridge audit design document. **This correction has not yet been user-tested.** The original supplied JSON remains historical; regenerate after the next forced test gate. Complete Gradle console was not supplied for the prior report, so report generation is confirmed but the whole test-suite result is not independently confirmed.

**Current checkpoint — 2026-11-15 (Intermediary Gradle configuration correction):** The user fast-forwarded their local `feat/forge1201-preflight-analyzer` checkout to `343f563cfefecffc916988ae52cf379eefc413e1`. The first forced loader test attempt stopped while evaluating `loader/build.gradle`, before compilation or tests, because the two Intermediary dependencies used invalid Groovy command-style notation with trailing closures. Corrected both declarations to explicit Gradle `add(configuration, dependency) { transitive = false }` calls in commit `4bbd0e155ac6277a6b50e930ec551d827c91f3c7`. This is a build-script-only correction; the audit code/tests remain unchanged. **The corrected forced test gate is still pending user execution.** Do not mark the Intermediary audit green or expect its report until the forced run succeeds.

**Current checkpoint — 2026-11-15:** Schema 1.10A is user-validated green at `cc651af52b192fb534d49f3ad494b42e3d15d6f0`; Schema 1.10B and its repeated-SRG-ID regression are user-validated green at `755281d3b91a6cb1b818f5a927728cc412a6ec71` (`BUILD SUCCESSFUL in 50s; 17/17 tasks`). The provenance report and unresolved Mojang mapping redistribution gate are documented. A new **read-only Intermediary bridge feasibility audit** has been added to the loader test graph, with pinned Tiny v1 inputs, build-local JSON output, and synthetic tests. The branch tip is `01d1fc5f6b520b0c6b6aabcd309e7b292810f29d`, 13 commits ahead / 0 behind the last user-tested local tip. The new Java/Gradle/test increment has **not** been run by the user. It does not map Forge SRG selectors or rewrite selectors; report scope is the MCPConfig obfuscated namespace → Intermediary → target obfuscated namespace only.

---

**Status:** Slices 1–3 are implemented and validated. Slice 4 is in progress: the shared TargetIndex API/tests passed at `246002c6fcc1af371bb11a716483dd1df23a2dbe` (`BUILD SUCCESSFUL in 1m 40s`, 14/14 tasks), and the bounded outer-JAR `Forge1201MixinAnalyzer` plus its synthetic ZIP/ASM tests passed at `90daa1ee78d2de73ff44e27ffd01ba6e126858f4` (`BUILD SUCCESSFUL in 57s`, 14/14 tasks). Remaining Slice 4 work includes automatic config declaration integration, nested archive provenance, descriptor-aware member/injector/selector analysis, and unified preflight reporting.

**Status:** Slices 1 and 2 are implemented and validated. The first Slice 3 dynamic-reference batch passed the user's forced focused loader test gate at `4457199a4109d774b25a428e7c68e23b4ec3b4ce` (`BUILD SUCCESSFUL in 24s`, 12/12 tasks executed). The nested-bytecode/SRG integration was committed as a494d20a3be797abd3de563fa3672df69e2276da; its stale limitation assertion was corrected in 3c6febb075c32391b6879b0f3ef00c5856e80d51; and test runtime mapping-resource wiring was added in 0c7c55b72033c986d94f1cba5d836dfce26e95a0. The test/runtime wiring checkpoint is 0c7c55b72033c986d94f1cba5d836dfce26e95a0; the branch may include a later documentation-only commit. Use the exact remote tip in the testbench handoff for the guarded pull. Structural reference coverage remains under audit.

**Report filename rule:** every analyzer output filename includes its slice number, e.g. `symbiote-1.1.3.jar-slice-2.json` and `symbiote-1.1.3.jar-slice-2.txt`. Repeated runs append a run number, e.g. `symbiote-1.1.3.jar-slice-2-run-2.json` and `.txt`. Use the root Gradle task's explicit `-PpreflightJson=<path>` and `-PpreflightText=<path>` options. Never overwrite the unsuffixed legacy root filenames or an earlier slice/run. The testbench archive index is `archive/preflight/README.md`.
  
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

**Detailed design:** see [`docs/FORGE1201-PREFLIGHT-SLICE-3-DESIGN.md`](FORGE1201-PREFLIGHT-SLICE-3-DESIGN.md). It records the scanner/archive/mapping architecture, bounded processing rules, report contract, and regression matrix.

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

### Slice 4 implementation checkpoint — target index API (2026-11-14)

- Source commit `e3aa921d01d8ee134de3a71d3aa1016d249dbaf1` exposes `Forge1201TargetApiVerifier.TargetIndex`, backed by the existing target verifier's per-instance cache and exact descriptor-aware inherited lookup.
- Test commit `69a93e13183466e51a6bab8023eecb0bb906e5e9` adds synthetic ASM tests for exact method/field descriptors, inherited lookup, missing classes, incomplete hierarchies, and independent index instances.
- The user subsequently ran the forced loader test gate at `246002c6fcc1af371bb11a716483dd1df23a2dbe`: `BUILD SUCCESSFUL in 1m 40s`, 14 actionable tasks, all 14 executed. The TargetIndex implementation and its tests are validated.
- The first `Forge1201MixinAnalyzer` outer-JAR config/target-discovery API and synthetic test class were pulled and passed in the user's forced loader test run at `90daa1ee78d2de73ff44e27ffd01ba6e126858f4`: `BUILD SUCCESSFUL in 57s`, 14 actionable tasks, all 14 executed. The implementation is validated within its documented scope; it is not complete Mixin semantic analysis.

### Slice 4 implementation checkpoint — bounded Mixin analyzer foundation (2026-11-14)

- The user-confirmed TargetIndex forced loader test gate passed at commit `246002c6fcc1af371bb11a716483dd1df23a2dbe`: `BUILD SUCCESSFUL in 1m 40s`, 14 actionable tasks, all 14 executed.
- Analyzer commit `df4d8531b1b1a739119ca3be8bbf982c04f42bd0` adds `Forge1201MixinAnalyzer`, a state-free per-call API for bounded outer-JAR config parsing, mixin-class resolution, visible/invisible `@Mixin` targets, explicit class mapping states, target class checks through shared `TargetIndex`, and non-executing plugin/refmap resource checks.
- Follow-up source commits: `d0248dd544e028f55abb2bf41ac2d1a43f51c9ad` (mapping/class/plugin state corrections) and `c81b8a2556819f85332b7b527f09635fff9f1422` (archive inventory truncation states and annotation iteration).
- Test commit `d852da1ffd64d59dca1e9b4f3148077e244d4cfd` adds synthetic ZIP/ASM tests. These new tests are pending the next user forced test run.
- Scope limits: outer JAR only; configs are supplied explicitly; Minecraft target checks need a named mapping input; plugin presence is checked in the source JAR only; nested source archives, Forge metadata discovery, member annotations/injectors, `@At`/`@Slice`, semantic refmap validation, and preflight JSON/text integration remain future work.


### Slice 4 validation checkpoint — Mixin config/target foundation passed (2026-11-14)

- User pulled from local `246002c6fcc1af371bb11a716483dd1df23a2dbe` to `90daa1ee78d2de73ff44e27ffd01ba6e126858f4`; all branch/clean-worktree/remote-SHA guards and the fast-forward/final-HEAD checks passed.
- User ran `:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks`.
- Result: `BUILD SUCCESSFUL in 57s`; 14 actionable tasks, all 14 executed. This validates the added `Forge1201MixinAnalyzerTest` along with the full loader test task at this source checkpoint.
- Latest verified feature-branch HEAD: `90daa1ee78d2de73ff44e27ffd01ba6e126858f4`.
- Next bounded step: inspect how the existing Gradle `preflight1201` task exposes the target classpath and how `Forge1201PreflightAnalyzer` discovers configs; then wire the tested Mixin result API into the unified preflight result without duplicating target indexes or archive traversal. Keep nested-archive config/class provenance explicit and reuse `Forge1201ArchiveGraph`.

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

### Slice 2 implementation checkpoint — corrected, tested, and real-JAR report verified

**Last code-bearing Slice 2 checkpoint:** 4ece76833d1614738bc626fe64dbde31e42b8941 (the two commits after this checkpoint are documentation-only updates to this plan).  
**Current Universal branch:** feat/forge1201-preflight-analyzer  
**Current branch tip before this plan update:** aa5f5d32ba865e7f056851b1dfd39c12fb8fcffc  
**Implementation scope:** bounded nested archive inspection, provider provenance, dependency graph, Forge/NeoForge dependency-type interpretation, and target-range evaluation.

**Focused validation:** the user-confirmed command :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks passed at code-bearing source HEAD 4ece76833d1614738bc626fe64dbde31e42b8941 on 2026-11-14. Output: BUILD SUCCESSFUL in 40s; 12 actionable tasks, all 12 executed, including production/test compilation, genBetaMappings (9,243 rows), and :loader:test. The current branch tip aa5f5d32ba865e7f056851b1dfd39c12fb8fcffc is two documentation-only commits beyond that tested code checkpoint; GitHub comparison confirms only this plan file changed.

**Corrected real-JAR report — generated and inspected:** the user generated the report using the corrected branch and uploaded both files to the testbench root in commit 9b89a4a1d10c89cb93f9399ac49b01ec93e1b1c7.

- JSON: symbiote-1.1.3.jar-slice-2-run-3.json; Git blob f7c8200ea32c5a9927e6459ce19547a4831e986a.
- Text: symbiote-1.1.3.jar-slice-2-run-3.txt; Git blob 8e6109e3f7401c9e56d96e81e15957e69a863ec7.
- Schema 1.2; analyzer 1.2.0; source symbiote-1.1.3.jar; source SHA-256 e40f48a8a9bf1f39ea173a7c163285df199b7d2cf1dfcbf3efa721fe7f2f942c.
- Target Minecraft 1.21.1; target NeoForge 21.1.258.
- Inventory: 515 archive entries, 11 nested/embedded archive records, 9 installed/embedded providers, 17 dependency edges, 5 Minecraft target-range findings, 278 classes, 442 referenced API types, 1,110 method references, 352 field references, 5,369 method call-sites, and 1,196 field call-sites.
- The JSON blob was fetched directly by Git blob SHA and parsed successfully; its structures and dependency edges agree with the text report. The ordinary file-content endpoint returned an empty body for this large JSON blob, so the blob fetch was used for validation.

**Corrected graph results:**

1. Forge and NeoForge metadata semantics are now reflected correctly. NeoForge minecraft / neoforge dependencies use host-bridge statuses rather than false REQUIRED_MISSING findings. Required / optional / incompatible dependency types are retained.
2. Symbiote's own Minecraft range [1.20.1,1.21) is HOST_VERSION_MISMATCH / OUTSIDE_RANGE against target 1.21.1. This is a metadata-range finding, not a claim that the mod necessarily cannot run after translation.
3. Installed NeoForge host ranges that the conservative parser supports match target 21.1.258 (including AzureLib, GeckoLib, KubeJS, Player Animator, and Better Advanced Tooltips). Architectury's qualified NeoForge range [21.0.110-beta,) remains UNPARSEABLE.
4. KubeJS finds its nested Better Advanced Tooltips provider and installed Rhino provider. Their qualified version strings 2101.1.0-build.1 and 2101.2.8-build.91 remain UNPARSEABLE; provider presence is not upgraded into a proven version match or API-compatibility claim.
5. KubeJS's EMI, JEI, and Roughly Enough Items dependencies are optional and absent in the scanned mods directory. GeckoLib declares geckoanimfix as incompatible; the report correctly records that incompatible provider as absent.
6. Symbiote's Forge [47,) dependency is represented as a host bridge. Its target Minecraft range remains the single relevant host-version mismatch.

**Warnings and evidence boundary:**

- One metadata warning: mods.toml line 10 begins a multiline string that the scalar metadata inspector does not parse.
- One archive-graph warning: Symbiote declares [1.20.1,1.21), excluding target 1.21.1.
- No Mixin config candidates were found in this report.
- This is static preflight evidence only. It does not prove class linking, startup, gameplay, client rendering, synchronization, or stability.
- Slice 2 inventories nested archives and their metadata, but bytecode references are still scanned only in the outer source JAR; nested bytecode analysis is deferred to Slice 3.
- The report explicitly preserves conservative handling of qualified version strings and the bounded nested-archive limits documented above.

Files changed by Slice 2:
- loader/src/main/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201ArchiveGraph.java — bounded archive graph analyzer.
- loader/src/main/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201JarInfo.java — explicit Forge/NeoForge dependency-type model.
- loader/src/main/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201JarInspector.java — in-memory metadata parsing helpers for embedded Forge 1.20.1 and NeoForge metadata.
- loader/src/main/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201PreflightAnalyzer.java — schema/analyzer version 1.2 / 1.2.0 and graph output in JSON/text.
- loader/src/test/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201ArchiveGraphTest.java — synthetic nested/provider/range/safety regression suite.

Implemented behavior:
1. Reads real nested JAR entries in memory (no extraction to persistent folders) using both Jar-in-Jar metadata paths and actual META-INF/jarjar/ / META-INF/jars/ archive entries.
2. Records source-root, installed top-level, source-nested, and installed-nested archive provenance, parent path, depth, size, SHA-256, Jar-in-Jar group/artifact/version range/artifact version where declared, detected loader family, archive classification, and contained mod IDs/versions.
3. Distinguishes Forge 1.20.1 mods, NeoForge mods, library/unrecognized archives, corrupt ZIPs, unsafe paths, missing declared archive files, byte/entry/depth/archive-count limits, and duplicate content skipped by SHA-256 cycle protection.
4. Discovers installed top-level NeoForge and Forge 1.20.1 providers and nested providers. A provider record links mod ID and declared version to the actual archive path and hash. An ID/version match does not establish API or behavioral compatibility.
5. Builds dependency edges for source and nested/installed mods while preserving declaring mod/archive, dependency ID, range, required/optional/incompatible/discouraged type, side, and ordering. It distinguishes host bridges, present providers, provider version mismatches, duplicate/ambiguous providers, required missing providers, optional missing providers, and incompatible/discouraged provider states.
6. Evaluates numeric dotted Maven-style ranges conservatively. Unsupported/qualified version strings remain UNPARSEABLE; they are never silently treated as matching.
7. Adds explicit target Minecraft range findings. For Symbiote, [1.20.1,1.21) is correctly identified as excluding target 1.21.1; the explanation calls this a possible metadata-range translation requirement, not an invented dependency or unconditional load-failure verdict.
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
- Provider version match, out-of-range mismatch, duplicate/ambiguous mod IDs, required/optional missing dependencies, and NeoForge dependency-type handling.
- Minecraft target mismatch, exact/numeric/union ranges, and unparseable qualified versions.
- Missing declared JAR, corrupt archive, traversal path, duplicate archive content, maximum depth, per-archive size, and archive-count limits.

### Slice 3 implementation checkpoint — outer-JAR dynamic/bootstrap references

**Status:** Initial code batch committed; focused tests pending. This is a partial Slice 3 milestone, not complete nested-bytecode coverage.

**Source implementation commits on `feat/forge1201-preflight-analyzer`:**
- `2c2a8f1bbe0805adbffadc6418c446b5c41ea227` — adds contextual dynamic-reference records and ASM collection for invokedynamic, bootstrap handles/arguments, LDC Type/Handle constants, and recursive ConstantDynamic values.
- `cd8e701f6c55cff0d48361fe6a4c66916a1754c4` — exposes dynamic-reference findings in preflight JSON/text and bumps the report to Schema 1.3 / Analyzer 1.3.0.
- `69daf6f4de554c4e57d76824bca1372ee3c15160` — adds synthetic bytecode fixture coverage.
- `1e7618b9ac226537403cce706f5a55fd4be6f506` — preserves the method-handle interface bit and fixes archive-entry path capture to use a per-class effectively-final value.
- `7c362404d5813efae7fc841d85d176bf7fef1723` — expands assertions for LDC method handles and serialized report output.
- `44fa6838ef7624ac0116759a31918caaf88673f1` — fixes the invokedynamic-site record call for the expanded reference model.
- `7260837c2d0027e7a4aa23f4844b2e581a7080fe` — balances the synthetic class fixture's operand stack.
- `95e18d3b6b163b3fbb3242e0e758c2763c95ffce` — reports the defensive recursive dynamic-constant depth limit explicitly.

**Current behavior in this batch:**
1. Existing aggregate method/field references and direct call-site findings are retained.
2. New contextual findings preserve archive path, class entry path, caller class/method/descriptor, context, target owner/name/descriptor, handle tag, interface-owner flag, count, and zero-based instruction ordinal.
3. `INVOKEDYNAMIC` instructions record the site, bootstrap handle, and recursively inspected bootstrap arguments.
4. `LDC` captures API method handles, field handles, class literals, and method-type constants.
5. `ConstantDynamic` captures its symbolic constant, bootstrap handle, and recursively inspected arguments. A defensive recursion-depth overflow is an explicit finding.
6. Instruction ordinal is deliberately not called a byte offset; ASM's visitor API does not supply exact byte offsets for each event.
7. Text and JSON reports share the same result model. The new dynamic-reference section is deterministic.

**Explicitly still incomplete:**
- This first batch scans only the outer source JAR. Nested archive classes are not yet fed into ASM.
- Source SRG member names are not yet normalized through the generated `SrgMojangTable`.
- Class headers, signatures, annotations, descriptors, exceptions and many other symbolic references continue to rely on the existing `ClassRemapper` aggregate callbacks; the remaining Slice 3 audit must add contextual evidence and tests where current callbacks are insufficient.
- Reflection/resource string candidates remain deferred heuristics.
- The Schema 1.3 report has not yet been generated against Symbiote; do not overwrite Slice 2 run-3.

### Slice 3 first test-run result and correction

**User-run result:** The Universal branch fast-forwarded from `aa5f5d32ba865e7f056851b1dfd39c12fb8fcffc` to `0bfcb5b4022da8da7d8aff7b3630ae6ec4ebc08d`. Gradle compiled the main and test code successfully, then ran 452 tests with exactly one failure: `Forge1201PreflightAnalyzerTest > buildsDeterministicInventoryAndPreservesDependencyEvidence()` at line 98. The failure was not in a dynamic-reference assertion. Line 98 still expected the Slice 2 limitation phrase `outer JAR only`, but the Slice 3 implementation had intentionally revised the limitation to say that nested archive class bytecode remains pending. All other 451 tests passed in that invocation.

**Fix committed:** `804c9c6c1e51969cc0168848cd0ebea3e551f354` updates the stale assertion to require the current report sentence to contain both `outer JAR` and `nested archive class bytecode`. This makes the test check the actual remaining-analysis boundary rather than freeze an obsolete phrase.

**Interpretation:** The first run reached `:loader:test`, so the code and test sources compiled. The failure indicates a stale assertion after a deliberate report-wording change, not evidence of a scanner implementation defect. Nevertheless, no passing Slice 3 test result exists yet; rerun the complete forced loader test suite after pulling the fix.

### Exact next action — pull the stale-assertion fix and rerun

The user has already fast-forwarded the Universal local worktree to `0bfcb5b4022da8da7d8aff7b3630ae6ec4ebc08d`; Gradle failed after running tests and the PowerShell guard correctly surfaced the nonzero exit. The new assertion fix is now on the remote branch. Confirm the same Universal path and branch, confirm the expected local HEAD and a clean worktree, fetch the exact remote branch head, fast-forward, then rerun the focused tests with all tasks forced.

```powershell
$repo = 'F:\Program\intellij-idea-portable\Legacy-Connector-1.20.1'
$branch = 'feat/forge1201-preflight-analyzer'
$expectedLocalHead = '0bfcb5b4022da8da7d8aff7b3630ae6ec4ebc08d'

$actualRepo = [System.IO.Path]::GetFullPath((git -C $repo rev-parse --show-toplevel).Trim().Replace('/', '\')).TrimEnd('\')
$expectedRepo = [System.IO.Path]::GetFullPath($repo).TrimEnd('\')
if (-not [string]::Equals($actualRepo, $expectedRepo, [System.StringComparison]::OrdinalIgnoreCase)) { throw "Unexpected repository path: $actualRepo" }
$actualBranch = (git -C $repo branch --show-current).Trim()
if ($actualBranch -ne $branch) { throw "Wrong branch: $actualBranch" }
$actualHead = (git -C $repo rev-parse HEAD).Trim()
if ($actualHead -ne $expectedLocalHead) { throw "Unexpected local HEAD: $actualHead" }
$changes = @(git -C $repo status --porcelain)
if ($changes.Count -gt 0) { $changes; throw "Worktree is not clean. Stop and inspect changes." }

$remoteLines = @(git -C $repo ls-remote origin "refs/heads/$branch")
if ($LASTEXITCODE -ne 0 -or $remoteLines.Count -ne 1) { throw "Could not resolve remote branch." }
$expectedRemoteHead = ($remoteLines[0] -split '\s+')[0]
if ($expectedRemoteHead -notmatch '^[0-9a-f]{40}$') { throw "Invalid remote branch SHA." }
git -C $repo pull --ff-only origin $branch
if ($LASTEXITCODE -ne 0) { throw "Pull failed." }
$pulledHead = (git -C $repo rev-parse HEAD).Trim()
if ($pulledHead -ne $expectedRemoteHead) { throw "Unexpected pulled HEAD: $pulledHead" }

Push-Location $repo
.\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
$testExit = $LASTEXITCODE
Pop-Location
if ($testExit -ne 0) { throw "Slice 3 focused test gate failed." }
```

Do not run the real Symbiote report yet. Once this gate passes, continue nested archive bytecode and source SRG-to-Mojang integration. Keep the Palladium + Alien Evolution worktree/branch isolated.



The user's last confirmed local Universal checkout was `aa5f5d32ba865e7f056851b1dfd39c12fb8fcffc`. The remote branch now has the Slice 3 source commits above plus this plan update. First fast-forward the **Universal** worktree only, then run the focused loader tests with `--rerun-tasks`. The command must stop on wrong path, branch, HEAD, dirty worktree, failed pull, or unexpected pulled HEAD.

After the forced loader tests pass, report the exact result. Do not generate the new real-JAR Schema 1.3 report yet: first continue Slice 3 integration with nested archive classes and the existing SRG-to-Mojang table, then run tests and produce one new unique Symbiote report filename.

```powershell
$repo = 'F:\Program\intellij-idea-portable\Legacy-Connector-1.20.1'
$branch = 'feat/forge1201-preflight-analyzer'
$expectedLocalHead = 'aa5f5d32ba865e7f056851b1dfd39c12fb8fcffc'

$actualRepo = [System.IO.Path]::GetFullPath((git -C $repo rev-parse --show-toplevel).Trim().Replace('/', '\')).TrimEnd('\')
$expectedRepo = [System.IO.Path]::GetFullPath($repo).TrimEnd('\')
if (-not [string]::Equals($actualRepo, $expectedRepo, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Unexpected repository path: $actualRepo"
}
$actualBranch = (git -C $repo branch --show-current).Trim()
if ($actualBranch -ne $branch) { throw "Wrong branch: expected $branch, found $actualBranch" }
$actualHead = (git -C $repo rev-parse HEAD).Trim()
if ($actualHead -ne $expectedLocalHead) { throw "Unexpected local HEAD: expected $expectedLocalHead, found $actualHead" }
$changes = @(git -C $repo status --porcelain)
if ($changes.Count -gt 0) { $changes; throw "Worktree is not clean. Stop and inspect local changes." }

$remoteLines = @(git -C $repo ls-remote origin "refs/heads/$branch")
if ($LASTEXITCODE -ne 0 -or $remoteLines.Count -ne 1) { throw "Could not resolve the exact remote branch tip." }
$expectedRemoteHead = ($remoteLines[0] -split '\s+')[0]
if ($expectedRemoteHead -notmatch '^[0-9a-f]{40}$') { throw "Invalid remote branch SHA: $expectedRemoteHead" }

git -C $repo pull --ff-only origin $branch
if ($LASTEXITCODE -ne 0) { throw "Fast-forward pull failed." }
$pulledHead = (git -C $repo rev-parse HEAD).Trim()
if ($pulledHead -ne $expectedRemoteHead) { throw "Unexpected pulled HEAD: $pulledHead" }

Push-Location $repo
.\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
$testExit = $LASTEXITCODE
Pop-Location
if ($testExit -ne 0) { throw "Slice 3 focused loader test gate failed." }
```

Keep the Palladium + Alien Evolution worktree/branch untouched. Do not start or merge into master. No runtime compatibility claim is implied by the new static references.

## Validation boundary

**Confirmed on 2026-11-14:**

- Focused loader test at code commit `4ece76833d1614738bc626fe64dbde31e42b8941`: `:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks` — `BUILD SUCCESSFUL in 40s`; 12 actionable tasks, all 12 executed.
- Corrected Slice 2 real-JAR report: Schema 1.2 / Analyzer 1.2.0; Symbiote run-3 JSON/text generated, uploaded, and inspected. See the Slice 2 checkpoint for exact blob SHAs and findings.
- Consolidated build at local code checkout `aa5f5d32ba865e7f056851b1dfd39c12fb8fcffc`: `:clean :loader:clean :testmods:clean :build :loader:build :testmods:assemble --no-daemon --console=plain` — `BUILD SUCCESSFUL in 2m 28s`; 33 actionable tasks, 25 executed and 8 from cache. The commits between the forced-test code checkpoint `4ece768...` and this checkout changed only documentation; no production/test source changed after the forced test gate.
- In the consolidated run, `:loader:test` and root `:test` were FROM-CACHE. The forced test gate above is the source for the claim that loader tests were freshly executed.
- Non-fatal warnings: 18 Java deprecation/removal warnings and Gradle 9.0 compatibility/deprecation warnings.
- The Slice 3 dynamic-reference implementation is committed, but its new forced test gate is pending. No Slice 3 test/build pass is claimed.
- No Minecraft runtime or gameplay compatibility is established by the tests or static report artifacts.


## CURRENT CONTINUATION UPDATE — 2026-11-14 — Slice 3 nested bytecode and SRG integration prepared

### Confirmed user test gate before this batch

The user pulled the stale test assertion fix through commit 4457199a4109d774b25a428e7c68e23b4ec3b4ce on feat/forge1201-preflight-analyzer and ran:

```text
:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
BUILD SUCCESSFUL in 24s
12 actionable tasks: 12 executed
```

This confirms the dynamic-reference outer-JAR batch and corrected assertion. The user's transcript did not include a JUnit test count. The old PSReadLine warning is unrelated.

### Prepared integration changes

- Extend the existing bounded Forge1201ArchiveGraph with a source-nested class-entry consumer and deterministic per-archive class scan summaries.
- Scan source-nested Forge mod and library/unrecognized archive classes using the same Forge1201ApiUsageAnalyzer.Inventory that scans the root JAR. Installed top-level provider bytecode and nested installed-provider bytecode remain excluded; native NeoForge nested mod bytecode is also excluded.
- Add explicit class limits: 8 MiB individual class, 16 MiB captured class bytes per nested archive, 128 MiB aggregate nested class bytes submitted to the scanner, and 64 MiB outer class bytes.
- Duplicate class entry names and unsafe paths are skipped with warnings. Oversized/aggregate-limit classes are summarized as skipped. A nested scan summary keeps the full archive path, archive classification, discovered/scanned/skipped counts, submitted bytes and warnings.
- Keep the old report classCount meaning (outer classes) and add nestedClassCount, nested per-archive summaries and warnings.
- Normalize SRG-looking member tokens only when the symbolic owner is net/minecraft/, using the existing generated SrgMojangTable.load(). Preserve raw SRG, mapped Mojang name or explicit UNMAPPED, descriptor, count, and archive/class locations. No mapping is treated as a target-version compatibility verdict.
- Regression fixtures cover a nested class with both a table-backed method token and a deliberately unknown token, and a nested class exceeding the entry limit. Mapping utility tests also exclude third-party owners.
- ClassRemapper structural callback completeness has not been fully audited. Headers, signatures, annotations, descriptors, exceptions, try/catch, type instructions, frames and local-variable contexts remain an explicit follow-on Slice 3 task. Duplicate-entry test coverage also remains to be added if a robust raw ZIP fixture is available.

### Validation boundary

- The integration batch is committed as a494d20a3be797abd3de563fa3672df69e2276da, followed by the stale-limitation assertion correction in 3c6febb075c32391b6879b0f3ef00c5856e80d51. These changes are not yet user-tested. Do not claim that the code compiles or that the new tests pass.
- Existing confirmed green checkpoint remains 4457199a4109d774b25a428e7c68e23b4ec3b4ce.
- Next: the user's local checkout is still expected at 4457199a4109d774b25a428e7c68e23b4ec3b4ce. Pull the Universal branch through the exact latest tip recorded in the testbench handoff with path/branch/HEAD/clean-worktree guards, then run the forced :loader:clean :loader:test gate. This gate now also runs fetchMojangMappings1201 and genSrgMojang1201 and places the generated table on the test classpath. Do not generate a new Symbiote report until this gate is green.
- The next Symbiote output must use a new run suffix; do not overwrite Slice 2 run-3.
- Keep the friend Palladium + Alien Evolution worktree/branch untouched.

### New-chat reminder

Read docs/CHATGPT-CONTINUATION-HANDOFF.md in the testbench repository first. Continue from the integration batch's user test gate; preserve the universal modpack-ready vision and branch isolation.


### Pre-test review correction — 2026-11-14

The integration test initially retained an assertion for the previous Slice 3 limitation that nested class bytecode was pending. The report limitation now correctly says the outer source JAR and bounded source-nested Forge/library archives are inventoried. Commit 3c6febb075c32391b6879b0f3ef00c5856e80d51 updates that assertion. The current Universal branch tip is 3c6febb075c32391b6879b0f3ef00c5856e80d51; user validation is still pending.


### Generated mapping-resource test wiring — 2026-11-14

Commit 0c7c55b72033c986d94f1cba5d836dfce26e95a0 updates loader/build.gradle so the loader test task depends on genSrgMojang and adds srgMojangDir to the test runtime classpath. The generated mapping table is not a committed source resource: SrgMojangTable.load() returns an empty map when that resource is absent. A clean forced loader test therefore runs the existing fetchMojangMappings1201 task (pinned Mojang client mapping URL) followed by genSrgMojang1201 before tests. The build wiring is committed but has not yet been exercised by the user.


## 2026-11-14 — Oversized nested-class summary warning regression

The user pulled commit `1c403bcc93f3e2c1d57215aea7e41419f29e1ed3` and ran the forced clean loader test gate. JUnit completed 454 tests with one failure at `Forge1201PreflightAnalyzerTest.java:144`, in `reportsOversizedNestedClassEntriesAsExplicitlySkipped()`. The oversized class was correctly counted as skipped and the archive snapshot correctly emitted the limit warning, but the per-archive `ClassScanSummary.warnings()` did not include snapshot class-entry warnings. The fix is committed in `54afdec3350bd31cf0ca8f5df63017f45a0e8af0`; the accompanying Slice 3 design documentation is committed in `0c22c684f984f20e8404f91cadcf65dcbd75818b`. Current branch tip after these commits: `0c22c684f984f20e8404f91cadcf65dcbd75818b`. User test gate for this fix is pending. Do not generate the Symbiote Schema 1.3 report before it passes.

## CURRENT CONTINUATION UPDATE — 2026-11-14 — ClassRemapper structural/annotation audit batch committed

### Verified test gate immediately before this batch

The user confirmed the Universal worktree was at the expected prior tip 43f6db7726c3e023b331d714cbc3ccb6452e7564 and ran:

    :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
    BUILD SUCCESSFUL in 52s
    14 actionable tasks: 14 executed

This validates the nested oversized-class warning-summary fix. The console excerpt did not show a JUnit count. The PSReadLine 2.0.0 warning is unrelated. The checkout was already at the expected remote SHA when the guard reported a stale local expected SHA; the subsequent fast-forward-only pull correctly reported Already up to date.

### New untested code/test batch on the Universal branch

Repository: kentvillamor08/Legacy-Connector-1.20.1
Branch: feat/forge1201-preflight-analyzer
The annotation fixture lifecycle correction is `3c01dc6d6361d7140324265d30df8f1eb5880a6f`; a final assertion that annotation rows retain the supplied nested archive path and class-entry path was added in `ba52f6ae643b2720655450551c480ebfa2edf094`.

Key source changes:

- Forge1201ApiUsageAnalyzer now returns recording annotation visitors for class, field, record-component, method, parameter, type, instruction, try/catch, local-variable and annotation-default scopes.
- Nested annotation values and arrays are recursively traversed. ASM Type values in annotations become class-literal dynamic-reference rows with archive/class/caller provenance; nested enum type descriptors remain captured through the ClassRemapper's normal remapping callbacks.
- Instruction type-annotation values retain the preceding instruction ordinal. Class/member metadata annotations use ordinal -1.
- Dynamic-reference display output now handles class/member contexts that have no method name/descriptor.
- Dynamic-reference ordering now treats null caller-method/descriptor keys safely and deterministically.

New synthetic test:

- Distinct class references prove coverage from class superclass/interfaces/generic bounds; field and method descriptors/signatures; declared exceptions; try/catch types; stack-map frames; local-variable descriptors/signatures; NEW type instructions; multi-dimensional array descriptors; and record-component descriptors/signatures.
- Annotation contexts include class/class-type, field/field-type, method/method-return-type, parameter, instruction, try/catch, local-variable, record-component, and nested annotation/array/enum/class-literal values.
- The new test runs byte arrays through Inventory.scanNestedClass; it does not load or define the synthetic classes.

The latest source/test batch is NOT yet compile/test validated by the user. Do not claim these changes pass until the next forced test output is supplied.

### Exact next action

First pull the latest remote branch tip with repository, branch, current local HEAD and clean-worktree checks. The user's current local worktree was confirmed at 43f6db7726c3e023b331d714cbc3ccb6452e7564; the remote tip has moved after that checkpoint. Resolve the exact remote SHA using git ls-remote origin refs/heads/$branch, then use git pull --ff-only origin $branch and verify the pulled HEAD equals the SHA returned by ls-remote. Run:

    Push-Location $repo
    .\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
    $testExit = $LASTEXITCODE
    Pop-Location
    if ($testExit -ne 0) { throw "Slice 3 ClassRemapper structural/annotation test gate failed." }

Review the complete output. Do not generate the new Symbiote report before this test gate passes.

After a green gate: review/finish duplicate raw-ZIP-entry and deep nested ConstantDynamic/depth-limit tests; generate a new uniquely suffixed Symbiote Schema 1.3 JSON/text report (never overwrite Slice 2 run-3); inspect all nested/SRG/annotation evidence; then run the consolidated clean build. Runtime launch/gameplay compatibility remains unverified.

Keep Palladium + Alien Evolution paused and isolated from this Universal branch. Preserve the goal of a reusable, modpack-ready Forge 1.20.1-to-NeoForge 1.21.1 compatibility platform, not one-off mod-specific hacks.

The final annotation provenance assertion commit `ba52f6ae643b2720655450551c480ebfa2edf094` is part of the same untested ClassRemapper batch. The exact pull target is the latest branch SHA recorded by the testbench handoff after this plan update.

## CURRENT CONTINUATION UPDATE — 2026-11-14 — duplicate ZIP and ConstantDynamic boundary tests added

### Latest user-confirmed green gate

The user fast-forwarded the Universal checkout to `c330c332126ad43d5b487ad8101129b2e96d2be7` and ran `:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks`. Result: `BUILD SUCCESSFUL in 1m 7s`, 14 actionable tasks / 14 executed. This validates the ClassRemapper structural-reference and annotation-value batch. The console excerpt did not print a JUnit count. PSReadLine 2.0.0 warning is unrelated.

### Additional tests committed after that green gate (not yet run)

- `reportsDuplicateNestedClassEntryNamesAsAmbiguousAndSkipsAllCopies()` uses a hand-built raw ZIP with two local file records and matching central-directory records for the same `.class` name. `ZipOutputStream` is deliberately avoided because it rejects duplicate names on write. The assertions require discovered=2, scanned=0, skipped=2, a path-specific duplicate warning, and no references from the ambiguous bytecode.
- `recordsDeepConstantDynamicNestingAndExplicitlyStopsAtTheDepthLimit()` creates 17 nested `ConstantDynamic` nodes and expects 17 regular constant rows (depths 0-16), exactly one `VALUE_DEPTH_LIMIT` row for depth 17, and the full nested archive/class path plus the complete repeated `constant-argument-0` context chain.
- The existing deterministic fixture continues to cover ordinary ConstantDynamic bootstrap handles and argument recursion.
- Edge-case test commit: `b8eb18ea8cbe19bca2ac897a933f24ad795799ea`; exact recursion-depth assertion follow-up: `ba8428932da023f9c527a6b6eb0d6056626a88bb`.
- Design audit update: `419c46f5a2d964d8e374d3b4219069d5008e51f7` was the prior structural-audit note; a new duplicate/deep-ConstantDynamic test note has now been committed.

Latest expected branch head after the new design update must be resolved from GitHub before the user pulls. User's last confirmed local HEAD is `c330c332126ad43d5b487ad8101129b2e96d2be7`. First verify the clean worktree and exact branch/path, resolve the current remote SHA with `git ls-remote`, fast-forward only, verify the pulled SHA, then run:

    Push-Location $repo
    .\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
    $testExit = $LASTEXITCODE
    Pop-Location
    if ($testExit -ne 0) { throw "Slice 3 duplicate ZIP / ConstantDynamic depth test gate failed." }

Do not generate the Symbiote Schema 1.3 report until this newest forced gate passes.

### After the next green gate

1. Review the duplicate raw-ZIP and depth-17 evidence and confirm no skipped/ambiguous archive findings disappeared from per-archive summaries.
2. Inspect the testbench `archive/preflight/README.md` for the next unused run suffix.
3. Generate Symbiote Schema 1.3 JSON/text using new unique filenames, preserving Slice 2 run-3.
4. Parse and review outer/nested scan counts, provenance, annotation/dynamic reference rows, mapped/unmapped SRG rows and warnings.
5. Run the consolidated clean build. Actual NeoForge runtime launch/gameplay remains unverified.

Keep Palladium + Alien Evolution paused and isolated. Maintain the universal, reusable, modpack-ready connector vision.
## CURRENT CONTINUATION UPDATE — 2026-11-14 — Slice 3 edge-case test gate passed; real report next

### User-confirmed validation

The user fast-forwarded the Universal branch `feat/forge1201-preflight-analyzer` to `b0d2b7b68915137aed44ef9153f4dfa304fa5819` and ran `:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks`.

    BUILD SUCCESSFUL in 31s
    14 actionable tasks: 14 executed

The test run included generated SRG-to-Mojang mappings (64,225 member entries). No JUnit test count was visible in the transcript. This is a complete green focused Slice 3 loader test gate for the implemented structural/annotation, duplicate ZIP, and ConstantDynamic depth regressions. It is not runtime/gameplay validation.

### First real Symbiote Schema 1.3 report — filename reserved, not yet generated

Testbench root listing and `archive/preflight/README.md` were checked. The next unused canonical first-run filenames are:

- `symbiote-1.1.3.jar-slice-3.json`
- `symbiote-1.1.3.jar-slice-3.txt`

Do not overwrite Slice 2 `symbiote-1.1.3.jar-slice-2-run-3.json` / `.txt` or legacy unsuffixed aliases.

Source: `C:\Users\Administrator\legacy-connector-testbench\symbiote-1.1.3.jar`
Expected SHA-256: `e40f48a8a9bf1f39ea173a7c163285df199b7d2cf1dfcbf3efa721fe7f2f942c`
Installed mods directory to preserve comparable provider evidence, provided it exists: `E:\Games\PolyMC\instances\Neoforge tester\.minecraft\mods`.
Schema/analyzer version: `1.3` / `1.3.0`; target Minecraft / NeoForge: `1.21.1` / `21.1.258`.

### Exact next action — generate and validate the real report

The root `preflight1201` Gradle task accepts `-PpreflightJar`, `-PpreflightModsDirs`, `-PpreflightJson`, and `-PpreflightText`. The source JAR is already in the testbench repository root. On the user's Windows system, set:

    $repo = 'F:\Program\intellij-idea-portable\Legacy-Connector-1.20.1'
    $testbench = 'C:\Users\Administrator\legacy-connector-testbench'
    $sourceJar = Join-Path $testbench 'symbiote-1.1.3.jar'
    $modsDir = 'E:\Games\PolyMC\instances\Neoforge tester\.minecraft\mods'
    $json = Join-Path $testbench 'symbiote-1.1.3.jar-slice-3.json'
    $text = Join-Path $testbench 'symbiote-1.1.3.jar-slice-3.txt'

Before running, verify source and mods paths exist, neither output path already exists, and source SHA-256 equals the expected value above. Then run:

    Push-Location $repo
    .\gradlew.bat preflight1201 --no-daemon --console=plain -PpreflightJar="$sourceJar" -PpreflightModsDirs="$modsDir" -PpreflightJson="$json" -PpreflightText="$text"
    $reportExit = $LASTEXITCODE
    Pop-Location
    if ($reportExit -ne 0) { throw "Symbiote Slice 3 report generation failed." }

After generation, verify both files exist and are nonempty; parse the JSON with `ConvertFrom-Json`; check `schemaVersion` is `1.3`, `analyzerVersion` is `1.3.0`, and `sourceSha256` matches the original. Report outer/nested class counts, dynamic-reference count, normalized SRG row count, per-archive summaries and warnings. Share the command output and upload the two files into the testbench root so they can be inspected before the archive README is updated.

If generation fails after creating either output, do not overwrite that path on a retry; report the failure and use a fresh run suffix such as `symbiote-1.1.3.jar-slice-3-run-2.json` / `.txt` after we diagnose it.

### After report inspection

Update `archive/preflight/README.md` with report schema/analyzer versions, input SHA, source commit, totals and output Git blob SHAs. Then run the consolidated clean build. Slice 4 Mixin semantic analysis and actual NeoForge launch/gameplay verification remain future gates.

Keep Palladium + Alien Evolution paused and isolated. Preserve the universal, reusable, modpack-ready compatibility platform goal.

## CURRENT CONTINUATION UPDATE — 2026-11-14 — Symbiote Schema 1.3 report generated and inspected

### Report generation and provenance

The user fast-forwarded branch `feat/forge1201-preflight-analyzer` from the tested implementation checkpoint `b0d2b7b68915137aed44ef9153fdfa304fa5819` to documentation-only tip `340805e8a1bb93bf1994645981c3246fd46298ea`, with the expected branch, clean-worktree, and remote-SHA guards passing. The user then ran the root `preflight1201` task against the original Symbiote source JAR and installed NeoForge mods directory:

```text
BUILD SUCCESSFUL in 10s
12 actionable tasks: 1 executed, 11 up-to-date
```

The resulting JSON parsed successfully. Schema/analyzer are `1.3 / 1.3.0`; source SHA-256 is the expected `e40f48a8a9bf1f39ea173a7c163285df199b7d2cf1dfcbf3efa721fe7f2f942c`; target is Minecraft `1.21.1` / NeoForge `21.1.258`. The exact root report blobs were inspected and archived under `archive/preflight/slice-3/` in the testbench. This report is generated from the implementation already covered by the forced test gate; only documentation commits separate `b0d2b7b...` from report checkout `340805e...`.

### Real report findings

- Source JAR: 515 entries; 278 outer class entries discovered and 278 scanned; 0 source-nested archive class entries and 0 class-scan warnings. The source JAR has no Jar-in-Jar paths. The 11 archive-graph records are the installed-mod inventory (7 top-level installed archives, 3 nested archives under KubeJS, and the Symbiote source root); installed provider JAR bytecode is intentionally not part of the source-mod bytecode scan. Do not misread zero nested-class entries as failure to scan the 278 source classes or as a count of Java inner classes.
- Archive graph: 9 provider records (8 installed NeoForge providers plus Symbiote), 17 dependency edges, and 5 target Minecraft range findings (4 `MATCH`, 1 `OUTSIDE_RANGE`). Symbiote's declared Minecraft range `[1.20.1,1.21)` excludes target `1.21.1`; metadata-range translation/review is required. One archive-graph warning records this.
- One metadata warning remains: `mods.toml` line 10 starts a multiline string that the scalar metadata inspector does not parse.
- API type classification: 443 types total — 322 `NOT_FORGE_API`, 78 `PACKAGE_RELOCATION_CANDIDATE`, 28 `MANUAL_ADAPTER_REQUIRED`, and 15 `EXPLICIT_TYPE_REPLACEMENT`.
- The explicit replacements include Forge `ForgeConfigSpec` to NeoForge `ModConfigSpec`, `IForgeMenuType` to `IMenuTypeExtension`, the Legacy Connector compatibility package for the Forge SimpleChannel/network family, and `RegistryObject` to `Supplier`.
- The 28 manual-adapter types cluster around config-screen registration, layered GUI/overlay APIs, tick and damage-event semantic changes, side separation and FML loading context, menu opening, event-subscriber registration, and registries. A package relocation candidate is not structural target verification.
- Member inventory: 1,110 method references, 352 field references, 5,369 method call-sites, 1,196 field call-sites.
- Dynamic/bootstrap inventory: 1,156 records (813 `INVOKEDYNAMIC_SITE`, 282 `METHOD_TYPE`, 56 `CLASS_LITERAL`, 5 `METHOD_HANDLE`).
- All 1,076 normalized source SRG member references are `MAPPED` to Minecraft 1.20.1 Mojang names; none remain unmapped.
- No Mixin config candidates, no Jar-in-Jar source paths, and no coremod class were discovered. Mixin target/handler/slice/refmap/plugin validation remains explicitly outside Slice 3.

The JSON and text reports agree on the parsed schema/source metadata, archive/provider/dependency counts, class totals, API/member/call-site totals, dynamic-reference count, normalized SRG count, and warning counts. The report's limitations remain binding: provider bytecode, target class/member compatibility, Mixin semantics, translated-resource parity, and runtime/gameplay behavior are not proven.

### Exact next action — consolidated clean build

At the user's Universal checkout, first verify the exact branch `feat/forge1201-preflight-analyzer`, HEAD `340805e8a1bb93bf1994645981c3246fd46298ea`, and a clean worktree. Then run the previously established full build gate:

```powershell
$repo = 'F:\Program\intellij-idea-portable\Legacy-Connector-1.20.1'
$expectedHead = '340805e8a1bb93bf1994645981c3246fd46298ea'

if ((git -C $repo branch --show-current).Trim() -ne 'feat/forge1201-preflight-analyzer') {
    throw 'Wrong Universal branch.'
}
if ((git -C $repo rev-parse HEAD).Trim() -ne $expectedHead) {
    throw 'Unexpected Universal HEAD.'
}
$changes = @(git -C $repo status --porcelain)
if ($changes.Count -gt 0) {
    $changes
    throw 'Universal worktree is not clean.'
}

Push-Location $repo
.\gradlew.bat :clean :loader:clean :testmods:clean :build :loader:build :testmods:assemble --no-daemon --console=plain
$buildExit = $LASTEXITCODE
Pop-Location

if ($buildExit -ne 0) {
    throw 'Slice 3 consolidated clean build failed.'
}
```

The focused Slice 3 loader tests already passed; the consolidated build is the remaining build/assembly gate. After it passes, move to Slice 4 Mixin analysis as planned. A Minecraft client/server launch and feature-level gameplay test remain separate runtime gates. Keep Palladium + Alien Evolution paused and isolated.

## CURRENT CONTINUATION UPDATE — 2026-11-14 — Slice 3 consolidated clean build passed; Slice 4 Mixin design prepared

### User-confirmed consolidated build

After pulling the documentation checkpoint, the user ran:

```text
:clean :loader:clean :testmods:clean :build :loader:build :testmods:assemble --no-daemon --console=plain
BUILD SUCCESSFUL in 1m 23s
33 actionable tasks: 24 executed, 9 from cache
```

The successful log showed:
- `:loader:clean`, loader compile/package/assemble/build, root clean/build, test-mod clean/compile/legacy packaging/assemble all completed.
- `SRG -> Mojang 1.20.1: 64225 members`.
- Beta 1.7.3 mappings: 9,243 rows.
- Generated shim index: 5,258 classes.
- Legacy API stubs: 1,848.
- Legacy 1.7.10 test mod reobfuscation: 16 classes.
- `:loader:test` and root `:test` were `FROM-CACHE` in this consolidated build. The separate forced Slice 3 loader test gate had already passed at `b0d2b7b68915137aed44ef9153fdfa304fa5819` with 14/14 tasks executed.
- Gradle emitted the non-fatal deprecated-feature / Gradle 9 compatibility warning. No build failure occurred.
- The startup PSReadLine 2.0.0 warning is an IntelliJ PowerShell terminal warning, not a Gradle or project build error.

This completes Slice 3's planned focused test and consolidated build gates. It does not prove a NeoForge runtime launch or gameplay compatibility.

### Slice 4 Mixin design checkpoint

Created `docs/FORGE1201-PREFLIGHT-SLICE-4-MIXIN-DESIGN.md` at commit `f3b61703be8a8f3481e105491e42b8720fd6338a`. The design is based on source inspection of:
- `loader/src/main/java/org/kylepitcock/legacyconnector/loader/tools/Mixin1201Triage.java`
- `loader/src/main/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201PreflightAnalyzer.java`
- `loader/src/main/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201TargetApiVerifier.java`
- `loader/src/main/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201JarInspector.java`

Concrete existing gaps documented:
- CLI/text-only output and mutable static target/rename maps.
- Target index stores member names without descriptors.
- Config candidates that are malformed, missing, or whose mixin class is missing can be silently ignored.
- `@Mixin` target extraction reads only invisible annotations.
- Selector handling does not comprehensively verify descriptors, slices, `@At` details, or handler signatures.
- Refmaps are not parsed; plugin classes are not checked; plugin behavior must never be executed by static analysis.
- The Forge metadata inspector does not currently expose Mixin declarations; if supported declaration forms are added, the partial TOML parser must report unsupported/ambiguous forms explicitly.

The new design defines a per-invocation state-free analyzer API, immutable provenance-rich result records, bounded safe config/refmap/archive parsing, exact target descriptor/hierarchy checks, explicit unresolved/plugin-dependent/runtime states, synthetic ASM/ZIP regression fixtures, and a no-classloading/no-plugin-execution boundary. It also requires reusing/refactoring the existing target verifier and archive traversal instead of cloning those systems.

### Exact next action

Begin Slice 4 implementation from the design document:
1. Inspect/refactor a reusable target class/member index that can expose exact descriptors and hierarchy completeness from `Forge1201TargetApiVerifier`.
2. Introduce the state-free `Forge1201MixinAnalyzer` result API and config-level findings first; preserve `Mixin1201Triage` as a delegating CLI wrapper.
3. Add synthetic JSON/ZIP/ASM tests for declared/missing/malformed configs and mixin class/target resolution before adding the more complex selector checks.
4. Add descriptor-aware Shadow/Overwrite/Accessor/Invoker and injector/`@At`/`@Slice` checks with unsupported/dynamic cases explicitly unresolved.
5. Add bounded refmap/plugin resource checks without ever loading or executing plugin code.
6. Wire the result into `Forge1201PreflightAnalyzer.PreflightResult` and Schema 1.4 only after isolated analyzer tests are green. Update design/plan versioning and report schema together.
7. Run the forced loader test gate, then the consolidated clean build. Runtime client/server launch remains separate.

The current user local branch is at `3e1291494b6c10cc58b2c54be8978df972e6cd68`; resolve the latest remote branch SHA before the next pull because the Slice 4 design and subsequent documentation updates are documentation-only commits after that local checkpoint. The testbench continuation handoff remains the single source for the exact pull guard.

Keep Palladium + Alien Evolution paused and isolated. Preserve the universal, reusable, modpack-ready compatibility platform goal.



## CURRENT UPDATE — 2026-11-14 — Schema 1.4 Mixin preflight wiring committed; test gate pending

### Integration increment

The feature branch now includes the first unified preflight wiring for Slice 4:

- `Forge1201MixinAnalyzer.Result` now distinguishes `AVAILABLE` from `UNAVAILABLE_NO_TARGET_CLASSPATH` target-index state.
- When no target classpath is supplied, a mapped platform target is reported as `TARGET_INDEX_UNAVAILABLE`, not `TARGET_CLASS_MISSING`. Unmapped Minecraft targets remain `TARGET_REMAP_UNRESOLVED`; no identity mapping is assumed.
- The existing manifest `MixinConfigs` entries and outer-JAR resource path/content heuristic candidates are converted to analyzer declarations. If a path is found by both routes, both provenance values are retained deterministically.
- `Forge1201PreflightAnalyzer.PreflightResult` now embeds the immutable Mixin result. JSON serializes it as `mixinAnalysis`; text output prints config, mixin-class, target-selector, and target-index findings.
- Preflight schema/analyzer versions are now `1.4` / `1.4.0`.
- The existing CLI accepts `--target-classpath-file`; the path list is bounded to 4 MiB / 4,096 entries and each entry is validated as an existing file or directory.
- Root `preflight1201` obtains `sourceSets.main.compileClasspath`, declares it as task input, writes its resolved entries to the target-classpath list, and passes that list to the analyzer. The root build uses ModDevGradle 2.0.148 with `neoForge.version = project.neo_version`; ModDevGradle adds its compile dependencies to the source set compile classpath. The analyzer uses resource lookup only through a dedicated URLClassLoader and never calls `loadClass`, defines, initializes, or executes target classes.
- The class-mapping input remains deliberately empty in this increment. Forge/NeoForge types can use the reviewed Forge mapping catalog; Minecraft/Mojang targets stay unresolved until a versioned class mapping is supplied.
- `FORGE_METADATA` remains a reserved discovery-source value. The current Forge `mods.toml` parser is scalar/partial and does not currently expose Mixin config declarations; no unsupported TOML syntax is being reinterpreted as a declaration.

### Synthetic coverage added (not yet user-tested)

- The existing deterministic preflight fixture now asserts schema 1.4, manifest-plus-heuristic provenance merging, heuristic-only provenance, explicit missing mixin-class findings, and unavailable target-index status when the compatibility overload receives no target classpath.
- A synthetic integration test builds a manifest-declared mixin whose source annotation targets `net.minecraftforge.fml.ModList`, supplies a temporary target classpath containing `net/neoforged/fml/ModList.class`, and asserts that the reviewed Forge mapping resolves to a present NeoForge class.
- A Mixin analyzer test asserts that a mapped target with no target index is `TARGET_INDEX_UNAVAILABLE`, not a false missing-class finding.

### Commit batch and test boundary

The integration batch begins from the last user-synced documentation checkpoint `0d3d04a9dbf54f2cc37f2eda7d42380a443ea688` and contains these commits:

- `985aa79f5b3cf34f956dac1c4f6f75bd0928744e` — explicit target-index availability state.
- `115ab4f73a6c9bc2834564b6949cfe3021e05f3c` — missing-index regression test.
- `caa874d3dd134d94019ba8b84d5cb31ebcfa9bc5` — preflight Schema 1.4 and Mixin result integration.
- `7ce6917469aacf8896eebe7d44e2b3bb5bc69619` — Gradle target compile-classpath handoff.
- `79148fb5533bafe655c8cb7e802e284a83da9b7a` — preflight integration tests.
- `97d35ae727ecb63142bce9cdde714f4e4af98e6b` — scope the classpath-list file in the task argument provider.
- `d6c50831efd1d6b2bb6f1ec2b67d53925ab35d11` — clarify that only mapped Mixin target-class presence is checked in this increment; general API member verification remains future work.
- `777b2059565fa1d1f74a1d5c806557c7973c6413` — correct the Mixin analyzer class-level scope comment now that unified preflight consumes its result.

Latest verified feature-branch HEAD at this update: `777b2059565fa1d1f74a1d5c806557c7973c6413`. The implementation compare against `0d3d04a...` initially showed six commits ahead and zero behind; two documentation/scope-correction commits followed. The branch is still a fast-forward from the user's last confirmed local base.

The user's last confirmed green loader test remains at `90daa1ee78d2de73ff44e27ffd01ba6e126858f4` (`BUILD SUCCESSFUL in 57s`, 14 actionable tasks executed). **The Schema 1.4 integration and new regression tests have not yet been compiled or run in the user's Gradle environment. Do not claim this increment passes until the next forced test output is reviewed.**

### Exact next action

Guard the local checkout at branch `feat/forge1201-preflight-analyzer`, expected local HEAD `0d3d04a9dbf54f2cc37f2eda7d42380a443ea688`, and a clean worktree. Verify the remote tip is exactly `97d35ae727ecb63142bce9cdde714f4e4af98e6b`; fast-forward only; verify the resulting HEAD; then run:

```powershell
.gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
```

If compilation or tests fail, fix the concrete issue in a new commit and rerun only after refreshing the expected remote SHA. If green, run the consolidated clean build and then continue with descriptor-aware `@Shadow`, `@Overwrite`, `@Accessor`, `@Invoker`, injector, `@At`, and `@Slice` findings. Source-nested config/class analysis must reuse `Forge1201ArchiveGraph`; do not create another ZIP traversal. Keep plugin code unexecuted and preserve unresolved states.


---

## 2026-11-14 — Schema 1.4 validation checkpoint: full clean build passed

The user validated the integrated Schema 1.4 Mixin preflight implementation on feature branch `feat/forge1201-preflight-analyzer`, source HEAD `afdf5c0b8b4410d0deb401683e60b0255843c58d`.

### Forced loader tests

Command:

```powershell
.\gradlew.bat :loader:test --no-daemon --console=plain --rerun-tasks
```

Result: **BUILD SUCCESSFUL in 45s; 13 actionable tasks: 13 executed.** Loader main and test sources compiled, and the complete loader test task ran successfully, including the Schema 1.4 Mixin integration regressions.

### Consolidated clean build

Command:

```powershell
.\gradlew.bat clean build --no-daemon --console=plain --rerun-tasks
```

Result: **BUILD SUCCESSFUL in 2m 34s; 33 actionable tasks: 33 executed.** This run included root, loader, and testmods clean/build tasks; loader and root tests; runtime compilation; shim index generation (5,258 classes); legacy 1.7.10 API stub generation (1,848 stubs); legacy 1.7.10 fixture reobfuscation (16 classes); and `bundleJar` packaging. Java/Gradle deprecation warnings remained non-fatal.

The initial `:loader:clean` Windows file-lock/concurrent-writer issue did not recur in the successful consolidated clean build.

### Evidence boundary

This proves the current code compiles, its automated unit tests pass, and the configured packaging tasks succeed at the stated commit. It does not prove that a real Forge 1.20.1 mod works in a NeoForge 1.21.1 runtime or modpack. The current Schema 1.4 Mixin analysis is still limited to outer-archive config discovery, mixin class/target scanning, provenance preservation, and resource-only presence checks for mapped targets against the supplied target compile classpath. It does not yet implement complete member/injector semantics.

### Next implementation increment

Proceed with descriptor-aware Mixin member annotation inventory:

1. Inspect the existing `Forge1201MixinAnalyzer` bytecode visitor, model types, output serialization, and tests.
2. Add exact descriptor-aware structural findings for `@Shadow`, `@Overwrite`, `@Accessor`, and `@Invoker`. Preserve full JVM owner/name/descriptor identity, aliases/prefix/value metadata, and explicit unresolved mapping/presence states. Never assume identity mappings.
3. Add deterministic synthetic classfile tests for annotation forms, overloads, descriptors and mismatches.
4. Follow with injector handler descriptors/selectors, `@At`, `@Slice`, refmap interpretation, dynamic plugin declarations (without executing plugin code), and nested config/class discovery using the existing `Forge1201ArchiveGraph`. Do not introduce another ZIP walker.
5. Re-run the forced loader tests after source changes and the consolidated clean build at the next integration checkpoint.



---

## 2026-11-14 — Schema 1.5 descriptor-aware Mixin member inventory committed (tests pending)

The prior Schema 1.4 integration has been user-validated:
- Forced `:loader:test --no-daemon --console=plain --rerun-tasks`: **BUILD SUCCESSFUL in 45s; 13/13 tasks** at `afdf5c0b8b4410d0deb401683e60b0255843c58d`.
- Full `clean build --no-daemon --console=plain --rerun-tasks`: **BUILD SUCCESSFUL in 2m 34s; 33/33 tasks** at the same user-tested base commit.

A new untested increment is now committed on `feat/forge1201-preflight-analyzer`, latest branch HEAD (including the updated design/plan documents) is `c91c69e48f9c0f9ecd9d2735afc18adfb0c3afb3`; the member-analysis source/test changes were committed through `e60447ed3769077ebf391d21d1a724cda4a64d40`.

### New source-side structural findings

`Forge1201MixinAnalyzer.Result.memberAnnotations` now contains immutable findings for `@Shadow`, `@Overwrite`, `@Accessor`, and `@Invoker` annotations on visible/invisible fields and methods. Findings retain exact source member JVM descriptors, source targets, archive/config/section/class provenance, element kind, prefix, aliases, explicit value, Overwrite constraints, effective remap state, target member name candidates, operation shape, warnings, and explicit status.

The effective member `remap` default comes from class-level `@Mixin(remap)` and can be overridden at member level. Shadow prefixes strip methods only; explicitly prefixed shadow fields are retained and marked as invalid annotation values. Alias use is checked against the private-member rule. Accessor/Invoker name inference requires the documented prefix followed by an uppercase character; constructor targets require explicit constructor notation/name. Plugins remain unexecuted and can mark member findings plugin-dependent.

The preflight schema is now `1.5` / analyzer `1.5.0`. The JSON adds `mixinAnalysis.memberAnnotations` and text output includes complete member annotation data and warnings. The implementation does not claim target member presence or target descriptor compatibility; no complete versioned 1.20.1 → 1.21.1 member map is supplied.

### Regression coverage added — not yet executed

Synthetic ASM tests now cover Shadow field/method and prefix/alias rules, Overwrite descriptors/aliases/constraints/remap, Accessor getter/setter/inference and invalid shapes, Invoker inferred method and explicit constructor, class-level remap inheritance/member override, plugin-dependent findings without plugin execution, and Schema 1.5 JSON/text serialization.

**Important:** none of these new Schema 1.5 source/test changes have yet been compiled or executed by Gradle. The only verified green checkpoint remains the Schema 1.4 build at `afdf5c0b8b4410d0deb401683e60b0255843c58d`.

### Exact next validation action

The user must fast-forward the clean local checkout from its last tested base `afdf5c0b8b4410d0deb401683e60b0255843c58d` to the verified remote feature branch head `e60447ed3769077ebf391d21d1a724cda4a64d40`, with branch/HEAD/clean-worktree/remote-SHA guards. Then run:

```powershell
.\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
```

The forced loader gate is the first validation. If it fails, diagnose and fix the actual compiler/test failure before any broader build. If it passes, update this plan and handoff with the new checkpoint; the next appropriate integration checkpoint is a full `clean build`. Do not claim Schema 1.5 is validated until the user supplies a successful Gradle output.



### Follow-up contract corrections — latest code/test commit `a118a81b69f6d94f81d02840c258ab26a84e7249`

Before handing this increment to the user, the Mixin inventory was tightened against the upstream annotation contracts:

- `@Accessor` and `@Invoker` methods must be abstract; concrete methods are now marked `INVALID_ANNOTATION_VALUE`.
- The private-member alias restriction is applied to `@Shadow` aliases only. `@Overwrite` aliases remain captured and are not incorrectly marked invalid merely because the overwrite method is public.
- ASM-generated test mixin classes are abstract when they contain Accessor/Invoker methods. A separate concrete Accessor fixture tests the new invalid-value state.
- The prior documented behavior remains: class-level `@Mixin(remap)` is the default for member annotations, member override wins, Shadow prefixes apply to methods only, and no target member mapping/presence claim is made.

**No Gradle test has yet been run against this Schema 1.5 batch.** The last green user-run build is still the Schema 1.4 checkpoint `afdf5c0b8b4410d0deb401683e60b0255843c58d`. The exact current code/test head is `a118a81b69f6d94f81d02840c258ab26a84e7249`; after documentation updates, verify the final remote branch head before giving the guarded pull command.



### Slice 4 implementation checkpoint — core injector handler and selector inventory (2026-11-14)

- Schema 1.5 `@Shadow` / `@Overwrite` / `@Accessor` / `@Invoker` inventory passed the user's forced loader test gate at `dad7bed4fedeac65aa6f9c0a3a67cd439e169804`: `BUILD SUCCESSFUL in 55s`, 14 actionable tasks, all 14 executed. The preceding Schema 1.4 full clean build remains the last consolidated integration build.
- The new increment adds source-side inventory for six core Mixin 0.8.5 injector annotations, exact handler descriptors, string `method` selectors, and `@Desc` `target` selectors. It reuses the existing per-call config/class scan and does not load mixin classes.
- Selector parsing is deliberately bounded: common owner/name/method-descriptor forms are decomposed; wildcard/quantifier selectors and raw input are retained; chained selector extensions are marked unsupported. No target resolution or member-name mapping is inferred.
- `@At`/`@Slice` values remain future work; this checkpoint records only their counts. Handler signature compatibility against injector-specific target instructions is not claimed.
- Schema/analyzer versions advance to `1.6 / 1.6.0`. ASM tests cover all six injector kinds, descriptor and owner selectors, `@Desc`, wildcard, `@At`/`@Slice` counts, remap=false, malformed selector, and invalid field placement.
- **Validation status:** the injector increment is pending the user's forced `:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks` run. After it passes, continue with `@At` and `@Constant` inventory (including the `@ModifyConstant` selector payload); the consolidated clean build remains a later integration checkpoint.

---

## Slice 4 implementation checkpoint — Schema 1.7 source-side @At and @Constant inventory (2026-11-14)

### Prior validation confirmed by the user

The user ran the forced loader test gate at Schema 1.6 branch HEAD `b661313f41460321845f617b4b81684f314eb614`:

```powershell
.\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
```

Result: **BUILD SUCCESSFUL in 50s; 14 actionable tasks, all 14 executed.** The local HEAD and clean-worktree guards passed. This validates the Schema 1.6 handler/selector inventory tests, not runtime compatibility or the new Schema 1.7 changes below.

### Schema 1.7 source changes — pending local Gradle verification

The current source increment adds immutable per-injection-point `AtFinding` records beneath each `InjectorFinding`:

- Preserve `@At.value` and classify known built-in injection-point codes versus custom/unknown values.
- Preserve raw `target`, parse common owner/name forms, and validate exact method and field JVM descriptors without claiming target presence.
- Preserve nested `@At.desc` selectors through the existing exact source-side `@Desc` parser.
- Capture `opcode`, `ordinal`, `shift`, `by`, `slice` ID, `args`, `remap`, and `id`, including Mixin 0.8.5 annotation defaults.
- Unsupported selector chains, malformed target descriptors, malformed `@Desc`, and absent required `value` remain explicit statuses/warnings. Unknown/custom injection-point values are retained as custom/unknown rather than executed or guessed.

It also adds `ConstantFinding` records for every `@ModifyConstant.constant` entry, preserving:
- `nullValue`, `intValue`, `floatValue`, `longValue`, `doubleValue`, `stringValue`, and class literal descriptor.
- `ordinal`, `slice`, `expandZeroConditions`, and `log`.
- Explicit distinction between an absent discriminator array (Mixin's implicit match-by-handler-return-type behavior), a declared typed discriminator, multiple conflicting type discriminators, invalid enum values, and unsupported nested annotation types.

The preflight report schema/analyzer versions advance to **`1.7 / 1.7.0`**. JSON carries the structured nested records through the immutable injector findings; text output prints the At and Constant details. Limitations are updated to state that source-side capture is not target instruction resolution, handler stack validation, @Slice boundary interpretation, member mapping, or semantic refmap application.

### Regression tests added — pending user execution

The synthetic ASM fixture checks:
- `FIELD` @At with owner/name/field descriptor, GETFIELD opcode, ordinal, BY shift, negative by value, slice ID, named args, remap=false, and custom id.
- `INVOKE` with nested `@Desc` owner/name/method descriptor and min/max.
- `HEAD` with default opcode/ordinal/shift/by/remap and no target.
- Explicit int/float/long/double/string/class/null constant discriminators, including ordinal/slice/expand-zero/log data.
- No explicit @Constant discriminator, which must be represented as Mixin's handler-return-type default.
- A conflicting int+string discriminator state.

**No Schema 1.7 Gradle test or build has been run yet.** The user must first fast-forward from their last confirmed local/tested commit `b661313f41460321845f617b4b81684f314eb614` to the final remote branch head recorded by the next handoff update. Then run the forced loader test gate before any broader build. If it fails, fix the concrete failure and rerun from a refreshed exact remote SHA.

### Next bounded increment after Schema 1.7 tests pass

Implement `@Slice` boundary inventory using both `from` and `to` nested `@At` annotations. Preserve defaults and boundary provenance separately; do not infer instruction order or prove slice validity without target method bytecode and resolved selectors. Then proceed to semantic refmap interpretation using explicit versioned mapping inputs. Nested source-archive config/class discovery must continue to reuse `Forge1201ArchiveGraph`; no second ZIP walker. Runtime client/server tests remain a separate later gate.
### Schema 1.7 parser follow-up

A review correction preserves the valid owner-only `@At.target` selector form used by construction injection points. The parser no longer rejects the absent member name when an owner is present, and a synthetic ASM regression assertion covers this case. This source/test correction is included in the current Schema 1.7 validation batch; Gradle verification remains pending.
### Schema 1.7 test correction — owner-only @At fixture count

The first user-run Schema 1.7 test execution compiled successfully and completed 471 tests with one failure at `Forge1201MixinAnalyzerTest.java:350`. The failed assertion expected three `@At` entries, but the same synthetic fixture now contains four (`FIELD`, `INVOKE`, `HEAD`, and the added owner-only `NEW` target). This was a stale test expectation, not a production parser/compiler failure.

Fixed by changing the expected `atHandler.injectionPoints().size()` from `3` to `4` in commit `fbe5b9e5739561bd561819ef26f05224c1baed2d`. The user's next validation must rerun the forced loader test gate at that exact commit. Schema 1.7 remains **pending** until it passes.

---

## 2026-11-14 — Schema 1.7 validated; Schema 1.8 @Slice boundary inventory implemented (tests pending)

### Schema 1.7 user validation

The user verified the Universal branch at commit 267db731f7b16b30498f3402daf83bcb340849d1. Branch, exact local HEAD, clean-worktree, remote SHA, fast-forward, and post-pull guards passed. The user then ran:

```powershell
.\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
```

Result: **BUILD SUCCESSFUL in 39s; 14 actionable tasks, all 14 executed.** This confirms the Schema 1.7 loader test suite, including the corrected four-entry @At fixture assertion. It does not establish Mixin application or runtime compatibility in a real game.

### Schema 1.8 source increment

The analyzer now inventories each declared core-injector @Slice entry as a SliceFinding, preserving:

- Slice index, ID, and nested annotation descriptor.
- Separate from and to SliceBoundaryFinding records.
- Whether each boundary was explicitly declared or materialized from the Mixin 0.8.5 annotation default.
- The nested @At metadata using the existing source-side At parser, including value, target/descriptor, opcode, ordinal, shift/by, args, remap, id, warnings, and syntax states.
- Unsupported nested annotation types, wrong value types, and malformed nested @At data as explicit findings instead of silently discarding them.

When omitted, Mixin 0.8.5 defaults are represented as from=@At("HEAD") and to=@At("TAIL"). A captured boundary does not prove that its selector resolves, that the selected instructions exist, that from precedes to, or that the resulting slice is valid for the target method.

Schema/analyzer versions advance to 1.8 / 1.8.0. Unified JSON includes mixinAnalysis.injectors[].slices; the text report prints each slice and both boundary findings. Mixin plugin code remains unexecuted; nested source JAR configs/classes remain outside the current outer-archive analyzer scope.

Implementation commit: 0dc6ae5b7d4497e341ef2968040f08ef124bdb3a.

### Synthetic coverage added — pending user verification

- Explicit from/to @At values, owner/member/method descriptor, ordinal and shift.
- Omitted from/to defaults, including explicit default provenance and HEAD/TAIL values.
- Unsupported nested annotation on a boundary, with preserved descriptor and an explicit review status.
- JSON and human-readable preflight report output containing the new slice inventory.

**Validation status: pending.** The Schema 1.8 source/test batch must pass the forced loader test gate before being marked green. Runtime and gameplay validation remain separate later gates.

### Exact next action

The user's last confirmed local Universal checkout is expected clean at 267db731f7b16b30498f3402daf83bcb340849d1. Verify branch, local HEAD, clean worktree, remote SHA, and then fast-forward exactly to the current feature-branch tip before running the forced loader gate. Do not run the full clean build as a substitute for this focused gate.


---

## 2026-11-14 — Schema 1.8 @Slice boundary inventory passed the user loader test gate

The user confirmed local branch `feat/forge1201-preflight-analyzer` at commit `4b4f9ce437671f08eafd3f9cf4a18e34a3d24284`. Branch, local HEAD, clean-worktree, remote SHA, fast-forward, post-pull HEAD, and final clean-worktree guards passed.

The user then ran:

```powershell
.\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
```

Result: **BUILD SUCCESSFUL in 33s; 14 actionable tasks, all 14 executed.** This confirms the Schema 1.8 loader test suite, including explicit @Slice from/to boundary parsing, default HEAD/TAIL provenance, unsupported nested annotation status, and unified JSON/text output assertions.

The earlier Schema 1.7 green checkpoint remains confirmed at `267db731f7b16b30498f3402daf83bcb340849d1` (BUILD SUCCESSFUL in 39s; 14/14 tasks).

### Evidence boundary

Schema 1.8 still inventories declaration metadata. It does not inspect target method instructions, establish that a from boundary precedes a to boundary, validate slice applicability, resolve selectors through a semantic refmap, or prove Mixin injection success. Mixin plugin code remains unexecuted. Nested source-JAR config/class discovery remains future work and must reuse `Forge1201ArchiveGraph`, never a second ZIP walker.

### Next bounded task — semantic refmap contract and design

Before implementing remapping, research and document the exact Mixin 0.8.5 refmap JSON shape and remapper lookup semantics relevant to Forge 1.20.1. Separate:
1. Raw refmap resource discovery and bounded JSON parsing.
2. Source namespace/context and target namespace identification.
3. Selector lookup behavior, including context-specific mappings and missing/ambiguous entries.
4. Explicit, versioned mapping inputs and provenance for any namespace conversion.
5. States for absent refmap, malformed JSON, unsupported structure, missing mapping, ambiguous mapping, plugin-dependent runtime behavior, and successfully interpreted source mapping data.

Do not infer a mapping from a same-name selector. Do not rewrite selectors or claim target resolution until the applicable source/target namespaces and reviewed mapping set are explicit. Keep plugin execution disabled. Semantic refmap interpretation must remain separate from target member/instruction resolution and runtime confirmation.


---

## 2026-11-14 — Schema 1.9 Mixin 0.8.5 refmap research and design completed

The Mixin 0.8.5 source audit is documented in docs/FORGE1201-PREFLIGHT-SCHEMA-1.9-REFMAP-DESIGN.md. It pins the exact upstream tag and key source blobs for ReferenceMapper, RemappingReferenceMapper, MixinConfig, MixinEnvironment, and annotation-processor reference generation.

### Material findings

- Refmap JSON contains base mappings and context-keyed data tables, each mapping owner mixin internal name to exact string reference keys and mapped string values.
- Context labels are annotation-processor obfuscation-type keys, not complete namespace declarations. Default MCP service keys include searge and notch; other services can add their own.
- Runtime lookup selects data[context] when that whole context table exists; only an absent context table falls back to base mappings. A missing owner/reference within an existing context does not fall back per entry.
- The owner key is the mixin class internal name. The AP stores whitespace-stripped reference keys.
- ReferenceMapper returns the original reference when no entry exists; absent or unreadable refmaps yield the default pass-through mapper.
- Mixin config refmap is an exact resource path. Omission can select the mixin.refmap.json default or a plugin-supplied path. refmapWrapper loads custom mapper code; neither it nor plugin code may be executed in preflight.
- The optional runtime SRG wrapper is a distinct stage from the refmap JSON tables and must not be approximated without explicit runtime options and a pinned mapping artifact.
- Injector method[], @At.target, and the @At("NEW") class argument are the relevant AP-emitted string-reference paths. @Desc target annotations must not be fabricated into refmap string lookups.

### Current implementation gap

The current analyzer stores refmapPath and a coarse JSON-resource status, but does not parse the mappings/data tables or preserve refmapWrapper. It currently treats an omitted refmap as not declared, while Mixin 0.8.5 has a default/plugin resource-resolution path. The new design specifies how to correct these gaps without making dynamic behavior appear static.

### Next implementation task

Schema 1.9A has passed the user's forced loader test gate at `a59fdac24ab848e31152901b9a4f4f3a5db5413d`. Schema 1.9B adds owner-scoped exact refmap lookup, explicit runtime-context input, exact Mixin 0.8.5 context fallback behavior, and synthetic regression coverage. Schema 1.9B awaits the user's forced loader gate; namespace translation remains a separate later stage requiring explicit mapping profiles.


---

## 2026-11-14 — Schema 1.9A refmap resource/table inventory implemented (validation pending)

### Source changes in the Schema 1.9A branch change set

- Forge1201MixinAnalyzer.Result now carries immutable refmap resource, table, and entry findings.
- Config selection distinguishes an explicit refmap path, Mixin's mixin.refmap.json default candidate, and a plugin-dependent default candidate when a plugin exists but no path is declared. The default path remains only a candidate when plugin code may provide a different one.
- refmapWrapper is retained as inert metadata and never loaded, instantiated, initialized, or executed.
- The analyzer parses base mappings and opaque data[context] tables with exact owner/reference/value strings. It preserves raw keys and values and does not classify all mapped strings as methods.
- Strict UTF-8 / JSON validation rejects duplicate object member names before Gson parsing. Wrong root/map/value types remain explicit unsupported-shape results. Unknown top-level fields are reported by name.
- Refmap reads reuse the existing outer-archive entry index and bounded readBounded reader. Resource/path/aggregate/context/owner/entry and 128-level JSON nesting caps are surfaced as explicit statuses. SHA-256, config/origin provenance, ZIP-declared size, compressed size, and inflated bytes are recorded where available.
- JSON serialization uses the existing result record. Text output prints resource/table/entry inventory. The unified report version advances to 1.9 / 1.9.0.
- Synthetic tests cover base + multiple context tables, shared path across configs, explicit wrapper metadata, default and plugin-dependent candidates, duplicate JSON keys, unsupported shapes/non-string values, per-resource and aggregate byte caps, JSON nesting depth, deterministic results, and context limit behavior. Unified preflight tests assert schema version, JSON/text exposure, and a real synthetic refmap inventory.

### Scope and validation boundary

This is **Schema 1.9A only**. It does not add selector lookup correlation, AP whitespace-normalization matching, explicit runtime context selection, context fallback simulation, namespace conversion, target member checks, or runtime Mixin execution. Those remain Schema 1.9B/later mapping work.

The code and tests are not yet locally compiled or executed. The last user-confirmed green test checkpoint remains Schema 1.8 at 4b4f9ce437671f08eafd3f9cf4a18e34a3d24284.

### Exact next validation action

After this batch is committed and the guarded local fast-forward is verified, run:

```powershell
.\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
```

Do not mark Schema 1.9A green or begin 1.9B until this full forced loader test gate passes. If it fails, diagnose the actual compiler/test failure, make the smallest source correction, and rerun the same forced gate from the updated exact commit. Runtime client/server and modpack testing remain separate later gates.


---

## 2026-11-14 — Schema 1.9A green; Schema 1.9B implementation prepared for validation

### Confirmed user-run checkpoint
- Local branch `feat/forge1201-preflight-analyzer` fast-forwarded from `4b4f9ce437671f08eafd3f9cf4a18e34a3d24284` to `a59fdac24ab848e31152901b9a4f4f3a5db5413d` after branch/head/worktree/remote-tip guards passed.
- The user ran `:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks`: `BUILD SUCCESSFUL in 50s`, all 14 actionable tasks executed.
- This validates Schema 1.9A compilation and synthetic loader tests only; it is not a runtime Mixin or mod-compatibility result.

### Schema 1.9B implementation scope
- Source mixin internal owners are preserved beside display names and used for exact lookup.
- New immutable lookup/candidate findings correlate only Mixin 0.8.5 AP string-reference paths; `@Desc` keys are not fabricated.
- Raw source text and the AP whitespace-normalized candidate are preserved; serialized refmap keys and owner are compared exactly.
- An absent context table selects base; a present context table with a missing key passes through without base fallback. No context means candidates are listed without automatic selection.
- Unified preflight accepts `--refmap-context` and Gradle `-PpreflightRefmapContext`.
- `@At.remap` inherits injector remap unless explicitly overridden, including slice boundaries.
- Plugins/wrappers remain inert and may leave resource selection dynamic. Namespace conversion and target/member/instruction verification are not included.

### Validation state
**Schema 1.9B has not yet passed a user-run Gradle gate.** Do not begin namespace integration until the updated source branch passes the forced loader test command. Real mod/client/server and modpack testing remain separate later gates.


---

## 2026-11-14 — Schema 1.9B owner-scoped exact refmap correlation passed

### User-confirmed forced loader gate

The user fast-forwarded `feat/forge1201-preflight-analyzer` from `6cf92e0017568a0ecb85808c537a102a06b0f9c5` to `242744d1baf94dc1c523b3a873a6463017184081` after branch, exact-HEAD, clean-worktree, fetched-tip, published-tip, and fast-forward guards passed.

The user then ran:

```powershell
.\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
```

Result: **BUILD SUCCESSFUL in 45s; 14 actionable tasks, all 14 executed.** Main sources and test sources compiled; the forced loader test task completed. This is the user-confirmed Schema 1.9B green gate.

### Failure and correction history

The first Schema 1.9B attempt at `6cf92e0017568a0ecb85808c537a102a06b0f9c5` failed in `:loader:compileJava` with `reached end of file while parsing`. The generated source insertion duplicated the `ConfigDeclaration` record declaration, leaving the analyzer's braces unbalanced. Commit `242744d1baf94dc1c523b3a873a6463017184081` removed that duplicate; the user's successful compile/test output confirms the correction.

The same corrective commit accidentally rewrote every line ending in `build.gradle`, creating a misleading 587-addition/587-deletion diff. Follow-up commit `af12df84f62850c49a5a111515e16e76561d8e6c` restores `build.gradle` to the exact blob from the already-tested parent `6cf92e0017568a0ecb85808c537a102a06b0f9c5`. A comparison from that parent to `af12df84f62850c49a5a111515e16e76561d8e6c` contains no `build.gradle` delta; the only code delta is the two-line analyzer declaration correction. The cleanup changes file bytes/line endings only and preserves the logical `preflightRefmapContext` property. The user's Gradle run was at `242744...`; no separate run at the cleanup commit is recorded.

### Scope and validation boundary

Schema 1.9A resource/table inventory and Schema 1.9B exact owner-scoped lookup correlation are now implemented and user-tested. Coverage includes Mixin 0.8.5 annotation-processor string-key paths, exact mixin internal owner matching, AP whitespace normalization, explicit identity versus missing-entry pass-through, explicit context handling, absent-context base fallback, present-context per-key pass-through, and inherited/overridden `@At.remap`.

This does **not** establish:
- source/target namespace identity or cross-version mapping;
- target selector/member/descriptor/instruction existence;
- Mixin plugin or custom wrapper behavior;
- runtime Mixin application, real mod launch, gameplay, or modpack compatibility.

The PSReadLine 2.0.0 startup warning in the user's PowerShell output is unrelated to the Gradle result.

### Next bounded engineering task — Schema 1.10 mapping-profile design

Do not begin selector rewriting. First produce a design/research contract for explicit mapping profiles that can connect a Mixin refmap output to the NeoForge 1.21.1 target environment without guessing.

Audit the existing source-version SRG-to-Mojang normalization, mapping catalog, generated mapping artifact, and target class/member index. Define exact provenance for each edge in a mapping chain: profile ID, direction, source/target MC versions, source/target namespaces, mapping provider and format, exact artifact identity/digest, and class/member/descriptor coverage. Keep opaque Mixin context keys distinct from namespace IDs. Establish deterministic states for missing, partial, ambiguous, unsupported, or version-mismatched mapping inputs. Reuse the existing target index; no second remapper or hierarchy cache.

The next implementation must remain blocked until the design establishes a fully evidenced and pinned chain. Never infer cross-version identity from a familiar name or from a context label.

### Universal vision and workstream boundaries

Keep the project goal as reusable, evidence-driven, modpack-ready compatibility infrastructure for Forge 1.20.1-exclusive mods on NeoForge 1.21.1—not a one-off adapter. Static analyzer success is a prerequisite, not a mod compatibility verdict.

Palladium + Alien Evolution remain paused and isolated in their own workstream. Legends core followed by Legends: Superheroes remains a later dependency-ordered candidate pair. Do not mix either into the Universal analyzer branch.


---

## 2026-11-14 — Schema 1.10 mapping-profile contract designed

### Design document and source checkpoint

- Design: `docs/FORGE1201-PREFLIGHT-SCHEMA-1.10-MAPPING-PROFILE-DESIGN.md`
- Design commit: `a73cf0198ef7309cbd5d1eecf634a4d4bfbbd7f4`
- Branch: `feat/forge1201-preflight-analyzer`
- Schema 1.9A/1.9B user-run green code checkpoint: `242744d1baf94dc1c523b3a873a6463017184081` (`BUILD SUCCESSFUL in 45s; 14/14 forced loader tasks executed).

### Verified repository audit

The design audits the current `Forge1201MappingCatalog`, `Forge1201SrgNameMappings`, generated `SrgMojangTable`, loader mapping-generation task, and `Forge1201TargetApiVerifier.TargetIndex`.

The source mapping inputs currently declared by the build are MCPConfig `1.20.1-20230612.114412` / `config/joined.tsrg`, combined with Mojang's content-addressed 1.20.1 client mappings file. The user's last successful test log generated 64,225 SRG → Mojang member-name rows. The generated table is useful source-version member-name normalization, but is not the missing 1.20.1 → 1.21.1 mapping edge and is not an owner/descriptor-complete cross-version selector map.

The target build pins Minecraft 1.21.1, NeoForge 21.1.258, ModDevGradle 2.0.148, and Parchment 2024.11.17. NeoForge's 1.21.1 MDK uses Mojang's official method/field names by default; Parchment adds parameter names and Javadocs. The exact resolved target classpath must still be captured for each static verification.

### Core design decisions

- Keep Schema 1.9 refmap lookup separate from namespace mapping.
- Treat context keys such as `searge` as opaque until a versioned profile explicitly binds them to a namespace with evidence.
- Represent mapping as a directed graph of version-bound edges with direction, namespace IDs, symbol coverage, parser/provider versions, exact artifact identity, SHA-256, licensing, and provenance.
- Transform selectors as typed structures, not blanket text replacements. Owner classes, member names, descriptor object types, and arrays need declared coverage.
- Keep Minecraft, Forge API, NeoForge API, connector shim, and mod-owned symbols in separate domains.
- Do not infer cross-version identity from same-name strings.
- Reuse `Forge1201TargetApiVerifier.TargetIndex` after a complete target selector candidate exists; do not create a second hierarchy cache.
- Keep Mixin runtime remapping, plugins, and custom wrappers separate and unexecuted unless exact runtime inputs are supplied.

### Important gap and boundary

There is **no complete, reviewed Minecraft 1.20.1 official-Mojang → 1.21.1 official-Mojang symbol edge** in the current repository. The Forge API package/adapter catalog is a different migration domain. Selector rewriting must remain blocked until that cross-version edge (or a bounded, versioned subset of it) has artifact-level evidence.

### Next bounded implementation — Schema 1.10A

Implement profile schema parsing and validation only:
- Explicit source/target versions and namespace IDs.
- Explicit refmap context-to-namespace bindings.
- Directed edge graph, connectivity, direction, coverage, and ambiguity checks.
- Artifact identity, checksum, and license/provenance records.
- Deterministic incomplete/missing/mismatched states.
- Synthetic valid/invalid profile tests.

**Do not yet apply mappings to selector strings.** Schema 1.10B may validate the pinned source edge and its actual coverage. Cross-version mapping and target selector checks remain later gates.


---

## 2026-11-14 — Schema 1.10A mapping-profile validator implemented; user test gate pending

### Scope and implementation

Design: `docs/FORGE1201-PREFLIGHT-SCHEMA-1.10-MAPPING-PROFILE-DESIGN.md`.

New source:
- `loader/src/main/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201MappingProfileValidator.java`

New synthetic tests:
- `loader/src/test/java/org/kylepitcock/legacyconnector/loader/tools/forge1201/Forge1201MappingProfileValidatorTest.java`

Unified preflight:
- `Forge1201PreflightAnalyzer` advances report schema/analyzer to `1.10 / 1.10.0`.
- `PreflightResult.mappingProfile` carries immutable profile, artifact, edge, and context-path findings into JSON.
- Human-readable output prints profile status, exact profile digest, namespaces/versions, artifact SHA-256/upstream checksum/license states, directed/versioned edges, context candidate paths, and warnings/errors.
- Optional CLI: `--mapping-profile <path>`.
- Optional Gradle property: `-PpreflightMappingProfile=<path>`. The root `build.gradle` change was inserted while preserving the file's original CRCRLF line endings; its commit patch contains seven additions and no deletions.

### Validation implemented

- Strict UTF-8 and strict JSON parsing, duplicate object member rejection, 1 MiB profile cap, 64 JSON nesting levels.
- Bounds: 64 refmap context bindings, 256 edges, 512 artifacts, 512 MiB per materialized artifact, and 100,000 edge examinations per context graph search.
- Explicit context and separate base-table namespace bindings, each requiring evidence.
- Directed `FORWARD` edges with per-edge source/target Minecraft versions and namespace endpoints; paths must be version-contiguous from the source profile version to the target profile version.
- Complete selector scope requires class names, method names, field names, and descriptors.
- Unique edge/artifact IDs, exact artifact coordinate-or-URL declaration, SHA-256 matching, optional upstream SHA-1/SHA-256 matching, profile-relative forward-slash artifact paths, and symlink escape protection.
- Artifact and profile-level license metadata/evidence; license review must be required and artifact state must be reviewed.
- Explicit identity edges require evidence, full selector scope, and unchanged Minecraft version. They are recorded only; the validator never applies them.
- Enabled runtime refmap remapping is blocking because Schema 1.10A does not execute or model that runtime stage.
- Path search reports no path, ambiguous path, or explicit search-budget exhaustion. No zero-hop or same-name identity is inferred.

### Synthetic regression cases

Tests cover a complete declared path, partial/missing path, multiple complete paths, per-edge Minecraft-version discontinuity, unsupported reverse direction, mapping artifact SHA-256 mismatch, upstream SHA-1 mismatch, pending license review, enabled-but-unmodeled runtime remapping, profile-level license-review opt-out, unsupported profile schema, duplicate JSON object members, unbound base table, absent profile, missing profile file, and unified JSON/text preflight integration.

### Validation truth

**Implementation and synthetic tests have been added, but have not been compiled or executed by the user.** The last confirmed green code gate remains Schema 1.9A/1.9B at `242744d1baf94dc1c523b3a873a6463017184081` (`BUILD SUCCESSFUL in 45s; 14/14 tasks executed`). Schema 1.10A remains pending until the user runs the forced loader gate at the exact latest branch tip.

**Scope boundary:** Schema 1.10A does not parse or apply mapping table contents, rewrite selectors, resolve target members, inspect injection-point instructions, execute plugins/wrappers, or prove semantic mapping correctness. The complete Forge/Minecraft 1.20.1 → 1.21.1 symbol mapping edge is still missing.

### Exact next user action

Guarded fast-forward from the user's last confirmed local HEAD `eb7caf2674253cd0cb759c2eb5a64fd4a724b5b1` to the latest remote branch tip in the continuation handoff, then run:

```powershell
.\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
```

Review the first concrete compile/test failure if any. Do not proceed to Schema 1.10B until this forced gate passes.


---

## 2026-11-14 — Schema 1.10A green; Schema 1.10B source-edge verification started

### Schema 1.10A validation status — GREEN

The earlier “Schema 1.10A mapping-profile validator implemented; user test gate pending” section above is historical and is superseded by this validated checkpoint.

- Tested commit: `cc651af52b192fb534d49f3ad494b42e3d15d6f0`
- User forced `:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks`.
- Result: **BUILD SUCCESSFUL in 53s; 14 actionable tasks, all 14 executed.**
- The failed 494-test attempt was traced to malformed JSON in the shared synthetic `validProfile()` fixture. The fixture was corrected in commit `cc651af52b192fb534d49f3ad494b42e3d15d6f0`; no validator assertions or guardrails were weakened.
- This validates Schema 1.10A's structural profile parser, path graph checks, artifact digest declarations, and preflight integration. It does not validate semantic contents of mapping tables or authorize selector rewriting.

### Schema 1.10B implementation — source-edge provenance and coverage

The initial Schema 1.10B implementation and source-side generation were user-tested at `493cae748f89f88308a7e1b98ea583d2e58871af`. The provenance report from that run has been reviewed. The follow-up test-only hardening increment at `9347a08fad3844f494c5635b28871afe062929c2` is now included in the user's successful forced gate at `755281d3b91a6cb1b818f5a927728cc412a6ec71`.

Changes made:
- `SrgMojangTable` exposes a deterministic generation result with counts for Mojang class/method/field rows, joined TSRG class/method/field rows, matched/unmatched methods and fields, duplicate intermediate member keys, duplicate SRG IDs, and generated output rows.
- Exact join keys are explicit: methods use obfuscated owner + obfuscated member name + full JVM descriptor; fields use obfuscated owner + obfuscated member name.
- Conflicting Mojang class/member mappings and conflicting values for a globally unique SRG ID now fail closed rather than silently overwriting.
- The generated resource remains a member-name table only: `SRG member ID -> Mojang member name`. It does not emit class-name mappings or descriptor-type mappings.
- Synthetic generator tests cover a complete owner/descriptor join, method descriptor mismatch, owner mismatch, and conflicting SRG IDs. Follow-up test `coalescesRepeatedSrgIdsWhenDifferentOwnersResolveToTheSameMojangName` asserts the same-name deduplication behavior observed in the real report; the conflict test now exercises reuse across distinct owners.
- Loader Gradle tasks now explicitly extract MCPConfig `config/joined.tsrg`; verify the downloaded Mojang 1.20.1 client mapping file's SHA-1 against the object identifier embedded in its URL; fingerprint the resolved MCPConfig ZIP, extracted TSRG2 input, and Mojang client mapping using SHA-256; and record the generated TSV digest and measured coverage in a build-local provenance JSON report.
- The provenance report is under `loader/build/reports/mappings/1.20.1/` and is not packaged. Raw Mojang mapping source files remain build-local and uncommitted. The generated mapping TSV itself is explicitly added to the loader JAR by `tasks.named('jar') { dependsOn genSrgMojang; from(srgMojangDir) }`; its redistribution/license status is not cleared by the report and must be reviewed before releasing the JAR.

### Scope and open questions

This is still the source-version SRG → Mojang 1.20.1 member-name normalization edge. The MCPConfig and Mojang inputs are joined for member-name lookup; this is not a reviewed Minecraft 1.20.1 → 1.21.1 symbol edge.

The source table does not itself map class owners or descriptor object types. The actual source refmap context's owner/descriptor namespace still needs evidence. Do not infer those dimensions from the context key or from same-name strings.

The Forge API migration catalog remains a separate domain from vanilla Minecraft mappings. Reuse `Forge1201TargetApiVerifier.TargetIndex` for any future target lookup; do not add another hierarchy cache.

### Next user gate

The initial Schema 1.10B code gate has passed and its report has been reviewed. The latest branch adds a test-only regression for same-name duplicate SRG IDs across different owners. Fast-forward from the user's tested code checkpoint to the exact remote tip recorded in the continuation handoff, then rerun:

```powershell
.\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
```

Do not begin selector rewriting. The initial corrected forced loader gate and provenance report review are complete at `493cae748f89f88308a7e1b98ea583d2e58871af`. The follow-up duplicate-ID regression test is also green at `755281d3b91a6cb1b818f5a927728cc412a6ec71`. Keep the independent mapping redistribution/license review open before packaging/release.


## 2026-11-14 — Schema 1.10B first forced run: test fixture correction pending validation

- The prior build-directory clean race was resolved: `:loader:clean` succeeded on the next run.
- Production and test compilation succeeded. MCPConfig extraction, Mojang SHA-1 verification, SRG-to-Mojang generation, and build-local provenance recording all completed.
- Verified during this run:
  - Mojang client mapping SHA-1: `6c48521eed01fe2e8ecdadbd5ae348415f3c47da`.
  - MCPConfig ZIP SHA-256: `f46d1050d8bed9046886b90a0d2fb80b4c6b5120fc35d32e5d37498dbe0a6d2a`.
  - Extracted `config/joined.tsrg` SHA-256: `19ae000175bfb58e5b26406c31a0e501bfb40000b078fee2f00ea60a8c7ed453`.
  - Mojang `client.txt` SHA-256: `67bfc33ef0c7103f002d6ac134f8eddd3d67e2ce45d266e83816d1a2e2c9b01d`.
  - Generated TSV SHA-256: `6353b8354260dc711b09e7540542ff5f2498635e021e4110870dd33ff466e175`.
  - Generation reported 64,225 member rows: 48,575/48,575 methods and 31,003/31,003 fields matched, zero duplicate member keys.
  - Build-local provenance report: `loader/build/reports/mappings/1.20.1/srg-mojang-provenance.json`.
- JUnit result: 498 tests completed, 2 failed, both in the first two `SrgMojangTableTest` cases. The failures were `IOException` thrown by `generateDetailed()`.
- Root cause: production TSRG2 parser correctly uses a leading tab to distinguish member rows from class rows. All four new synthetic fixtures used spaces for member indentation. Therefore positive fixtures were parsed as class rows and produced no mapping matches; negative tests passed for the wrong reason.
- Test-only fix committed at `e03c55daa6fd01e0855774e53a4255beedbad1fb`: all four fixtures now include Java `\\t` escape sequences to generate real leading tab characters. No production logic or assertions were loosened.
- Schema 1.10B's corrected forced loader test gate subsequently passed at `493cae748f89f88308a7e1b98ea583d2e58871af`; the provenance JSON was reviewed as recorded in the later checkpoint below.
- The later corrected run succeeded and its provenance JSON was reviewed. A follow-up test-only regression was added to lock duplicate-ID coalescing; run the latest pending gate before continuing.

## 2026-11-14 — Schema 1.10B corrected forced loader gate GREEN

- User fast-forwarded cleanly from `36db185a734ce387fe46f065e9658d8112df4b87` to `493cae748f89f88308a7e1b98ea583d2e58871af`. The fetched tip, published tip, final HEAD, clean worktree, and `git diff --check` guards passed.
- Forced command: `.\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks`.
- Result: **BUILD SUCCESSFUL in 50s; 17 actionable tasks, all 17 executed.** Clean, production compilation, test compilation, mapping extraction, mapping generation, provenance recording, and the full loader test task succeeded.
- The corrected `SrgMojangTableTest` fixtures now pass. The two initial failures were caused by spaces in synthetic TSRG2 member indentation; the test-only tab correction was committed as `e03c55daa6fd01e0855774e53a4255beedbad1fb`. No production parser change was needed.
- The successful rerun reproduced the previously reported fingerprints and coverage: Mojang SHA-1 verified as `6c48521eed01fe2e8ecdadbd5ae348415f3c47da`; MCPConfig ZIP SHA-256 `f46d1050d8bed9046886b90a0d2fb80b4c6b5120fc35d32e5d37498dbe0a6d2a`; extracted joined.tsrg SHA-256 `19ae000175bfb58e5b26406c31a0e501bfb40000b078fee2f00ea60a8c7ed453`; Mojang client.txt SHA-256 `67bfc33ef0c7103f002d6ac134f8eddd3d67e2ce45d266e83816d1a2e2c9b01d`; generated TSV SHA-256 `6353b8354260dc711b09e7540542ff5f2498635e021e4110870dd33ff466e175`.
- Coverage output: 64,225 member-name rows; methods 48,575/48,575; fields 31,003/31,003; zero duplicate member keys. This does not mean class or descriptor mappings were emitted.
- Provenance JSON path: `loader/build/reports/mappings/1.20.1/srg-mojang-provenance.json`. The user provided the full JSON and it was reviewed: its hashes, declared namespaces, generated table hash, size, and row count agree with the console output.
- Validation boundary: Schema 1.10B's forced loader test gate is **green** at `493cae...`, and the provenance report review is complete. The latest follow-up same-name duplicate-ID regression is not yet user-tested. Source mapping remains method/field member-name-only and explicitly `PARTIAL`; the Minecraft 1.20.1 → 1.21.1 edge remains missing. No selector rewriting or runtime-compatibility claims.

## 2026-11-14 — Schema 1.10B provenance report reviewed; duplicate-ID regression hardened

### Provenance report review
- User provided the complete `loader/build/reports/mappings/1.20.1/srg-mojang-provenance.json` produced by the successful 50-second run at `493cae748f89f88308a7e1b98ea583d2e58871af`.
- Report `schemaVersion` is 1. It declares `minecraft:forge-srg@1.20.1` → `minecraft:official-mojang@1.20.1`, direction `FORWARD`, status `PARTIAL`, scope `method-names` and `field-names` only.
- All three source artifact fingerprints match the successful Gradle console. MCPConfig ZIP: 1,740,711 bytes, SHA-256 `f46d1050d8bed9046886b90a0d2fb80b4c6b5120fc35d32e5d37498dbe0a6d2a`; extracted `config/joined.tsrg`: 5,653,149 bytes, SHA-256 `19ae000175bfb58e5b26406c31a0e501bfb40000b078fee2f00ea60a8c7ed453`; Mojang 1.20.1 client mappings: 8,001,795 bytes, SHA-1 matches the pinned URL object ID `6c48521eed01fe2e8ecdadbd5ae348415f3c47da`, and SHA-256 `67bfc33ef0c7103f002d6ac134f8eddd3d67e2ce45d266e83816d1a2e2c9b01d`.
- Generated TSV: 1,607,917 bytes, SHA-256 `6353b8354260dc711b09e7540542ff5f2498635e021e4110870dd33ff466e175`, 64,225 rows. Gradle compares its line count with `coverage.outputRows`; both are 64,225.
- Mojang source has 7,436 class rows, 63,477 method rows and 33,135 field rows. Joined TSRG has 7,436 class rows, 48,575 method rows and 31,003 field rows. All joined methods and fields matched (zero unmatched joined rows); the additional Mojang rows are outside the joined TSRG member-row denominator. Class rows are used internally for owner/descriptor joining, but are not written into the generated output.
- `duplicateMemberKeys=0` refers to duplicate intermediate Mojang lookup keys. `duplicateSrgIds=15,353` counts repeated SRG-ID row occurrences after the first, not 15,353 distinct IDs or conflicting names. The generator retains the first output name, permits a repeated ID only when its name is identical, and throws on a different name. The successful generation therefore had 15,353 same-name repeated occurrences and no conflicting SRG values.
- Arithmetic: `48,575 + 31,003 - 15,353 = 64,225`. The `outputRows`, generated table `rowCount`, and physical TSV line count agree.
- The report's licensing entry is `NOT_REVIEWED_BY_THIS_BUILD_TASK`. Inspection of `loader/build.gradle` confirms the generated TSV is included in the loader JAR (`tasks.named('jar') { dependsOn genSrgMojang; from(srgMojangDir) }`). The provenance JSON is not packaged, and the raw mapping inputs remain build-local. The license status of the derived packaged TSV is **not cleared**; a separate redistribution review must occur before release. Do not infer that a partial/modified subset is automatically permitted.

### Regression-test hardening prompted by the real report
- The 15,353 repeated SRG-ID occurrences make same-name cross-owner coalescing a material production path. The existing tests covered conflicting reuse but did not explicitly cover successful same-name reuse across owners.
- Commit `9347a08fad3844f494c5635b28871afe062929c2` adds a test with two distinct owners reusing the same method and field SRG IDs with the same Mojang names. It asserts two duplicate occurrences and two unique output rows. The conflicting-name test now also reuses an ID across distinct owners.
- This is test-only hardening. The previous Schema 1.10B test gate remains green at `493cae...`; the new test must pass on the next forced run before this extra coverage increment is called green.

## 2026-11-15 — Mojang mapping notice inspected; redistribution remains unresolved

- The user inspected the first four lines of the exact generated build input `loader/build/mojang-mappings/1.20.1-client.txt`. Its header reads: `© 2020 Microsoft Corporation. These mappings are provided as-is ... You may copy and use the mappings for development purposes, but you may not redistribute the mappings complete and unmodified ... Use and modification ... is governed by the Minecraft End User License Agreement ...`.
- The notice expressly prohibits redistribution of the mappings complete and unmodified. It permits copying/using for development purposes. It does **not**, on its face, expressly state that every modified extract, subset, or derived table may be redistributed. Do not interpret the narrower prohibition as an affirmative license grant for the generated 64,225-row TSV.
- The current Minecraft EULA says not to distribute Mojang/Microsoft material without permission and describes permitted mods as original work that does not contain a substantial part of Mojang/Microsoft copyrightable code or content. The EULA also says users do not own Mojang/Microsoft content or its substantial copies/derivatives. The extent to which a member-name-only mapping table is covered is a legal question not resolved by this engineering audit.
- Relevant official/current references reviewed: `https://www.minecraft.net/en-us/eula` and `https://www.minecraft.net/en-us/usage-guidelines`. The mapping notice itself points to the Minecraft EULA. Community commentary about the 2020 notice is not treated as authoritative legal advice.
- Engineering status: `loader/build.gradle` currently packages `srg-mojang.tsv` in the loader JAR. Its packaging/redistribution status is **NOT CLEARED**. Do not publish a release containing that generated table until written permission or an appropriate legal review confirms the distribution model. This is not a finding that the current artifact is necessarily infringing; it is an unresolved permission question.
- Candidate paths for later research, not decisions: (1) obtain explicit written permission for distributing the derived table; (2) determine whether a user-local generation/use model can satisfy both the mapping notice and runtime needs; or (3) identify a suitably licensed alternative mapping chain. Do not silently switch to a different namespace or claim equivalent coverage.
- Keep the license issue separate from technical validation: the Schema 1.10B code test gate passed at `493cae748f89f88308a7e1b98ea583d2e58871af`; the additional duplicate-ID regression test at `9347a08fad3844f494c5635b28871afe062929c2` is still pending its own forced run.

## 2026-11-15 — Follow-up duplicate-ID regression gate GREEN

- The user supplied a PowerShell transcript where the first local-HEAD precondition failed with `Unexpected HEAD.` because the command expected `493cae748f89f88308a7e1b98ea583d2e58871af`, but the local branch was already at `ee0196f3d0576e8013582d24173b2218fdba7ae0`.
- Because the commands were entered interactively, the failed `throw` did not terminate the PowerShell session. The user then fetched the expected remote tip, confirmed fetched/published tip `755281d3b91a6cb1b818f5a927728cc412a6ec71`, and successfully fast-forwarded. Git reported `Updating ee0196f3..755281d3` and updated only the two documentation files from that intermediate HEAD.
- The follow-up regression test commit `9347a08fad3844f494c5635b28871afe062929c2` was already in the local ancestor at `ee0196...`; therefore the forced Gradle run after the fast-forward included it.
- `git diff --check` completed without reported errors. User then ran `:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks`: **BUILD SUCCESSFUL in 50s; 17 actionable tasks, all 17 executed.** This validates the same-name cross-owner coalescing test and conflicting-name-across-owners test along with the loader suite.
- GitHub comparison confirms the remote branch tip is exactly `755281d3b91a6cb1b818f5a927728cc412a6ec71` (0 ahead / 0 behind). The branch contains the repeated-owner regression test; compare evidence shows test plus documentation files were the only changes since the earlier `493cae...` green code gate.
- The `Unexpected HEAD` message was a stale guard expectation, not a Git or Gradle failure. The subsequent fast-forward and test succeeded. Do not rerun solely because of that guard message. A post-merge `git rev-parse HEAD` / `git status --short` can be used as a final housekeeping confirmation if desired.
- Technical validation state: the Schema 1.10B forced loader suite including the duplicate-ID regression is **GREEN** at `755281d...`. The source edge remains `PARTIAL`, outputting member names only. The license/redistribution gate remains unresolved because the generated TSV is packaged into the loader JAR.
## 2026-11-15 — Read-only Intermediary bridge feasibility audit added (user validation pending)

### What was researched
- Fabric Intermediary maintains version-specific Tiny mappings and match information; its README describes Intermediary as a namespace intended to stay stable across Minecraft versions/mapping changes and states the dataset is CC0 1.0. Its build script publishes a plain Tiny v1 JAR and a separate `-v2` classifier, both with `mappings/mappings.tiny` inside.
- Exact pinned coordinates added: `net.fabricmc:intermediary:1.20.1` and `net.fabricmc:intermediary:1.21.1`. The plain artifacts are intentionally used because the implementation parser accepts Tiny v1 (`v1\tofficial\tintermediary`); do not replace with the `-v2` classifier without changing the parser and tests.
- Source inputs: the same resolved MCPConfig 1.20.1 `config/joined.tsrg` used by Schema 1.10B, plus the two version-specific Tiny v1 mappings. Tiny files are extracted under `loader/build/tmp/fabric-intermediary/` with a 32 MiB bound. Neither inputs nor JSON report are added to the loader JAR.
- The audit code consumes MCPConfig TSRG2 `obf` owner/member/descriptor columns, joining methods by obfuscated owner + obfuscated member name + full descriptor and fields by obfuscated owner + member name. It compares the resulting source Intermediary ID/owner/normalized descriptor against the 1.21.1 Tiny map. Unresolved descriptor class types are not counted exact; missing/changed owners/descriptors, removed IDs, and ambiguous joins are separate statuses.
- Critical namespace boundary: MCPConfig's header is `tsrg2 obf srg id`. This audit consumes the **obf** columns and retains SRG names/IDs only as diagnostic metadata. It does **not** convert a Forge SRG owner/member/descriptor selector into the obfuscated source namespace. The JSON declares `completeForgeSrgSelectorPath=false`, `selectorTransformationPerformed=false`, and `runtimeMappingTableProduced=false`. This is a vanilla symbol-continuity feasibility audit, not a complete Mixin selector bridge.
- The output stops at `minecraft:official-obfuscated@1.21.1`; converting target obfuscated names to NeoForge's Mojang-named runtime remains a separate edge.

### Added source/test/build files
- `loader/src/main/java/org/kylepitcock/legacyconnector/loader/mapping/Forge1201IntermediaryBridgeAudit.java`: bounded Tiny v1/TSRG2 parser, exact source join, target owner/member/descriptor continuity classifications, SRG-ID reuse diagnostics, SHA-256/size provenance, and build-local JSON output. No transformation methods or runtime mapping loader are added.
- `loader/src/test/java/org/kylepitcock/legacyconnector/loader/mapping/Forge1201IntermediaryBridgeAuditTest.java`: synthetic tests for exact matching, descriptor changes vs owner changes, SRG identifiers reused across owners, unmatched/ambiguous source rows, unresolved descriptor types, and JSON scope disclaimers.
- `loader/build.gradle`: exact Intermediary 1.20.1/1.21.1 dependencies with transitive dependencies disabled; restricted Fabric Maven repository; bounded Tiny file extraction; `auditIntermediaryBridge1201To1211` JavaExec; and an added dependency from `:loader:test` to the audit task.
- Design document: `docs/FORGE1201-PREFLIGHT-INTERMEDIARY-BRIDGE-AUDIT-DESIGN.md`.
- Code was reviewed for JSON separators, class-id overlap counting, and orphan TSRG2 member rows. The user has **not** compiled or run it yet.

### Preliminary source inspection — not the build report
- Fabric mapping files are pinned by exact version coordinates and their Tiny bytes will be fingerprinted in the user's build-local JSON report. The report generated by the Gradle task is authoritative; preliminary data read from public GitHub source blobs must not be substituted for the resolved Maven artifact results.
- The audit deliberately reports partial coverage. Intermediary stability is only a candidate identity relation; it is not proof of semantic equivalence, binary compatibility, or runtime behavior.

### Exact next action
- User's last confirmed local HEAD: `755281d3b91a6cb1b818f5a927728cc412a6ec71`.
- Latest remote tip: `01d1fc5f6b520b0c6b6aabcd309e7b292810f29d` (13 commits ahead, 0 behind).
- First run the guarded fast-forward to this exact tip, then run `:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks`.
- On a successful run, inspect `loader/build/reports/mappings/1.20.1-1.21.1/intermediary-bridge-audit.json` and check the reported SHA-256 values, source join coverage, shared Intermediary class/member counts, and method/field classifications. Do not proceed to selector transformation; first design the missing Forge SRG selector → source-obfuscated edge and target-obfuscated → NeoForge Mojang-named edge.
