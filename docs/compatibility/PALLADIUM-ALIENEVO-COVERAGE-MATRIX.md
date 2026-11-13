# Palladium 4.5.10 + Alien Evolution Compatibility Coverage Matrix

**Audit date:** 2026-11-12. **Target:** Minecraft 1.21.1 / NeoForge 21.1.258 / Java 21.
**Starting live branch HEAD:** `43ced3558d145f58bc8b4a20d21c46a88c757dba`; newer work is recorded in the continuation handoff.
**Validation boundary:** this original-archive/static audit does not claim current transformed artifacts compile, start, register all powers, or execute in game. No Gradle/JUnit build, bundle install, or gameplay test was run for this batch.

## Exact archives

| Archive | SHA-256 | Entries including directories | Files | Classes | JSON |
|---|---|---:|---:|---:|---:|
| AlienEvo filename artifact (internal metadata says 1.1.2) | `115a9b6526c8ea93ed22dda90e171a3a0bb88b1a3af69667224aea75df22dae0` | 3285 | 2914 | 34 | 799 |
| Palladium Forge 4.5.10 | `c0f7541f906e6717ca360cc0da8b13a4c53ab77c71d33c460c5a9d688e041ac7` | 1827 | 1642 | 1068 | 416 |

Palladium JarJar selects PalladiumCore Forge `1.0.1+1.20.1`, MixinExtras Forge `0.2.0`, and Player Animator Forge `1.0.2-rc1+1.20`. AlienEvo selects MixinExtras Forge `0.5.0`. The AlienEvo filename/internal version mismatch is preserved as an intentional developer discrepancy.

The audited target profile had KubeJS NeoForge `2101.7.2-build.381`, Rhino `2101.2.8-build.91`, GeckoLib `4.9.3`, Player Animator `2.0.4+1.21.1`, Architectury `13.0.11`, and Pehkui `3.8.3+1.21-neoforge`. Curios, JEI, Iris/Oculus, and AttributesLib were absent in that profile snapshot. Presence is not feature validation.

## Complete original-resource inventory

| Area | Count | Static classification |
|---|---:|---|
| AlienEvo JSON | 799 (200 data, 508 asset) | All JSON parsed: True. Data JSON/model JSON use `DataConverter1201`; other JSON remains preserved. |
| AlienEvo JavaScript | 126 | 70 `addon/alienevo`, 22 `assets/alienevo`, 21 `data/alienevo`, 7 `assets/afomni`, 6 `addon/afomni`; callback execution remains a runtime gate. |
| AlienEvo functions | 223 `.mcfunction` | All paths are visited by `FunctionConverter1201`; known syntax findings are below. |
| AlienEvo embedded power commands | 1,114 nonempty strings | Extracted from command-bearing Palladium power fields and passed through the same function-command converter via data conversion. Static conversion is not execution proof. |
| AlienEvo structure NBT | 14 gzip templates | Two files have inspected Forge attribute IDs: `galvan_priory.nbt` and `ship2.nbt`; only `Attributes` list `Name` strings are changed. |
| Palladium JSON | 416 | 288 data JSON, 123 asset JSON, 5 mixin/refmap/metadata JSON; data groups: 88 recipes, 86 advancements, 80 tags, 21 loot tables, 10 worldgen, 2 Forge entries, 1 damage type. |
| Palladium classes | 1068 | Generic class/Mixin translation plus named Palladium adapters; current transformed-link report is unavailable. |
| Palladium outer mixins | **60** (25 common + 28 client + 7 Forge) | Exact list and per-class classification below. The request’s 59 omitted one client class. |
| PalladiumCore references | 66 distinct symbolic classes | Full list below; dependency selected through JarJar metadata. |

## Palladium UI, skin, rendering, and animation inventory

This inventory was grouped from exact original archive paths; each path remains individually searchable in the CSV manifest. Counts below are Palladium outer class files and resource files, not test coverage claims.

