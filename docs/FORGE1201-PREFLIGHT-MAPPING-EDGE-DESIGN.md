# Forge 1.20.1 → NeoForge 1.21.1 — Versioned Selector Mapping-Edge Design

**Status:** Design checkpoint following the validated read-only Intermediary feasibility audit at source commit 9dee3dca648b137590e8879eaf0705bc8422e2d4. No selector rewriting or runtime table generation is implemented by this document.

## 1. Objective and non-goals

Define a reusable, owner/member/descriptor-aware chain that can translate a Forge 1.20.1 Mixin selector/refmap value into the actual NeoForge 1.21.1 compile/runtime namespace. This is shared compatibility infrastructure, not a Symbiote-only or Palladium-only patch.

The chain must retain class, owner, member, and descriptor dimensions at every hop; preserve ambiguity and missing data as explicit results; and validate the final target against the actual target compile classpath. It must not treat a class/member name or an SRG token as a globally unique key.

This design does not enable selector rewriting, Mixin runtime remapping, or distribution of mapping payloads. The existing SRG-to-Mojang resource and its local-cache replacement remain a separate packaging/runtime milestone. No generated mapping payload is to be placed in the distributable connector JAR or modpack bundle.

## 2. Evidence-backed namespace graph

The candidate end-to-end path for a refmap value proven to use the Forge SRG namespace is:

1. Forge SRG 1.20.1 → Minecraft official-obfuscated 1.20.1.
2. Minecraft official-obfuscated 1.20.1 → Fabric Intermediary 1.20.1.
3. Fabric Intermediary 1.20.1 → Fabric Intermediary 1.21.1.
4. Fabric Intermediary 1.21.1 → Minecraft official-obfuscated 1.21.1.
5. Minecraft official-obfuscated 1.21.1 → the exact Mojang-named namespace used by the NeoForge 21.1.258 target compile classpath.

The namespace identifiers must be explicit and version-scoped. In particular, use distinct IDs such as minecraft:fabric-intermediary@1.20.1 and minecraft:fabric-intermediary@1.21.1. The Schema 1.10 validator treats an edge whose from/to IDs are identical as an explicit same-version identity declaration and rejects a version-changing identity edge. The cross-version Intermediary hop is therefore a data-backed mapping relation between two version-qualified nodes, not an unversioned self-edge.

This is only the Forge SRG refmap path. Other refmap contexts and the base mappings table can use different source namespaces; each binding must retain its own evidence-backed start namespace. A context label such as searge is an opaque refmap key until bound to the exact namespace by source-mod/refmap provenance.

## 3. Edge 1 — Forge SRG 1.20.1 to official-obfuscated 1.20.1

The pinned MCPConfig input is joined.tsrg with TSRG2 header tsrg2 obf srg id. It contains both obfuscated and SRG-side class/member names. The existing Intermediary audit currently reads the obfuscated class/member/descriptor columns and retains the SRG member token for diagnostics. The next source-side engine must retain the SRG names as first-class lookup dimensions rather than reducing them to one global member-ID table.

Required lookup keys and transforms:

- Class: exact SRG internal class name → one obfuscated internal class name. Detect duplicate/conflicting source aliases and fail closed.
- Method: exact SRG owner + SRG method name + full SRG descriptor → one obfuscated owner + obfuscated method name + obfuscated descriptor. Transform every object type in the descriptor through the SRG-to-obfuscated class map; preserve primitive types and array structure. Do not key methods by the SRG member token alone.
- Field: exact SRG owner + SRG field name. The joined TSRG2 field rows do not carry a descriptor in the format currently consumed. Join the mapped owner/name to the source Tiny inventory and accept a descriptor only when the owner/name candidate is unique. Zero or multiple candidates remain missing/ambiguous; never guess a descriptor.
- External descriptor classes must be identified separately from source Minecraft classes. Preserve a package-qualified external type only under the same conservative classification already validated by the Intermediary audit. Do not treat a pass-through external type as verified on the target classpath.

The current audit reports 188 SRG method tokens reused across distinct Intermediary symbols. This is concrete evidence against global SRG-token lookup. Owner plus member plus descriptor is mandatory for methods; owner plus member and a unique descriptor join is mandatory for fields.

The first implementation should use immutable structured keys and candidate lists. It should report conflicts, no matches, multiple matches, unresolved descriptor components, and external passthrough separately. A one-to-many or many-to-one mapping is never silently collapsed to the first row.

## 4. Edge 2 — source official-obfuscated 1.20.1 to Intermediary 1.20.1

Use the exact pinned Fabric Intermediary 1.20.1 Tiny v1 artifact. Its official side is the obfuscated source namespace used by the current MCPConfig join. Use:
- Classes: source official-obfuscated internal name → Intermediary class ID.
- Methods: exact owner + official member name + full official descriptor.
- Fields: exact owner + official field name, requiring a unique field candidate and retaining its descriptor.

The user's validated report records 7,413 of 7,436 MCPConfig classes joined to source Intermediary. Method joins are 34,390 of 48,575 rows; field joins are 31,003 of 31,003 rows. These are inventory join counts, not game/mod compatibility percentages.

## 5. Edge 3 — version-to-version Intermediary candidate relation

Join the source and target Tiny datasets using their Intermediary IDs, while retaining owner and descriptor context.

Classification must distinguish:
- class/member ID retained with same owner and fully normalized descriptor;
- same owner/member ID but descriptor changed;
- member ID retained only under a different owner;
- member ID missing in target;
- source or target descriptor unresolved;
- external descriptor passthrough (not exact);
- duplicate/ambiguous candidates.

