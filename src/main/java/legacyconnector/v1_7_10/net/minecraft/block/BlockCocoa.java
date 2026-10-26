package legacyconnector.v1_7_10.net.minecraft.block;

/** The 1.7.10 class {@code net.minecraft.block.BlockCocoa}: type hierarchy for casts and instanceof checks (bone meal is handled by the modern block). */
public class BlockCocoa extends BlockDirectional implements IGrowable {
    public BlockCocoa() {
        super(legacyconnector.v1_7_10.net.minecraft.block.material.Material.plants);
    }

    public BlockCocoa(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
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

    public static int func_149987_c(int meta) {
        return (meta & 12) >> 2;
    }

    /** The pod's icons by growth stage (field_149989_a). */
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon[] field_149989_a;

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        field_149989_a = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[3];
        for (int i = 0; i < field_149989_a.length; i++) field_149989_a[i] = register.registerIcon(getTextureName() + "_stage_" + i);
    }

    /** The pod's icon for a growth stage (0-2). */
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getCocoaIcon(int stage) {
        int s = stage < 0 || stage >= 3 ? 2 : stage;
        if (field_149989_a != null && s < field_149989_a.length && field_149989_a[s] != null) return field_149989_a[s];
        return mcc$vanillaIcon("cocoa_stage_" + s);
    }
}
