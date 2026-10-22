package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockLeaves}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockLeaves extends legacyconnector.v1_7_10.net.minecraft.block.BlockLeavesBase {
    public BlockLeaves(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockLeaves(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockLeaves() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.leaves);
    }

    private static final String[] LEAVES = {"leaves_oak", "leaves_spruce", "leaves_birch", "leaves_jungle"};

    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getIcon(int side, int meta) {
        if (mcc$vanilla == null) return super.getIcon(side, meta);
        return mcc$vanillaIcon(LEAVES[meta & 3]);
    }

    @Override
    public int getRenderColor(int meta) {
        return (meta & 3) == 1 ? 6396257 : (meta & 3) == 2 ? 8431445 : 4764952;
    }

    @Override
    public int colorMultiplier(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z) & 3;
        if (meta == 1) return 6396257;
        if (meta == 2) return 8431445;
        return world.mcc$blockTint(x, y, z, 1);
    }
}
