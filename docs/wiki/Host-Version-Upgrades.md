# Host version upgrades (NeoForge / Minecraft 1.21.2 and later)

The layer targets Minecraft 1.21.1 / NeoForge 21.1 (`gradle.properties`: `minecraft_version=1.21.1`, `neo_version=21.1.258`). The roadmap asks that it
can follow newer hosts (1.21.4+). This page is the offline inventory (2026-11-22, no game run): where the code depends on the host, what changed in
later versions, what a bump costs, and which isolation steps are already done. The change list is written from knowledge of the NeoForge/Mojang
migration primers for 1.21.2, 1.21.3, 1.21.4 and later; verify each item against the primer of the target version when the bump starts. The counts
are grep counts over `src/main/java` (5553 Java files, of which about 5200 are shim classes in the legacy packages, which mostly do not touch
modern classes).

## Where the layer touches the host

Modern-class imports (`net.minecraft`, `net.neoforged`, `com.mojang`) per package, the real exposure:

| Area | Files | Modern imports | Notes |
|---|---|---|---|
| `org/.../v1_20_1/migrate` | 138 | 1044 | In-place remap onto 1.21.1 names: the whole `Mc1201Migration` table and `CallRewrites1201` are written against 1.21.1 |
| `org/.../v1_7_10/bridge` | 84 | 452 | Block, item, entity, tile, recipe, loot, world gen bridges |
| `legacyconnector/v1_20_1` + `v1_7_10` + `v1_12_2` shim | ~3800 | ~940 | Shim classes that wrap a modern object (`mcc$modern`) |
| `org/.../*/client` (1.7.10, 1.12.2, Beta) | 159 | 720 | GL emulation, ISBRH/TESR, GUI, models: biggest client exposure |
| `org/.../v1_12_2/bridge` | 69 | 195 | |
| `org/.../core/worldgen` | 18 | 155 | Owned by other workers while this page was written |
| `org/.../*/adapter`, `entity`, `dimension` | ~80 | ~500 | Block/item/entity hosts, dimension types |
| `org/.../api/internal`, `core/pack` | 22 | 73 | Capabilities adapters, generated resource/data pack |
| Mixins (`org/.../mixin`, 144 classes, 2 mixin configs) | 144 | n/a | Target vanilla classes by name; 94 access-transformer lines |
| Loader (FML service, 212 files) | 105 refer to MC class names | n/a | Mostly strings (remap tables); the FML service itself is small |

Frequency of the APIs that changed (matches / files): `ResourceLocation` 4323/773; `BlockState` 5324/577; `PoseStack`/`RenderType`/`VertexConsumer` 1750/280;
`CompoundTag` 1086/228; `Level` 1722/422; `Sounds` 1598/180; `Enchantment` 1246/132; `EntityRenderer` family 730/177; `LootTable` 538/120;
`Capabilities` 532/121; `BuiltInRegistries` 532/188; `@SubscribeEvent` 505/212; `Biome/feature/structure` 585/121; `Recipe*` 347/68; `DataComponents` 281/62;
`Tier/ArmorMaterial/ArmorItem` 385/46; `Item.Properties` 38/16; `BlockBehaviour` 49/15.

## What changed in later hosts, by area

1. **1.21.2 to 1.21.4 item and block construction.** `Item.Properties` and `BlockBehaviour.Properties` need `setId(ResourceKey)` before the constructor
   runs (registering without it crashes); item models are data driven (`assets/<ns>/items/*.json` "item model definitions", `ItemModel`, `ItemStackRenderState`
   instead of `BakedModel` lookup by item); `Item.use` family returns `InteractionResult` (the `InteractionResultHolder` variants are gone), many
   `ItemStack` hooks moved to data components (food, tool, armor, weapon, `consumable`, `use_remainder`, `equippable`).
2. **Recipe system rewrite (1.21.2).** `RecipeInput`/`CraftingInput` already exist in 1.21.1, but recipes are registered as `RecipeHolder` keyed by `ResourceKey<Recipe<?>>`,
   `Recipe.placementInfo()`/`RecipeDisplay`/`SlotDisplay` replace `getIngredients`/`getResultItem`, ingredients become `Ingredient` backed by `HolderSet` (no `ItemStack[] getItems` use),
   `RecipeManager` is split into a server `RecipeMap` and a client `RecipeBook` synced as displays; recipes are no longer all synced to the client.
