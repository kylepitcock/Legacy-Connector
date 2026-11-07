# 1.20.1 corpus and gap report

Back to [Home](Home.md). Measurement for [Legacy-1.20.1-Estimate](Legacy-1.20.1-Estimate.md) (section 9). Measured 2026-11-07 with the tools in this
repository, nothing here is an estimate except where it says so.

## 1. Corpus

`test-mods-1201/` (gitignored, never redistributed) holds **140 Forge 1.20.1 jars (982 MB)**, built by `tools/compat/corpus1201.py` from Modrinth's most downloaded
Forge 1.20.1 projects plus the required dependencies each download declares:

| Group | Jars | Meaning |
|---|---|---|
| Abandoned content mods | **72** | no NeoForge 1.21.1 file on Modrinth and none under the same slug on CurseForge (cfwidget); the set the 1.20.1 layer exists for |
| Content mods with a 1.21.1 port | 26 | kept as a reference sample (popular mods: Quark, Supplementaries, Mekanism, Xaero...) |
| Libraries and dependencies | 42 | Curios, GeckoLib, Architectury, Cloth Config, Kotlin for Forge, Patchouli, JEI, Create, Mantle, Citadel, Moonlight, Zeta, Blueprint, Creative Core... |

How "newer 1.21.1 NeoForge version exists" was decided: Modrinth `project/<id>/version?game_versions=["1.21.1"]&loaders=["neoforge"]` and the
CurseForge file list of the same slug through cfwidget (a file tagged 1.21.1 and NeoForge). A project with neither is counted as abandoned. The check
is per project, so **unofficial ports and rewrites under other names are not counted as a port**; the appendix lists candidates found by a title search
(`Port candidates`, unverified; many are add-ons or continuations by other authors). Examples: Alex's Mobs and Alex's Caves have community 1.21.1
ports by other authors, Tinkers' Construct has an unofficial port (`hexa-tinkers-construct-unofficial-port`) and no official 1.21.1 release at the
time of the measurement (CLAUDE.md says otherwise; the CurseForge file list of `tinkers-construct` ends at 1.20.1), Botania's CurseForge list also ends at
1.20.1. Fabric jars that came in as dependencies (Connector, Fabric API, Origins, Continuity...) were moved to `test-mods-1201-fabric/` and are not part of the numbers.

Rebuild: `python tools/compat/corpus1201.py test-mods-1201 --scan 2500 --abandoned 75 --with-ported 22` (about 15 minutes, downloads about 1 GB),
`--extra slug,slug` adds named projects, `--index-only` refreshes `index.json`/`index.md` from the jars on disk.

## 2. Method

`./gradlew gap1201 -PtargetJars=<minecraft+neoforge merged jar, neoforge jar, FML/bus/modlauncher and the other libraries of the game class path> -PgapJars=test-mods-1201`
(`loader/.../tools/Gap1201`) renames every reference like the layer would (SRG ids to Mojang names, Forge classes through
`forward/1.20.1/*.tsv`, other `net.minecraftforge` classes to `net.neoforged.neoforge` where that class exists) and reports every class, method and field
reference that does not resolve against Minecraft 1.21.1 + NeoForge 21.1.258, the JDK, the libraries and the other jars of the set. Changes to the tool
for this run: libraries on the target list (the `Dist` annotation lives in a separate API jar and was a false gap in 94 jars), the mods' own members are
renamed too (descriptors with moved classes made every cross-class call inside a mod count as missing: library gaps fell from 12,656 to 1,238 fields),
and `<report>.detail.tsv` (jar, kind, symbol) is written for `tools/compat/gap1201_rank.py`, which ranks and classifies. Mixins are counted from the
mixin config files each jar declares (mixin classes, not files).

What the report cannot see: behaviour changes with an unchanged signature (e.g. `Block.use`, data-driven enchantments), mixin targets, resource formats,
reflection and string-based lookups. The numbers are a lower bound on the work. `com.mojang.*` classes that vanished are not reported as missing
classes (the tool treats `com/mojang/` as library), only their members.

## 3. Results

140 jars, 8315 distinct unresolved symbols in total; the abandoned content mods alone (72 jars) have 3801 distinct unresolved symbols.


### 3.1 Distinct unresolved symbols by area

| Area | All 140 jars | Abandoned content mods (72 jars) |
|---|---|---|
| Library / other mod | 5244 | 1938 |
| Minecraft 1.20.1 to 1.21.1 change | 2189 | 1353 |
| Forge API | 525 | 363 |
| Rendering / client | 297 | 142 |
| JDK (not an API gap) | 60 | 5 |

"Library / other mod" is the residue of references to other mods (including mods outside the corpus, and members inherited through renamed Minecraft classes); it shrinks when the libraries themselves are migrated and is not an API gap of Forge or Minecraft.

### 3.2 Top 40 unresolved symbols in the abandoned content mods (jars that need them, of 72)

| Jars | Kind | Symbol | Area |
|---|---|---|---|
| 49 | class | `net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext` | Forge API |
| 40 | class | `net/minecraftforge/registries/ForgeRegistries` | Forge API |
| 40 | class | `net/minecraftforge/registries/IForgeRegistry` | Forge API |
| 39 | method | `net/minecraft/resources/ResourceLocation.<init>(Ljava/lang/String;)V` | Minecraft 1.20.1 to 1.21.1 change |
| 30 | method | `net/neoforged/neoforge/registries/DeferredRegister.create(Lnet/minecraftforge/registries/IForgeRegistry;Ljava/lang/String;)Lnet/neoforged/neoforge/registries/DeferredRegister;` | Forge API |
| 29 | method | `net/minecraft/world/item/ItemStack.hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V` | Minecraft 1.20.1 to 1.21.1 change |
| 28 | class | `net/minecraftforge/network/simple/SimpleChannel` | Forge API |
| 26 | class | `net/minecraftforge/network/NetworkEvent$Context` | Forge API |
| 25 | method | `net/minecraft/world/item/ItemStack.getTag()Lnet/minecraft/nbt/CompoundTag;` | Minecraft 1.20.1 to 1.21.1 change |
| 24 | field | `net/minecraft/world/entity/ai/attributes/AttributeModifier$Operation.ADDITION:Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;` | Minecraft 1.20.1 to 1.21.1 change |
| 24 | method | `net/minecraft/network/syncher/SynchedEntityData.define(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V` | Minecraft 1.20.1 to 1.21.1 change |
| 24 | method | `net/minecraft/world/entity/player/Player.broadcastBreakEvent(Lnet/minecraft/world/InteractionHand;)V` | Minecraft 1.20.1 to 1.21.1 change |
| 24 | method | `net/minecraft/world/item/ItemStack.getOrCreateTag()Lnet/minecraft/nbt/CompoundTag;` | Minecraft 1.20.1 to 1.21.1 change |
| 23 | field | `net/minecraft/world/entity/ai/attributes/Attributes.ATTACK_DAMAGE:Lnet/minecraft/world/entity/ai/attributes/Attribute;` | Minecraft 1.20.1 to 1.21.1 change |
| 23 | field | `net/minecraft/world/entity/ai/attributes/Attributes.MOVEMENT_SPEED:Lnet/minecraft/world/entity/ai/attributes/Attribute;` | Minecraft 1.20.1 to 1.21.1 change |
| 23 | method | `net/minecraft/world/effect/MobEffectInstance.<init>(Lnet/minecraft/world/effect/MobEffect;II)V` | Minecraft 1.20.1 to 1.21.1 change |
| 23 | method | `net/minecraft/world/effect/MobEffectInstance.<init>(Lnet/minecraft/world/effect/MobEffect;IIZZ)V` | Minecraft 1.20.1 to 1.21.1 change |
| 23 | method | `net/minecraft/world/entity/ai/attributes/AttributeModifier.<init>(Ljava/util/UUID;Ljava/lang/String;DLnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)V` | Minecraft 1.20.1 to 1.21.1 change |
| 22 | class | `net/minecraftforge/network/NetworkHooks` | Forge API |
| 22 | class | `net/minecraftforge/network/simple/IndexedMessageCodec$MessageHandler` | Forge API |
| 22 | class | `net/minecraftforge/registries/ForgeRegistries$Keys` | Forge API |
| 22 | field | `net/minecraft/world/entity/ai/attributes/Attributes.ARMOR:Lnet/minecraft/world/entity/ai/attributes/Attribute;` | Minecraft 1.20.1 to 1.21.1 change |
| 22 | field | `net/minecraft/world/entity/ai/attributes/Attributes.KNOCKBACK_RESISTANCE:Lnet/minecraft/world/entity/ai/attributes/Attribute;` | Minecraft 1.20.1 to 1.21.1 change |
| 22 | field | `net/minecraft/world/entity/ai/attributes/Attributes.MAX_HEALTH:Lnet/minecraft/world/entity/ai/attributes/Attribute;` | Minecraft 1.20.1 to 1.21.1 change |
| 22 | method | `net/minecraft/world/effect/MobEffectInstance.getEffect()Lnet/minecraft/world/effect/MobEffect;` | Minecraft 1.20.1 to 1.21.1 change |
| 22 | method | `net/minecraft/world/level/block/state/BlockBehaviour$Properties.copy(Lnet/minecraft/world/level/block/state/BlockBehaviour;)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;` | Minecraft 1.20.1 to 1.21.1 change |
| 21 | class | `net/minecraft/world/item/alchemy/PotionUtils` | Minecraft 1.20.1 to 1.21.1 change |
| 21 | class | `net/minecraftforge/fml/DistExecutor` | Forge API |
| 21 | method | `net/neoforged/fml/ModLoadingContext.registerConfig(Lnet/neoforged/fml/config/ModConfig$Type;Lnet/neoforged/fml/config/IConfigSpec;)V` | Forge API |
| 20 | class | `net/minecraft/world/entity/MobType` | Minecraft 1.20.1 to 1.21.1 change |
| 20 | class | `net/minecraftforge/event/TickEvent$Phase` | Forge API |
| 20 | class | `net/minecraftforge/network/NetworkDirection` | Forge API |
| 20 | method | `net/minecraft/world/entity/ai/attributes/AttributeSupplier$Builder.add(Lnet/minecraft/world/entity/ai/attributes/Attribute;D)Lnet/minecraft/world/entity/ai/attributes/AttributeSupplier$Builder;` | Minecraft 1.20.1 to 1.21.1 change |
| 19 | class | `net/minecraftforge/common/ToolAction` | Forge API |
| 19 | class | `net/minecraftforge/common/ToolActions` | Forge API |
| 19 | method | `com/mojang/blaze3d/vertex/VertexConsumer.endVertex()V` | Rendering / client |
| 19 | method | `net/minecraft/world/item/ItemStack.hasTag()Z` | Minecraft 1.20.1 to 1.21.1 change |
| 18 | class | `net/minecraftforge/common/util/LazyOptional` | Forge API |
| 18 | class | `net/minecraftforge/network/PacketDistributor$PacketTarget` | Forge API |
| 18 | field | `net/minecraft/world/effect/MobEffects.MOVEMENT_SLOWDOWN:Lnet/minecraft/world/effect/MobEffect;` | Minecraft 1.20.1 to 1.21.1 change |