| Surface | Exact path group | Class/resource files | Compatibility review |
|---|---|---:|---|
| UI screens and overlays | `net/threetag/palladium/client/screen/`, `client/gui/`, `client/overlay/` | 64 classes | GUI list/screen/accessory/tailoring/widget adapters are inventoried; layout, scrolling, hitboxes, narration and custom AlienEvo panel remain runtime proof. |
| Skin lookup and skin rendering | client class paths containing `skin` | 6 classes | `PlayerSkinFetcher1201` and `PalladiumCorePlayerSkin1201` are source-backed; lookup/cache/dynamic texture behavior remains runtime proof. |
| Models and renderers | `net/threetag/palladium/client/renderer/`, `client/model/` | 95 classes | Rotation, SuitStand, TrailSegment, BodyPart, render-layer, and GeckoLib compatibility adapters are inventoried; non-weapon rendering remains runtime proof. |
| Animation APIs and classes | class paths containing `animation` or `animat` | 39 classes | Palladium animation/flight/player-animation and GeckoLib KubeJS integration surfaces are inventoried; GeckoLib-gated mixins and Player Animator dependency are listed separately. |
| Animation resource paths | paths containing `/animations/` or `/animation/` | 18 files | Preserved in the full archive manifest; resource links and playback require fresh translated-artifact/game evidence. |

### AlienEvo command findings

Existing converter coverage includes old particle options, `Count`/custom-model item data, item enchantment arguments, ActiveEffects, entity Attributes, and `minecraft:grass`. The original `.mcfunction` audit found 15 legacy particle options, 56 Count lines, 54 CustomModelData lines, one Enchantments line, one Attributes line, one ActiveEffects line, and three grass identifiers; it found no unsupported Count candidate and no `replaceitem`/`blockdata`/`entitydata` candidate. The same shared command converter processes the 1,114 embedded power commands.

`forge:entity_gravity`, `forge:step_height_addition`, and `forge:swim_speed` are mapped in data and entity-command SNBT. New structure NBT handling applies that map only inside list compounds named `Attributes`, preserves other strings, rejects partial rewrites on malformed files, and is covered by new unit tests (not run here). `code_power_test` remains removed only because it is unreferenced. EarthSpikes’ incomplete actionless selector remains documented as intentionally omitted.

### AlienEvo scripts and commands

- Script API static counts: 27 unique `Java.loadClass` names in 67 occurrences; 102 `new ResourceLocation` expressions in 11 scripts. Existing script-scope fixes, KubeJS 7 builder rewrite, quick-change keybind migration, and six-script scoreboard migration remain in place.
- New `flatten.js` gap closure: guard top-level Pehkui `ScaleTypes` loading. Without Pehkui, script registration continues and scale operations are no-ops; the flatten effect itself still requires Pehkui.
- `/maxaliens` and `/mastercontrol` retain permission level 2 and one `PLAYER` argument. Their scoreboard operations migrate to `ScoreHolder`/`ScoreAccess`; current callback execution is unverified because no matched server exception is available.
- Detransform ability 5 still removes the current alien power, restores the watch power, removes the transformation tag, halves the timer for non-master-control users, disables quick-change, and plays its sound. The new null-objective guard prevents timer lookup from aborting restoration. Repeated transform/reversion remains runtime-only validation.

### Assets and resource roots

The archive manifest lists all `addon/`, `assets/`, `data/`, image, audio, font, model, animation, and particle paths. AlienEvo’s addon root and metadata ownership fixes already exist. Historical warnings (`alienevo:botton`, `prototype_switch1.ogg`, animation names, missing model textures/AFOMNI particle sprites, and `/addon/.mcassetsroot`) require a fresh translated archive/report before any changes; none was guessed at here.

## Palladium outer mixin matrix

