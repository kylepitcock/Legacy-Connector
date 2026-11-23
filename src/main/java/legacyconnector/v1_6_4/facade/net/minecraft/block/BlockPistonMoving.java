package legacyconnector.v1_6_4.facade.net.minecraft.block;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code BlockPistonMoving} for 1.6.4 and older mods: the members that take the pushed block as a numeric id. Never instantiated. */
public final class BlockPistonMoving {
    private BlockPistonMoving() {}

    /** {@code BlockPistonMoving.getTileEntity(blockId, meta, orientation, extending, renderHead)}: the moving-piston tile entity of the block with that id. */
    public static legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity getTileEntity(int blockId, int meta, int orientation, boolean extending, boolean renderHead) {
        return legacyconnector.v1_7_10.net.minecraft.block.BlockPistonMoving.getTileEntity(Ids.block(blockId), meta, orientation, extending, renderHead);
    }

    /** {@code getAxisAlignedBB(world, x, y, z, blockId, progress, direction)}: the collision box of the pushed block at its position along the push. */
    public static legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB getAxisAlignedBB(legacyconnector.v1_7_10.net.minecraft.block.BlockPistonMoving self,
            legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int blockId, float progress, int direction) {
        return self.func_149964_a(world, x, y, z, Ids.block(blockId), progress, direction);
    }
}