### 3.3 Top 40 unresolved symbols over all 140 jars

| Jars | Kind | Symbol | Area |
|---|---|---|---|
| 94 | class | `net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext` | Forge API |
| 90 | method | `net/minecraft/resources/ResourceLocation.<init>(Ljava/lang/String;)V` | Minecraft 1.20.1 to 1.21.1 change |
| 77 | class | `net/minecraftforge/registries/ForgeRegistries` | Forge API |
| 77 | class | `net/minecraftforge/registries/IForgeRegistry` | Forge API |
| 57 | class | `net/minecraftforge/network/NetworkEvent$Context` | Forge API |
| 56 | class | `net/minecraftforge/network/simple/SimpleChannel` | Forge API |
| 56 | method | `net/minecraft/world/item/ItemStack.getTag()Lnet/minecraft/nbt/CompoundTag;` | Minecraft 1.20.1 to 1.21.1 change |
| 49 | method | `com/mojang/blaze3d/vertex/VertexConsumer.endVertex()V` | Rendering / client |
| 49 | method | `net/neoforged/neoforge/registries/DeferredRegister.create(Lnet/minecraftforge/registries/IForgeRegistry;Ljava/lang/String;)Lnet/neoforged/neoforge/registries/DeferredRegister;` | Forge API |
| 47 | class | `net/minecraftforge/event/TickEvent$Phase` | Forge API |
| 46 | method | `com/mojang/blaze3d/vertex/VertexConsumer.uv(FF)Lcom/mojang/blaze3d/vertex/VertexConsumer;` | Rendering / client |
| 44 | class | `net/minecraftforge/network/simple/IndexedMessageCodec$MessageHandler` | Forge API |
| 44 | method | `net/minecraft/world/item/ItemStack.setTag(Lnet/minecraft/nbt/CompoundTag;)V` | Minecraft 1.20.1 to 1.21.1 change |
| 44 | method | `net/neoforged/bus/api/IEventBus.post(Lnet/neoforged/bus/api/Event;)Z` | Forge API |
| 43 | class | `net/minecraftforge/event/TickEvent$ClientTickEvent` | Forge API |
| 42 | class | `net/minecraftforge/network/NetworkDirection` | Forge API |
| 41 | class | `net/minecraftforge/fml/DistExecutor` | Forge API |
| 41 | class | `net/minecraftforge/registries/ForgeRegistries$Keys` | Forge API |
| 41 | method | `com/mojang/blaze3d/vertex/VertexConsumer.vertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;` | Rendering / client |
| 41 | method | `net/minecraft/world/item/ItemStack.getOrCreateTag()Lnet/minecraft/nbt/CompoundTag;` | Minecraft 1.20.1 to 1.21.1 change |
| 41 | method | `net/minecraft/world/item/ItemStack.hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V` | Minecraft 1.20.1 to 1.21.1 change |
| 40 | class | `net/minecraftforge/network/NetworkHooks` | Forge API |
| 40 | method | `net/minecraft/world/effect/MobEffectInstance.getEffect()Lnet/minecraft/world/effect/MobEffect;` | Minecraft 1.20.1 to 1.21.1 change |
| 40 | method | `net/minecraft/world/item/ItemStack.hasTag()Z` | Minecraft 1.20.1 to 1.21.1 change |
| 39 | class | `net/minecraftforge/network/PacketDistributor$PacketTarget` | Forge API |
| 39 | method | `com/mojang/blaze3d/vertex/BufferBuilder.begin(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;)V` | Rendering / client |
| 39 | method | `com/mojang/blaze3d/vertex/Tesselator.getBuilder()Lcom/mojang/blaze3d/vertex/BufferBuilder;` | Rendering / client |
| 38 | field | `net/minecraft/world/entity/ai/attributes/AttributeModifier$Operation.ADDITION:Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;` | Minecraft 1.20.1 to 1.21.1 change |
| 38 | method | `net/minecraft/world/item/ItemStack.save(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;` | Minecraft 1.20.1 to 1.21.1 change |
| 37 | class | `net/minecraftforge/common/util/LazyOptional` | Forge API |
| 37 | field | `net/minecraft/world/entity/ai/attributes/Attributes.ATTACK_DAMAGE:Lnet/minecraft/world/entity/ai/attributes/Attribute;` | Minecraft 1.20.1 to 1.21.1 change |
| 37 | method | `com/mojang/blaze3d/vertex/VertexConsumer.color(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;` | Rendering / client |
| 37 | method | `net/minecraft/world/item/ItemStack.of(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/item/ItemStack;` | Minecraft 1.20.1 to 1.21.1 change |
| 37 | method | `net/neoforged/neoforge/network/PacketDistributor.with(Ljava/util/function/Supplier;)Lnet/minecraftforge/network/PacketDistributor$PacketTarget;` | Forge API |
| 36 | class | `net/minecraftforge/common/capabilities/Capability` | Forge API |
| 36 | method | `com/mojang/blaze3d/vertex/VertexConsumer.color(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;` | Rendering / client |
| 36 | method | `net/minecraft/world/effect/MobEffectInstance.<init>(Lnet/minecraft/world/effect/MobEffect;II)V` | Minecraft 1.20.1 to 1.21.1 change |
| 36 | method | `net/minecraft/world/level/block/state/BlockBehaviour$Properties.copy(Lnet/minecraft/world/level/block/state/BlockBehaviour;)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;` | Minecraft 1.20.1 to 1.21.1 change |
| 35 | field | `net/minecraft/world/entity/ai/attributes/Attributes.MAX_HEALTH:Lnet/minecraft/world/entity/ai/attributes/Attribute;` | Minecraft 1.20.1 to 1.21.1 change |
| 35 | method | `net/minecraft/network/syncher/SynchedEntityData.define(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V` | Minecraft 1.20.1 to 1.21.1 change |

### 3.4 Top 20 per area (all jars)

**Forge API**

| Jars | Kind | Symbol |
|---|---|---|
| 94 | class | `net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext` |
| 77 | class | `net/minecraftforge/registries/ForgeRegistries` |
| 77 | class | `net/minecraftforge/registries/IForgeRegistry` |
| 57 | class | `net/minecraftforge/network/NetworkEvent$Context` |
| 56 | class | `net/minecraftforge/network/simple/SimpleChannel` |
| 49 | method | `net/neoforged/neoforge/registries/DeferredRegister.create(Lnet/minecraftforge/registries/IForgeRegistry;Ljava/lang/String;)Lnet/neoforged/neoforge/registries/DeferredRegister;` |
| 47 | class | `net/minecraftforge/event/TickEvent$Phase` |
| 44 | class | `net/minecraftforge/network/simple/IndexedMessageCodec$MessageHandler` |
| 44 | method | `net/neoforged/bus/api/IEventBus.post(Lnet/neoforged/bus/api/Event;)Z` |
| 43 | class | `net/minecraftforge/event/TickEvent$ClientTickEvent` |
| 42 | class | `net/minecraftforge/network/NetworkDirection` |
| 41 | class | `net/minecraftforge/fml/DistExecutor` |
| 41 | class | `net/minecraftforge/registries/ForgeRegistries$Keys` |
| 40 | class | `net/minecraftforge/network/NetworkHooks` |
| 39 | class | `net/minecraftforge/network/PacketDistributor$PacketTarget` |
| 37 | class | `net/minecraftforge/common/util/LazyOptional` |
| 37 | method | `net/neoforged/neoforge/network/PacketDistributor.with(Ljava/util/function/Supplier;)Lnet/minecraftforge/network/PacketDistributor$PacketTarget;` |
| 36 | class | `net/minecraftforge/common/capabilities/Capability` |
| 35 | method | `net/neoforged/fml/ModLoadingContext.registerConfig(Lnet/neoforged/fml/config/ModConfig$Type;Lnet/neoforged/fml/config/IConfigSpec;)V` |
| 34 | class | `net/minecraftforge/common/ForgeHooks` |

