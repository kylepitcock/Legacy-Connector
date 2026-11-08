# Legacy 1.20.1 fork porting notes (kentvillamor08)

> Written by the contributor kentvillamor08 in the fork `kentvillamor08/Legacy-Connector-1.20.1` while building an independent Forge 1.20.1 pipeline
> (pilot mod: Symbiote). Included unchanged below for reference and credit. It describes the fork's design (separate `Forge1201*` loader classes, scoped
> call-site adapter profiles, offline verifiers), not our main pipeline: for that see [Legacy-1.20.1-Estimate](Legacy-1.20.1-Estimate.md).
> The ported offline tools live in `loader/.../tools/forge1201`. Gradle task names and commands mentioned here (`inspectForge1201`, `transformForge1201`, ...)
> belong to the fork's build files and do not exist in this repository.

# Forge 1.20.1 mods on NeoForge 1.21.1

## Goal

Keep Legacy Connector's host runtime on **Minecraft 1.21.1 + NeoForge 21.1.x + Java 21**. Add a new compatibility family that can translate selected **Minecraft 1.20.1 Forge 47.x gameplay/content mods** so they can run inside that host.

This is not a Forge 1.20.1 build of Legacy Connector, and it is not a plan to downgrade the runtime.

## Scope

In scope:

- Gameplay and content mods: items, blocks, entities, abilities, recipes, GUIs, rendering, networking, world content and their ordinary gameplay hooks.
- Required libraries/APIs when there is no usable 1.21.1 NeoForge release.
- Reusing native 1.21.1 NeoForge dependencies where they are available and compatible.
- Keeping the original input jars untouched; translate/cache derived jars as the existing loader does.

Out of scope for the initial implementation:

- Performance/optimization mods.
- Old-engine patches, launch tweakers, and coremods that modify vanilla internals.
- General support for every 1.20.1 mod on the first milestone.
- Loading both a translated old copy and a native 1.21.1 copy of the same mod at once.

A native 1.21.1 NeoForge mod should be used instead of its older 1.20.1 build. A 1.20.1 jar should only be translated when it supplies functionality that is not already available natively in the pack.

## First compatibility target: Symbiote: A Bonding Experience

The current pilot is **Symbiote 1.1.3**, a Minecraft 1.20.1 Forge 47.x content mod. Its original JAR is kept in the user's private test repository and is used only as a local analysis/test input. Its All-Rights-Reserved binary and assets must not be copied into this public repository.

The static report for the exact JAR records:
- 278 class files and Java 17 bytecode (compatible with the Java 21 runtime at the bytecode level).
- Two mandatory metadata dependencies: Minecraft 1.20.1 and Forge 47.x.
- 121 distinct Forge API class references and 202 distinct Forge API field/method references.
- 1,220 distinct Minecraft field/method references.
- 123 Symbiote classes with direct Forge API references.
- One Curios API class reference, in `CuriosRebondCompat`; Curios is not declared as a mandatory dependency in the source metadata.
- An `EpicFightArmorDiagnostics` class that checks mod presence, with no direct Epic Fight API class reference detected.

The report is a static symbolic inventory, not a runtime trace. It does not prove that any class or code path loads successfully.

### Symbiote dependency and integration profile

| Dependency/API | Source evidence | Planned handling |
|---|---|---|
| Minecraft | Required `minecraft` dependency in `mods.toml` | Rewrite to the host's 1.21.1 range in derived metadata |
| Forge | Required `forge` dependency in `mods.toml` | Rewrite to the `neoforge` dependency in derived metadata |
| Curios | `CurioDropsEvent` referenced only by `CuriosRebondCompat`; not declared in `mods.toml` | Keep isolated as an optional integration; test with Curios present and absent |
| Epic Fight | Presence check by mod ID; no direct API type detected | Do not add a dependency unless deeper inspection finds actual API use |

The first porting target should be a dedicated bootstrap/registry slice, followed by networking, gameplay events and client rendering. The class-level inventory is documented in the private testbench; the public project should not contain the original test JAR or its assets.
## Technical milestones

1. **Jar identification**
   - Recognize 1.20.1 Forge jars that contain `META-INF/mods.toml`.
   - Read their Minecraft/Forge requirements and mod declarations.
   - Do not capture native 1.21.1 NeoForge mods or unrelated Forge versions.
   - Detect and reject unsupported engine-patch/coremod jars with a useful report.

