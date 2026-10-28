package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** 1.12.2's end gateway: where it sends entities. A gateway of the game is seen through a view that sets the modern gateway's exit. */
public class TileEntityEndGateway extends TileEntityEndPortal {
    private BlockPos exitPortal;
    private boolean exactTeleport;
    private final net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity mcc$gateway;

    public TileEntityEndGateway() {
        this.mcc$gateway = null;
    }

    /** A view of a game end gateway. */
    public TileEntityEndGateway(net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity gateway) {
        this.mcc$gateway = gateway;
    }

    /** Sends entities to exactly this position (not the nearest safe spot). */
    public void setExactPosition(BlockPos p_190603_1_) {
        exactTeleport = true;
        exitPortal = p_190603_1_;
        if (mcc$gateway != null && p_190603_1_ != null) {
            mcc$gateway.setExitPosition(new net.minecraft.core.BlockPos(p_190603_1_.getX(), p_190603_1_.getY(), p_190603_1_.getZ()), true);
        }
        markDirty();
    }

    /** Sends the entity through the gateway to its exit (as the modern gateway would on contact). */
    public void teleportEntity(Entity entityIn) {
        if (entityIn == null || entityIn.mcc$entity == null || world == null || world.isRemote) return;
        BlockPos target = exitPortal;
        if (target == null && mcc$gateway != null) {
            var level = world.mcc$level();
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                var exit = mcc$gateway.getPortalPosition(server, mcc$gateway.getBlockPos());
                if (exit != null) target = new BlockPos((int) Math.floor(exit.x), (int) Math.floor(exit.y), (int) Math.floor(exit.z));
            }
        }
        if (target == null) return;
        var e = entityIn.mcc$entity;
        e.teleportTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
    }
}