3. **Entity rendering (1.21.2).** Render state objects: `EntityRenderer<T, S extends EntityRenderState>` with `createRenderState`/`extractRenderState`; layers and models take the state, not the
   entity; `LivingEntityRenderer`, `RenderLayer` generics change; render events (`RenderLivingEvent`) change to state events. 730 matches in 177 files, and the legacy `RenderContext` replays
   old `doRender(entity, x, y, z)` calls so every adaptation lives in a handful of hosts.
4. **Registry access.** `Registry.get(ResourceLocation)` returns `Optional<Holder.Reference<T>>`; `getValue`/`getOptional` take over the old semantics; `Holder`/`HolderLookup` access is split. Equipment and
   armor: `ArmorMaterial` becomes a record with `EquipmentAsset` keys, `ArmorItem` is gone for `Item.Properties.humanoidArmor` (1.21.2/1.21.3) and equippable data components; tool tiers via `ToolMaterial` (1.21.2)
   instead of `Tier` and `DiggerItem`; `Enchantment` is fully data driven since 1.21 already (the layer is on that side).
5. **Entities and living behaviour (1.21.2 to 1.21.5).** `Entity.hurt` becomes `hurtServer(ServerLevel, ...)`; `Level` is passed explicitly to many server calls (`ServerLevel` required), `EntityType.Builder.build(ResourceKey)`,
   `spawn` logic and `SpawnPlacements` take `ResourceKey`; `Mob`/AI goals change little; `Attributes` and `MobEffect` take `Holder`s already.
6. **Block state, chunks, world gen.** `BlockBehaviour` methods that take `Level` take `Level`/`ScheduledTickAccess` (1.21.2 tick scheduling split, `updateShape` signature); `LevelChunk`/`ChunkAccess` mostly stable;
   structure and feature registries stay data driven; `ChunkGenerator` and `WorldGenRegion` signatures drift. Light engine fields (`LevelLightEngine`) are unchanged for the most part.
7. **NeoForge events, capabilities, networking.** NeoForge 21.2+ renames/moves events (`PlayerInteractEvent` hierarchy stable, `LivingHurtEvent` to `LivingDamageEvent.Pre/Post` was 1.21.1 already);
   capabilities (`RegisterCapabilitiesEvent`, `Capabilities.ItemHandler.BLOCK`) are stable across 1.21.x but `IItemHandler` moves to `ResourceHandler<ItemResource>`/transfer API in 1.21.9+ (NeoForge 21.9 transfer rewrite);
   `StreamCodec` payload registration is stable; `ResourceLocation` is renamed `Identifier` in 1.21.11 (Mojang names), mechanical but touches ~770 files.
8. **Rendering (1.21.5+ and 26.x).** `RenderType`/`RenderSystem` pipeline objects (`RenderPipeline`, `GpuDevice`) replace direct GL state starting 1.21.5/1.21.6; `PoseStack` use is stable; `BufferBuilder` needs a `ByteBufferBuilder` since 1.21.0
   (already done). The GL emulation (`RenderContext`, `LegacyGlState`, `DisplayLists`) is the part that cannot be ported mechanically.
9. **Loader and mixins.** FML 4 service interfaces are stable across 21.x; NeoForge changes the `mods.toml` fields rarely; mixin targets and `@Shadow` names change with every Mojang refactor and are
   the cheapest to find (the `MixinSelfCheckHook` fails loudly) but the longest tail.
10. **1.20.1 in-place remap.** `Mc1201Migration`, `Mc1201Tables`, `CallRewrites1201` map Forge 47 names onto 1.21.1 names: every host bump changes the right-hand side of those tables.

## Effort estimate, ranked by risk