2. **Dedicated metadata and archive path**
   - Keep the Forge 1.20.1 family separate from the 1.7.10/1.12.2 `LegacyVersion` enum and `LegacyJarTransformer`.
   - Convert Forge `mods.toml` to NeoForge `neoforge.mods.toml`, including Forge-to-NeoForge platform dependency IDs and the `mandatory` to `type` dependency syntax conversion.
   - Create a derived JAR in a separate cache; do not alter the user's original JAR.
   - Preserve unrelated archive entries and remove stale signature files after metadata changes.
   - Keep the current safe-skip discovery behavior until all bytecode/API translation stages are ready.

3. **Modern bytecode and mapping**
   - Add a pinned 1.20.1-to-1.21.1 mapping set for class, descriptor, field and method references.
   - Do not apply the old SRG-to-MCP logic to modern Forge jars.
   - Add narrowly scoped call-site adapters only for APIs whose semantics changed; do not use a global package rename as a substitute for API adaptation.

4. **Runtime API compatibility**
   - Keep runtime shims isolated from the pre-Minecraft loader service.
   - Bridge only APIs evidenced by supported mods, prioritizing entrypoints, registries, configuration, events, networking, menus/screens and client rendering.
   - Handle 1.20.1-to-1.21.1 Minecraft API changes deliberately.

5. **Pilot validation**
   - Start with Symbiote 1.1.3 and its exact JAR/metadata.
   - Test client and dedicated-server startup separately.
   - Verify registration, models/textures, entities, abilities, networking, UI/HUD and save/reload behavior in separate stages.
   - Record each feature as working, partial or unsupported; a metadata-translated JAR is not proof of gameplay compatibility.
## Compatibility policy

This is an API compatibility layer, not a universal guarantee. A mod is supported only to the extent that the Minecraft/Forge APIs it uses have been bridged and its required dependencies are available. Compatibility reports should identify missing APIs and unsupported hooks rather than silently claiming success.

## Inspecting the first real mod jar

The loader now includes an offline bytecode inventory tool. It does not define, load or execute the target mod. It reports:

- mods.toml mod IDs and declared dependencies.
- Forge @Mod entrypoint classes.
- Manifest-declared Mixin configurations and FML coremod entrypoints.
- Minecraft/Forge/Mixin classes referenced by the bytecode.
- Frequently referenced Minecraft/Forge method and field symbols.

After placing the actual Forge 1.20.1 jar somewhere accessible on disk, run from the project root:

```powershell
.\gradlew.bat :loader:inspectForge1201 -Pforge1201Jar="F:\path\to\symbiote-1.1.3.jar"
```

The report is printed to the Gradle console. The exact jar and dependency versions used for the pilot should be recorded; do not assume the mod's latest release or a dependency page alone defines the bytecode API surface.

Run the read-only mapping decision audit against the same original input JAR:

```powershell
.\gradlew.bat :loader:auditForge1201Mappings -Pforge1201Jar="F:\\path\\to\\symbiote-1.1.3.jar"
```

This command classifies reviewed namespace moves, known semantic adapters and unreviewed Forge API types. It does not rewrite the JAR. A package relocation is only a candidate until the exact class and member signatures are verified against the pinned NeoForge 21.1.x artifact.

After the mapping catalog has been reviewed, verify the remaining namespace-relocation candidates against the **actual target game's compile classpath** configured by the root NeoForge ModDev project. The candidate count is expected to change as additional APIs are reclassified for manual migration:

```powershell
.\gradlew.bat :verifyForge1201TargetApi -Pforge1201Jar="C:\\path\\to\\symbiote-1.1.3.jar"
```

This task runs in the root project's target compile classpath while the `:loader` module remains independent of Minecraft/NeoForge classes. It reads class-file structures with ASM and checks target class presence plus exact method/field descriptors, including inherited members where the visible hierarchy is complete. Missing classes, missing members, incomplete hierarchy lookups, and member descriptors that contain manual-adapter types are reported separately. No target class is loaded or initialized and no JAR bytes are changed. A structural match is not proof of behavioral compatibility; manual-adapter types and Minecraft 1.20.1-to-1.21.1 changes remain separate.

## Upstream maintenance strategy

The 1.20.1 port should remain an isolated extension of the maintained Legacy Connector codebase, not an unrelated standalone reimplementation. Shared 1.7.10/1.12.2 loader and runtime fixes should continue to arrive from the upstream project, while 1.20.1-only transformation and compatibility code stays in its own classes and package paths.

