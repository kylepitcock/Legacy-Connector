package legacyconnector.v1_12_2.net.minecraftforge.event.entity;

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

/** An event about an entity. */
public class EntityEvent extends Event {
    private final Entity entity;

    public EntityEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    /** Fired at the end of every entity's construction (mods register data keys here). */
    public static class EntityConstructing extends EntityEvent {
        public EntityConstructing(Entity entity) {
            super(entity);
        }
    }

    /** Whether an entity outside the ticking area still updates. */
    public static class CanUpdate extends EntityEvent {
        private boolean canUpdate;

        public CanUpdate(Entity entity) {
            super(entity);
        }

        public boolean getCanUpdate() {
            return canUpdate;
        }

        public void setCanUpdate(boolean canUpdate) {
            this.canUpdate = canUpdate;
        }
    }

    /** The entity moved into another chunk. */
    public static class EnteringChunk extends EntityEvent {
        private final int newChunkX;
        private final int newChunkZ;
        private final int oldChunkX;
        private final int oldChunkZ;

        public EnteringChunk(Entity entity, int newChunkX, int newChunkZ, int oldChunkX, int oldChunkZ) {
            super(entity);
            this.newChunkX = newChunkX;
            this.newChunkZ = newChunkZ;
            this.oldChunkX = oldChunkX;
            this.oldChunkZ = oldChunkZ;
        }

        public int getNewChunkX() {
            return newChunkX;
        }

        public int getNewChunkZ() {
            return newChunkZ;
        }

        public int getOldChunkX() {
            return oldChunkX;
        }

        public int getOldChunkZ() {
            return oldChunkZ;
        }
    }
}
