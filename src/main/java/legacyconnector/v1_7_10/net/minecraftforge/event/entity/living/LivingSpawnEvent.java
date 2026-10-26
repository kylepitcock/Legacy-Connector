package legacyconnector.v1_7_10.net.minecraftforge.event.entity.living;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.world.World;

public class LivingSpawnEvent extends LivingEvent {
    public final World world;
    public final float x;
    public final float y;
    public final float z;

    public LivingSpawnEvent(EntityLivingBase entity, World world, float x, float y, float z) {
        super(entity);
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** A natural spawn position is being checked: ALLOW forces it, DENY forbids it. */
    @Event.HasResult
    public static class CheckSpawn extends LivingSpawnEvent {
        public CheckSpawn(EntityLivingBase entity, World world, float x, float y, float z) {
            super(entity, world, x, y, z);
        }
    }

    /** A mob far from players may despawn: ALLOW forces it, DENY keeps the mob. */
    @Event.HasResult
    public static class AllowDespawn extends LivingSpawnEvent {
        public AllowDespawn(EntityLivingBase entity) {
            super(entity, entity.mcc$world(), (float) entity.mcc$x(), (float) entity.mcc$y(), (float) entity.mcc$z());
        }
    }

    /** A mob has been placed and is about to be initialised. */
    @Cancelable
    public static class SpecialSpawn extends LivingSpawnEvent {
        public SpecialSpawn(EntityLivingBase entity, World world, float x, float y, float z) {
            super(entity, world, x, y, z);
        }
    }
}
