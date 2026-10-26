package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;

/** A fence: connects to other fences, fence gates and solid blocks. Mods subclass it for their own fences and walls. */
public class BlockFence extends Block {
    public BlockFence(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockFence(Material material) {
        super(material);
    }

    public BlockFence(String texture, Material material) {
        this(material);
    }

    public boolean canConnectFenceTo(IBlockAccess world, int x, int y, int z) {
        Block other = world.getBlock(x, y, z);
        return other instanceof BlockFence || other.isOpaqueCube();
    }

    public static boolean func_149825_a(Block block) {
        return block instanceof BlockFence;
    }

    /** Vanilla's check whether a block is a fence (fences connect to fences and to fence gates). */
    public static boolean isFence(Block block) {
        return func_149825_a(block);
    }
}
