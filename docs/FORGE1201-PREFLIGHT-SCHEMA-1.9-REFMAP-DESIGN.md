# Schema 1.9 — Mixin Refmap Inventory and Interpretation Design

**Status:** Schema 1.9A resource/table inventory and Schema 1.9B owner-scoped exact lookup correlation are implemented with synthetic regression coverage. The user's forced loader gate passed at `242744d1baf94dc1c523b3a873a6463017184081` (`BUILD SUCCESSFUL in 45s; 14/14 actionable tasks executed`). Follow-up commit `af12df84f62850c49a5a111515e16e76561d8e6c` restores the exact previously tested `build.gradle` blob to remove line-ending-only churn; the cleanup commit has not itself been rerun locally.
**Upstream contract pinned:** SpongePowered Mixin tag 0.8.5.  
**Universal target:** Forge 1.20.1 source mods analyzed for NeoForge 1.21.1 / 21.1.x.  
**Latest user-confirmed green checkpoint:** Schema 1.9A/1.9B at `242744d1baf94dc1c523b3a873a6463017184081` (forced loader tests: `BUILD SUCCESSFUL in 45s; 14/14 actionable tasks executed`).


This document defines the next bounded Mixin-analysis increment. It does not implement selector rewriting, invent source/target namespace identities, load Mixin plugins, or claim that a refmap entry resolves a target member.

## 1. Why this increment is necessary

The current analyzer records the configured refmap path and checks whether that archive resource looks like a JSON object. That is only a resource-presence check. It does not read the actual mapping tables, identify the mapping contexts they contain, correlate source selectors with entries, or model runtime fallback behavior.

A refmap is not a complete general-purpose mapping file. It stores soft references authored in Mixin annotations and the annotation processor's mapped replacements. The runtime chooses a context and owner before looking up a reference. The JSON itself does not fully declare the source/target namespace identities required to convert the values into NeoForge 1.21.1 names.

Schema 1.9 should therefore first inventory and interpret the refmap contract faithfully. Namespace translation and target-member verification remain separate capabilities.

## 2. Pinned Mixin 0.8.5 source audit

The design is based on the actual 0.8.5 tag rather than master, later Mixin versions, or a secondary implementation. The source files below were fetched from that tag; blob SHA values make the reviewed source snapshot explicit.

| Pinned source | Reviewed contract | Git blob |
|---|---|---|
| src/main/java/org/spongepowered/asm/mixin/refmap/ReferenceMapper.java | JSON fields, context selection, owner/reference lookup, pass-through behavior, resource loading | c816133905668391daf86472e20291e76cb22605 |
| src/main/java/org/spongepowered/asm/mixin/refmap/RemappingReferenceMapper.java | Optional SRG-based runtime wrapper and string-replacement behavior | 66eb1514b9636f765b937a1c7ca872c311d31e53 |
| src/main/java/org/spongepowered/asm/mixin/refmap/IReferenceMapper.java | remap and remapWithContext API contracts | e6f4d6f0eb9cee23a3c9d3548227a44c7c1cd23b |
| src/main/java/org/spongepowered/asm/mixin/transformer/MixinConfig.java | Config refmap/refmapWrapper, plugin fallback, mapper loading and wrapper construction | 40e6b8cd18bc4ef6dd84080a0b4537a765bcd1a1 |
| src/main/java/org/spongepowered/asm/mixin/MixinEnvironment.java | Runtime obfuscation context and refMapRemapping options | 0eb13c9c53a3f92a900e707fa4e6dc97156282e3 |
| src/ap/java/org/spongepowered/tools/obfuscation/ReferenceManager.java | Annotation-processor emission of refmap entries | 850b368f3b591e0cec4aaeb29f6e118813df11d7 |
| src/ap/java/org/spongepowered/tools/obfuscation/ObfuscationTypeDescriptor.java / ObfuscationType.java | Obfuscation-context keys and default-context selection | adc7a10908a212bcf91b6344678c672a49b10d99 / 5865fd5694296208ccf127580f714bb446e0d3c7 |
| src/ap/java/org/spongepowered/tools/obfuscation/AnnotatedMixin.java / AnnotatedMixinElementHandler.java | Mixin owner key uses the class's internal bytecode name | f3b918a8029f0647b2105f688db4505ab9842667 / 2e7c7415f498aa03e829bc8638f2aa00263a2ea7 |
| src/ap/java/org/spongepowered/tools/obfuscation/AnnotatedMixinElementHandlerInjector.java | Soft-reference keys emitted for injector method and At.target strings | fb6b96239547f8a099fc08004c82477bd66118ca |

