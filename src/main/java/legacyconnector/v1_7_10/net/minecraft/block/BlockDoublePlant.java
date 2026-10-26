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

    public static final String[] field_149892_a = {"sunflower", "syringa", "grass", "fern", "rose", "paeonia"};
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon[] doublePlantBottomIcons, doublePlantTopIcons, sunflowerIcons;

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        doublePlantBottomIcons = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[field_149892_a.length];
        doublePlantTopIcons = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[field_149892_a.length];
        for (int i = 0; i < field_149892_a.length; i++) {
            doublePlantBottomIcons[i] = register.registerIcon("double_plant_" + field_149892_a[i] + "_bottom");
            doublePlantTopIcons[i] = register.registerIcon("double_plant_" + field_149892_a[i] + "_top");
        }
        sunflowerIcons = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[] {register.registerIcon("double_plant_sunflower_front"), register.registerIcon("double_plant_sunflower_back")};
    }

    /** Whether the metadata is the upper half. */
    public static boolean func_149887_c(int meta) {
        return (meta & 8) != 0;
    }

    /** The plant type of a lower half's metadata. */
    public static int func_149890_d(int meta) {
        return meta & 7;
    }

    /** The icon of the upper or lower half of a plant type. */
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon func_149888_a(boolean top, int type) {
        legacyconnector.v1_7_10.net.minecraft.util.IIcon[] icons = top ? doublePlantTopIcons : doublePlantBottomIcons;
        int t = Math.max(0, Math.min(type, field_149892_a.length - 1));
        if (icons != null && t < icons.length && icons[t] != null) return icons[t];
        return mcc$vanillaIcon("double_plant_" + field_149892_a[t] + (top ? "_top" : "_bottom"));
    }

    /** The sunflower head's front (0) and back (1) icons. */
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon mcc$sunflowerIcon(int side) {
        if (sunflowerIcons != null && sunflowerIcons[side] != null) return sunflowerIcons[side];
        return mcc$vanillaIcon(side == 0 ? "double_plant_sunflower_front" : "double_plant_sunflower_back");
    }
}