Maintain a Git remote for the upstream repository and merge its current default branch into a disposable integration/preview branch before updating the active 1.20.1 port branch. Review conflicts rather than copying upstream folders wholesale. Prefer upstream's behavior for shared fixes after checking the affected code; preserve and retest the 1.20.1 adapter path when a shared file changes. Run the project's normal tests and static verifiers on the preview before promoting it.

Avoid force-pushing the active port branch. If an upstream sync conflicts with 1.20.1 work, abort or retain the preview branch for investigation rather than resetting the working port.

## Scoped call-site adapters

Some Forge-to-NeoForge changes cannot be represented by a package or descriptor remap alone. The bytecode transformer therefore accepts an optional external adapter profile. The public loader implements reusable, narrowly scoped operations; a profile supplies the exact original class/method selectors and expected instruction patterns for a particular mod.

Adapter profiles are JSON files kept outside this repository when they contain mod-specific analysis or derived implementation details. They are applied **before** the package relocation pass and are never embedded in the output JAR.

The supported profile schema is version 1. Each rule identifies one class by JVM internal name, then one method by its original name and full descriptor. Each operation declares an exact number of expected matches. Current operation types are:

- `field-descriptor`: replace a field instruction descriptor when owner, name, original descriptor and opcode all match.
- `method-descriptor`: replace a method invocation descriptor when owner, name, original descriptor and invocation opcode all match.
- `method-declaration-descriptor`: change the selected method's declaration descriptor, with an exact expected source descriptor.
- `frame-local-type`: change the declared type at a specified JVM local-variable slot in an explicit `F_FULL`/`F_NEW` stack-map frame. Compressed frames are not guessed; a missing match fails the transform.
- `local-variable`: change the name and descriptor of a LocalVariableTable entry selected by slot, old name and old descriptor.
- `lazy-dist-gate`: replace exact static `DistExecutor.unsafeRunWhenOn(Dist, Supplier)` calls with a generated private synthetic helper that compares the requested side against `FMLEnvironment.dist` (the NeoForge 1.21.1 field) before evaluating the supplier and running its `Runnable`.
- `checkcast-type`: replace a specific `CHECKCAST` target type.
- `instruction-sequence`: replace an exact sequence of typed ASM instructions with another sequence. Recipes support local-variable, field, method, type, constant, and no-operand instructions; optional minimum stack/local limits are applied after a rewrite. Patterns crossing labels, frames, or control-flow instructions are rejected.

Example profile structure using synthetic names:

```json
{
  "schemaVersion": 1,
  "id": "example-profile",
  "rules": [
    {
      "className": "example/mod/ExampleHandler",
      "methodName": "onEvent",
      "methodDescriptor": "(Lexample/api/ExampleEvent;)V",
      "operations": [
        {
          "type": "method-descriptor",
          "owner": "example/api/ExampleEvent",
          "name": "accept",
          "descriptor": "(Lexample/api/OldValue;)V",
          "replacementDescriptor": "(Lexample/api/NewValue;)V",
          "opcode": "INVOKEVIRTUAL",
          "expectedMatches": 1
        }
      ]
    }
  ]
}
```

The loader rejects unknown schema versions, duplicate selectors, missing required methods, unexpected match counts, and unrecognized instruction patterns. A mismatch aborts the transform before the output archive is finalized; it does not silently skip an adapter or emit a partially adapted JAR. With no profile supplied, the existing package-relocation path is unchanged.

For a local profile file, pass its path to the existing task:

```powershell
.\gradlew.bat :loader:transformForge1201 `
  -Pforge1201Jar="F:\path\to\original-forge-1.20.1.jar" `
  -Pforge1201Output="F:\path\to\preliminary-neoforge-1.21.1.jar" `
  -Pforge1201AdapterProfile="F:\private-testbench\profiles\adapter-profile.json"
