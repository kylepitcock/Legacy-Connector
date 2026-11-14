# Schema 1.10 — Explicit Mapping-Profile Contract Design

**Status:** Schema 1.10A parser, validator, preflight-report integration, Gradle input, and synthetic tests are user-validated green at `cc651af52b192fb534d49f3ad494b42e3d15d6f0`. Schema 1.10B source-edge implementation and its corrected forced loader test gate are user-validated green at `493cae748f89f88308a7e1b98ea583d2e58871af` (`BUILD SUCCESSFUL in 50s; 17/17 actionable tasks`). The build-local provenance JSON was generated and remains to be reviewed. No selector transformation is implemented.
**Design baseline:** Universal analyzer branch `feat/forge1201-preflight-analyzer`; current implementation status and exact tested/untested boundary are recorded in the analyzer plan and continuation handoff.  
**Input feature:** Schema 1.9A/1.9B Mixin 0.8.5 refmap inventory and exact owner-scoped lookup.  
**Target:** Minecraft 1.21.1 / NeoForge 21.1.258.  
**Rule:** A refmap context key is not a namespace declaration. No name equality, package similarity, or target class presence may be used to invent a mapping.

This design establishes the evidence contract required before any refmap selector is converted from its Forge 1.20.1 mapping environment to the NeoForge 1.21.1 compile environment. It does not implement a cross-version mapping dataset, a remapper, selector rewriting, or Mixin runtime behavior.

## 1. Executive conclusion

The repository contains useful mapping infrastructure, but it does **not** yet contain a complete, pinned, end-to-end Forge 1.20.1 → NeoForge 1.21.1 Mixin selector mapping chain.

The current chain has at least four distinct concerns that must not be collapsed:

1. **Refmap lookup:** Schema 1.9 chooses the exact Mixin 0.8.5 owner/reference entry and, only when explicitly supplied, its context. This is table lookup, not namespace conversion.
2. **Source-version name interpretation:** The selected refmap value may be expressed in a Forge/MCP source mapping environment. The repository already generates a source-version SRG-to-Mojang member-name table from MCPConfig and Mojang's 1.20.1 client mappings. That table is not a complete cross-version map.
3. **Cross-version Minecraft migration:** A separate, evidence-backed edge is required from the 1.20.1 source symbol space to the 1.21.1 target symbol space. The repository does not currently define this edge.
4. **Target verification:** Once a target owner/name/descriptor candidate has been produced by an explicit mapping chain, the existing `Forge1201TargetApiVerifier.TargetIndex` can perform read-only class/member checks. It does not create the missing mapping.

Forge API migration is another distinct domain: `Forge1201MappingCatalog` records package relocation candidates, explicit type replacements, adapter-required types, and unmapped Forge APIs. It must not be treated as a Minecraft cross-version mapping database.

**Decision:** design and validate profiles and their evidence first. Do not rewrite selectors until a complete path exists for the selector's symbol domain and all required mapping dimensions.

## 2. Pinned repository audit

The following files were re-read from source branch HEAD `412ed1aa4f1220a4d2d02ed7f077ce4b06ca805c`:

| Existing component | Source blob | What it does | What it does not establish |
|---|---|---|---|
| `loader/src/main/java/.../Forge1201MappingCatalog.java` | `ed226440ecf105e56119d34376ccc35b99f1b9df` | Version-specific Forge API package moves, explicit type replacements, manual adapters, and unmapped Forge types | Vanilla Minecraft 1.20.1 → 1.21.1 class/member mapping; complete method/field migration; semantic compatibility |
| `loader/src/main/java/.../Forge1201SrgNameMappings.java` | `64c6aea0d61897f4d413c0473bbeb49655c1ffb4` | Reads a pinned source-version CSV contract and translates `m_<id>_` / `f_<id>_` member names to readable 1.20.1 names | Target-version member names; owner/class mapping; descriptor mapping; a general Mixin selector remapper |
| `loader/src/main/java/.../mapping/SrgMojangTable.java` | `fd354c5f36bf778cef12240f8456c24261f21c10` | Generates a source SRG-ID → Mojang member-name table by joining MCPConfig's `joined.tsrg` with Mojang's source-version client mappings; the generator matches obfuscated owner/member/descriptor before writing the compact table | A retained, owner-qualified class/member/descriptor graph; a 1.20.1 → 1.21.1 cross-version edge |
| `loader/src/main/java/.../Forge1201TargetApiVerifier.java` | `b8106cbe58dd0642e5b4331eb442f14929a1da2a` | Exposes a per-analysis `TargetIndex` for class metadata and exact inherited method/field lookup | How a source selector should be mapped; Mixin injection-point or bytecode instruction success |
| `loader/build.gradle` | `1b6036007ea02998decb497f5e63eefd05e2f5fe` | Declares the source MCPConfig input and generates the compact SRG-to-Mojang table | A checked-in source mapping manifest with all artifact digests and license metadata |
| Root `build.gradle` | `eed37279c86ed845c4aa25d0967fcfbf79bcd75f` | Supplies target compile-classpath roots to preflight and accepts the explicit refmap context Gradle property | A mapping profile or cross-version selector converter |

