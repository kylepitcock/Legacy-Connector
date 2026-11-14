# Forge 1.20.1 Preflight Analyzer — Slice 3 Design

Status: Slice 3 implementation, focused forced loader tests, real Symbiote Schema 1.3 report generation/inspection, and the consolidated clean build have all passed. The forced loader test checkpoint is `b0d2b7b68915137aed44ef9153fdfa304fa5819`; the report-generation checkout was `340805e8a1bb93bf1994645981c3246fd46298ea`; the consolidated build passed after the user fast-forwarded to `3e1291494b6c10cc58b2c54be8978df972e6cd68`. The report is static evidence only. Slice 4 Mixin analysis is now the next analyzer milestone.

## Goal

Extend the existing Slice 1/2 preflight pipeline so one report inventories the source Forge 1.20.1 JAR and its bounded nested mod/library archives. Preserve where each reference came from, identify additional bytecode reference contexts (including method handles, invokedynamic bootstrap data, and ConstantDynamic), and normalize source Minecraft SRG member names to the readable Minecraft 1.20.1 Mojang namespace before any 1.20.1 → 1.21.1 target comparison.

The analyzer remains static and read-only. It must never load or initialize scanned mod classes. A reference match is evidence, not proof of semantic or gameplay compatibility.

## Findings from the implementation audit

### Existing API scanner to extend

`Forge1201ApiUsageAnalyzer` is the existing ASM scanner and must remain the single source of bytecode inventory. It currently:
- scans `.class` entries in the outer JAR;
- uses ASM 9 `ClassReader` and `ClassRemapper`;
- records referenced game/API classes and package counts through `Remapper.map`;
- records method and field member references through `mapMethodName` / `mapFieldName`;
- records direct `visitMethodInsn` / `visitFieldInsn` call sites with caller class/method/descriptor and opcode;
- detects the main `@Mod` entrypoint, the manifest `MixinConfigs` attribute and `FMLCorePlugin` value.

Its current CLI bytecode trace printer can display `INVOKEDYNAMIC` bootstrap handles/arguments and `LDC` values, but these contextual records are not part of the preflight result. The Slice 3 implementation must verify exactly which type/member forms are already captured by `ClassRemapper` (including annotation/signature/header descriptors, Handles, method Types and nested ConstantDynamic values) before adding collectors. Do not double-count or replace existing member-reference semantics without a regression proving the need.

### Existing archive walker to extend

`Forge1201ArchiveGraph` already performs bounded recursive archive inspection. It reads nested JAR bytes in memory, follows nested archive candidates, preserves archive path/provenance, and enforces depth/count/size/inflated-byte limits. Its current nested snapshot retains metadata and child-JAR bytes but does not retain/scan `.class` entries. The outer source JAR's bytecode is scanned separately by `Forge1201ApiUsageAnalyzer`.

Add an optional nested-class consumer to this existing traversal. Do not create another independent ZIP/Jar-in-Jar walker and do not persist extracted files. The archive walker should continue owning archive safety, cycle protection, provenance, and budgets; the ASM analyzer should own bytecode interpretation.

### Existing mapping table to reuse

`loader/src/main/java/org/kylepitcock/legacyconnector/loader/mapping/SrgMojangTable.java` already generates and packages `legacyconnector/mappings/1.20.1/srg-mojang.tsv`. The table is a source-version mapping from Forge 1.20.1 SRG identifiers (`m_####_` / `f_####_`) to readable 1.20.1 Mojang member names. The build generated 64,225 mappings during the Slice 2 consolidated build.

Use `SrgMojangTable.load()` as the one integrated preflight mapping source. Do not create a third mapper or require users to provide a second CSV path for preflight. The older `Forge1201SrgNameMappings` CSV-based class is currently used by the translated-JAR verifier; review whether its parsing/mapping logic can eventually delegate to the shared table, but do not change that CLI contract in this slice unless required and covered by tests.

