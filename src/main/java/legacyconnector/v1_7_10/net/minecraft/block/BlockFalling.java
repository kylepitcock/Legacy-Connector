package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockFalling}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockFalling extends legacyconnector.v1_7_10.net.minecraft.block.Block {
    public BlockFalling(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockFalling(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockFalling() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.rock);
    }

    /** True if a falling block can fall into the block at x, y, z: air, fire, water or lava. */
    public static boolean canFallBelow(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == null || block.isAir(world, x, y, z)) return true;
        if (block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.fire) return true;
        legacyconnector.v1_7_10.net.minecraft.block.material.Material m = block.getMaterial();
        return m == legacyconnector.v1_7_10.net.minecraft.block.material.Material.water || m == legacyconnector.v1_7_10.net.minecraft.block.material.Material.lava;
    }
}
