# Estimate: legacy 1.20.1 Forge mods (and Sinytra Connector Fabric mods) on NeoForge 1.21.1

Research note. **Roadmap (2026-11-04): 1.20.1 comes before 1.16.5**, right after 1.12.2; groundwork so far is the Forge to NeoForge rename tables under `loader/src/main/resources/legacyconnector/forward/1.20.1/`. Back to [Home](Home.md). Latest pack measurement (506 jars of ATM 9, with native-versus-abandoned split and the baseline to compare re-runs with): **[Legacy-1.20.1-ATM9-Run](Legacy-1.20.1-ATM9-Run.md)**. Written 2026-11-01 from the repository state and general knowledge of the
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
| **Total to broad coverage** | **about 25 to 30 agent-days, i.e. roughly 1 to 2 weeks of wall-clock time with about five agents in parallel** |
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

## 7. Milestones (wall-clock, corrected 2026-11-06)

| # | Milestone | Wall-clock |
|---|---|---|
| 0 | Corpus and gap report | about 1 day |
| 1 | First mod loads on a server | 1 to 2 days |
| 2 | Content registered across the corpus (60%) | 3 to 4 days |
| 3 | Gameplay: block entities, menus, networking, capabilities, client rendering, main library adapters | about 1 week |
| 4 | Breadth: NBT/component edge cases, worldgen, mixin triage | 1 to 2 weeks |
| 5 | Fabric 1.20.1 through Sinytra Connector (reuse it at runtime, do not port it: MIT licensed, about 9 to 10k lines; our part is the 1.20.1 to 1.21.1 migration of the jar) | +3 to 5 days |

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

### 9.1 Second run, 2026-11-07: 140 jars, 72 of them abandoned

Full tables, method and the per-jar classification are in **[Legacy-1.20.1-Corpus](Legacy-1.20.1-Corpus.md)**. Summary of what changed against the 44-jar run above:

- The value check of section 3 (option D) is answered: scanning down Modrinth's Forge 1.20.1 list by downloads until 72 abandoned content mods were found (so the 72 is a cap, not a rate), 72 projects have **no** NeoForge 1.21.1 file on Modrinth or
  CurseForge (Alex's Mobs and Caves, Ice and Fire, Botania, Tinkers' Construct, Blue Skies, Species, Enigmatic Legacy, Spartan Weaponry, Goety Revelation, Steam 'n' Rails, Decorative Blocks...).
  Some have unofficial community ports; none counts as an official one. The abandoned set is large, not marginal.
- Tool fixes: the `Dist` annotation lives in its own API jar (it was reported as a missing class in 94 jars), and the mods' own members are now renamed like the references to them
  (library gaps fell from 12,656 to 1,238 fields). Forge API gaps are unchanged by this.
- Abandoned content mods: 8 data-only, 20 cheap (at most 40 Forge/Minecraft symbols, at most 1 mixin class), 21 medium, 23 heavy. Mixin classes: 30 of the 72 have none, 22 have 1 to 5, 20 have 6 or more.
- Top gaps by number of the 72 abandoned jars: `FMLJavaModLoadingContext` 49, `ForgeRegistries`/`IForgeRegistry` 40, `new ResourceLocation(String)` 39, `DeferredRegister.create(IForgeRegistry, String)` 30,
  `ItemStack.hurtAndBreak(int, LivingEntity, Consumer)` 29, `SimpleChannel` 28, `NetworkEvent$Context` 26, `ItemStack.getTag` 25, attribute fields and `AttributeModifier` 22 to 24,
  `SynchedEntityData.define` 24, `Player.broadcastBreakEvent` 24, `MobEffectInstance` 23, `BlockBehaviour$Properties.copy` 22.
- Recommended order (details in the corpus page): mod loading and registries first (about 50 and 40 jars; that alone loads the data-only and several cheap mods), then `ResourceLocation` and the
  attribute/effect holders, networking, `ItemStack` NBT, vertex builder rewrite, capabilities, bridge methods, mixin triage last; heavy mods (Botania, Tinkers', Alex's, Ice and Fire, Blue Skies) are not a target for milestone 2.

## 10. Status 2026-11-08: Minecraft migration

The Minecraft 1.20.1 to 1.21.1 part of the layer (`loader/.../transform/Mc1201Migration`, tables `forward/1.20.1/mc-*.tsv`, helpers
`org/kylepitcock/legacyconnector/v1_20_1/migrate/**`). Runs after the SRG/Forge renames in `Forge1201Transformer`; classes it changes get their frames
recomputed (`Mc1201Hierarchy.frameWriter`, mod classes plus the 1.21.1 class tree `mc-hierarchy.tsv.gz` from `tools/dev/gen_mc_hierarchy.py`).

| Mechanism | Table | What it does |
|---|---|---|
| Call redirect | `mc-calls.tsv` | call (or `NEW/DUP/INVOKESPECIAL` constructor) to a static helper; `=`, `=swap`, `=pop`, `=ret` change a call in place. Not applied when a mod class declares the method on the way up |
| Field | `mc-fields.tsv` | `MobEffects.X`, `Attributes.X`, `GameEvent.X`... are `Holder`s (unwrapped), enchantments are `ResourceKey`s (looked up), loot tables are keys, removed fields come from helpers |
| Rename | `mc-classes.tsv`, `mc-members.tsv` | class and member renames; also lambdas (`FriendlyByteBuf.Writer.accept` to `StreamEncoder.encode`) |
| Bridge method | `mc-bridges.tsv` | overridden method with a changed signature gets the 1.21.1-shaped method calling the mod's: `BlockEntity.load/saveAdditional/getUpdateTag`, `Block.use` to `useItemOn`, `defineSynchedData`, `Recipe.matches/assemble`, `appendHoverText`, `playerWillDestroy`, `Model.renderToBuffer`, `finalizeSpawn`... `super.` calls get the provider/builder from `MigrateContext` |

Helpers: `ItemStackNbt` (NBT over `CUSTOM_DATA` plus Damage, display, Enchantments, BlockEntityTag... components), `HolderCompat`, `VertexCompat` and
`CompatBufferBuilder` (`BufferBuilder` is renamed to it; `Tesselator.getBuilder/end`), `MiscCompat`, `RecipeCompat`, `BridgeHelpers`, `ClientCompat`.
Measured with `./gradlew gap1201 -Pgap.translate=true` (jars go through the real transformer; 248 Forge 1.20.1 jars of `test-mods4`), Minecraft symbols still
unresolved, distinct/jar occurrences: before (groundwork, `ResourceLocation` factories only) classes 612/1668, methods 1792/9210, fields 526/2417; after
classes 610/1610, methods 1393/4700, fields 291/558. `./gradlew verify1201 -PverifyDir=<-Pgap.translated.dir>` links the translated classes with the JVM verifier:
54 of 73,000 linkable classes are invalid, none involve the migration helpers (API visibility changes, `ModelResourceLocation` no longer a `ResourceLocation`).

Open: `ArmorMaterial`/`Tier` implementations, custom `Enchantment` subclasses (data-driven now), `MobType`, `RenderSystem.getModelViewStack`, `LootDataManager`,
recipe serializers (codecs), datagen, super-constructor calls of changed constructors (only `=swap` ones), mixin triage (see section 13).
## 11. Status 2026-11-08: milestone 1 (Forge API facades)

**Milestone 1 reached:** a Forge 1.20.1 content mod loads and registers its content on a NeoForge 1.21.1 server through the layer. Another Furniture 3.0.4 registers
174 blocks, 159 items, 1 entity type and 4 block entity types (`run-subtest/mods/1.20.1/`, `experimental.forge1201`; the server self test logs
`1.20.1 content registered: ...`), and Calcmod and ClearDespawn construct and finish loading beside it (server reaches "Done"). Facades live in `src/main/java/legacyconnector/v1_20_1`
(Forge classes that no longer exist; the translator prefers them to the rename tables), the runtime half in `org.kylepitcock.legacyconnector.v1_20_1.Forge1201Runtime`.

