# Forge 1.20.1 Preflight Analyzer — Slice 4 Mixin Analysis Design

Status (2026-11-14): Slice 4 remains in progress. Schema 1.7 @At/@Constant inventory passed the user loader test gate at 267db731f7b16b30498f3402daf83bcb340849d1 (BUILD SUCCESSFUL in 39s; 14/14 tasks), and Schema 1.8 @Slice from/to inventory passed at 4b4f9ce437671f08eafd3f9cf4a18e34a3d24284 (BUILD SUCCESSFUL in 33s; 14/14 tasks). The Mixin 0.8.5 refmap runtime/AP contract has now been audited and the Schema 1.9 design is documented in docs/FORGE1201-PREFLIGHT-SCHEMA-1.9-REFMAP-DESIGN.md. Schema 1.9 implementation and tests have not started. The next step is bounded refmap resource/table inventory, not selector rewriting or guessed namespace conversion.

Historical implementation checkpoints below remain useful context; the latest dated checkpoint at the end controls current status.
## Goal

Turn the existing offline Mixin triage utility into a reusable, deterministic analyzer that contributes machine-readable findings to the unified Forge 1.20.1 -> NeoForge 1.21.1 preflight report.

The analyzer must do more than count missing classes or members. It must preserve the provenance from each config to each mixin class, target, shadow/accessor/invoker, injector selector, `@At` selector, slice boundary, refmap, and plugin. It must distinguish a confirmed structural miss from incomplete information, unsupported selector syntax, remapping uncertainty, plugin-dependent behavior, and semantic/runtime validation.

Mixin analysis remains static. It must never load or initialize classes from the mod, instantiate a Mixin plugin, apply a mixin, or claim that an injection will succeed at runtime.

## Existing implementation audit

The current implementation is `loader/src/main/java/org/kylepitcock/legacyconnector/loader/tools/Mixin1201Triage.java`. Source review at Universal branch `3e1291494b6c10cc58b2c54be8978df972e6cd68` found these concrete limitations:

1. It is a CLI-only utility. Its result is a mutable `Stats` accumulator and a text file; there is no reusable per-run result model for `Forge1201PreflightAnalyzer` or JSON consumers.
2. Target indexes and rename tables are static mutable maps (`MC_INDEX`, `CLASS_RENAMES`, `MEMBER_RENAMES`). They can leak state between analyses and are not safe for independent/repeated runs.
3. Its CLI requires a merged target JAR and optional external rename-table directory. The target class index stores member names but not descriptors, and the lookup checks name presence rather than exact owner/name/descriptor signatures.
4. Config discovery is based on JSON path/content heuristics inside each scanned JAR. The unified preflight already discovers manifest `MixinConfigs` declarations and heuristic candidates, but the triage utility does not consume that unified provenance. Forge TOML mixin declarations are not currently exposed by `Forge1201JarInspector`.
5. A config whose declared mixin class is not found in the source archive is silently skipped. Malformed candidate JSON is swallowed. These cases therefore do not become explicit report findings.
6. `@Mixin` targets are read only from invisible class annotations. The implementation should support both visible and invisible annotations and both class-literal and string targets.
7. Member checks are mostly name-only. They do not consistently compare descriptors, distinguish inherited/incomplete hierarchies, or preserve every selector as a structured finding.
8. Injection annotations and some `@At` strings are recognized, but full selector parsing, exact descriptor verification, slices (`from`/`to`), opcode/ordinal/shift constraints, and handler-descriptor semantics are incomplete.
9. Refmaps are not parsed or validated. A config plugin is counted but never checked as a class resource and is never executed (execution must remain prohibited).
10. Dynamic targets, plugin-provided targets, unsupported selectors, and missing target-index data are not represented with sufficiently precise uncertainty states.

These are implementation gaps to correct, not grounds to discard the existing tool. Preserve useful CLI compatibility by making the CLI a thin wrapper over the new result API.

## Architecture

### 1. A state-free analysis API

Introduce a dedicated `Forge1201MixinAnalyzer` (name may be adjusted to match repository conventions) with an immutable `analyze` entry point. Inputs should be explicit and per invocation:

- a safe source-archive view that exposes entry names, bounded entry streams/bytes, archive identity, and provenance path;
- the discovered config declarations and their discovery sources;
- a read-only target class/member index for the selected Minecraft/NeoForge target;
- source-to-target class/member rename data, with its version/source identified;
- limits and target profile.

No static mutable analysis state. Repeated calls with different fixtures must not share cached classes, mappings, counters, or findings. Any cache must be owned by one analyzer invocation and keyed by the exact target/mapping profile.

Retain `Mixin1201Triage` as a command-line compatibility layer that builds the inputs, invokes the reusable API, and formats a legacy text summary. It must not contain a second implementation of selector analysis.

### 2. Result model and provenance

