package legacyconnector.v1_12_2.net.minecraftforge.event.entity.living;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Spawning of a mob at a position. */
public class LivingSpawnEvent extends LivingEvent {
    private final World world;
    private final float x;
    private final float y;
    private final float z;

    public LivingSpawnEvent(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity, World world, float x, float y, float z) {
        super(entity);
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public World getWorld() {
        return world;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    /** Whether the mob may spawn here (ALLOW forces it, DENY prevents it, DEFAULT uses the mob's own check). */
    @Event.HasResult
    public static class CheckSpawn extends LivingSpawnEvent {
        private final boolean isSpawner;

        public CheckSpawn(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity, World world, float x, float y, float z, boolean isSpawner) {
            super(entity, world, x, y, z);
            this.isSpawner = isSpawner;
        }

        public boolean isSpawner() {
            return isSpawner;
        }

        public boolean fromSpawner() {
            return isSpawner;
        }
    }

    /** A mob is about to be set up after spawning (cancel to skip its own set-up). */
    @Cancelable
    public static class SpecialSpawn extends LivingSpawnEvent {
        public SpecialSpawn(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity, World world, float x, float y, float z) {
            super(entity, world, x, y, z);
        }
    }

    /** Whether a mob may despawn now (ALLOW forces it, DENY keeps it, DEFAULT uses its own rules). */
    @Event.HasResult
    public static class AllowDespawn extends LivingSpawnEvent {
        public AllowDespawn(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity) {
            super(entity, entity.mcc$world(), (float) entity.mcc$x(), (float) entity.mcc$y(), (float) entity.mcc$z());
        }
    }
}
