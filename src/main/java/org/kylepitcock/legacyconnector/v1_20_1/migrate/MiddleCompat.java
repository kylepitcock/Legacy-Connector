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

    /** {@code Potion.byName(String)}. */
    public static net.minecraft.world.item.alchemy.Potion potionByName(String name) {
        return net.minecraft.core.registries.BuiltInRegistries.POTION.get(net.minecraft.resources.ResourceLocation.parse(name));
    }
}