```

The transform console reports the profile ID, number of rules applied and number of operations applied. This is still a static rewrite, not proof of target method linkage, mod bootstrap or gameplay correctness. Run the loader tests and the translated-JAR verifier after every profile change.


## Current status

- Added a separate metadata detector for jars whose `mods.toml` declares JavaFML 47.x, Minecraft 1.20.1 and Forge 47.x.
- The loader scans both `mods/` and `mods/1.20.1/`, then reports and safely skips recognized 1.20.1 jars instead of passing them to the 1.7.10/1.12.2 translator or NeoForge's native mod discovery.
- Added detector tests for a Forge 1.20.1 example, NeoForge metadata, a different Minecraft version and a missing Forge dependency.
- The detector now extracts each `[[dependencies.<modid>]]` entry (mod ID, version range, mandatory flag, ordering and side) and includes a dependency summary in the safe-skip warning. The inspector test fixture models a multi-dependency Forge 1.20.1 mod; it is not a statement about Symbiote's dependency list.
- Added a native dependency audit that scans installed NeoForge jars for mod IDs in `[[mods]]` sections only, then classifies Forge/Minecraft platform bridges, native ID candidates, and missing required/optional dependencies. The audit explicitly does not claim that a matching mod ID proves API or version compatibility.
- Added a regression test ensuring IDs mentioned only in another mod's dependency list are not mistaken for installed mods.
- Added an offline ASM-based API usage analyzer and a :loader:inspectForge1201 Gradle task. It reports entrypoints, mixin/coremod metadata, referenced game API classes, method/field symbols, and direct Forge/optional-API invocation call sites without executing the jar. A synthetic bytecode test checks call-site ownership as well as the global symbol inventory. The analyzer supports an optional class#method filter and the class wildcard ClassName#*; wildcard behavior is covered by a synthetic regression test. The user has successfully run :loader:clean :loader:test --no-daemon after this addition and generated a complete SeizedInteractionListener class trace against the private Symbiote JAR.
- Palladium/PalladiumCore is not a Symbiote dependency and remains outside this pilot; any compatibility work for it belongs in a separate project.
- Added a pure Forge-to-NeoForge metadata transformer. It rewrites the JavaFML loader range, the Forge platform dependency ID, and Forge `mandatory` flags into NeoForge dependency `type` values.
- Added a metadata-only archive transformer that writes a separate derived JAR, replaces `META-INF/mods.toml`, removes stale signature files, preserves other archive entries, and records source/output SHA-256 digests. It never overwrites the source JAR.
- Added unit tests for metadata conversion and archive-copy behavior; the user ran `:loader:clean :loader:test --no-daemon` successfully after correcting Java string escaping in the archive test assertions.
- Ran `:loader:transformForge1201Metadata` against the private Symbiote 1.1.3 JAR successfully. The derived archive copied 515 entries, removed zero signature entries, and produced distinct input/output SHA-256 digests. Its generated `META-INF/neoforge.mods.toml` was manually inspected and confirmed the expected JavaFML `[1,)`, NeoForge `[21.1.258,)`, and Minecraft `[1.21.1,1.22)` ranges with required dependency types.
- The user ran `:loader:auditForge1201Mappings` against the original Symbiote 1.1.3 JAR. The initial catalog classified 120 Forge API classes as 103 package-relocation candidates, 17 manual-adapter types, and zero unclassified Forge API types; it separately found 310 Minecraft API classes. These counts describe static references, not verified runtime compatibility.
- Review of that audit exposed over-broad classifications for nested `ForgeConfigSpec` value types, the config-screen extension point, the old tick-event family, `Event.Result`, `ForgeMod`, `DeferredRegister`, and parts of the old networking API. Those have now been explicitly moved to the manual-adapter category.
- Expanded the mapping catalog to 44 explicit manual-adapter types and added focused tests for nested configuration types, tick-event Pre/Post migration, config-screen registration, Forge attributes, event results, deferred registries, and the six target types absent at the mechanically relocated NeoForge paths. The user ran `:loader:clean :loader:test --no-daemon` successfully after this refinement. The final audit reported 76 package-relocation candidates, 44 manual-adapter types, and zero unclassified Forge API types (120 Forge API classes total, plus 310 Minecraft API classes).
- Added a read-only Forge 47.x to NeoForge 21.1.x mapping catalog and `:loader:auditForge1201Mappings` task. It distinguishes reviewed namespace-relocation candidates from known semantic adapters and unreviewed Forge APIs; it does not rewrite the JAR or feed the discovery pipeline.
- Added `Forge1201TargetApiVerifier` and the root task `:verifyForge1201TargetApi`. It uses the root NeoForge ModDev compile classpath to verify candidate target class files and referenced member descriptors without defining or initializing classes. Synthetic tests cover mapped descriptors, inherited-member lookup, missing members/classes, incomplete hierarchies, exclusion of manual-adapter owners, and descriptors containing manual-adapter types.
- Fixed an ASM `Remapper.mapType()` name-resolution recursion in the verifier's descriptor remapper and corrected the Gradle task's multiline classpath expression. The user then ran `:loader:clean :loader:test --no-daemon` successfully and ran `:verifyForge1201TargetApi` successfully against the original Symbiote 1.1.3 JAR with the NeoForge 21.1.258 target classpath.
- The first target-classpath report (before the six missing target types were reclassified) covered 82 package-relocation candidates: 76 target classes were found and 6 were absent. That initial report is superseded by the final report below.
- Inspection of the six absent target names identified concrete migration work: `LivingAttackEvent` must be adapted to the NeoForge incoming-damage event model; Forge's nested `MobSpawnEvent.FinalizeSpawn` must be adapted to NeoForge's top-level `FinalizeSpawnEvent`; and the mapped nested `Mod.EventBusSubscriber` annotation/Bus types, `NetworkHooks`, and `IForgeRegistry` are absent at their mechanically relocated paths. The catalog classifies these six as manual migrations, with a regression test for those decisions.
- Final verification after the catalog refinement: the user ran `:loader:clean :loader:test --no-daemon` successfully, then `:loader:auditForge1201Mappings` successfully, and finally `:verifyForge1201TargetApi` successfully against the original Symbiote 1.1.3 JAR with NeoForge 21.1.258. The final audit reported 120 Forge API classes, 310 Minecraft API classes, 76 package-relocation candidates, 44 manual-adapter types, and zero unmapped Forge API types.
- The final target verifier found all 76 of 76 relocation-candidate classes. It checked 108 unique candidate method references: 97 matched exact target or inherited descriptors, 10 were reported missing/changed, and one was excluded from exact descriptor matching because its descriptor contains the manual-adapter `Event.Result` type. All 14 candidate field references matched. This successful static verification does not demonstrate class loading or gameplay compatibility.
- The 10 missing/changed method references are: `EntityRenderersEvent.AddLayers#getSkin(String)`; `RenderGuiEvent.Pre#getWindow()`; `RenderLevelStageEvent#getPartialTick()`; `VanillaGameEvent#getVanillaEvent()`; `EntityAttributeModificationEvent#add(EntityType, Attribute, double)`; `EntityAttributeModificationEvent#has(EntityType, Attribute)`; `EntityEvent.Size#getNewEyeHeight()`; `EntityEvent.Size#setNewEyeHeight(float)`; `PlayerInteractEvent#isCancelable()`; and `PlayerInteractEvent#setCanceled(boolean)`. Their exact target replacements and behavioral intent must be reviewed before any call-site rewrite.
- The method report is consistent with known 1.21.1 API shifts that require source-level attention: player renderer lookup uses `PlayerSkin.Model`; render event timing/context types changed; the vanilla game event is represented by `Holder<GameEvent>`; entity-attribute modification APIs use `Holder<Attribute>`; and entity-size events no longer expose the old eye-height accessors. The two base `PlayerInteractEvent` cancellation methods require special care because cancelability is expressed through concrete cancellable subevents rather than assumed uniformly on the base type.
- The static per-class inventory traces those 11 unresolved references to these Symbiote classes: `LivingArmorClientHook` (skin renderer lookup); `SculkEchoBlipRenderer` (GUI window and render partial tick); `BellCounterplayHandler` (vanilla game event holder); `WildHostAttributeFix` (attribute holder arguments); `LivingArmorSizing` (entity dimensions / eye height); and `SeizedInteractionListener` (interaction cancelability, cancellation and result). This proves class ownership but not the exact containing method or the behavior of its branch.
- Extended `Forge1201ApiUsageAnalyzer` to print direct Forge/optional-API invocation call sites, including caller class, caller method and descriptor, target owner/name/descriptor, opcode, and reference count. Added synthetic regression coverage. The user ran the analyzer tests and the private Symbiote inspection successfully.
- The call-site scanner records direct `INVOKE*` instructions. It does not by itself identify branch intent, data values, reflective calls, or method handles; inspect those separately before writing behavior-sensitive adapters.
- The user successfully ran the enhanced `:loader:inspectForge1201` task after the analyzer test fix. Exact callers for the unresolved call sites are: `LivingArmorClientHook#onAddLayers`, `SculkEchoBlipRenderer#onRenderGui` (two `getWindow()` calls), `SculkEchoBlipRenderer#onRenderLevelStage`, `BellCounterplayHandler#onVanillaGameEvent`, `WildHostAttributeFix#onAttributeModification` (both `add` and `has`), `LivingArmorSizing#applyLargeDimensions` (both eye-height calls), and `SeizedInteractionListener#deny` (cancelability, cancellation, and result).
- The inspector also shows partial-tick calls in `HandMorphRenderer#onRenderHand`, `WildHostRenderLayer#onRenderLivingPost`, and `BloomHeadHandler#onRenderPre`; these references were not among the target verifier's unresolved signatures. Only `RenderLevelStageEvent#getPartialTick()` remains unresolved in this audit.
- The direct caller methods are now confirmed: `LivingArmorClientHook#onAddLayers`; `SculkEchoBlipRenderer#onRenderGui` (two old window calls); `SculkEchoBlipRenderer#onRenderLevelStage`; `BellCounterplayHandler#onVanillaGameEvent`; `WildHostAttributeFix#onAttributeModification`; `LivingArmorSizing#applyLargeDimensions`; and `SeizedInteractionListener#deny`. The private testbench contains a separate detailed call-site audit.
- The inspector task accepts `-Pforge1201TraceMethods=<comma-separated-class#method-list>` and `-Pforge1201TraceMethods=<fully.qualified.Class#*>` to emit read-only ASM instructions for selected methods or every method in one class. The user successfully ran the clean loader test suite and uploaded both selected-method and class-wide traces. The trace is static evidence only; it does not execute the mod.
- The user uploaded and the project reviewed `symbiote-selected-bytecode.txt`. The method trace confirms `LivingArmorClientHook#onAddLayers` iterates skin models and adds three render layers to each `PlayerRenderer`; `SculkEchoBlipRenderer` captures its view/projection/camera state after particles and later uses GUI-scaled dimensions plus game-time partial ticks for projected blips; `BellCounterplayHandler` specifically matches `GameEvent.BLOCK_CHANGE` and verifies a `BellBlock` at the event position; `WildHostAttributeFix` adds `Attributes.ATTACK_DAMAGE` at 1.0 for every event entity type missing it; and `LivingArmorSizing#applyLargeDimensions` scales width, height, and eye height by 1.35, except fixed, sleeping, and dying cases.
- The NeoForge adaptation directions are now grounded in the actual bytecode: player skin keys become `PlayerSkin.Model`; GUI size comes from `GuiGraphics.guiWidth()/guiHeight()`; render-stage interpolation comes from `DeltaTracker.getGameTimeDeltaPartialTick(...)`; the vanilla event holder should be checked against `GameEvent.BLOCK_CHANGE`; attribute operations use `Attributes.ATTACK_DAMAGE` as a `Holder<Attribute>`; and the scaled dimensions should carry the clamped eye height via `EntityDimensions.withEyeHeight(...)`.
- The uploaded `symbiote-seized-interaction-trace.txt` class-wide trace confirms that `onRightClickItem`, `onRightClickBlock`, `onEntityInteract`, and `onEntityInteractSpecific` call `deny` with reasons `use_item`, `use_block`, and `use_entity` (the two entity handlers share the same reason). It also confirms a separate `onUseItemStart` handler that cancels `LivingEntityUseItemEvent.Start` and logs `consume_item`.
- The user successfully ran `:loader:clean :loader:test --no-daemon` with class-wildcard trace support enabled, generated `com.scout.symbiote.event.SeizedInteractionListener#*`, and uploaded the resulting `symbiote-seized-interaction-trace.txt`. The class-wide output has been reviewed; no repeat trace run is needed.
- The private call-site audit now documents the class-wide event handlers and target mapping. In NeoForge 1.21.1 the four concrete `PlayerInteractEvent` subtypes implement `ICancellableEvent`; the abstract base does not. Preserve the `ServerPlayer` and `BodySeized` guards, the reason strings, and `setCanceled(true)`. The `RightClickBlock` override automatically sets both block/item controls to `TriState.FALSE`; the other interaction events retain default `InteractionResult.PASS` cancellation results. Do not map the old generic `Event.Result.DENY` write to a global `TriState.FALSE` or `InteractionResult.FAIL`.
- The first dedicated Forge 1.20.1 ASM transformation stage was run successfully by the user, and its preliminary output passed the new read-only translated-JAR verifier. The separate scoped call-site adapter engine and its synthetic tests are now committed, but those new tests and the profile-driven Symbiote transformation have not yet been run by the user. The transformer mechanically relocates reviewed Forge package references and can optionally apply an external, fail-fast adapter profile before relocation. Manual-adapter and unreviewed Forge API types remain separately reported. Any generated artifact remains intermediate and is not a load-ready mod.