**Minecraft 1.20.1 to 1.21.1 change**

| Jars | Kind | Symbol |
|---|---|---|
| 90 | method | `net/minecraft/resources/ResourceLocation.<init>(Ljava/lang/String;)V` |
| 56 | method | `net/minecraft/world/item/ItemStack.getTag()Lnet/minecraft/nbt/CompoundTag;` |
| 44 | method | `net/minecraft/world/item/ItemStack.setTag(Lnet/minecraft/nbt/CompoundTag;)V` |
| 41 | method | `net/minecraft/world/item/ItemStack.getOrCreateTag()Lnet/minecraft/nbt/CompoundTag;` |
| 41 | method | `net/minecraft/world/item/ItemStack.hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V` |
| 40 | method | `net/minecraft/world/effect/MobEffectInstance.getEffect()Lnet/minecraft/world/effect/MobEffect;` |
| 40 | method | `net/minecraft/world/item/ItemStack.hasTag()Z` |
| 38 | field | `net/minecraft/world/entity/ai/attributes/AttributeModifier$Operation.ADDITION:Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;` |
| 38 | method | `net/minecraft/world/item/ItemStack.save(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;` |
| 37 | field | `net/minecraft/world/entity/ai/attributes/Attributes.ATTACK_DAMAGE:Lnet/minecraft/world/entity/ai/attributes/Attribute;` |
| 37 | method | `net/minecraft/world/item/ItemStack.of(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/item/ItemStack;` |
| 36 | method | `net/minecraft/world/effect/MobEffectInstance.<init>(Lnet/minecraft/world/effect/MobEffect;II)V` |
| 36 | method | `net/minecraft/world/level/block/state/BlockBehaviour$Properties.copy(Lnet/minecraft/world/level/block/state/BlockBehaviour;)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;` |
| 35 | field | `net/minecraft/world/entity/ai/attributes/Attributes.MAX_HEALTH:Lnet/minecraft/world/entity/ai/attributes/Attribute;` |
| 35 | method | `net/minecraft/network/syncher/SynchedEntityData.define(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V` |
| 35 | method | `net/minecraft/world/entity/ai/attributes/AttributeModifier.<init>(Ljava/util/UUID;Ljava/lang/String;DLnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)V` |
| 34 | class | `net/minecraft/world/item/alchemy/PotionUtils` |
| 34 | field | `net/minecraft/world/entity/ai/attributes/Attributes.MOVEMENT_SPEED:Lnet/minecraft/world/entity/ai/attributes/Attribute;` |
| 34 | method | `net/minecraft/world/entity/player/Player.broadcastBreakEvent(Lnet/minecraft/world/InteractionHand;)V` |
| 32 | method | `net/minecraft/world/effect/MobEffectInstance.<init>(Lnet/minecraft/world/effect/MobEffect;IIZZ)V` |

**Rendering / client**

| Jars | Kind | Symbol |
|---|---|---|
| 49 | method | `com/mojang/blaze3d/vertex/VertexConsumer.endVertex()V` |
| 46 | method | `com/mojang/blaze3d/vertex/VertexConsumer.uv(FF)Lcom/mojang/blaze3d/vertex/VertexConsumer;` |
| 41 | method | `com/mojang/blaze3d/vertex/VertexConsumer.vertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;` |
| 39 | method | `com/mojang/blaze3d/vertex/BufferBuilder.begin(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;)V` |
| 39 | method | `com/mojang/blaze3d/vertex/Tesselator.getBuilder()Lcom/mojang/blaze3d/vertex/BufferBuilder;` |
| 37 | method | `com/mojang/blaze3d/vertex/VertexConsumer.color(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;` |
| 36 | method | `com/mojang/blaze3d/vertex/VertexConsumer.color(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;` |
| 30 | method | `com/mojang/blaze3d/vertex/BufferBuilder.vertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;` |
| 30 | method | `com/mojang/blaze3d/vertex/VertexConsumer.uv2(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;` |
| 30 | method | `net/minecraft/client/Minecraft.getFrameTime()F` |
| 30 | method | `net/minecraft/client/model/geom/ModelPart.render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V` |
| 27 | method | `com/mojang/blaze3d/vertex/VertexConsumer.normal(Lorg/joml/Matrix3f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;` |
| 26 | method | `com/mojang/blaze3d/vertex/BufferBuilder.end()Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;` |
| 25 | method | `com/mojang/blaze3d/vertex/Tesselator.end()V` |
| 22 | method | `com/mojang/blaze3d/vertex/BufferUploader.drawWithShader(Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;)V` |
| 21 | method | `com/mojang/blaze3d/vertex/VertexConsumer.overlayCoords(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;` |
| 19 | method | `com/mojang/blaze3d/vertex/BufferBuilder.vertex(DDD)Lcom/mojang/blaze3d/vertex/VertexConsumer;` |
| 18 | method | `com/mojang/blaze3d/vertex/VertexConsumer.normal(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;` |
| 17 | method | `com/mojang/blaze3d/systems/RenderSystem.getModelViewStack()Lcom/mojang/blaze3d/vertex/PoseStack;` |
| 17 | method | `com/mojang/blaze3d/vertex/VertexConsumer.vertex(DDD)Lcom/mojang/blaze3d/vertex/VertexConsumer;` |

### 3.5 Classes touched by the most abandoned content mods (a missing class or any missing member)

| Jars | Class | Area |
|---|---|---|
| 49 | `net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext` | Forge API |
| 42 | `net/minecraft/world/item/ItemStack` | Minecraft 1.20.1 to 1.21.1 change |
| 40 | `net/minecraftforge/registries/ForgeRegistries` | Forge API |
| 40 | `net/minecraftforge/registries/IForgeRegistry` | Forge API |
| 39 | `net/minecraft/resources/ResourceLocation` | Minecraft 1.20.1 to 1.21.1 change |
| 33 | `net/minecraft/world/effect/MobEffectInstance` | Minecraft 1.20.1 to 1.21.1 change |
| 32 | `net/minecraft/world/effect/MobEffects` | Minecraft 1.20.1 to 1.21.1 change |
| 32 | `net/minecraft/world/entity/player/Player` | Minecraft 1.20.1 to 1.21.1 change |
| 30 | `net/neoforged/fml/ModLoadingContext` | Forge API |
| 30 | `net/neoforged/neoforge/registries/DeferredRegister` | Forge API |
| 28 | `net/minecraft/world/entity/ai/attributes/Attributes` | Minecraft 1.20.1 to 1.21.1 change |
| 28 | `net/minecraftforge/network/simple/SimpleChannel` | Forge API |
| 27 | `net/minecraft/world/item/enchantment/EnchantmentHelper` | Minecraft 1.20.1 to 1.21.1 change |
| 26 | `net/minecraftforge/network/NetworkEvent$Context` | Forge API |
| 25 | `net/minecraft/world/entity/Entity` | Minecraft 1.20.1 to 1.21.1 change |
| 25 | `net/minecraft/world/entity/ai/attributes/AttributeModifier$Operation` | Minecraft 1.20.1 to 1.21.1 change |
| 24 | `net/minecraft/network/syncher/SynchedEntityData` | Minecraft 1.20.1 to 1.21.1 change |
| 24 | `net/minecraft/world/entity/ai/attributes/AttributeModifier` | Minecraft 1.20.1 to 1.21.1 change |
| 23 | `net/minecraft/world/entity/LivingEntity` | Minecraft 1.20.1 to 1.21.1 change |
| 23 | `net/minecraft/world/item/Item` | Minecraft 1.20.1 to 1.21.1 change |
| 22 | `net/minecraft/world/level/block/state/BlockBehaviour$Properties` | Minecraft 1.20.1 to 1.21.1 change |
| 22 | `net/minecraftforge/network/NetworkHooks` | Forge API |
| 22 | `net/minecraftforge/network/simple/IndexedMessageCodec$MessageHandler` | Forge API |
| 22 | `net/minecraftforge/registries/ForgeRegistries$Keys` | Forge API |
| 21 | `net/minecraft/world/item/alchemy/PotionUtils` | Minecraft 1.20.1 to 1.21.1 change |
| 21 | `net/minecraftforge/fml/DistExecutor` | Forge API |
| 20 | `net/minecraft/world/entity/MobType` | Minecraft 1.20.1 to 1.21.1 change |
| 20 | `net/minecraft/world/entity/ai/attributes/AttributeSupplier$Builder` | Minecraft 1.20.1 to 1.21.1 change |
| 20 | `net/minecraft/world/level/gameevent/GameEvent` | Minecraft 1.20.1 to 1.21.1 change |
| 20 | `net/minecraftforge/event/TickEvent$Phase` | Forge API |
| 20 | `net/minecraftforge/network/NetworkDirection` | Forge API |
| 19 | `com/mojang/blaze3d/vertex/VertexConsumer` | Rendering / client |
| 19 | `net/minecraft/world/item/enchantment/Enchantments` | Minecraft 1.20.1 to 1.21.1 change |
| 19 | `net/minecraft/world/level/Level` | Minecraft 1.20.1 to 1.21.1 change |
| 19 | `net/minecraftforge/common/ToolAction` | Forge API |
| 19 | `net/minecraftforge/common/ToolActions` | Forge API |
| 18 | `net/minecraft/world/food/FoodProperties$Builder` | Minecraft 1.20.1 to 1.21.1 change |
| 18 | `net/minecraft/world/item/alchemy/Potions` | Minecraft 1.20.1 to 1.21.1 change |
| 18 | `net/minecraft/world/item/enchantment/Enchantment` | Minecraft 1.20.1 to 1.21.1 change |
| 18 | `net/minecraft/world/level/block/Block` | Minecraft 1.20.1 to 1.21.1 change |

