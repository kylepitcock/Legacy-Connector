# Estimate: legacy 1.20.1 Forge mods (and Sinytra Connector Fabric mods) on NeoForge 1.21.1

Research note. **Roadmap (2026-11-04): 1.20.1 comes before 1.16.5**, right after 1.12.2; groundwork so far is the Forge to NeoForge rename tables under `loader/src/main/resources/legacyconnector/forward/1.20.1/`. Back to [Home](Home.md). Written 2026-11-01 from the repository state and general knowledge of the
Forge 47 / NeoForge 21.1 APIs. Figures about third-party APIs were not re-verified against the 1.20.1 sources; check them when the work starts.
**There are no 1.20.1 jars in `test-mods*`** (they hold 1.7.10 and 1.12.2 jars, plus a Curios 1.21.1 jar in `test-mods/modern-deps`), so "which APIs
real mods use most" below is an expectation, not a measurement. Milestone 0 is to collect a corpus and measure it with the tool described in the plan.

## 1. How 1.7.10 and 1.12.2 are structured today

| Layer | Where | Size | Role |
|---|---|---|---|
| Loader (FML locator service) | `loader/src/main/java/.../loader` | 69 files; `transform/` about 3,400 lines | Detects the legacy version (`LegacyVersion` enum), translates the jar into `.legacyconnector/cache` |
| Remap | `LegacyRemapper`, `mapping/McpMappings` | 270 + 90 lines | Relocates `net/minecraft`, `net/minecraftforge`, `cpw/mods/fml`, LWJGL 2 into `legacyconnector/v1_x/...`; SRG ids to MCP names |
| Link | `ShimLinker`, `shim/ShimIndex`, `synth/*` | about 390 + 2,800 lines | Resolves every reference against the hand-written shim, else generates a stub class from `hierarchy.txt` |
| Other transforms | `OptionalStripper`, `SideOnlyStripper`, `JavaCompatRedirects`, `AbsentDependencyStubber`, coremod runner, `CrossVersionPackages` | rest of `transform/` | Reproduce legacy loader behaviour, Java 8 to 21 fixes |
| Resources | `resource/LegacyResourceConverter` | 395 lines | Lang, textures, models to 1.21 formats |
| Shim | `src/main/java/legacyconnector/v1_7_10` (1,273 files), `v1_12_2` (1,953 files) | about 3,200 files | A re-implementation of the whole legacy API (Block, Item, World, IBlockState, ...) as views over modern objects |
| Bridges and platform | `org/kylepitcock/legacyconnector/v1_7_10/bridge` (181 files), `v1_12_2` (142 files), `core/LegacyPlatform`, `Platform1122` | about 320 files | Run the legacy lifecycle, turn legacy registrations into modern registries, data packs, capabilities, menus, world gen |

The central design fact: **the legacy world is a different class model** (numeric ids and metadata, `World`/`IBlockState`, `IIcon`,
`cpw.mods.fml`), so the shim must re-implement it, and legacy packages are relocated so two `net.minecraft.client` never share a module. That
costs thousands of hand-written classes, which is why 1.12.2 is still "server side only" after months of work.

## 2. 1.20.1 is a different kind of problem

Forge 1.20.1 mods use the *same class model* as 1.21.1: `Block`, `Item`, `BlockEntity`, `ResourceLocation`, `PoseStack`, registries,
`DeferredRegister`, JSON blockstates and models, data-driven recipes/loot/tags, mixins. The difference is a long tail of **signature,
type and format changes** (MC 1.20.2, 1.20.3, 1.20.5/1.20.6 and 1.21 each broke a lot) plus the Forge to NeoForge split. Consequences:

- A hand-written shim of the 1.20.1 API is wasteful: most of it would copy the 1.21.1 classes. No relocation of Minecraft packages is
  needed or wanted (mod code should call the real classes).
- The work is a **bytecode migration tool** (what Sinytra and the "mod porting" tools do offline, here done at load time) plus a **thin
  adapter library** for the pieces whose semantics, not just names, changed. The synth stub generator is still useful for the residue.