Keep the original SRG name and the normalized Mojang name side-by-side in diagnostics. Never destroy the raw value or map Forge member names just because they resemble an SRG token. Only apply this source mapping to recognized Minecraft/Mojang owners.

## Proposed implementation shape

### 1. One shared ASM inventory with explicit provenance

Refactor `Forge1201ApiUsageAnalyzer` around a reusable per-run inventory/collector. Its entry points should support:
- scanning the outer source JAR from a `ZipFile`;
- scanning one already-bounded nested class entry supplied as bytes by `Forge1201ArchiveGraph`;
- finishing one deterministic combined result.

Each class scan receives at least:
- archive path (for the source root, the stable `source/<filename>` path; for nested archives, the full parent `!/entry.jar` chain);
- class entry path within that archive;
- class internal name (read from the class file, not inferred solely from the ZIP path);
- byte length and scan outcome.

Preserve root-vs-nested class counts and per-archive counts. Do not treat installed top-level NeoForge provider JARs as old-mod bytecode to migrate. For this report's source dependency graph, scan nested archives discovered inside the input Forge JAR (mod JARs and library JARs); do not recursively scan the separate installed-mod directory as part of the input mod's bytecode inventory.

The scanner must process byte arrays synchronously and release them after ASM returns. It must not define/load classes.

### 2. Reference model and context

Keep the current aggregate member references and direct call-site models stable where possible. Add provenance-rich records for contexts that cannot be reconstructed from the aggregates. At minimum distinguish:
- class header: superclass and implemented interfaces;
- class/member/method descriptors and generic signatures;
- class, field, method, parameter, record-component, and type annotations (including annotation class literals, enum types, nested annotations, and arrays);
- declared method exceptions and try/catch exception types;
- type instructions, multi-dimensional array descriptors, frame object types, and local-variable descriptors/signatures;
- direct method/field instructions (retain opcode and caller);
- class literals and method-type constants in `LDC`;
- method handles and field handles (retain handle tag, owner, name, descriptor and interface bit);
- `INVOKEDYNAMIC` sites (retain caller, method name/descriptor, bootstrap handle, and recursively inspected bootstrap arguments);
- `ConstantDynamic` values (retain constant name/descriptor, bootstrap handle, and recursively inspected bootstrap arguments).

Recursively inspect ASM `Type`, `Handle`, and `ConstantDynamic` values with a defensive nesting-depth limit and null/unknown handling. Do not stringify a handle and then attempt to parse the text back into members.

For a source location, ASM's visitor API reliably provides instruction order but not the original bytecode byte offset for every instruction. Record an explicit zero-based instruction ordinal, not a fabricated byte offset. If a future low-level class-file reader obtains exact offsets, it can add a distinct nullable offset field.

For global type/member references, preserve existing deduplication/counting behavior. For contextual references, record stable source provenance and sort deterministically by archive path, class entry, caller, reference kind, and symbolic target.

### 3. Nested class traversal and safety

Extend `Forge1201ArchiveGraph` with an optional class-entry consumer invoked while each nested archive is already being decompressed by the existing bounded `snapshotFromBytes` pass. The consumer should only be invoked for the source input's nested archives, not for installed top-level provider mods.

Requirements:
- Reuse the current max depth 4, max nested archive count 128, max nested JAR payload 32 MiB, max inflated per nested archive 64 MiB, and max aggregate nested inflated 256 MiB limits.
- Add an explicit maximum individual class-entry payload and/or total captured class bytes if needed; every skipped class must produce a deterministic `ANALYSIS_INCOMPLETE` finding/warning, never silently disappear.
- Keep the current archive SHA-256 cycle protection. If identical nested content is skipped by the graph, it must not be scanned a second time.
- Scan class entries only after their containing nested archive is classified and only if the archive is a recognized nested mod or a library/unrecognized archive that passed ZIP/path/size safety checks.
- Preserve archive and class entry paths in every nested result.
- Detect duplicate class entry names within an archive and report ambiguity instead of counting multiple arbitrary versions.
- Do not extract classes to disk or reopen/reinflate the same nested archive through a competing walker.