Current validated measurements:
- Classes: 7,011 source Intermediary class IDs are shared with target; 402 source IDs are absent; 1,234 IDs are target-only.
- Methods: 20,131 exact target rows; 9,232 external-descriptor passthrough candidates; 1,686 descriptor-changed; 35 descriptor-unresolved; 108 owner-changed; 2,977 target member IDs absent; zero target ambiguities. Source mapping joins are 34,390/48,575; 36 source descriptors remain unresolved. There are 188 reused SRG method tokens across distinct Intermediary symbols.
- Fields: 22,813 exact target rows; 4,866 external-descriptor passthrough candidates; 791 descriptor-changed; 21 descriptor-unresolved; 110 owner-changed; 2,316 target member IDs absent; zero target ambiguities. All 31,003 source field rows join; 22 source descriptors remain unresolved.

External passthrough candidates are intentionally not exact. The audit has not established that those external types exist in the target compile/runtime classpath. Shared Intermediary IDs are candidate continuity evidence only, not semantic or behavioral compatibility.

## 6. Edge 4 — target Intermediary 1.21.1 to target official-obfuscated 1.21.1

Use the inverse side of the pinned target Tiny v1 artifact. Resolve target owner, member, and every descriptor class back to target official-obfuscated names. A missing reverse mapping, duplicate target candidate, changed descriptor, moved owner, or external class stays explicit. Do not promote a candidate to exact solely because its Intermediary ID appears on the target side.

## 7. Edge 5 — target official-obfuscated to the actual NeoForge compile namespace

The final target namespace must match the actual class names present in the resolved NeoForge 21.1.258 compile classpath used by Forge1201PreflightAnalyzer's target classloader.

The candidate source is Minecraft 1.21.1's version-pinned official Mojang client mapping dataset, or the exact NeoForm/ModDevGradle-produced mapping artifact used to construct the target compile classpath. This choice is not yet verified. Before implementation:
- inspect the target classpath and build mapping pipeline to identify which mapping namespace it actually uses;
- pin the exact artifact coordinate/URL, version, internal entry, upstream checksum, and SHA-256 of the bytes consumed;
- verify mapping direction from official-obfuscated names to the target Mojang-named names;
- test class, member, and descriptor conversion independently.

Do not assume that a mapping file named official, named, or Mojang necessarily matches the runtime classpath. Namespace labels must describe the actual bytes/names, not just a tool's informal terminology.

## 8. Reuse the existing target class index

Forge1201PreflightAnalyzer already creates one Forge1201TargetApiVerifier.TargetIndex from the resolved target compile classpath and passes that index to Mixin analysis. The current call supplies an empty class map and a not-supplied mapping status, so no cross-version mapping is currently applied.

Reuse this exact per-analysis TargetIndex when mapped target candidates become available. It already provides:
- read-only class metadata through classInfo;
- exact owner/name/descriptor lookup for methods and fields;
- inherited member search using the existing hierarchy cache;
- distinct FOUND, MISSING, and INCOMPLETE_HIERARCHY results.

The index verifies a target candidate; it does not generate the candidate, translate a namespace, or prove semantic/gameplay correctness. Do not build a second hierarchy cache or a parallel target class index.

## 9. Separate profile connectivity, edge coverage, and per-selector resolution

Schema 1.10 has three important current limitations:

1. Equal from/to namespace IDs are treated as same-namespace identity declarations and require the same Minecraft version. Version-qualified Intermediary node IDs are mandatory for the cross-version bridge.
2. An edge marked VERIFIED is structurally checked for supported direction/scope, review evidence, and referenced artifacts that pass digest/license-review checks. The validator does not parse mapping rows or prove the declared mapping's actual completeness.
3. The current path evaluator counts complete graph paths using VERIFIED and explicit identity-not-applied edges; it excludes PARTIAL edges. A complete graph path is not proof that every selector in a JAR is mapped.

Keep three separate reports/gates:
- Profile/artifact gate: strict profile structure, namespace/version continuity, pinned bytes, review evidence, and an unambiguous candidate route.
- Edge-data gate: parsed symbol inventories and actual class/member/descriptor coverage, collision policy, exact/partial/missing/ambiguous counts, external passthrough, and input/output fingerprints.
- Per-selector gate: for each exact selector, every required edge must produce one unique, descriptor-complete result. A globally PARTIAL dataset may still contain a valid row for one selector, but no selector may pass by assuming missing rows or by treating the overall edge as complete.

Do not weaken the existing PROFILE_VALIDATED or completePathCount semantics. Add an explicit schema revision or additive, regression-tested coverage model before asking the profile validator to reason about per-symbol applicability. Unknown edge fields are warnings in the current schema; merely adding a coverage object to JSON today would not make it enforced.

## 10. Artifact provenance and generated output

Each mapping edge must record:
- exact input artifacts and versions;
- exact archive and extracted-entry hashes;
- format and parser version;
- mapping direction and source/target namespace identities;
- declared symbol scope;
- counts for classes, methods, fields, descriptors, and all unresolved/ambiguous categories;
- output fingerprint for any derived mapping dataset;
- known exclusions and a deterministic failure policy.

The build-local audit and later derived mapping datasets stay outside the distributable loader JAR. The runtime cache/loader must be designed separately with bounded, hash-verified inputs, atomic cache writes, versioned cache keys, and explicit offline cache-miss handling. Do not remove the currently packaged srg-mojang.tsv until its runtime consumer is replaced and the archive-inspection regression is in place.

## 11. Staged implementation plan

