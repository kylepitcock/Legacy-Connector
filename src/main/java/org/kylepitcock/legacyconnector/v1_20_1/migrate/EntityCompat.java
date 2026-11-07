package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.UUID;
import java.util.function.Supplier;
import legacyconnector.v1_20_1.net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Entity, item and block signature changes of 1.21.1 that need a helper (called by code the loader rewrote, see {@code mc-calls.tsv}). */
public final class EntityCompat {
    private EntityCompat() {}

    // ------------------------------------------------------------------ entities

    /** {@code Entity.setMaxUpStep(float)}: the step height is an attribute of living entities now. */
    public static void setMaxUpStep(Entity entity, float height) {
        if (entity instanceof LivingEntity living) {
            var attribute = living.getAttribute(Attributes.STEP_HEIGHT);
            if (attribute != null) attribute.setBaseValue(height);
        }
    }

    /** {@code LivingEntity.dropExperience()}: the orbs a death drops. */
    public static void dropExperience(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel level) ExperienceOrb.award(level, entity.position(), entity.getExperienceReward(level, null));
    }

    /** {@code LivingEntity.getExperienceReward()}. */
    public static int xpReward(LivingEntity entity) {
        return entity.level() instanceof ServerLevel level ? entity.getExperienceReward(level, null) : 0;
    }

    /** {@code Entity.getPassengersRidingOffset()}: where a rider sat (the passenger attachment point replaced it). */
    public static double passengersRidingOffset(Entity entity) {
        return entity.getBbHeight() * 0.75D;
    }

    public static void arrowSetKnockback(AbstractArrow arrow, int knockback) {
        // 1.21.1 arrows take their knockback from the weapon's enchantments
    }

    public static ItemStack noStack(EntityType<?> type) {
        return ItemStack.EMPTY;
    }

    public static ItemStack nullStack(EntityType<?> type) {
        return null;
    }

    public static boolean hasAttribute(AttributeMap map, Attribute attribute) {
        return map.hasAttribute(HolderCompat.holder(attribute));
    }

    public static int spawnX(LevelData data) {
        return data.getSpawnPos().getX();
    }

    public static int spawnY(LevelData data) {
        return data.getSpawnPos().getY();
    }

    public static int spawnZ(LevelData data) {
        return data.getSpawnPos().getZ();
    }

    public static ImmutableMap<net.minecraft.world.level.block.state.properties.Property<?>, Comparable<?>> stateValues(BlockState state) {
        return ImmutableMap.copyOf(state.getValues());
    }

    public static UUID uuidFromString(String s) {
        return UUID.fromString(s.length() == 32 ? s.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5") : s);
    }

    // ------------------------------------------------------------------ items

    /** {@code Tier.getLevel()}: the mining level, derived from the tag of blocks the tier cannot drop. */
    public static int tierLevel(Tier tier) {
        var tag = tier.getIncorrectBlocksForDrops();
        if (tag.equals(BlockTags.INCORRECT_FOR_WOODEN_TOOL) || tag.equals(BlockTags.INCORRECT_FOR_GOLD_TOOL)) return 0;
        if (tag.equals(BlockTags.INCORRECT_FOR_STONE_TOOL)) return 1;
        if (tag.equals(BlockTags.INCORRECT_FOR_IRON_TOOL)) return 2;
        if (tag.equals(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)) return 3;
        if (tag.equals(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)) return 4;
        return 2;
    }

    /** {@code ItemStack.hurt(int, RandomSource, ServerPlayer)}: damages the stack, true when it broke. */
    public static boolean stackHurt(ItemStack stack, int amount, RandomSource random, ServerPlayer player) {
        if (!stack.isDamageableItem() || amount <= 0) return false;
        int damage = stack.getDamageValue() + amount;
        stack.setDamageValue(damage);
        return damage >= stack.getMaxDamage();
    }

    /** {@code Item.getDefaultAttributeModifiers(slot)}: the attribute modifiers the item carries in its components for that slot. */
    public static Multimap<Attribute, AttributeModifier> defaultAttributeModifiers(Item item, EquipmentSlot slot) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> out = ImmutableMultimap.builder();
        for (var entry : item.components().getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
                net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).modifiers()) {
            if (entry.slot().test(slot)) out.put(entry.attribute().value(), entry.modifier());
        }
        return out.build();
    }

    public static UUID baseAttackDamageUuid() {
        return UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    }

    public static UUID baseAttackSpeedUuid() {
        return UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
    }

    public static void readShareTag(Item item, ItemStack stack, CompoundTag tag) {
        ItemStackNbt.setTag(stack, tag);
    }

    /** {@code ItemAttributeModifierEvent.addModifier(Attribute, AttributeModifier)} (the event is per stack now, the modifier goes to the main hand). */
    public static boolean attrEventAdd(ItemAttributeModifierEvent event, Attribute attribute, AttributeModifier modifier) {
        event.addModifier(HolderCompat.holder(attribute), modifier, net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        return true;
    }

    public static EquipmentSlot attrEventSlot(ItemAttributeModifierEvent event) {
        return EquipmentSlot.MAINHAND;
    }

    public static CompoundTag handlerSerialize(ItemStackHandler handler) {
        return handler.serializeNBT(MigrateContext.provider());
    }

    public static void handlerDeserialize(ItemStackHandler handler, CompoundTag tag) {
        handler.deserializeNBT(MigrateContext.provider(), tag);
    }

    @SuppressWarnings("unchecked")
    public static LazyOptional<net.neoforged.neoforge.items.IItemHandlerModifiable>[] sidedWrappers(WorldlyContainer container, Direction... sides) {
        LazyOptional<net.neoforged.neoforge.items.IItemHandlerModifiable>[] out = new LazyOptional[sides.length];
        for (int i = 0; i < sides.length; i++) {
            Direction side = sides[i];
            out[i] = LazyOptional.of(() -> new SidedInvWrapper(container, side));
        }
        return out;
    }

    public static boolean addBrewingRecipe(Object recipe) {
        return false; // brewing recipes are data/event driven in 1.21.1 (RegisterBrewingRecipesEvent)
    }

    // ------------------------------------------------------------------ blocks and fluids

    /** {@code new StairBlock(() -> base.defaultBlockState(), props)}: the base state now, falling back to stone while the base block is not registered yet. */
    public static BlockState stairBase(Supplier<BlockState> base) {
        try {
            BlockState state = base.get();
            if (state != null) return state;
        } catch (RuntimeException e) {
            // base block registered later
        }
        return net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
    }

    public static FlowingFluid fluidOf(Supplier<? extends FlowingFluid> fluid) {
        return fluid.get();
    }

    public static Fluid bucketFluid(Supplier<? extends Fluid> fluid) {
        return fluid.get();
    }

    /** The effect of a {@code FlowerBlock}: a registry object is already a holder and stays lazy. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Holder<MobEffect> effectHolder(Supplier<MobEffect> effect) {
        if (effect instanceof Holder holder) return holder;
        return HolderCompat.holder(effect.get());
    }

    // ------------------------------------------------------------------ attributes that moved from NeoForge to vanilla

    private static DeferredHolder<Attribute, Attribute> attribute(Holder<Attribute> holder) {
        return DeferredHolder.create(Registries.ATTRIBUTE, holder.unwrapKey().orElseThrow().location());
    }

    public static DeferredHolder<Attribute, Attribute> blockReach() {
        return attribute(Attributes.BLOCK_INTERACTION_RANGE);
    }

    public static DeferredHolder<Attribute, Attribute> entityReach() {
        return attribute(Attributes.ENTITY_INTERACTION_RANGE);
    }

    public static DeferredHolder<Attribute, Attribute> entityGravity() {
        return attribute(Attributes.GRAVITY);
    }

    public static DeferredHolder<Attribute, Attribute> stepHeightAddition() {
        return attribute(Attributes.STEP_HEIGHT);
    }

    public static DeferredHolder<Attribute, Attribute> swimSpeed() {
        return attribute(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED);
    }
}