### 2.1 Source 1.20.1 mapping inputs

The loader build declares:

- MCPConfig coordinate: `de.oceanlabs.mcp:mcp_config:1.20.1-20230612.114412@zip`.
- The source file extracted for the join is `config/joined.tsrg`.
- The Mojang client mapping input is downloaded from the content-addressed URL `https://piston-data.mojang.com/v1/objects/6c48521eed01fe2e8ecdadbd5ae348415f3c47da/client.txt`.
- The repository's generated output is `build/generated/srg-mojang/legacyconnector/mappings/1.20.1/srg-mojang.tsv`.

The user's green Schema 1.9B test log generated **64,225** SRG → Mojang member rows. That confirms this build-time generation task ran; it does not prove that all refmap selector forms or all mapping dimensions are covered.

The compact `SrgMojangTable` output is intentionally a global SRG identifier → readable member name table. Its generator uses owner and descriptor information while matching intermediate names, but those dimensions are not retained as a complete output graph. The current class therefore must not be advertised as a generic owner-aware selector mapping engine.

At the original Schema 1.10 design checkpoint, the task downloaded Mojang's client mapping file from a hash-addressed URL without independently validating the downloaded bytes against that URL's SHA-1 or recording SHA-256. The MCPConfig dependency was pinned by Maven coordinate/version, but the build did not record the resolved archive's SHA-256 or the extracted `joined.tsrg` digest. Schema 1.10B implementation work now adds those checks and a build-local provenance report; user validation of that new code is pending.

### 2.2 Target 1.21.1 mapping environment

The current repository pins:

- Minecraft: `1.21.1`.
- NeoForge: `21.1.258`.
- ModDevGradle: `2.0.148`.
- Parchment's Minecraft version: `1.21.1`.
- Parchment mapping release: `2024.11.17`.

NeoForge's 1.21.1 ModDevGradle MDK states that its default method and field names are Mojang's official mapping names. NeoForged's Parchment documentation describes Parchment as parameter-name and Javadoc augmentation of Mojang's official names. Thus the target namespace baseline for this project is the version-bound official Mojang naming environment, with the pinned Parchment augmentation recorded separately. Parchment's version is not a cross-version selector-mapping edge.

For reproducible target verification, the profile must record the actual resolved target compile artifacts/classpath used by the analyzer, not just the declared NeoForge version. The analyzer already receives the root compile classpath as class-resource roots; later provenance should identify those inputs by stable artifact identity and digest, and should preserve the exact target class bytes used for each verification.

## 3. Mixin refmap context is not a namespace

Schema 1.9 already implements the Mixin 0.8.5 lookup rules:

- Refmap `mappings` and `data[context]` are owner-scoped reference tables.
- The requested context is an opaque key supplied explicitly to preflight.
- If the requested context table is absent, Mixin falls back to base mappings.
- If that table exists but the owner/reference entry is absent, lookup passes through; it does not retry base mappings for that individual miss.
- Without an explicit context, the analyzer inventories candidates rather than choosing a table.

Mixin's runtime remapping properties document a default source environment of `searge` for the optional `mixin.env.refMapRemappingFile` path, and require an explicitly different source environment when appropriate. This is evidence about that optional Mixin runtime remapper convention. It is **not** permission for the analyzer to equate every refmap context named `searge` with a fully specified, versioned namespace, nor to assume that a runtime remapping file is enabled.

