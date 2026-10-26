package legacyconnector.v1_7_10.net.minecraftforge.client.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/**
 * Fired before the screen overlay of the block the camera is in (fire, water, a suffocating block) is drawn; cancelling it
 * hides the overlay. Handlers may draw their own overlay instead.
 */
@Cancelable
public class RenderBlockOverlayEvent extends Event {
    public enum OverlayType {
        FIRE, BLOCK, WATER
    }

    public final EntityPlayer player;
    public final float renderPartialTicks;
    public final OverlayType overlayType;
    public final Block blockForOverlay;
    public final int blockX;
    public final int blockY;
    public final int blockZ;

    public RenderBlockOverlayEvent(EntityPlayer player, float renderPartialTicks, OverlayType type, Block block, int blockX, int blockY, int blockZ) {
        this.player = player;
        this.renderPartialTicks = renderPartialTicks;
        this.overlayType = type;
        this.blockForOverlay = block;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
    }
}