- Overridden methods are the hard part: a mod that overrides `BlockEntity.load(CompoundTag)` is not fixed by renaming a call site; the
  translator must add a bridge method (`loadAdditional(CompoundTag, HolderLookup.Provider)` calling `load`), as javac bridge methods do. That
  needs the hierarchy of both versions (the repo already has hierarchy data tooling in `synth/LegacyHierarchyData` and `transform/Hierarchy`).

### 2.1 Names

- 1.20.1 Forge jars are **SRG-named at runtime** (`m_46859_`, `f_46441_`, class names are Mojang names). NeoForge 1.21.1 runs with
  **Mojang names**. Step 1 is SRG to Mojang for 1.20.1: tables come from the 1.20.1 MCPConfig/Forge `joined.tsrg` plus Mojang's mapping file. This is
  the same shape as `McpMappings` (SRG id to name table, id unique, no owner context), so about a day to adapt.
- Step 2 is the 1.20.1 Mojang name to the 1.21.1 Mojang name for everything Mojang moved or renamed (e.g. `net.minecraft.world.Container`
  users to `RecipeInput`, `ItemStack.getTag` to components, `ResourceLocation` constructors, `RenderSystem`/`BufferBuilder` calls). There is no
  ready table. Build one by diffing the two official mapping files (matching through the obfuscated names is unreliable across versions;
  match by structure and fill the rest by hand) and keep it as a data file like `hierarchy.txt`. Expect an automatically derived 60 to 70% (pure moves and renames) and a manual list for the rest.
- Forge to NeoForge: `net.minecraftforge.*` to `net.neoforged.neoforge.*`, `net.minecraftforge.fml.*` to `net.neoforged.fml.*`, `@Mod`
  constructor injection (`IEventBus`, `ModContainer`), `mods.toml` to `neoforge.mods.toml`. Mostly package maps; the semantic part is below.

### 2.2 Main API gaps (Forge 47 / MC 1.20.1 versus NeoForge 21.1 / MC 1.21.1)