### 4. SRG → Mojang normalization

Use the existing classpath resource via `SrgMojangTable.load()`. Add a deterministic normalization result that records, separately for methods and fields:
- original source SRG name;
- normalized 1.20.1 Mojang name when present;
- owner and descriptor;
- occurrence count and source locations;
- explicit unmapped status if the source name matches the SRG pattern but has no table entry.

Only normalize members whose owner is in the Minecraft/Mojang namespaces (currently `net/minecraft/` and relevant `com/mojang/` namespaces). Do not silently normalize Forge, NeoForge, third-party, or connector shim owners. Do not equate a mapped source name with a 1.21.1 target match; target-side aliases and structural verifier findings remain later/independent evidence.

### 5. Reporting / schema

The current report is Schema 1.2 / Analyzer 1.2.0. Treat Slice 3's nested class provenance, new contextual reference sections, and explicit SRG normalization as an additive report-contract change and bump the report to Schema 1.3 / Analyzer 1.3.0 when the first integrated output is committed. Preserve the existing fields and their meaning:
- `classCount` should continue to mean outer source JAR classes for backward interpretation;
- add explicit nested/total counts and per-archive class scan summaries;
- keep existing method/field aggregate reference and direct call-site sections stable;
- add separate contextual/dynamic reference sections and source-name normalization sections;
- state limits and skipped/unsupported evidence explicitly.

Both JSON and text report output must use the same immutable result model and stable ordering. Do not add timestamps to canonical report JSON/text. The text report should summarize counts before printing detailed references to avoid exploding normal output; detailed lists should still be present in JSON.

## Test plan

Add focused synthetic ASM + ZIP fixtures. The scanner tests must independently prove:
1. superclass, interface, class signature, field/method descriptors, generic signatures, and declared exceptions;
2. class/method/field/parameter/type annotations, enum values, nested annotations and annotation class literals;
3. direct method and field instructions with the correct caller, opcode and instruction ordinal;
4. type instructions, array types, local variables, try/catch and frame object types;
5. class literals and method-type constants;
6. method handles from `LDC`, invokedynamic bootstrap handles and each supported bootstrap argument value type;
7. nested `ConstantDynamic` values, including nested handle/type arguments;
8. unknown/unmapped SRG identifiers remain visible and are never guessed;
9. identical outer/nested class bytes retain different archive provenance when actually present as separate non-cyclic archives, while graph duplicate-content protection skips repeated identical nested archive content;
10. maximum nested class bytes / per-class size limits produce explicit incomplete findings;
11. deterministic JSON/text output across repeated runs and stable class/archive ordering.

Use ASM `ClassWriter` to build bytecode fixtures; do not compile fixture source with javac when the test is specifically about a rare opcode/value shape. Use `@TempDir` and in-memory nested JAR bytes like the existing archive graph tests.

## Exit gates

1. The new scanner tests pass with `--rerun-tasks`.
2. Existing Slice 1/2 loader tests still pass.
3. The Slice 3 Symbiote report is generated using a new unique run filename (never overwrite Slice 2 run-3), and JSON/text outputs are parsed and reviewed.
4. The report confirms outer plus nested class counts/provenance and shows any SRG normalization/unmapped results without claiming target compatibility.
5. The consolidated clean build succeeds after code changes.
6. Runtime compatibility remains unverified until an actual NeoForge test-profile launch and gameplay checks.

## Explicit non-goals for this slice

- No automatic Forge-to-NeoForge API migration or bytecode rewriting.
- No target-side compatibility verdict from SRG normalization alone.
- No semantic Mixin target/handler validation (Slice 4).
- No reflection string rewriting; reflection/resource strings can be a later heuristic layer.
- No installed native provider bytecode scanning as part of the source mod's bytecode inventory.


## Implementation checkpoint — 2026-11-14

### Implemented in the current integration batch (pending user test gate)