Pinned source links:

- [Mixin 0.8.5 release](https://github.com/SpongePowered/Mixin/releases/tag/0.8.5)
- [ReferenceMapper.java](https://github.com/SpongePowered/Mixin/blob/0.8.5/src/main/java/org/spongepowered/asm/mixin/refmap/ReferenceMapper.java)
- [RemappingReferenceMapper.java](https://github.com/SpongePowered/Mixin/blob/0.8.5/src/main/java/org/spongepowered/asm/mixin/refmap/RemappingReferenceMapper.java)
- [MixinConfig.java](https://github.com/SpongePowered/Mixin/blob/0.8.5/src/main/java/org/spongepowered/asm/mixin/transformer/MixinConfig.java)
- [MixinEnvironment.java](https://github.com/SpongePowered/Mixin/blob/0.8.5/src/main/java/org/spongepowered/asm/mixin/MixinEnvironment.java)
- [ReferenceManager.java](https://github.com/SpongePowered/Mixin/blob/0.8.5/src/ap/java/org/spongepowered/tools/obfuscation/ReferenceManager.java)
- [AnnotatedMixinElementHandlerInjector.java](https://github.com/SpongePowered/Mixin/blob/0.8.5/src/ap/java/org/spongepowered/tools/obfuscation/AnnotatedMixinElementHandlerInjector.java)

### 2.1 Refmap JSON shape

ReferenceMapper owns two non-transient maps:

- mappings: owner → source reference → mapped reference
- data: obfuscation context → owner → source reference → mapped reference

The serialized top-level shape is therefore:

    {
      "mappings": {
        "com/example/mixins/ExampleMixin": {
          "Lnet/minecraft/example/Example;field:I": "Labc;field_123:I"
        }
      },
      "data": {
        "searge": {
          "com/example/mixins/ExampleMixin": {
            "Lnet/minecraft/example/Example;field:I": "Labc;field_123:I"
          }
        },
        "notch": {
          "com/example/mixins/ExampleMixin": {
            "Lnet/minecraft/example/Example;field:I": "La;f:I"
          }
        }
      }
    }

The example values above are illustrative only; they are not a recommended namespace mapping for the connector.

Important format details:

1. mappings is the annotation processor's default obfuscation-environment table.
2. data contains separate tables keyed by obfuscation-type/context strings.
3. Each context table has the same owner/reference/value structure as mappings.
4. The owner key is the Mixin class's internal bytecode name (slash-separated), not the target class name and not a display name.
5. The reference key and mapped value are opaque strings to the runtime mapper. They may represent method selectors, field selectors, or class references; the refmap format does not attach a typed record to each pair.
6. The serialized format does not carry a formal schema version or a complete source-namespace/target-namespace declaration. readJson deserializes the object through Gson into ReferenceMapper.
7. The context, resource name, and read-only marker are transient runtime state, not serialized mapping tables.

### 2.2 How the annotation processor emits entries

AnnotatedMixinElementHandler sets its mapping owner to mixin.getClassRef(). AnnotatedMixin builds that value using TypeUtils.getInternalName(type). The runtime context interface also exposes getClassRef() as the internal mixin class reference.

ReferenceManager adds each mapping under ObfuscationType.getKey(). If that type is the annotation processor's configured default obfuscation environment, it also writes the same entry into the base mappings table. The default is chosen from annotation-processor configuration; it is not a universal property of the refmap file.

The default MCP obfuscation service in Mixin 0.8.5 defines searge and notch keys; the ForgeGradle 3 obfuscation service also defines a searge key. Other services may define other keys. Treat all data keys as opaque identifiers unless a separately reviewed environment profile explains them.

ReferenceMapper.addMapping removes whitespace from the source reference key before storing it. It skips a generated mapping when the normalized source key equals the mapped value. The analyzer must preserve both the original annotation string and the AP-normalized key candidate; it must not invent additional selector spellings.

The injector annotation processor directly receives the original strings in the injector method array and the At.target string. For an At NEW injection point it also has a separate class-reference path through args[class]. Injector Desc targets are processed through a path with no string reference key; ReferenceMapper.addMapping ignores a null reference. Schema 1.9 must not fabricate refmap lookup keys for those Desc annotations.

### 2.3 Runtime lookup behavior

The pinned ReferenceMapper.remapWithContext(context, className, reference) follows this sequence:

1. Start with the base mappings table.
2. If context is non-null, look up the complete context table data[context].
3. If that entire context table does not exist, fall back to base mappings.
4. If the context table does exist, use it as the selected table. A missing owner or missing reference in that selected context does not fall through entry-by-entry to base mappings.
5. Look up the owner exactly, then the reference key exactly. If no mapped value is found, return the original reference unchanged.

The no-owner case (className == null) scans owners and returns the first table containing the reference. The implementation uses map iteration and does not establish a deterministic owner priority. The preflight analyzer must not use that path as proof of a unique mapping.

MixinConfig sets the mapper context from MixinEnvironment.getRefmapObfuscationContext(). That method returns the mixin.env.obf override when one is supplied; otherwise it returns the active environment's obfuscation context. A static archive analyzer normally does not know the eventual runtime value. Therefore Schema 1.9 must inventory context tables without silently selecting searge, notch, or any other context as the user's runtime context.

A missing refmap is not an identity mapping. ReferenceMapper.read returns the default pass-through mapper when the resource is absent or cannot be read. Runtime may then return the original selector even though a production obfuscated target expects a remapped selector.

### 2.4 Effective refmap resource selection and wrappers

MixinConfig reads the config's refmap value. If it is absent, Mixin asks the config plugin for getRefMapperConfig(). If that returns null (or no plugin supplies a path), Mixin uses ReferenceMapper.DEFAULT_RESOURCE, which is mixin.refmap.json.

Consequences for static analysis:

- An explicit refmap path is an exact classpath resource path; Mixin passes that path to its resource service. Do not prepend the config package or reinterpret it as a path relative to the mixin JSON.
- When no path is explicit and no plugin is declared, mixin.refmap.json is the default candidate and should be inspected.
- When a plugin is declared and refmap is absent, the plugin may supply a different path. The analyzer must report the effective resource as plugin-dependent rather than assert that the default path is the one Mixin will use. It may still inventory mixin.refmap.json as a candidate with explicitly weaker provenance.
- If refmapWrapper is set, Mixin reflectively loads a custom IReferenceMapper wrapper using the mixin package and a constructor taking MixinEnvironment and IReferenceMapper. Its behavior cannot be inferred from the JSON alone.
- Neither plugin nor refmapWrapper code may be loaded, instantiated, initialized, or executed by preflight.

The optional RemappingReferenceMapper is a separate runtime stage. In Mixin 0.8.5 it is enabled only under the remapRefMap option when a configured SRG file exists. Its source environment defaults to searge and can be overridden by refMapRemappingEnv. It loads MD and FD SRG lines, derives member-name substitutions, applies string replacements after the base refMap.remap(...), and delegates remapWithContext(...) directly to the underlying refmap without applying those replacements. This is not equivalent to another data table and must not be flattened into the base refmap model.

## 3. Consequences for Legacy Connector

The current Forge1201MixinAnalyzer.ConfigFinding stores refmapPath and a ResourceStatus. Its current inspection checks only safe path, duplicate archive entries, size limit, JSON parseability, and an object root. The config parser does not yet preserve refmapWrapper; an omitted refmap is currently represented as not declared rather than resolved to the Mixin default/plugin-dependent behavior.

Schema 1.9 should close that gap without conflating three separate questions:

1. Was a resource declared/discovered and safely parsed?
2. Does an exact source owner/reference key exist in a selected refmap table?
3. Can the mapped value be converted to the requested NeoForge target namespace and verified against target bytecode?

The first two can be addressed by this increment, within explicit evidence limits. The third requires separate, explicit mapping and target-index work.

## 4. Proposed Schema 1.9 result model

Names below are design candidates; final record names may follow the existing analyzer's naming conventions.

### 4.1 Resource-level finding

One deterministic RefmapResourceFinding per unique resource path and source archive, carrying:

- Exact resource path and origin: explicit config field, Mixin default candidate, or plugin-supplied/unknown candidate.
- Config paths that reference or may select the resource.
- Archive entry count/duplicate state, compressed and inflated sizes where available, and SHA-256.
- Parse status and warnings.
- Whether mappings and data fields were present and whether their values had the expected object/map shapes.
- Sorted context keys, owner counts, reference-entry counts, and total parsed entries.
- Whether unknown top-level fields or unsupported value types were encountered.

Use the existing outer-archive entry index and bounded byte reader. Do not add another ZIP walker. Multiple configs sharing one resource should share the parsed inventory while retaining each config's provenance.

### 4.2 Mapping-table and entry inventory

Preserve the two table families separately:

- Base/default mappings table, where the context identity is not encoded in the JSON field itself.
- Each data[contextKey] table, preserving its opaque context key exactly.

Within each table, retain sorted entries with:

- Owner mixin internal name.
- Raw serialized reference key.
- Mapped reference value.
- An AP-normalized source key candidate (whitespace removed), without discarding the raw key.
- Resource path, context identity, and evidence.

Do not coerce every value into a method. A string value may represent a method selector, field selector, or class reference. Any classification is advisory and must preserve the raw strings.

### 4.3 Owner-scoped lookup correlation

Correlate only reference-bearing source annotations for which the pinned Mixin 0.8.5 annotation processor actually emits a string refmap key:

- Each core injector annotation's string method[] selector, when effective remap=true.
- Each At.target string, when its injection point has effective remap=true.
- The At NEW class argument path (args[class]) as a separate candidate where present and remappable.

Do not treat injector target=Desc as an ordinary refmap string key: the Mixin 0.8.5 annotation processor passes a null reference for that path, and ReferenceMapper.addMapping declines null reference keys. Do not invent a key from the nested owner/name/descriptor.

The owner key must be the source mixin's internal class name (for example com/example/mixins/ExampleMixin). Existing report fields expose a dotted display name; Schema 1.9 should carry the original classfile internal name explicitly rather than relying on a later display-name conversion.

For each candidate, preserve:

- Source archive/config/section/mixin class and annotation location/index.
- Source annotation's raw selector and remap flag.
- Raw candidate key and AP whitespace-normalized key.
- Owner internal name.
- Candidate refmap resource(s).
- Requested context, if explicitly supplied to preflight; otherwise the complete available context inventory.
- Exact matching entries and any cross-context differences.

### 4.4 Context handling rules

- If a caller supplies a runtime context, look up that exact context key.
- If the supplied context table is absent entirely, record that Mixin 0.8.5 falls back to base mappings.
- If the selected context table exists but the owner/reference pair is absent, record no entry in that table. Do not fall back to the base table for that individual miss.
- If no runtime context is supplied, inventory all contexts and their candidate values. Do not choose a context by convention.
- Never perform an ownerless search to resolve a source mixin reference.
- If different available contexts map the same owner/reference to different values, report that difference as context-dependent, not as a collision that can be resolved without a runtime profile.
- If the stored value is identical to the query, distinguish an explicit identity entry from no entry. Absence of an entry is the runtime mapper's pass-through case, not evidence of a valid target identity.

### 4.5 Candidate status vocabulary

Resource-level states:

- REFMAP_RESOURCE_EXPLICIT
- REFMAP_RESOURCE_DEFAULT_CANDIDATE
- REFMAP_RESOURCE_PLUGIN_DEPENDENT
- REFMAP_RESOURCE_MISSING
- REFMAP_RESOURCE_DUPLICATE_ENTRY
- REFMAP_RESOURCE_INVALID_PATH
- REFMAP_RESOURCE_INVALID_JSON
- REFMAP_RESOURCE_UNSUPPORTED_SHAPE
- REFMAP_RESOURCE_LIMIT_EXCEEDED
- REFMAP_RESOURCE_PARSED
- REFMAP_RESOURCE_EMPTY
- REFMAP_CUSTOM_WRAPPER_NOT_EXECUTED

Lookup-level states:

- REFMAP_ENTRY_FOUND
- REFMAP_ENTRY_NOT_FOUND_PASSTHROUGH
- REFMAP_EXPLICIT_IDENTITY_ENTRY
- REFMAP_CONTEXT_TABLE_SELECTED
- REFMAP_CONTEXT_MISSING_DEFAULT_FALLBACK
- REFMAP_CONTEXT_NOT_SELECTED
- REFMAP_CONTEXT_DEPENDENT_VALUES
- REFMAP_OWNER_UNAVAILABLE
- REFMAP_PLUGIN_DEPENDENT
- REFMAP_SELECTOR_NOT_EMITTED_AS_REF
- REFMAP_ANALYSIS_INCOMPLETE

Final enum design may consolidate states, but it must keep resource presence, table selection, exact entry presence, and namespace verification separate. A single REFMAP_VALID boolean is insufficient.

## 5. Namespace and mapping provenance requirements

Refmap context keys are annotation-processor obfuscation-type identifiers. They are not guaranteed to be globally standardized namespace names, and the base mappings table does not itself name its environment. The JSON also does not declare a complete source namespace, target namespace, game version, or mapping artifact.

Before any future stage transforms a refmap value toward NeoForge 1.21.1, the caller must supply an explicit mapping profile with at least:

- Stable profile ID and mapping direction.
- Minecraft source and target versions.
- Source namespace ID and target namespace ID.
- Mapping format and provider/tool identity/version.
- Exact mapping artifact identity and SHA-256 digest.
- The refmap context key (or base-table identity) that the profile applies to.
- Any intermediate mapping stages required to bridge the refmap output into the target namespace.

A profile for one game version or namespace must not be reused for another merely because a name looks familiar. searge, notch, official, intermediary, Mojang names, and custom keys are not interchangeable labels.

No same-name identity mapping may be inferred. No mixin.refmap.json entry may be called target-compatible solely because the mapped string is syntactically valid or appears in a source mapping file.

## 6. Runtime features that remain explicitly out of scope

Schema 1.9 static analysis must not:

- Load or execute IMixinConfigPlugin, IReferenceMapper wrappers, or dynamic selector implementations.
- Guess the effective context from the refmap's first data key.
- Assume searge is the source environment unless a caller explicitly supplies that context.
- Silently fall back from a present context table to the base table for an individual missing selector.
- Simulate the RemappingReferenceMapper SRG substring replacement without the exact runtime option values and exact mapping artifact.
- Convert mapped reference strings into final 1.21.1 target members without an explicit, versioned mapping profile.
- Claim target member presence, descriptor parity, injection point existence, or runtime Mixin success.

## 7. Bounded parsing and archive safety

Initial implementation limits should be conservative and visible in the result. Reuse the existing indexed outer archive and path-validation routines. Do not add another ZIP walker. Current implementation ceilings, to be profiled against representative mod archives:

- 1 MiB inflated bytes per refmap resource.
- 8 MiB aggregate inflated bytes across refmap resources in one analyzed archive.
- 256 distinct resource paths.
- 64 obfuscation context tables per resource.
- 20,000 owner buckets per resource (counted across base and context tables).
- 100,000 mapping entries per resource (counted across base and context tables).
- 128 JSON nesting levels for the strict duplicate-key pre-scan and bounded Gson materialization.

If a limit is reached, preserve resource identity, the limit that was reached, partial counts where available, and an explicit incomplete status. Never silently truncate and then call the refmap completely analyzed. The bounded reader may consume at most one byte beyond a byte cap to detect overflow; that byte is charged to the aggregate refmap budget. These values are implementation guardrails, not performance-profiled limits.

## 8. Synthetic regression matrix

Before source integration is considered complete, synthetic JSON and ASM/ZIP fixtures should cover:

1. Both mappings and multiple data contexts with stable JSON/text serialization.
2. Base mappings only, data-only, empty tables, and missing optional top-level map fields.
3. Exact owner + exact reference match, missing owner, missing reference, and explicit identity mapping.
4. Requested context present; requested context wholly absent with base fallback; requested context present but key absent with no per-entry fallback.
5. No context supplied with multiple differing table values; no automatic selection.
6. Same selector key in different owners; ownerless query must not choose an arbitrary owner.
7. Reference keys containing whitespace, preserving raw text and the AP's whitespace-stripped form.
8. Injector method[], At.target, and At NEW args[class] correlation.
9. Desc selector data not treated as a string refmap entry.
10. remap=false source annotations not correlated as AP remap requests.
11. Explicit config refmap; omitted refmap and default resource; plugin-declared omitted path; explicit custom refmapWrapper.
12. Missing resource, unsafe path, duplicate ZIP entries, malformed JSON, wrong root/map/value types, and per-resource/aggregate limit exhaustion.
13. Multiple configs sharing one refmap with separate config provenance.
14. Unknown top-level fields retained as warnings rather than silently treated as known semantics.
15. Deterministic output independent of ZIP entry ordering.

Tests must use generated fixtures and must not instantiate or execute Mixin runtime services or plugins.

## 9. Implementation sequence

### Schema 1.9A — resource and table inventory

**Implementation and forced loader test gate: PASSED on 2026-11-14** at `a59fdac24ab848e31152901b9a4f4f3a5db5413d` (`:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks`; `BUILD SUCCESSFUL in 50s; 14/14 tasks executed`).

- Adds immutable RefmapResourceFinding, RefmapTableFinding, and RefmapEntryFinding records to the existing Forge1201MixinAnalyzer.Result.
- Resolves explicit refmap paths, the mixin.refmap.json default candidate, and a weaker plugin-dependent default candidate. Multiple configs sharing a path share one parsed inventory while retaining sorted config/origin provenance.
- Preserves refmapWrapper as an inert class-name string and warns that it is never loaded or executed.
- Parses the base mappings table and every data context table up to the configured guardrail; preserves opaque context names, owner internal names, raw reference keys, mapped values, unknown top-level field names, exact byte digest, and ZIP-declared/compressed/inflated sizes.
- Rejects duplicate JSON object member names before Gson materialization, avoiding silent last-value-wins loss. Wrong JSON types/shapes and invalid UTF-8 remain explicit findings.
- Reuses the existing indexed outer-archive entries and readBounded method. It does not add a ZIP walker, Mixin runtime, plugin loader, wrapper loader, selector remapper, or namespace converter.
- Adds JSON data through the existing result record and prints a human-readable refmap inventory in preflight text output. The unified report schema/analyzer versions advance to 1.9 / 1.9.0.
- Implements the current guardrails as public constants: 1 MiB/resource, 8 MiB aggregate, 256 paths, 64 contexts/resource, 20,000 owner buckets/resource, and 100,000 entries/resource. These remain conservative, unprofiled initial limits; the bounded reader consumes at most one extra byte to detect overflow and accounts for those bytes against the aggregate budget.
- Adds synthetic coverage for base/context tables, shared-path provenance, default/plugin-dependent candidates, wrapper preservation, unknown top-level fields, duplicate JSON keys, unsupported table/value shapes, deterministic inventory, per-resource and aggregate byte caps, JSON nesting depth, and the context-table cap.

**Validation boundary:** this gate confirms compilation and synthetic tests for 1.9A resource/table inventory only. It does not prove runtime Mixin application, namespace conversion, target-member existence, or mod compatibility. Source and target namespaces remain unresolved.

### Schema 1.9B — explicit-context exact lookup correlation

**Implementation and forced loader test gate: PASSED on 2026-11-14** at `242744d1baf94dc1c523b3a873a6463017184081` (`:loader:clean :loader:test --no-daemon --console=plain --rerun-tasks`; `BUILD SUCCESSFUL in 45s; 14/14 actionable tasks executed`).

- Preserve mixin classfile internal owners beside display names and use exact owners for lookup.
- Correlate only Mixin 0.8.5 AP string-reference paths: injector `method[]`, fully-qualified `@At.target`, `@At("NEW")` constructor target, and separate `args[class]` constructor-selector path. `@Desc` paths remain non-string-ref evidence; no synthetic key is invented.
- Preserve raw annotation text and its AP whitespace-stripped source-key candidate. Match that candidate exactly against the serialized refmap key and exact mixin owner.
- Distinguish an absent-entry pass-through from an explicit identity entry.
- Accept runtime context only via explicit analyzer input (`--refmap-context`; Gradle `-PpreflightRefmapContext=<key>`). Without context, inventory base/context candidates without selecting one.
- Match Mixin 0.8.5 context behavior: an absent context table falls back to base; a present table with a missing owner/reference does not.
- Inherit effective injector `remap` into `@At.remap` and slice boundary `@At` unless explicitly overridden.
- Plugins and wrappers remain inert/dynamic. No namespace conversion, target-member verification, selector rewriting, or runtime compatibility claim is added.
- Synthetic tests cover owner isolation, source whitespace normalization, present-context/missing-key, absent-context fallback, no-context divergent candidates, identity versus absence, inherited/overridden `@At.remap`, and both NEW mapping paths.

**Validation boundary:** this gate confirms compilation and synthetic loader tests for owner-scoped exact lookup correlation. It does not validate refmap namespace conversion, target-member or target-instruction resolution, runtime Mixin application, real mod behavior, or modpack compatibility. The follow-up `af12df84f62850c49a5a111515e16e76561d8e6c` changes only `build.gradle` line endings back to the exact tested parent blob; no Gradle rerun has been recorded at that cleanup tip.

### Next stage — Schema 1.10 mapping-profile design

The Schema 1.10 contract is now documented in [`FORGE1201-PREFLIGHT-SCHEMA-1.10-MAPPING-PROFILE-DESIGN.md`](FORGE1201-PREFLIGHT-SCHEMA-1.10-MAPPING-PROFILE-DESIGN.md). It audits the current source/target mapping inputs, distinguishes context keys from namespace IDs, defines versioned directed edges and artifact provenance, and records that the 1.20.1 → 1.21.1 cross-version symbol edge is currently missing.

The next bounded implementation is Schema 1.10A profile parsing and validation only. Do not yet rewrite selectors. Require explicit source/target namespaces, context bindings, artifact digests, symbol-kind coverage, and deterministic missing/ambiguous/partial states. Reuse `Forge1201TargetApiVerifier.TargetIndex`; do not create a second remapper or hierarchy cache.

## 10. Acceptance gate

Schema 1.9 is acceptable only when:

1. Every selected refmap resource has an explicit origin and parse/limit/duplicate status.
2. Both refmap table families and their opaque contexts are represented without data loss.
3. Exact owner-scoped entries are correlated only when their source annotation is known to emit a refmap key.
4. Context fallback behavior matches Mixin 0.8.5, including the critical distinction between a missing context table and a missing key inside a present context table.
5. Plugin-supplied paths and custom wrappers remain explicit unknown/dynamic behavior and are never executed.
6. No source/target namespace conversion is claimed without a versioned mapping profile.
7. Forced :loader:clean :loader:test --no-daemon --console=plain --rerun-tasks passes.
8. A later consolidated build and real mod/client/server validation remain separate gates.

**Design conclusion:** Schema 1.9 should make Mixin refmaps inspectable and explainable first. It should not pretend a refmap is a complete mapping database or that parsing one tells us whether the corresponding injection will work on NeoForge 1.21.1.
