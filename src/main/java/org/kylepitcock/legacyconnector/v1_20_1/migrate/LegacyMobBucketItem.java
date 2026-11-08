package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.entity.animal.Bucketable;

/**
 * {@code new MobBucketItem(Supplier<EntityType>, Supplier<Fluid>, Supplier<SoundEvent>, Properties)} of Forge 1.20.1 (the suppliers let a mod name its own
 * entity type before it is registered); 1.21.1 takes the values. The entity type and sound are read when the bucket is emptied; the fluid (almost always
 * water) when the item is built.
 */
public class LegacyMobBucketItem extends MobBucketItem {
    private final Supplier<? extends EntityType<?>> entityType;
    private final Supplier<? extends SoundEvent> emptySound;

    /** Also the super constructor of a mod bucket class ({@code Mc1201Migration} makes such classes extend this one). */
    @SuppressWarnings("unchecked")
    public LegacyMobBucketItem(Supplier<?> entityType, Supplier<?> fluid, Supplier<?> emptySound, Item.Properties properties) {
        super(EntityType.PIG, contentOf(fluid), SoundEvents.BUCKET_EMPTY_FISH, properties);
        this.entityType = (Supplier<? extends EntityType<?>>) entityType;
        this.emptySound = (Supplier<? extends SoundEvent>) emptySound;
    }

    private static Fluid contentOf(Supplier<?> fluid) {
        try {
            return fluid.get() instanceof Fluid f ? f : Fluids.WATER;
        } catch (RuntimeException e) {
            return Fluids.WATER;
        }
    }

    /** The call a translated {@code new MobBucketItem(..)} makes (through {@code mc-calls.tsv}). */
    public static MobBucketItem create(Supplier<?> entityType, Supplier<?> fluid, Supplier<?> emptySound, Item.Properties properties) {
        return new LegacyMobBucketItem(entityType, fluid, emptySound, properties);
    }

    @Override
    protected void playEmptySound(Player player, LevelAccessor level, BlockPos pos) {
        SoundEvent sound = emptySound.get();
        level.playSound(player, pos, sound, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
    }

    @Override
    public void checkExtraContent(Player player, Level level, ItemStack stack, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        Entity spawned = entityType.get().spawn(server, stack, null, pos, MobSpawnType.BUCKET, true, false);
        if (spawned instanceof Mob && spawned instanceof Bucketable bucketable) {
            CustomData data = stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY);
            bucketable.loadFromBucketTag(data.copyTag());
            bucketable.setFromBucket(true);
        }
    }
}
