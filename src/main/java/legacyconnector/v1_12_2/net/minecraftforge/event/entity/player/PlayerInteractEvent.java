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

/** A player interacts with a block, an item or an entity. Cancelling cancels the interaction (with the cancellation result). */
public class PlayerInteractEvent extends PlayerEvent {
    private final EnumHand hand;
    private final BlockPos pos;
    private final EnumFacing face;
    private legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult cancellationResult = legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS;

    protected PlayerInteractEvent(EntityPlayer player, EnumHand hand, BlockPos pos, EnumFacing face) {
        super(player);
        this.hand = hand == null ? EnumHand.MAIN_HAND : hand;
        this.pos = pos;
        this.face = face;
    }

    public EnumHand getHand() {
        return hand;
    }

    public ItemStack getItemStack() {
        return getEntityPlayer().getHeldItem(hand);
    }

    public BlockPos getPos() {
        return pos;
    }

    public EnumFacing getFace() {
        return face;
    }

    public World getWorld() {
        return getEntityPlayer().mcc$world();
    }

    public legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side getSide() {
        return getWorld() != null && getWorld().isRemote ? legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side.CLIENT : legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side.SERVER;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult getCancellationResult() {
        return cancellationResult;
    }

    public void setCancellationResult(legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult result) {
        this.cancellationResult = result;
    }

    /** Right-clicking an entity (before the entity's own interaction). */
    @Cancelable
    public static class EntityInteract extends PlayerInteractEvent {
        private final Entity target;

        public EntityInteract(EntityPlayer player, EnumHand hand, Entity target) {
            super(player, hand, new BlockPos(target.mcc$x(), target.mcc$y(), target.mcc$z()), null);
            this.target = target;
        }

        public Entity getTarget() {
            return target;
        }
    }

    /** Right-clicking a point of an entity (armor stands). */
    @Cancelable
    public static class EntityInteractSpecific extends PlayerInteractEvent {
        private final legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d localPos;
        private final Entity target;

        public EntityInteractSpecific(EntityPlayer player, EnumHand hand, Entity target, legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d localPos) {
            super(player, hand, new BlockPos(target.mcc$x(), target.mcc$y(), target.mcc$z()), null);
            this.localPos = localPos;
            this.target = target;
        }

        public legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d getLocalPos() {
            return localPos;
        }

        public Entity getTarget() {
            return target;
        }
    }

    /** Right-clicking a block: the block's own use and the held item's use can be allowed or denied separately. */
    @Cancelable
    public static class RightClickBlock extends PlayerInteractEvent {
        private Event.Result useBlock = Event.Result.DEFAULT;
        private Event.Result useItem = Event.Result.DEFAULT;
        private final legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d hitVec;

        public RightClickBlock(EntityPlayer player, EnumHand hand, BlockPos pos, EnumFacing face, legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d hitVec) {
            super(player, hand, pos, face);
            this.hitVec = hitVec;
        }

        public legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d getHitVec() {
            return hitVec;
        }

        public Event.Result getUseBlock() {
            return useBlock;
        }

        public Event.Result getUseItem() {
            return useItem;
        }

        public void setUseBlock(Event.Result result) {
            this.useBlock = result;
        }

        public void setUseItem(Event.Result result) {
            this.useItem = result;
        }

        @Override
        public void setCanceled(boolean canceled) {
            super.setCanceled(canceled);
            if (canceled) {
                useBlock = Event.Result.DENY;
                useItem = Event.Result.DENY;
            }
        }
    }

    /** Right-clicking with an item in the air (or after the block did nothing). */
    @Cancelable
    public static class RightClickItem extends PlayerInteractEvent {
        public RightClickItem(EntityPlayer player, EnumHand hand) {
            super(player, hand, new BlockPos(player.mcc$x(), player.mcc$y(), player.mcc$z()), null);
        }
    }

    /** Client: right-clicking at nothing with an empty hand. */
    public static class RightClickEmpty extends PlayerInteractEvent {
        public RightClickEmpty(EntityPlayer player, EnumHand hand) {
            super(player, hand, new BlockPos(player.mcc$x(), player.mcc$y(), player.mcc$z()), null);
        }
    }

    /** Left-clicking a block (starting to break it). */
    @Cancelable
    public static class LeftClickBlock extends PlayerInteractEvent {
        private Event.Result useBlock = Event.Result.DEFAULT;
        private Event.Result useItem = Event.Result.DEFAULT;
        private final legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d hitVec;

        public LeftClickBlock(EntityPlayer player, BlockPos pos, EnumFacing face, legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d hitVec) {
            super(player, EnumHand.MAIN_HAND, pos, face);
            this.hitVec = hitVec;
        }

        public legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d getHitVec() {
            return hitVec;
        }

        public Event.Result getUseBlock() {
            return useBlock;
        }

        public Event.Result getUseItem() {
            return useItem;
        }

        public void setUseBlock(Event.Result result) {
            this.useBlock = result;
        }

        public void setUseItem(Event.Result result) {
            this.useItem = result;
        }

        @Override
        public void setCanceled(boolean canceled) {
            super.setCanceled(canceled);
            if (canceled) {
                useBlock = Event.Result.DENY;
                useItem = Event.Result.DENY;
            }
        }
    }

    /** Client: left-clicking at nothing. */
    public static class LeftClickEmpty extends PlayerInteractEvent {
        public LeftClickEmpty(EntityPlayer player) {
            super(player, EnumHand.MAIN_HAND, new BlockPos(player.mcc$x(), player.mcc$y(), player.mcc$z()), null);
        }
    }
}