| Area | Change | Mechanical? | Adapter needed |
|---|---|---|---|
| Event bus | `MinecraftForge.EVENT_BUS` to `NeoForge.EVENT_BUS`; `@Mod.EventBusSubscriber` kept; `FMLJavaModLoadingContext.get().getModEventBus()` removed (inject into the constructor); many event classes renamed/moved or changed fields (`TickEvent` to `*Tick.Pre/Post`, `RegisterEvent`, `LivingHurtEvent` to `LivingDamageEvent.*`) | Partly | Event translation layer for renamed events (about 40 commonly used) |
| Registries | `DeferredRegister`/`RegistryObject` to `DeferredRegister`/`DeferredHolder`; `ForgeRegistries` to `BuiltInRegistries`; registry objects keyed by `ResourceLocation.fromNamespaceAndPath` | Mostly | `RegistryObject` as a subclass facade of `DeferredHolder` |
| `ResourceLocation` | public constructors gone (`new ResourceLocation(ns, path)` to `fromNamespaceAndPath` / `parse`) | Yes (invoke rewrite of `<init>`) | None |
| Item NBT | `getTag`/`getOrCreateTag`/`setTag`/`hasTag` and `ItemStack` NBT replaced by **data components** (1.20.5) | No | `ItemStackNbt` helper that maps NBT to `CUSTOM_DATA`, and known keys (Damage, display, Enchantments, AttributeModifiers, BlockEntityTag) to their components, both directions. Large, central, lossy at the edges |
| Item construction | `Item.Properties` (food, tool tiers, armor materials, attribute modifiers, `hurtAndBreak(int, LivingEntity, Consumer)`) reworked; `Tier`/`ArmorMaterial` registry based | No | Facades for `Tiers`, `ArmorMaterial` holders, `FoodProperties` builder |
| Block entities | `load`/`saveAdditional` gain `HolderLookup.Provider`; `getUpdateTag`, `getUpdatePacket` same; `onDataPacket` | Via bridge methods | Bridge-method generator (above) |
| Interactions | `Block.use` split into `useItemOn`/`useWithoutItem` (1.20.5); `InteractionResult`, `ItemInteractionResult` | Via bridge methods | Bridge dispatch |
| Capabilities | `LazyOptional`, `ICapabilityProvider`, `getCapability(Capability, Direction)` to `BlockCapability`/`ItemCapability` registered with `RegisterCapabilitiesEvent`; item capabilities attach to item types, not stacks | No | Adapter: `LazyOptional` facade plus a registration shim that reads the mod's `getCapability` overrides. The repo already exposes legacy containers as capabilities (`CapabilityRules`, `BuiltinAdapters`), the same machinery in the other direction |
| Networking | `SimpleChannel`/`NetworkRegistry`/`FriendlyByteBuf` messages to `CustomPacketPayload` + `StreamCodec` + `RegisterPayloadHandlersEvent` | No, but contained | `SimpleChannel` facade that registers one generic payload per message class and keeps the mod's encode/decode lambdas. about a day of work at this project's pace for such a facade |
| Recipes | `Container` to `RecipeInput`; `Recipe`/`RecipeSerializer` use `MapCodec` + `StreamCodec`; `assemble(Container, RegistryAccess)` to `assemble(RecipeInput, Provider)`; custom serializers need codecs | Partly | Bridge methods plus a codec derived from the mod's `fromJson`/`toNetwork`. JSON: result `item` to `id`, `count` stays, ingredient `{"item":..}` unchanged, `nbt` to `components`; **folders become singular** (`recipes` to `recipe`, `loot_tables` to `loot_table`, `advancements` to `advancement`, `structures` to `structure`, `tags/blocks` to `tags/block`) |
| Loot | `LootContext`/`LootParams` renamed; functions `set_nbt`, `enchant_with_levels` and enchantments are **data-driven** since 1.21 (no `Enchantments.*` constants, enchantments are `Holder` from registry); Global Loot Modifier codec changes | Partly | Enchantment lookup helper; loot JSON rewriter |
| Damage, enchantments, worldgen | Damage types data-driven since 1.19.4 (already in 1.20.1); enchantments new in 1.21; noise settings, density functions, structure sets changed formats | Data rewrite | JSON converters per type |
| Entities | `defineSynchedData()` takes a `SynchedEntityData.Builder`; attribute/`MobEffect` holders; `Entity.hurt`/`DamageSource` unchanged mostly | Via bridge methods | Small |
| Rendering | `VertexConsumer.vertex(..).color(..).endVertex()` to `addVertex/setColor`; `BufferBuilder`/`Tesselator` begin/end (1.21); `RenderType`, `RenderStateShard` changes; `BakedModel` quads and `ModelData` APIs; Forge client extensions to `IClientItemExtensions`; item model overrides; `GuiGraphics` mostly stable | Partly | Vertex-call rewrite plus `Tesselator` facade; the existing `Rendering` work (tile renderers, GL emulation) does not apply |
| Menus / screens | `MenuType`, `AbstractContainerMenu` stable; `NetworkHooks.openScreen` to `Player.openMenu(provider, buf writer)` | Yes | Small |
| Config | `ForgeConfigSpec` to `ModConfigSpec`; `ModConfig.Type` | Package map | None |
| Mixins and ATs | Mixins carry refmaps in SRG and target 1.20.1 methods; ATs use SRG names | No | Retarget refmaps SRG to Mojang (1); the mixin *targets* often changed, so many fail (see risks) |
| Mod metadata | `META-INF/mods.toml` to `META-INF/neoforge.mods.toml`, dependency `forge` to `neoforge`, loader/version ranges | Yes | `ModMetadataGenerator` already generates metadata |
| Libraries | Curios, GeckoLib, Patchouli, JEI, Architectury, Cloth Config, Create, Mekanism ... each changed API too (Curios 1.21 uses attribute slots/data components; GeckoLib 4 package stable; Architectury major version bumps; Create 0.5 to 6 is a rewrite) | No | One adapter per popular library, same as Baubles to Curios today |

### 2.3 How Sinytra Connector does it, and what that implies

Connector runs **Fabric mods of the same Minecraft version** on Forge/NeoForge (for 1.21.1 it runs Fabric 1.21.1 mods on NeoForge 1.21.1). It does
not cross Minecraft versions. Its pieces: remap Fabric intermediary names to the runtime mapping, a "Forgified Fabric API" implementing the Fabric API
on top of the loader, a transformer that adapts Fabric mod classes, and a mixin remapping layer. All of it relies on both sides having identical
Minecraft classes. So:

- "Sinytra compatibility" in this project has two readings. (a) Make the 1.20.1 layer coexist with Connector 1.21.1 in a pack: legacy
  Fabric 1.21.1 mods are Connector's job, nothing to do except not break its class loading. (b) Run **Fabric 1.20.1** mods on NeoForge
  1.21.1, which no tool does: intermediary 1.20.1 to Mojang 1.20.1 (Fabric ships its mapping, simple), then the same 1.20.1 to 1.21.1 migration as for Forge
  mods, then hand over to Connector as if the jar were a Fabric 1.21.1 mod (its Fabric API 1.21.1 differs from the 1.20.1 one: `FabricLoader`,
  events, `FabricItemSettings` to `Item.Properties`, networking API v1 to payloads). Reading (b) multiplies the effort of the Forge side by about 1.5 and
  depends on Connector's stability; do it last, if at all. Fabric mods also lean heavily on mixins, the most fragile part.

## 3. Approach options

| Option | Description | Effort | Verdict |
|---|---|---|---|
| A. Hand-written shim like 1.7.10/1.12.2 | Copy 1.20.1 API under a relocated package and re-implement it over 1.21.1 | the largest option (roughly the 1.12.2 effort again: 169k lines), thousands of classes | Reject: duplicates classes that exist in both versions |
| B. **Bytecode migration + thin adapters** | Remap SRG to Mojang 1.20.1, apply a 1.20.1 to 1.21.1 member/class migration table, generate bridge methods for changed overrides, redirect removed APIs to static helper classes, convert resources, translate Forge to NeoForge | about 25 to 30 agent-days to broad coverage (2 to 4 weeks wall-clock); first mods load within 3 to 5 days | Recommended |
| C. Synth only | Like B but resolve every miss with generated defaults | Cheap, loads mods, content silently broken | Use as the residue fallback only |
| D. Wait for ports | Most popular 1.20.1 mods already have 1.21.1 NeoForge ports | 0 | Real alternative: value is limited to abandoned mods; the project rule that mods with 1.21.1 versions get low priority applies strongly here |

Honest value check: the 1.7.10 and 1.12.2 targets exist because those mods have no 1.21 equivalents. For 1.20.1 the abandoned-mod set is much smaller
(1.20.1 is a long-lived version, many modpacks, plenty of mods stopped there: Tinkers' Construct 3 (see Compatibility-and-Status), some Twilight-era
content mods, many Forge-only packs). Pick the corpus before committing.

## 4. Reusable parts

| Part | Reuse |
|---|---|
| Locator, per-jar cache, provenance watermark, translation pool, progress | Directly (add `V1_20_1` to `LegacyVersion`; its `shimPrefix` becomes empty/identity, so that enum needs a mode flag) |
| `LegacyRemapper` (ASM `Remapper` with class/member tables) | Core of steps 1 and 2; table loading from `McpMappings` generalised |
| `CheckJar` / `checkLegacyMod` offline link check | Very valuable: link-check a 1.20.1 jar against the real NeoForge 1.21.1 jar to count missing symbols, i.e. an automatic gap report for the corpus |
| `synth/*`, `AbsentDependencyStubber`, `OptionalStripper`, `SideOnlyStripper` (`@OnlyIn`), `JavaCompatRedirects` (not needed, Java 17 bytecode runs on 21) | Stub residue, `@Optional`-style mod checks, side stripping |
| `ModMetadataGenerator`, `DirectoryMods`, `SplitPackageResolver`, `CrossVersionPackages`, `LegacyMixinPlugin` | Metadata, loading, library clashes |
| `LegacyResourceConverter` | Structure reusable, rules differ (mostly JSON rewrites, not texture/model rewrites) |
| Capability, recipe-to-datapack, loot and biome-modifier bridges | Concepts and `CapabilityRules`/`BuiltinAdapters` shared; the 1.20.1 side is mostly pass-through since the target API is native |
| Everything in `legacyconnector/v1_7_10`, `v1_12_2` and their bridges | Not reusable (different class model) |

New: a version-diff table builder (mapping diff), bridge-method generator, `ItemStack` NBT to components layer, `SimpleChannel` facade, event
renames, JSON format rewriter, per-library adapters, mixin/AT retargeting.