Add immutable records (or equivalent repository-standard result types) for:

- **Config finding:** archive path, config resource path, discovery source(s), parse state, package, required/minimum version/compatibility level, plugin class, refmap path, injector defaults, and config-level warnings.
- **Mixin class finding:** config path, declared section (`mixins`, `client`, or `server`), fully resolved class name, source class-entry path, class presence, annotation parse state, and plugin/dynamic flags.
- **Mixin target finding:** original selector, source owner, mapped target owner, target status, selector form (class literal or string), remap flag, and evidence.
- **Member finding:** annotation kind, source mixin class/member, target owner/member, source and target descriptors, member kind (method/field), match state, alias/prefix evidence, and whether the check is inherited.
- **Injection finding:** injector annotation, target selector(s), handler signature, parsed `@At` selectors, slice `from`/`to` boundaries, ordinal/opcode/shift/constraints where present, and a conservative validation state.
- **Plugin/refmap finding:** declared resource/class, existence and parse checks, remap information, and reasons static analysis cannot resolve behavior.
- **Warning/limitation:** stable code, severity, evidence level, source location, and an explanatory message.

Every finding must point back to its config and source archive/class entry. Do not reduce the result to counters or a string key like `owner.name`. Sorting must be stable and independent of ZIP entry order.

### 3. Config discovery and safe parsing

Combine these sources while retaining how each path was discovered:

1. Manifest `MixinConfigs` declarations (authoritative declared paths).
2. Supported Forge metadata declarations, where the metadata reader explicitly recognizes them. Extend the partial TOML inspector for the actual supported declaration form rather than guessing from arbitrary text. Unsupported/multiline/ambiguous metadata must remain a warning and must not be silently treated as absent.
3. Resource-path and content heuristics (candidate only, never promoted to a confirmed declaration solely because a filename contains “mixin”).

Normalize and validate every config path. Reject absolute paths, parent traversal, NULs, and ambiguous duplicate ZIP entries. Missing declared configs, invalid JSON, oversized configs, and heuristic false positives must have explicit result states.

Parse standard config fields without instantiating any plugin: `package`, `required`, `minVersion`, `compatibilityLevel`, `plugin`, `refmap`, `mixins`, `client`, `server`, and `injectors.defaultRequire` (plus other known scalar/list values only when supported). Resolve each entry relative to the config package using the exact config/archive context. A missing class named in a config is a finding, not a silently ignored entry.

Use bounded JSON sizes, bounded config counts, bounded aggregate bytes, and safe ZIP entry reads. Nested source archives must use the existing `Forge1201ArchiveGraph` traversal and its established depth/archive/size/inflated-byte budgets; do not create a second unbounded archive walker. If a source-nested archive cannot be inspected because a limit or archive error was reached, report the omitted analysis explicitly.

### 4. Target class/member index

Reuse or refactor the structural class metadata work already present in `Forge1201TargetApiVerifier`. Mixin validation needs exact class hierarchy, method descriptor, and field descriptor information, not only member names.

The target index should be read-only and expose:
- class presence, superclass, interfaces, and whether hierarchy resolution is complete;
- methods by name plus descriptor, including constructor/static initializer distinctions;
- fields by name plus descriptor;
- inherited lookup with a trace of the hierarchy considered;
- missing target class vs incomplete target index vs a confirmed absent member.

Use the pinned target profile (Minecraft 1.21.1 / NeoForge 21.1.258) and identify the exact target source/index used. Any source 1.20.1 -> target class/member renames must come from a named, versioned mapping input. Do not silently assume that a class/package rename table is complete. Unavailable mapping data yields an explicit unresolved/remapping-required state.

Do not treat a matching name alone as a verified member. Exact descriptor checks are required where the annotation/selector provides enough descriptor information. If Mixin's selector language or an event/plugin alters the resolved target, mark it as requiring additional validation rather than fabricating a match.

### 5. Annotation and selector coverage

At minimum, inspect annotations on the mixin class, fields, and methods, including visible and invisible annotations.

- `@Mixin`: class-literal and string targets, multiple targets, remap/pseudo flags, and missing/ambiguous target declarations.
- `@Shadow`: default name, prefix, aliases, remap flags, field vs method, and descriptor.
- `@Overwrite`: exact method signature, inherited/target state, and overwrite constraints when statically observable.
- `@Accessor` / `@Invoker`: explicit value or inferred target name, getter/setter/invoker shape, target field/method kind, and descriptor compatibility.
- Injection annotations: `@Inject`, `@Redirect`, `@ModifyArg`, `@ModifyArgs`, `@ModifyVariable`, `@ModifyConstant`, `@ModifyExpressionValue`, `@ModifyReceiver`, `@ModifyReturnValue`, `@WrapOperation`, `@WrapWithCondition`, `@WrapMethod`, and other known injector annotations. Unknown Mixin annotation types must remain visible as unclassified findings.
- `@At`: preserve `value`, `target`, `args`, `ordinal`, `opcode`, `shift`, and `by`. Parse method and field selectors including owners and descriptors. Distinguish codes such as `HEAD`, `TAIL`, `RETURN`, `INVOKE`, `INVOKE_ASSIGN`, `FIELD`, `NEW`, and `CONSTANT`; unsupported codes/args must be marked unresolved rather than guessed.
- `@Slice`: analyze both `from` and `to` `@At` boundaries, retaining each boundary separately.

