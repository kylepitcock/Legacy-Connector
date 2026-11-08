package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Signature changes of Minecraft 1.20.1 to 1.21.1 in the middle and tail of the ATM 9 ranking: item and stack data that became components, enchantment queries
 * that became registry lookups, entity and block entity methods that gained a provider or lost a parameter (see {@code mc-calls.tsv}).
 */
public final class MiddleCompat {
    private MiddleCompat() {}

    // ------------------------------------------------------------------ item stacks

    /** {@code ItemStack.getBaseRepairCost()}: the repair cost component. */
    public static int getBaseRepairCost(ItemStack stack) {
        return stack.getOrDefault(DataComponents.REPAIR_COST, 0);
    }

    /** {@code ItemStack.setRepairCost(int)}. */
    public static void setRepairCost(ItemStack stack, int cost) {
        stack.set(DataComponents.REPAIR_COST, cost);
    }

    /** {@code ItemStack.hurtEnemy(LivingEntity, Player)} returned nothing in 1.20.1. */
    public static void hurtEnemy(ItemStack stack, LivingEntity target, Player attacker) {
        stack.hurtEnemy(target, attacker);
    }

    /** {@code new ItemStack(ItemLike, int, CompoundTag capabilities)}: the capability tag has no meaning any more. */
    public static ItemStack newStack(net.minecraft.world.level.ItemLike item, int count, CompoundTag capNbt) {
        return new ItemStack(item, count);
    }

    /** {@code ItemStack.getAllEnchantments()}. */
    public static java.util.Map<LegacyEnchantment, Integer> getAllEnchantments(ItemStack stack) {
        return HolderCompat.getEnchantments(stack);
    }

