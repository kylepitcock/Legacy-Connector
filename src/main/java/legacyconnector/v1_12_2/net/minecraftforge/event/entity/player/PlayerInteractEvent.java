package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;

/** A player right- or left-clicks a block, an item or an entity. */
public class PlayerInteractEvent extends PlayerEvent {
    private final EnumHand hand;
    private final BlockPos pos;
    private final EnumFacing face;
    private EnumActionResult cancellationResult = EnumActionResult.PASS;

    private PlayerInteractEvent(EntityPlayer player, EnumHand hand, BlockPos pos, EnumFacing face) {
        super(player);
        this.hand = hand;
        this.pos = pos;
        this.face = face;
    }

    public EnumHand getHand() {
        return hand;
    }

    public ItemStack getItemStack() {
        return hand == null ? ItemStack.EMPTY : getEntityPlayer().getHeldItem(hand);
    }

    public BlockPos getPos() {
        return pos;
    }

    public EnumFacing getFace() {
        return face;
    }

    public World getWorld() {
        return getEntityPlayer().getEntityWorld();
    }

    public Side getSide() {
        return getWorld() != null && getWorld().isRemote ? Side.CLIENT : Side.SERVER;
    }

    public EnumActionResult getCancellationResult() {
        return cancellationResult;
    }

    public void setCancellationResult(EnumActionResult result) {
        this.cancellationResult = result;
    }

    @legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
    public static class EntityInteractSpecific extends PlayerInteractEvent {
        private final Vec3d localPos;
        private final Entity target;

        public EntityInteractSpecific(EntityPlayer player, EnumHand hand, Entity target, Vec3d localPos) {
            super(player, hand, target.getPosition(), null);
            this.localPos = localPos;
            this.target = target;
        }

        public Vec3d getLocalPos() {
            return localPos;
        }

        public Entity getTarget() {
            return target;
        }
    }

    @legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
    public static class EntityInteract extends PlayerInteractEvent {
        private final Entity target;

        public EntityInteract(EntityPlayer player, EnumHand hand, Entity target) {
            super(player, hand, target.getPosition(), null);
            this.target = target;
        }

        public Entity getTarget() {
            return target;
        }
    }

    @legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
    public static class RightClickBlock extends PlayerInteractEvent {
        private Event.Result useBlock = Event.Result.DEFAULT;
        private Event.Result useItem = Event.Result.DEFAULT;
        private final Vec3d hitVec;

        public RightClickBlock(EntityPlayer player, EnumHand hand, BlockPos pos, EnumFacing face, Vec3d hitVec) {
            super(player, hand, pos, face);
            this.hitVec = hitVec;
        }

        public Vec3d getHitVec() {
            return hitVec;
        }

        public Event.Result getUseBlock() {
            return useBlock;
        }

        public Event.Result getUseItem() {
            return useItem;
        }

        public void setUseBlock(Event.Result triggerBlock) {
            this.useBlock = triggerBlock;
        }

        public void setUseItem(Event.Result triggerItem) {
            this.useItem = triggerItem;
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

    @legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
    public static class RightClickItem extends PlayerInteractEvent {
        public RightClickItem(EntityPlayer player, EnumHand hand) {
            super(player, hand, player.getPosition(), null);
        }
    }

    public static class RightClickEmpty extends PlayerInteractEvent {
        public RightClickEmpty(EntityPlayer player, EnumHand hand) {
            super(player, hand, player.getPosition(), null);
        }
    }

    @legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
    public static class LeftClickBlock extends PlayerInteractEvent {
        private Event.Result useBlock = Event.Result.DEFAULT;
        private Event.Result useItem = Event.Result.DEFAULT;
        private final Vec3d hitVec;

        public LeftClickBlock(EntityPlayer player, BlockPos pos, EnumFacing face, Vec3d hitVec) {
            super(player, EnumHand.MAIN_HAND, pos, face);
            this.hitVec = hitVec;
        }

        public Vec3d getHitVec() {
            return hitVec;
        }

        public Event.Result getUseBlock() {
            return useBlock;
        }

        public Event.Result getUseItem() {
            return useItem;
        }

        public void setUseBlock(Event.Result triggerBlock) {
            this.useBlock = triggerBlock;
        }

        public void setUseItem(Event.Result triggerItem) {
            this.useItem = triggerItem;
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

    public static class LeftClickEmpty extends PlayerInteractEvent {
        public LeftClickEmpty(EntityPlayer player) {
            super(player, EnumHand.MAIN_HAND, player.getPosition(), null);
        }
    }
}
