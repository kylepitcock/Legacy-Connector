package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/** Common-side 1.20.1 members that 1.21.1 reshaped, ATM 9 gap round 8 (rows in {@code mc-calls.tsv}). */
public final class Round8Compat {
    private Round8Compat() {
    }

    /**
     * {@code new InventoryChangeTrigger.TriggerInstance(player, occupied, full, empty, items)}: the player predicate is optional and the three slot bounds
     * are one {@code Slots} record now; the item list is a list.
     */
    public static InventoryChangeTrigger.TriggerInstance newInventoryChange(ContextAwarePredicate player, MinMaxBounds.Ints occupied, MinMaxBounds.Ints full,
            MinMaxBounds.Ints empty, ItemPredicate[] items) {
        return new InventoryChangeTrigger.TriggerInstance(Optional.ofNullable(player),
                new InventoryChangeTrigger.TriggerInstance.Slots(occupied, full, empty), List.of(items));
    }

    /** Extra leading argument of {@code new BucketPickupHandlerWrapper(pickup, level, pos)}: the player who picks up is optional (null) now. */
    public static Player noPlayer(Level level) {
        return null;
    }

    /** Extra trailing argument of {@code new MobEffectEvent.Remove(entity, instance)}: the cure that removed the effect is unknown to the 1.20.1 caller. */
    public static EffectCure noCure(MobEffectInstance instance) {
        return null;
    }

    /** {@code new OrCondition(ICondition...)} / {@code new AndCondition(...)}: a list now. */
    public static List<ICondition> conditions(ICondition[] conditions) {
        return List.of(conditions);
    }

    /** {@code BlockSnapshot.restore(force, notifyNeighbors)}: the flags are one int now; without {@code force} the block must still be what the snapshot was taken after. */
    public static boolean restoreSnapshot(BlockSnapshot snapshot, boolean force, boolean notifyNeighbors) {
        LevelAccessor level = snapshot.getLevel();
        BlockPos pos = snapshot.getPos();
        BlockState current = level.getBlockState(pos);
        if (!force && current.getBlock() != snapshot.getCurrentState().getBlock()) return false;
        return snapshot.restoreToLocation(level, pos, notifyNeighbors ? 3 : 2);
    }

    /** {@code FinalizeSpawnEvent.getSpawnTag()}: the spawn NBT of a spawner is not part of the 1.21.1 event; no tag. */
    public static CompoundTag spawnTag(FinalizeSpawnEvent event) {
        return null;
    }

    /** {@code new MobEffectInstance(effect, duration, amplifier, ambient, visible, showIcon, hiddenEffect)}: a holder now. */
    public static net.minecraft.world.effect.MobEffectInstance newEffectHidden(net.minecraft.world.effect.MobEffect e, int duration, int amplifier, boolean ambient,
            boolean visible, boolean showIcon, net.minecraft.world.effect.MobEffectInstance hidden) {
        return new net.minecraft.world.effect.MobEffectInstance(HolderCompat.holder(e), duration, amplifier, ambient, visible, showIcon, hidden);
    }

    /** The 1.20.1 constructor with an {@code Optional<FactorData>} (the fade-in data of the darkness effect, gone in 1.21.1): the data is dropped. */
    public static net.minecraft.world.effect.MobEffectInstance newEffectFactor(net.minecraft.world.effect.MobEffect e, int duration, int amplifier, boolean ambient,
            boolean visible, boolean showIcon, net.minecraft.world.effect.MobEffectInstance hidden, java.util.Optional<?> factorData) {
        return newEffectHidden(e, duration, amplifier, ambient, visible, showIcon, hidden);
    }

    /** {@code new RecipeManager()}: the registry lookup is a constructor argument now (the server sets the real one when it loads the recipes). */
    public static net.minecraft.world.item.crafting.RecipeManager newRecipeManager() {
        return new net.minecraft.world.item.crafting.RecipeManager(net.minecraft.core.RegistryAccess.EMPTY);
    }

    /** Extra arguments of {@code new ClientboundExplodePacket(x, y, z, power, blocks, knockback)}: the 1.20.1 packet carried none of them. */
    public static net.minecraft.world.level.Explosion.BlockInteraction explodeInteraction(float power) {
        return net.minecraft.world.level.Explosion.BlockInteraction.DESTROY;
    }

    public static net.minecraft.core.particles.ParticleOptions explodeSmall(float power) {
        return net.minecraft.core.particles.ParticleTypes.EXPLOSION;
    }

    public static net.minecraft.core.particles.ParticleOptions explodeLarge(float power) {
        return net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER;
    }

    public static net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> explodeSound(float power) {
        return net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE;
    }

    /** The background of an advancement is optional in {@code new DisplayInfo(...)}. */
    public static java.util.Optional<net.minecraft.resources.ResourceLocation> optionalLocation(net.minecraft.resources.ResourceLocation location) {
        return java.util.Optional.ofNullable(location);
    }
}
