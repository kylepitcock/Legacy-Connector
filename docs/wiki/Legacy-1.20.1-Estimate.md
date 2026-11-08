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
