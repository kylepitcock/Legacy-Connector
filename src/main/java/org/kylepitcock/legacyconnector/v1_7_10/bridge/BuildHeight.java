package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import legacyconnector.v1_7_10.net.minecraft.world.ChunkCache;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The build range of the world a legacy method was handed. 1.7.10 code hard-codes 0..255 on its y parameter (RotaryCraft's and
 * ChromatiCraft's {@code ItemBlockPlacer.checkValidBounds}: {@code y >= 0 && y < world.provider.getHeight() - 1}, so machines could
 * not be placed below y=0 of a modern overworld); the loader's BuildHeightRewriter turns {@code y < 0}, {@code y > 0},
 * {@code y >= 256} and {@code y > 255} on such a parameter into comparisons with these. Generation views and worlds that are
 * 0..256 answer 1.7.10's range, so nothing changes there.
 */
public final class BuildHeight {
    private BuildHeight() {}

    /** The lowest y (0 in 1.7.10). */
    public static int bottom(IBlockAccess world) {
        if (world instanceof World w) return w.mcc$buildBottom();
        if (world instanceof ChunkCache c && c.worldObj != null) return c.worldObj.mcc$buildBottom();
        if (world instanceof BlockAccessView v) return v.buildBottom();
        return 0;
    }

    /** One above the highest y (256 in 1.7.10). */
    public static int top(IBlockAccess world) {
        if (world instanceof World w) return w.mcc$buildTop();
        if (world instanceof ChunkCache c && c.worldObj != null) return c.worldObj.mcc$buildTop();
        if (world instanceof BlockAccessView v) return v.buildTop();
        return 256;
    }

    /**
     * {@code floor} blocks above the bottom of the tile entity's world: the loader's WorldFloorClampRewriter turns a tile's
     * {@code if (y < 5) y = 5} on a height picked around itself (Thaumcraft's Arcane Lamp) into a clamp to this, so it means
     * "above the bedrock floor" in a world that starts below y=0 too.
     */
    public static int aboveFloor(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity tile, int floor) {
        World world = tile == null ? null : tile.worldObj;
        return world == null ? floor : world.mcc$buildBottom() + floor;
    }

    /** The highest y (255 in 1.7.10). */
    public static int lastY(IBlockAccess world) {
        return top(world) - 1;
    }
}