1. **SRG-to-obfuscated source edge:** extend the TSRG2 reader to retain SRG class and member names and build owner/member/descriptor-qualified maps. Join descriptorless fields to the source Tiny field inventory only when unique. Add deterministic synthetic classfile-independent mapping tests for overloads, duplicated SRG tokens, nested/array descriptor classes, external types, and ambiguity.
2. **Intermediary edge composition:** consume the already pinned source/target Tiny datasets and expose a structured per-symbol candidate result, reusing the validated audit classifications rather than maintaining a second parser or duplicate set of counters.
3. **Target named mapping:** identify and pin the actual target namespace mapping artifact, then map the target official-obfuscated candidate to the actual compile namespace.
4. **Target verification:** use the existing TargetIndex to report final target owner/member/descriptor lookup and hierarchy completeness.
5. **Profile integration:** add version-qualified Intermediary nodes and a separately enforced per-symbol coverage/resolution model. Keep schema changes explicit and covered by compatibility tests.
6. **Mixin integration:** only after each selector can be resolved uniquely end-to-end, integrate the mapping result into Mixin analysis. Keep dynamic targets, unsupported selector forms, and ambiguous references unresolved.

## 12. Acceptance criteria and exclusions

The mapping-edge work is accepted only when:
- all input bytes are version-pinned and hash-verified;
- the SRG-to-obfuscated lookup is owner/member/descriptor-aware;
- the Tiny source and target joins are deterministic and ambiguity-safe;
- external descriptors remain distinct from missing Minecraft classes;
- source/target mapping gaps and changed signatures are reported rather than silently guessed;
- each selector is transformed only when every edge returns one complete result;
- final candidates are checked against the actual target classpath through the existing TargetIndex;
- regression tests cover every classification and generated reports are deterministic.

This stage does not implement Mixin selector rewriting, injectors, @At/@Slice transformations, runtime mapping injection, or actual Fabric Loader/mod support. It does not change the Universal roadmap's scope or resume Palladium/Alien Evolution.

## 13. Implementation checkpoint 1 — SRG-to-obfuscated source edge

**Status:** First read-only implementation published and user-validated by the forced loader test gate at source commit `2c8c4f8ebbcd3e67fe496c73326a4c64a64e2547`. It is not wired into Mixin selector rewriting or the runtime remapper.

Files in this checkpoint:
- `Forge1201SrgObfuscatedBridgeAudit.java`: bounded TSRG2/Tiny v1 reader, explicit SRG↔obfuscated class aliases, owner/name method index, full-descriptor conversion, and unique Tiny owner/name field descriptor join.
- `Forge1201SrgObfuscatedBridgeAuditTest.java`: synthetic tests for overloads, repeated SRG member tokens on different owners, nested/array descriptor conversion, external descriptors, missing Minecraft descriptor types, ambiguous class aliases, ambiguous/missing Tiny field descriptors, and conflicting full method selector candidates.
- `loader/build.gradle`: `:loader:auditSrgObfuscatedBridge1201` emits a build-local report at `loader/build/reports/mappings/1.20.1/srg-obfuscated-bridge-audit.json`. The normal loader `test` task depends on this audit alongside the existing Intermediary audit.

Resolution contract:
- Class lookups are SRG internal name → exactly one official-obfuscated internal name; no or multiple matches remain missing/ambiguous.
- Method lookups require SRG owner + SRG member name + complete source descriptor. Every object descriptor type must be mapped through the source class edge, except package-qualified external candidates which are preserved and flagged. Known or package-qualified `net/minecraft/` types without a mapping remain unresolved.
- A global `m_N_` token is never used as a lookup key. The owner and full descriptor are always required.
- TSRG2 field rows do not carry descriptors. The implementation joins their obfuscated owner/name to the pinned source Tiny field inventory and only accepts exactly one distinct field descriptor. Zero is missing; multiple descriptors are ambiguous.
- An external descriptor may yield a separate `MAPPED_WITH_EXTERNAL_TYPES` candidate; it is never represented as a fully exact mapping.
- The index and report are build-local. No mapping TSV/JSON is added to JAR inputs, no selector is modified, and no runtime table is generated.

Validation boundary:
- User ran `:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks` at commit `2c8c4f8ebbcd3e67fe496c73326a4c64a64e2547`: **BUILD SUCCESSFUL in 1m 3s; 21/21 actionable Gradle tasks executed**. The run compiled the source/tests, executed both read-only mapping audits, generated the existing SRG→Mojang provenance table, and completed `:loader:test`. The first attempt at parent `d0bf5abe` failed before audit parsing because the JavaExec classpath omitted ASM; the classpath correction is included in the validated commit.
- This first edge does not yet validate the source selector namespace for every refmap context. A source mod's refmap context must still be explicitly bound to the correct namespace by evidence.
- The target 1.21.1 official-obfuscated → actual NeoForge compile namespace edge remains separately unverified.


### Build-task runtime classpath correction — 2026-11-15

The first user-run forced loader test reached `:loader:auditSrgObfuscatedBridge1201` but the forked Java process failed with `NoClassDefFoundError: org/objectweb/asm/Type`. The task's `JavaExec` classpath contained only `sourceSets.main.output`, which contains connector classes but omits the ASM libraries declared through the loader's `api` dependencies.

The task now uses `sourceSets.main.output + configurations.runtimeClasspath`. This supplies its declared runtime dependencies (including ASM) without inheriting the generated mapping directories that have separately been appended to `sourceSets.main.runtimeClasspath`. The change is limited to this build-local audit task: it does not alter loader runtime behavior, package mapping data, or change the audit's read-only contract.

**Validation:** the corrected task wiring is published but awaits the user's repeated forced `:loader:clean :loader:test` run. The previous run did not reach the new audit's parsing or report generation, so no coverage result is available yet.


### 13.2 User validation result — 2026-11-15

The user supplied the complete forced loader console and the generated `srg-obfuscated-bridge-audit.json` from source commit `2c8c4f8ebbcd3e67fe496c73326a4c64a64e2547`.

**Build gate:** `:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks` completed **BUILD SUCCESSFUL in 1m 3s; 21 actionable tasks: 21 executed**. No exact JUnit test count is inferred from this Gradle summary.