- The existing Forge1201ArchiveGraph now accepts an optional nested-class consumer. It reuses its bounded in-memory ZipInputStream pass to capture safe .class entries alongside metadata and child JARs; there is no extraction or second nested-archive walker.
- Only nested archives originating inside the input Forge JAR are submitted to the bytecode consumer. Installed provider JARs remain dependency evidence only.
- Nested FORGE_1201_MOD and LIBRARY_OR_UNRECOGNIZED archives are fed into the same Forge1201ApiUsageAnalyzer.Inventory that scans the outer JAR. Embedded native NeoForge mod bytecode is explicitly excluded from the legacy-source scan.
- Class entry paths and archive chains are preserved in dynamic-reference provenance and member-reference source locations. Per-archive summaries report discovered, scanned, and skipped class entries.
- Additional bounds are 8 MiB per class, 16 MiB captured class payload per nested archive, 128 MiB aggregate nested class payload submitted to the scanner, and 64 MiB aggregate outer class payload. Unsafe paths, duplicate entry names, oversize payloads, aggregate-limit skips, and parser rejections remain visible as warnings/summaries.
- SRG method/field member tokens owned by net/minecraft/ are looked up through the existing generated SrgMojangTable.load() resource. Reports preserve the raw token, 1.20.1 Mojang name, descriptor, count, and archive/class source locations. Unknown tokens are explicitly UNMAPPED. No Forge/NeoForge/third-party owner is normalized, and mapping is not a 1.21.1 compatibility verdict.
- The preflight report adds nested class totals, per-archive class scan summaries, normalized member rows, and bytecode scan warnings while keeping classCount as the outer source-JAR count.

### Synthetic coverage added in this batch

- A nested ZIP fixture containing a class with Minecraft SRG member references proves that the shared collector sees nested call sites and retains the nested archive path.
- The fixture chooses a method token from the generated 1.20.1 SRG table and also contains an intentionally unknown token, covering both MAPPED and UNMAPPED reporting.
- A nested class larger than the class payload limit proves it is counted as skipped rather than silently omitted.
- Utility-level coverage proves mapped/unmapped behavior and excludes third-party owners from normalization.

### Still pending / not claimed complete

1. User must pull and run the forced loader test gate against the integration commit. No compile/test success is claimed for this integration batch yet.
2. Add raw ZIP duplicate-entry regression coverage if a stable fixture can be constructed without relying on invalid ZipOutputStream behavior.
3. Complete the ClassRemapper coverage audit with deterministic fixtures for class headers, generic signatures, annotation structures, descriptors, exceptions, try/catch, type instructions, frames, and local-variable descriptors; prove which callbacks already capture these without double-counting.
4. Review source SRG mapping behavior against real Symbiote references after the focused test passes; keep all unknown cases visible.
5. Generate a new uniquely suffixed Symbiote Schema 1.3 report, inspect JSON/text and per-archive counts, then run the consolidated clean build.
6. Mixin semantic target/handler analysis remains Slice 4; runtime/gameplay compatibility remains unverified.

Do not overwrite the prior Symbiote Slice 2 run-3 artifacts. Keep Palladium + Alien Evolution isolated from this Universal branch.


### Pre-test review correction — 2026-11-14

The first integration commit was followed by commit 3c6febb075c32391b6879b0f3ef00c5856e80d51, which updates the existing limitation assertion to match the now-implemented source-nested Forge/library scan. Both commits remain pending the user's forced loader test gate.


### Generated mapping-resource test wiring — 2026-11-14

Commit 0c7c55b72033c986d94f1cba5d836dfce26e95a0 updates loader/build.gradle so the loader test task depends on genSrgMojang and includes the generated srgMojangDir on its runtime classpath. This is necessary because SrgMojangTable.load() returns an empty map when the generated resource is absent. A clean forced loader test will therefore run the existing fetchMojangMappings1201 and genSrgMojang1201 tasks before executing the regression suite. This is committed but not yet user-tested.