    /** {@code ItemStack.onBlockStartBreak(BlockPos, Player)}: the hook is gone; the break goes on. */
    public static boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
        return false;
    }

    /** {@code Item.onBlockStartBreak(ItemStack, BlockPos, Player)}. */
    public static boolean itemOnBlockStartBreak(Item item, ItemStack stack, BlockPos pos, Player player) {
        return false;
    }

    /** {@code ItemStack.getShareTag()}: the whole NBT-shaped data of the stack. */
    public static CompoundTag stackShareTag(ItemStack stack) {
        return ItemStackNbt.getTag(stack);
    }

    /** {@code Item.getShareTag(ItemStack)}. */
    public static CompoundTag getShareTag(Item item, ItemStack stack) {
        return ItemStackNbt.getTag(stack);
    }

    /** {@code ItemStack.readShareTag(CompoundTag)}. */
    public static void readShareTag(ItemStack stack, CompoundTag tag) {
        ItemStackNbt.setTag(stack, tag);
    }

    /** {@code ItemStack.areShareTagsEqual(ItemStack)}. */
    public static boolean areShareTagsEqual(ItemStack a, ItemStack b) {
        return Objects.equals(ItemStackNbt.getTag(a), ItemStackNbt.getTag(b));
    }

    /** {@code Item.shouldOverrideMultiplayerNbt()}: the data components always travel. */
    public static boolean shouldOverrideMultiplayerNbt(Item item) {
        return true;
    }

    /** {@code Item.verifyTagAfterLoad(CompoundTag)}: nothing to repair in component data. */
    public static void verifyTagAfterLoad(Item item, CompoundTag tag) {}

    /** {@code Item.getDefaultTooltipHideFlags(ItemStack)}: no flags (tooltip visibility lives in the components). */
    public static int getDefaultTooltipHideFlags(Item item, ItemStack stack) {
        return 0;
    }

    /** {@code Item.canBeDepleted()}. */
    public static boolean canBeDepleted(Item item) {
        return MiscCompat.getMaxDamage(item) > 0;
    }

    /** {@code Item.getMaxStackSize()}. */
    public static int itemMaxStackSize(Item item) {
        return item.getDefaultMaxStackSize();
    }

    /** {@code Item.getRarity(ItemStack)}. */
    public static Rarity getRarity(Item item, ItemStack stack) {
        return stack.getRarity();
    }

    /** {@code Item.isFireResistant()}. */
    public static boolean isFireResistant(Item item) {
        return item.components().has(DataComponents.FIRE_RESISTANT);
    }

    /** {@code Item.getUseDuration(ItemStack)} lost nothing but gained the user. */
    public static int getUseDuration(Item item, ItemStack stack) {
        return item.getUseDuration(stack, null);
    }

    /** {@code Item.isCorrectToolForDrops(BlockState)}. */
    public static boolean isCorrectToolForDrops(Item item, BlockState state) {
        return item.isCorrectToolForDrops(new ItemStack(item), state);
    }

    /** {@code Item.canEquip(ItemStack, EquipmentSlot, Entity)}. */
    public static boolean canEquip(Item item, ItemStack stack, EquipmentSlot slot, Entity entity) {
        return entity instanceof LivingEntity living ? item.canEquip(stack, slot, living) : false;
    }

    /** {@code ItemStack.canEquip(EquipmentSlot, Entity)}. */
    public static boolean stackCanEquip(ItemStack stack, EquipmentSlot slot, Entity entity) {
        return canEquip(stack.getItem(), stack, slot, entity);
    }

    /** {@code Item.getAttributeModifiers(EquipmentSlot, ItemStack)}: the modifiers of the stack's component for that slot. */
    public static Multimap<Attribute, AttributeModifier> getAttributeModifiers(Item item, EquipmentSlot slot, ItemStack stack) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> out = ImmutableMultimap.builder();
        stack.getAttributeModifiers().forEach(slot, (attribute, modifier) -> out.put(attribute.value(), modifier));
        return out.build();
    }

    // ------------------------------------------------------------------ enchantment queries (registry lookups now)

    private static Holder<Enchantment> enchantment(ResourceKey<Enchantment> key) {
        return MigrateContext.registries().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static int level(LivingEntity entity, ResourceKey<Enchantment> key) {
        return EnchantmentHelper.getEnchantmentLevel(enchantment(key), entity);
    }

    private static int level(ItemStack stack, ResourceKey<Enchantment> key) {
        return stack.getEnchantmentLevel(enchantment(key));
    }

    public static int getFireAspect(LivingEntity entity) {
        return level(entity, Enchantments.FIRE_ASPECT);
    }

    public static int getKnockbackBonus(LivingEntity entity) {
        return level(entity, Enchantments.KNOCKBACK);
    }

    public static boolean hasFrostWalker(LivingEntity entity) {
        return level(entity, Enchantments.FROST_WALKER) > 0;
    }

    public static boolean hasAquaAffinity(LivingEntity entity) {
        return level(entity, Enchantments.AQUA_AFFINITY) > 0;
    }

    public static int getBlockEfficiency(LivingEntity entity) {
        return level(entity, Enchantments.EFFICIENCY);
    }

    public static float getSweepingDamageRatio(LivingEntity entity) {
        int level = level(entity, Enchantments.SWEEPING_EDGE);
        return level > 0 ? 1.0F - 1.0F / (float) (level + 1) : 0.0F;
    }

    public static boolean hasBindingCurse(ItemStack stack) {
        return level(stack, Enchantments.BINDING_CURSE) > 0;
    }

    public static boolean hasVanishingCurse(ItemStack stack) {
        return level(stack, Enchantments.VANISHING_CURSE) > 0;
    }

    public static int getLoyalty(ItemStack stack) {
        return level(stack, Enchantments.LOYALTY);
    }

    public static int getFishingLuckBonus(ItemStack stack) {
        return level(stack, Enchantments.LUCK_OF_THE_SEA);
    }

    public static int getFishingSpeedBonus(ItemStack stack) {
        return level(stack, Enchantments.LURE);
    }

    /** {@code EnchantmentHelper.getTagEnchantmentLevel(Enchantment, ItemStack)}. */
    public static int getTagEnchantmentLevel(LegacyEnchantment e, ItemStack stack) {
        return EnchantmentHelper.getTagEnchantmentLevel(HolderCompat.holder(e), stack);
    }

    /** {@code EnchantmentHelper.enchantItem(RandomSource, ItemStack, int level, boolean allowTreasure)}. */
    public static ItemStack enchantItem(net.minecraft.util.RandomSource random, ItemStack stack, int level, boolean allowTreasure) {
        return EnchantmentHelper.enchantItem(random, stack, level, MigrateContext.registries(), java.util.Optional.empty());
    }

    // ------------------------------------------------------------------ entities

    /** {@code Entity.serializeNBT()} (the Forge INBTSerializable form): the entity's save data with its id. */
    public static CompoundTag serializeNBT(Entity entity) {
        CompoundTag tag = new CompoundTag();
        String id = entity.getEncodeId();
        if (id != null) tag.putString("id", id);
        return entity.saveWithoutId(tag);
    }

    /** {@code Entity.deserializeNBT(CompoundTag)}. */
    public static void deserializeNBT(Entity entity, CompoundTag tag) {
        entity.load(tag);
    }

    /** {@code Entity.ignoreExplosion()}. */
    public static boolean ignoreExplosion(Entity entity) {
        return entity.ignoreExplosion(null);
    }

    /** {@code Entity.getHandSlots()}: only living entities hold items. */
    public static Iterable<ItemStack> getHandSlots(Entity entity) {
        return entity instanceof LivingEntity living ? living.getHandSlots() : List.of();
    }

    /** {@code Entity.getArmorSlots()}. */
    public static Iterable<ItemStack> getArmorSlots(Entity entity) {
        return entity instanceof LivingEntity living ? living.getArmorSlots() : List.of();
    }

    /** {@code Entity.setItemSlot(EquipmentSlot, ItemStack)}. */
    public static void setItemSlot(Entity entity, EquipmentSlot slot, ItemStack stack) {
        if (entity instanceof LivingEntity living) living.setItemSlot(slot, stack);
    }

    /** {@code Entity.getFeetBlockState()}. */
    public static BlockState getFeetBlockState(Entity entity) {
        return entity.getInBlockState();
    }

    /** {@code Entity.canChangeDimensions()}. */
    public static boolean canChangeDimensions(Entity entity) {
        return entity.canChangeDimensions(entity.level(), entity.level());
    }

    /** {@code Entity.getMyRidingOffset()}: the vehicle attachment points replaced the offsets; players sat 0.35 lower. */
    public static double getMyRidingOffset(Entity entity) {
        return entity instanceof Player ? -0.35D : 0.0D;
    }

    /** {@code LivingEntity.curePotionEffects(ItemStack)}: a curative item cures like milk. */
    public static boolean curePotionEffects(LivingEntity entity, ItemStack curativeItem) {
        return entity.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
    }

    /** {@code LivingEntity.removeEffectNoUpdate(MobEffect)}. */
    public static net.minecraft.world.effect.MobEffectInstance removeEffectNoUpdate(LivingEntity entity, net.minecraft.world.effect.MobEffect effect) {
        return entity.removeEffectNoUpdate(HolderCompat.holder(effect));
    }

    /** {@code Player.disableShield(boolean)}: the cooldown is applied to the shield in use. */
    public static void disableShield(Player player, boolean sprinting) {
        player.disableShield();
    }

    /** {@code Player.startAutoSpinAttack(int)}: the riptide spin with a trident's damage and the item in use. */
    public static void startAutoSpinAttack(Player player, int ticks) {
        player.startAutoSpinAttack(ticks, 8.0F, player.getUseItem());
    }

    /** {@code Player.canReach(BlockPos, double)}. */
    public static boolean canReach(Player player, BlockPos pos, double padding) {
        return player.canInteractWithBlock(pos, padding);
    }

    /** {@code Player.canReach(Entity, double)}. */
    public static boolean canReach(Player player, Entity entity, double padding) {
        return player.canInteractWithEntity(entity, padding);
    }

    // ------------------------------------------------------------------ block entities

    private static net.minecraft.core.HolderLookup.Provider provider(BlockEntity be) {
        Level level = be.getLevel();
        return level != null ? level.registryAccess() : MigrateContext.provider();
    }

    /** {@code BlockEntity.getUpdateTag()}. */
    public static CompoundTag getUpdateTag(BlockEntity be) {
        return be.getUpdateTag(provider(be));
    }

    /** {@code BlockEntity.saveWithId()}. */
    public static CompoundTag saveWithId(BlockEntity be) {
        return be.saveWithId(provider(be));
    }

    /** {@code BlockEntity.serializeNBT()} (Forge): the full save data. */
    public static CompoundTag serializeNBT(BlockEntity be) {
        return be.saveWithFullMetadata(provider(be));
    }

    /** {@code BlockEntity.deserializeNBT(CompoundTag)}. */
    public static void deserializeNBT(BlockEntity be, CompoundTag tag) {
        be.loadWithComponents(tag, provider(be));
    }

    /** {@code BlockEntity.loadStatic(BlockPos, BlockState, CompoundTag)}. */
    public static BlockEntity loadStatic(BlockPos pos, BlockState state, CompoundTag tag) {
        return BlockEntity.loadStatic(pos, state, tag, MigrateContext.provider());
    }

    /** {@code BlockEntity.saveToItem(ItemStack)}. */
    public static void saveToItem(BlockEntity be, ItemStack stack) {
        be.saveToItem(stack, provider(be));
    }

    /** {@code BlockEntity.getRenderBoundingBox()} (Forge): the block's cell, which is what vanilla renders in. */
    public static net.minecraft.world.phys.AABB getRenderBoundingBox(BlockEntity be) {
        return new net.minecraft.world.phys.AABB(be.getBlockPos());
    }

    // ------------------------------------------------------------------ small statics and registries

    /** {@code GsonHelper.getAsItem(JsonObject, String)} answered the item itself. */
    public static Item getAsItem(com.google.gson.JsonObject object, String key) {
        return net.minecraft.util.GsonHelper.getAsItem(object, key).value();
    }

    /** {@code StringUtil.formatTickDuration(int)}: at the normal 20 ticks per second. */
    public static String formatTickDuration(int ticks) {
        return net.minecraft.util.StringUtil.formatTickDuration(ticks, 20.0F);
    }

    /** {@code new MobEffectInstance(MobEffect)}: a zero-length effect, as in 1.20.1. */
    public static net.minecraft.world.effect.MobEffectInstance newEffect(net.minecraft.world.effect.MobEffect effect) {
        return HolderCompat.newEffect(effect, 0);
    }

    /** {@code MobEffect.addAttributeModifier(Attribute, String uuid, double, Operation)}. */
    public static net.minecraft.world.effect.MobEffect addAttributeModifier(net.minecraft.world.effect.MobEffect effect, Attribute attribute, String uuid,
            double amount, AttributeModifier.Operation operation) {
        return effect.addAttributeModifier(HolderCompat.holder(attribute), HolderCompat.modifierId(java.util.UUID.fromString(uuid)), amount, operation);
    }

    /** {@code MobEffect.byId(int)}. */
    public static net.minecraft.world.effect.MobEffect effectById(int id) {
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.byId(id);
    }

    /** {@code MobEffect.getId(MobEffect)}. */
    public static int effectGetId(net.minecraft.world.effect.MobEffect effect) {
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getId(effect);
    }

    /** {@code AttributeModifier.getId()}: the UUID a 1.20.1 modifier was built from (a fresh stable one for modifiers made natively). */
    public static java.util.UUID modifierUuid(AttributeModifier modifier) {
        String path = modifier.id().getPath();
        if (path.startsWith("modifier/")) {
            try {
                return java.util.UUID.fromString(path.substring("modifier/".length()));
            } catch (IllegalArgumentException e) {
                // not one of ours
            }
        }
        return java.util.UUID.nameUUIDFromBytes(modifier.id().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** {@code BlockItem.getBlockEntityData(ItemStack)}: the block entity data component as a tag (null without). */
    public static CompoundTag getBlockEntityData(ItemStack stack) {
        net.minecraft.world.item.component.CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyTag();
    }

    /** {@code EntityType.getDefaultLootTable()}. */
    public static net.minecraft.resources.ResourceLocation getDefaultLootTable(net.minecraft.world.entity.EntityType<?> type) {
        return type.getDefaultLootTable().location();
    }

    /** {@code EntityType.getAABB(double, double, double)}. */
    public static net.minecraft.world.phys.AABB getAABB(net.minecraft.world.entity.EntityType<?> type, double x, double y, double z) {
        return type.getDimensions().makeBoundingBox(x, y, z);
    }

    /** {@code Util.getOrThrow(DataResult, Function<String, E>)}. */
    public static <T, E extends Exception> T getOrThrow(com.mojang.serialization.DataResult<T> result, java.util.function.Function<String, E> onError) throws E {
        java.util.Optional<T> value = result.result();
        if (value.isPresent()) return value.get();
        throw onError.apply(result.error().map(com.mojang.serialization.DataResult.Error::message).orElse("error"));
    }

    /** {@code Util.identityStrategy()}. */
    public static <T> it.unimi.dsi.fastutil.Hash.Strategy<T> identityStrategy() {
        return new it.unimi.dsi.fastutil.Hash.Strategy<T>() {
            @Override
            public int hashCode(T o) {
                return System.identityHashCode(o);
            }

            @Override
            public boolean equals(T a, T b) {
                return a == b;
            }
        };
    }

    // ------------------------------------------------------------------ constructors with a new parameter, codecs, potions

    /** The experience range 1.20.1's one-argument {@code DropExperienceBlock} constructor stood for: none. */
    public static net.minecraft.util.valueproviders.IntProvider zeroXp(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        return net.minecraft.util.valueproviders.ConstantInt.of(0);
    }

    /** The pickup stack of {@code new Arrow(level, ...)}: an arrow. */
    public static ItemStack arrowStack(Level level) {
        return new ItemStack(net.minecraft.world.item.Items.ARROW);
    }

    /** The weapon that fired a {@code new Arrow(level, ...)}: none known. */
    public static ItemStack noWeapon(Level level) {
        return null;
    }

    /** The client settings of a server player made without any ({@code new ServerPlayer(server, level, profile)}). */
    public static net.minecraft.server.level.ClientInformation clientInfo(net.minecraft.server.MinecraftServer server) {
        return net.minecraft.server.level.ClientInformation.createDefault();
    }

    /** {@code KeyDispatchDataCodec.codec()}: a Codec in 1.20.1, a MapCodec in 1.21.1 (the record component). */
    public static <A> com.mojang.serialization.Codec<A> keyDispatchCodec(net.minecraft.util.KeyDispatchDataCodec<A> k) {
        return k.codec().codec();
    }

    /** {@code ExtraCodecs.lazyInitializedCodec(Supplier)}. */
    public static <A> com.mojang.serialization.Codec<A> lazyInitializedCodec(java.util.function.Supplier<com.mojang.serialization.Codec<A>> supplier) {
        return com.mojang.serialization.Codec.lazyInitialized(supplier);
    }

    /** {@code ExtraCodecs.validate(Codec, Function)}. */
    public static <A> com.mojang.serialization.Codec<A> validate(com.mojang.serialization.Codec<A> codec,
            java.util.function.Function<A, com.mojang.serialization.DataResult<A>> check) {
        return codec.validate(check);
    }

    /** {@code ExtraCodecs.validate(MapCodec, Function)}. */
    public static <A> com.mojang.serialization.MapCodec<A> validate(com.mojang.serialization.MapCodec<A> codec,
            java.util.function.Function<A, com.mojang.serialization.DataResult<A>> check) {
        return codec.validate(check);
    }

    /** {@code ExtraCodecs.stringResolverCodec(toString, fromString)}: a string key that must name an element. */
    public static <E> com.mojang.serialization.Codec<E> stringResolverCodec(java.util.function.Function<E, String> toString,
            java.util.function.Function<String, E> fromString) {
        return com.mojang.serialization.Codec.STRING.flatXmap(name -> {
            E value = fromString.apply(name);
            return value == null ? com.mojang.serialization.DataResult.<E>error(() -> "Unknown element name: " + name)
                    : com.mojang.serialization.DataResult.success(value);
        }, element -> {
            String name = toString.apply(element);
            return name == null ? com.mojang.serialization.DataResult.<String>error(() -> "Element with unknown name: " + element)
                    : com.mojang.serialization.DataResult.success(name);
        });
    }

    /** {@code ExtraCodecs.COMPONENT}. */
    public static com.mojang.serialization.Codec<net.minecraft.network.chat.Component> componentCodec() {
        return net.minecraft.network.chat.ComponentSerialization.CODEC;
    }

    /** {@code ExtraCodecs.FLAT_COMPONENT}. */
    public static com.mojang.serialization.Codec<net.minecraft.network.chat.Component> flatComponentCodec() {
        return net.minecraft.network.chat.ComponentSerialization.FLAT_CODEC;
    }

    /** {@code Potion.getName(String prefix)}: the prefix and the potion's registry name. */
    public static String potionName(net.minecraft.world.item.alchemy.Potion potion, String prefix) {
        return prefix + net.minecraft.core.registries.BuiltInRegistries.POTION.getKey(potion).getPath();
    }

    // ------------------------------------------------------------------ plants and taming (override bridges and calls)

    /** The plantable a 1.20.1 {@code canSustainPlant} override receives, made from the plant state 1.21.1 passes. */
    public static legacyconnector.v1_20_1.net.minecraftforge.common.IPlantable plantable(BlockState plant) {
        return new legacyconnector.v1_20_1.net.minecraftforge.common.IPlantable() {
            @Override
            public legacyconnector.v1_20_1.net.minecraftforge.common.PlantType getPlantType(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
                return plantTypeOf(plant.getBlock());
            }

            @Override
            public BlockState getPlant(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
                return plant;
            }
        };
    }

    private static legacyconnector.v1_20_1.net.minecraftforge.common.PlantType plantTypeOf(net.minecraft.world.level.block.Block block) {
        if (block instanceof net.minecraft.world.level.block.CropBlock || block instanceof net.minecraft.world.level.block.StemBlock) {
            return legacyconnector.v1_20_1.net.minecraftforge.common.PlantType.CROP;
        }
        if (block instanceof net.minecraft.world.level.block.NetherWartBlock) return legacyconnector.v1_20_1.net.minecraftforge.common.PlantType.NETHER;
        if (block instanceof net.minecraft.world.level.block.WaterlilyBlock) return legacyconnector.v1_20_1.net.minecraftforge.common.PlantType.WATER;
        if (block instanceof net.minecraft.world.level.block.CactusBlock || block instanceof net.minecraft.world.level.block.DeadBushBlock) {
            return legacyconnector.v1_20_1.net.minecraftforge.common.PlantType.DESERT;
        }
        return legacyconnector.v1_20_1.net.minecraftforge.common.PlantType.PLAINS;
    }

    /** The answer of a bridged {@code canSustainPlant} override: a yes or a no, never "ask the default". */
    public static net.neoforged.neoforge.common.util.TriState triState(boolean value) {
        return value ? net.neoforged.neoforge.common.util.TriState.TRUE : net.neoforged.neoforge.common.util.TriState.FALSE;
    }

    /** Forge's default {@code canSustainPlant}: what the plant type needs under it. */
    public static boolean defaultCanSustainPlant(BlockState soil, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction facing,
            legacyconnector.v1_20_1.net.minecraftforge.common.IPlantable plantable) {
        var type = plantable.getPlantType(level, pos.relative(facing));
        if (type == legacyconnector.v1_20_1.net.minecraftforge.common.PlantType.DESERT) return soil.is(net.minecraft.tags.BlockTags.SAND);
        if (type == legacyconnector.v1_20_1.net.minecraftforge.common.PlantType.NETHER) return soil.is(net.minecraft.world.level.block.Blocks.SOUL_SAND);
        if (type == legacyconnector.v1_20_1.net.minecraftforge.common.PlantType.CROP) return soil.is(net.minecraft.world.level.block.Blocks.FARMLAND);
        if (type == legacyconnector.v1_20_1.net.minecraftforge.common.PlantType.CAVE) return soil.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
        if (type == legacyconnector.v1_20_1.net.minecraftforge.common.PlantType.WATER) {
            return soil.getFluidState().is(net.minecraft.tags.FluidTags.WATER) || soil.getBlock() instanceof net.minecraft.world.level.block.IceBlock;
        }
        if (type == legacyconnector.v1_20_1.net.minecraftforge.common.PlantType.BEACH) {
            boolean sandy = soil.is(net.minecraft.tags.BlockTags.DIRT) || soil.is(net.minecraft.tags.BlockTags.SAND);
            boolean water = false;
            for (net.minecraft.core.Direction side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                var next = level.getBlockState(pos.relative(side));
                if (next.getFluidState().is(net.minecraft.tags.FluidTags.WATER) || next.is(net.minecraft.world.level.block.Blocks.FROSTED_ICE)) water = true;
            }
            return sandy && water;
        }
        return soil.is(net.minecraft.tags.BlockTags.DIRT) || soil.is(net.minecraft.world.level.block.Blocks.FARMLAND);
    }

    /** {@code super.canSustainPlant(...)} of a block whose parent is Minecraft's: the default rules. */
    public static boolean superCanSustainPlant(net.minecraft.world.level.block.Block block, BlockState soil, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.core.Direction facing, legacyconnector.v1_20_1.net.minecraftforge.common.IPlantable plantable) {
        return defaultCanSustainPlant(soil, level, pos, facing, plantable);
    }

    /** {@code block.canSustainPlant(state, level, pos, facing, plantable)}. */
    public static boolean blockCanSustainPlant(net.minecraft.world.level.block.Block block, BlockState soil, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.core.Direction facing, legacyconnector.v1_20_1.net.minecraftforge.common.IPlantable plantable) {
        return stateCanSustainPlant(soil, level, pos, facing, plantable);
    }

    /** {@code state.canSustainPlant(level, pos, facing, plantable)}. */
    public static boolean stateCanSustainPlant(BlockState soil, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction facing,
            legacyconnector.v1_20_1.net.minecraftforge.common.IPlantable plantable) {
        BlockState plant = plantable.getPlant(level, pos.relative(facing));
        net.neoforged.neoforge.common.util.TriState answer = soil.canSustainPlant(level, pos, facing, plant);
        if (answer == net.neoforged.neoforge.common.util.TriState.TRUE) return true;
        if (answer == net.neoforged.neoforge.common.util.TriState.FALSE) return false;
        return defaultCanSustainPlant(soil, level, pos, facing, plantable);
    }

    /** The "apply taming side effects" flag a 1.20.1 {@code setTame(boolean)} always had. */
    public static boolean sideEffects(boolean tame) {
        return true;
    }

    // ------------------------------------------------------------------ enchantment tags, ingredients, server and level statics

    /** {@code EnchantmentHelper.getEnchantmentId(CompoundTag)}: the {@code id} of a stored enchantment entry. */
    public static net.minecraft.resources.ResourceLocation getEnchantmentId(CompoundTag entry) {
        return net.minecraft.resources.ResourceLocation.tryParse(entry.getString("id"));
    }

    /** {@code EnchantmentHelper.getEnchantmentLevel(CompoundTag)}. */
    public static int getEnchantmentLevel(CompoundTag entry) {
        return net.minecraft.util.Mth.clamp(entry.getInt("lvl"), 0, 255);
    }

    /** {@code EnchantmentHelper.storeEnchantment(ResourceLocation, int)}. */
    public static CompoundTag storeEnchantment(net.minecraft.resources.ResourceLocation id, int level) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", String.valueOf(id));
        tag.putShort("lvl", (short) level);
        return tag;
    }

    /** {@code EnchantmentHelper.setEnchantmentLevel(CompoundTag, int)}. */
    public static void setEnchantmentLevel(CompoundTag entry, int level) {
        entry.putShort("lvl", (short) level);
    }

    /** {@code EnchantmentHelper.getEnchantmentId(Enchantment)}. */
    public static net.minecraft.resources.ResourceLocation getEnchantmentId(LegacyEnchantment enchantment) {
        return HolderCompat.holder(enchantment).unwrapKey().map(ResourceKey::location).orElse(null);
    }

    public static int getRiptide(ItemStack stack) {
        return level(stack, Enchantments.RIPTIDE);
    }

    public static boolean hasChanneling(ItemStack stack) {
        return level(stack, Enchantments.CHANNELING) > 0;
    }

    public static boolean hasSilkTouch(ItemStack stack) {
        return level(stack, Enchantments.SILK_TOUCH) > 0;
    }

    public static int getDepthStrider(LivingEntity entity) {
        return level(entity, Enchantments.DEPTH_STRIDER);
    }

    public static int getRespiration(LivingEntity entity) {
        return level(entity, Enchantments.RESPIRATION);
    }

    /** {@code Ingredient.fromJson(JsonElement, boolean allowEmpty)}. */
    public static net.minecraft.world.item.crafting.Ingredient ingredientFromJson(com.google.gson.JsonElement json, boolean allowEmpty) {
        var codec = allowEmpty ? net.minecraft.world.item.crafting.Ingredient.CODEC : net.minecraft.world.item.crafting.Ingredient.CODEC_NONEMPTY;
        return codec.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow(com.google.gson.JsonParseException::new);
    }

    /** {@code Ingredient.isVanilla()}: no custom ingredient type behind it. */
    public static boolean isVanilla(net.minecraft.world.item.crafting.Ingredient ingredient) {
        return ingredient.isSimple();
    }

    /** {@code Ingredient.checkInvalidation()}: ingredients no longer cache a stack list that needs invalidating. */
    public static boolean checkInvalidation(net.minecraft.world.item.crafting.Ingredient ingredient) {
        return false;
    }

    public static void invalidate(net.minecraft.world.item.crafting.Ingredient ingredient) {}

    public static void invalidateAll() {}

    /** {@code Ingredient.merge(Collection)}: one ingredient accepting what any of them accepts. */
    public static net.minecraft.world.item.crafting.Ingredient merge(java.util.Collection<net.minecraft.world.item.crafting.Ingredient> ingredients) {
        return net.minecraft.world.item.crafting.Ingredient.of(ingredients.stream().flatMap(i -> java.util.Arrays.stream(i.getItems())));
    }

    /** {@code buf.readWithCodec(ops, codec)}. */
    public static <T> T readWithCodec(net.minecraft.network.FriendlyByteBuf buf, com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops,
            com.mojang.serialization.Codec<T> codec) {
        return buf.readWithCodec(ops, codec, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
    }

    /** {@code buf.writeWithCodec(ops, codec, value)} returned nothing in 1.20.1. */
    public static <T> void writeWithCodec(net.minecraft.network.FriendlyByteBuf buf, com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops,
            com.mojang.serialization.Codec<T> codec, T value) {
        buf.writeWithCodec(ops, codec, value);
    }

    /** {@code MinecraftServer.getServerDirectory()} was a File. */
    public static java.io.File getServerDirectory(net.minecraft.server.MinecraftServer server) {
        return server.getServerDirectory().toFile();
    }

    /** {@code MinecraftServer.getFile(String)}. */
    public static java.io.File getServerFile(net.minecraft.server.MinecraftServer server, String name) {
        return server.getFile(name).toFile();
    }

    /** {@code MinecraftServer.isNetherEnabled()}: the nether level exists. */
    public static boolean isNetherEnabled(net.minecraft.server.MinecraftServer server) {
        return server.getLevel(Level.NETHER) != null;
    }

    /** {@code Level.getFreeMapId()} answered a plain int. */
    public static int getFreeMapId(Level level) {
        return level.getFreeMapId().id();
    }

    /** {@code Level.getExistingBlockEntity(BlockPos)}: the block entity if its chunk is loaded. */
    public static BlockEntity getExistingBlockEntity(Level level, BlockPos pos) {
        return level.isLoaded(pos) ? level.getBlockEntity(pos) : null;
    }

    /** {@code Level.dimensionTypeId()}. */
    public static ResourceKey<net.minecraft.world.level.dimension.DimensionType> dimensionTypeId(Level level) {
        return level.dimensionTypeRegistration().unwrapKey().orElse(null);
    }

    /** {@code ItemStack.canApplyAtEnchantingTable(Enchantment)}: the enchanting table picks primary items. */
    public static boolean canApplyAtEnchantingTable(ItemStack stack, LegacyEnchantment enchantment) {
        return stack.isPrimaryItemFor(HolderCompat.holder(enchantment));
    }

    /** {@code ItemStack.addAttributeModifier(Attribute, AttributeModifier, EquipmentSlot)}: edits the attribute modifiers component. */
    public static void addAttributeModifier(ItemStack stack, Attribute attribute, AttributeModifier modifier, EquipmentSlot slot) {
        net.minecraft.world.entity.EquipmentSlotGroup group = slot == null ? net.minecraft.world.entity.EquipmentSlotGroup.ANY
                : net.minecraft.world.entity.EquipmentSlotGroup.bySlot(slot);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, stack.getAttributeModifiers().withModifierAdded(HolderCompat.holder(attribute), modifier, group));
    }

    /** The 1.21.1 primary-item hook for a bridged {@code canApplyAtEnchantingTable} override. */
    public static LegacyEnchantment legacyEnchantment(Holder<Enchantment> holder) {
        return HolderCompat.legacy(holder);
    }

    /** The level a bridged {@code isPathfindable} override gets: 1.21.1 has no level there, so an empty one (all air). */
    public static net.minecraft.world.level.BlockGetter emptyGetter(BlockState state) {
        return net.minecraft.world.level.EmptyBlockGetter.INSTANCE;
    }

    /** The position a bridged {@code isPathfindable} override gets: the origin. */
    public static BlockPos originPos(BlockState state) {
        return BlockPos.ZERO;
    }

    /** {@code Potion.byName(String)}. */
    public static net.minecraft.world.item.alchemy.Potion potionByName(String name) {
        return net.minecraft.core.registries.BuiltInRegistries.POTION.get(net.minecraft.resources.ResourceLocation.parse(name));
    }
}