**Pinned audit inputs:**
- MCPConfig `joined.tsrg`: 5,653,149 bytes; SHA-256 `19ae000175bfb58e5b26406c31a0a501bfb40000b078fee2f00ea60a8c7ed453`.
- Fabric Intermediary 1.20.1 Tiny v1: 3,110,223 bytes; SHA-256 `5a808bd1ebc4a9ee7d1c5900dbd2bf391586b536c3d86fbf2f972047faa2d165`.

**Observed source-edge coverage:**
- Classes: 7,436 TSRG2 rows; 7,436 uniquely mapped; zero missing SRG names; zero ambiguous SRG aliases; zero ambiguous obfuscated aliases.
- Methods: 48,575 rows; all 48,575 owners indexed; zero owner-unmapped rows; zero descriptor-unresolved rows; 13,330 external-descriptor rows containing 19,579 external type occurrences; 47,104 owner/name groups, of which 1,397 are overloaded; zero duplicate full selector keys.
- Fields: 31,003 TSRG2 rows; all 31,003 joined uniquely to a Tiny field descriptor; zero missing/ambiguous Tiny joins; zero owner-unmapped or descriptor-unresolved rows; 5,722 external-descriptor rows/occurrences.

**Interpretation:** the tested parser/index has complete class/owner row joins for these pinned source inputs, and the synthetic regressions plus build task execute successfully. The report remains `PARTIAL` correctly: external descriptor types are pass-through candidates, not proved target-classpath members; the cross-version Intermediary hop and final target compile-namespace hop are separate; and selector transformation/runtime table generation remain false. This does not authorize promotion to a complete selector path or runtime remapper.

**Next engineering increment:** expose per-symbol, owner/member/descriptor-qualified lookup across the existing source and target Tiny datasets and compose it with `Forge1201SrgObfuscatedBridgeAudit.Index` without duplicating the Tiny parser or the target hierarchy cache. Separately identify the exact official-obfuscated 1.21.1 → names actually present in the root ModDevGradle-resolved `sourceSets.main.compileClasspath`; verify the consumed mapping artifact/direction/hashes instead of relying only on build comments or informal namespace labels. Final candidate lookups must reuse the existing `Forge1201TargetApiVerifier.TargetIndex`. Keep selector rewriting disabled until every edge resolves uniquely for a selector.


## 14. Implementation checkpoint 2 — per-symbol SRG-to-Intermediary composition

**Status:** implemented and user-validated by the forced full loader test at source commit `ad2eaa85d7ad2934cd3f0efbeac8c84013fa1260`. The synthetic TSRG/Tiny field-name mismatch and fixture correction are historical notes below.

- `Forge1201IntermediaryBridgeAudit.Index` exposes owner/member/full-descriptor queries over its existing parsed source and target Tiny v1 data, while the aggregate report remains available from the same index.
- `Forge1201SrgIntermediaryBridgeResolver` composes Forge SRG→source official-obfuscated mappings with the source/target Intermediary relation. It provides explicit class, method and field result states for missing/ambiguous source entries, moved owners, missing target IDs, unresolved/changed descriptors, and external passthrough.
- The candidate namespace ends at target official-obfuscated 1.21.1. No final Mojang-name conversion, target classpath verification, selector rewrite, or runtime mapping output is implemented.
- Method candidates preserve the exact caller-supplied SRG descriptor. The descriptor is forward-normalized for matching but is no longer reverse-normalized again to reconstruct the original input.
- Synthetic tests cover exact class/method/field composition, external non-exact handling, target descriptor changes, moved owners, and missing target IDs. The source-only SRG audit tests continue to cover overloads, reused tokens, and source-field descriptor ambiguity.
- This increment adds no new Tiny parser and no target class hierarchy cache. The two existing read-only indexes remain responsible for their respective edges.
- Validation is pending; do not declare this increment green until the user's forced loader test passes.

### 14.1 Target compile namespace evidence

The root build uses ModDevGradle `2.0.148`, NeoForge `21.1.258`, and Parchment configured for Minecraft `1.21.1` with mappings version `2024.11.17`. The analyzer receives the resolved root `sourceSets.main.compileClasspath` as its actual target class-resource source.

NeoForged documentation describes Parchment as supplemental parameter names and Javadocs layered onto Mojang's official names, rather than a distinct class/member namespace. This supports the working target namespace identity `minecraft:official-mojang@1.21.1`. The exact 1.21.1 official mapping bytes, checksum, direction, and actual artifact/classpath agreement still require a pinned local-byte audit. Do not enable the final mapping edge from documentation alone.


### 14.2 First forced validation result and synthetic fixture correction

The user's first forced run at source commit `90f5f24c3be97c9748a798aa0ec3fd171826785d` completed compilation and both build-local mapping audit tasks, then failed in `:loader:test`: **515 tests completed, 1 failed**. The failure was `Forge1201SrgIntermediaryBridgeResolverTest.resolvesClassMethodAndFieldThroughBothSourceEdges()`, at the field-resolution assertion.

The failure is a test-fixture mismatch, not a production resolver defect. The synthetic TSRG2 row named the field's official-obfuscated name `f` (`\tf f_20_ 3`), while the source Tiny v1 row for the same owner/member ID named that field `x` (`FIELD a Lb; x field_1`). The SRG bridge correctly joins descriptorless TSRG2 field entries to Tiny by owner plus official-obfuscated field name; `f` and `x` cannot join.

The fixture uses `\tx f_20_ 3`, aligning the TSRG2 official-obfuscated field name with the source Tiny row while retaining the SRG name `f_20_`. The expected source `x` → target `y` composition assertions remain intact. The user subsequently passed the full forced loader test at `ad2eaa85d7ad2934cd3f0efbeac8c84013fa1260`; no production Java behavior was changed by this fixture correction.