A profile must declare a mapping-context binding explicitly, for example:

```json
{
  "refmapContextBindings": {
    "searge": {
      "namespaceId": "forge-mcp-srg",
      "minecraftVersion": "1.20.1",
      "evidence": [
        "source build configuration",
        "pinned MCPConfig artifact",
        "reviewed refmap-generation contract"
      ]
    }
  }
}
```

This is an illustrative declaration shape, not a validated binding for every mod. The binding is valid only when the source mod's build/refmap provenance supports it. If the source context cannot be bound to a namespace with evidence, report `REFMAP_CONTEXT_NAMESPACE_UNBOUND` and stop that path.

The base `mappings` table also requires an explicit source/output environment binding. Do not give it an invented context name.

The optional `RemappingReferenceMapper` path is a separate runtime transformation stage. Its enablement, source environment, remapping file, and effective target environment must be known before it can be modeled. Unknown runtime settings remain `RUNTIME_REFMAP_REMAP_UNMODELED`; do not silently insert an assumed SRG rewrite.

## 4. Required mapping graph

A mapping profile describes a directed graph of version- and namespace-qualified edges. A selector can be converted only if the graph contains one complete, unambiguous directed path from the selected refmap output namespace to the target compile namespace for the selector's symbol domain.

The conceptual path is:

```text
Mixin annotation string
    |
    v
Schema 1.9 exact owner + reference + explicit-context lookup
    |
    v
Selected refmap value
    |
    | context binding supplied by the profile
    v
Forge 1.20.1 source mapping namespace (if proven)
    |
    | version-pinned source normalization edge
    v
Minecraft 1.20.1 official Mojang namespace
    |
    | explicit cross-version migration edge (NOT CURRENTLY AVAILABLE)
    v
Minecraft 1.21.1 official Mojang namespace
    |
    | exact read-only verification against resolved target class bytes
    v
Forge1201TargetApiVerifier.TargetIndex
```

This is a conceptual graph, not a claim that every edge currently exists. In particular, the repository's source SRG-to-Mojang table does not provide the missing 1.20.1 → 1.21.1 edge.

### 4.1 Edge contract

Every edge must declare:

- Stable edge ID.
- Explicit direction (`FORWARD`); reverse use is a separate edge, never an implicit inverse.
- Source and target Minecraft versions for that individual edge, including intermediate-version hops.
- Source and target namespace IDs, with their mapping-set versions.

- Symbol domains covered: classes, methods, fields, descriptors, packages, or a smaller declared subset.
- Mapping format and parser version.
- Provider/tool identity and exact version.
- Exact artifact coordinate or canonical URL, repository, classifier, and internal path where relevant.
- Upstream checksum when published, plus SHA-256 calculated from the exact bytes actually consumed.
- License identifier/notice and whether the artifact or a derived output may be redistributed.
- Mapping cardinality/ambiguity policy, completeness declaration, and known exclusions.
- Evidence links and review provenance.
- Whether the edge performs an actual mapping, a narrowly scoped explicit identity declaration, or a lossy/partial normalization.

Mapping edges are not assumed to be reversible. A one-to-many or many-to-one relation must return an explicit ambiguity/alias result, not choose the first row.

### 4.2 Symbol-aware mapping requirements

A future selector converter must parse the supported Mixin selector form before applying a mapping. It must not run blind text replacement across the whole selector.

For a fully qualified method or field selector, the mapping operation may need to transform:

- Declaring owner class.
- Member name.
- Every object type appearing in the descriptor.
- Array component types in the descriptor.
- Constructor/owner-only selector forms according to their own syntax.

Primitive descriptor components remain unchanged. A class-name mapping does not automatically map a member; a member-name mapping does not automatically map descriptor types.

For a selector with no owner, the analyzer must use only a proven target-owner context derived from the mixin's target evidence. Multiple target owners or dynamic/plugin-provided targets make owner-dependent resolution ambiguous or dynamic. Never replace this with a global ownerless member-name search.

The mixin class internal name used as the refmap lookup owner remains the exact Schema 1.9 lookup key. It is not the target owner and must not be substituted with a mapped target class.

### 4.3 Separate symbol domains

At minimum, keep these domains separate:

1. Minecraft/vanilla class and member symbols.
2. Forge 47.x API symbols.
3. NeoForge 21.1.x API symbols.
4. Connector-owned compatibility shim symbols.
5. Mod-owned classes and members.