| Class | Config section | Classification and evidence boundary |
|---|---|---|
| `net.threetag.palladium.mixin.AbstractContainerMenuMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.ArmorItemMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.ArmorStandMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.BlockBehaviourMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.BlockStateBaseMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.CrossbowItemMixin` | `mixins` (`palladium-common.mixins.json`) | intentionally excluded from this pass; Crossbow/weapon feature behavior is explicitly excluded |
| `net.threetag.palladium.mixin.ElytraItemMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.EntityMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.EntitySelectorOptionsInvoker` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.EntitySelectorParserMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by Palladium-specific adapter; Named Palladium mixin adapter; runtime application pending |
| `net.threetag.palladium.mixin.InventoryMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.ItemMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.LivingEntityMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.OreFeatureMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.PackTypeMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.PlayerListMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.PlayerMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.RangedAttributeAccessor` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.ReloadableResourceManagerMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.ReloadableServerResourcesMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.ScriptFileInfoMixin` | `mixins` (`palladium-common.mixins.json`) | optional integration and correctly gated; PalladiumMixinPlugin KubeJS class-presence gate; KubeJS 7 adapter also applies |
| `net.threetag.palladium.mixin.ScriptManagerMixin` | `mixins` (`palladium-common.mixins.json`) | optional integration and correctly gated; PalladiumMixinPlugin KubeJS class-presence gate; KubeJS 7 adapter also applies |
| `net.threetag.palladium.mixin.ServerScoreboardMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.SlotMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.VibrationListenerMixin` | `mixins` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.AbstractClientPlayerMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.AgeableListModelInvoker` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.ClientEntityMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.ClientPacketListenerMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.EntityRendererMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.GameRendererMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.GeoArmorRendererInvoker` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.GeoArmorRendererMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.GeoRendererMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.HttpTextureMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.HumanoidArmorLayerMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.ItemInHandRendererMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.LevelRendererMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.LivingEntityRendererInvoker` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.LivingEntityRendererMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.LocalPlayerMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.MinecraftMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.ModelPartInvoker` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.ModelPartMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.MouseHandlerMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.PlayerInfoMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.PlayerRendererInvoker` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.PlayerRendererMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.RenderTypeMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.ScreenEffectRendererMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.SodiumFixMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.SplashManagerMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.client.TextureManagerMixin` | `client` (`palladium-common.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.forge.CapabilityProviderMixin` | `mixins` (`palladium.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.forge.ForgeHooksMixin` | `mixins` (`palladium.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.forge.IEnergyStorageMixin` | `mixins` (`palladium.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.forge.LootTableMixin` | `mixins` (`palladium.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.forge.MultiversalExtrapolatorItemMixin` | `mixins` (`palladium.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |
| `net.threetag.palladium.mixin.forge.NetworkManagerMixin` | `mixins` (`palladium.mixins.json`) | intentionally excluded from this pass; Shared packet/legacy SimpleChannel behavior is explicitly excluded |
| `net.threetag.palladium.mixin.forge.PathPackResourcesAccessor` | `mixins` (`palladium.mixins.json`) | migrated by existing generic adapter; Generic class/Mixin translation path; current target application report pending |

PalladiumCore has its own four mixin classes: common `LightningBoltMixin`; Forge `ExtendedEntitySpawnDataMixin`, `PalladiumItemMixin`; and client `ClientLevelMixin`. Player Animator is a nested dependency with 13 client mixins. The recursive dependency scan found 18 additional nested-dependency mixin declarations; all are recorded in the path manifest.

**LightningBolt warning investigation:** PalladiumCore’s `LightningBoltMixin` targets `LightningBolt.tick`, the second `Level.getEntities(... )` call (ordinal 1), and captures the resulting `List<Entity>` through `INVOKE_ASSIGN` shifted by one instruction. The cached NeoForge 1.21.1 source confirms ordinal 1 is the actual lightning-hit entity pass; ordinal 0 is the earlier larger delayed-strike list. The new narrow adapter adds `maxShiftBy=1`, declaring the preexisting shift without changing its location. Exact adapter tests are added but not run; runtime application/warning status needs the next report.

## PalladiumCore references (all 66)