Validate target existence structurally where an exact selector is resolvable. Do not claim that a structurally present target proves a valid injection point or correct handler stack semantics. Handler argument/return compatibility should be checked only for rules that can be stated precisely; all other forms remain explicitly semantic/runtime validation.

### 6. Refmaps and plugins

- Check whether the declared refmap resource exists in the same archive context; parse its JSON under limits and preserve its mapping sections/data. Missing or malformed refmaps are findings.
- Do not assume names are already in target namespace merely because a refmap is absent. Report source mapping/remap ambiguity.
- Check whether the declared plugin class exists in the archive (or an explicitly identified dependency/source archive). Never load, instantiate, initialize, or call it.
- Any config plugin that can provide dynamic mixin lists/targets, a dynamic selector, or runtime decisions must make affected findings `PLUGIN_DEPENDENT` / `RUNTIME_VALIDATION_REQUIRED`. Static target guesses must not be marked verified.

### 7. Integration and reporting

Extend `Forge1201PreflightAnalyzer.PreflightResult` with a versioned, immutable Mixin analysis result. Keep the report's deterministic sorting and explicit limitation policy. Include per-config summaries in text output but preserve all structured per-target/member/injection findings in JSON.

Keep Slice 4 separate from target API migration decisions. A Mixin finding may consume the shared target class index and class/member rename maps, but must not invent new compatibility adapters or rewrite the mod. Later slices can join these findings with bridge/shim coverage and runtime feedback.

## Finding-state vocabulary

Use states with distinct meanings; adapt names to existing repository enums if a compatible vocabulary exists:

- `DECLARED_CONFIG`: path came from a recognized declaration.
- `HEURISTIC_CONFIG_CANDIDATE`: resource heuristic only.
- `CONFIG_MISSING`, `CONFIG_INVALID`, `CONFIG_LIMIT_EXCEEDED`, `CONFIG_DUPLICATE_ENTRY`.
- `MIXIN_CLASS_PRESENT`, `MIXIN_CLASS_MISSING`.
- `TARGET_CLASS_PRESENT`, `TARGET_CLASS_MISSING`, `TARGET_HIERARCHY_INCOMPLETE`, `TARGET_REMAP_UNRESOLVED`.
- `MEMBER_VERIFIED`, `MEMBER_MISSING`, `MEMBER_DESCRIPTOR_MISMATCH`, `MEMBER_HIERARCHY_INCOMPLETE`.
- `SELECTOR_VERIFIED_STRUCTURALLY`, `SELECTOR_UNRESOLVED`, `SELECTOR_UNSUPPORTED`.
- `PLUGIN_DEPENDENT`, `REFMAP_MISSING`, `REFMAP_INVALID`, `RUNTIME_VALIDATION_REQUIRED`.

A static “verified” state applies only to the specific structural fact checked. It does not mean the whole mixin applies, the injector succeeds, or game behavior is correct.

## Implementation checkpoint — reusable target index (2026-11-14)

The first Slice 4 source increment is committed on `feat/forge1201-preflight-analyzer`:

- `Forge1201TargetApiVerifier.TargetIndex` is created with `createIndex(ClassBytesProvider)`. Each instance owns its own target-class cache.
- `classInfo` exposes immutable target class metadata (superclass, interfaces, and exact method/field name+descriptor signatures).
- `lookupMethod` and `lookupField` reuse the existing verifier's hierarchy traversal and return `FOUND`, `MISSING`, or `INCOMPLETE_HIERARCHY`, plus the classes visited.
- This is a public read-only view over the existing verifier; it does not create a second hierarchy implementation or claim semantic/runtime compatibility.

Source commit: `e3aa921d01d8ee134de3a71d3aa1016d249dbaf1`.
Focused synthetic ASM tests: `69a93e13183466e51a6bab8023eecb0bb906e5e9`.

The tests cover exact descriptor matching, inherited methods and fields, missing classes, incomplete hierarchy, and isolation between index instances. The user subsequently ran `:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks` at commit `246002c6fcc1af371bb11a716483dd1df23a2dbe`; it passed (`BUILD SUCCESSFUL in 1m 40s`, 14 actionable tasks, all 14 executed). This validates the TargetIndex API and its tests. The newer Mixin analyzer tests described below were added after that run and remain pending.