## 5. Effort per component (agent-days of wall-clock work, several agents in parallel)

Rewritten 2026-11-06. The first version of this table was in engineer-weeks (about 60 weeks in total), a conventional estimate that ignored how this
project has actually moved: the first commit is 2026-10-10 and by 2026-11-06 the repository holds the 1.7.10 shim (112k lines), the 1.12.2 shim
(169k lines) and the 1.20.1 groundwork. The numbers below keep the old table's relative sizes, converted at that measured pace (roughly one agent-day of
parallel work per old engineer-week for the mechanical parts, less where tooling already exists).

| Component | Days |
|---|---|
| Version plumbing, corpus collection, gap-report tool (link check against 1.21.1) | 0.5 to 1 |
| SRG to Mojang 1.20.1 table and remapper mode (no relocation) | 0.5 |
| 1.20.1 to 1.21.1 mapping/migration table (diff tooling + manual fix list) | 2 to 3 |
| Forge to NeoForge package map, `@Mod` constructor/lifecycle, config, metadata | 1 |
| Bridge-method generator for changed overrides (block entity, interaction, recipe, entity data) | 1.5 |
| Registry/DeferredRegister/RegistryObject facade, `ResourceLocation`, `ForgeRegistries` | 1 |
| `ItemStack` NBT to data components layer, item properties/tiers/armor/food | 2 to 3 |
| Capabilities facade (`LazyOptional`, attach/provide), item capabilities | 1 to 1.5 |
| Networking facade (`SimpleChannel`) | 1 |
| Events (renames, moved fields, 40+ events) | 1 to 1.5 |
| Recipes and recipe serializers, JSON converters (recipes, loot, advancements, tags, folder renames) | 1.5 to 2 |
| Loot, enchantments, damage sources | 1 |
| Rendering and client (vertex builder calls, model APIs, screens, client extensions) | 2 to 3 |
| Mixin refmap/AT retargeting, mixin target failures triage | 1.5 |
| Worldgen data formats and features | 1 |
| Library adapters (Curios, GeckoLib, Patchouli, JEI, Cloth Config, Architectury; Create not feasible) | 2 |
| Testing, bug triage, corpus runs, compat site data (ongoing, 25% of the above) | 5 |
| **Total to broad coverage** | **about 25 to 30 agent-days, i.e. 2 to 4 weeks of wall-clock time** |
| Fabric 1.20.1 via Connector (reading b) | +4 to 6 |

Still plus or minus 40%, and the long tail after "broad coverage" is open-ended (it is the issue list, as for every version).

## 6. Biggest risks

1. **Mixins.** Forge mods use mixins into vanilla; their targets (method descriptors, injection points, local variable indexes) changed in 1.20.2 to 1.21.
   Mappings cannot repair a changed target. Expect a large share of mods with vanilla mixins to fail or misbehave; each is a mod-specific problem.
   Mitigation: classify by mixin count in the gap report, support the 80% that use few simple mixins, skip the rest with a message.
2. **Item data components.** NBT-based items (tools with custom tags, backpacks, armor with stored data) have no 1:1 form; lossy mapping makes saves and
   tooltips subtly wrong. It is the highest correctness risk and touches almost every mod.
3. **Library ecosystem.** A 1.20.1 mod depends on 1.20.1 versions of Curios, GeckoLib, Create, Mekanism... which will not load; each needs a bridge
   or a 1.21.1 port that has a different API. The long tail exceeds the effort table.
4. **Value.** Most 1.20.1 mods that matter already have NeoForge 1.21.1 ports; the abandoned set may be too small to justify the roadmap slot. Measure first.
5. **Maintenance.** The migration table is tied to 1.21.1; NeoForge 21.x point updates and the project's own move to later Minecraft versions
   (1.21.4+) change many of the same symbols.
6. **Rendering.** Custom `RenderType`/shader/vertex code (GeckoLib-free mods that draw by hand) breaks in many small ways with no compile-time guide.
7. **Sinytra reading (b).** Depends on a third-party transformer whose internals shift between releases.

## 7. Milestones

