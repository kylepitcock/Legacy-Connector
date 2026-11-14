## User report review and descriptor-classification correction — 2026-11-15

The user supplied the generated build-local JSON report from the audit at the prior implementation. The report confirms that the audit task executed and emitted a report. The complete Gradle console was not supplied, so the overall forced test-suite result is not independently recorded as green.

### Observed artifact fingerprints

| Input | SHA-256 | Size |
|---|---|---:|
| MCPConfig 1.20.1 joined.tsrg | `19ae000175bfb58e5b26406c31a0e501bfb40000b078fee2f00ea60a8c7ed453` | 5,653,149 bytes |
| Fabric Intermediary 1.20.1 Tiny v1 | `5a808bd1ebc4a9ee7d1c5900dbd2bf391586b536c3d86fbf2f972047faa2d165` | 3,110,223 bytes |
| Fabric Intermediary 1.21.1 Tiny v1 | `83071b5b17866658463255f5f21430ade2a49ce2dd089dc3c14de1e93b99211c` | 3,614,213 bytes |

### Observed source/cross-version counts from the supplied report

- Classes: 7,436 MCPConfig class rows; 7,413 source Intermediary class joins; 7,011 of those source class IDs also occur in target Intermediary; 402 source IDs are absent from target; 1,234 target-only class IDs.
- Methods: 48,575 source SRG rows; 34,390 source obfuscated-owner/name/descriptor joins; 188 SRG IDs are reused across distinct Intermediary symbols; no source or target ambiguities were reported. Of the source-joined method rows, 20,131 were counted exact, 877 descriptor-changed, 108 owner-changed and 2,977 target-ID-missing. There were 10,076 target descriptor-unresolved rows.
- Fields: 31,003 source rows and 31,003 source owner/name joins; no source or target ambiguities were reported. Of the source-joined field rows, 22,813 were counted exact, 416 descriptor-changed, 110 owner-changed and 2,316 target-ID-missing. There were 5,262 target descriptor-unresolved rows.
- The 11,330 method and 5,722 field source descriptor-unresolved counts must **not** be interpreted as confirmed missing Minecraft classes.

### Parser issue found during report review

The Tiny v1 specification's own example includes descriptors such as `(Ljava/lang/String;)I`. The old `normalizeDescriptor` implementation required every object type in a descriptor to exist in the Intermediary class map and returned unresolved for any absent type. That conflates unmapped external types (JDK and libraries) with Minecraft types whose mapping is actually missing. It likely inflated the unresolved descriptor counts in the supplied report.

### Corrective implementation now committed; validation pending

- Commit `2b6f64feee1d557647ea11b2275d237514761b64` changes descriptor normalization to receive the source MCPConfig class inventory. A package-qualified descriptor type absent from the relevant Minecraft mapping inventory is preserved as an external candidate. A type known to be a source Minecraft class but missing an Intermediary identity remains unresolved. Unqualified unknown official-obfuscated names remain unresolved.
- External descriptor candidates are deliberately **not** counted as fully mapped exact rows. New counters distinguish source external descriptor rows/occurrences, target external descriptor rows/occurrences, and owner/member/descriptor matches that require external-name pass-through.
- Target package-qualified external-candidate classpath existence is not proven by this audit; such rows are kept separate from fully mapped exact evidence. Do not treat external pass-through as runtime compatibility.
- A final counter refinement in commit `0dbd61d72d9d246c73b7734e67b669c5efa6413d` counts external descriptor occurrences independently even when a different type in the same descriptor remains unresolved.
- Regression tests added: Java/library types `java/lang/String` and `java/util/List` pass through and are classified separately; a source class known to MCPConfig but absent from Tiny remains unresolved. Test changes are in commits `a0eaa30b64d244d8aa07239f3bd0a9e7f1d13548` and `43e02d86b4290e5325faa0b487ad975cf2b7c5eb`; a following source-only counter refinement is `0d65007b9dd73c8d3069dd691fc028c257c56766`.
- **Do not use the original JSON's descriptor-unresolved counts as final coverage.** The immediate next step is to run the forced loader test gate on the latest branch, regenerate the audit report, and compare the new external/unresolved classifications.

# Forge 1.20.1 → NeoForge 1.21.1 — Intermediary Bridge Feasibility Audit

**Status:** Initial read-only implementation has generated a user-supplied report. Review exposed overly strict descriptor normalization for external/JDK types; a corrective classifier and synthetic tests are committed, but the corrected forced loader gate is pending user validation. The complete Gradle console for the first report was not supplied.  
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
- Object types found in the version's Tiny class table are converted to Intermediary IDs.
- On the source side, the parser also receives MCPConfig's source obfuscated class inventory. A descriptor type known to be a source Minecraft class but missing from Tiny remains unresolved.
- Package-qualified descriptor types absent from the relevant Minecraft class inventory are preserved as external candidates and counted separately. On the target side, unknown unqualified official-obfuscated names remain unresolved; package-qualified unknowns are treated only as external candidates.
- External pass-through does not prove that the class is present on the target runtime classpath. Rows using it are not counted as fully mapped exact; they receive a separate external-pass-through classification.
- The classification is intentionally conservative and must be revisited if the target official namespace's class naming rules or the completeness of a pinned Tiny dataset change.
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
- known source Minecraft descriptor types missing from Tiny remaining unresolved;
- package-qualified JDK/library descriptor types being preserved as non-exact external candidates;
- JSON report validity and explicit false values for selector transformation, complete Forge SRG selector path, and runtime table production.

The original audit report has been generated, but its external-type descriptor counts were over-classified as unresolved. The descriptor-classification correction and new synthetic cases are not yet user-tested. Do not mark this design green before the new forced Gradle run succeeds and the regenerated report is reviewed.

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

**Decision:** retain this as a bounded, read-only feasibility audit. First validate the corrected descriptor classification and regenerate its measurements. Then use the reviewed output to design the next owner/member/descriptor-qualified SRG conversion and target name edge. Do not proceed to selector transformation until the whole selector path is explicit, complete for the symbol, version-pinned, and independently validated.