### 3.6 Reading the numbers

- **Forge API (facades, agent B).** In the abandoned set: `FMLJavaModLoadingContext`/mod event bus and `ModLoadingContext.registerConfig` (about 50 of 72 jars),
  `ForgeRegistries`/`IForgeRegistry`/`DeferredRegister.create(IForgeRegistry, String)`/`RegistryObject` (40), networking (`SimpleChannel`, `NetworkEvent$Context`,
  `NetworkDirection`, `NetworkHooks`, `PacketDistributor`, `IndexedMessageCodec$MessageHandler`: 20 to 28), `DistExecutor`/`ToolAction(s)`/`TickEvent.Phase` (19 to 21),
  `LazyOptional`/`Capability` (17 to 18), `ForgeHooks` (16).
- **Minecraft changes (agent C).** `ResourceLocation(String)` (39), `ItemStack.hurtAndBreak(int, LivingEntity, Consumer)` (29), item NBT `getTag/getOrCreateTag/hasTag/setTag/save/of`
  (19 to 25), attribute holders (`Attributes.*` fields, `AttributeModifier(UUID, ...)`, `AttributeModifier$Operation.ADDITION`, `AttributeSupplier$Builder.add`: 20 to 24),
  `MobEffectInstance` constructors and `getEffect()` with `MobEffects.*` (22 to 23, holders), `SynchedEntityData.define` (24), `Player.broadcastBreakEvent` (24),
  `BlockBehaviour$Properties.copy(BlockBehaviour)` (22), `PotionUtils` (21), `MobType` (20), `EnchantmentHelper`, `FriendlyByteBuf`, `GameEvent` (all jars 27 to 45).
- **Rendering.** `VertexConsumer.endVertex/uv/color/vertex(Matrix4f, ..)`, `BufferBuilder.begin/end`, `Tesselator.getBuilder` appear in 19 abandoned mods and 36 to 49 of all
  jars; a single rewrite fixes most (the client part of the corpus is dominated by it).
- The single most common symbols are all small: a facade or an invoke rewrite closes `FMLJavaModLoadingContext`, `ResourceLocation(String)`, `ForgeRegistries`, `DeferredRegister.create`,
  `hurtAndBreak`, the attribute fields. Item NBT and capabilities are the two that are central and semantic.

## 4. Mixins

Mixin classes counted from the declared mixin configs.

| Mixin classes | All 140 jars | Abandoned content mods (72) |
|---|---|---|
| 0 | 46 | 30 |
| 1-5 | 36 | 22 |
| 6+ | 58 | 20 |

