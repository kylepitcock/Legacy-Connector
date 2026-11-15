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

**Status:** First read-only implementation published; awaiting the user's forced loader test gate. It is not wired into Mixin selector rewriting or the runtime remapper.

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
- The synthetic tests and Gradle task are newly published but are **not yet user-run**. Await the complete `:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks` output and inspect the generated report. Fix any first failing test before moving to Intermediary composition.
- This first edge does not yet validate the source selector namespace for every refmap context. A source mod's refmap context must still be explicitly bound to the correct namespace by evidence.
- The target 1.21.1 official-obfuscated → actual NeoForge compile namespace edge remains separately unverified.