| # | Milestone | Done when | Wall-clock |
|---|---|---|---|
| 0 | Corpus and gap report | 30 to 50 real 1.20.1 jars in `test-mods4/`; `checkLegacyMod` reports missing symbols vs 1.21.1 per jar; ranked list of the most-missing APIs, mixin counts per mod; decide go/no-go | 1 day |
| 1 | First mod loads | A simple content mod (blocks, items, one block entity, DeferredRegister, no mixins) is translated, loads on a server, registers its content | 3 to 5 days cumulative |
| 2 | Content registered across the corpus | 60% of the corpus loads without errors; items, blocks, recipes, loot, tags, assets resolve; server self-test prints registration counts like the 1.12.2 one | about 1.5 weeks |
| 3 | Gameplay | Block entities, menus, networking, capabilities and entities work in a survival session; client renders blocks/items/tile entities; the three most-used library adapters in | about 2.5 weeks |
| 4 | Breadth | NBT/component edge cases, worldgen, mixin triage, compat site entries | 3 to 4 weeks |
| 5 | Fabric 1.20.1 through Connector | Optional | +1 week |

## 8. Recommendation

1. Do milestone 0 before committing anything: collect jars and run the link check to turn this estimate into measured numbers. It reuses existing
   tooling and takes about a day.
2. If the abandoned-mod set is meaningful, take **option B** (bytecode migration + thin adapters), never a hand-written shim. Start with the
   data-heavy, API-light mods (content packs, simple tech/magic mods), which pass milestone 2 early.
3. Keep the existing order of the roadmap but treat 1.20.1 as a separate pipeline sharing only the loader plumbing; 1.7.10/1.12.2 work and the
   shim code stay untouched.
4. Postpone Fabric 1.20.1 via Connector until Forge 1.20.1 reaches milestone 3.
5. Mods with a 1.21.1 NeoForge version get the project's `lowpriority` treatment: prefer pointing users at the port.

## 9. Milestone 0 measurements (2026-11-04)

`./gradlew gap1201` (`loader/.../tools/Gap1201`) renames every reference of a jar (SRG ids to Mojang names with a table from `tools/dev/gen_srg_mojang.py`,
Forge classes with `loader/src/main/resources/legacyconnector/forward/1.20.1/*.tsv`) and checks it against the Minecraft 1.21.1 + NeoForge jar, the JDK and the
other jars of the set. First run on 44 Forge 1.20.1 jars from Modrinth (`test-mods4/`, filled with `fetch_probe_mods.py 1.20.1 test-mods4 --content`):

- Plain content and data packs are cheap: Terralith, Dungeons and Taverns and Vein Miner have no (or 3) code references. Small mods miss 0 to 10 members.
- After the renames the misses cluster. Forge API that needs an adapter (jars affected of 44): `FMLJavaModLoadingContext` and mod event bus injection 30,
  `ForgeRegistries`/`IForgeRegistry` 17, `SimpleChannel`/`NetworkEvent`/`NetworkDirection` about 16, `TickEvent` 15, `LazyOptional`/`Capability`/`ForgeCapabilities`
  13, `DistExecutor` and the `NonNull*` function types 11 (trivial to provide), `ConfigScreenFactory`/`ForgeGui`/`IModInfo` 9.
- Minecraft 1.20.1 to 1.21.1 changes: `new ResourceLocation(String)` (32 jars), `ItemStack` NBT (`getTag`, `setTag`, `save`, `of`, `isSameItemSameTags`; 11 to 17),
  the vertex builder (`endVertex`, `uv`, `Tesselator.getBuilder`, `BufferBuilder.begin/end`; 9 to 17), `FriendlyByteBuf.writeNbt/readItem/writeItem`,
  `MobEffectInstance.getEffect`, `BlockEntity.load`, `Component.Serializer`, `Level.gameEvent`; fields: `MobEffects.*`, `Attributes.*`, `Enchantments.*` (now holders/data driven), `Blocks.GRASS`.
- The heavy mods (Create 490 classes, Quark 368, Sophisticated Backpacks 410, Supplementaries 353) are dominated by the other mods they link against and by rendering.
- Design consequence: relocate only the Forge API that no longer exists into a small facade package (`FMLJavaModLoadingContext`, `SimpleChannel`, `LazyOptional`,
  `RegistryObject`, `NonNull*`, `DistExecutor`, ...), pass everything else through to the real classes.