### Test-gate diagnosis — 2026-11-14

The first forced test run against source commit `1c403bcc93f3e2c1d57215aea7e41419f29e1ed3` reached 454 tests with one failure: `reportsOversizedNestedClassEntriesAsExplicitlySkipped()`, line 144. The class entry was counted as skipped, and `snapshotFromBytes()` generated the expected oversized-class warning. However, `scanNestedClassEntries()` initialized the per-archive summary warning list as empty, so the warning was retained only in the graph-wide warnings and omitted from `ClassScanSummary.warnings()`. The regression assertion therefore exposed a reporting-provenance gap, not a failure to enforce the byte limit.

The follow-up fix seeds each per-archive class summary with the class-specific snapshot warnings (unsafe class-entry paths, duplicate class entries, and class payload capture-limit skips). Non-class archive warnings remain in the graph-wide warning list and are not copied into the class summary. The user must pull the new commit and rerun the forced loader test gate; no passing result is claimed yet.

## ClassRemapper structural/context audit — 2026-11-14

### Confirmed by source inspection

The scanner uses one ASM 9 ClassRemapper over the custom collector. The inherited remapping path already records aggregate type/member references through ApiReferenceCollector.map, mapMethodName, and mapFieldName for the following standard class-file structures:

- class superclass, interfaces, generic class signatures, and type-annotation descriptors;
- field descriptors and generic signatures;
- method descriptors, generic signatures, declared exceptions, method/parameter/type-annotation descriptors;
- instruction owner/type/array descriptors;
- try/catch exception types, stack-map frame object types, local-variable descriptors/signatures;
- record-component descriptors/signatures.

The ClassRemapper path alone did not traverse nested annotation element values in most scopes because the downstream visitors returned null. Field and record-component visitors were also absent, preventing their annotation values from being visited. Therefore annotation class literals could be missing even when the annotation descriptor itself was counted.

### Annotation-value correction

The scanner now returns recording visitors for class, field, record-component, method, parameter, type, instruction, try/catch, local-variable, and annotation-default contexts. The visitors recurse through nested annotations and arrays. Annotation class literals and method types flow into the existing dynamic-reference result with archive/class provenance. Enum type descriptors continue to be captured by ASM's remapping callbacks. Instruction type-annotation values retain the ordinal of the instruction they annotate; metadata-level annotation values use instructionOrdinal = -1 and are formatted as non-code contexts in text output.

Dynamic-reference sorting now treats class/field-level null caller-method and caller-descriptor values as empty sort keys. This is required for deterministic output once metadata-level references are present.

### Synthetic fixture coverage added (pending user test)

The new structural fixture distinguishes types used only in class headers/interfaces/generic bounds, field and method descriptors/signatures, declared exceptions, try/catch blocks, stack-map frames, local-variable descriptors/signatures, type instructions, multi-dimensional array descriptors, and record-component descriptors/signatures. It also covers class, class-type, field, field-type, method, method-return-type, parameter, instruction, try-catch, local-variable, record-component, and nested annotation values, including arrays, enum descriptors, and annotation class literals.

The new code/test batch has not yet been run by the user. Do not claim compilation or test success for these newest commits. The previously green gate at 43f6db7726c3e023b331d714cbc3ccb6452e7564 validates the nested-class warning fix only.

### Remaining Slice 3 work

1. Pull the latest feat/forge1201-preflight-analyzer tip and run the complete forced :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks gate.
2. Fix any compile/runtime fixture failures before generating any real-mod report.
3. Review whether a stable raw-ZIP duplicate-entry fixture can be added without depending on ZipOutputStream duplicate-entry rejection behavior.
4. Add/confirm deep nested ConstantDynamic recursion and depth-limit coverage.
5. After tests pass, generate a uniquely suffixed Symbiote Schema 1.3 JSON/text report; never overwrite Slice 2 run-3. Review nested archive class counts, annotation/dynamic references, SRG mapped/unmapped evidence and warnings.
6. Run the consolidated clean build. Runtime launch/gameplay compatibility remains a separate unverified gate.
## Duplicate ZIP entries and deep ConstantDynamic regression coverage — 2026-11-14