## 15. Implementation checkpoint 3 — deterministic composed per-symbol report

**Status:** implementation and regression test published and user-validated by the full forced loader test at `ad2eaa85d7ad2934cd3f0efbeac8c84013fa1260`.

- Forge1201SrgObfuscatedBridgeAudit.Index exposes deterministic class, method and field selector inventories from the same bounded TSRG2/Tiny inputs. Every parsed row is retained, including missing/ambiguous owner, missing/ambiguous field Tiny join, and unresolved descriptor cases.
- Forge1201SrgIntermediaryBridgeAuditReport composes those rows through Forge1201SrgIntermediaryBridgeResolver and writes deterministic JSON with one record per source class, method and field entry, status histograms, namespace identities, and SHA-256/size provenance for all three pinned artifacts.
- The report remains PARTIAL; completeForgeSrgSelectorPath, selectorTransformationPerformed, and runtimeMappingTableProduced are explicitly false. It ends at target official-obfuscated 1.21.1. External descriptor types are not checked against the target classpath.
- The build-local task auditSrgIntermediaryBridge1201To1211 is a JavaExec dependency of loader:test. Output: loader/build/reports/mappings/1.20.1-1.21.1/srg-intermediary-composed-audit.json. It is not a JAR input and no runtime mapping consumer is changed.
- Diagnostic external-type counts are summed from source and target descriptors, including non-exact candidates.
- Forge1201SrgIntermediaryBridgeAuditReportTest checks byte-for-byte determinism, row inventory sizes, exact synthetic class/method/field composition, and the explicit no-transformation boundary.

The next step after this gate is to pin the exact 1.21.1 official-obfuscated -> actual NeoForge compile-namespace mapping bytes, then verify composed candidates through the existing Forge1201TargetApiVerifier.TargetIndex. Do not enable selector rewriting from this report alone.


## 16. Observed real-input ambiguity — regression locked after composed report review

**Observed at source checkpoint d0b215dc** in the full pinned composed report:

- Forge SRG selector: owner `net/minecraft/src/C_507_`, method `m_8061_`, descriptor `(Lnet/minecraft/src/C_516_;Lnet/minecraft/src/C_1391_;)V`.
- Source official-obfuscated selector: owner `bfj`, name `a`, descriptor `(Lbfo;Lcfz;)V`.
- Source Intermediary identity: owner `net/minecraft/class_1297`, method ID `method_5673`, descriptor `(Lnet/minecraft/class_1304;Lnet/minecraft/class_1799;)V`.
- The target Tiny inventory contains two rows with that method ID and equivalent Intermediary descriptor, under distinct owners `net/minecraft/class_9460` and `net/minecraft/class_1309` (target official-obfuscated owners `btb` and `btn`).
- Neither target owner equals the source Intermediary owner `net/minecraft/class_1297`; the resolver therefore reports `INTERMEDIARY_TARGET_AMBIGUOUS` and preserves both candidates. This is not a reason to select by name or row order.

A synthetic regression mirrors the observed owner/member/descriptor relation. It requires the ambiguous status, exactly two candidates, both target owners, the shared source Intermediary ID, and the complete target Intermediary descriptor. The regression passed as part of the full forced loader test at `ad2eaa85d7ad2934cd3f0efbeac8c84013fa1260`.


## 17. Target compile-classpath API inventory

**Status:** Read-only inventory tool and Gradle task implemented and validated by the user. The inventory ran against the resolved root compile classpath and completed with 7,942 unique Minecraft classes, 7,942 resolved classes, zero missing classes, 71,727 methods, 36,227 fields, and 4,201 unique referenced Minecraft types with zero missing referenced types.

- The user's direct root compile-classpath entry is `build/moddev/artifacts/neoforge-21.1.258.jar`, SHA-256 `E5B8CC08A21CFD11F16FA62869051E97587D05D95980BA5C9FB2387BB9324F32`. A prior root Gradle diagnostic found 103 entries and zero direct matches for `neoforge-21.1.258-merged.jar`.
- User `javap` checks on the direct compile JAR verified Mojang-official anchors across `Entity`, `Level`, `BlockPos`, `ItemStack`, and `BlockState`. The `BlockState` hierarchy check located `getBlock()`, `isAir()`, and `getFluidState()` on `BlockBehaviour$BlockStateBase`; their descriptors are recorded in the testbench continuation handoff.
- Added `Forge1201TargetApiInventory`, a read-only tool that enumerates `net/minecraft/**/*.class` occurrences across the ordered root compile classpath, records duplicate class resources and first-resource resolution, and obtains class hierarchy/member/descriptor metadata from one existing `Forge1201TargetApiVerifier.TargetIndex`.
- The report fingerprints classpath entries that actually contain Minecraft class entries, inventories declared methods and fields with JVM descriptors, summarizes Minecraft superclass/interface/descriptor references, and lists unresolved or missing referenced Minecraft types rather than silently dropping them.
- Added `inventoryTargetApi1211`, a root Gradle JavaExec task. It hands the exact ordered `sourceSets.main.compileClasspath` to the loader tool through a generated path-list file, avoiding long Windows command lines. An explicit ordered-classpath input is declared because resource precedence matters.
- Added synthetic tests for deterministic output, duplicate-class resolution order, class/member/descriptor inventory, missing referenced Minecraft types, and directory classpath entries.
- All generated inventory outputs remain local under `build/reports/target-api/1.21.1/`; they are not committed or packaged. The generated report is an engineering audit artifact, not a selector mapping or a distribution permission.
- **User validation (2026-11-15):** `inventoryTargetApi1211` completed successfully against the exact root compile classpath; the full counts are recorded above and in the continuation handoff.
- The tool does not transform selectors or alter runtime behavior. Reuse the same TargetIndex architecture and keep ambiguity/missing descriptors explicit.



