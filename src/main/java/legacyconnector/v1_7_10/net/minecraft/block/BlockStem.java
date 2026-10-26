package legacyconnector.v1_7_10.net.minecraft.block;

/** The 1.7.10 class {@code net.minecraft.block.BlockStem}: type hierarchy for casts and instanceof checks (bone meal itself is handled by the modern block). */
public class BlockStem extends legacyconnector.v1_7_10.net.minecraft.block.BlockBush implements legacyconnector.v1_7_10.net.minecraft.block.IGrowable {
    public BlockStem(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockStem() {
        super();
    }

    @Override
    public boolean canFertilize(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, boolean isClient) {
        return true;
    }

    @Override
    public boolean shouldFertilize(legacyconnector.v1_7_10.net.minecraft.world.World world, java.util.Random random, int x, int y, int z) {
        return true;
    }

    @Override
    public void fertilize(legacyconnector.v1_7_10.net.minecraft.world.World world, java.util.Random random, int x, int y, int z) {
    }

    /** The fruit the stem grows (pumpkin or melon). */
    public Block field_149877_a;
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon field_149876_b;

    protected BlockStem(Block fruit) {
        this();
        this.field_149877_a = fruit;
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        blockIcon = register.registerIcon(getTextureName() + "_disconnected");
        field_149876_b = register.registerIcon(getTextureName() + "_connected");
    }

    /** The bent stem's icon (pointing at its fruit). */
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getStemIcon() {
        if (field_149876_b != null) return field_149876_b;
        return mcc$vanillaIcon(mcc$vanilla != null && mcc$vanilla.legacyName().endsWith("melon_stem") ? "melon_stem_connected" : "pumpkin_stem_connected");
    }

    /** The direction of the grown fruit next to a full-grown stem (0 -x, 1 +x, 2 -z, 3 +z), -1 if none. */
    public int getState(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        if (meta < 7) return -1;
        Block fruit = field_149877_a;
        if (fruit == null && mcc$vanilla != null) {
            fruit = mcc$vanilla.legacyName().endsWith("melon_stem") ? legacyconnector.v1_7_10.net.minecraft.init.Blocks.melon_block : legacyconnector.v1_7_10.net.minecraft.init.Blocks.pumpkin;
        }
        if (world.getBlock(x - 1, y, z) == fruit) return 0;
        if (world.getBlock(x + 1, y, z) == fruit) return 1;
        if (world.getBlock(x, y, z - 1) == fruit) return 2;
        return world.getBlock(x, y, z + 1) == fruit ? 3 : -1;
    }
}