| Area | What exists |
|---|---|
| Mod loading | `FMLJavaModLoadingContext`, `ModLoadingContext.registerConfig` (call rewrite), `DistExecutor`, `NonNull*`, `IExtensionPoint$DisplayTest`/`NetworkConstants` (accepted, ignored), `IEventBus.register` on classes without subscribers (Forge allowed it), `ForgeSpawnEggItem` to `DeferredSpawnEggItem`, `mods.toml` to `neoforge.mods.toml` with the manifest's `MixinConfigs` as `[[mixins]]`, nested jars in `META-INF/jarjar` (MixinExtras dropped, Forge mods inside translated) |
| Registries | `ForgeRegistries` (+`Keys`), `IForgeRegistry`, tag views over the vanilla registries; `RegistryObject` stays `DeferredHolder` with the missing methods (`isPresent`, `ifPresent`, `map`...) as call rewrites; `DeferredRegister.create(IForgeRegistry, ..)` and `RegisterEvent.getForgeRegistry` rewritten |
| Capabilities | `LazyOptional`, `Capability`, `CapabilityManager`/`CapabilityToken`, `ForgeCapabilities` (item, fluid, energy tied to the NeoForge keys), `ICapabilityProvider`/`Serializable`, `AttachCapabilitiesEvent`; every `getCapability` call goes to `Caps1201`, classes declaring it get the facade interface, and `Forge1201Runtime` registers NeoForge providers for all block entity and entity types of the legacy mods |
| Networking | `SimpleChannel` (one payload type per channel, the mod's own encode/decode), `NetworkRegistry`/`ChannelBuilder`, `NetworkEvent.Context`, `NetworkDirection`, `PacketDistributor`, `NetworkHooks.openScreen`, `PlayMessages$SpawnEntity` |
| Events | `TickEvent` (server, level, player, client, render) posted from NeoForge's `*TickEvent.Pre/Post`; `RegisterCapabilitiesEvent` fired on the legacy mods' buses |
| Tools | `forward/1.20.1/forge-call-rewrites.tsv` (data), `ForgeApiRewrites1201`, `Forge1201ApiTest`; `gap1201` counts facades and call rewrites as present and prints `unresolved Forge-API symbols` per jar |

Measurement on the 60-jar set of the first run (`gap1201`, same jars): unresolved Forge-API symbols **1172** (groundwork facades only) to **599**; part of the drop is the tool now
indexing the FML and bus jars. Not done in the Forge API: generic filtering of `AttachCapabilitiesEvent<T>` (all handlers see all objects), item capabilities (`Item.initCapabilities`), renamed
or removed events that need semantic adapters (`PlayerSleepInBedEvent`, `LivingHurtEvent`...), `ConfigScreenFactory`/gui overlays, `ModList`/`IModInfo` extras, `RecipeBookType.create`
(enum extension), `INBTSerializable` with the old signatures. What blocks milestone 2 outside the Forge API (Minecraft side): mixins that fail hard when a target changed (Clumps,
Spartan Weaponry, Bygone Nether abort the whole launch), data-driven `Enchantment` subclasses, `StairBlock`/`GlassBlock` changes, `ItemPredicate.serializeToJson`, mods registering custom stats in common setup (frozen registries), library mods (GeckoLib, PuzzlesLib, Resourceful Lib, Architectury).

## 12. Status 2026-11-08: data and resources

The `data/**` and `assets/**` side of a Forge 1.20.1 jar is converted at translation time by `loader/.../resource/DataConverter1201.java` (called from `Forge1201Transformer`; a pure function of path
and bytes, unit tests in `DataConverter1201Test`, offline runner `loader/.../tools/Data1201Tool.java`, measurement `tools/compat/data1201.py`). The per-category numbers over the 60 corpus jars
are in [Legacy-1.20.1-Corpus](Legacy-1.20.1-Corpus.md) section 8: before, 19.7k pack files sat in folders 1.21.1 does not read (every data folder was plural in 1.20.1) and about 12k more files had a changed format; after, the checker finds no breaking file.

What is converted:

- **Folders**: `recipes`, `loot_tables`, `advancements`, `structures`, `functions`, `item_modifiers`, `predicates` and `tags/{blocks,items,fluids,entity_types,game_events,functions}` become singular
  (1.21); `data/<ns>/forge/{biome_modifier,structure_modifier}` becomes `data/<ns>/neoforge/...`; `data/forge/loot_modifiers/global_loot_modifiers.json` becomes `data/neoforge/loot_modifiers/...`.
- **Tags**: `forge:` tags become the convention tags `c:` (`#forge:ingots/iron` is `#c:ingots/iron`; plural/segment renames such as `glass` to `glass_blocks`, `cobblestone` to `cobblestones`, `shears` to
  `tools/shear`, derived from NeoForge 21.1's own `data/c/tags`), also the files of `data/forge/tags` (moved to `data/c/tags`, collisions merged).
- **Recipes** (vanilla recipe types; a mod's own type keeps its own format, its serializer reads it): `result` string or `{item,count,nbt}` becomes `{id,count,components}` (stonecutting's sibling `count`
  moved in), item NBT becomes data components where there is a form (potions, enchantments, stored enchantments), other NBT is dropped and counted (`nbt_dropped`); `forge:nbt` ingredients become
  `neoforge:components` (or the plain item); top-level `conditions` becomes `neoforge:conditions`; `forge:conditional` becomes one recipe per branch (`name.json`, `name_alt1.json`...).
- **Loot tables / modifiers**: `looting_enchant` to `enchanted_count_increase`, `random_chance_with_looting` to `random_chance_with_enchanted_bonus`, `set_nbt`/`copy_nbt` to `set_custom_data`/`copy_custom_data`
  (`{Potion:..}` to `set_potion`), `set_attributes` (uuid dropped, id added, operation names), `enchant_with_levels.treasure` to `options`, `enchant_randomly.enchantments` to `options`, `set_contents.type`
  to `component`, `loot_table` entry `name` to `value`, `match_tool` item predicates, `forge:can_tool_perform_action` to `neoforge:can_item_perform_ability`.
- **Advancements**: `display.icon {item,nbt}` to `{id}`, item predicates in trigger conditions (`tag`/`nbt`/`enchantments`/`potion`/`durability` to `items` and `predicates`), `conditions` to `neoforge:conditions`.
- **`forge:` ids** in `type`/`loader`/`condition` (conditions, biome modifiers, ingredient types, global loot modifier types) become `neoforge:`; ids renamed since 1.20.1 (`minecraft:grass` to `short_grass`,
  `scute` to `turtle_scute`, `sweeping` to `sweeping_edge`); dimension types get the 1.21 `monster_spawn_*` fields; models: `forge:` loaders (`forge:bucket` is `neoforge:fluid_container`), `forge_data`
  (NeoForge throws on it), `forge:item/default` parents.
- **Metadata**: `pack.mcmeta` gets format 34/48 and the open `supported_formats`; `mods.toml` already became `neoforge.mods.toml` (`mandatory` to `type`, loader/minecraft/forge ranges); now also the version
  ranges of other mods' dependencies are opened to `[0,)` (a 1.20.1 range like `[1.20.1-x,1.21)` rejects the 1.21.1 build) and `displayTest` is dropped.
- **Worldgen leniency**: 1.21 aborts the whole world load when one worldgen element does not parse, where 1.20.1 logged and went on. Known cases are repaired (Dungeons and Taverns has a structure set
  `exclusion_zone.chunk_count` of 17 where the codec allows 16, and an `other_set` list where an id is expected).

Server test (NeoForge 1.21.1, `experimental.forge1201=true`): Dungeons and Taverns (data only, 1820 pack files) loads its structure sets, template pools, processors and loot tables; the only remaining
messages are two loot tables naming `minecraft:scute` (fixed since). Six more jars were stripped to their data (no classes, so no registered content) and loaded together: the server reaches `Done`, and
every remaining recipe/loot/advancement/tag/biome-modifier error is `Unknown registry key` for something the stripped mod would have registered in code (items, blocks, feature types, custom conditions);
no parse error of a converted format is left.

What remains (data side): a mod's own recipe types still read the 1.20.1 format (`result.item`, `Ingredient.fromJson`), so the facades for `ShapedRecipe.itemStackFromJson`/`CraftingHelper` must accept the
legacy form; NBT in recipes/loot beyond potions and enchantments is dropped; function files (`.mcfunction`) are renamed but their commands are not converted; plain data packs with overlays
(`1-20-2-overlay`, Terralith) are not mods and are not touched; mods that register enchantments in code are not in the `#minecraft:non_treasure` tag that converted `enchant_with_levels` uses; texture,
sound and lang assets need nothing.

## 13. Status 2026-11-08: library mods (milestone 2, part 1)

Decision per library: a 1.20.1 library that also exists as a native NeoForge 1.21.1 mod under the same mod id is **not translated** when the pack has the 1.21.1 build; the
translated mods link against the installed build, and the API changes that matter are handled by call/field/class/bridge rewrites. Without the 1.21.1 build the pack's own 1.20.1 jar is
translated as before (fallback); with neither, the log says which build to install. This is the Waila-to-Jade pattern without a shim copy of the API: the 1.21.1 libraries keep their package
names, so only the differences need code.

| Piece | Where |
|---|---|
| Table of native libraries (mod id, name, download page) | `forward/1.20.1/libraries.tsv`, `loader/.../NativeLibraries1201` |
| Skip of the 1.20.1 jar when the modern build is installed (a `Bridge` with `configKey native1201`, so the report line reads "replaced by <id>") | `LibraryBridges.replacing` calls `NativeLibraries1201.replacing` |
| Message when a mod's mandatory dependency (mods.toml) is a table library the pack lacks: "X needs Architectury ..., install the NeoForge 1.21.1 build (url)" | `NativeLibraries1201.missing`, logged by `LegacyModLocator` |
| Rewrites, same formats as the Minecraft ones (`Mc1201Tables` reads `lib-X.tsv` after `mc-X.tsv`) | `lib-classes.tsv`, `lib-calls.tsv`, `lib-fields.tsv`, `lib-bridges.tsv` |
| Helpers | `org/kylepitcock/legacyconnector/v1_20_1/migrate/{CuriosCompat,PatchouliCompat,ArchitecturyCompat,ArchitecturyEventBuses}` |
| Tools | `./gradlew libApiDiff -Pold=<1.20.1 jar> -Pnew=<1.21.1 jar> -Ppkgs=<internal package prefixes> [-Preport=f] [-Prenames=lib-classes.tsv]` lists the public API of the old build that the new one lacks (`LibApiDiff`); `gap1201` takes `-PtargetJars=@list-file` and now translates with the real shim index, so descriptors match the game's |

What each bridged library needed (measured with `libApiDiff` and `gap1201 -Pgap.translate=true` over 10 abandoned content mods that need these libraries, with the 1.21.1 jars on the target list):

- **Architectury 9 to 13** (6 of the 72 abandoned mods need it, the top library): the API kept its packages and `ResourceLocation` networking. Only `platform.forge.EventBuses` is gone (13 asks the mod
  container for the bus: class rename to a no-op/ModList facade) and `LootEvent.LootTableModificationContext.addPool(LootPool)` now takes a builder (call rewrite through reflection). 22 missing classes in 5 jars (no library) became 4 classes and 1 method
  with the 1.21.1 jar on the class path, and 0 after the rewrites. Not handled: lambdas for `LootEvent.ModifyLootTable` (its first parameter type changed, the call site is an `invokedynamic`), `NetworkManager` receivers/senders with `FriendlyByteBuf`
  (13 uses `RegistryFriendlyByteBuf`: subclass, so most calls link), fluid/registrar internals (`FluidStack` NBT, `RegistrarBuilder.saveToDisc`, `SpawnPlacementsRegistry`, `SimpleTrade`).
- **Curios 5 to 9** (3 mods: Botania, Enigmatic Legacy, Meet Your Fight): `getCuriosInventory`/`getCurio` return `Optional` now (call rewrites to the `LazyOptional` facade), `CuriosCapability.INVENTORY/ITEM` are NeoForge capabilities (field rewrite to facade `Capability`
  objects that carry the entity/item key), the attribute modifier API moved from `UUID` to `ResourceLocation` ids and `Holder` keys (call rewrites for `CuriosApi`/`ICuriosHelper`, and bridge methods so a mod's
  `ICurioItem.getAttributeModifiers(SlotContext, UUID, ItemStack)` / `ICurio.getAttributeModifiers` override is the one Curios calls, converting the keys), `getLootingLevel` (bridge), `playRightClickEquipSound` (generic sound). Result: 27 missing classes became 7 methods and 1 field with
  the 1.21.1 jar, and 0. Not handled: `Item.initCapabilities` + `CuriosApi.createCurioProvider` (needs the item-capability facade of the Forge agents), `CurioEquipEvent`/`CurioUnequipEvent` (events renamed in 9), data generators.
- **Patchouli 85 to 93** (2 mods): the variable API takes a `HolderLookup.Provider`; seven calls rewrite to `PatchouliCompat` (registries of the running server/client). 26 missing classes became 8 methods, and 0.
- **GeckoLib 4.8 to 4.9** (a library of many content mods outside the 72; only Soul's Weapons in the corpus): the `core` packages were folded into the API packages. 49 class renames are in `lib-classes.tsv` (generated by matching simple names). **Not enough**: the renderer
  methods content mods override (`actuallyRender`, `renderRecursively`, `preRender`, `createVerticesOfQuad`... colour as four floats became a packed int), `AnimationProcessor`/`CoreGeoModel` internals, the molang `IValue` keyframes, the packet classes and `GeckoLibNetwork` have no rewrites yet.
  This is the next library task (about a day: bridge methods for the renderer overrides, a call rewrite for the colour parameters).
- **Cloth Config 11 to 15, Resourceful Lib/Config, Puzzles Lib, Jade, The One Probe, Citadel, Moonlight, Blueprint** are in the skip table (same mod ids) but have no rewrites: their API diffs were not measured (`libApiDiff` is the tool). Bookshelf has no Modrinth NeoForge file.

Measurement (10 content jars of the corpus that need Architectury/Curios/Patchouli, `gap1201 -Pgap.translate=true`): unresolved symbols **845** with no library jars on the target list (every library class missing) to **702**
(the rest is Minecraft/NeoForge/other libraries: JEI 44 classes, EMI 23, Vinery 18, Bookshelf 8, Ears 8 ...), with **0** left in the Architectury, Curios and Patchouli packages (from 22 + 27 + 26 classes in 5, 3 and 2 jars). `verify1201` over the translated jars: 2105 classes, 1772
verified, 332 not linkable here (libraries not on the verifier's class path), 1 invalid (Botania `ModelResourceLocation`, a known Minecraft item, not from the library tables).
Not done: a server run (other game processes were running, the rule is one java at a time; the translated mods were only link-checked), the config switch to force translating a 1.20.1 library even when the modern build exists, library mods other than the above.


## 14. Status 2026-11-08: milestone 2, Minecraft side (mixins and signature changes)

**Mixin triage** (`loader/.../transform/Mixin1201`, called from `Forge1201Transformer`; offline measurement `loader/.../tools/Mixin1201Triage`: `java -cp <asm,asm-tree,gson,tools> org.kylepitcock.legacyconnector.loader.tools.Mixin1201Triage <neoforge-merged.jar> <report> <jar-dir>`,
`-Dtriage.tables=<forward/1.20.1>` applies the rename tables first). Corpus of 60 Forge 1.20.1 jars (`test-mods-1201-a`): 50 carry mixin configs (82 configs, 843 mixin classes, 792 target Minecraft/NeoForge classes).
Before: 17 mixin targets and 24 members/`@At` targets named in annotations do not exist in 1.21.1 (140 annotation strings use SRG names, which the refmap used to translate), 24 configs have a plugin. What the layer does now:

- **Names**: SRG names in `method`/`@At(target)`/`@Accessor` strings become Mojang names, the class and member renames of `mc-classes.tsv`/`mc-members.tsv` are applied inside those strings (descriptors too, but never to the `CompatBufferBuilder`-style stand-ins).
- **No aborts**: Mixin aborts the launch on a required config and, whatever the config says, on an injector whose `require` is not met (`InjectionError`). Every injector gets `require = 0`, the config becomes `required: false`, `injectors.defaultRequire: 0`, no refmap;
  an invalid mixin (missing shadow member or accessor target) is then logged and skipped by Mixin itself.
- **Dropped**: a mixin whose targets are all Minecraft/NeoForge classes that do not exist in 1.21.1 (`PotionUtils`, `LootDataManager`, `ForgeGui`...) is removed from its config with a report note (12 in the corpus).
- After translation: 1 unresolved target class and 1 unresolved member (down from 17 and 24; the name-only check does not see changed signatures, so a mixin can still fail to apply at runtime and is then skipped, not fatal).
- Not done: mixin config plugins still run unchanged (they read Forge's `LoadingModList`, which the facades answer); mixins that target another mod's class are untouched.

**Signature changes** (tables in `forward/1.20.1`, helpers in `org/kylepitcock/legacyconnector/v1_20_1/migrate`):

- New generic in-place rule `=args` in `mc-calls.tsv` (spills the old arguments to fresh locals and builds the new ones, with optional static helpers `Class.method@a+b`): `ArmorItem(ArmorMaterial, Type, Properties)` (holder + durability from the material), `ShapedRecipe`/`ShapelessRecipe`
  without the id (pattern object), `FlowerBlock(MobEffect, int, ...)`, `LootItemConditionalFunction(LootItemCondition[])`, `TemptGoal`, `FollowOwnerGoal`; `=swappop` drops the second to last argument (`CustomRecipe(id, category)`).
- Stand-ins (classes renamed to facades): `ArmorMaterial` (interface, built into a 1.21.1 `ArmorMaterial` holder and cached by `ArmorCompat`), `ArmorMaterials` (wraps the vanilla ones), `MobType` (answered from entity type tags), `LootDataManager` (`MinecraftServer.getLootData()` over the reloadable registries).
- `Tier` implementations get `getIncorrectBlocksForDrops()` from their `getLevel()` (bridge). `BlockSource` (a record now) calls go to helpers. 33 class renames (`ChunkStatus`, options screens, `BlockPathTypes` to `PathType`, `GlassBlock` to `TransparentBlock`, `FrameType`, `BootstapContext`...),
  `SpawnPlacements.Type` constants, `RenderSystem.getModelViewStack()` (a `PoseStack` mirroring onto the JOML stack), `Registry.register` after the registries froze (custom stats and similar entries in common setup, the registry is opened for that one entry), `CriteriaTriggers.register`,
  `ItemStack.getUseDuration/getTooltipLines/getAttributeModifiers`, `FoodProperties.getEffects`, `Recipe.getId` (looked up in the server's recipe manager), `EnchantedBookItem.getEnchantments`, `Commands.performPrefixedCommand`, `VertexConsumer.defaultColor`, small utilities.

Measured on the 60 jars (`gap1201 -Pgap.translate=true`), Minecraft symbols still unresolved, distinct/occurrences: before classes 83/217, fields 76/115, methods 717/1288; after classes 49/129, fields 75/113, methods 643/1072.
`verify1201` over the translated classes: 26,635 linked and verified (26,594 before), the same 2 invalid (`invokevirtual` of a method that became protected in another package, `AnalogLeverBlock.getShape`, `CeilingBannerBlock.getDrops`; not caused by the migration).
Unit tests: `Mixin1201Test`, and `Mc1201MigrationTest` for `=args`, `=swappop` and the `Tier` bridge.

Open: custom `Enchantment` subclasses (data-driven in 1.21: needs a conversion of the subclass to an enchantment definition, `Enchantment.Rarity`/`EnchantmentCategory` are gone), `AbstractTreeGrower` (`TreeGrower` is final), recipe serializers and `LootItemFunctionType`/`LootItemConditionType` (serializers became codecs),
`DyeableArmorItem`/`DyeableLeatherItem`, `PortalInfo`, `Score`/scoreboard, `ServerAdvancementManager.getAdvancement`, datagen providers (`RecipeProvider`, `LootTableProvider`), `ImageButton`/`ObjectSelectionList` constructors, `Options.renderDebug`.
Not yet run in a server: the mixin and migration changes were verified offline (unit tests, `gap1201`, `verify1201`, the mixin triage tool); the server test of mixin-carrying mods (`run-subtest/mods/1.20.1`: Clumps, AttributeFix, NoChatReports, EasyAnvils, VisualWorkbench, PickUpNotifier...) is still to do.

## 15. Status 2026-11-08: GeckoLib, Farmer's Delight, Citadel, Create, first server runs (milestone 2, part 2)

- **GeckoLib 4.8 to 4.9**: the renderer API moved the colour from four floats to one packed ARGB int in 37 methods. `./gradlew libApiDiff` now reports these as `colour` lines (also when the type variable erases differently per class), and
  `tools/dev/gen_geckolib_1201.py` turns them into `lib-calls.tsv` rows (new call mode `=colour`: `GeckoCompat.pack(FFFF)I` is inserted before the call, which keeps its target and gets the new descriptor) and `lib-bridges.tsv` rows (a mod class that overrides the
  old method gets the new one, which unpacks the colour with `GeckoCompat.red/green/blue/alpha`). Hand written: `GeckoLib.initialize/shadowInit` (no-op), `AnimationController.setTransitionLength`, the eight armor bone getters (they take the model now), `GeoModel.handleAnimations`.
  Measured with `gap1201 -Pgap.translate=true` over Soulslike Weaponry and Mowzie's Mobs: GeckoLib symbols missing 18 (Soulslike: 3) to 0 for Soulslike, Mowzie 33 to 19. Left: calls of the removed no-argument bone getters from mod code (need a
  reflection helper or a compile dependency on GeckoLib), `applyMolangQueries`, `GeoArmorRenderer.renderToBuffer` (SRG `m_7695_` override, handled by the Minecraft `Model.renderToBuffer` bridge only for Minecraft owners), `RenderUtils`, `GeckoLibNetwork`/packets, molang `IValue` keyframes.
- **Farmer's Delight 1.20.1 to 1.21.1**: its `ModItems.X` / `ModBlocks.X` registry fields were `RegistryObject`s and are `Supplier`s (`Holder` for effects) now. `LibApiDiff` reports `retype` lines and `tools/dev/gen_lib_fields_1201.py` writes 368 `lib-fields.tsv`
  rows that convert the value back to a `DeferredHolder` (`RegistryCompat`); the same generator serves every library that does this. Nether's Delight and Delightful: field gaps 87 to 29. Left: the `CommonTags$Items` tag constants (now `c:` tags), `Configuration` specs,
  `KnifeItem`/`WildCropBlock` constructors (tool and effect changes of 1.21), `ToolActionIngredient`, `ModEnchantments`, recipe builder datagen.
- **Citadel 2.6 to 2.7**: the colour rows of `AdvancedModelBox.render` etc. are generated (2 rows). Alex's Mobs and Ice and Fire still miss `Citadel.sendMSGToAll/sendMSGToServer` (Citadel messages became payloads), `PacketBufferUtils.read/writeItemStack`, `CustomToolMaterial`/`CustomArmorMaterial` (the
  tier/armor material rework), `Event.Result` events; both mods are dominated by Minecraft gaps (153 and 88 methods) anyway. Not worth more until those are in.
- **Create 6.0.8 to 6.0.10**: not bridged. The 1.21.1 build replaced Ponder by its own library and Registrate changed; 77 methods in 8 classes were unresolved for Steam 'n' Rails (`CreateSceneBuilder.*`, `CreateRegistrate.*`, `IconButton.withCallback`), 3,744 symbols differ in total. Create add-ons are a milestone of their own.
- **Server runs** (`runServerSubTest`, NeoForge 1.21.1, `experimental.forge1201=true`, 1.21.1 libraries in `mods/`, mods in `mods/1.20.1/`): (1) Curios 9.5.1 plus the 1.20.1 Curios jar and Pillager Deterrent: the report says "replaced by curios (legacy curios API bridged)", the 1.20.1 Curios was not translated,
  `1.20.1 content registered: 2 blocks, 3 items`, the server reached its self test and stopped cleanly. (2) Architectury 13 plus the 1.20.1 Architectury jar and Cave Dust / Meet Your Fight: the library jar was skipped; Cave Dust (a client mod) fails on the dedicated server ("class Screen for invalid dist DEDICATED_SERVER", expected), Meet Your Fight fails at
  `RecordItem` (removed in 1.21, music discs are components: Minecraft migration). (3) Enigmatic Legacy with Curios, Patchouli and Caelus: translated fine, but its mixins abort the launch (mixin targets that no longer exist), which the Minecraft/mixin agent owns. So no run exercised Curios code paths yet (`CuriosCompat` is link-checked only);
  a Curios-using mod without mixins or removed Minecraft classes is needed.

## 16. Status 2026-11-08: milestone 2, Forge API side

Measured with `./gradlew gap1201 -Pgap.translate=true` over the 60 jars of `test-mods-1201-a` (the Forge API symbols the translated classes still miss, summed over the jars):
**594 before, 174 after** (the tool itself first had to learn to show the facades to the translator in translate mode; without that it reported 917). `verify1201` over the translated classes: 2 invalid of 26,944 linked
(both Minecraft-side `protected` access, not the facades). Two changes to the tool: `-Pgap.translate` now hands the translator the real shim index (before, the facades were invisible to it, every facade
class counted as missing: 917), and `@file,dir` target lists work together with the facade class directories.

| Item | What was done |
|---|---|
| `getCapability` on any receiver | `forge-call-rewrites.tsv` wildcard rules take any receiver as `Object`; before, the helper descriptor carried the call's owner type (`ItemStack`, `Player`, a mod class), so everything except an exact `Object` receiver was a `NoSuchMethodError` waiting to happen |
| `AttachCapabilitiesEvent<T>` filtering | `AttachGuards1201` (translator): a handler method with a generic signature (`AttachCapabilitiesEvent<Entity>`) gets an `instanceof` guard at its head; `IEventBus.addGenericListener(..)` (4 overloads) is redirected to a helper that filters by the object. Lambdas have no generic signature and are covered by the `addGenericListener` path only. The runtime attaches lazily on the first `getCapability` of an object (block entities, stacks, levels, entities), by identity, and an exception in a handler is logged once instead of failing the lookup |
| Item capabilities | classes declaring `initCapabilities(ItemStack, CompoundTag)` get `Caps1201$InitCapabilities1201`; the provider is created per stack (first), then the attach event's providers; `super.initCapabilities` is answered "none"; `Forge1201Runtime` registers NeoForge `ItemCapability` providers (items, fluids, energy) for every such item so NeoForge code sees them |
| Event adapters | `Forge1201Events` posts the old event from its replacement and applies the outcome: `LivingAttackEvent` and `LivingHurtEvent` from `LivingIncomingDamageEvent` (cancel, `setAmount`), `LivingDamageEvent` (1.20.1 meaning) from `LivingDamageEvent.Pre`, `LivingEvent.LivingTickEvent` from `EntityTickEvent.Pre`, `ShieldBlockEvent` from `LivingShieldBlockEvent`, `EntityItemPickupEvent` from `ItemEntityPickupEvent.Pre` (cancel or `DENY` blocks), `PlayerSleepInBedEvent` from `CanPlayerSleepEvent`; facades in `legacyconnector/v1_20_1/net/minecraftforge/event/entity/**`. `Event.Result` is a facade enum; `setResult/getResult/hasResult` on NeoForge events are kept per event object (`@event.` rules); `setCanceled/isCanceled/isCancelable` on NeoForge event classes that are not `ICancellableEvent` go through `eventSetCanceled`/`eventIsCanceled`/`eventIsCancelable`; `SleepingTimeCheckEvent` comes from `CanContinueSleepingEvent` |
| Class moves and renames | `ToolAction(s)` to `ItemAbility/ItemAbilities`, `IForgeShearable`, `ICondition` and the condition classes (`common/conditions`), `IForgeMenuType`, `ForgeFlowingFluid`, `ForgeEventFactory` to `EventHooks`, `ForgeHooks` to `CommonHooks`, `ForgeConfig`, `MobSpawnEvent.FinalizeSpawn`, `SpawnPlacementRegisterEvent`, `TextureStitchEvent.Post`, `EmptyHandler`, `IForgeBlock`, `ForgeBiomeModifiers.*`, `ForgeAdvancementProvider` (curated renames); member renames in the new `forward/1.20.1/forge-members.tsv` (`Tags.Items.STRING` to `STRINGS`..., `getToolAction`, `getNewTarget`, `getScrollDelta`) |
| New facades | `IEntityAdditionalSpawnData` (bridges to `IEntityWithComplexSpawn`), `IPlantable`/`PlantType`, `ForgeSoundType`, `IShapedRecipe`, `MissingMappingsEvent` (never fired), `EventNetworkChannel` (no delivery), `ForgeVersion`/`MCPVersion`, `INBTSerializable` with the old signatures (the new ones are default methods, so NeoForge code can save the object; `ICapabilitySerializable` extends it) |
| Gui | `IGuiOverlay`, `ForgeGui` (a `Gui` subclass), `NamedGuiOverlay`, `VanillaGuiOverlay` (each constant maps to the NeoForge layer that draws it), `RegisterGuiOverlaysEvent` (posted to each legacy mod's bus from `RegisterGuiLayersEvent`, forwards to it), `RenderGuiOverlayEvent.Pre/Post` (from `RenderGuiLayerEvent`); `ConfigScreenHandler.ConfigScreenFactory` is re-registered as `IConfigScreenFactory` on the mod container at client setup. Client side, compiled and linked, not run |
| Recipes and NBT | `CraftingHelper` facade (`getItemStack` in the 1.20.1 `item/count/nbt` form, `getNBT`, `processConditions`, `register` for ingredient and condition serializers, which are remembered but not consulted: a mod's own ingredient type is not understood by 1.21.1); `ShapedRecipe.itemStackFromJson` rewrite; `ItemStackHandler`/`FluidTank`/`FluidStack` NBT, packet and tag methods rewritten to the registry-aware or component forms (`ForgeApi1201.registries()`), `FluidUtil.getFluidHandler` returns a `LazyOptional`; `RecipeBookType.create` and `RecipeBookCategories.create` add enum constants at runtime (`LegacyEnums`); `LazyOptional.map/orElseGet/orElseThrow/filter` take the `NonNull*` functions; `NetworkHooks.getEntitySpawningPacket`, `SimpleChannel.sendTo`, login-packet builder methods (no-ops), `ForgeRegistries.FLUID_TYPES` as a `Supplier`, `Keys.BIOME_MODIFIERS`, `IForgeRegistry.getDelegateOrThrow` |

Not done: `IIngredientSerializer`/`AbstractIngredient` as working ingredient types (Ingredient is final in 1.21.1; needs `ICustomIngredient` + codecs), `ITeleporter`, `ForgeSlider`, `ForgeHooksClient`, `ModelEvent.RegisterGeometryLoaders.register(String, ..)`, `Event.Result` semantics on NeoForge's own events (`RightClickBlock.setUseBlock` and similar are
TriState-based now), `FluidStack(Fluid, int, CompoundTag)` constructors, `GlobalLootModifierProvider` datagen, `NeoForgeMod.*_REACH`/`WATER_TYPE` attribute fields (Minecraft side), `ModList`/`IModInfo` extras (nothing in the corpus needs more than FML 4 offers).

Server test (`run-subtest/mods/1.20.1`, Another Furniture, AttributeFix, Comforts, Tom's Storage, Veinminer, Lootr, Farmer's Delight, Carry On): no run reached the "1.20.1 content registered" line in this session. Each blocker found was fixed on the Forge side (Lootr's `ModConfigSpec.setConfig`, Farmer's Delight's `CompoundIngredient` subclass, Comforts' `SleepingTimeCheckEvent`) or is Minecraft-side and belongs to the other agents: Farmer's Delight `Enchantment` subclass (final), Lootr `loot.Serializer`, mixins that abort the launch (Carry On, Farmer's Delight; the test copies had the manifest `MixinConfigs` stripped, which in turn broke Another Furniture's `FireBlockAccessor` cast: a test artifact). Mod-less checks passed: `./gradlew compileJava --rerun generateShimIndex check`, `verify1201`.

## 17. Status 2026-11-07: Terramity (Forge 1.20.1, GeckoLib content mod)

Terramity 0.9.8 (MCreator generated, no newer version, needs GeckoLib, Caelus, Curios and Patchouli) is the first full content mod through the 1.20.1 layer: offline 0 unresolved Minecraft symbols (from 46 methods and 5 classes), server `Done` with 207 blocks, 754 items, 81 entity types and 19 block entity types, client in a world with blocks and GeckoLib mobs/bosses rendering (`docs/screenshots/1.20.1/`).
Generic fixes it needed (no mod-specific code): custom `Enchantment` subclasses (a stand-in `LegacyEnchantment` registered in a registry of its own plus a generated data pack entry per enchantment, `LegacyEnchantments`); overrides of methods that became final (`mc-overrides.tsv`: `LivingEntity.getDimensions` to `getDefaultDimensions`); `@Mod.EventBusSubscriber` renamed to NeoForge's annotation (classes without subscribers, or with only client ones, are adjusted); constructor rewrites for tools, arrows, stairs, buttons, plates, liquids, buckets, flowers; private `SpawnPlacements.register` / `MenuScreens.register` through reflection; `RecordCodecBuilder` codecs registered as `MapCodec` (loot modifiers); GeckoLib `CoreGeoBone` calls made virtual; Caelus and `PotionUtils` stand-ins; many small signature helpers (`EntityCompat`, `ClientCompat`).
Not done: enchantment hooks (`doPostAttack`...), `Item.getDefaultAttributeModifiers` overrides, brewing recipes, first-person held model of GeckoLib items, GUIs not exercised, world generation not checked.

## 18. Status 2026-11-08: ATM 9 top blockers (datagen, items, hooks)

Measured with `tools/compat/atm9_run.py` over the 506 ATM 9 jars (layer 0.4.7 plus this branch, own re-run of the baseline first): jars that link **53 to 79**, jars with unresolved references 435 to 409, unresolved
jar/symbol pairs **26,994 to 21,890 (-19%)**. 60-jar corpus (`gap1201 -Pgap.translate=true`, `test-mods-1201-a`): Minecraft symbols classes 49/129 to **31/64**, fields 75/113 to **67/99**, methods 643/1072 to **432/628**
(distinct/occurrences; the "before" is section 14, the corpus is unchanged); `verify1201`: 27,238 classes linked and verified, 3 invalid (2 were known `protected` access cases; the third, `RegUtils.lambda$regBlockItem$1`, is a
merge of a class of another mod the verifier can see now). Server run not done (two game processes of other agents were already running).

| Blocker (jars before to after) | What was done |
|---|---|
| Datagen: `ShapedRecipeBuilder.unlockedBy` 112 to 0, `RecipeProvider.<init>` 96 to 0, `LootTableProvider$SubProviderEntry` 89 to 0, `BlockLootSubProvider.<init>` 62 to 0, `FinishedRecipe` 65 to 21 | `Datagen1201` (loader, runs in `Forge1201Transformer` before the migration): a class that extends or implements a `net.minecraft.data`/model-generator/`neoforge.common.data` class, is named like a provider or builder (`*RecipeProvider`, `*LootTableProvider`, `*TagsProvider`, `*RecipeBuilder`, a `datagen` package...) or is nested in one becomes an empty shell (constructors throw, static fields and `<clinit>` stay because tag providers double as `TagKey` holders, no instance fields or methods, no interfaces); any other method that takes a `GatherDataEvent`, uses a datagen-only class, or has one in its descriptor returns its default value, abstract ones go. A kill switch for measuring: `-Dlegacyconnector.keepDatagen=true`. Unit test `Datagen1201Test` |
| `critereon.*` (46 to 46), `AdvancementHolder$Builder` 58 to 11 | Custom advancement triggers (`AbstractCriterionTriggerInstance`, `DeserializationContext`, `ItemPredicate.fromJson`) are runtime code and stay: they need the `Criterion`/codec rework |
| Items: `RecordItem` 42 to 0, `DyeableLeatherItem` 33 to 0, `ItemStack.serializeNBT` 50 to 0, `PotionUtils` / `Enchantment$Rarity` / `Holder.get` were already 0 on master (Terramity work) | stand-ins `LegacyRecordItem` (an item with the disc's sound and length; **does not play in a jukebox**: that needs a `jukebox_song` data pack entry), `LegacyDyeableLeatherItem` (the `dyed_color` component); `ApiCompat` helpers; missing `LegacyPotionUtils` members; 17 more `Potions.*` constants (`Potions.EMPTY` is water, as `getPotion` answers) |
| Loot/particles: `loot.Serializer` 56 to 0, `ParticleOptions$Deserializer` 45 to 0, `ParticleType.<init>` 43 to 0 | `LegacyLootSerializer`, `LegacyConditionalFunctionSerializer` (reads the `conditions` array), `LootCompat.codec` (a `MapCodec` that hands the serializer the JSON object; nested loot objects come as JSON, the Gson context is plain); `LootItemFunctionType`/`LootItemConditionType` constructors are `=args` rows. `ParticleType`: constructor drops the deserializer, bridge `codec()` to `MapCodec` and a generated `streamCodec()`; `LegacyParticleDeserializer` interface |
| NeoForge: `CommonHooks` 43 to 0 (burn time, crops growth, block break, critical hit, left click, living drops), `ForgeHooksClient` 47 to 0 (renamed to `ClientHooks`), `TierSortingRegistry` 39 to 0, `ITeleporter` 32 to 2, `Tags$Items` constants 21 to 0 for the common ones (armor, heads, colored glass and a few more tags remain, 1 to 6 jars each), `BuildCreativeModeTabContentsEvent.accept(Supplier)` 24 to 0, `RegisterGeometryLoaders.register(String,..)` 48 to 0, `RegisterAdditional` 41 to 0, `RenderLevelStageEvent.getPartialTick` 34 to 0, `FluidStack` constructors 32 to 1 | facades `TierSortingRegistry` (names and order kept, correctness answered from the incorrect-blocks tag) and `ITeleporter` (`Entity.changeDimension(level, teleporter)` calls `placeEntity` then a `DimensionTransition`); `ApiCompat`/`ApiCompatClient`; `forge-members.tsv` rows for the `c:` tag names (`COBBLESTONE` to `COBBLESTONES`, `TOOLS_BOWS` to `TOOLS_BOW`...) |
| Also (next tier of the ranking) | `SaplingBlock`/`AbstractTreeGrower` (`LegacyTreeGrower` over `TreeGrower` keys), `TorchBlock` particle argument, `BucketPickup`/`LiquidBlockContainer`/`BonemealableBlock`/`ArrowItem` (calls and bridges for the added `Player`/weapon parameters), `Screen.mouseScrolled(DDD)`, `MobRenderer.render` (call and bridge), `NbtIo` over `Path`, `BlockEntity.saveWithoutMetadata`, `DataResult.getOrThrow(boolean, Consumer)`, fluid/item buffer writes, `NeoForgeMod.WATER_TYPE`, `IForgeItem`/`IForgeBlockEntity`/`IForgeMobEffect` to the `I*Extension` interfaces |

Lesson from the run: a helper row whose descriptor is derived from the old argument types (`=args` with `Class.method@N`) must declare the helper parameter with exactly that type; a mismatch shows up as a reference to the helper itself in the next probe
(`ApiCompat.noPlayer(LevelAccessor)` unresolved in 29 jars), so the probe also checks the layer's own helpers.

Remaining top blockers on the ATM 9 set (jars): custom advancement triggers and `critereon` (46), `EntityRenderersEvent.AddLayers.getSkin` (31), `Options.renderDebug` (29), `AbstractProjectileDispenseBehavior` (26), `CauldronInteraction.WATER` map type (24),
recipe serializers `fromNetwork`/`fromJson`/`toNetwork` (23/22/20, codecs), `ItemStack.ATTRIBUTE_MODIFIER_FORMAT` (23), `ForgeRegistry` (22), `CraftingRecipe.getRemainingItems(Container)` (22), `UnbakedModel.bake` (22), `PathPackResources` (20),
`DeferredRegister.makeRegistry` (20), `PortalInfo` (20), `RecipeWrapper` (19), `MapDecoration.Type` (18), `DiggerItem.<init>` with tag (18), `Entity.getMyRidingOffset` (18), `IFluidBlock` (18). Not run in a game: everything here is link level, like sections 10 to 16.

## 19. Status 2026-11-08: ATM 9, registries and small Forge facades

ATM 9 re-run (`atm9_run.py`, 506 jars): unresolved jar/symbol pairs **21,890 to 21,700**, jars that link unchanged at 79 (409 with unresolved references). New facades (`legacyconnector/v1_20_1/net/minecraftforge/...`): `RegistryBuilder` (Forge settings, adapted to NeoForge's builder and callbacks), `IForgeRegistry.AddCallback/ClearCallback/BakeCallback/CreateCallback/ValidateCallback`, `IForgeRegistryInternal`, `RegistryManager` (`ACTIVE.getRegistry`), `ForgeRegistry` (abstract base of the wrapper: `getID`, `getValue(int)`), `IFluidBlock`, `items.wrapper.RecipeWrapper` (takes an `IItemHandlerModifiable`), `NonNullLazy`; call rewrites `DeferredRegister.makeRegistry(Supplier)` and `NewRegistryEvent.create(RegistryBuilder)` (both return `Supplier<IForgeRegistry>`). Jars before to after: `RegistryBuilder.disableSaving` 18 to 0, `makeRegistry` 20 to 0, `IFluidBlock` 18 to 0, `RecipeWrapper.<init>` 19 to 0, `NonNullLazy` 13 to 0, `RegistryManager.ACTIVE` 12 to 0, `NewRegistryEvent.create` 14 to 6 (other descriptor forms), `ForgeRegistry` (all members) 78 to 55.
Known limit: `makeRegistry` copies the non-final fields of the Forge builder onto NeoForge's builder by reflection; callbacks are applied only for `NewRegistryEvent.create`. Not done: recipe serializer codec bridge (`fromNetwork`/`fromJson`/`toNetwork`), `EventHooks.getMobGriefingEvent`, `ForgeChunkManager`, `ModLoader`, `ForgeTier`, `AbstractIngredient`/`StrictNBTIngredient`, `RightClickBlock.getUseBlock`, `PathPackResources`, `ModConfig` constructors, `CapabilityProvider`. Not run in a game or server; `gap1201`/`verify1201` on the 60-jar corpus and a server test were not repeated in this session.

## 20. Status 2026-11-08: Minecraft-side blockers of the ATM 9 run, round 2
## 20b. Status 2026-11-08: Minecraft-side blockers of the ATM 9 run, round 2

Fresh baseline on master with `tools/compat/atm9_run.py` (506 jars): links 79, jars with unresolved references 409, unresolved jar/symbol pairs 21,890. After this round: links **81**, jars with unresolved references **407**, pairs **21,505 (-385, -1.8%)**; the big single-symbol blockers are gone, but most jars have many other misses, so few jars flip to linking.

| Blocker (jars) | What was done |
|---|---|
| `Options.renderDebug` 29 | the loader now rewrites dropped **instance fields**: `mc-calls.tsv` rows named `$get$field` / `$set$field` (descriptors `()T` / `(T)V`, helper takes the receiver) replace `GETFIELD` / `PUTFIELD`; `ClientCompat.getRenderDebug/setRenderDebug` read and toggle the `DebugScreenOverlay` |
| `AbstractProjectileDispenseBehavior` 26 | stand-in class `LegacyProjectileDispenseBehavior` (the old `execute`/`getProjectile`/`getPower`/`getUncertainty` over `DefaultDispenseItemBehavior`), `mc-classes.tsv` |
| `CauldronInteraction.WATER` 24 | `WATER/LAVA/POWDER_SNOW/EMPTY` fields read as `InteractionMap`, `MiscCompat.cauldronMap` returns its `map()`; `newInteractionMap()` registers a named map |
| `ItemStack.ATTRIBUTE_MODIFIER_FORMAT` 23 | `none` field row with `MiscCompat.attributeModifierFormat` |
| `CraftingRecipe.getRemainingItems(Container)` 22 | call row to `RecipeCompat.getRemainingItems`, and an `invokespecial` (super call) `=args` row that converts the `Container` with `BridgeHelpers.input` |
| `UnbakedModel.bake` 22 | bridge (`0,1,2,null`: the mod's 4-argument override is called from 1.21.1's 3-argument one) and a `=pop` call row |
| `PortalInfo` 20 | stand-in `LegacyPortalInfo` (the four public fields); `ITeleporter.getPortalInfo` default |
| `MapDecoration.Type` 18 | stand-in enum `LegacyMapDecorationType` with `holder()` (`MapDecorationTypes` by reflection); `addTargetDecoration`/`addDecoration` convert it |
| `AddLayers.getSkin(String)` 31 | `ClientCompat.getSkin` ("slim"/"default" to `PlayerSkin.Model`) |
| smaller | `BucketItem.getFluid` (17, the `content` field), `MobEffectInstance.save(CompoundTag)` (18), `ShapedRecipe.getRecipeWidth/Height` and `Advancement.getId` renames (`mc-members.tsv`) |

Not done (still the top Minecraft-side blockers): custom advancement triggers (`DeserializationContext` 46, `AbstractCriterionTriggerInstance` 42, `ContextAwarePredicate.ANY` 29, `SerializationContext` 27): a bridge needs stand-ins for the JSON context classes, an instance class implementing `SimpleCriterionTrigger.SimpleInstance`, and a `Codec` that reads the JSON object through the mod's `createInstance(JsonObject, ContextAwarePredicate, DeserializationContext)`
(plus the predicate `fromJson`/`toJson` family: `ItemPredicate.fromJson`...); `FinishedRecipe` 21, `ParticleTypes.ENTITY_EFFECT` 20 (now `ParticleType<ColorParticleOption>`), `Block.getCloneItemStack` 4-argument form 20, `Entity.canChangeDimensions()` 18 and `getMyRidingOffset` 18, `DiggerItem.<init>` with tag 18, `Pack.readMetaAndCreate` 18, `ObjectSelectionList.<init>` 17, `BuiltInRegistries.POTION` type 17, `EnchantmentHelper.enchantItem` 17, `VertexConsumer.vertex` 14-argument 17, `DataResult.get()` 16, mixin accessors on `RandomizableContainerBlockEntity.setLootTable` 16. `PathPackResources` (20) belongs to the Forge-API side (`net.minecraftforge.resource`). Mod subclasses that inherit a renamed member (`ShapedRecipe.getRecipeWidth` called through the mod's own subclass) are not matched by `mc-members.tsv` (owner is the receiver type). No server run was done in this round (the processes limit was reached by other agents); verified by the unit tests (`Mc1201MigrationTest`), `./gradlew compileJava --rerun generateShimIndex check` and the ATM 9 link probe.
## 21. Status 2026-11-08: recipe serializer codec bridge and more Forge API

ATM 9 (506 jars): unresolved jar/symbol pairs **21,700 to 21,495** (section 19 started at 21,890); jars that link still 79. A class of a mod that declares the 1.20.1 serializer methods (`fromNetwork(ResourceLocation, FriendlyByteBuf)`...) gets `codec()` (a `MapCodec` that reads the JSON through `JsonOps` and calls the mod's `fromJson`) and `streamCodec()` (the `toNetwork`/`fromNetwork` pair over the registry buffer) added by `Mc1201Migration.addRecipeSerializerCodecs`; calls of `fromJson`/`fromNetwork`/`toNetwork` on any `RecipeSerializer` go to `RecipeCompat` (the legacy method when the object has one, the codec of a NeoForge serializer otherwise). Limits: the recipe id is not known to a codec (a placeholder `legacyconnector:unknown` is passed), encoding to JSON is not supported (data generation only).
Other additions: `ForgeTier` facade (level picks the vanilla "incorrect for" tag), `PathPackResources` facade, `FriendlyByteBuf.read/writeRegistryId(Unsafe)` helpers, `RightClickBlock.get/setUseBlock/UseItem` as `Event.Result`, `AnvilUpdateEvent.setCost(int)`, `ForgeSpawnEggItem.fromEntityType`. Jars before to after: `RecipeSerializer.fromJson` 24 to 0, `toNetwork` 20 to 0, `ForgeTier` 15 to 0, `AnvilUpdateEvent.setCost` 15 to 0, `fromEntityType` 13 to 0, `getUseBlock` 14 to 2, `RegistryId` 16 to 9, `PathPackResources` 30 to 13 (the rest are `ResourcePackLoader.createPackForMod`).
60-jar corpus (`probe.py --mc 1.20.1`): 6 of 60 link, 7,329 unresolved symbols (2,995 classes, 2,796 methods, 1,538 fields; no earlier number in this metric). Not done: `verify1201` on the translated classes and a server test were not run in this session; `EventHooks.getMobGriefingEvent`, `BuildCreativeModeTabContentsEvent.getEntries`, `CapabilityProvider`, `ForgeChunkManager`, `ModLoader.get`, `ModContainer.addConfig`, `StrictNBTIngredient`/`AbstractIngredient`, `FluidHandlerItemStack(ItemStack,int)`.
## 22. Status 2026-11-08: soft link (stand-ins for symbols 1.21.1 lacks)

A reference to a Minecraft 1.21.1 or NeoForge symbol that does not exist used to count against the whole mod, and a class that merely mentioned a missing class could fail to verify and take the registration of the mod's content with it. `SoftLink1201` (loader, runs after the whole jar is migrated; config `experimental.forge1201.softlink`, default on) now replaces what cannot link by a stand-in, as the older-version pipelines do with synthesized classes:

- a call of a missing method or constructor calls `SoftLink1201Runtime.fail(symbol)`, which logs the symbol once (capped) and throws `UnsupportedOperationException` when, and only when, it runs; the arguments are popped and the default of the result type is left, so the stack-map frames stay valid (a constructor call on `this` throws directly and that class gets its frames recomputed);
- a read of a missing field gives the default (null, 0), a write is dropped; `instanceof` of a missing class is false; `new`, casts, arrays and class literals of it fail when executed; a `catch` of it is dropped; a missing interface is removed from the class;
- a mod class whose superclass is missing (e.g. `AbstractIngredient`) is reported but not changed: code assigns it to the original type.

Only provable gaps are touched: the owner must be a class of the jar or of the generated 1.21.1 index (`mc-members.index.gz`, made by `tools/McMembers1201Tool` from the NeoForge merged jar), and every class on the way up must be known; chains that leave into another mod or a library, and Mixin classes, are left alone. Each stand-in is a `Soft link: ...` note per symbol in the translation report, and the offline gap report keeps those symbols as unresolved, so the compatibility site never rates such a mod above "loads". `verify1201` on the 60-jar corpus: 27,246 classes link and verify (27,238 without soft link), the same 2 known `protected` access cases invalid; about 1,250 symbols in 29 of the 60 jars are stood in.

Server batches were not run: more than two game processes of other agents were running the whole time. `tools/dev/make_batch_1201.py <mods dir> <target> <fragment>...` builds a batch (seeds plus required dependency jars) from a read-only pack folder; `test-mods-1201-batch*/` is gitignored.

## 23. Status 2026-11-08: Minecraft-side blockers, round 3 (advancement triggers and the rest of the list)

ATM 9 link probe (506 jars): links 79 to **82**, jars with unresolved references 409 to **406**, unresolved jar/symbol pairs 21,890 to **21,002 (-888, -4.1%)** (this round alone: 21,505 to 21,002). 60-jar corpus (`gap1201 -Pgap.translate=true`): Minecraft symbols classes 31/64 to **24/37**, fields 67/99 to **61/76**, methods 432/628 to **404/570** (distinct/occurrences). `verify1201`: 27,274 classes linked and verified (27,238 before), the same 2 invalid (protected access across packages: `CeilingBannerBlock.getDrops`, `AnalogLeverBlock.getShape`).

| Blocker (jars) | What was done |
|---|---|
| Custom advancement triggers: `DeserializationContext` 46, `AbstractCriterionTriggerInstance` 42, `ContextAwarePredicate.ANY` 29, `SerializationContext` 27 (all 0 now) | `mc-classes.tsv` renames four classes to stand-ins: `LegacySimpleCriterionTrigger` (extends 1.21.1's `SimpleCriterionTrigger`; its `codec()` is a `Codec` that converts the input to JSON with the ops of the decode in progress, decodes the `player` predicate with `ContextAwarePredicate.CODEC`, and calls the mod's `createInstance(JsonObject, ContextAwarePredicate, DeserializationContext)`; encoding calls the instance's `serializeToJson`), `LegacyCriterionInstance` (implements `SimpleInstance`, `getCriterion`, `serializeToJson`, `player()`), `LegacyDeserializationContext`, `LegacySerializationContext`. `CritCompat` holds the JSON helpers: `ItemPredicate`/`EntityPredicate`/`LocationPredicate` `fromJson`/`serializeToJson`/`fromJsonArray` (through the 1.21.1 codecs), `ContextAwarePredicate.toJson`/`fromElement`, `EntityPredicate.fromJson(obj, name, ctx)`, and the `ANY` constants (`none` field rows: the predicate builders' empty result; `ContextAwarePredicate.create()`). `ItemPredicate.matches` becomes `test`, `Listener.getTriggerInstance` becomes `trigger`. Registration was already `CriteriaTriggers.register` to `MiscCompat.registerTrigger`. Not run in a game: a trigger fires only with an advancement that names it |
| `FinishedRecipe` 21 | `LegacyFinishedRecipe` stand-in interface |
| `ParticleTypes.ENTITY_EFFECT` 20 | field row to `MiscCompat.entityEffectParticle` (the witch swirl: the 1.21.1 particle takes a colour option, the old call passed the colour as speed) |
| `Block.getCloneItemStack` 5 arguments 20, `BlockState.getCloneItemStack` 13 | call rows (also `super` calls) re-describe the call with `LevelReader`; bridge for the mod's override |
| `Entity.canChangeDimensions()` 18, `getMyRidingOffset()` 18 | calls to `MiscCompat` (the entity's own level twice; no offset), bridge from the mod's `canChangeDimensions()` to the two-level method |
| `DiggerItem.<init>` with tag 18, `ObjectSelectionList`/`ContainerObjectSelectionList.<init>` 17+10 | `=args` rows (attributes into the properties; the bottom coordinate dropped) |
| `EnchantmentHelper.enchantItem(random, stack, level, treasure)` 17, `VertexConsumer.vertex` 14 floats 17, `DataResult.get()` 16, `RandomizableContainerBlockEntity.setLootTable` (static 16, instance with seed 12) | `MiscCompat`/`VertexCompat` helpers (the enchantments come from the server registry; `addVertex` with a packed colour; `Either` of value and error; loot table key and seed) |
| `mc-members.tsv` limitation | `Mc1201Migration` renames a Minecraft member called through a mod class (`MyRecipe.getRecipeWidth()` where `MyRecipe extends ShapedRecipe`) when no mod class declares it: the rows are tried against every Minecraft ancestor |

Still open (top of the fresh ranking, jars): recipe serializers `fromNetwork`/`fromJson`/`toNetwork` 23/22/20 (Forge-API side), `ItemStack.equals(ItemStack, boolean)` 19, `Pack.readMetaAndCreate` 18 (needs the new `PackLocationInfo` and a `ResourcesSupplier` adapter), `BuiltInRegistries.POTION` 17 (now a plain `Registry`; the old type was a `DefaultedRegistry`, which cannot be faked without a wrapper), `MobEffect.getAttributeModifierValue`/`getAttributeModifiers` 16, `MobRenderer.setupRotations` 16, `NbtUtils.writeGameProfile` 16, `VertexFormat.getElements` 16, `EnchantmentHelper.getTagEnchantmentLevel` 16 (stand-in `LegacyEnchantment` arguments), `BlockModel.bake` 15, `ModelBakery.MISSING_MODEL_LOCATION` 15, `ShapedRecipe.assemble/matches(CraftingContainer)` 15, `ClientboundAddEntityPacket.<init>(Entity)` 15, `RecipeManager.byType` 14, `LootTableReference` 14; the predicate leftovers `EnchantmentPredicate.<init>` 12, `StatePropertiesPredicate` fromJson/ANY 6+4, `MinMaxBounds.fromJson` 5.

## 25. Status 2026-11-08: soft link policy and `MobEffect.addAttributeModifier`

Crash report (Born in Chaos, Forge 1.20.1): `StunMobEffect.<init>` called `MobEffect.addAttributeModifier(Attribute, String, double, Operation)`, which 1.21.1 changed to `(Holder<Attribute>, ResourceLocation, double, Operation)`; the soft link stand-in threw during registration and took the mod and the game down. Two fixes:

- Real migration: `mc-calls.tsv` row to `HolderCompat.addEffectModifier` (attribute wrapped as a holder, the UUID string turned into `legacyconnector:modifier/<sanitized uuid>`).
- Soft link policy (`SoftLink1201.call`): a stand-in for a missing non-static method that returns the receiver class or a supertype of it (builder-style setters) now **returns the receiver** after logging once (`SoftLink1201Runtime.note`); a missing method with a void or primitive result called from `<init>`, `<clinit>` or `lambda$static$`/`lambda$new$` is a logged **no-op**. Everything else still throws only when executed. Tests: `SoftLink1201Test`.

## 26. Status 2026-11-08: ATM 9 ranking round (method handles, recipes, enchantment helpers, creative tab entries)

ATM 9 link probe (506 jars): links 81 to **82**, unresolved jar/symbol pairs 19,297 to **18,789 (-508)** (own re-run of the same day as baseline). Generic changes:

- **Method references follow the call rules**: `Mc1201Migration.redirectHandles` rewrites a method handle (`ItemStack::of`, `Foo::new`) to the static helper of a `mc-calls.tsv` row, and `CallRewrites1201` turns `ResourceLocation::new` into the `parse`/`fromNamespaceAndPath` factory (`ResourceLocation.<init>(String)` was the top Minecraft symbol, 33 jars).
- `ShapedRecipe`/`ShapelessRecipe` `assemble`/`matches` with `CraftingContainer` (calls, `super` calls, and bridges for the mod overrides; `RoundCompat.craftInput`/`craftingContainer`), `LivingEntityRenderer.setupRotations` (scale argument), `ItemStack.equals(ItemStack, boolean)`, `IShearable` (player argument; call and bridge rows), `BuildCreativeModeTabContentsEvent.getEntries()` (`LegacyTabEntries`, a `MutableHashedLinkedMap` view that forwards to the event), `EnchantmentHelper` `hasFrostWalker`/`hasBindingCurse`/`hasVanishingCurse`/`getFireAspect`/`getKnockbackBonus`/`getTagEnchantmentLevel`/`deserializeEnchantments`, `EnchantmentInstance.enchantment`, `BuiltInRegistries.POTION`/`PAINTING_VARIANT` (typed `Registry` now), `NbtUtils.read/writeGameProfile`, `new ClientboundAddEntityPacket(Entity)`, `CriticalHitEvent` getters and `ForgeEventFactory.getMobGriefingEvent` renames, `ModLoader.get()` facade.

Not done, and why: `Pack.readMetaAndCreate` (the `ResourcesSupplier` interface has two abstract methods now, so a lambda written for 1.20.1 cannot be adapted by a call row), `AbstractIngredient`/`StrictNBTIngredient` (a mod subclass needs an `ICustomIngredient` adapter), `ForgeChunkManager` (tickets need a `TicketController` registered on the mod bus before loading finishes), `ModContainer.addConfig`/`ModConfig` constructors, `MobEffect.getAttributeModifiers` (templates are private in 1.21.1), library packages (REI/EMI/TOP/Create/Catnip/Flywheel: their own 1.21.1 builds exist or are far larger than a bridge).
## 24. Status 2026-11-08: mixin injection failures must not abort the launch

Wither Storm crashed the bootstrap with `InjectionError: LVT ... has incompatible changes` in `AbstractSkeleton::performRangedAttack`: an `@Inject(locals = LocalCapture.CAPTURE_FAILHARD)` whose captured locals differ in 1.21.1. `require`/`defaultRequire` do not cover that error (it is raised while injecting, after the target matched), so `Mixin1201` now rewrites every injector's `locals` to `CAPTURE_FAILSOFT` (everything but `NO_CAPTURE`); Mixin then logs a warning naming the mixin and mod and skips only that injection. Together with the existing `required: false`, `defaultRequire: 0`, per-injector `require = 0` and dropping of mixins whose target class is gone, a translated mod's mixin cannot kill the game for target, descriptor or locals mismatches; the per-jar report line counts the relaxed captures. Still impossible: errors thrown by the mixin class itself (invalid `@Shadow`/accessor are only logged), a `@Redirect`/`@ModifyArg` that matches with wrong semantics, or a mixin that applies cleanly but behaves wrongly; a config plugin cannot know beforehand whether an injection will fit.

## 28. Status 2026-11-08: ingredients, packs, chunk tickets, mod configs, Create

ATM 9 (506 jars): links 82 to **85**, unresolved pairs 18,789 to **18,655** (Create and its Flywheel/Ponder/Catnip libraries now count as obsolete when the native build is present). Generic changes:

- **NBT ingredients**: `StrictNBTIngredient`/`PartialNBTIngredient` are renamed to `Ingredient` (it is final in 1.21.1) and their `of` factories go to `DataComponentIngredient` (`RoundCompat.strictIngredient`/`partialIngredient`; NBT is the custom data component). **Not done: `AbstractIngredient` subclasses** (a mod class cannot extend the final `Ingredient`; would need a rewrite of every use site to an `ICustomIngredient` wrapper), `CraftingHelper.register` stays a remembered no-op.
- **Packs**: `Pack$ResourcesSupplier` is renamed to the one-method stand-in `LegacyResourcesSupplier` (so 1.20.1 lambdas link); `Pack.readMetaAndCreate` builds the `PackLocationInfo`/`PackSelectionConfig` and adapts the supplier; `new PathPackResources(String, Path, boolean)` builds its location info.
- **ForgeChunkManager** facade over `TicketController`: one controller per 1.20.1 mod id registered on `RegisterTicketControllersEvent`, the mod's validation callback adapted (`TicketHelper` shows the pair of sets again); a request without a controller falls back to the vanilla forced chunk.
- **Mod configs**: `new ModConfig(...)` goes through `ConfigTracker.registerConfig`, `ModContainer.addConfig` is then a no-op, `getConfigData`/`save` read the loaded config. `MobEffect.getAttributeModifiers()` (built with `createModifiers(0)`) and `getAttributeModifierValue`.
- **Create**: `libraries.tsv` rows for `create`, `flywheel`, `ponder`, `catnip` (the pack's Create 6.0 for 1.20.1 and the native 1.21.1 Create 6 share most class names; addons are not checked offline against it).
## 27. Status 2026-11-08: library and other-mod references (Jade, JEI, Architectury, native library table)

The "library or other mod" part of the ATM 9 ranking was mostly **classes of mods that are not on the probe's class path** (The One Probe, EMI, REI, Waila/WTHIT, JourneyMap, CraftTweaker, Game Stages, JEI, Jade: 9,485 symbols), not API drift. Two measurement changes and a set of bridges:

- **The probe can check against the real 1.21.1 library jars.** `GAP1201_LIBS=<folder of NeoForge 1.21.1 mods>` (environment of `tools/compat/probe.py`/`atm9_run.py`) adds them to the target list, and `Gap1201` now indexes jar-in-jar libraries (`META-INF/jarjar`: Create carries Registrate, Flywheel and Ponder). On a 97-jar subset of ATM 9 with the libraries of an ATM 10 folder (Jade, JEI, Curios, Patchouli, GeckoLib, Architectury, Create, Citadel, Cyclops...) the library/other-mod symbols fall from 15,173 to 6,803, and what is left is real drift or classes of mods the folder lacks.
- `GAP1201_ASSUME_NATIVE=prefix,prefix` (off by default) does not count references into API packages of libraries the pack would install as native 1.21.1 builds when no jar is at hand (for example `mcjty/theoneprobe/,dev/emi/emi/,me/shedaniel/,mcp/mobius/waila/,journeymap/client/api/,com/blamejared/crafttweaker/,net/darkhax/gamestages/`); it cannot see signature drift, so use it only to rank what is left (5,346 symbols on the subset).
- `libraries.tsv` gained REI, WTHIT (mod id `waila`), JourneyMap, CraftTweaker and Game Stages (they have native NeoForge 1.21.1 builds; the 1.20.1 copy is skipped when the build is installed, and translated as a fallback otherwise).
- **Bridges (`lib-calls.tsv`, `lib-classes.tsv`, helpers `JadeCompat`, `JeiCompat`, `ArchitecturyCompat`)**: Jade 15 provider registration (`IComponentProvider<Accessor>` parameters), `ITooltip.getElementHelper/remove`, `IElementHelper.text`, `Identifiers` to `JadeIds`; JEI 19 `ForgeTypes` to `NeoForgeTypes`, `getCraftingCategory`, `useNbtForSubtypes`, `IPlatformFluidHelper.create` (fluid holders); Architectury 13 `FluidStack.getTag` and `create(..., CompoundTag)` (custom data component).
- Left (largest first): Botarium energy/fluid API (3 jars), Refined Storage 1 API (RS 2 is another API), Structure Gel (abandoned), Resourceful Config/ByteCodecs, GeckoLib `GeoModel.setCustomAnimations` override, Citadel packets, Cyclops Core (a mod with its own drift), Waila/WTHIT plugin API as such (the classes are WTHIT's; their signatures were not checked).

## 32. Status 2026-11-08: library round 2 (native libraries no longer shadowed, client-only mods deprioritised)

- `Gap1201` no longer indexes the 1.20.1 copy of a library that has a native 1.21.1 build (`libraries.tsv`) over the real 1.21.1 classes (Jade, GeckoLib... were being checked against their own 1.20.1 jars); `-Dgap.keepNativeLibs=true` restores that. `GAP1201_LIBS` takes several folders separated by `;`. Citadel events (`TriState` result) are bridged (`CitadelCompat`).
- Full ATM 9 run (506 jars) with the ATM 10 mods plus EMI, REI, Collective and Forge Config API Port as 1.21.1 libraries: **95 links / 20 obsolete / 391 unresolved**. Client-only mods (mods.toml client side, mostly `client` classes, or recipe viewer / minimap / overlay mods by name) are 26 of them (3 link, 3 obsolete, 20 unresolved); without them: 92 links / 17 obsolete / 371 unresolved. Client-only libraries (REI/EMI/JEI plugin APIs, Jade/WTHIT overlay APIs, JourneyMap) get no further work.
- Still open for content mods: Balm (`Balm.initialize` takes a runtime context, networking is payload based), Botarium (no 1.21.1 jar found on the machine), Refined Storage 1 API (RS 2 is another API), Resourceful Lib/Config networking, Create 6 block entity `read/write(CompoundTag, boolean)` and recipe params, GeckoLib `GeoBone` matrix API.
## 31. Status 2026-11-08: registration-time constructors, recipe holders, class stand-in keys

Fresh ATM 9 baseline after the other rounds merged (506 jars): 96 links, 13,932 unresolved pairs; after this round 13,736 (own re-run, `--out build/atm9_r4`). Generic changes:

- **`mc-classes.tsv` is keyed by the name after the Forge renames.** The mapper looked the table up only for names the Forge rename left unchanged, so a stand-in for a class that NeoForge also lacks (`StrictNBTIngredient`, `PartialNBTIngredient`) never applied; now `Mapper.map` consults the table with the renamed name.
- **Registration-time constructors** (a throw there takes the mod and the game down): `new BlockSetType(...)` (11 arguments), `new PaintingVariant(w, h)`, `new BannerPattern(hash)`, `Rarity.create(name, color)` (nearest vanilla rarity: enum constants cannot be added at runtime), `new Explosion(...)` (both old forms), `BowlFoodItem` (stand-in `LegacyBowlFoodItem`), `ComponentContents.EMPTY`, `ItemEntity.setThrower(UUID)`.
- **Recipe holders**: `RecipeManager.byType`, `CachedCheck.getRecipeFor`, `getRemainingItemsFor`, `RecipeCraftingHolder.setRecipeUsed`/`getRecipeUsed` convert between recipes and `RecipeHolder` (id found through `RecipeCompat.getId`, `legacyconnector:unknown` when the manager does not hold it).
- `isPathfindable`, `canSustainPlant` and `setTame` rows were written here and by the Minecraft middle round at the same time; the duplicates were dropped.

Not done: `AbstractIngredient` subclasses (the class `extends` a final class; a rewrite would have to turn every `new Sub(...)` into the vanilla `Ingredient` of a generated `ICustomIngredient` and every use of the subclass type into `Ingredient`, with an `IngredientType` and `MapCodec` per subclass), `MobBucketItem` with suppliers (vanilla keeps type/fluid/sound in final fields), `LootTableReference`, `CommandRuntimeException`, `Score`, `SkinManager`.

## 33. Status 2026-11-08: library round 3 (Create block entity NBT, library jar-in-jar order, what needs no bridge)

- Full ATM 9 run: links **95 to 108**, unresolved jars 391 to 377, non-client jars 105 links / 18 obsolete / 357 unresolved (client-only jars: 3 / 3 / 20). `Gap1201` indexes jar-in-jar libraries after all top-level target jars, so a mod's bundled old library copy no longer shadows the installed one.
- **Create 6 block entities**: `SmartBlockEntity.read/write(CompoundTag, boolean)` gained the registry provider; mod overrides get a bridge (`lib-bridges.tsv`), `super` calls and `writeSafe` get the provider from `MigrateContext` (`BridgeHelpers.providerFor`, `=args` rows incl. `KineticBlockEntity`/`GeneratingKineticBlockEntity`).
- **Balm and Resourceful Lib/Config need no bridge**: every ATM 9 jar that calls them (Waystones, Cooking/Farming for Blockheads, Crafting Tweaks, Inventory Essentials, Nether Portal Fix, Trash Slot; Ad Astra, Chipped, Handcrafted, Creeper/Enderman Overhaul, Tempad) has a native 1.21.1 build, which is the right answer; Balm networking is payload based and Balm 21 initialises with a platform load context, so a bridge would be large for no content mod that lacks a native build. Botarium (Ad Astra, Argonauts) was added to `libraries.tsv`.
- Open: Refined Storage 1 API (RS 2 is another API: for the side-by-side loading mechanism), `GeoModel.setCustomAnimations` in 9 jars (the descriptor exists in GeckoLib 4.9.3; the jars most likely bundle their own GeckoLib copy, which the probe indexes first), Create recipe params / `ProcessingRecipe`, Create `SimplePacketBase`/`AllPackets` networking, Citadel packets.
## 34. Status 2026-11-08: side-by-side libraries (several game versions of one library)

Libraries are not bridged one by one: a 1.20.1 library whose 1.21.1 build has another API (Citadel: Alex's Mobs 1.21 needs Citadel 2.7, Alex's Caves 1.0 for 1.20.1 needs 2.6) now runs next to the native build as an isolated unit (`SideBySide`, `SideBySidePlanner`; decision order in [Bridges](Bridges.md#library-decision-order), config `experimental.sideBySideLibraries`). In `Forge1201Transformer` the classes of the library's package roots are relocated by the `Mapper` before any table lookup (so the native-build tables never touch them), then string constants, annotation values (`@Mod`, `@Mod.EventBusSubscriber(modid)`, mixin targets), `ModList.isLoaded("<id>")`, the toml (`rewriteToml`), mixin config names/package/plugin, services files and access transformers follow. The fingerprint of the plan is part of the translation cache key. The same plan also isolates two legacy versions of one library (`dropDuplicateMods` keeps both and gives the older version's copy `<id>_l<version>`).

Evidence (server batch with native Citadel 2.7.1 for 1.21.1, Citadel 2.6.2 for 1.20.1 and Alex's Caves 2.0.2 from the ATM 9 folder): the mod list shows `Citadel 2.7.1 (citadel)` and `Citadel 2.6.2 (citadel_l1201)` next to `Alex's Caves (alexscaves)`; the module layer resolves, both mixin configs apply (the isolated one with priority 2000 and a renamed config; its injector handlers carry a `_l1201` suffix; the library's refmap file is dropped because the native config would find it by name), and `Citadel.<init>` of the relocated copy runs. Three problems found on the way are fixed generically: service files naming providers the jar does not contain (Citadel's `net.sourceforge.jaad...` made both modules export that package), a dependent's `MixinConfigs` manifest entry naming the library's config, and `KeyDispatchDataCodec.codec()` (Codec in 1.20.1, MapCodec in 1.21.1; `MiddleCompat.keyDispatchCodec`). `ForgeApi1201.busRegister` now registers the valid `@SubscribeEvent` methods one by one when an object has a method for the other bus (Citadel registers itself on the game bus with a `ModConfigEvent` handler). Open: the batch then stops at Citadel's `ServerProxy` (an `@EventBusSubscriber` class with an instance `@SubscribeEvent` method, which NeoForge rejects) - a plain translation gap of Citadel 2.6.2 that also exists without side-by-side - so Alex's Caves does not reach registration yet. Other limits: mixins of the isolated copy that inject into the same vanilla instruction as the native copy lose their injection (logged, not fatal); members merged into a vanilla class by name (`setGlobalTickLengthMs`) conflict with the native copy's and the later one is skipped; registry ids the library registers under its own namespace would clash with the native build's (none seen for Citadel).

## 35. Status 2026-11-08: crash-class constructors, enchantment/stem/armour stand-ins

Baseline of this loop (after the other agents merged): 34,303 unresolved pairs over 506 jars (34,127 without the 21 client-only jars the heuristic `kp_client.py` finds; the number grew because native libraries are no longer shadowed, see section 32). Work done: `mc-classes.tsv` entries are looked up after the Forge rename (StrictNBT/PartialNBT ingredients now map); constructor-time in-place `=args` rewrites that also cover `super(...)` calls (Explosion, projectiles, cooking/smithing recipes, `ServerGamePacketListenerImpl`, foliage placer types, mushroom/sand blocks); registration-time constructors (`BlockSetType`, `PaintingVariant`, `BannerPattern`, `Rarity.create`, trunk/foliage/decorator types, `LootPoolEntryType`, `SmithingTemplateItem`); stand-ins `LegacyProtectionEnchantment`/`LegacyDamageEnchantment`/`LegacyVanishable`/`LegacyMegaTreeGrower`/`LegacyDyeableArmorItem`/`LegacyStemGrownBlock`/`LegacyCommandRuntimeException`; `LootTableReference` to `NestedLootTable`; advancement enchantment predicates; saved data `get`/`save(File)`, text serializer leftovers, `PotionBrewing.addMix` (queued for `RegisterBrewingRecipesEvent`), `LootContext.Builder.create`, `SpawnPlacementType` check, `BowItem.customArrow` bridge, `isPathfindable` rows, `SaplingGrowTreeEvent` to `BlockGrowFeatureEvent`. Tooling: a scan of the translated jars (`SoftLink1201Runtime.fail/note` sites inside `<init>`, `<clinit>` and static lambdas) is how the constructor-time list was found; it showed 14,733 such sites in 506 jars, mostly Fabric-named classes of the .connector folder and client code. Open: `AbstractIngredient` subclasses, `ICustomPacket`, `FluidHandlerItemStack` (needs a data component type), `LootPoolSingletonContainer` subclasses, `Deserializers`/`LootDataId`, Aquaculture/ToolBelt client classes on a dedicated server (needs the server log).

## 36. Status 2026-11-08: Citadel side by side reaches registration; Alex's Caves, ingredient subclasses, #218

Server batch (native Citadel 2.7.1, Citadel 2.6.2 legacy, Alex's Caves 2.0.2; `run-subtest/mods` + `mods/1.20.1`): the isolated Citadel now loads, and Alex's Caves passes mod construction, registration and the data load (it stops in feature placement of the first chunk, see below). Fixes on the way, all generic:

- `@Mod.EventBusSubscriber` classes with **instance** `@SubscribeEvent` methods in their hierarchy (Citadel proxies): `Subscribers1201` removes the annotation and the `@Mod` constructor registers the static handlers (`ForgeApi1201.subscribeStatics`); Forge ignored the instance ones. A `DeferredRegister.register(bus)` of an isolated library is not attached (the native copy registers the same ids).
- **#218 (Fisk's Superheroes)**: a `@Mod` constructor taking `FMLJavaModLoadingContext` gets a public `(IEventBus, ModContainer)` constructor (`ModConstructors1201`); the original becomes package private.
- **`AbstractIngredient` subclasses**: `Ingredient` is opened by the access transformer (`public-f`), the facade `AbstractIngredient` extends it and hands NeoForge a `Bridge` (`ICustomIngredient`) that forwards to the subclass; the ingredient type (codec, network form) is made from the 1.20.1 `IIngredientSerializer` and registered by `CraftingHelper.register`. Not run in a game yet (no ATM 9 mod using it reached recipe loading).
- Soft link: a constructor whose **argument** class is missing now pairs its `new` as well (it left an uninitialized object and a VerifyError); `Datagen1201` no longer shells worldgen classes called `BlockStateProvider`; enum extension `create` calls (`Rarity`, `MobCategory`, `SpawnPlacementType`, `PathType`, `Raid.RaiderType`, `ItemDisplayContext`) map to the nearest vanilla constant or a lambda; `SimpleCookingSerializer`/`SimpleCraftingRecipeSerializer` factories (stand-in interfaces: the lambdas of the mod implemented the 1.20.1 method), `EntityDataSerializer.optional/simple/simpleEnum`, `EntityDimensions(float, float, boolean)`, `LootItemConditions.or/andConditions`, particle option codecs, `DECORATED_POT_PATTERNS`, `SmithingTemplateItem.createArmorTrimTemplate`.
- Data: int providers (`{"type": "minecraft:uniform", "value": {...}}` became flat in 1.20.2), biome `spawners` of a mod's own mob category, structure set exclusion zones naming a tag.

Open on this batch: the mixins of Alex's Caves with `@Shadow` members whose signatures changed (`LivingEntity.hasEffect(MobEffect)`, `Mob.dropCustomDeathLoot`, `AbstractMinecart.getHurtTime`, `Frog.setVariant`) do not apply, and the first chunk fails in feature placement (`MinecartChest has not defined synched data value 8`, the minecart mixin's data accessor). Real-libraries baseline of the ATM 9 probe (`GAP1201_LIBS` = the ATM 10 mods folder): 112 jars link, 22,609 unresolved pairs (22,486 without the 21 client-only jars).

## 37. Status 2026-11-08: entity data a mixin adds to a vanilla class; Alex's Caves server reaches "Done"

Server batch (native Citadel 2.7.1 + legacy Citadel 2.6.2 + Alex's Caves 2.0.2): the dedicated server now finishes starting ("Done"), with 352 blocks, 555 items and 83 entity types of Alex's Caves registered. The last blocker was `MinecartChest has not defined synched data value 8`: the mod's mixin reserves entity data ids on vanilla classes (`SynchedEntityData.defineId(Entity.class, ...)` in static fields) and defines them in a handler at the end of `Entity.<init>`; 1.21.1 collects the data in a builder before the entity exists and `build()` refuses ids nobody defined. `LegacyEntityData.defineId` (call row) records the ids reserved on `net.minecraft` classes, `SynchedEntityDataBuilderMixin` defines the missing ones with the default of their serializer at the start of `build()`, and `MiscCompat.define` called on an existing entity just sets the value. `Mixin1201.defineSynchedDataHandler` also moves an `@Inject` into `defineSynchedData()` to the builder signature (handler gets the builder, which is remembered for `define`). Open: a self test reports `FakePlayer has not defined synched data value 19` (a Player level id nobody defined: another mixin with a serializer without a default; players joining would hit it), the mixins with changed `@Shadow` members still fail as a whole (logged, skipped).

## 38. Status 2026-11-08: first content batch (12 mods) on the server; abstract events, wrong bus listeners, JiJ in META-INF/jars

Server batch of 14 ATM 9 content mods (Aquaculture, Croptopia, Cyclic, Farmers Delight, Tool Belt, Twigs, Artifacts, Comforts, Elevator ID, Iron Furnaces, Lootr, Naturalist, Tombstone, plus Architectury/Curios/GeckoLib/Cloth Config fallbacks): the failures at mod construction were, in order, each fixed generically: nested jars named in `META-INF/jarjar/metadata.json` but stored in `META-INF/jars/` (Artifacts: MixinExtras and Expandability) are translated or dropped like the others; classes the tables rename onto layer classes (Architectury `EventBuses`) are no longer also written into the jar (a package in two modules); `@EventBusSubscriber` classes whose handlers listen to an abstract event (ExplosionEvent, AdvancementEvent, PlayerEvent...) are registered by the main class for every concrete subclass (`ForgeApi1201.concreteEvents`; NeoForge refuses abstract events, Forge delivered all subclasses; Lootr, Iron Furnaces); `addListener` of an event of the other bus is ignored as in Forge (Artifacts registered `RegisterCapabilitiesEvent` on the game bus); subscriber classes whose handlers all take client events or `FMLClientSetupEvent` get `Dist.CLIENT` (Naturalist: reading the methods of the class loaded `ClientLevel` on the server, the Aquaculture/ToolBelt class of failure); `PlayerEvent.ItemPickupEvent` maps to `ItemEntityPickupEvent.Post`; the `NetworkEvent` payload events exist; a codec lambda rewrite now counts as a change (the class lost its stack limit and failed to verify); `ICustomPacket`, `FluidHandlerItemStack(ItemStack, int)` (a layer data component for the fluid), the drop rule for mixins whose `@Shadow` members do not exist in 1.21.1 (`Mixin1201.missingShadow`). Result: 12 of 14 construct. Left: Tombstone (`TombstoneModConfig extends ModConfig`, final in FML, which no access transformer reaches), Croptopia (EpheroLib uses `EventListenerHelper.getListenerList`/`ListenerList`), Cyclic (`BlockSimpleHopper` calls the now protected `getInteractionShape` on another block: a verify error; needs the methods widened or the call routed), Aquaculture reached registration only partly (not checked further). `FakePlayer has not defined synched data value 19` is still open: the id belongs to LivingEntity (vanilla has 8 there in 1.21.1), reserved by code that did not define it for players.

## 39. Status 2026-11-08: protected block methods, listener tables, FakePlayer data, batch rows

- The 12-mod batch of section 38 now constructs all but Tombstone (it subclasses `ModConfig` with its own `ConfigFileTypeHandler`: FML internals, not translatable) and registers up to a failure inside Architectury's fallback copy (`arch$registryName` is added to `EntityType` by a mixin of the 1.20.1 copy; with the native Architectury installed the call does not exist). Croptopia/EpheroLib: `EventListenerHelper`/`ListenerList`/`IEventListener` facades (detached, empty tables). Cyclic: calls of the now protected `BlockBehaviour` shape methods on another block (`Blocks.HOPPER.getInteractionShape(...)`) go through `BlockCalls1201` (method handles, same virtual dispatch); call rows ending in `@virtual` leave `super` calls alone. A broad access transformer for these methods cannot be used: NeoForge recompiles Minecraft with it and the vanilla overrides must be widened too.
- `FakePlayer has not defined synched data value 19` fixed: a mixin on `SynchedEntityData.defineId` records every accessor handed out, whoever asks (a native mod's id had no definition for players), and `build()` defines holes with the serializer default. The server self test of the Alex's Caves + Citadel batch passes (Done, 6 of 6 silent-failure checks). A real client join was not tried.
- `SynchedEntityData.hasItem` (gone) is answered through `get`. Batch results are recorded with `tools/compat/from_client.py` (18 mods rated "loads": the run stopped at registration, no self test).

## 40. Status 2026-11-08: Architectury members, animal ingredients, regression check of Terramity (server)

- Terramity (regression check after today's changes), server: 207 blocks, 754 items, 81 entity types, 19 block entity types, "Done", 6 of 6 silent-failure checks: unchanged from section 17. The client join and a screenshot were not taken (a client of another run held the slot).
- Batch of 13 content mods without a native Architectury (Artifacts needs it and was left out): construction passes for all; 750 blocks, 1485 items, 99 entity types, 121 block entity types register. Fixed on the way: Architectury 9's interface-injected members (`arch$registryName`, `arch$getFluid`, `arch$tab`) answered from the registries; removed animal ingredient and food fields (`Chicken.FOOD_ITEMS`, `Cat.TEMPT_INGREDIENT`, `Pig`, `AbstractHorse`, `Strider`, `Parrot.TAME_FOOD`); `ItemStack.areCapsCompatible` takes any object; `SleepingLocationCheckEvent` posted from `CanContinueSleepingEvent`; `SimpleChannel.MessageBuilder.ToBooleanBiFunction`; `Capability.addListener`; `DataResult` static calls as interface calls (Datafixer Upper 8); a lambda over a captured codec for a `MapCodec` type interface (`() -> codec` in Farmers Delight placement modifiers).
- Open: a `LootTableLoadEvent` handler of one of the mods asks for the enchantment registry while the server registries are not there yet (`Missing registry: minecraft:enchantment`), which stops the load; the fallback Architectury 9 copy registers a biome modifier with a plain codec; Tombstone (`ModConfig` subclass with its own `ConfigFileTypeHandler`) is not translatable.

## 41. Status 2026-11-08: batch 2, data shapes and reflection on changed fields

Batch of 13 content mods (no native Architectury): the server now loads past registration, the loot table load and most recipes, and stops at the advancement load: Lootr's `GenericTrigger` implements `CriterionTrigger` directly (the `SimpleCriterionTrigger` stand-in covers subclasses only; a direct implementer needs `codec()`). Fixed on the way: loot table handlers read enchantments through the registries of the reload (`MigrateContext.remember` from a `LootTableLoadEvent` listener; the lookups use the provider, not a `RegistryAccess`); `Field.get`/`set` in mod code hand out the three `LootPool` lists as arrays and take arrays back (`ForgeApi1201.fieldGet/fieldSet`, the idiom of mods that add loot to tables); `MinMaxBounds.Doubles/Ints.fromJson`; loot `block_state_property` values given as booleans become strings; tag files with `"replace": "false"` (a string); an ingredient class that extends `Ingredient` directly gets the constructor opened (access transformer) and a `Bridge` made by its 1.20.1 serializer. Not fixed: the fallback Architectury 9 biome modifier registers a plain `Codec` through a path other than `DeferredRegister` (use the native Architectury); the Terramity client join/screenshot regression check (a client of another worktree, PID 35668, held the only slot; it was started 9 hours earlier by a different checkout, so it was left alone).

## 42. Status 2026-11-08: direct CriterionTrigger implementers, batch 3 reaches Done, Terramity client check

- A class that implements `CriterionTrigger` itself (Lootr GenericTrigger, no `SimpleCriterionTrigger` base) gets a generated `codec()` (`CritCompat.directCodec`): it reads the conditions as JSON and calls the mod createInstance(JsonObject, DeserializationContext).
- Batch of 16 content mods (Aquaculture, Croptopia, Cyclic, Farmers Delight, ToolBelt, Twigs, Comforts, Elevator, Iron Furnaces, Lootr, Naturalist and dependencies), server: Done; rows recorded as loads with from_client.py.
- Remaining errors of that run: recipes naming modded items missing from the registry (aquaculture hooks), forge: tags that became c: tags, Curios API missing (no native Curios in the test folder), naturalist sound ids.
- Real client regression check (Terramity with Patchouli, Caelus, Curios, GeckoLib): joins a superflat world, held legacy item renders (docs/screenshots/1.20.1/terramity-join-regression.jpg).

## 43. Status 2026-11-08: conditions, empty subscriber classes, tag spellings

- A class annotated with an event bus subscriber but without handlers is loaded for its static initializer (Forge did; Aquaculture Hooks registers its hook items there): Subscribers1201 now replaces the annotation by a load of the class. Aquaculture: 123 items, hook recipes parse.
- Recipe condition serializers (CraftingHelper.register) are registered as codecs in neoforge:condition_codecs, also late (common setup, like Farmers Delight); condition classes get a generated codec(); IConditionSerializer is bounded by ICondition like Forge so mod bridge methods match.
- Convention tag spelling: tools/shields to c:tools/shield (also bows, crossbows, fishing rods, tridents, shears).
- Silent failure check: a sound registered under one key naming another sound of its own namespace passes (Naturalist). Batch of 16 mods: Done, 6 of 6 checks; remaining recipe errors are mod data bugs (a recipe with type forge:mod_loaded, a settings file in the recipes folder).

## 44. Status 2026-11-08: empty tags, own-jar mixins, batch 4 reaches Done

- Unknown c: tag references (recipes, loot tables, advancements, tag files) get an empty tag file generated by the data converter (NeoForge list in c-tags.txt, taken from the jar); tools/spear, shield etc. checked against that list.
- Mixin shadow/accessor drop rules also fire for @Accessor of a field whose type changed (Croptopia LootPool.entries) and ignore JDK interfaces above the target (curios ApplyBonusCount).
- mods.toml with both mandatory and type in a dependency no longer gets a second type (Tectonic: TOML error).
- Sapling growers are asked for their feature key when a tree grows (TreeGrowerLegacyMixin), not at registration (Deeper and Darker read unbound holders in a static initializer).
- A mod mixin config may apply to a class of its own jar (Polymorph sets its API services through a mixin on its own class); IConfigSpec.self(), ItemParser.parseForItem/ItemResult.nbt added.
- Batch of 12 mods (Deeper and Darker, Dungeons and Taverns, Polymorph, Tom storage, Traveler backpack, another furniture, Terralith, AttributeFix, Natures Compass, Carry On, global packs, Veinminer): Done, 6 of 6 checks, rows recorded. Open: Traveler backpack RecipeUtils static init (45 recipes), capability registration of two mods, two Deeper and Darker mixins. Tectonic, Towns and Towers and Cataclysm need libraries not in the test folder.

## 45. Status 2026-11-13: ATM 9 gap round (links 116 to 136), dependency findings, JourneyMap Integration

- **Measurement.** `python tools/compat/atm9_run.py <ATM 9 mods> --no-native` with `GAP1201_LIBS=<ATM10 mods folder>` (the 1.21.1 builds of Jade, JEI, Create, GeckoLib, Curios, Architectury...): before 116 links / 21 obsolete / 369 unresolved (273 need layer support, 93 miss another mod's API, 3 a JDK/library class); after **136 links / 21 obsolete / 349 unresolved** (252 / 94 / 3). Without `GAP1201_LIBS` the same set reads 99 links, because every reference into JEI, Jade, Create, GeckoLib... is then "missing".
- **What the "missing mod" class is.** 133 of the 369 unresolved jars reference a class that exists in no jar of the ATM 9 or ATM 10 folder. The big ones are all libraries that are already in `libraries.tsv` and have a native 1.21.1 build, but whose 1.21.1 jar the offline run does not have: The One Probe API (38 jars), REI (31), EMI (28), CraftTweaker (12), Game Stages (8), WTHIT (7). Those are probe artifacts: in a game the native build is installed. Put their 1.21.1 jars into the `GAP1201_LIBS` folder (or pass `GAP1201_ASSUME_NATIVE`) to count them. Absent optional integrations (TFC, Agricraft, Better Combat, PMMO, Vivecraft, Xaero...) are not gaps either. The rest is real API drift of mods the pack has (Create 17 jars, GeckoLib 15, Farmer's Delight 10 (`ModEffects.COMFORT/NOURISHMENT` are `Holder`s now, `ModCreativeTabs` suppliers), Balm 8, Resourceful Lib 8, Botarium 5, Refined Storage 1 API 5, Titanium 5, Cyclops 4).
- **Libraries worth listing next (native 1.21.1 build, same mod id, not in `libraries.tsv`):** CC: Tweaked (`computercraft`, 3 jars), Titanium (`titanium`, 5), Cyclops Core/Common Capabilities (`cyclopscore`, 4), Diet (`diet`, 2), Configured (`configured`, 2), Xaero's minimap (client). Not added without a check of the API drift of their dependents.
- **JourneyMap Integration (jmi) regressed from "links" to unresolved on 2026-11-08** with the commit that put JourneyMap into `libraries.tsv`: from then on the 1.20.1 JourneyMap jar is no longer indexed (its copy is replaced by the native build), and JourneyMap 6 (1.21.1) moved its API from `journeymap.client.api.*` to `journeymap.api.*` (and rebuilt it; the 1.21.1 jar holds no `journeymap/client/api` class at all), so the 116 API classes jmi references are gone. Not a dependency-resolution bug. A fix would need the 1.20.1 API package kept (side by side) or a bridge from the 5.x API to the 6.x one; it is a client-only integration, so not done.
- **Mod events.** Calls on events a mod defines itself (and on the Forge event facades) now reach the `@event.` cancel/result rules (`ForgeApiRewrites1201.apply(node, modEvents)`); a mod event annotated `@Cancelable` implements NeoForge's `ICancellableEvent`, so `setCanceled` is honoured by the bus.
- **Signature rows** (`GapCompat`, `mc-calls.tsv`, `mc-bridges.tsv`, `mc-classes.tsv`, `mc-members.tsv`): the crafting grid helper `RoundCompat.craftInput(CraftingContainer)` (26 jars named a descriptor the helper lacked), `MobEffect.applyEffectTick` returning a boolean (override bridge with `const:true` result adapter, call row), `ArrowItem.isInfinite` (Player to LivingEntity), `Block.getExpDrop` (override bridge: random, fortune and silk touch from the tool), `BlockState/Block.isValidSpawn`, `BlockState.neighborChanged` (renamed `handleNeighborChanged`), `BlockSourceImpl`/`PositionImpl` (a `BlockSource` record and `Vec3`), `StructureManager.getStructureWithPieceAt(pos, key)`, loot table ids of brushable blocks and minecarts, `SpawnData`, the jigsaw piece constructor, `Player.triggerRecipeCrafted`, the forget-chunk packet, `DamageSource.isIndirect`, `SimpleContainer.createTag/fromTag`, `BaseContainerBlockEntity.setCustomName` (through the `custom_name` component), `AreaEffectCloud` potions, `EnchantmentHelper.getRandomItemWith`, `CropBlock.getGrowthSpeed(Block, ...)`, packets that moved to `protocol.common` (1.20.2), the `FinishedRecipe` stand-in members a data generator read, `ForgeRegistries.Keys.COMMAND_ARGUMENT_TYPES`, and Forge's item pickup / sleeping time / sapling growth hooks (`ForgeApiTail1201`).
- **Tooling.** `GAP1201_TRANSLATED_DIR=<dir>` (probe.py) keeps the translated jars of a gap run for inspection.
- **Left, by value (jars blocked, weighted by how few other symbols the jar has):** `Checkbox.<init>` as a super constructor of a mod's own subclass (SecurityCraft, elevatorid, observable: needs a class stand-in), `HorseArmorItem` (stand-in needs a registered armor material holder), Farmer's Delight `ModEffects` holders (7), `Recipe.getId` still reported for polymorph/Paraglider although the call is rewritten (cause not found), `Player.findRespawnPositionAndUseSpawnBlock` (6), Refined Storage 1 `appendHoverText` (cross-jar override with the old tooltip signature, 3 jars), Forge `NetworkDirection.buildPacket` / `ICustomPacket.getThis` / `eventNetworkChannel` (8), `AdvancementList` (10), `GeoRenderer.reRender` / `GeoModel.setCustomAnimations` (GeckoLib 4.x, 8), `ModLoadingException.<init>` (2).

## 46. Status 2026-11-13: mixin target proof, handler re-signing, per-injection degradation

`Mixin1201` used to rename strings and drop `require`; an injection whose 1.21.1 target gained a parameter then failed with "Invalid descriptor ... Expected (...)"
(issue #250: `ItemStack.getTooltipLines` gained `Item.TooltipContext`), and one whose target is gone cost the whole mixin. The new package
`loader/.../transform/mixin1201/` (algorithm after Retromod's `AutomaticMixinTranslator`, MIT, see `THIRD_PARTY_NOTICES.md`) proves every injector against the
member index the gap report uses (`mc-members.index.gz` + `mc-hierarchy.tsv.gz`, plus `mc-static.index.gz` for static methods; `McMembers1201Tool` writes all three):

- **Exact**: the method exists as written, or (name only) the handler's captured parameters are a prefix of one overload. Untouched.
- **Signature changed, parameters added**: the old parameters (the selector's descriptor, else the refmap's, else the handler's captured list) reappear in order in
  exactly one overload, same return type, at most three added. The selector gets the new descriptor, the `@Inject` handler gets the added parameters (ignored,
  `HandlerResigner`: descriptor, local slots, parameter annotations, `@Local(index)`), `@At(target)` of a call that gained parameters follows. A handler whose `@Local`
  capture is of the added type, a static target of an instance handler, ambiguous alignments, changed return types and removed or retyped parameters are not repaired.
- **Gone** (method or class missing, proven by a complete indexed class chain) or **not repairable**: only that injection is removed (the selector from its annotation,
  the annotation from its handler, the handler from the class) with one report line `Mixin <class>.<handler>: injection into <selector> dropped: <reason>`; the mixin
  class and the mod stay. SRG/intermediary names no table moved and lambdas are never judged.

`./gradlew` offline measurement: `Mixin1201Survey <report> <jars>` (class `transform/Mixin1201Survey`, no game).

## 47. Status 2026-11-13 (second round): cross-jar hierarchy, networking that delivers, stand-ins

- **Measurement** (`atm9_run.py ... --no-native`, `GAP1201_LIBS` = ATM 10 mods folder): before **137 links / 21 obsolete / 348 unresolved** (run line: 242 gap_shim, 94 gap_mod, 3 gap_lib); after **155 links / 22 obsolete / 329 unresolved** (231 / 87 / 2). One jar that had linked only through a probe artifact (a GeckoLib/Curios copy shaded as jar-in-jar by another mod, which the game never uses) moved to a real Curios row (`SlotContext.getWearer`, fixed).
- **Cross-jar hierarchy** (`Mc1201Hierarchy.peers`, `Forge1201Transformer.scan`): the other 1.20.1 jars of the set tell a jar their supertypes and the declared methods that migration tables name. A call through a library class of another jar (`super.appendHoverText(...)` via Refined Storage `BaseBlockItem`, mcjty `BaseItem`, Cyclops `ItemBlockNBT`, `super.invalidateCaps()` via Mantle/ShetiPhian base block entities) now reaches the rules of Minecraft's classes; a method a peer declares itself keeps its old signature (its own translation keeps it). Jars replaced by a native library are not peers. When the chain leaves the set (an absent or native library), `@special` super-call rules with a static helper apply by name and descriptor.
- **Interface method handles** (`Recipe::getId`, `Collectors.toMap(Recipe::getId, ...)`): `H_INVOKEINTERFACE` handles were never redirected, so rules for interface methods only covered direct calls (polymorph, Paraglider, and the `Recipe.getId` rows of Botania, Create, IE...).
- **Stand-ins and helpers**: `LegacyCheckbox` (constructors, subclasses, `setTooltipDelay`), `LegacyHorseArmorItem` / `LegacyDyeableHorseArmorItem` (a keyed material holder, texture name as before), `LegacyAdvancementList` (+ `AdvancementCompat`: `ClientAdvancements.getAdvancements()`, `AdvancementHolder.getDisplay/getParent/getCriteria/getChatComponent`), `PredicateCompat` (`new ItemPredicate()` and the eight-argument constructor, `NbtPredicate.ANY`, `StatePropertiesPredicate.ANY`, `EnchantmentPredicate.NONE`), `Round2Compat` (`Player.findRespawnPositionAndUseSpawnBlock`, `UUIDTypeAdapter`, `Screen.renderDirtBackground`, `SharedConstants.filterText`). `Advancement$Builder` keeps its name (an identity row; it was derived as `AdvancementHolder$Builder`, which does not exist).
- **Networking**: `EventNetworkChannel` is a real channel: one payload type per channel name carrying the raw bytes both ways, posting `ServerCustomPayloadEvent` / `ClientCustomPayloadEvent` (payload buffer, `Context`) to the channel's own bus on the main thread; `NetworkDirection.buildPacket(Pair, name).getThis()` and `SimpleChannel.encodeMessage` wrap mod-encoded bytes for any registered channel (`RawChannels`). Not run in a game: CoFH Core (#271) is the check (client and server both send and receive a packet).
- **Farmer's Delight** is now a native library (its rows existed; `ModEffects.COMFORT/NOURISHMENT` `Holder`s are converted by the generated `lib-fields.tsv` rows). The general rule for a dependency's static field that became a `Holder` is `tools/dev/gen_lib_fields_1201.py` over `libApiDiff`; run it per library.
- **Libraries measured and not added**: CC: Tweaked (17 dependents), Titanium (4), Cyclops Core + Common Capabilities (7): with their native builds in the target set none of the dependents changed from unresolved to linking (CC: Tweaked even lost one, `Capabilities`), so they stay translated. Diet and Configured have no jar in either folder (nothing to measure).
- **Not a layer gap**: `integrated_api` in the ATM 9 folder is the Fabric jar (`-fabric`, intermediary names), so `ResourcefulRegistries.create(Registry, String)` of the Forge integrated_* mods cannot resolve against it.
- **Left, by value**: `GeoModel.setCustomAnimations` / `GeoRenderer.reRender` (the remaining jars have a mod class of their own in the chain), `Recipe.getId` on classes of native libraries (Farmer's Delight recipes), `MeleeAttackGoal.getAttackReachSqr`, `Holder.get()` receivers, `BlockModel.bake` (15), `EntityRenderersEvent$AddLayers.getRenderer` (13), `QuadBakingVertexConsumer.<init>` (12), `ItemPredicate.<init>()V` for the remaining receivers (6), the REI/EMI/TOP/CraftTweaker APIs (probe artifacts: add their 1.21.1 jars to `GAP1201_LIBS`).