The existing `Forge1201MappingCatalog` is a Forge API migration audit. Its `PACKAGE_RELOCATION_CANDIDATE` status is not a blanket approval: the catalog itself says class/member compatibility still requires verification. Explicit type replacements and manual adapters are individual decisions. None of those categories is a substitute for a Minecraft version mapping profile.

## 5. Proposed profile document shape

The profile is a human-reviewable, version-controlled declaration. The runtime analyzer validates it strictly before use. This example intentionally has an incomplete cross-version edge and unmaterialized source artifacts:

```json
{
  "schemaVersion": 1,
  "profileId": "forge-1.20.1-to-neoforge-1.21.1-mixin",
  "status": "INCOMPLETE",
  "source": {
    "minecraftVersion": "1.20.1",
    "loader": {
      "id": "forge",
      "versionRange": "[47,48)"
    },
    "compileNamespaceId": "minecraft:official-mojang@1.20.1",
    "refmapContextBindings": {
      "searge": {
        "namespaceId": "minecraft:forge-srg@1.20.1",
        "evidence": [
          "reviewed source-mod build/refmap provenance",
          "pinned Mixin 0.8.5 annotation-processor contract"
        ]
      }
    },
    "baseRefmapBinding": {
      "namespaceId": "minecraft:forge-srg@1.20.1",
      "evidence": [
        "reviewed source-mod build/refmap provenance"
      ]
    }
  },
  "target": {
    "minecraftVersion": "1.21.1",
    "loader": {
      "id": "neoforge",
      "version": "21.1.258"
    },
    "compileNamespaceId": "minecraft:official-mojang@1.21.1",
    "mappingAugmentations": [
      {
        "provider": "Parchment",
        "minecraftVersion": "1.21.1",
        "version": "2024.11.17",
        "scope": ["parameter-names", "javadocs"],
        "renamesClassesOrMembers": false
      }
    ]
  },
  "artifacts": [
    {
      "artifactId": "mcpconfig-1201",
      "provider": "MCPConfig",
      "version": "1.20.1-20230612.114412",
      "format": "tsrg2",
      "coordinate": "de.oceanlabs.mcp:mcp_config:1.20.1-20230612.114412",
      "sha256": "0000000000000000000000000000000000000000000000000000000000000000",
      "localPath": "artifacts/mcp_config-1.20.1-20230612.114412.zip",
      "license": {
        "id": "REVIEW_REQUIRED",
        "reviewStatus": "PENDING",
        "evidence": ["license review required before enabling this artifact"]
      }
    },
    {
      "artifactId": "mojang-client-mappings-1201",
      "provider": "Mojang",
      "version": "1.20.1",
      "format": "proguard",
      "url": "https://piston-data.mojang.com/v1/objects/6c48521eed01fe2e8ecdadbd5ae348415f3c47da/client.txt",
      "sha256": "0000000000000000000000000000000000000000000000000000000000000000",
      "upstreamChecksum": "sha1:0000000000000000000000000000000000000000",
      "localPath": "artifacts/1.20.1-client.txt",
      "license": {
        "id": "REVIEW_REQUIRED",
        "reviewStatus": "PENDING",
        "evidence": ["license review required before distributing a derived mapping table"]
      }
    }
  ],
  "edges": [
    {
      "edgeId": "source-srg-to-source-mojang-1201",
      "from": "minecraft:forge-srg@1.20.1",
      "to": "minecraft:official-mojang@1.20.1",
      "direction": "FORWARD",
      "sourceMinecraftVersion": "1.20.1",
      "targetMinecraftVersion": "1.20.1",
      "scope": ["method-names", "field-names"],
      "artifactIds": ["mcpconfig-1201", "mojang-client-mappings-1201"],
      "status": "PARTIAL",
      "reviewEvidence": [
        "pinned MCPConfig and Mojang 1.20.1 inputs",
        "coverage audit remains pending"
      ]
    },
    {
      "edgeId": "minecraft-1201-to-1211",
      "from": "minecraft:official-mojang@1.20.1",
      "to": "minecraft:official-mojang@1.21.1",
      "direction": "FORWARD",
      "sourceMinecraftVersion": "1.20.1",
      "targetMinecraftVersion": "1.21.1",
      "scope": [],
      "artifactIds": [],
      "status": "MISSING"
    }
  ],
  "runtimeRemapping": {
    "status": "UNMODELED_UNLESS_EXPLICITLY_SUPPLIED"
  },
  "licenseReview": {
    "required": true,
    "evidence": [
      "profile-level license review must be completed before enabling a mapping profile"
    ]
  }
}
```