The existing 1.7.10/1.12.2 NeoForge 1.21.1 runtime remains the baseline. The 1.20.1 API bridge, bytecode adaptation and gameplay validation remain future milestones.


## Preliminary Forge 1.20.1 ASM transformation stage

A separate modern path has now been added without changing the pre-1.20 `LegacyJarTransformer`:

- `Forge1201BytecodeTransformer` reads an already metadata-translated JAR and applies `Forge1201MappingCatalog` package moves through ASM `ClassRemapper`.
- It remaps class headers, superclasses/interfaces, descriptors, signatures, annotations, method/field/type instructions, handles, class literals, and stack-map frame types through ASM's remapper visitor.
- Class entry paths are moved to match rewritten internal class names. Multi-release class entry prefixes are retained.
- `META-INF/services` filenames and provider class names are updated. Stale top-level JAR signature files are omitted.
- Duplicate source entries, relocated path collisions, malformed classes, and attempts to overwrite the source JAR fail safely before publishing the destination.
- Types marked `MANUAL_ADAPTER_REQUIRED` or `UNMAPPED_FORGE_API` are intentionally left unchanged and listed in the transformation report. This is not a global name rewrite and does not rename Minecraft methods or fields.
- `Forge1201JarTransformer` orchestrates metadata conversion followed by the ASM pass. The new `:loader:transformForge1201` task exposes the pipeline.
- Regression tests cover metadata conversion, annotation/method/field/class-literal descriptors, a moved class entry, service file remapping, manual/unmapped type reporting, signature removal, input preservation, and collision rejection.