Rule of thumb for the pipeline: 0 mixins is a pure API port (the target for milestones 1 and 2); 1 to 5 mixins are usually one or two vanilla hooks (a failed mixin
should disable only that hook with a log line); 6+ mixins are the heavy mods (Steam 'n' Rails 192 mixin classes, Botania 83, Supplementaries 107, Quark 116, FancyMenu 102,
Alex's Caves 52, Goety Revelation 42) and should be skipped with a message until the mixin work (section 6 of the Estimate) is done.

## 5. Which jars are cheap, medium, heavy

Classification (tools/compat/gap1201_rank.py): gap = Forge + Minecraft + client symbols that do not resolve, library residue ignored (it vanishes when the library is migrated).
**cheap** = at most 40 such symbols and at most 1 mixin class; **medium** = at most 300 and at most 5 mixin classes; **heavy** = everything else; **data only** = no code references at all.
Abandoned content mods (72), cheapest first:

| Jar | Mod id | Version | KB | Refs | Mixin classes | Forge / MC / client gaps | Libs | Class | Port candidates (unverified) |
|---|---|---|---|---|---|---|---|---|---|
| tru.e-ending-v1.1.0c.jar | mr_limesplatus_ending | 1-v1.1.0c | 5396 | 0 | 0 | 0 / 0 / 0 |  | data only |  |
| letsdo-addon-structures-1.7.2.jar | letsdoaddonstructures | 1.7.2 | 322 | 0 | 0 | 0 / 0 / 0 | meadow, vinery | data only |  |
| create-structures-0.1.1-1.20.1-FORGE.jar | create_structures | 0.1.1 | 473 | 0 | 0 | 0 / 0 / 0 | create | data only | create-structures-nikrecs, create-structures-arise |
| ctov-friends-and-foes-compat-2.0.jar | mr_ctov_friendsandfoescompat | 2.0 | 63 | 0 | 0 | 0 / 0 / 0 |  | data only |  |
| ctov-beautify-compat-2.0.jar | mr_ctov_beautifycompat | 2.0 | 103 | 0 | 0 | 0 / 0 / 0 |  | data only |  |
| vanilla-refresh-1.4.19h.jar | mr_vanilla_refresh | 1.4.19h | 1857 | 0 | 0 | 0 / 0 / 0 |  | data only |  |
| anytag-1.0.0.jar | mr_anytag | 1.0.0 | 542 | 0 | 0 | 0 / 0 / 0 |  | data only |  |
| ctov-chefs-delight-compat-2.0.jar | mr_ctov_chefsdelightcompat | 2.0 | 97 | 0 | 0 | 0 / 0 / 0 |  | data only |  |
| global-server-config-forge-1.18.2+-1.0.jar | global_server_config | ${file.jarVersion} | 3 | 33 | 1 | 0 / 0 / 0 |  | cheap |  |
| From-The-Fog-1.20-v1.9.2-Forge-Fabric.jar | watching | 1.9.2 | 4638 | 14 | 0 | 0 / 0 / 0 |  | cheap |  |
| fullstackwatchdog-1.0.1+1.19.2-forge.jar | fullstackwatchdog | 1.0.1+1.19.2-forge | 14 | 215 | 1 | 0 / 0 / 0 |  | cheap |  |
| MyServerIsCompatible-1.20-1.0.jar | myserveriscompatible | 1.0 | 3 | 9 | 1 | 0 / 0 / 0 |  | cheap |  |
| lradd-1.20.1-0.3.0.jar | lradd | 0.3.0 | 41496 | 27 | 0 | 1 / 0 / 0 | tacz | cheap |  |
| ClearDespawn-forge-1.20.2-1.1.15.jar | cleardespawn | ${file.jarVersion} | 19 | 230 | 0 | 1 / 0 / 0 |  | cheap | cleardespawn-reborn |
| taczdd-0.1.1.jar | taczdd | 0.1.1 | 16 | 810 | 0 | 2 / 0 / 0 | tacz, tslatentitystatus | cheap |  |
| calcmod-forge-1.20.1-1.3.0.jar | calcmod | 1.3.0 | 516 | 88131 | 0 | 0 / 2 / 0 |  | cheap | calcmod-neoforged |
| FriendlyFire-Forge-1.20.1-18.0.8.jar | friendlyfire | 18.0.8 | 25 | 488 | 0 | 2 / 0 / 0 | bookshelf | cheap | no-friendly-fire, zonis-mob-friendly-fire |
| loadmyresources_forge_1.0.4_MC_1.19.3.jar | loadmyresources | 1.0.4 | 31 | 2889 | 0 | 1 / 3 / 0 |  | cheap |  |
| Grass_Overhaul-Forge-23.10.10-MC1.20.1.jar | grassoverhaul | 23.10.10 | 115 | 1750 | 0 | 4 / 2 / 0 |  | cheap |  |
| [MC-1.20] Hide Key Binding v1.0.0.7 - 2025-3-31.jar | hide_key_binding | 1.0.0.7 | 28 | 997 | 0 | 6 / 0 / 2 |  | cheap |  |
| eureka-1201-1.6.4.jar | vs_eureka | 1.6.4 | 456 | 12922 | 1 | 5 / 6 / 0 | valkyrienskies | cheap |  |
| emitrades-forge-1.2.1+mc1.20.1.jar | emitrades | 1.2.1+mc1.20.1 | 52 | 2034 | 0 | 0 / 11 / 1 | emi | cheap | emi-trades-reborn |
| EMIEnchants-forge-1.19.3-1.20.4-1.0.0.jar | emienchants | 1.0.0 | 32 | 931 | 1 | 0 / 12 / 0 | emi | cheap |  |
| ToolStats-Forge-1.20.1-16.0.10.jar | toolstats | 16.0.10 | 21 | 874 | 0 | 2 / 11 / 0 | bookshelf | cheap | advanced-tool-stats |
| bbb-forge-2.0pre4.jar | bbb | 2.0pre4 | 1846 | 36067 | 1 | 1 / 14 / 1 | architectury | cheap |  |
| letsdo-nethervinery-forge-1.2.19.jar | nethervinery | 1.2.19 | 635 | 5665 | 0 | 1 / 19 / 0 | architectury, vinery | cheap |  |
| enchanted_arsenal-1.0.1-forge-1.20.1.jar | enchanted_arsenal | 1.0.1 | 2123 | 1678 | 0 | 12 / 16 / 0 |  | cheap |  |
| ironchests-5.0.2-forge.jar | ironchests | 5.0.2 | 480 | 7071 | 0 | 7 / 28 / 2 |  | cheap |  |
| GeckoLibOculusCompat-Forge-1.0.1.jar | geckoanimfix | 1.0.0 | 6 | 113 | 2 | 0 / 0 / 1 |  | medium |  |
| cavedust-2.0.4-1.20.1-forge.jar | cavedust | 2.0.4 | 31 | 2100 | 2 | 4 / 1 / 0 | architectury | medium | cave-dust-rethinking |
| bulktrade-1.0.0.jar | bulktrade | 1.0.0 | 10 | 210 | 3 | 4 / 1 / 0 |  | medium | bulk-trade, bulk-villager-trading-reborn |
| betternethermap-1.2-1.20.1.jar | betternethermap | 1.2-1.20.1 | 41 | 1299 | 3 | 6 / 2 / 0 |  | medium |  |
| hearth_and_home-forge-1.20.1-2.0.3.jar | hearth_and_home | 1.20.1-2.0.3 | 1154 | 9036 | 2 | 4 / 5 / 0 |  | medium |  |
| customvillagertrades-forge-20.25.0.jar | customvillagertrades | 20.25.0 | 54 | 4790 | 2 | 3 / 11 / 0 | architectury | medium |  |
| veinmining-forge-1.5.0+1.20.1.jar | veinmining | 1.5.0+1.20.1 | 224 | 4061 | 2 | 21 / 7 / 0 |  | medium |  |
| constructionwand-1.20.1-2.11.jar | constructionwand | ${file.jarVersion} | 205 | 8073 | 0 | 24 / 18 / 0 |  | medium | construction-wand-kots, construction-wands-revived |
| takesapillage-1.0.3-1.20.1.jar | takesapillage | 1.0.3 | 3670 | 6972 | 3 | 9 / 38 / 3 |  | medium | it-takes-a-pillage-continuation, big-globe-it-takes-a-pillage-compatibility |
| decorative_blocks-forge-1.20.1-4.1.3.jar | decorative_blocks | 4.1.3 | 560 | 12645 | 5 | 22 / 30 / 0 |  | medium | decorative-blocks-reborn, more-decorative-blocks |
| mythicmounts-20.1-7.4.2-forge.jar | mythicmounts | ${file.jarVersion} | 4233 | 20741 | 1 | 19 / 36 / 1 |  | medium | mythic_mounts |
| mischief_illagers-1.2-forge-1.20.1.jar | mischief_illagers | 1.2 | 1716 | 31271 | 0 | 18 / 46 / 1 |  | medium |  |
| meetyourfight-1.20.1-1.6.2.jar | meetyourfight | ${file.jarVersion} | 23121 | 22453 | 0 | 14 / 56 / 13 | curios | medium |  |
| diet-forge-2.1.1+1.20.1.jar | diet | 2.1.1+1.20.1 | 359 | 11392 | 4 | 43 / 31 / 11 |  | medium | origins-dietary-delights, diet-appleseed-edition |
| nethersdelight-1.20.1-4.0.jar | nethersdelight | 1.20.1-4.0 | 357 | 13897 | 4 | 14 / 72 / 0 | farmersdelight | medium | my-nethers-delight |
| SpartanShields-1.20.1-forge-3.1.1.jar | spartanshields | 3.1.1 | 527 | 16474 | 0 | 32 / 57 / 4 |  | medium | spartan-shields-unofficial, spartan-shields-reborn |
| The-Man-From-The-Fog-1.4-1.20.1.jar | man | 1.4 | 3681 | 18047 | 0 | 44 / 50 / 1 |  | medium |  |
| illageandspillagerespillaged-1.2.8.jar | illageandspillage | 1.2.8 | 32791 | 214285 | 3 | 30 / 85 / 19 |  | medium |  |
| structure_gel-1.20.1-2.16.2.jar | structure_gel | ${file.jarVersion} | 943 | 56412 | 0 | 46 / 73 / 19 |  | medium |  |
| Delightful-1.20.1-3.8.1.jar | delightful | ${file.jarVersion} | 1732 | 50830 | 3 | 50 / 88 / 0 | farmersdelight | medium | delightful-creators, delightful-fares |
| eeeabsmobs-1.20.1-0.98.1.jar | eeeabsmobs | 1.20.1-0.98.1 | 28736 | 310101 | 1 | 69 / 162 / 27 |  | medium |  |
| textrues_embeddium_options-0.1.5+mc1.20.1.jar | textrues_embeddium_options | 0.1.5+mc1.20.1 | 159 | 5581 | 8 | 0 / 2 / 2 | embeddium | heavy |  |
| dynamictrim-1.4.1-forge+1.20.1.jar | dynamictrim | 1.4.1 | 286 | 2351 | 25 | 3 / 2 / 0 | architectury | heavy | dynamic-trims |
| moremobvariants-forge+1.20.1-1.3.0.1.jar | moremobvariants | 1.3.0.1 | 371 | 8711 | 25 | 12 / 2 / 2 |  | heavy | more-mob-variants-modernized, more-mob-variants-renewed |
| brb-1.10.0-rc5+1.20.0-1.jar | brb | 1.10.0-rc5+1.20.0-1 | 1211 | 8685 | 40 | 0 / 17 / 9 | architectury | heavy | better-recipe-book-extended |
| Decorative Blocks-forge-1.20.1-4.0.2.jar | decorative_blocks | ${file.jarVersion} | 565 | 13271 | 6 | 21 / 30 / 0 |  | heavy | decorative-blocks-reborn, more-decorative-blocks |
| immersive_weathering-1.20.1-2.0.5-forge.jar | immersive_weathering | 1.20.1-2.0.5 | 2215 | 53136 | 31 | 7 / 57 / 0 | moonlight | heavy | immersive-weather-renewed |
| swampier_swamps-forge-1.20-4.0.0.jar | swampier_swamps | 1.20-4.0.0 | 699 | 12765 | 15 | 23 / 35 / 9 |  | heavy |  |
| bygonenether-1.3.2-1.20.x.jar | bygonenether | 1.3.2 | 7463 | 15196 | 6 | 11 / 57 / 4 |  | heavy |  |
| GoetyRevelation-2.3.6.jar | goety_revelation | 2.3.6 | 18275 | 16154 | 42 | 11 / 39 / 22 | goety | heavy |  |
| lrtactical-1.20.1-0.4.3.jar | lrtactical | 0.4.3 | 6459 | 25233 | 8 | 58 / 67 / 6 | tacz | heavy |  |
| lost_aether_content-1.20.1-1.2.3.jar | lost_aether_content | ${file.jarVersion} | 19789 | 20959 | 12 | 58 / 102 / 16 | aether, structure_gel | heavy |  |
| Steam_Rails-1.7.3+forge-mc1.20.1.jar | railways | 1.7.3+forge-mc1.20.1 | 16642 | 128251 | 192 | 52 / 108 / 20 | create | heavy |  |
| endergetic-1.20.1-5.0.1.jar | endergetic | ${file.jarVersion} | 15246 | 135884 | 22 | 45 / 136 / 16 | blueprint | heavy |  |
| SpartanWeaponry-1.20.1-forge-3.2.1-all.jar | spartanweaponry | 3.2.1 | 2767 | 78661 | 12 | 70 / 168 / 4 |  | heavy | spartan-weaponry-unofficial, spartan-weaponry-reborn |
| species-3.5.jar | species | 3.5 | 41127 | 210820 | 25 | 51 / 198 / 22 |  | heavy | spicy_species, species-rpg |
| soulslike-weaponry-1.4.10-1.20.1-forge.jar | soulsweapons | 1.4.10-1.20.1-forge} | 53474 | 153882 | 28 | 49 / 255 / 12 | attributefix, geckolib, ranged_weapon_api | heavy |  |
| alexsmobs-1.22.9.jar | alexsmobs | 1.22.9 | 25733 | 380397 | 0 | 57 / 264 / 34 | citadel | heavy | alexs-mobs-1.21.1, alexs-mobs-continued |
| iceandfire-2.1.13-1.20.1-beta-5.jar | iceandfire | ${file.jarVersion} | 21341 | 263052 | 2 | 78 / 253 / 33 | citadel | heavy | iceandfire_smithing, iceandfireaddon |
| EnigmaticLegacy-2.30.1.jar | enigmaticlegacy | 2.30.1 | 7106 | 81079 | 33 | 75 / 287 / 19 | caelus, curios, patchouli | heavy | enigmaticlegacy-plus |
| alexscaves-2.0.2.jar | alexscaves | 2.0.2 | 73116 | 390589 | 52 | 71 / 329 / 40 | citadel | heavy | alexs-caves-unofficial-port, alexs-caves-continued |
| blue_skies-1.20.1-1.3.31.jar | blue_skies | ${file.jarVersion} | 81605 | 233143 | 18 | 100 / 364 / 31 | structure_gel | heavy | blue-skies-and-black-smoke |
| Botania-1.20.1-456-FORGE.jar | botania | ${file.jarVersion} | 11939 | 265102 | 83 | 79 / 428 / 40 | curios, patchouli | heavy | ars-botania |
| TConstruct-1.20.1-3.12.1.231.jar | tconstruct | 3.12.1.231 | 23310 | 473706 | 0 | 187 / 464 / 36 | mantle | heavy | hexa-tinkers-construct-unofficial-port |