## Implementation checkpoint — bounded config and @Mixin target discovery (2026-11-14)

The first implementation increment added `Forge1201MixinAnalyzer` and synthetic tests:

- It accepts explicit config declarations with discovery provenance (`MANIFEST`, `FORGE_METADATA`, or `RESOURCE_HEURISTIC`); duplicate declarations for one path are coalesced while retaining all sources.
- It performs bounded, read-only inventory of the **outer source JAR only**. Unsafe paths, missing/duplicate configs, malformed JSON, config count/size budgets, unresolved mixin classes, duplicate class entries, class byte limits, and incomplete archive inventory are explicit states/findings.
- It resolves `package` plus `mixins` / `client` / `server` entries and records missing classes rather than silently skipping them. It checks both visible and invisible `@Mixin` annotations, class-literal targets, string `targets`, and `remap`.
- Minecraft and Mojang targets require an explicit versioned source-to-target class mapping. The analyzer does not assume identity mapping. Forge namespace targets use only the existing reviewed `Forge1201MappingCatalog` package moves/explicit replacements; manual-adapter and unmapped cases remain unresolved.
- Resolved target class presence is checked through the shared `Forge1201TargetApiVerifier.TargetIndex`. Structural presence is not an injection or gameplay success claim.
- It checks a declared plugin class as a source-archive resource and marks a present plugin `PRESENT_NOT_EXECUTED`; plugin code is never loaded or run. Refmap presence and bounded JSON object validity are checked without applying remapping.
- Plugin lookup is currently limited to the source archive; a missing-in-source plugin may still be supplied by a declared dependency and must be reconciled with dependency-graph provenance in a later increment.
- Nested JAR configs/classes, Forge metadata config discovery, member annotations, injector target descriptors, `@At` / `@Slice`, refmap semantic mapping, and `Forge1201PreflightAnalyzer` JSON/text integration are **not yet implemented**.

Source commits:
- `df4d8531b1b1a739119ca3be8bbf982c04f42bd0` — initial analyzer API.
- `d0248dd544e028f55abb2bf41ac2d1a43f51c9ad` — stricter mapping/plugin/class-entry states.
- `c81b8a2556819f85332b7b527f09635fff9f1422` — explicit archive-index-incomplete findings and simpler annotation iteration.
- `d852da1ffd64d59dca1e9b4f3148077e244d4cfd` — synthetic analyzer tests.

The new `Forge1201MixinAnalyzerTest` covers declaration-source coalescing, config parsing, visible/invisible target annotations, class literal/string targets, mixin class missing, unsafe/missing/malformed config paths, explicit mapping absence, `remap=false`, mapped target missing, plugin non-execution, and refmap presence/absence. The user ran the forced `:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks` gate at `90daa1ee78d2de73ff44e27ffd01ba6e126858f4`; it passed (`BUILD SUCCESSFUL in 57s`, 14 actionable tasks, all 14 executed). The tests are validated within the documented outer-archive foundation scope.

## Regression test matrix

Use small synthetic ZIPs and ASM-generated class files; do not load or execute the fixture classes.

1. Manifest config path, heuristic-only path, metadata declaration, duplicate path, traversal path, missing declared file, malformed JSON, oversized config, and bounded-count/aggregate-byte limits.
2. Config package resolution and separate `mixins` / `client` / `server` class lists; missing class must be reported.
3. `@Mixin` visible/invisible annotations, multiple class-literal targets, string targets, remap=false, missing class, and mapping-not-found.
4. `@Shadow` field/method, prefix and aliases; `@Overwrite` descriptor match/mismatch; `@Accessor` getter/setter and `@Invoker` method with exact descriptor.
5. Each supported injector annotation; multiple target selectors; descriptor selectors and wildcard/quantifier syntax; explicit unsupported selector.
6. `@At` method and field selectors, target descriptor, opcode, ordinal, shift, args, owner mapping, and non-member codes such as HEAD/RETURN/CONSTANT.
7. `@Slice` from/to selectors and missing/unsupported boundaries.
8. Refmap present/absent/malformed and plugin present/absent. Assert plugin code is never loaded or executed.
9. Inherited members, exact descriptors, overloaded methods, incomplete hierarchy, absent target classes, and deterministic repeated analyses with isolated caches.
10. Nested source archive config and class provenance, duplicate nested resource entries, and archive traversal budget/unsafe-entry outcomes.
11. JSON/text consistency, stable ordering across differently ordered ZIP entries, and preservation of every warning/unknown state.

## Exit gates