### Added synthetic tests (pending user test gate)

- `reportsDuplicateNestedClassEntryNamesAsAmbiguousAndSkipsAllCopies()` builds a raw ZIP byte stream with two local-file records and two central-directory records for the same class-entry name. This intentionally avoids ZipOutputStream, which rejects duplicate entry names before the archive reader can be tested. The regression asserts both copies are discovered, neither is scanned, both are counted as skipped, the per-archive warning names the duplicated class path, and no bytecode references from that ambiguous class leak into the result.
- `recordsDeepConstantDynamicNestingAndExplicitlyStopsAtTheDepthLimit()` creates a nested ConstantDynamic chain with 17 nodes. It asserts all 17 constants at depths 0 through 16 are recorded and the attempted value at depth 17 becomes exactly one VALUE_DEPTH_LIMIT finding. It checks the repeated constant-argument path and full archive/class provenance.
- The earlier one-level ConstantDynamic bootstrap-argument case remains in the main deterministic inventory fixture.

Latest test source commit for these two edge cases: `ba8428932da023f9c527a6b6eb0d6056626a88bb` (includes exact recursion-boundary assertion).

These tests have not yet been run locally. Keep the Schema 1.3 real Symbiote report blocked until the complete forced loader test gate passes on the latest branch tip.
## Slice 3 edge-case validation complete — 2026-11-14

The user pulled Universal branch tip `b0d2b7b68915137aed44ef9153f4dfa304fa5819` and ran the forced clean loader test command:

    :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
    BUILD SUCCESSFUL in 31s
    14 actionable tasks: 14 executed

This validates the newly added raw duplicate-ZIP class-entry fixture and the 17-level ConstantDynamic recursion boundary test, in addition to the previously green structural/annotation fixture and nested archive/SRG test coverage. The transcript shows generated SRG-to-Mojang mapping output of 64,225 members. It does not show a JUnit count; do not invent one. The IntelliJ PSReadLine 2.0.0 startup warning is unrelated to the Gradle result.

### Slice 3 test exit gate

All currently planned focused Slice 3 test cases have now passed in the user's forced loader run. This establishes analyzer test coverage and compilation, not actual NeoForge runtime or gameplay compatibility.

### Next action — real Symbiote Schema 1.3 report

The testbench `archive/preflight/README.md` confirms no Slice 3 report exists yet and requires the canonical first-run root filenames:

- `symbiote-1.1.3.jar-slice-3.json`
- `symbiote-1.1.3.jar-slice-3.txt`

The source jar is `C:/Users/Administrator/legacy-connector-testbench/symbiote-1.1.3.jar`, whose expected SHA-256 is `e40f48a8a9bf1f39ea173a7c163285df199b7d2cf1dfcbf3efa721fe7f2f942c`. Preserve Slice 2 run-3 unchanged. Include the installed mods directory in the scan, as done for the earlier reports, if it still exists: `E:/Games/PolyMC/instances/Neoforge tester/.minecraft/mods`.

Use root Gradle task `preflight1201` with explicit `-PpreflightJar`, `-PpreflightModsDirs`, `-PpreflightJson`, and `-PpreflightText` paths. The schema/analyzer versions in source are `1.3` / `1.3.0`; target is Minecraft `1.21.1` / NeoForge `21.1.258`. Validate the JSON parses and its source SHA matches the expected original before uploading the JSON/text files to the testbench root.

After reviewing the report, update `archive/preflight/README.md` with output blob SHAs and report totals, then run the consolidated clean build. Actual NeoForge launch and gameplay remain a separate unverified gate.

## Real Symbiote Schema 1.3 report inspection — 2026-11-14

