package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;

/**
 * Forge: the local player is about to be pushed out of blocks it is inside (the box can be shrunk, or the push cancelled). The modern
 * client has no hook for its push-out, so this event is not posted; mods' handlers stay registered and harmless.
 */
@Cancelable
public class PlayerSPPushOutOfBlocksEvent extends PlayerEvent {
    private AxisAlignedBB entityBoundingBox;

    public PlayerSPPushOutOfBlocksEvent(EntityPlayer player, AxisAlignedBB entityBoundingBox) {
        super(player);
        this.entityBoundingBox = entityBoundingBox;
    }

    public void setEntityBoundingBox(AxisAlignedBB entityBoundingBox) {
        this.entityBoundingBox = entityBoundingBox;
    }

    public AxisAlignedBB getEntityBoundingBox() {
        return entityBoundingBox;
    }
}
