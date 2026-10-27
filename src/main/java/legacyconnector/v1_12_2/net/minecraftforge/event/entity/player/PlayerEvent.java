package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingEvent;

/** Base of Forge's player events (FML's login and tick events are in {@code fml.common.gameevent}). */
public class PlayerEvent extends LivingEvent {
    private final EntityPlayer entityPlayer;

    public PlayerEvent(EntityPlayer player) {
        super(player);
        this.entityPlayer = player;
    }

    public EntityPlayer getEntityPlayer() {
        return entityPlayer;
    }

    /** A player was copied into a new entity on respawn or on returning from the End. */
    public static class Clone extends PlayerEvent {
        private final EntityPlayer original;
        private final boolean wasDeath;

        public Clone(EntityPlayer player, EntityPlayer original, boolean wasDeath) {
            super(player);
            this.original = original;
            this.wasDeath = wasDeath;
        }

        public EntityPlayer getOriginal() {
            return original;
        }

        public boolean isWasDeath() {
            return wasDeath;
        }
    }

    @legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
    public static class BreakSpeed extends PlayerEvent {
        private final IBlockState state;
        private final float originalSpeed;
        private float newSpeed;
        private final BlockPos pos;

        public BreakSpeed(EntityPlayer player, IBlockState state, float original, BlockPos pos) {
            super(player);
            this.state = state;
            this.originalSpeed = original;
            this.newSpeed = original;
            this.pos = pos;
        }

        public IBlockState getState() {
            return state;
        }

        public float getOriginalSpeed() {
            return originalSpeed;
        }

        public float getNewSpeed() {
            return newSpeed;
        }

        public void setNewSpeed(float newSpeed) {
            this.newSpeed = newSpeed;
        }

        public BlockPos getPos() {
            return pos;
        }
    }

    public static class StartTracking extends PlayerEvent {
        private final Entity target;

        public StartTracking(EntityPlayer player, Entity target) {
            super(player);
            this.target = target;
        }

        public Entity getTarget() {
            return target;
        }
    }

    public static class StopTracking extends PlayerEvent {
        private final Entity target;

        public StopTracking(EntityPlayer player, Entity target) {
            super(player);
            this.target = target;
        }

        public Entity getTarget() {
            return target;
        }
    }
}