1. Reusable state-free analyzer API and immutable result records exist; CLI delegates to that API.
2. Every discovered config is either parsed or represented by an explicit finding; no silent skip/swallow behavior.
3. Class/method/field target checks compare descriptors and preserve incomplete hierarchy separately from confirmed absence.
4. Supported annotation/selector cases have synthetic regression tests; unsupported/dynamic/plugin cases remain explicit.
5. Refmap/plugin resources are checked without executing plugin code.
6. Unified JSON/text preflight includes deterministic per-config findings and limitations.
7. Forced `:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks` passes.
8. Consolidated root/loader/test-mod clean build passes; runtime validation remains a separate gate.

## Exact next action

Before editing source, inspect the existing `Mixin1201Triage`, `Forge1201TargetApiVerifier`, archive traversal interfaces, target mapping inputs, and current test conventions against this design. Reuse the existing target verifier where possible; avoid cloning its hierarchy logic or creating another ZIP walker. Then implement the smallest state-free result API and synthetic tests first, wire it into the preflight result only after the API's isolated tests pass.

Keep the universal, reusable, modpack-ready connector vision. Palladium + Alien Evolution remain paused and isolated.


## Implementation update — 2026-11-14 — first unified preflight integration

The first unified integration increment is now committed on `feat/forge1201-preflight-analyzer`:

1. **Discovery provenance:** `Forge1201PreflightAnalyzer` retains all sources for a config path. The standard manifest `MixinConfigs` value maps to `MANIFEST`; outer-archive path/content candidates map to `RESOURCE_HEURISTIC`. Duplicate discoveries merge instead of overwriting. `FORGE_METADATA` remains reserved until a reliable metadata declaration source is implemented; the existing partial Forge TOML reader does not expose a Mixin declaration.
2. **Unified result:** `PreflightResult.mixinAnalysis` embeds `Forge1201MixinAnalyzer.Result`. Schema/analyzer versions are `1.4` / `1.4.0`. The deterministic text report prints Mixin config states, mixin-class findings, target selector findings, target-index availability, class-mapping version, and warnings. JSON includes the same immutable result.
3. **No-target-index behavior:** A null index is an explicit supported input. If a Forge/NeoForge target can be mapped but no target classpath was passed, the result is `TARGET_INDEX_UNAVAILABLE` and index status is `UNAVAILABLE_NO_TARGET_CLASSPATH`. It must never be mislabeled as a missing target class.
4. **Target classpath source:** The root `preflight1201` Gradle task passes the resolved root `sourceSets.main.compileClasspath` through a generated path-list file (4 MiB / 4,096 entry bounds and path validation). ModDevGradle's source-set compile dependency wiring places its Minecraft/NeoForge compile dependencies on that source set. The Java analyzer creates a per-run `URLClassLoader` with the platform loader as parent and reads only `.class` resources through `getResourceAsStream`. It never calls `loadClass`, defines, initializes, or executes target/mod/plugin classes. This compile classpath can also include other declared compile-only dependencies; the analyzer currently checks only the platform namespaces that its selector resolver recognizes.
5. **Mapping remains conservative:** No Minecraft 1.20.1-to-1.21.1 class mapping table is supplied in this increment. Minecraft/Mojang target selectors remain `TARGET_REMAP_UNRESOLVED`. Reviewed Forge-to-NeoForge mapping catalog entries can be resolved and their target class presence queried. No same-name identity mapping is inferred.
6. **Nested archive boundary:** Config discovery and Mixin class resource analysis are still outer-source-JAR only. Nested JAR traversal/config discovery is not implemented yet; it must reuse `Forge1201ArchiveGraph` snapshots and path provenance rather than adding a second ZIP walker.

### New tests (pending user Gradle verification)

- No classpath: mapped target status is `TARGET_INDEX_UNAVAILABLE`.
- Preflight integration: a synthetic manifest config and Forge `@Mixin(targets=...)` entry map `net/minecraftforge/fml/ModList` to `net/neoforged/fml/ModList`; a synthetic target class resource on the supplied compile-classpath directory produces `TARGET_CLASS_PRESENT`.
- Existing fixture checks merged manifest/heuristic provenance, heuristic-only provenance, parsed config entries, explicit missing mixin-class findings, schema 1.4, and unavailable index through the compatibility overload.

**Validation boundary:** the preceding user green checkpoint is still `90daa1ee78d2de73ff44e27ffd01ba6e126858f4` (`BUILD SUCCESSFUL in 57s`, 14/14 tasks executed). The current integration commits through `97d35ae727ecb63142bce9cdde714f4e4af98e6b` have not yet been locally pulled, compiled, or tested by the user. Run the forced loader test gate before adding further selector/member functionality.


### Scope corrections after the first integration commit

- The preflight limitations now distinguish the implemented mapped `@Mixin` target-class presence check from the still-future general source-API member verification, shim behavior checks, translated-resource comparison, and runtime feedback.
- The analyzer class comment now states that unified preflight consumes the result API; nested archive resources, member annotations, injection selectors, and semantic refmap remapping remain later work.
- Latest branch update for these corrections is `58d614608a105bb7daa53d0a576f20a0e69d2acb`; no user build/test has yet validated the Schema 1.4 code batch.


