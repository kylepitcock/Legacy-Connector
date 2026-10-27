package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** The screen overlay of fire, water or a block the camera is in; cancelling hides it. */
@legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
public class RenderBlockOverlayEvent extends legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event {
    public enum OverlayType {
        FIRE,
        BLOCK,
        WATER
    }

    private final EntityPlayer player;
    private final float renderPartialTicks;
    private final OverlayType overlayType;
    private final IBlockState blockForOverlay;
    private final BlockPos blockPos;

    public RenderBlockOverlayEvent(EntityPlayer player, float renderPartialTicks, OverlayType type, IBlockState block, BlockPos blockPos) {
        this.player = player;
        this.renderPartialTicks = renderPartialTicks;
        this.overlayType = type;
        this.blockForOverlay = block;
        this.blockPos = blockPos;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public float getRenderPartialTicks() {
        return renderPartialTicks;
    }

    public OverlayType getOverlayType() {
        return overlayType;
    }

    public IBlockState getBlockForOverlay() {
        return blockForOverlay;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }
}