### 5.1 Libraries and dependencies the corpus needs (all ported to 1.21.1 upstream, but the 1.20.1 versions must load)

Citadel (Alex's Mobs/Caves, Ice and Fire), Architectury (bbb, brb, cavedust, customvillagertrades, dynamictrim, nethervinery), Bookshelf (FriendlyFire, ToolStats), Curios, Patchouli, Mantle
(Tinkers' Construct), Structure Gel (Blue Skies, Lost Aether Content, Structure Gel), Moonlight (Immersive Weathering), Farmer's Delight (Delightful, Nether's Delight, Chef's Delight),
Create (Steam 'n' Rails, Create Structures), Blueprint (Endergetic), Valkyrien Skies (Eureka), TACZ (lradd, lrtactical, taczdd), GeckoLib, Caelus, EMI (EMIEnchants, EMI Trades).
Gap report of each library jar is in the table of section 7; the libraries with few symbols (Bookshelf 143, Architectury 168, Curios 157, Patchouli 78, GeckoLib 112,
Cloth Config 31, Kotlin for Forge 0) are cheap relative to their reach, Mantle (529), Citadel (154 but 1,419 classes), Create (2,084) and Mekanism (1,161) are not.

## 6. Recommended order

**Agent B (Forge API facades), in this order, each closes the listed share of the 72 abandoned mods:**
1. Mod loading: `FMLJavaModLoadingContext`, `getModEventBus`, `ModLoadingContext.registerConfig` (ForgeConfigSpec to ModConfigSpec), `@Mod` constructor injection, `NonNull*`, `DistExecutor`, `LogicalSide`. About 50 jars; first mods load here (data-only and the cheap list need nothing else).
2. Registries: `ForgeRegistries` (+`$Keys`), `IForgeRegistry`, `RegistryObject`, `DeferredRegister.create(IForgeRegistry, String)`, `RegisterEvent` hooks. 40 jars.
3. Events: `TickEvent.*` (Phase, Client/Player/Level), `LivingEvent.LivingTickEvent`, `LivingHurtEvent`/`LivingAttackEvent`, `ForgeEventFactory`, `ToolAction(s)`, `ForgeHooks`, `AttachCapabilitiesEvent`. 16 to 21 jars each.
4. Networking: `SimpleChannel`, `NetworkEvent.Context`, `NetworkDirection`, `NetworkHooks`, `PacketDistributor`, `IndexedMessageCodec`. 20 to 28 abandoned jars, 40 to 57 of all.
5. Capabilities: `LazyOptional`, `Capability`, `CapabilityManager`/`CapabilityToken`, `ForgeCapabilities`, `RegisterCapabilitiesEvent`. 17 to 24 jars.
6. Client-side Forge: `ConfigScreenHandler$ConfigScreenFactory`, `ForgeGui`/`VanillaGuiOverlay`/`NamedGuiOverlay`, `ForgeHooksClient`, `ForgeSpawnEggItem`, `IModInfo`/`IModFileInfo`/`IModFile`/`LoadingModList` (spi classes).

**Agent C (Minecraft 1.20.1 to 1.21.1), in this order:**
1. Pure rewrites: `new ResourceLocation(String)` / `(String, String)` (39 jars), `Tesselator`/`VertexConsumer`/`BufferBuilder` calls (19 abandoned, 49 total), `Minecraft.getFrameTime`, `ModelPart.render`, `Level.gameEvent`/`GameEvent`.
2. Holders and data-driven registries as helpers: `Attributes.*` and `MobEffects.*` fields (holders), `MobEffectInstance` constructors and `getEffect`, `AttributeModifier(UUID, ...)` + `Operation` enum rename, `AttributeSupplier$Builder.add`, `Enchantments.*`/`EnchantmentHelper`, `PotionUtils`, `MobType`.
3. `ItemStack` NBT (`getTag/getOrCreateTag/hasTag/setTag/save/of`, `hurtAndBreak(int, LivingEntity, Consumer)`, `broadcastBreakEvent`) onto components. Central and lossy, start with `CUSTOM_DATA` pass-through.
4. Bridge methods for overridden hooks: `BlockEntity.load/saveAdditional`, `Block.use`, `SynchedEntityData.define`/`defineSynchedData`, `Container` recipes, `BlockBehaviour.Properties.copy`.
5. Mixin triage last, using the mixin-class counts above (the 0 and 1 to 5 mixin jars first).

**Targets to prove each step (all abandoned, cheap, from the list):** data only (Vanilla Refresh, CTOV compat packs, Tru.E Ending, anytag: need metadata and resource conversion only), then
ClearDespawn, Friendly Fire, calcmod, Grass Overhaul, Hide Key Binding, EMI Trades, ToolStats, Iron Chests (1 to 40 symbols), then Nether's Delight/Delightful (need Farmer's Delight 1.20.1), Spartan
Shields/Weaponry, Decorative Blocks, Mischief Illagers, Meet Your Fight. Do not start with Botania, Tinkers', Alex's Caves/Mobs, Ice and Fire, Blue Skies (heavy: 250 to 690 Forge/Minecraft symbols each and, for four of them, 50 to 83 mixin classes).

## 6.1 Probe baseline

`python tools/compat/probe.py test-mods-1201 --mc 1.20.1 --version 0.4.0-1201-baseline` (new `gap1201` path of `probe.py`) recorded the run before any facade exists in
`docs/data/probe.json` and 135 mods as probe rows in `docs/data/mods.json`: **14 links** (data packs and tiny mods), 117 need Minecraft/Forge features the layer does not provide yet,
3 need another mod's API, 4 only miss JDK-level code. It is a renaming-and-link check, never proof that a mod works. Re-run it after each of the B/C
landings (`--fail-on-regression` guards against going backwards); the pie of the version appears on `docs/probe.html`.

## 7. Appendix: all corpus jars

Generated by `tools/compat/corpus1201.py` (`test-mods-1201/index.md` has the same table with the dependency column).

| Jar | Mod id | Version | KB | Classes | Mixin classes | Group | 1.21.1 NeoForge exists | Mandatory deps |
|---|---|---|---|---|---|---|---|---|
| [MC-1.20] Hide Key Binding v1.0.0.7 - 2025-3-31.jar | hide_key_binding | 1.0.0.7 | 28 | 10 | 0 | content | no |  |
| alexscaves-2.0.2.jar | alexscaves | 2.0.2 | 73116 | 1303 | 52 | content | no | citadel |
| alexsmobs-1.22.9.jar | alexsmobs | 1.22.9 | 25733 | 1118 | 0 | content | no | citadel |
| anytag-1.0.0.jar | mr_anytag | 1.0.0 | 542 | 0 | 0 | content | no |  |
| bbb-forge-2.0pre4.jar | bbb | 2.0pre4 | 1846 | 73 | 1 | content | no | architectury |
| betternethermap-1.2-1.20.1.jar | betternethermap | 1.2-1.20.1 | 41 | 10 | 3 | content | no |  |
| blue_skies-1.20.1-1.3.31.jar | blue_skies | ${file.jarVersion} | 81605 | 1040 | 18 | content | no | structure_gel |
| Botania-1.20.1-456-FORGE.jar | botania | ${file.jarVersion} | 11939 | 1372 | 83 | content | no | curios, patchouli |
| brb-1.10.0-rc5+1.20.0-1.jar | brb | 1.10.0-rc5+1.20.0-1 | 1211 | 100 | 40 | content | no | architectury |
| bulktrade-1.0.0.jar | bulktrade | 1.0.0 | 10 | 6 | 3 | content | no |  |
| bygonenether-1.3.2-1.20.x.jar | bygonenether | 1.3.2 | 7463 | 81 | 6 | content | no |  |
| calcmod-forge-1.20.1-1.3.0.jar | calcmod | 1.3.0 | 516 | 116 | 0 | content | no |  |
| cavedust-2.0.4-1.20.1-forge.jar | cavedust | 2.0.4 | 31 | 17 | 2 | content | no | architectury |
| ClearDespawn-forge-1.20.2-1.1.15.jar | cleardespawn | ${file.jarVersion} | 19 | 10 | 0 | content | no |  |
| constructionwand-1.20.1-2.11.jar | constructionwand | ${file.jarVersion} | 205 | 72 | 0 | content | no |  |
| create-structures-0.1.1-1.20.1-FORGE.jar | create_structures | 0.1.1 | 473 | 0 | 0 | content | no | create |
| ctov-beautify-compat-2.0.jar | mr_ctov_beautifycompat | 2.0 | 103 | 0 | 0 | content | no |  |
| ctov-chefs-delight-compat-2.0.jar | mr_ctov_chefsdelightcompat | 2.0 | 97 | 0 | 0 | content | no |  |
| ctov-friends-and-foes-compat-2.0.jar | mr_ctov_friendsandfoescompat | 2.0 | 63 | 0 | 0 | content | no |  |
| customvillagertrades-forge-20.25.0.jar | customvillagertrades | 20.25.0 | 54 | 23 | 2 | content | no | architectury |
| Decorative Blocks-forge-1.20.1-4.0.2.jar | decorative_blocks | ${file.jarVersion} | 565 | 120 | 6 | content | no |  |
| decorative_blocks-forge-1.20.1-4.1.3.jar | decorative_blocks | 4.1.3 | 560 | 122 | 5 | content | no |  |
| Delightful-1.20.1-3.8.1.jar | delightful | ${file.jarVersion} | 1732 | 218 | 3 | content | no | farmersdelight |
| diet-forge-2.1.1+1.20.1.jar | diet | 2.1.1+1.20.1 | 359 | 97 | 4 | content | no |  |
| dynamictrim-1.4.1-forge+1.20.1.jar | dynamictrim | 1.4.1 | 286 | 44 | 25 | content | no | architectury |
| eeeabsmobs-1.20.1-0.98.1.jar | eeeabsmobs | 1.20.1-0.98.1 | 28736 | 674 | 1 | content | no |  |
| EMIEnchants-forge-1.19.3-1.20.4-1.0.0.jar | emienchants | 1.0.0 | 32 | 7 | 1 | content | no | emi |
| emitrades-forge-1.2.1+mc1.20.1.jar | emitrades | 1.2.1+mc1.20.1 | 52 | 12 | 0 | content | no | emi |
| enchanted_arsenal-1.0.1-forge-1.20.1.jar | enchanted_arsenal | 1.0.1 | 2123 | 19 | 0 | content | no |  |
| endergetic-1.20.1-5.0.1.jar | endergetic | ${file.jarVersion} | 15246 | 410 | 22 | content | no | blueprint |
| EnigmaticLegacy-2.30.1.jar | enigmaticlegacy | 2.30.1 | 7106 | 395 | 33 | content | no | caelus, curios, patchouli |
| eureka-1201-1.6.4.jar | vs_eureka | 1.6.4 | 456 | 98 | 1 | content | no | valkyrienskies |
| FriendlyFire-Forge-1.20.1-18.0.8.jar | friendlyfire | 18.0.8 | 25 | 4 | 0 | content | no | bookshelf |
| From-The-Fog-1.20-v1.9.2-Forge-Fabric.jar | watching | 1.9.2 | 4638 | 1 | 0 | content | no |  |
| fullstackwatchdog-1.0.1+1.19.2-forge.jar | fullstackwatchdog | 1.0.1+1.19.2-forge | 14 | 5 | 1 | content | no |  |
| GeckoLibOculusCompat-Forge-1.0.1.jar | geckoanimfix | 1.0.0 | 6 | 2 | 2 | content | no |  |
| global-server-config-forge-1.18.2+-1.0.jar | global_server_config | ${file.jarVersion} | 3 | 2 | 1 | content | no |  |
| GoetyRevelation-2.3.6.jar | goety_revelation | 2.3.6 | 18275 | 112 | 42 | content | no | goety |
| Grass_Overhaul-Forge-23.10.10-MC1.20.1.jar | grassoverhaul | 23.10.10 | 115 | 13 | 0 | content | no |  |
| hearth_and_home-forge-1.20.1-2.0.3.jar | hearth_and_home | 1.20.1-2.0.3 | 1154 | 38 | 2 | content | no |  |
| iceandfire-2.1.13-1.20.1-beta-5.jar | iceandfire | ${file.jarVersion} | 21341 | 932 | 2 | content | no | citadel |
| illageandspillagerespillaged-1.2.8.jar | illageandspillage | 1.2.8 | 32791 | 374 | 3 | content | no |  |
| immersive_weathering-1.20.1-2.0.5-forge.jar | immersive_weathering | 1.20.1-2.0.5 | 2215 | 271 | 31 | content | no | moonlight |
| ironchests-5.0.2-forge.jar | ironchests | 5.0.2 | 480 | 66 | 0 | content | no |  |
| letsdo-addon-structures-1.7.2.jar | letsdoaddonstructures | 1.7.2 | 322 | 0 | 0 | content | no | meadow, vinery |
| letsdo-nethervinery-forge-1.2.19.jar | nethervinery | 1.2.19 | 635 | 40 | 0 | content | no | architectury, vinery |
| loadmyresources_forge_1.0.4_MC_1.19.3.jar | loadmyresources | 1.0.4 | 31 | 12 | 0 | content | no |  |
| lost_aether_content-1.20.1-1.2.3.jar | lost_aether_content | ${file.jarVersion} | 19789 | 129 | 12 | content | no | aether, structure_gel |
| lradd-1.20.1-0.3.0.jar | lradd | 0.3.0 | 41496 | 1 | 0 | content | no | tacz |
| lrtactical-1.20.1-0.4.3.jar | lrtactical | 0.4.3 | 6459 | 210 | 8 | content | no | tacz |
| meetyourfight-1.20.1-1.6.2.jar | meetyourfight | ${file.jarVersion} | 23121 | 106 | 0 | content | no | curios |
| mischief_illagers-1.2-forge-1.20.1.jar | mischief_illagers | 1.2 | 1716 | 172 | 0 | content | no |  |
| moremobvariants-forge+1.20.1-1.3.0.1.jar | moremobvariants | 1.3.0.1 | 371 | 58 | 25 | content | no |  |
| MyServerIsCompatible-1.20-1.0.jar | myserveriscompatible | 1.0 | 3 | 2 | 1 | content | no |  |
| mythicmounts-20.1-7.4.2-forge.jar | mythicmounts | ${file.jarVersion} | 4233 | 77 | 1 | content | no |  |
| nethersdelight-1.20.1-4.0.jar | nethersdelight | 1.20.1-4.0 | 357 | 66 | 4 | content | no | farmersdelight |
| soulslike-weaponry-1.4.10-1.20.1-forge.jar | soulsweapons | 1.4.10-1.20.1-forge} | 53474 | 780 | 28 | content | no | attributefix, geckolib, ranged_weapon_api |
| SpartanShields-1.20.1-forge-3.1.1.jar | spartanshields | 3.1.1 | 527 | 78 | 0 | content | no |  |
| SpartanWeaponry-1.20.1-forge-3.2.1-all.jar | spartanweaponry | 3.2.1 | 2767 | 317 | 12 | content | no |  |
| species-3.5.jar | species | 3.5 | 41127 | 471 | 25 | content | no |  |
| Steam_Rails-1.7.3+forge-mc1.20.1.jar | railways | 1.7.3+forge-mc1.20.1 | 16642 | 959 | 192 | content | no | create |
| structure_gel-1.20.1-2.16.2.jar | structure_gel | ${file.jarVersion} | 943 | 325 | 0 | content | no |  |
| swampier_swamps-forge-1.20-4.0.0.jar | swampier_swamps | 1.20-4.0.0 | 699 | 87 | 15 | content | no |  |
| taczdd-0.1.1.jar | taczdd | 0.1.1 | 16 | 8 | 0 | content | no | tacz, tslatentitystatus |
| takesapillage-1.0.3-1.20.1.jar | takesapillage | 1.0.3 | 3670 | 48 | 3 | content | no |  |
| TConstruct-1.20.1-3.12.1.231.jar | tconstruct | 3.12.1.231 | 23310 | 2695 | 0 | content | no | mantle |
| textrues_embeddium_options-0.1.5+mc1.20.1.jar | textrues_embeddium_options | 0.1.5+mc1.20.1 | 159 | 32 | 8 | content | no | embeddium |
| The-Man-From-The-Fog-1.4-1.20.1.jar | man | 1.4 | 3681 | 142 | 0 | content | no |  |
| ToolStats-Forge-1.20.1-16.0.10.jar | toolstats | 16.0.10 | 21 | 4 | 0 | content | no | bookshelf |
| tru.e-ending-v1.1.0c.jar | mr_limesplatus_ending | 1-v1.1.0c | 5396 | 0 | 0 | content | no |  |
| vanilla-refresh-1.4.19h.jar | mr_vanilla_refresh | 1.4.19h | 1857 | 0 | 0 | content | no |  |
| veinmining-forge-1.5.0+1.20.1.jar | veinmining | 1.5.0+1.20.1 | 224 | 40 | 2 | content | no |  |
| AmbientSounds_FORGE_v6.3.11_mc1.20.1.jar | ambientsounds | 6.3.11 | 52180 | 65 | 2 | content | yes | creativecore |
| appleskin-forge-mc1.20.1-2.5.1.jar | appleskin | ${file.jarVersion} | 46 | 29 | 0 | content | yes |  |
| BiomesOPlenty-forge-1.20.1-19.0.0.96.jar | biomesoplenty | 19.0.0.96 | 21856 | 325 | 2 | content | yes | glitchcore, terrablender |
| chat_heads-0.15.7-forge-1.20.jar | chat_heads | 0.15.7 | 707 | 45 | 20 | content | yes |  |
| Clumps-forge-1.20.1-12.0.0.4.jar | clumps | ${file.jarVersion} | 19 | 15 | 2 | content | yes |  |
| Controlling-forge-1.20.1-12.0.2.jar | controlling | ${file.jarVersion} | 112 | 39 | 5 | content | yes | searchables |
| EnchantmentDescriptions-Forge-1.20.1-17.1.21.jar | enchdesc | 17.1.21 | 65 | 6 | 0 | content | yes | bookshelf |
| entity_model_features-3.3.12-1.20.1-forge.jar | entity_model_features | 3.3.12 | 820 | 282 | 64 | content | yes | entity_texture_features |
| entity_texture_features-7.2.7-1.20.1-forge.jar | entity_texture_features | 7.2.7 | 980 | 268 | 60 | content | yes |  |
| Essential_1-5-2_forge_1-20-1.jar | ? | ? | 50302 | 6 | 0 | content | yes |  |
| fancymenu_forge_3.9.14_MC_1.20.1.jar | fancymenu | 3.9.14 | 25578 | 2418 | 102 | content | yes | konkrete, melody |
| FarmersDelight-1.20.1-1.3.4.jar | farmersdelight | 1.20.1-1.3.4 | 3233 | 289 | 14 | content | yes |  |
| InventoryProfilesNext-forge-1.20-1.10.20.jar | inventoryprofilesnext | 1.10.20 | 1423 | 986 | 14 | content | yes | libipn |
| Mekanism-1.20.1-10.4.16.80.jar | mekanism | 10.4.16 | 12264 | 3141 | 0 | content | yes |  |
| MouseTweaks-forge-mc1.20.1-2.25.1.jar | mousetweaks | ${file.jarVersion} | 74 | 23 | 2 | content | yes |  |
| NoChatReports-FORGE-1.20.1-v2.2.3.jar | nochatreports | 1.20.1-v2.2.3 | 265 | 87 | 0 | content | yes |  |
| notenoughanimations-forge-1.12.7-mc1.20.1.jar | notenoughanimations | 1.12.7 | 1829 | 79 | 13 | content | yes |  |
| Quark-4.0-462.jar | quark | ${file.jarVersion} | 14585 | 936 | 116 | content | yes | zeta |
| shulkerboxtooltip-forge-4.0.4+1.20.1.jar | shulkerboxtooltip | 4.0.4+1.20.1 | 215 | 125 | 8 | content | yes | cloth_config |
| sophisticatedbackpacks-1.20.1-3.26.19.2239.jar | sophisticatedbackpacks | 3.26.19.2239 | 1203 | 376 | 1 | content | yes | sophisticatedcore |
| sound-physics-remastered-forge-1.20.1-1.5.1.jar | sound_physics_remastered | 1.20.1-1.5.1 | 202 | 91 | 11 | content | yes |  |
| supplementaries-1.20-3.1.44-forge.jar | supplementaries | 1.20-3.1.44 | 13365 | 1106 | 107 | content | yes | moonlight |
| veinminer-1.1.0.jar | mr_veinminer | 1.1.0 | 45 | 1 | 0 | content | yes |  |
| voicechat-forge-1.20.1-2.6.27.jar | voicechat | 1.20.1-2.6.27 | 4826 | 688 | 0 | content | yes |  |
| xaerominimap-forge-1.20.1-26.6.0.jar | xaerominimap | 26.6.0 | 2144 | 689 | 22 | content | yes | xaerolib |
| xaeroworldmap-forge-1.20.1-1.47.0.jar | xaeroworldmap | 1.47.0 | 1395 | 372 | 13 | content | yes | xaerolib |
| valkyrienskies-120-2.4.11.jar | valkyrienskies | 2.4.11 | 27102 | 3816 | 357 | dep | no |  |
| aether-1.20.1-1.5.2-neoforge.jar | aether | ${file.jarVersion} | 38814 | 977 | 65 | dep | yes | curios |
| architectury-9.2.14-forge.jar | architectury | 9.2.14 | 566 | 382 | 19 | dep | yes |  |
| AttributeFix-Forge-1.20.1-21.0.5.jar | attributefix | 21.0.5 | 19 | 9 | 1 | dep | yes |  |
| azurelib-neo-1.20.1-3.2.7.jar | azurelib | 3.2.7 | 1486 | 725 | 24 | dep | yes |  |
| balm-forge-1.20.1-7.3.44.jar | balm | 7.3.44 | 548 | 403 | 16 | lib | yes |  |
| blueprint-1.20.1-7.1.5.jar | blueprint | 7.1.5 | 1133 | 472 | 34 | dep | yes |  |
| Bookshelf-Forge-1.20.1-20.2.15.jar | bookshelf | 20.2.15 | 335 | 180 | 24 | dep | yes |  |
| caelus-forge-3.2.0+1.20.1.jar | caelus | 3.2.0+1.20.1 | 39 | 13 | 4 | dep | yes |  |
| chefs-delight-1.0.5-forge-1.20.1.jar | chefsdelight | ${file.jarVersion} | 101 | 8 | 0 | dep | yes | farmersdelight |
| citadel-2.6.3-1.20.1.jar | citadel | 2.6.3 | 3110 | 1419 | 16 | dep | yes |  |
| cloth-config-11.1.106-forge.jar | cloth_config | 11.1.106 | 1140 | 633 | 0 | dep | yes |  |
| collective-1.20.1-8.83.jar | collective | 8.83 | 1021 | 532 | 82 | lib | yes |  |
| create-1.20.1-6.0.8.jar | create | 6.0.8 | 18721 | 2687 | 57 | dep | yes | flywheel, ponder |
| CreativeCore_FORGE_v2.12.40_mc1.20.1.jar | creativecore | 2.12.40 | 1117 | 642 | 11 | dep | yes |  |
| ctov-3.3.3.jar | ctov | 3.3.3 | 4180 | 2 | 0 | dep | yes |  |
| curios-forge-5.14.1+1.20.1.jar | curios | ${file.jarVersion} | 388 | 143 | 13 | dep | yes |  |
| embeddium-0.3.31+mc1.20.1.jar | embeddium | 0.3.31+mc1.20.1 | 1289 | 602 | 0 | dep | yes |  |
| emi-1.1.24+1.20.1+forge.jar | emi | 1.1.24+1.20.1+forge | 1095 | 454 | 25 | dep | yes |  |
| geckolib-forge-1.20.1-4.8.4.jar | geckolib | 4.8.4 | 1013 | 260 | 1 | dep | yes |  |
| GlitchCore-forge-1.20.1-0.0.1.1.jar | glitchcore | 0.0.1.1 | 85 | 76 | 11 | dep | yes |  |
| jei-1.20.1-forge-15.74.0.234.jar | jei | 15.74.0.234 | 4693 | 2671 | 0 | lib | yes | mezz_config |
| konkrete_forge_1.8.0_MC_1.20-1.20.1.jar | konkrete | 1.8.0 | 610 | 399 | 5 | dep | yes |  |
| kotlinforforge-4.12.0-all.jar | ? | ? | 7268 | 4314 | 0 | dep | yes |  |
| letsdo-meadow-forge-1.3.25.jar | meadow | 1.3.25 | 2613 | 191 | 10 | dep | yes | architectury |
| letsdo-vinery-forge-1.4.41.jar | vinery | 1.4.41 | 1993 | 212 | 14 | dep | yes | architectury |
| libIPN-forge-1.20-4.0.2.jar | libipn | 4.0.2 | 677 | 400 | 0 | dep | yes |  |
| Mantle-1.20.1-1.11.128.jar | mantle | 1.11.128 | 1629 | 792 | 0 | dep | yes |  |
| melody_forge_1.0.3_MC_1.20.1-1.20.4.jar | melody | 1.0.2 | 36 | 22 | 3 | dep | yes |  |
| mezz_config-1.20.1-forge-0.6.11.jar | mezz_config | 0.6.11 | 377 | 230 | 0 | dep | yes |  |
| moonlight-1.20-2.16.39-forge.jar | moonlight | 1.20-2.16.39 | 1390 | 512 | 53 | dep | yes |  |
| Necronomicon-Forge-1.6.0+1.20.1.jar | necronomicon | 1.6.0 | 32 | 28 | 2 | dep | yes |  |
| Patchouli-1.20.1-85-FORGE.jar | patchouli | ${file.jarVersion} | 627 | 196 | 10 | dep | yes |  |
| PuzzlesLib-v8.1.36-1.20.1-Forge.jar | puzzleslib | 8.1.36 | 1748 | 728 | 20 | lib | yes | puzzlesaccessapi |
| ranged_weapon_api-forge-2.3.4+1.20.1.jar | ranged_weapon_api | 2.3.4+1.20.1 | 775 | 60 | 22 | dep | yes |  |
| Searchables-forge-1.20.1-1.0.3.jar | searchables | ${file.jarVersion} | 75 | 41 | 1 | dep | yes |  |
| sophisticatedcore-1.20.1-1.5.29.2459.jar | sophisticatedcore | 1.5.29.2459 | 1733 | 810 | 4 | dep | yes |  |
| supermartijn642corelib-1.1.26a-forge-mc1.20.1.jar | supermartijn642corelib | 1.1.26+a | 585 | 226 | 15 | lib | yes |  |
| tacz-forge-1.20.1-1.2.0.jar | tacz | ${file.jarVersion} | 67384 | 1061 | 20 | dep | yes |  |
| TerraBlender-forge-1.20.1-3.0.1.11.jar | terrablender | ${file.jarVersion} | 105 | 53 | 7 | dep | yes |  |
| tslatentitystatus-forge-1.20.1-1.6.6.jar | tslatentitystatus | 1.6.6 | 749 | 46 | 7 | dep | yes |  |
| Zeta-1.0-31.jar | zeta | ${file.jarVersion} | 982 | 696 | 16 | dep | yes |  |
