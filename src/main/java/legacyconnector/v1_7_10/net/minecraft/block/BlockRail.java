package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The plain rail: can curve; a redstone change next to a T-junction flips its curve. */
public class BlockRail extends BlockRailBase {
    public BlockRail(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockRail() {
        super(false);
    }

    @Override
    protected void onRedstoneSignal(World world, int x, int y, int z, int meta, int shape, Block neighbor) {
        if (neighbor.canProvidePower() && new Rail(world, x, y, z).countAdjacentRails() == 3) refreshTrackShape(world, x, y, z, false);
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getIcon(int side, int meta) {
        if (blockIcon == null && mcc$vanilla != null) return mcc$vanillaIcon(meta >= 6 ? "rail_normal_turned" : "rail_normal");
        return blockIcon;
    }
}
