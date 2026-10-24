package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;

/** A cobblestone-style wall built from another block. */
public class BlockWall extends Block {
    public BlockWall(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockWall(Material material) {
        super(material);
    }

    public BlockWall(Block block) {
        this(Material.rock);
    }

    public boolean canConnectWallTo(IBlockAccess world, int x, int y, int z) {
        Block other = world.getBlock(x, y, z);
        return other instanceof BlockWall || other instanceof BlockFence || other.isOpaqueCube();
    }
}