**Validation status — user-confirmed local run on 2026-11-03:**
- `.gradlew.bat :loader:clean :loader:test --no-daemon` completed with **BUILD SUCCESSFUL**.
- `.gradlew.bat :loader:transformForge1201` completed with **BUILD SUCCESSFUL** and created the preliminary private output JAR.
- Input SHA-256: `e40f48a8a9bf1f39ea173a7c163285df199b7d2cf1dfcbf3efa721fe7f2f942c`.
- Output SHA-256: `85ed9c007e5355f8a36834ba920a9744bba3feaba393ed1448bf1b155983a4db`.
- 278 classes scanned; 58 class-file byte arrays differ after ASM serialization; 0 class-entry paths relocated; 182 non-class resources copied; 0 signature entries removed.
- 77 reviewed API types were package-relocated; 44 semantic-adapter types were deliberately left unchanged; 0 unreviewed Forge API types were found.
- The zero class-entry path relocations are expected for this mod: its own declared classes live under `com/scout/symbiote`; the 77 relocations are referenced Forge API types/descriptors inside the mod's bytecode, not 77 Symbiote-owned class definitions.
- Zero signatures removed means the source contained no matching top-level signature files; it is not a transformation failure.
- The compiler emitted only the existing deprecation note ("Some input files use or override a deprecated API"); the test task still completed successfully.

