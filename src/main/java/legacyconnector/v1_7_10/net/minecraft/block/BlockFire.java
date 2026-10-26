package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockFire}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockFire extends legacyconnector.v1_7_10.net.minecraft.block.Block {
    public BlockFire(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockFire(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    protected BlockFire() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.fire);
    }

    private static final java.util.Map<legacyconnector.v1_7_10.net.minecraft.block.Block, int[]> FIRE_INFO = new java.util.concurrent.ConcurrentHashMap<>();

    /** How readily fire spreads onto the block ({@code encouragement}) and how fast it burns away ({@code flammability}). */
    public void setFireInfo(legacyconnector.v1_7_10.net.minecraft.block.Block block, int encouragement, int flammability) {
        if (block == null) return;
        FIRE_INFO.put(block, new int[] {encouragement, flammability});
    }

    public int getEncouragement(legacyconnector.v1_7_10.net.minecraft.block.Block block) {
        int[] info = FIRE_INFO.get(block);
        return info == null ? 0 : info[0];
    }

    public int getFlammability(legacyconnector.v1_7_10.net.minecraft.block.Block block) {
        int[] info = FIRE_INFO.get(block);
        return info == null ? 0 : info[1];
    }

    /** The two animated fire layers. */
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon[] field_149850_M;

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        field_149850_M = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[] {register.registerIcon(getTextureName() + "_layer_0"), register.registerIcon(getTextureName() + "_layer_1")};
        blockIcon = field_149850_M[0];
    }

    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getFireIcon(int layer) {
        if (field_149850_M != null && layer >= 0 && layer < field_149850_M.length && field_149850_M[layer] != null) return field_149850_M[layer];
        return mcc$vanillaIcon("fire_layer_" + (layer == 1 ? 1 : 0));
    }

    /** Whether fire can spread onto the block at a position. */
    public boolean canBlockCatchFire(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return getEncouragement(block) > 0 || block.isFlammable(world, x, y, z, legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection.UP);
    }

    /** Forge: whether the block at a position burns on the given face. */
    public boolean canCatchFire(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection face) {
        return world.getBlock(x, y, z).isFlammable(world, x, y, z, face);
    }
}