- `net/threetag/palladiumcore/event/ChatEvents`
- `net/threetag/palladiumcore/event/ChatEvents$ServerSubmitted`
- `net/threetag/palladiumcore/event/ClientTickEvents`
- `net/threetag/palladiumcore/event/ClientTickEvents$ClientLevelTick`
- `net/threetag/palladiumcore/event/ClientTickEvents$ClientTick`
- `net/threetag/palladiumcore/event/CommandEvents`
- `net/threetag/palladiumcore/event/CommandEvents$Register`
- `net/threetag/palladiumcore/event/Event`
- `net/threetag/palladiumcore/event/EventResult`
- `net/threetag/palladiumcore/event/InputEvents`
- `net/threetag/palladiumcore/event/InputEvents$KeyPressed`
- `net/threetag/palladiumcore/event/InputEvents$MouseScrolling`
- `net/threetag/palladiumcore/event/LifecycleEvents`
- `net/threetag/palladiumcore/event/LivingEntityEvents`
- `net/threetag/palladiumcore/event/LivingEntityEvents$Attack`
- `net/threetag/palladiumcore/event/LivingEntityEvents$Hurt`
- `net/threetag/palladiumcore/event/LivingEntityEvents$Jump`
- `net/threetag/palladiumcore/event/LivingEntityEvents$Tick`
- `net/threetag/palladiumcore/event/PlayerEvents`
- `net/threetag/palladiumcore/event/PlayerEvents$ChangedDimension`
- `net/threetag/palladiumcore/event/PlayerEvents$Clone`
- `net/threetag/palladiumcore/event/PlayerEvents$Join`
- `net/threetag/palladiumcore/event/PlayerEvents$NameFormat`
- `net/threetag/palladiumcore/event/PlayerEvents$Quit`
- `net/threetag/palladiumcore/event/ScreenEvents`
- `net/threetag/palladiumcore/event/ScreenEvents$InitPost`
- `net/threetag/palladiumcore/event/ScreenEvents$Opening`
- `net/threetag/palladiumcore/event/ViewportEvents`
- `net/threetag/palladiumcore/event/ViewportEvents$ComputeCameraAngles`
- `net/threetag/palladiumcore/event/ViewportEvents$ComputeFogColor`
- `net/threetag/palladiumcore/event/ViewportEvents$RenderFog`
- `net/threetag/palladiumcore/forge/PalladiumCoreForge`
- `net/threetag/palladiumcore/item/PalladiumItem`
- `net/threetag/palladiumcore/item/SimpleArmorMaterial`
- `net/threetag/palladiumcore/item/SimpleToolTier`
- `net/threetag/palladiumcore/network/ExtendedEntitySpawnData`
- `net/threetag/palladiumcore/network/Message`
- `net/threetag/palladiumcore/network/MessageC2S`
- `net/threetag/palladiumcore/network/MessageContext`
- `net/threetag/palladiumcore/network/MessageS2C`
- `net/threetag/palladiumcore/network/MessageType`
- `net/threetag/palladiumcore/network/NetworkManager`
- `net/threetag/palladiumcore/network/NetworkManager$MessageDecoder`
- `net/threetag/palladiumcore/network/forge/NetworkManagerImpl`
- `net/threetag/palladiumcore/registry/CreativeModeTabRegistry`
- `net/threetag/palladiumcore/registry/CreativeModeTabRegistry$ItemGroupEntries`
- `net/threetag/palladiumcore/registry/DeferredRegister`
- `net/threetag/palladiumcore/registry/EntityAttributeRegistry`
- `net/threetag/palladiumcore/registry/PalladiumRegistry`
- `net/threetag/palladiumcore/registry/RegistrySupplier`
- `net/threetag/palladiumcore/registry/ReloadListenerRegistry`
- `net/threetag/palladiumcore/registry/VillagerTradeRegistry`
- `net/threetag/palladiumcore/registry/client/ColorHandlerRegistry`
- `net/threetag/palladiumcore/registry/client/EntityRendererRegistry`
- `net/threetag/palladiumcore/registry/client/ItemPropertyRegistry`
- `net/threetag/palladiumcore/registry/client/KeyMappingRegistry`
- `net/threetag/palladiumcore/registry/client/OverlayRegistry`
- `net/threetag/palladiumcore/registry/client/OverlayRegistry$IngameOverlay`
- `net/threetag/palladiumcore/registry/client/ParticleProviderRegistry`
- `net/threetag/palladiumcore/registry/client/RenderTypeRegistry`
- `net/threetag/palladiumcore/util/DataSyncUtil`
- `net/threetag/palladiumcore/util/DataSyncUtil$DataSync`
- `net/threetag/palladiumcore/util/DataSyncUtil$EntitySync`
- `net/threetag/palladiumcore/util/Platform`
- `net/threetag/palladiumcore/util/Platform$Mod`
- `net/threetag/palladiumcore/util/PlayerUtil`

## Optional API references and gates

Types below were extracted from class-file `CONSTANT_Class` entries and class descriptors/signatures (generic signatures normalized to internal names). The manifest includes each type record. Source gates do not prove an integration works.