The preliminary output is not load-ready. Semantic Forge adapters, Minecraft 1.20.1-to-1.21.1 API changes, bootstrap/registration, networking, and runtime/gameplay validation remain outstanding.

Run from the project root after IntelliJ Ctrl+T:

```powershell
.\gradlew.bat :loader:clean :loader:test --no-daemon

.\gradlew.bat :loader:transformForge1201 `
  -Pforge1201Jar="C:\Users\Administrator\legacy-connector-testbench\symbiote-1.1.3.jar" `
  -Pforge1201Output="C:\Users\Administrator\legacy-connector-testbench\symbiote-1.1.3-neoforge-1.21.1-preliminary.jar"
```

Keep the resulting derivative JAR private with the original input. Do not place it in the active mods folder.


## First user-confirmed preliminary transformation result

The user ran both required commands successfully on 2026-11-03. The generated file was written locally to `C:\Users\Administrator\legacy-connector-testbench\symbiote-1.1.3-neoforge-1.21.1-preliminary.jar` and has not yet been uploaded to the private testbench for direct post-transform inspection.

| Metric | Result |
|---|---:|
| Input SHA-256 | `e40f48a8a9bf1f39ea173a7c163285df199b7d2cf1dfcbf3efa721fe7f2f942c` |
| Output SHA-256 | `85ed9c007e5355f8a36834ba920a9744bba3feaba393ed1448bf1b155983a4db` |
| Classes scanned | 278 |
| Classes whose bytecode changed | 58 |
| Class entry paths relocated | 0 |
| Non-class resources copied | 182 |
| Signature entries removed | 0 |
| Reviewed API types relocated | 77 |
| Manual-adapter types left unchanged | 44 |
| Unreviewed Forge API types | 0 |

