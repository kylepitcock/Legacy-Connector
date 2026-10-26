package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockDoublePlant}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockDoublePlant extends legacyconnector.v1_7_10.net.minecraft.block.BlockBush implements legacyconnector.v1_7_10.net.minecraft.block.IGrowable {
    public BlockDoublePlant(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockDoublePlant(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockDoublePlant() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.vine);
    }

    /** The plant types by the low three metadata bits; bit 3 marks the upper half. */
    public static final String[] field_149892_a = {"sunflower", "syringa", "grass", "fern", "rose", "paeonia"};
    protected legacyconnector.v1_7_10.net.minecraft.util.IIcon[] doublePlantBottomIcons;
    protected legacyconnector.v1_7_10.net.minecraft.util.IIcon[] doublePlantTopIcons;
    protected legacyconnector.v1_7_10.net.minecraft.util.IIcon[] sunflowerIcons;

    /** Whether the metadata is the upper half's. */
    public static boolean func_149887_c(int meta) {
        return (meta & 8) != 0;
    }

    /** The plant type of a lower half's metadata. */
    public static int func_149890_d(int meta) {
        return meta & 7;
    }

    /** The plant type at the position, read from the lower half. */
    public int func_149885_e(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        return !func_149887_c(meta) ? meta & 7 : world.getBlockMetadata(x, y - 1, z) & 7;
    }

    /** The icon of a plant type's upper or lower half. */
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon func_149888_a(boolean top, int type) {
        legacyconnector.v1_7_10.net.minecraft.util.IIcon[] icons = top ? doublePlantTopIcons : doublePlantBottomIcons;
        if (icons != null && type >= 0 && type < icons.length && icons[type] != null) return icons[type];
        if (type < 0 || type >= field_149892_a.length) type = 0;
        return mcc$vanillaIcon("double_plant_" + field_149892_a[type] + (top ? "_top" : "_bottom"));
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getIcon(int side, int meta) {
        return func_149887_c(meta) ? func_149888_a(false, 0) : func_149888_a(false, meta & 7);
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        doublePlantBottomIcons = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[field_149892_a.length];
        doublePlantTopIcons = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[field_149892_a.length];
        for (int i = 0; i < field_149892_a.length; ++i) {
            doublePlantBottomIcons[i] = register.registerIcon(getTextureName() + "_" + field_149892_a[i] + "_bottom");
            doublePlantTopIcons[i] = register.registerIcon(getTextureName() + "_" + field_149892_a[i] + "_top");
        }
        sunflowerIcons = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[] {register.registerIcon(getTextureName() + "_sunflower_front"),
                register.registerIcon(getTextureName() + "_sunflower_back")};
    }

    /** Places the plant: the lower half with its type and the upper half (metadata 8) above it. */
    public void func_149889_c(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int type, int flags) {
        if (mcc$vanilla != null) {
            // the modern upper half is its own plant's (metadata 8 alone would flatten to a sunflower top)
            var lower = org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.toModern(this, type);
            var half = net.minecraft.world.level.block.DoublePlantBlock.HALF;
            if (lower.hasProperty(half)) {
                if (world.setBlock(x, y, z, this, type, flags) && world.mcc$level() != null) {
                    world.mcc$level().setBlock(new net.minecraft.core.BlockPos(x, y + 1, z),
                            lower.setValue(half, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), flags);
                }
                return;
            }
        }
        world.setBlock(x, y, z, this, type, flags);
        world.setBlock(x, y + 1, z, this, 8, flags);
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
        // the vanilla block's own bone meal handling runs on the modern block
    }
}