This is an illustrative schema fixture, not a usable or accepted mapping profile. The all-zero digests are deliberately invalid placeholders; local artifacts are unverified, license reviews are pending, the source edge is partial, and the required cross-version edge is missing. It must not pass validation or authorize selector transformation. The `searge` namespace binding is only an example declaration shape; each actual mod's build/refmap provenance must justify it. The base table has a separate binding because Mixin's base `mappings` table is distinct from `data[context]`.

Schema 1.10A requires one explicit `direction` and per-edge `sourceMinecraftVersion` / `targetMinecraftVersion`. A multi-edge route is complete only when both namespace and Minecraft-version endpoints connect at every hop. This prevents a superficially connected graph from silently jumping across game versions.

## 6. Deterministic profile and edge states

Profile-level states:

- `PROFILE_VALIDATED`
- `PROFILE_INCOMPLETE`
- `PROFILE_UNSUPPORTED_SCHEMA`
- `PROFILE_SOURCE_VERSION_MISMATCH`
- `PROFILE_TARGET_VERSION_MISMATCH`
- `PROFILE_CONTEXT_NAMESPACE_UNBOUND`
- `PROFILE_NO_COMPLETE_PATH`
- `PROFILE_AMBIGUOUS_PATH`
- `PROFILE_ARTIFACT_MISSING`
- `PROFILE_ARTIFACT_DIGEST_MISMATCH`
- `PROFILE_LICENSE_REVIEW_REQUIRED`

Edge/lookup states:

- `EDGE_APPLIED_EXACT`
- `EDGE_EXPLICIT_IDENTITY`
- `EDGE_MAPPING_NOT_FOUND`
- `EDGE_MAPPING_AMBIGUOUS`
- `EDGE_COVERAGE_INCOMPLETE`
- `EDGE_DIRECTION_UNSUPPORTED`
- `EDGE_VERSION_MISMATCH`
- `EDGE_NAMESPACE_MISMATCH`
- `EDGE_SYMBOL_KIND_UNSUPPORTED`
- `EDGE_DESCRIPTOR_CLASS_UNMAPPED`
- `EDGE_OWNER_CONTEXT_REQUIRED`
- `EDGE_OWNER_CONTEXT_AMBIGUOUS`
- `EDGE_IDENTITY_UNVERIFIED`

Target verification remains a separate result, using the existing target index statuses. A structurally found method does not prove that the Mixin injection point, ordinal, shift, slice, handler signature, or runtime transformation succeeds.

## 7. Mapping artifact and build provenance

Before an edge is enabled, the analyzer needs a generated provenance record for the exact input bytes:

- Maven coordinate/URL and repository.
- Resolved artifact file name and size.
- Upstream checksum algorithm/value, if supplied.
- Computed SHA-256.
- Internal file path(s) extracted from archives, with their own SHA-256.
- Mapping format, namespace endpoints, Minecraft versions, and symbol coverage.
- Mapping generator version and complete input artifact list/hashes.
- Build/toolchain identity needed to reproduce generation.
- License/redistribution metadata.

Specific repository follow-ups for the eventual implementation:

1. Validate the Mojang 1.20.1 client mapping download against its known upstream SHA-1 object identifier, then compute/store a SHA-256 for provenance. The current download task uses a hash-addressed URL but does not validate the returned file bytes itself.
2. Record a SHA-256 for the resolved MCPConfig ZIP and extracted `config/joined.tsrg`; the Maven coordinate alone is not a cryptographic content digest.
3. Record the exact resolved target compile artifacts used by the root preflight classpath. Do not equate the NeoForge version string with a digest of the target classes.
4. Preserve generator inputs and versions so a generated mapping table can be reproduced and audited.
5. Do not commit downloaded Mojang mapping files or redistribute complete, unmodified mapping datasets. The current repository already notes Mojang mapping redistribution restrictions; any generated/derived table distribution requires a separate license review.