| Family | Distinct types | Source gate/dependency selection |
|---|---:|---|
| GeckoLib | 27 | PalladiumForge checks geckolib before integration init; client mixins are class-presence-gated |
| Player Animator | 10 | Selected as a nested JarJar dependency in the original Palladium archive |
| Architectury | 2 | Two expectation-platform annotation references; no direct gameplay class path identified |
| KubeJS | 23 | ScriptFileInfo/ScriptManager mixins are class-presence-gated; KubeJS 7 adapter also applies |
| Rhino | 5 | KubeJS scripting dependency; target profile supplies Rhino |
| Curios | 15 | PalladiumForge checks ModList before Curios common/client initialization |
| JEI | 27 | Palladium integration uses JEI @JeiPlugin discovery and is client-side |
| Pehkui | 7 | Palladium setup hook checks Platform.isModLoaded("pehkui") before PehkuiCompat.init |
| Iris | 1 | RenderUtil short-circuits Iris/Oculus; Iris API access is on the Oculus path |
| AttributesLib | 1 | PalladiumForge checks mod presence before ApothicAttributesCompat.init |
| Sodium | 1 | Client SodiumFixMixin is gated on the legacy Sodium renderer class |

### GeckoLib (27 types)

`software/bernie/geckolib/animatable/GeoItem`, `software/bernie/geckolib/animatable/SingletonGeoAnimatable`, `software/bernie/geckolib/cache/object/BakedGeoModel`, `software/bernie/geckolib/cache/object/GeoBone`, `software/bernie/geckolib/cache/object/GeoQuad`, `software/bernie/geckolib/cache/object/GeoVertex`, `software/bernie/geckolib/cache/texture/AnimatableTexture`, `software/bernie/geckolib/constant/DataTickets`, `software/bernie/geckolib/constant/DefaultAnimations`, `software/bernie/geckolib/core/animatable/GeoAnimatable`, `software/bernie/geckolib/core/animatable/instance/AnimatableInstanceCache`, `software/bernie/geckolib/core/animatable/model/CoreGeoBone`, `software/bernie/geckolib/core/animation/AnimatableManager`, `software/bernie/geckolib/core/animation/AnimatableManager$ControllerRegistrar`, `software/bernie/geckolib/core/animation/AnimationController`, `software/bernie/geckolib/core/animation/AnimationController$AnimationStateHandler`, `software/bernie/geckolib/core/animation/AnimationState`, `software/bernie/geckolib/core/animation/RawAnimation`, `software/bernie/geckolib/core/molang/MolangParser`, `software/bernie/geckolib/core/object/Color`, `software/bernie/geckolib/core/object/DataTicket`, `software/bernie/geckolib/core/object/PlayState`, `software/bernie/geckolib/model/GeoModel`, `software/bernie/geckolib/renderer/GeoArmorRenderer`, `software/bernie/geckolib/renderer/GeoRenderer`, `software/bernie/geckolib/util/GeckoLibUtil`, `software/bernie/geckolib/util/RenderUtils`

### Player Animator (10 types)

`dev/kosmx/playerAnim/api/TransformType`, `dev/kosmx/playerAnim/api/layered/AnimationStack`, `dev/kosmx/playerAnim/api/layered/IAnimation`, `dev/kosmx/playerAnim/api/layered/KeyframeAnimationPlayer`, `dev/kosmx/playerAnim/api/layered/ModifierLayer`, `dev/kosmx/playerAnim/api/layered/modifier/AbstractFadeModifier`, `dev/kosmx/playerAnim/api/layered/modifier/AbstractFadeModifier$EasingFunction`, `dev/kosmx/playerAnim/core/data/KeyframeAnimation`, `dev/kosmx/playerAnim/minecraftApi/PlayerAnimationAccess`, `dev/kosmx/playerAnim/minecraftApi/PlayerAnimationRegistry`

### Architectury (2 types)

`dev/architectury/injectables/annotations/ExpectPlatform`, `dev/architectury/injectables/annotations/ExpectPlatform$Transformed`

### KubeJS (23 types)

`dev/latvian/mods/kubejs/KubeJS`, `dev/latvian/mods/kubejs/KubeJSPlugin`, `dev/latvian/mods/kubejs/entity/EntityEventJS`, `dev/latvian/mods/kubejs/event/EventGroup`, `dev/latvian/mods/kubejs/event/EventHandler`, `dev/latvian/mods/kubejs/event/EventJS`, `dev/latvian/mods/kubejs/event/EventResult`, `dev/latvian/mods/kubejs/level/LevelEventJS`, `dev/latvian/mods/kubejs/registry/BuilderBase`, `dev/latvian/mods/kubejs/registry/BuilderFactory`, `dev/latvian/mods/kubejs/registry/RegistryInfo`, `dev/latvian/mods/kubejs/script/BindingsEvent`, `dev/latvian/mods/kubejs/script/ConsoleLine`, `dev/latvian/mods/kubejs/script/ScriptFile`, `dev/latvian/mods/kubejs/script/ScriptFileInfo`, `dev/latvian/mods/kubejs/script/ScriptManager`, `dev/latvian/mods/kubejs/script/ScriptPack`, `dev/latvian/mods/kubejs/script/ScriptPackInfo`, `dev/latvian/mods/kubejs/script/ScriptSource`, `dev/latvian/mods/kubejs/script/ScriptType`, `dev/latvian/mods/kubejs/util/AttachedData`, `dev/latvian/mods/kubejs/util/ConsoleJS`, `dev/latvian/mods/kubejs/util/UtilsJS`