## 18. Target official-obfuscated → Mojang-official edge — TSRG2 descriptor namespace and read-only index

**Status:** The bounded read-only parser and synthetic regression suite are published and user-validated. The three initial failures were fixture nesting mistakes: TSRG2 member rows are attached to the most recent class row. The corrected fixtures are in `ad2eaa85d7ad2934cd3f0efbeac8c84013fa1260`; focused tests and the full forced clean loader test passed there. This remains separate from selector rewriting and runtime mapping consumption.

### Verified format and descriptor direction

The locally fingerprinted 1.21.1 mapping output has header `tsrg2 left right`. Observed class rows include `bsr net/minecraft/world/entity/Entity` and `a com/mojang/math/Axis`; the official client mapping file independently shows `net.minecraft.world.entity.Entity -> bsr` and `com.mojang.math.Axis -> a`. The member anchor `q_ ()I getTeamColor` agrees with the Mojang `getTeamColor() -> q_` row and the target inventory's `Entity.getTeamColor()I`.

A further implementation-level detail is now verified from NeoForged SRGUtils source:

- `MappingFile(NamedMappingFile, from, to)` retains each field/method descriptor from the `from` namespace.
- The TSRG2 writer emits the original descriptor between the original and mapped member names.
- `getMappedDescriptor()` uses `remapDescriptor`, which converts every object type through the class mappings.
- NeoForged InstallerTools' `MergeMappings` builds this output with `IMappingFile.merge` and writes TSRG2.

