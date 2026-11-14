# Forge 1.20.1 → NeoForge 1.21.1 — Intermediary Bridge Feasibility Audit

**Status:** Initial read-only implementation, Gradle wiring, and synthetic regression tests are committed on `feat/forge1201-preflight-analyzer`; the user has not yet run the new gate.  
**Scope:** Audit a candidate version-to-version identity path through Fabric Intermediary. Do not create a runtime table, rewrite selectors, or claim compatibility.  
**Target:** Minecraft 1.20.1 / Forge SRG-side source data → Minecraft 1.21.1 / NeoForge 21.1.258.  
**Architectural boundary:** this audit consumes the `obf` columns of MCPConfig's TSRG2 file. It does not convert a Forge SRG selector into that `obf` namespace; SRG values are diagnostic metadata only.

## 1. Why investigate Intermediary?

The 1.20.1 SRG → Mojang-name table audited in Schema 1.10B is a useful source normalization aid, but its generated output is a global member-ID → name table. It does not retain an owner-qualified class/member/descriptor graph, and the real provenance report showed 15,353 repeat SRG-ID row occurrences. The generator correctly coalesces only identical names, but a global SRG identifier is not a safe identity key: the same member identifier may occur on different owners or different signatures.

Fabric Intermediary offers a separately licensed, version-specific candidate identity namespace. The Fabric project describes the mappings as an intermediary naming form designed to remain stable across versions and mapping changes; it documents Tiny mappings per Minecraft version and match files used to update the chain. The project states that its Intermediary mappings are CC0 1.0. See:

- [FabricMC/intermediary README and licensing statement](https://github.com/FabricMC/intermediary)
- [Minecraft 1.20.1 Tiny v1 mapping](https://github.com/FabricMC/intermediary/blob/master/mappings/1.20.1.tiny)
- [Minecraft 1.21.1 Tiny v1 mapping](https://github.com/FabricMC/intermediary/blob/master/mappings/1.21.1.tiny)
- [Fabric Intermediary publication build script](https://github.com/FabricMC/intermediary/blob/master/build.gradle)

The publication script distinguishes the original Tiny v1 JAR from a separately generated Tiny v2 classifier. It packages each version's source mapping file at `mappings/mappings.tiny`. This implementation intentionally consumes the plain, Tiny v1 artifacts:

- `net.fabricmc:intermediary:1.20.1`
- `net.fabricmc:intermediary:1.21.1`

The 1.20.1 and 1.21.1 Maven artifact indexes list these exact versioned plain JARs. Do not use a floating `latest` version or the `-v2` classifier with the current parser. See the [1.20.1 artifact index](https://maven.fabricmc.net/net/fabricmc/intermediary/1.20.1/) and [1.21.1 artifact index](https://maven.fabricmc.net/net/fabricmc/intermediary/1.21.1/).

**Important limitation:** Intermediary IDs are candidate continuity evidence, not a semantic promise that the corresponding code remains binary-compatible. A class/member can be removed, moved, have its descriptor changed, or be repurposed. An unchanged ID and descriptor are structural evidence only.

## 2. Namespace contract — do not collapse the stages

The pinned MCPConfig input is `config/joined.tsrg`, with header:

```text
tsrg2 obf srg id
```

It contains separate obfuscated and SRG-side columns. The current audit reads the `obf` class/member/descriptor columns and joins those to the Tiny v1 `official` namespace. It retains SRG member IDs only as diagnostic metadata.

The audit's actual stages are:

1. **MCPConfig obfuscated source names (1.20.1) → Fabric Intermediary (1.20.1):** exact owner/member/descriptor lookup in the 1.20.1 Tiny map.
2. **Fabric Intermediary (1.20.1) → Fabric Intermediary (1.21.1):** compare stable class/member IDs, while requiring owner identity and a fully resolved descriptor for an exact method/field result.
3. **Fabric Intermediary (1.21.1) → target official-obfuscated names (1.21.1):** the 1.21.1 Tiny map supplies the target official owner/member names after the audit establishes a candidate identity.

This is **not** yet a complete Forge mod selector path. In particular, `minecraft:forge-srg@1.20.1` has not been translated into the obfuscated namespace consumed by stage 1. The Forge SRG class/member/descriptor → source-obfuscated conversion must be independently modeled and evidenced. A SRG name by itself is not sufficient: the lookup must be owner- and descriptor-scoped. The 1.20.1 Mojang client mappings may provide evidence for that missing source-side conversion, but their licensing and redistribution terms remain a separate constraint.

The target is still `minecraft:official-obfuscated@1.21.1`, not NeoForge's Mojang-named runtime namespace. A separate target-side official-obfuscated → target Mojang-named edge will be required before selector candidates could be compared to the actual NeoForge target API. No stage is inferred from equal-looking names.

## 3. Join and classification rules

### Source Tiny join

- Classes: MCPConfig obfuscated class owner → Tiny v1 `official` class name.
- Methods: obfuscated owner + obfuscated member name + complete JVM descriptor.
- Fields: obfuscated owner + obfuscated member name. TSRG2 field rows do not carry a descriptor, so any Tiny lookup with more than one candidate is marked ambiguous and is not accepted.
- Every Tiny descriptor object type must resolve through that version's class table before the descriptor is used as exact evidence. A missing class type makes the descriptor unresolved; a coincidentally identical string is not sufficient evidence.
- Repeated SRG IDs are counted diagnostically, but are never used as the join key.

### Target Tiny comparison

For a source member that uniquely joins to Intermediary, the audit checks the target mapping for the same Intermediary member ID:

- **Exact owner + ID + descriptor:** candidate unchanged symbol identity.
- **Same owner + ID, descriptor changed:** candidate was retained, but its structural signature changed.
- **Member ID exists only under another owner:** owner moved/changed; do not treat as exact.
- **Member ID absent:** no target entry under that identity.
- **Descriptor unresolved:** cannot claim exactness.
- **Multiple rows:** ambiguous; fail closed.

For methods, both descriptors include parameters and return type. For fields, the source TSRG row lacks its descriptor, so source/target resolution remains subject to explicit Tiny uniqueness and owner/ID checks.

## 4. Build and report behavior

Gradle declares the exact versioned Intermediary dependencies in the loader build. It extracts only `mappings/mappings.tiny` from each plain JAR with a 32 MiB extraction cap. Original archives and extracted Tiny files stay under `loader/build/tmp/fabric-intermediary/`.

The task is:

```text
:loader:auditIntermediaryBridge1201To1211
```

The normal `:loader:test` task depends on it. Its report is:

```text
loader/build/reports/mappings/1.20.1-1.21.1/intermediary-bridge-audit.json
```

The report fingerprints each actual input file and records row totals, joins, missing/ambiguous entries, owner/descriptor changes, repeated SRG identifiers that resolve to different Intermediary symbols, and explicit limitations. This report is build-local. The audit task does not add the Tiny inputs or report to the loader JAR, and it produces no runtime remapping table.

The audit utility class is part of loader main classes because Gradle executes it as a Java tool. That does not make its input mappings or report part of the JAR.

## 5. Synthetic regression coverage

`Forge1201IntermediaryBridgeAuditTest` covers:

- exact owner + member + full descriptor join and target continuity;
- separate descriptor changes from owner changes;
- repeated SRG IDs across different owners resolving to different Intermediary IDs;
- unmatched methods and ambiguous descriptorless field joins;
- unresolved descriptor types not being counted as exact;
- JSON report validity and explicit false values for selector transformation, complete Forge SRG selector path, and runtime table production.

The user-run loader test gate is still pending for this new increment. Do not mark this design green before the new forced Gradle run succeeds.

## 6. Preliminary repository inspection — not the build result

A read-only inspection of the public Fabric Tiny files and a public MCPConfig 1.20.1-rc1 source tree suggests the approach is viable but partial. The GitHub MCPConfig sample is not a substitute for the exact resolved Maven archive; the Gradle task report from the user's build is authoritative for actual counts and fingerprints. Do not copy preliminary counts into the final compatibility catalog.

The public Intermediary repository was archived in 2026. The required 1.20.1 and 1.21.1 mappings already exist as exact versioned artifacts, so this audit pins those coordinates and records the extracted file digests. A future Minecraft target version requires a deliberate new mapping pin and new validation, not an implicit upgrade.

## 7. Acceptance and non-goals

This audit is acceptable when the forced loader test passes, the generated JSON is reviewed, and the report's exact source/target counts and digests reconcile with the console.

It does **not**:
- map Forge SRG selectors to source-obfuscated names;
- map target-obfuscated names to NeoForge's Mojang-named runtime;
- prove every common Intermediary ID has unchanged behavior;
- rewrite refmaps, Mixin selectors, accessors, invokers, or `@At` targets;
- load a mod, launch Minecraft, or prove runtime/gameplay compatibility;
- resolve the Mojang-derived mapping table's unresolved redistribution/licensing question.

**Decision:** retain this as a bounded, read-only feasibility audit. Use its measured output to design the next exact SRG-owner-member-descriptor conversion and target name edge. Do not proceed to selector transformation until the whole selector path is explicit, complete for the symbol, pinned, licensed, and independently validated.