### Rhino (5 types)

`dev/latvian/mods/rhino/Context`, `dev/latvian/mods/rhino/Scriptable`, `dev/latvian/mods/rhino/util/wrap/TypeWrapperFactory`, `dev/latvian/mods/rhino/util/wrap/TypeWrapperFactory$Simple`, `dev/latvian/mods/rhino/util/wrap/TypeWrappers`

### Curios (15 types)

`top/theillusivec4/curios/api/CuriosApi`, `top/theillusivec4/curios/api/CuriosCapability`, `top/theillusivec4/curios/api/SlotContext`, `top/theillusivec4/curios/api/SlotTypeMessage`, `top/theillusivec4/curios/api/SlotTypeMessage$Builder`, `top/theillusivec4/curios/api/client/CuriosRendererRegistry`, `top/theillusivec4/curios/api/client/ICurioRenderer`, `top/theillusivec4/curios/api/event/CurioEquipEvent`, `top/theillusivec4/curios/api/type/ISlotType`, `top/theillusivec4/curios/api/type/capability/ICurio`, `top/theillusivec4/curios/api/type/capability/ICuriosItemHandler`, `top/theillusivec4/curios/api/type/inventory/ICurioStacksHandler`, `top/theillusivec4/curios/api/type/inventory/IDynamicStackHandler`, `top/theillusivec4/curios/api/type/util/ICuriosHelper`, `top/theillusivec4/curios/api/type/util/ISlotHelper`

### JEI (27 types)

`mezz/jei/api/IModPlugin`, `mezz/jei/api/JeiPlugin`, `mezz/jei/api/constants/RecipeTypes`, `mezz/jei/api/gui/builder/IIngredientAcceptor`, `mezz/jei/api/gui/builder/IRecipeLayoutBuilder`, `mezz/jei/api/gui/builder/IRecipeSlotBuilder`, `mezz/jei/api/gui/drawable/IDrawable`, `mezz/jei/api/gui/drawable/IDrawableStatic`, `mezz/jei/api/gui/ingredient/IRecipeSlotDrawablesView`, `mezz/jei/api/gui/ingredient/IRecipeSlotView`, `mezz/jei/api/gui/ingredient/IRecipeSlotsView`, `mezz/jei/api/gui/widgets/IRecipeExtrasBuilder`, `mezz/jei/api/gui/widgets/IScrollGridWidget`, `mezz/jei/api/helpers/IGuiHelper`, `mezz/jei/api/helpers/IJeiHelpers`, `mezz/jei/api/recipe/IFocusGroup`, `mezz/jei/api/recipe/RecipeIngredientRole`, `mezz/jei/api/recipe/RecipeType`, `mezz/jei/api/recipe/category/IRecipeCategory`, `mezz/jei/api/recipe/transfer/IRecipeTransferError`, `mezz/jei/api/recipe/transfer/IRecipeTransferHandler`, `mezz/jei/api/recipe/transfer/IRecipeTransferHandlerHelper`, `mezz/jei/api/registration/IGuiHandlerRegistration`, `mezz/jei/api/registration/IRecipeCatalystRegistration`, `mezz/jei/api/registration/IRecipeCategoryRegistration`, `mezz/jei/api/registration/IRecipeRegistration`, `mezz/jei/api/registration/IRecipeTransferRegistration`

### Pehkui (7 types)

`virtuoel/pehkui/api/ScaleData`, `virtuoel/pehkui/api/ScaleModifier`, `virtuoel/pehkui/api/ScaleRegistries`, `virtuoel/pehkui/api/ScaleType`, `virtuoel/pehkui/api/ScaleType$Builder`, `virtuoel/pehkui/api/ScaleTypes`, `virtuoel/pehkui/api/TypedScaleModifier`