References:
- [NeoForged SRGUtils MappingFile.java](https://github.com/neoforged/SRGUtils/blob/main/src/main/java/net/neoforged/srgutils/MappingFile.java)
- [NeoForged InstallerTools MergeMappings.java](https://github.com/neoforged/InstallerTools/blob/main/src/main/java/net/neoforged/installertools/MergeMappings.java)
- [NeoForged InstallerTools MergeMappings test](https://github.com/neoforged/InstallerTools/blob/main/src/test/java/net/neoforged/installertools/MergeMappingsTest.java)

Therefore method keys must use **left obfuscated owner + left member name + left JVM descriptor**. The right descriptor is not the descriptor text stored in a TSRG2 row; it must be produced by translating every object type in that left descriptor through the class mapping table. A reader that converts only the owner/member name would emit invalid target descriptors.

### Read-only index contract

`Forge1201TargetOfficialMappingIndex` currently:
- Requires the exact `tsrg2 left right` header and parse top-level class rows plus one-level member rows. Package rows and deeper parameter/metadata rows are not treated as class/member entries.
- Retains source class/member candidates and preserve conflicting rows as explicit owner/member ambiguity instead of selecting a first match.
- Resolves methods by exact left owner/name/descriptor. Resolve fields by left owner/name, checking a row descriptor when present; TSRG2 field rows may omit descriptors, so the caller must supply the already-resolved left descriptor.
- Translates object descriptors, including array element types. Package-qualified object types absent from the Minecraft class map remain explicitly counted external candidates, not exact proof of classpath presence. Unqualified missing obfuscated classes remain unresolved; multiple class targets make a descriptor ambiguous.
- Enforces a bounded input size, strict UTF-8 decoding, retain input byte size and SHA-256, and offer an expected-SHA-256 read overload.
- Adds only read-only parsing/tests. It does not infer cross-version identity, alter `Forge1201SrgIntermediaryBridgeResolver`, rewrite selectors, emit a runtime table, or replace/rebuild `Forge1201TargetApiVerifier.TargetIndex`.

### Next integration and validation boundary

The existing first-four-edge resolver still returns target official-obfuscated candidates. This integration is now implemented in `Forge1201TargetOfficialBridgeAudit` and uses the existing per-analysis `Forge1201TargetApiVerifier.TargetIndex`. See checkpoint 19 below. Do not create another target hierarchy cache. External descriptor types remain explicitly non-exact.

The mapping artifact itself stays in the local NeoForm Gradle cache. Do not commit, upload, package, or otherwise redistribute its contents or a copied derived mapping payload. The locally observed content fingerprint belongs in the evidence record only; licensing review remains a separate gate.


## 19. Implementation checkpoint 4 — final target namespace and compile API audit

**Status:** End-to-end read-only audit, synthetic regression tests, and an opt-in Gradle task are published at source branch `feat/forge1201-preflight-analyzer`, current code checkpoint `442785d1619847fb8e29fc4f1a2b5f668f900623`. The new increment awaits user validation. The last user-validated source checkpoint remains `ad2eaa85d7ad2934cd3f0efbeac8c84013fa1260`.

### Composition and safety properties

- `Forge1201TargetOfficialBridgeAudit` composes the existing `Forge1201SrgIntermediaryBridgeResolver` through `Forge1201TargetOfficialMappingIndex`, then checks the resulting Mojang-official owner/name/full descriptor against the target compile API.
- The report retains upstream statuses and every ambiguous upstream candidate. An ambiguous Intermediary target relation stays ambiguous even if one candidate class is absent from the target compile classpath; per-candidate final mapping and API results are retained for diagnosis rather than used to pick a winner.
- Class mapping, target compile-class presence, target TSRG2 member mapping, and hierarchy-aware target member lookup are separate results. Member lookup distinguishes exact structural match, missing owner class, missing member, and incomplete hierarchy.
- Both upstream and final-edge external descriptor counts are retained independently. A found member with any external descriptor types is reported as `TARGET_API_MEMBER_FOUND_WITH_EXTERNAL_TYPES`, never as an exact mapping.
- One `Forge1201TargetApiVerifier.TargetIndex` instance is created for the audit and reused for every class, method, and field. The existing superclass/interface traversal and cache are reused; no second hierarchy cache is created.
- The target classpath order comes from the same generated `target-classpath.txt` used by `inventoryTargetApi1211`. The audit loads class resources through a URLClassLoader with the platform classloader parent, parses bytes only, and does not define or initialize target classes.
- The target merged TSRG2 is read directly from the local NeoForm Gradle cache. The Gradle task pins the observed SHA-256 `7BD3E2BE95FA246778B90B078DBB78B5D1A7A98853BAEA5E1321A6C3D5383929`; an alternate path can be supplied with `-PtargetOfficialTsrg1211=<path>` and an alternate expected digest with `-PtargetOfficialTsrg1211Sha256=<sha256>`. No mapping bytes are copied into the repository.
- Input artifact paths, file sizes, and hashes are recorded. Regular target classpath files are fingerprinted; directory entries are represented by ordered path and kind. Gradle also tracks the full ordered target compile classpath as task inputs so in-place dependency changes invalidate the report.

### New task and output

Run from the repository root:

```powershell
.\gradlew.bat :loader:auditSrgTargetApiBridge1201To1211 --no-daemon --console=plain
```

The task depends on the source mapping extraction tasks and `:inventoryTargetApi1211`. Its build-local output is:

`loader/build/reports/mappings/1.20.1-1.21.1/srg-target-api-bridge-audit.json`

The report declares `selectorTransformationPerformed=false` and `runtimeMappingTableProduced=false`. It is not packaged in the loader JAR and is not a selector rewrite table.

### Synthetic regressions

`Forge1201TargetOfficialBridgeAuditTest` covers:
- method and field mapping through both cross-version and final target namespace edges;
- inherited interface method lookup through the shared `TargetIndex`;
- preservation of two ambiguous target candidates when only one candidate's class exists on the target API provider; and
- an exact target member signature whose external descriptor type counts remain nonzero and therefore non-exact.

### Validation still required

The source changes are published but **not yet tested by the user**. First run the focused class `org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201TargetOfficialBridgeAuditTest`, then the forced clean `:loader:test` gate. Only after those pass, run `:loader:auditSrgTargetApiBridge1201To1211` and inspect the report's status histograms and ambiguous/external/incomplete rows. Do not interpret a generated report as runtime compatibility proof.

No selector rewriting, runtime mapping table, or license conclusion is authorized by this checkpoint.


### 19.1 Audit task tool-classpath correction — 2026-11-15

The first user invocation of `:loader:auditSrgTargetApiBridge1201To1211` completed target API inventory successfully (7,942 unique classes resolved, 71,727 declared methods, 36,227 declared fields, and 4,201 referenced Minecraft types with none missing), then failed before audit/report generation:

`NoClassDefFoundError: com/google/gson/GsonBuilder`

The failure was isolated to the forked JavaExec classpath. `Forge1201TargetApiInventory` has a static Gson serializer, and the bridge audit calls its shared `readClasspathFile` helper. Gson is intentionally `compileOnly` in the loader because NeoForge supplies it in the actual loader runtime. The inventory task already supplies Gson/SLF4J explicitly from the loader compile classpath for its standalone tool process; the new audit task initially supplied only main output plus `configurations.runtimeClasspath`, which excludes compile-only jars.

The audit task now adds the same narrowly filtered compile-classpath entries (`gson*` and `slf4j*`) to its standalone JavaExec classpath, in addition to main output and the ordinary runtime dependencies. This is a build-tool classpath correction only: no dependency scope or shipped loader runtime classpath changed, no mapping data was copied, and no report/selector/runtime mapping was produced by the failed attempt.

Source correction commit: `9f50f0af1c79a24e02d59ba3a983eb3581655317`.

**Validation status:** awaiting a repeat invocation of `:loader:auditSrgTargetApiBridge1201To1211`. The focused `Forge1201TargetOfficialBridgeAuditTest` had already passed at parent `37bc531f8b629402b2ae0a6382e30554f7f08484` before this Gradle-only classpath correction. The previous audit attempt did not enter the composed audit or produce `srg-target-api-bridge-audit.json`; inspect the report only after the corrected task succeeds.


### 19.2 User validation — target API audit task executes end to end (2026-11-15)

After the JavaExec tool-classpath correction at `9f50f0af1c79a24e02d59ba3a983eb3581655317`, the user reran `:loader:auditSrgTargetApiBridge1201To1211` at source HEAD `9f8afd12e1383fc7c00cc1db951695e0b21dc4de`.

**Observed result:** `BUILD SUCCESSFUL in 27s; 18 actionable tasks (8 executed, 10 up-to-date)`.

The audit printed:
- classes: 7,436
- methods: 48,575
- fields: 31,003
- report: `loader/build/reports/mappings/1.20.1-1.21.1/srg-target-api-bridge-audit.json`
- explicit boundary: read-only audit; no selector transformation or runtime mapping table was produced.

The prerequisite target API inventory was up-to-date in this run; in the prior run it had completed with `INVENTORY_STATUS=COMPLETE` (7,942 unique classes resolved, no unresolved metadata, 71,727 methods, 36,227 fields, 4,201 referenced Minecraft types, zero missing referenced types). The focused new audit test had also passed at parent `37bc531f8b629402b2ae0a6382e30554f7f08484`.

**Interpretation:** the final audit pipeline now executes through report generation. The headline class/method/field numbers are inventory row counts, not exact-match totals. The JSON `coverage.*.byStatus` histograms and candidate-level fields (`upstreamExternalDescriptorTypeCount`, `targetExternalDescriptorTypeCount`, `targetOfficialMappingStatus`, `targetApiStatus`, and `hierarchyVisited`) must be inspected before drawing conclusions about coverage. Preserve ambiguous candidates and external descriptor classifications. Do not enable selector rewriting, runtime mapping production, or claim binary/gameplay compatibility from task success.


### 19.3 User report inspection — first end-to-end coverage histograms (2026-11-15)

The user loaded `loader/build/reports/mappings/1.20.1-1.21.1/srg-target-api-bridge-audit.json` in Windows PowerShell at the successful audit checkpoint. The report metadata confirms:
- `status=READ_ONLY_STRUCTURAL_AUDIT`
- `selectorTransformationPerformed=false`
- `runtimeMappingTableProduced=false`
- `targetClassesDefinedOrInitialized=false`

Observed class selector statuses (7,436 total):
- `TARGET_COMPILE_CLASS_FOUND`: 7,009 (94.26%)
- `TARGET_COMPILE_CLASS_MISSING`: 2 (0.03%)
- `SOURCE_INTERMEDIARY_CLASS_MISSING`: 23 (0.31%)
- `TARGET_INTERMEDIARY_CLASS_MISSING`: 402 (5.41%)

Observed method selector statuses (48,575 total):
- `TARGET_API_MEMBER_FOUND_EXACT`: 19,770 (40.70%)
- `TARGET_API_MEMBER_FOUND_WITH_EXTERNAL_TYPES`: 8,988 (18.50%)
- `TARGET_API_MEMBER_MISSING`: 604 (1.24%)
- `TARGET_COMPILE_CLASS_MISSING`: 1
- `INTERMEDIARY_SOURCE_MISSING`: 14,185 (29.20%)
- `INTERMEDIARY_SOURCE_OWNER_UNMAPPED`: 221
- `INTERMEDIARY_SOURCE_DESCRIPTOR_UNRESOLVED`: 36
- `INTERMEDIARY_TARGET_AMBIGUOUS`: 1
- `INTERMEDIARY_TARGET_DESCRIPTOR_CHANGED`: 1,686 (3.47%)
- `INTERMEDIARY_TARGET_ID_MISSING`: 2,976 (6.13%)
- `INTERMEDIARY_TARGET_OWNER_CHANGED`: 107

Observed field selector statuses (31,003 total):
- `TARGET_API_MEMBER_FOUND_EXACT`: 22,711 (73.25%)
- `TARGET_API_MEMBER_FOUND_WITH_EXTERNAL_TYPES`: 4,788 (15.44%)
- `TARGET_API_MEMBER_MISSING`: 176 (0.57%)
- `TARGET_COMPILE_CLASS_MISSING`: 4
- `INTERMEDIARY_SOURCE_OWNER_UNMAPPED`: 86
- `INTERMEDIARY_SOURCE_DESCRIPTOR_UNRESOLVED`: 22
- `INTERMEDIARY_TARGET_DESCRIPTOR_CHANGED`: 791 (2.55%)
- `INTERMEDIARY_TARGET_ID_MISSING`: 2,315 (7.47%)
- `INTERMEDIARY_TARGET_OWNER_CHANGED`: 110

**Preliminary interpretation, with limits:**
- For symbols with a candidate path all the way through the target namespace, the target hierarchy-aware API lookup reports 19,770 exact method matches and 22,711 exact field matches. Another 8,988 methods and 4,788 fields were found but remain non-exact because descriptor types are external. These are structural lookups, not semantic or runtime compatibility claims.
- The 604 method and 176 field `TARGET_API_MEMBER_MISSING` results are reached only after a unique upstream candidate and target official mapping; they are meaningful potential API/member incompatibilities to inspect individually.
- No `TARGET_API_HIERARCHY_INCOMPLETE` status appeared in the reported histograms. No `TARGET_OFFICIAL_MAPPING_*` terminal status appeared either. The only cross-version target ambiguity reported is one method selector, preserved as `INTERMEDIARY_TARGET_AMBIGUOUS`.
- `INTERMEDIARY_SOURCE_MISSING` means the exact (owner, official-obfuscated name, full descriptor) key was absent from the source Tiny method index. The resolver code returns this before attempting a target lookup. It is a missing bridge row, **not proof the Minecraft method was deleted**.
- `INTERMEDIARY_TARGET_ID_MISSING` means a source Intermediary ID has no row in the target Tiny ID index. This can indicate removal or mapping coverage gaps; it is not by itself proof of semantic deletion.
- `INTERMEDIARY_TARGET_OWNER_CHANGED` means that ID exists once in the target but under a different owner. The current fail-closed policy does not accept it as a move without stronger identity evidence.
- `INTERMEDIARY_TARGET_DESCRIPTOR_CHANGED` means the shared Intermediary ID exists under the same owner, but the source and target Intermediary descriptors differ. It is correctly left unresolved rather than silently accepting a changed signature.
- The user's helper counted 10,081 methods and 5,283 fields with *any candidate* carrying external descriptor-type counts. This is broader than the terminal `TARGET_API_MEMBER_FOUND_WITH_EXTERNAL_TYPES` totals (8,988 and 4,788), because candidate data may be retained even when the complete selector path does not finish. Do not conflate these two metrics.
- Windows PowerShell 5.1/PSReadLine 2.0.0 repeatedly crashed while the user was entering the long diagnostic loop; the loop eventually executed and printed samples. This is a console line-editor issue, not an audit/report failure. Prefer short one-line PowerShell diagnostics.

**Next diagnostic before changing resolver behavior:** obtain compact upstream-status histograms for methods and fields, then inspect a small representative sample of `TARGET_API_MEMBER_MISSING`, `INTERMEDIARY_TARGET_DESCRIPTOR_CHANGED`, and `INTERMEDIARY_TARGET_OWNER_CHANGED` entries with their candidate namespace owner/name/descriptors. Group the 14,185 `INTERMEDIARY_SOURCE_MISSING` methods by SRG owner to identify whether the gap is concentrated in special/client/data classes or broadly distributed. Do not add guessed fallback mapping rules from names alone.

No selector transformation, runtime mapping table, or licensing conclusion is authorized by these audit results.