---

## Implementation checkpoint — descriptor-aware Mixin member annotation inventory (2026-11-14)

### Current increment

The reusable analyzer now emits an immutable `MemberAnnotationFinding` inventory for the four core Mixin member annotations:

- `@Shadow` on fields and methods.
- `@Overwrite` on methods.
- `@Accessor` on methods.
- `@Invoker` on methods.

Each finding preserves source archive/config/section/class provenance, source target selectors, annotation kind, field-vs-method element kind, exact source member name and JVM descriptor, prefix, aliases, explicit annotation value, Overwrite constraints, effective remap flag, inferred/explicit target-name candidates, operation shape, validation state, evidence, and warnings. Visible and invisible annotations are inspected on both fields and methods.

The current implementation deliberately **does not claim target member presence or target descriptor compatibility**. No complete versioned Forge 1.20.1 → NeoForge 1.21.1 member mapping is supplied. Remappable members remain `MEMBER_REMAP_UNRESOLVED`; `remap=false` is `MEMBER_REMAP_DISABLED_UNVERIFIED`; plugin-declared configurations produce `PLUGIN_DEPENDENT` where the annotation itself is otherwise structurally usable. Invalid element placement, descriptor, selector, or known annotation-value combinations are separate states.

### Contract details preserved

- A member annotation's remap default is inherited from class-level `@Mixin(remap=...)`, unless the member explicitly overrides it.
- `@Shadow` prefix stripping applies to methods only. An explicitly supplied prefix on a shadow field is warned and marked as an invalid annotation value; the field name is not stripped.
- Shadow aliases are retained and checked against the documented private-member restriction; Overwrite aliases are preserved without applying the Shadow-only private-member restriction.
- Accessor inference uses `get`, `is`, or `set` followed by an uppercase character. Invoker inference uses `call` or `invoke` followed by an uppercase character. Constructor invocation is represented from an explicit `<init>` or fully qualified target value, not inferred from a generic method name.
- Accessor getter/setter shape is inventoried from its exact source descriptor, and Accessor/Invoker methods must be abstract; these are source-side contract checks, not comparisons to target members or descriptors.
- `@Overwrite.constraints` is preserved as source annotation data.

The report schema/analyzer were bumped to `1.5 / 1.5.0`. JSON exposes `mixinAnalysis.memberAnnotations`; text output prints every finding's descriptor, candidates, remap state, prefix/aliases/value/constraints, and warnings. The limitation text explicitly says member target mapping/presence/descriptor comparison is not implemented.

### Synthetic fixtures added — pending user verification

ASM-generated fixtures and assertions cover:
- Shadow field and method, custom/default prefix data, aliases, and invalid explicit field prefix.
- Overwrite method descriptor, aliases, remap=false, and constraints.
- Accessor inferred getter, explicit setter, uninferrable name, uncapitalized prefix, and unsupported descriptor shape.
- Invoker inferred method and explicit constructor.
- Class-level `@Mixin(remap=false)` inheritance and a member-level `remap=true` override.
- Plugin-declared configurations retain a plugin-dependent state; plugin code is never loaded or executed.
- Schema 1.5 JSON/text report serialization and deterministic empty member findings in the base fixture.

Current source feature-branch HEAD after these source/test changes: `a118a81b69f6d94f81d02840c258ab26a84e7249`. **No Gradle test or build has yet been run against this increment.** The last user-confirmed green checkpoint remains the Schema 1.4 build at `afdf5c0b8b4410d0deb401683e60b0255843c58d`.

### Remaining Slice 4 work

The four annotation inventory is not complete Mixin analysis. Next increments remain:
1. Target member mapping and exact target descriptor/hierarchy checks when a reviewed versioned member-map source is available; otherwise keep the unresolved state.
2. Injector annotations and handler descriptors/selectors.
3. `@At` parsing including method/field selectors, args/opcode/ordinal/shift.
4. `@Slice` from/to boundary analysis.
5. Refmap data interpretation and remapping only with explicit versioned mappings.
6. Dynamic/unknown Mixin annotations and plugin-provided decisions, without executing plugin code.
7. Nested configs/classes by reusing `Forge1201ArchiveGraph` and its bounds/provenance; never add a second ZIP walker.



### Follow-up correction — annotation contract validation (2026-11-14)

The source/test batch has since been tightened:
- `@Accessor` and `@Invoker` methods are flagged when the source method is not abstract, matching the documented annotation contract.
- The private-alias restriction is applied to `@Shadow` aliases. `@Overwrite` aliases remain recorded but are not subjected to the Shadow-only private-member restriction.
- Test ASM mixin classes are abstract when they contain Accessor/Invoker methods; a separate concrete Accessor fixture verifies the explicit invalid-value state.

