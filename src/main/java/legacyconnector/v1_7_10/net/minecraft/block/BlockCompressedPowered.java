package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.MapColor;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;

/** A powered storage block (the redstone block): a storage block that gives full redstone power on every side. */
public class BlockCompressedPowered extends BlockCompressed {
    public BlockCompressedPowered(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockCompressedPowered(MapColor color) {
        super(color);
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        return 15;
    }
}
