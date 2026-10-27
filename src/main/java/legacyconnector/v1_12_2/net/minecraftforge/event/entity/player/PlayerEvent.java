package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

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

/** An event about a player. */
public class PlayerEvent extends legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingEvent {
    private final EntityPlayer entityPlayer;

    public PlayerEvent(EntityPlayer player) {
        super(player);
        this.entityPlayer = player;
    }

    public EntityPlayer getEntityPlayer() {
        return entityPlayer;
    }

    /** Whether the player can harvest the block (the result can be changed). */
    public static class HarvestCheck extends PlayerEvent {
        private final legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state;
        private boolean success;

        public HarvestCheck(EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, boolean success) {
            super(player);
            this.state = state;
            this.success = success;
        }

        public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getTargetBlock() {
            return state;
        }

        public boolean canHarvest() {
            return success;
        }

        public void setCanHarvest(boolean success) {
            this.success = success;
        }
    }

    /** How fast the player breaks a block (cancel to not break it; the speed can be changed). */
    @Cancelable
    public static class BreakSpeed extends PlayerEvent {
        private final legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state;
        private final float originalSpeed;
        private float newSpeed;
        private final BlockPos pos;

        public BreakSpeed(EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, float original, BlockPos pos) {
            super(player);
            this.state = state;
            this.originalSpeed = original;
            this.newSpeed = original;
            this.pos = pos;
        }

        public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getState() {
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

    /** The player's display name (it can be changed). */
    public static class NameFormat extends PlayerEvent {
        private final String username;
        private String displayname;

        public NameFormat(EntityPlayer player, String username) {
            super(player);
            this.username = username;
            this.displayname = username;
        }

        public String getUsername() {
            return username;
        }

        public String getDisplayname() {
            return displayname;
        }

        public void setDisplayname(String displayname) {
            this.displayname = displayname;
        }
    }

    /** A new player object replaces the old one (respawn after death, or return from the End): copy data from the original. */
    public static class Clone extends PlayerEvent {
        private final EntityPlayer original;
        private final boolean wasDeath;

        public Clone(EntityPlayer newPlayer, EntityPlayer oldPlayer, boolean wasDeath) {
            super(newPlayer);
            this.original = oldPlayer;
            this.wasDeath = wasDeath;
        }

        public EntityPlayer getOriginal() {
            return original;
        }

        public boolean isWasDeath() {
            return wasDeath;
        }
    }

    /** The player starts seeing an entity (it is sent to the player's client). */
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

    /** The player stops seeing an entity. */
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