Latest feature-branch code/test commit: `a118a81b69f6d94f81d02840c258ab26a84e7249`. The current changes remain **pending user Gradle verification**.


## Implementation checkpoint — core injector handler/selector inventory (2026-11-14)

The analyzer inventories the six core SpongePowered Mixin 0.8.5 injector annotations: `@Inject`, `@Redirect`, `@ModifyArg`, `@ModifyArgs`, `@ModifyVariable`, and `@ModifyConstant`.

Each `InjectorFinding` preserves archive/config/section/mixin provenance, the exact source handler name and JVM descriptor, source `method` selector strings, and `target` `@Desc` selectors with owner/name/exact source descriptor and min/max match constraints. Common `MemberInfo` selector forms are decomposed into owner/name/descriptor and wildcard/quantifier fields while raw selector text remains authoritative; chained selector extensions are retained but explicitly marked unsupported.

The finding also preserves effective `remap`, explicit `@At` and `@Slice` counts, index/argsOnly/cancellable values, require/expect/allow, and constraints. Basic JVM handler and target method descriptor syntax is checked.

This increment does **not** compare handler signatures against target instructions, resolve targets through versioned 1.20.1→1.21.1 member mappings, validate target presence, parse `@At` values or `@Slice` boundaries, execute plugins, or claim injection success. Invalid annotation placement, malformed handler/selector descriptors, unsupported selector chains, plugin dependence, remap-enabled unresolved mappings, and remap-disabled-unverified cases remain explicit.

The preflight schema/analyzer versions are `1.6 / 1.6.0`; JSON includes `mixinAnalysis.injectors`, and text output reports handlers, selectors, and unresolved evidence. Synthetic ASM fixtures cover all six core injector kinds, overloaded and owner-qualified selectors, wildcard selectors, `@Desc`, match limits, `@At`/`@Slice` inventory counts, remap=false, malformed selectors, and invalid field placement.

### Next bounded increment

Implement source-side `@At` and `@Constant` inventory: preserve injection-point value, target selector, opcode, ordinal, shift/by, slice ID, args, remap, and the typed constant values/constraints used by `@ModifyConstant`. Parse method/field target selectors conservatively with exact descriptors. Do not resolve a target unless a reviewed versioned mapping and the shared `Forge1201TargetApiVerifier.TargetIndex` can prove it; preserve non-member injection-point codes such as HEAD/RETURN/CONSTANT as such. Then implement `@Slice` from/to boundaries, followed by refmap interpretation. Do not create a second hierarchy cache or ZIP walker.

---

## Implementation checkpoint — Schema 1.7 @At and @Constant source inventory (2026-11-14)

The analyzer now retains detailed source-side injection-point and constant discriminator metadata beneath each core injector finding.

### @At capture

Each @At finding preserves its annotation index, value, built-in-vs-custom/unknown classification, raw target, conservatively parsed owner/member/descriptor, optional nested @Desc selector, opcode, ordinal, shift/by, slice ID, named args, remap, ID, status, evidence, and warnings. Method and field descriptors are syntax-checked using JVM descriptor rules. Selector chains and malformed target forms remain unsupported/invalid instead of being guessed. These records do not claim that a target exists or that an instruction matches.

### @ModifyConstant capture

Each nested @Constant is represented separately with nullable explicit typed values for null/int/float/long/double/string/class, ordinal, slice ID, zero-condition enum values, and log. An absent discriminator array is represented explicitly as Mixin's implicit match-by-handler-return-type behavior. Conflicting typed discriminators and unknown enum/nested annotation values are reported rather than normalized away.

Preflight schema/analyzer versions are now `1.7 / 1.7.0`. JSON includes the nested records through `mixinAnalysis.injectors`; text output prints every captured @At and @Constant. The analyzer's class documentation and preflight limitations now state that @Slice from/to interpretation, target instruction resolution, target member mapping, handler stack compatibility, semantic refmap mapping, and nested config/class provenance remain future work.

Synthetic ASM tests cover a field target and exact field descriptor, opcode, ordinal, BY shift, shift amount, named arguments, slice ID, remap override, custom ID, nested @Desc method target, built-in HEAD defaults, each supported constant value type, implicit constant matching, and conflicting discriminators.

**Validation remains pending.** The user previously confirmed Schema 1.6 loader tests pass at `b661313f41460321845f617b4b81684f314eb614` (14/14 tasks, 50s). Schema 1.7 source and test changes have not yet been run locally. Do not treat this increment as green until the forced loader test output is reviewed.
### Schema 1.7 parser follow-up

A review correction preserves the valid owner-only `@At.target` selector form used by construction injection points. The parser no longer rejects the absent member name when an owner is present, and a synthetic ASM regression assertion covers this case. This source/test correction is included in the current Schema 1.7 validation batch; Gradle verification remains pending.
### Validation follow-up — corrected @At finding count

