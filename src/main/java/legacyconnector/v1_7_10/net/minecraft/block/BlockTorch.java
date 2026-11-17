package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** A wall or floor torch (and the base of mods' torches). */
public class BlockTorch extends Block {
    public BlockTorch(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockTorch() {
        super(Material.circuits);
    }

    protected BlockTorch(Material material) {
        super(material);
    }

    /** Whether a torch can stand on the block at the position: it has a solid top surface (or the block says torches may sit on it). */
    public boolean canPlaceTorchOn(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        if (legacyconnector.v1_7_10.net.minecraft.world.World.doesBlockHaveSolidTopSurface(world, x, y, z)) return true;
        Block block = world.getBlock(x, y, z);
        return block != null && block.canPlaceTorchOnTop(world, x, y, z);
    }
}