| # | Area | Rework | Risk | Estimate |
|---|---|---|---|---|
| 1 | Client rendering: entity render states, item model definitions, GL emulation on the new render pipeline | `org/*/client` (159 files, 720 imports), `IsbrhBlockModel`, `LegacyBlockEntityRenderer`, `LegacyItemRenderer`, `Models1122`, `Assets1710/1122` (item model JSON), `GeneratedPack` | Highest: behaviour, not just names; only a real client shows regressions | 3 to 5 weeks |
| 2 | Recipe rewrite: legacy recipes as modern `Recipe`s, oreDict ingredients, NEI/JEI mapping | 68 files, 347 matches (`recipe/`, `RecipeBridge*`, `LegacyCraftingRecipe`, `JsonRecipes1122`, `RecipeSyncGuard`, generated data pack) | High: server-client sync model changes | 1.5 to 2.5 weeks |
| 3 | Item/block/entity construction and hosts: setId requirement, data component items, `Item.use` return types, `hurtServer`, tier and armor materials | bridges + adapters + hosts, ~120 files, `Item.Properties` 38, `BlockBehaviour.Properties` 49, `Tier/Armor` 385 matches | High but mechanical; the legacy-id model keeps it in few classes | 2 to 3 weeks |
| 4 | 1.20.1 in-place remap tables and 1.20.1 shim (`v1_20_1/migrate` 138 files) | Re-point every name and signature in `Mc1201Migration`, `CallRewrites1201`, `RoundNCompat`; probe with `gap1201` | High: the biggest single user of 1.21.1 names (1044 imports) | 2 to 3 weeks |
| 5 | Mixins and access transformers (144 mixins, 94 AT lines) | Retarget by hand, remove those whose vanilla code changed; `Mixin-Audit.md` lists them | Medium: loud failures at startup, many small fixes | 1.5 to 2 weeks |
| 6 | Registry access, ids, events, capabilities (`Identifier` rename, `Optional<Holder>` registries, NeoForge event moves, transfer API) | `BuiltInRegistries` 532/188, `ResourceLocation` 4323/773 (mechanical sed), events 505/212, capabilities 532/121 | Medium: compile-time errors, cheap after the host adapters | 1 to 1.5 weeks (name changes) plus 2 weeks if the item transfer API replaces `IItemHandler` |
| 7 | World gen, dimensions, structures (`core/worldgen`, `dimension`, village pieces) | 18 + ~12 files; `ChunkGenerator`/`WorldGenRegion` signature drift | Medium | 1 week |
| 8 | Loader (FML service, tables) | Small; strings in 105 files hold MC class names for the 1.20.1 and Forge remap tables | Low | 2 to 3 days |

Total for a first bump to 1.21.4: about 10 to 14 weeks of one engineer at the measured pace, most of it client rendering, recipes and the 1.20.1 tables. The shim itself (about 5200 classes of legacy
API) is mostly independent of the host and is not in the estimate.

## Isolation steps

Done on 1.21.1 without behaviour change (gate: compile, shim index, `check`: ok):

- **`core/host/HostRegistries.get(Registry, ResourceLocation|ResourceKey)`**: all 37 `BuiltInRegistries.X.get(id)` call sites under `org/` now go through it. On 1.21.2+ `Registry.get` returns an `Optional<Holder.Reference>` and
  the value accessor is `getValue`; this is now a one-class change (the 11 shim call sites in `legacyconnector/...` still call the registry directly and are listed below).
- **`core/host/HostProperties.item()` / `block()`**: all 8 `new Item.Properties()` and 4 `BlockBehaviour.Properties.of()` sites under `org/` (item/block bridges of 1.7.10, 1.12.2 and Beta, spawn eggs, fluid buckets, entity hosts)
  create the builders through it, so the 1.21.2 `setId(ResourceKey)` requirement is one parameter added to one method plus the callers that know the id. Sites that receive a `Properties` from the mod
  (`v1_20_1/migrate` `ItemProperties`/`ofFullCopy`) are untouched: they pass the mod's builder through.

Proposed next, in payoff order, each a pure refactor on 1.21.1:

1. Route the 11 shim sites (`legacyconnector/v1_12_2` `BlockSapling`, `ItemBed`, `SoundEvent`, `StructureVillagePieces`; `v1_20_1` registry wrappers) through `HostRegistries` (shim code may call it, the access widener index is unaffected).
2. A `HostResourceLocations` (or reuse `Ids`) as the only place calling `ResourceLocation.fromNamespaceAndPath/parse/withDefaultNamespace` (458 call sites; the `Identifier` rename would otherwise be a 770-file sed). Cheaper alternative: do the sed when the bump starts, as it is mechanical.
3. A `HostRecipes` adapter for `RecipeManager`, `RecipeHolder` creation and ingredient reading (347 matches): all legacy recipes already flow through `RecipeBridge`, `RecipeBridge1122`, `LegacyCraftingRecipe`.
4. A `HostRenderers` facade for the entity renderer registration (`EntityRenderersEvent`) and the `EntityRenderer` subclass the legacy `Render` classes sit on, so the render state split is confined to one renderer class per host.
5. A `HostTools` adapter for `Tier`/`ArmorMaterial` creation (385 matches; `ArmorCompat`, `MiscCompat`, `ToolMaterial` shims).
6. A host version constant (`HostVersion.AT_LEAST_1_21_2`) read from `FMLLoader`/`SharedConstants` so one jar can carry both code paths while the bump is in progress, and a per-host source set (`src/host/1.21.1`, `src/host/1.21.4`) for the `core/host` classes.

Keep the existing rule: nothing outside `core/host` should be added that calls these APIs directly; new code uses the adapters.