## 8. Schema 1.10A parser guardrails and validation semantics

The first implementation enforces these bounded, deterministic limits:

- Profile JSON: 1 MiB maximum, strict UTF-8, duplicate object keys rejected, JSON nesting limited to 64.
- Context/base bindings: at most 64 context-key bindings.
- Mapping edges: at most 256.
- Mapping artifacts: at most 512.
- Each materialized mapping artifact: at most 512 MiB while hashing.
- Directed graph search: at most 100,000 edge examinations per context binding; the search reports an explicit limit state rather than treating an incomplete search as a complete route.
- Artifact paths must be relative, use forward slashes, remain under the profile directory after normalization and symlink resolution, and may not use drive prefixes or backslashes.

Only `FORWARD` directed edges are accepted in this increment. Every edge must identify its source/target Minecraft versions and namespace IDs. A route is complete only when both namespace and version endpoints connect at every hop. Reverse mapping is never inferred.

The profile requires explicit source-context bindings, a distinct optional base-table binding, target namespace/version, a runtime-remapping declaration, and profile-level license-review evidence. Artifacts declare exactly one coordinate or URL, SHA-256, optional upstream SHA-1/SHA-256, provider/version/format, local materialization path for byte verification, and license review data.

A verified edge must declare complete class/member/descriptor selector scope, reviewed evidence, and materialized artifacts whose actual hashes match their declarations. An explicit identity edge requires its own evidence and must remain within one Minecraft version; the validator records it but never applies it. Declaring runtime refmap remapping as enabled is deliberately blocking in Schema 1.10A because that runtime stage is not yet independently modeled or executed.

`PROFILE_VALIDATED` therefore means only that the supplied declarations, artifact bytes, license evidence, and directed/version-contiguous graph passed this structural validator. It does not validate mapping table contents, prove that a claimed edge is semantically correct, or authorize a selector rewrite.

## 9. Runtime remapping and wrappers remain separate

The optional Mixin `RemappingReferenceMapper` can apply an SRG replacement stage under runtime settings. This operation must be represented independently from the JSON context table and must not be assumed to run.

The profile may model that stage only when the following are supplied and matched to the exact runtime:

- Whether `mixin.env.remapRefMap` is enabled.
- Exact remapping file and its hash.
- Exact source environment from `mixin.env.refMapRemappingEnv` (or the known default with evidence).
- Exact target/runtime environment.
- The Mixin implementation/version and relevant wrapper configuration.

Mixin plugins, custom `refmapWrapper` classes, and dynamically supplied reference mappers remain unexecuted. If they can affect the effective reference or resource, the result remains dynamic/unresolved.

## 10. Integration with existing target verification

Do not introduce a second class hierarchy cache.

Once a complete mapping path yields a specific target owner, member name, and descriptor:

1. Ask `Forge1201TargetApiVerifier.TargetIndex.classInfo` for target class metadata.
2. Use `lookupMethod` or `lookupField` with the **mapped target descriptor**.
3. Preserve `FOUND`, `MISSING`, and `INCOMPLETE_HIERARCHY` distinctly.
4. Record the exact target classpath artifacts/class bytes that supplied the lookup.
5. Keep target selector/member lookup separate from injection-point bytecode analysis.

A `FOUND` result is structural existence only. A later stage must inspect target method instructions to assess `@At` targets, ordinals, shifts, slices, and injection point behavior. No mapping profile can prove these semantics on its own.

## 11. Proposed implementation sequence

### Schema 1.10A — profile schema and provenance inventory

- Add strict profile parsing and validation, with no selector transformation.
- Require explicit source/target version and namespace declarations.
- Require explicit refmap context bindings; preserve base-table namespace as unknown until declared.
- Validate edge connectivity, direction, symbol coverage, artifact identifiers, and digest format.
- Emit deterministic JSON/text findings for incomplete, mismatched, ambiguous, and missing inputs.
- Record the current repository mapping assets as partial source-edge evidence and record the missing cross-version edge explicitly.
- Add synthetic tests for valid profile, missing edge, namespace/version mismatch, digest mismatch, unsupported schema/format, duplicate edge paths, partial descriptor coverage, and ambiguous paths.

### Schema 1.10B — exact source-edge validation

Only after the profile schema is green:

- Verify the pinned MCPConfig and Mojang source mapping inputs.
- Audit the source table's exact member coverage and its behavior on representative Mixin selectors.
- Establish whether class owners and descriptor types in each supported refmap output are already in the source named namespace or need an explicit class/descriptor edge.
- Do not claim more coverage than the source artifacts and selector fixtures prove.

### Later — cross-version mapping data and target selector checks

This stage remains blocked until a reviewed 1.20.1 → 1.21.1 edge exists for the relevant symbol domains. It may require a connector-maintained, versioned migration catalog with exact per-symbol evidence, rather than a general-purpose third-party mapping file. Once implemented, use the existing target index and keep bytecode injection-point verification as a separate gate.

## 12. Acceptance gate

Schema 1.10 profile support is acceptable only when:

1. A refmap context is bound to a source namespace through explicit, versioned evidence, not by string-name coincidence.
2. Every mapping edge declares version, direction, namespace, provider, format, artifact identity/digest, license status, and supported symbol domains.
3. The profile graph is one complete, deterministic path; zero or multiple paths are explicit failures.
4. Name-only tables are not presented as owner/descriptor-complete mappings.
5. Class names, method names, field names, and descriptor object types are transformed only within declared coverage.
6. Cross-version identity requires an explicit, version-scoped declaration with evidence.
7. Target lookup uses the shared `TargetIndex`; no duplicate hierarchy cache is added.
8. Runtime remapping, plugins, and wrappers remain separate and inert unless their exact behavior is explicitly supplied.
9. Synthetic tests cover all profile/edge failure states before any selector conversion is enabled.
10. Mapping and target lookup success is never described as Mixin/runtime/modpack compatibility.

## 13. Reviewed external contracts