The first Schema 1.7 local loader run compiled successfully; 470 of 471 tests passed. The only failure was a stale expected count of three @At findings at `Forge1201MixinAnalyzerTest.java:350`. The fixture now contains four points: FIELD, INVOKE, HEAD, and owner-only NEW. Commit `fbe5b9e5739561bd561819ef26f05224c1baed2d` corrects the expected count to four. No production-code change was needed for this failure. The full Schema 1.7 test gate remains pending.

---

## 2026-11-14 — Schema 1.7 validation and Schema 1.8 @Slice boundary inventory

The user confirmed the Schema 1.7 forced loader test gate at commit 267db731f7b16b30498f3402daf83bcb340849d1: **BUILD SUCCESSFUL in 39s; 14 actionable tasks, all 14 executed.** The single earlier failure was a stale fixture count (expected three @At entries after a fourth owner-only NEW fixture was added); the assertion was corrected to four. No production parser defect was found in that failed run.

Schema 1.8 extends InjectorFinding with immutable SliceFinding entries. Each valid @Slice retains its index/ID and separate from/to boundaries. Each boundary records whether it was explicit or defaulted, the source annotation descriptor, its parsed AtFinding, status, evidence, and warnings. Omitted boundaries are materialized according to Mixin 0.8.5 defaults: from=@At("HEAD"), to=@At("TAIL").

Wrong nested annotation types, malformed boundary metadata, and unexpected value types are surfaced explicitly. The existing createAtFinding logic is reused; no second selector parser is introduced. Unified JSON serializes mixinAnalysis.injectors[].slices, and text output prints the slice and boundary records.

**Evidence limit:** the analyzer does not inspect target method instructions, infer their order, resolve selectors through refmaps/member mappings, or establish that a slice or injector will apply. It does not execute plugins and remains outer-source-JAR-only. Nested config/class provenance still needs to reuse Forge1201ArchiveGraph.

Synthetic tests cover explicit from/to selector metadata, omitted HEAD/TAIL defaults and provenance, unsupported nested boundary annotations, and JSON/text reporting. **The Schema 1.8 forced loader test gate is pending user execution.**


---

## 2026-11-14 — Schema 1.8 validated; next: semantic refmap contract

The user fast-forwarded the Universal branch to `4b4f9ce437671f08eafd3f9cf4a18e34a3d24284` and ran the forced loader gate:

```powershell
.\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks
```

**BUILD SUCCESSFUL in 33s; 14 actionable tasks, all 14 executed.** This validates the Schema 1.8 loader tests. It is not evidence that target instructions are found or any runtime Mixin applies.

The schema now preserves source-side @Slice index/ID, separate from/to records, explicit-versus-default provenance, nested AtFinding metadata, and explicit unsupported/invalid statuses. Mixin 0.8.5 omitted boundary defaults are recorded as `from=@At("HEAD")` and `to=@At("TAIL")`. The report serializes the nested slice findings in JSON and prints them in text output.

### Next bounded design task

Research the Mixin 0.8.5 refmap JSON contract and the actual runtime remapper's context/namespace lookup semantics before coding semantic interpretation. The design must identify raw resource discovery, JSON structure, context keys, source/target namespace assumptions, versioned mapping inputs, provenance, and explicit unknown/ambiguous/error states. A raw refmap entry is not itself proof of target member existence. Never guess a missing mapping from identical names; never execute Mixin plugins. Keep refmap interpretation distinct from target member/instruction resolution and runtime compatibility.


---

## 2026-11-14 — Mixin 0.8.5 refmap research completed; Schema 1.9 design authored

See docs/FORGE1201-PREFLIGHT-SCHEMA-1.9-REFMAP-DESIGN.md for the pinned source audit, JSON model, runtime lookup rules, resource selection, wrapper/plugin limits, proposed records/statuses, namespace provenance contract, and synthetic regression matrix.

The key runtime rule is that an absent requested context table falls back to base mappings, but a missing owner/reference inside an existing context table returns the original input and does not retry the base table. The design makes this distinction a mandatory test. Context labels are opaque obfuscation-type keys, not complete source/target namespace identities. The Mixin AP stores the internal mixin owner key and removes whitespace from its source reference key before writing.

The current analyzer checks only the refmap resource/JSON object; it does not parse its two mapping tables or preserve refmapWrapper. Effective resource selection also differs when a config omits refmap: Mixin may use its default mixin.refmap.json resource or ask its runtime plugin for a resource path.

**Next bounded step:** implement Schema 1.9A resource resolution and bounded inventory of mappings and each data context table. After that passes, implement Schema 1.9B exact owner-scoped lookup correlation for AP-emitted raw string references, with explicit runtime context input. Do not execute plugins/wrappers, simulate SRG runtime rewriting, or translate to NeoForge names without an explicit versioned mapping profile.
