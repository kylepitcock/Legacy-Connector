package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
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
 * Forge 1.20.1's {@code MobBucketItem(Supplier entity, Supplier fluid, Supplier sound, Properties)}: Forge patched the vanilla class to take suppliers so
 * that registry objects can be named before they exist. 1.21.1 takes the values, so the entity type and the sound are resolved when first needed and
 * the placeholders given to the vanilla constructor are never used for spawning. The fluid is read at construction (it is nearly always a vanilla fluid).
 */
public class LegacyMobBucketItem extends MobBucketItem {
    private final Supplier<? extends EntityType<?>> entity;
    private final Supplier<? extends SoundEvent> sound;

    public LegacyMobBucketItem(Supplier<? extends EntityType<?>> entity, Supplier<? extends Fluid> fluid, Supplier<? extends SoundEvent> sound, Properties properties) {
        super(EntityType.PIG, fluid(fluid), SoundEvents.BUCKET_EMPTY_FISH, properties);
        this.entity = entity;
        this.sound = sound;
    }

    /** The 1.21.1 form, for code that passes values. */
    public LegacyMobBucketItem(EntityType<?> entity, Fluid fluid, SoundEvent sound, Properties properties) {
        this(() -> entity, () -> fluid, () -> sound, properties);
    }

    private static Fluid fluid(Supplier<? extends Fluid> supplier) {
        try {
            Fluid f = supplier.get();
            if (f != null) return f;
        } catch (RuntimeException e) {
            // not registered yet: water is what a mob bucket holds
        }
        return Fluids.WATER;
    }

    @Override
    public void checkExtraContent(Player player, Level level, ItemStack stack, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            Entity spawned = entity.get().spawn(server, stack, null, pos, MobSpawnType.BUCKET, true, false);
            if (spawned instanceof Bucketable bucketable) {
                CustomData data = stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY);
                bucketable.loadFromBucketTag(data.copyTag());
                bucketable.setFromBucket(true);
            }
            level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        }
    }

    @Override
    protected void playEmptySound(Player player, LevelAccessor level, BlockPos pos) {
        level.playSound(player, pos, sound.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
    }
}