- [Mixin 0.8.5 release](https://github.com/SpongePowered/Mixin/releases/tag/0.8.5)
- [Mixin refmap / soft-reference model](https://github.com/SpongePowered/Mixin/wiki/Introduction-to-Mixins---Obfuscation-and-Mixins)
- [Mixin runtime refmap remapping properties](https://github.com/SpongePowered/Mixin/wiki/Mixin-Java-System-Properties)
- [Mixin 0.8.5 ReferenceMapper source](https://github.com/SpongePowered/Mixin/blob/0.8.5/src/main/java/org/spongepowered/asm/mixin/refmap/ReferenceMapper.java)
- [Mixin 0.8.5 RemappingReferenceMapper source](https://github.com/SpongePowered/Mixin/blob/0.8.5/src/main/java/org/spongepowered/asm/mixin/refmap/RemappingReferenceMapper.java)
- [MCPConfig 1.20.1 joined.tsrg source](https://github.com/MinecraftForge/MCPConfig/blob/master/versions/release/1.20.1/joined.tsrg)
- [NeoForge 1.21.1 ModDevGradle MDK mapping-name note](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/blob/main/README.md)
- [NeoForged Parchment documentation](https://docs.neoforged.net/toolchain/docs/parchment/)
- [NeoForged ModDevGradle documentation](https://docs.neoforged.net/toolchain/docs/plugins/mdg/)

**Design conclusion:** Schema 1.10A now provides the first profile declaration/provenance validator, but the actual 1.20.1 → 1.21.1 cross-version symbol edge remains missing. Selector rewriting is still blocked. Treat that missing edge as an engineering task to solve with evidence, not something to hide behind a generic “Mojang mappings” label.

### Schema 1.10A implementation checkpoint — user-validated green

- Tested source commit: `cc651af52b192fb534d49f3ad494b42e3d15d6f0` on `feat/forge1201-preflight-analyzer`.
- User ran `.\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks`.
- Result: **BUILD SUCCESSFUL in 53s; 14 actionable tasks, all 14 executed.**
- The earlier 494-test run reported 10 failures caused by malformed JSON in the synthetic `validProfile()` fixture. The fixture was repaired without weakening assertions; the subsequent full loader test task passed.

The validator still performs structural profile checks only. It does not load/apply the generated mapping table, validate the semantic contents of mappings, rewrite selectors, or prove runtime compatibility.

### Schema 1.10B source-edge validation — implementation in progress

Current source branch work (not yet user-tested):
- `SrgMojangTable` now exposes exact join coverage counts for Mojang class/method/field rows and MCPConfig class/method/field rows, matched/unmatched method and field rows, duplicate mapping keys, duplicate SRG identifiers, and total output rows.
- The source join explicitly matches obfuscated owner + member name + full JVM descriptor for methods, and obfuscated owner + member name for fields.
- Conflicting Mojang class/member keys or conflicting names for a globally unique SRG ID now fail closed instead of silently overwriting an earlier entry.
- The generated TSV remains only `SRG member ID -> Mojang member name`; it does not emit class-name or descriptor-type mappings.
- New synthetic tests cover owner+descriptor matching, descriptor mismatch, wrong owner, and conflicting SRG IDs.
- Loader build tasks now extract the pinned MCPConfig `config/joined.tsrg` as a declared task output, verify Mojang `client.txt` against the SHA-1 object identifier embedded in its pinned URL, compute SHA-256 for the MCPConfig ZIP, extracted TSRG2 file, and Mojang client mapping, then record the generated TSV SHA-256 and row-coverage counters in a build-local JSON provenance report under `loader/build/reports/mappings/1.20.1/`.
- Raw mapping source files and the generated report remain build-local; no full Mojang mapping file is added to source control.

The user forced loader gate has passed after the test-only TSRG2 fixture correction: `BUILD SUCCESSFUL in 50s; 17/17 actionable tasks` at `493cae748f89f88308a7e1b98ea583d2e58871af`. The build-local provenance JSON has been generated and must now be reviewed. The 1.20.1 → 1.21.1 symbol edge remains missing; no selector rewriting is permitted.


### Schema 1.10B first forced test run — TSRG2 fixture issue corrected

The first user run after the Schema 1.10B implementation successfully completed `:loader:clean`, production compilation, test compilation, mapping extraction, Mojang checksum verification, table generation, and build-local provenance recording. It reported 498 tests with two failures in:
- `joinsMemberNamesByObfuscatedOwnerAndFullMethodDescriptor()`
- `refusesToMatchMethodsWhenTheirFullDescriptorDiffers()`

Both failures occurred during `generateDetailed()` with `IOException`. Source inspection established that the production parser correctly recognizes TSRG2 member rows by a leading tab, while all four synthetic `joined.tsrg` fixtures had used spaces. The malformed fixtures caused member rows to be interpreted as class rows; the two positive cases therefore produced no matches, and the two negative cases passed for the wrong reason.

The test-only correction is committed at `e03c55daa6fd01e0855774e53a4255beedbad1fb`: every synthetic TSRG2 member row now uses the Java text-block escape `\\t`, which produces an actual tab at runtime. Parser behavior and assertions were not weakened, and production source was not changed for this fixture issue.

**Corrected forced loader gate: GREEN.** The user ran `.\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks` at `493cae748f89f88308a7e1b98ea583d2e58871af`: `BUILD SUCCESSFUL in 50s; 17/17 actionable tasks executed`. The next step is to inspect `loader/build/reports/mappings/1.20.1/srg-mojang-provenance.json` from that run. The 1.20.1 → 1.21.1 symbol edge remains missing; no selector rewriting is permitted.

### Schema 1.10B corrected forced loader gate — user-validated green

- Tested code checkpoint: `493cae748f89f88308a7e1b98ea583d2e58871af` on `feat/forge1201-preflight-analyzer`.
- Command: `.\gradlew.bat :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks`.
- Result: **BUILD SUCCESSFUL in 50s; 17 actionable tasks, all 17 executed.**
- Production/test compilation, clean, MCPConfig extraction, Mojang SHA-1 verification, mapping generation, provenance recording, and the full loader test task all passed.
- The preceding two failures were caused by spaces in the synthetic TSRG2 fixtures; the fixtures were corrected to use real tab indentation in `e03c55daa6fd01e0855774e53a4255beedbad1fb`, without production parser changes or weakened assertions.
- The successful build generated `loader/build/reports/mappings/1.20.1/srg-mojang-provenance.json`; review its contents before closing the Schema 1.10B source-edge audit.
- The table remains a partial same-version source normalization edge, `minecraft:forge-srg@1.20.1` → `minecraft:official-mojang@1.20.1`, with method-name and field-name output only. It is not a class/descriptor mapping and is not the cross-version 1.20.1 → 1.21.1 edge. No selector rewriting.
