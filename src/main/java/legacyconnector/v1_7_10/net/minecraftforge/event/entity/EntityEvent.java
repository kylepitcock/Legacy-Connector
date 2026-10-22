package legacyconnector.v1_7_10.net.minecraftforge.event.entity;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;

public class EntityEvent extends Event {
    public final Entity entity;

    public EntityEvent(Entity entity) {
        this.entity = entity;
    }

    /** Fired at the end of every entity's construction (mods attach attributes and extended properties here). */
    public static class EntityConstructing extends EntityEvent {
        public EntityConstructing(Entity entity) {
            super(entity);
        }
    }

    @HasResult
    public static class CanUpdate extends EntityEvent {
        public boolean canUpdate = false;

        public CanUpdate(Entity entity) {
            super(entity);
        }
    }

    public static class EnteringChunk extends EntityEvent {
        public int newChunkX;
        public int newChunkZ;
        public int oldChunkX;
        public int oldChunkZ;

        public EnteringChunk(Entity entity, int newChunkX, int newChunkZ, int oldChunkX, int oldChunkZ) {
            super(entity);
            this.newChunkX = newChunkX;
            this.newChunkZ = newChunkZ;
            this.oldChunkX = oldChunkX;
            this.oldChunkZ = oldChunkZ;
        }
    }
}
