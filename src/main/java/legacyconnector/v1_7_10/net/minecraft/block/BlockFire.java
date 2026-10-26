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

    private legacyconnector.v1_7_10.net.minecraft.util.IIcon[] iconArray;

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        iconArray = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[] {register.registerIcon(getTextureName() + "_layer_0"),
                register.registerIcon(getTextureName() + "_layer_1")};
    }

    /** The two animated fire layers (0, 1) that the fire renderer draws. */
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getFireIcon(int layer) {
        if (iconArray != null && layer >= 0 && layer < iconArray.length && iconArray[layer] != null) return iconArray[layer];
        return mcc$vanillaIcon(layer == 1 ? "fire_layer_1" : "fire_layer_0");
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getIcon(int side, int meta) {
        return getFireIcon(0);
    }

    /** Forge: whether fire can burn the block at the position from that face. */
    public boolean canCatchFire(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection face) {
        return world.getBlock(x, y, z).isFlammable(world, x, y, z, face);
    }

    /** Forge: the larger of {@code oldChance} and how readily the block at the position spreads fire from that face. */
    public int getChanceToEncourageFire(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, int oldChance,
            legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection face) {
        int chance = world.getBlock(x, y, z).getFireSpreadSpeed(world, x, y, z, face);
        return Math.max(chance, oldChance);
    }

    /** Vanilla 1.7.10: {@code canBlockCatchFire}, the face-less check. */
    public boolean canBlockCatchFire(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return canCatchFire(world, x, y, z, legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection.UP);
    }
}