The zero class-entry path relocations are expected: the mod's own class definitions are in `com/scout/symbiote`; package relocation occurred inside the mod's API references and descriptors. Zero signature removals simply means the source contained no matching top-level signature entries. The compiler printed a deprecation note, but the loader test task completed successfully.

**Next validation step:** upload the output JAR to the private testbench (do not put it in the active mods folder). Then add a read-only post-transform verifier that scans the resulting class files and checks relocated NeoForge class/member references against the actual 21.1.258 target compile classpath. Keep manual adapters and remaining Minecraft 1.20.1 member changes as separate reports. The source and output digests must be retained so the exact tested artifact is unambiguous.


## Read-only verification of the translated JAR

A second inspection path has been added for the output of `:loader:transformForge1201`:

- `Forge1201ApiUsageAnalyzer.analyzeTranslated(Path)` accepts the translated artifact's `META-INF/neoforge.mods.toml` and inventories the relocated `net/neoforged` symbols plus any Forge API types that remain unchanged.
- `Forge1201TargetApiVerifier.verifyTranslated(...)` checks the relocated NeoForge classes and exact method/field descriptors against the target compile classpath. It also marks descriptors that still contain a manual-adapter type.
- `Forge1201TranslatedJarVerifier` prints a post-transform report, including target classes/members found or missing and the remaining Forge API types categorized as manual or unreviewed.
- The root task `:verifyForge1201TranslatedJar` provides the configured NeoForge 21.1.258 target classpath. It does not load or execute the transformed mod.
- New synthetic tests cover translated metadata recognition, NeoForge annotation/member inventory, target namespace recognition, and target member verification using already-relocated owners.

**Validation status (2026-11-03):** The user has now run both the clean loader test suite and the translated-JAR verifier successfully. The verifier completed with 278 classes scanned, 76/76 relocated NeoForge classes found, 97/108 relocated method references matched, 14/14 relocated field references matched, 44 remaining known semantic-adapter types, and zero unreviewed Forge API types. Eleven method references remain changed/unresolved and require scoped adaptations. This is static verification only.

## Scoped adapter engine — implementation committed, local validation pending

The public loader now has a generic, opt-in adapter-profile engine. It supports exact field-descriptor, method-descriptor, CHECKCAST-type, and declarative instruction-sequence operations. Each rule selects one original class/method signature and declares exact match counts. Missing methods, stale instruction patterns, overlapping sequences, and unexpected counts abort the transform before the output JAR is finalized. With no profile supplied, the original package-relocation path is unchanged.

The public test suite now includes synthetic ASM tests for descriptor and CHECKCAST changes, instruction-sequence rewriting, archive integration, and fail-closed behavior. These new tests have **not yet been run by the user**. The assistant could not execute Gradle in this environment because repository network access was unavailable.

Profiles that contain mod-specific selectors or derived bytecode patterns must stay outside this public repository. To test a local profile, pass it with `-Pforge1201AdapterProfile=<path>` when running `:loader:transformForge1201`. The loader reports the selected profile ID and the number of adapter rules/operations applied.

After IntelliJ Ctrl+T, run the clean tests first:

```powershell
.\gradlew.bat :loader:clean :loader:test --no-daemon
```

Only if that succeeds, create a **new** adapted derivative from the unchanged original input (do not overwrite the existing preliminary artifact):

```powershell
.\gradlew.bat :loader:transformForge1201 `
  -Pforge1201Jar="C:\Users\Administrator\legacy-connector-testbench\symbiote-1.1.3.jar" `
  -Pforge1201Output="C:\Users\Administrator\legacy-connector-testbench\symbiote-1.1.3-neoforge-1.21.1-adapted-preliminary.jar" `
  -Pforge1201AdapterProfile="C:\Users\Administrator\legacy-connector-testbench\profiles\symbiote-1.1.3-neoforge-21.1.258-adapters.json"
```

Then run the translated-JAR verifier against that new output:

```powershell
.\gradlew.bat :verifyForge1201TranslatedJar `
  -Pforge1201TranslatedJar="C:\Users\Administrator\legacy-connector-testbench\symbiote-1.1.3-neoforge-1.21.1-adapted-preliminary.jar"
```

The output must remain private and must not be placed in the active mods folder. Even if the new verifier count improves, the JAR is still preliminary: it does not establish successful mod bootstrap, linkage of every Minecraft member, or gameplay behavior.

