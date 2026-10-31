package legacyconnector.v1_7_10.net.minecraft.block;

/** 1.7.10 {@code BlockLeaves}: vanilla leaf icons and tints, and the fast/fancy graphics switch mods call. */
public class BlockLeaves extends legacyconnector.v1_7_10.net.minecraft.block.BlockLeavesBase implements legacyconnector.v1_7_10.net.minecraftforge.common.IShearable {
    public BlockLeaves(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        // The vanilla oak/spruce/birch/jungle leaves (BlockOldLeaf in 1.7.10) had their icons here after registerBlockIcons.
        mcc$vanillaIcons(new String[][] {LEAVES, {"leaves_oak_opaque", "leaves_spruce_opaque", "leaves_birch_opaque", "leaves_jungle_opaque"}});
    }

    /**
     * 1.7.10 {@code BlockLeaves.field_150129_M}: the leaf icons by graphics level (0 fancy, 1 fast), then leaf type, filled by
     * BlockOldLeaf/BlockNewLeaf's registerBlockIcons. Mods read vanilla's: ChromatiCraft's glowing leaves return
     * {@code Blocks.leaves.field_150129_M[opacity][0]} as their icon (an empty array threw, and the leaves were the
     * missing texture). Custom leaves fill their own in registerBlockIcons, as in 1.7.10.
     */
    protected legacyconnector.v1_7_10.net.minecraft.util.IIcon[][] field_150129_M = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[2][];

    /** Fills {@link #field_150129_M} of a vanilla leaves wrapper with the vanilla textures (vanilla never calls registerBlockIcons here). */
    protected final void mcc$vanillaIcons(String[][] names) {
        for (int i = 0; i < names.length && i < field_150129_M.length; i++) {
            field_150129_M[i] = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[names[i].length];
            for (int j = 0; j < names[i].length; j++) field_150129_M[i][j] = mcc$vanillaIcon(names[i][j]);
        }
    }

    protected BlockLeaves(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockLeaves() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.leaves);
    }

    /** Icon set in use: 0 fancy (cut out), 1 fast (opaque). */
    protected int field_150127_b;

    /** As 1.7.10: opaque only with fast graphics. */
    @Override
    public boolean isOpaqueCube() {
        return !field_150121_P;
    }

    public void setGraphicsLevel(boolean fancy) {
        this.field_150121_P = fancy;
        this.field_150127_b = fancy ? 0 : 1;
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

    /** The wood names of the leaf types, by metadata bits 0-1 (subclasses list theirs). */
    public String[] func_150125_e() {
        return new String[] {"oak", "spruce", "birch", "jungle"};
    }

    @Override
    public int damageDropped(int meta) {
        return meta & 3;
    }

    /** Forge: leaves can always be sheared. */
    @Override
    public boolean isShearable(legacyconnector.v1_7_10.net.minecraft.item.ItemStack item, legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y,
            int z) {
        return true;
    }

    /** Forge: shearing gives the leaf block itself (its type without the decay bits). */
    @Override
    public java.util.ArrayList<legacyconnector.v1_7_10.net.minecraft.item.ItemStack> onSheared(legacyconnector.v1_7_10.net.minecraft.item.ItemStack item,
            legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, int fortune) {
        java.util.ArrayList<legacyconnector.v1_7_10.net.minecraft.item.ItemStack> ret = new java.util.ArrayList<>();
        ret.add(new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(this, 1, world.getBlockMetadata(x, y, z) & 3));
        return ret;
    }
}
