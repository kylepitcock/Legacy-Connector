package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.block.BlockDaylightDetector;

/** 1.12.2 daylight sensor tile entity: re-reads the sky light once a second (every 20 world ticks). */
public class TileEntityDaylightDetector extends TileEntity implements legacyconnector.v1_12_2.net.minecraft.util.ITickable {
    @Override
    public void update() {
        if (world != null && !world.isRemote && world.getTotalWorldTime() % 20L == 0L) {
            var block = getBlockType();
            if (block instanceof BlockDaylightDetector detector) detector.updatePower(world, getPos());
        }
    }
}