The first actual report from `symbiote-1.1.3.jar` is now generated, parsed, and checked against its text companion. Input SHA-256: `e40f48a8a9bf1f39ea173a7c163285df199b7d2cf1dfcbf3efa721fe7f2f942c`; target: Minecraft `1.21.1` / NeoForge `21.1.258`; schema/analyzer: `1.3 / 1.3.0`.

### Observed source inventory

- 515 source archive entries.
- 278 outer source-JAR class entries discovered; 278 scanned; zero class-scan warnings.
- No source Jar-in-Jar paths were declared. Therefore zero source-nested archive class entries is the expected result for this input, not an indication that its 278 class entries were skipped.
- The separate installed-mod directory contributes 11 archive graph records: 7 top-level installed archives, 3 nested archives under KubeJS, and the Symbiote source root. The 9 provider records include 8 installed NeoForge providers and Symbiote itself. The installed provider archives are not scanned as Forge source-mod bytecode.
- 1 metadata warning: the partial scalar metadata reader does not parse the multiline value beginning on `mods.toml` line 10.
- 1 archive-graph warning: Symbiote's declared Minecraft range `[1.20.1,1.21)` excludes target `1.21.1`.

### Reference inventory

- 443 API types: 322 outside the Forge API namespace, 78 package-relocation candidates, 28 manual-adapter requirements, and 15 explicit type replacements.
- 1,110 method references and 352 field references; 5,369 method call-sites and 1,196 field call-sites.
- 1,156 dynamic/bootstrap references: 813 invokedynamic sites, 282 method-type constants, 56 class literals, and 5 method handles.
- 1,076 source SRG member references normalized to 1.20.1 Mojang names; all 1,076 map successfully.
- The explicit replacement set includes ModConfigSpec, IMenuTypeExtension, Legacy Connector-owned networking compatibility types, and Supplier mappings. The manual set includes config-screen, GUI overlay/layered draw, tick/damage-event, FML/side-separation, menu-opening, event-subscriber, and registry changes.

### Interpretation boundary

The report has no Mixin config candidates, no source Jar-in-Jar paths, and no coremod class. It still contains package references under `org/spongepowered/asm`; the absence of a detected config is not Mixin target validation. The report does not validate target class/member existence, injection targets/handler descriptors/slices/refmaps/plugins, provider bytecode, translated resources, initialization, rendering, networking behavior, or gameplay. The 78 package-relocation candidates remain provisional until target verification; the 28 manual-adapter types require semantic review.

The JSON and text summaries were compared and agree on source identity, schema, archive/provider/dependency totals, class counts, API/member/call-site totals, dynamic references, normalized SRG rows, and warnings. All Slice 3 test gates and the real report generation are complete. The next build gate is the consolidated clean root/loader/test-mod build; then begin Slice 4 Mixin analysis.

## Slice 3 consolidated clean build gate — 2026-11-14

The user fast-forwarded the Universal branch to `3e1291494b6c10cc58b2c54be8978df972e6cd68` and ran:

```text
:clean :loader:clean :testmods:clean :build :loader:build :testmods:assemble --no-daemon --console=plain
BUILD SUCCESSFUL in 1m 23s
33 actionable tasks: 24 executed, 9 from cache
```

Observed generated outputs:
- SRG-to-Mojang Minecraft 1.20.1 mapping table: 64,225 members.
- Shim index: 5,258 classes.
- Beta 1.7.3 mapping output: 9,243 rows.
- Legacy API stubs: 1,848 generated.
- Legacy 1.7.10 test mod reobfuscation: 16 classes.
- Loader/root/test-mod clean build, build, and assembly tasks completed.
- `:loader:test` and root `:test` were `FROM-CACHE` during this consolidated build; they were separately forced and passed earlier at Slice 3 source checkpoint `b0d2b7b...` (`:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks`, 14/14 tasks executed).
- Gradle emitted its non-fatal deprecated-feature / Gradle 9 compatibility warning. No failure occurred.

This completes the planned Slice 3 source-test and consolidated build gates. It does not imply a NeoForge client/server launch or gameplay validation.