### Iris (1 types)

`net/irisshaders/iris/api/v0/IrisApi`

### AttributesLib (1 types)

`dev/shadowsoffire/attributeslib/util/IEntityOwned`

### Sodium (1 types)

`me/jellysquid/mods/sodium/client/render/immediate/model/EntityRenderer`

Palladium’s mixin plugin probes bytecode without loading candidate classes; it gates KubeJS, Trinkets, GeckoLib, Quilt resource loader, and the legacy Sodium renderer mixins. `PalladiumForge` separately gates Curios common/client init, GeckoLib init, and AttributesLib init. Its setup callback gates Pehkui. JEI uses `@JeiPlugin` discovery. Iris/Oculus access is short-circuited through the Oculus path. Architectury refs are expectation-platform annotations. Player Animator is selected by JarJar metadata.

## Palladium-specific adapters and remaining runtime proof

| Feature | Existing adapter | Runtime proof still required |
|---|---|---|
| Flight tick | `PalladiumFlightHandler1201` changes private `EntityDimensions.height` field access to public accessor | Tick an active flight ability repeatedly |
| PalladiumCore overlays | Existing Forge overlay shim plus `PalladiumCoreOverlay1201` validator | Confirm ability bar/wheel HUD renders |
| Lightning strike mixin | `PalladiumCoreLightningBoltMixin1201` declares exact shift allowance | Fresh report has no shift warning; mixin/event works |
| Skin fetching | `PalladiumPlayerSkinFetcher1201`, `PalladiumCorePlayerSkin1201` | Skin lookup and dynamic rendering in world |
| Addon packs | `PalladiumAddonPackManager1201` and resource adapter | Discover/list/enable AlienEvo/AFOMNI; real data/resources/scripts register |
| GUI lists/screens | Selection/Powers/Accessory/Tailoring/Widget adapters | hitboxes, scroll, clipping, narration, layout at varied sizes |
| Selector | `PalladiumMixinCompat1201` | positive and negated selector with real registered power |
| Body/rendering | rotation, SuitStand, TrailSegment, BodyPart and Gecko render adapters | exercise a non-weapon path that uses each renderer |
| KubeJS | `PalladiumKubeJS1201` and AlienEvo resource adapter | actual builders/properties/callback invocation in addon pack |
| Data/functions | `DataConverter1201`, `FunctionConverter1201` | loot, modifiers, recipes, abilities, and function execution |

## Unresolved/soft-link and scope classifications

- No new transformed JAR/report exists, so current soft-link/unresolved call sites cannot be enumerated honestly. The historical 33-site count and older link reports are not carried forward as current measurements.
- AlienEvo `/maxaliens`, `/mastercontrol`, repeated transform/reversion, ability 5, real power registration, and custom `PalladiumEvents.renderPowerScreen` panel are reachable but unverified. The custom panel is distinct from the quick-change wheel and PalladiumCore HUD.
- Resource warnings listed above stay pending fresh transformed-resource evidence. No speculative asset rename/removal was made.
- Crossbow/weapon/Add-onCrossbow/projectile/explosion behavior and shared packet/`SimpleChannel` compatibility remain explicitly outside this supported pass.

## Full path manifest

[`PALLADIUM-ALIENEVO-ARCHIVE-MANIFEST.csv`](PALLADIUM-ALIENEVO-ARCHIVE-MANIFEST.csv) contains every outer central-directory entry and every recursively embedded JarJar dependency entry (including directories), outer and nested declared mixins, the 66 Core type references, and every optional API type. It separates implementation classification from validation boundaries.

## Consolidated build checkpoint

Run after source review. The assistant has not run builds/tests for this pass.

```powershell
git switch fix/forge1201-palladium-runtime-adapters
git pull --ff-only
git rev-parse HEAD
.\gradlew.bat :clean :loader:clean :testmods:clean :build :loader:build :testmods:assemble --no-daemon --console=plain
```

Only after `BUILD SUCCESSFUL`, close Minecraft and install once:

```powershell
.\gradlew.bat clean installBundle "-PmodsDir=E:\Games\PolyMC\instances\Neoforge tester\.minecraft\mods" --no-daemon --console=plain
```

Then run one broad client/gameplay pass and collect same-launch `latest.log`, `startup.log`, `legacyconnector-report.txt`, and any crash report. The continuation handoff records the full feature checklist and exact next action.
