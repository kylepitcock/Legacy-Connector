package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockSlab}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockSlab extends legacyconnector.v1_7_10.net.minecraft.block.Block {
    /** Whether this is the double slab (mods create the other half when it is false). */
    protected boolean isFullBlock;

    public BlockSlab(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockSlab(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockSlab(boolean p0, legacyconnector.v1_7_10.net.minecraft.block.material.Material p1) {
        this(p1);
        this.isFullBlock = p0;
        this.mcc$slab = true;
        // Block's constructor read isOpaqueCube() before the flag was set (1.7.10's BlockSlab refreshes these the same way)
        this.opaque = p0;
        this.lightOpacity = p0 ? 255 : 0;
    }

    /** 1.7.10 {@code func_150002_b}: the name of the slab variant for a metadata value (plain slabs: the block's own name). */
    public String func_150002_b(int meta) {
        return getUnlocalizedName();
    }

    /** 1.6.4 {@code getFullSlabName}; a mod's override reaches {@link #func_150002_b} through the bridge-methods table. */
    public String getFullSlabName(int meta) {
        return func_150002_b(meta);
    }

    /** Set by the legacy constructor: only slabs of mods know whether they are the double one (vanilla wrappers keep Block's answer). */
    private boolean mcc$slab;

    /** Slabs of mods (made with the legacy constructor) carry the 1.7.10 behaviour below; vanilla slab wrappers keep what the modern block does. */
    @Override
    public boolean renderAsNormalBlock() {
        return mcc$slab ? isFullBlock : super.renderAsNormalBlock();
    }

    /** The box: the whole block for the double slab, else the lower or (metadata bit 8) the upper half. */
    @Override
    public void setBlockBoundsBasedOnState(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        if (!mcc$slab) {
            super.setBlockBoundsBasedOnState(world, x, y, z);
        } else if (isFullBlock) {
            setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        } else if ((world.getBlockMetadata(x, y, z) & 8) != 0) {
            setBlockBounds(0.0F, 0.5F, 0.0F, 1.0F, 1.0F, 1.0F);
        } else {
            setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
        }
    }

    @Override
    public void setBlockBoundsForItemRender() {
        if (!mcc$slab) super.setBlockBoundsForItemRender();
        else if (isFullBlock) setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        else setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
    }

    @Override
    public void addCollisionBoxesToList(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB mask, java.util.List list, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        if (mcc$slab) setBlockBoundsBasedOnState(world, x, y, z);
        super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
    }

    /** A half slab put against the lower half of a side, or on top of a block, is the lower one; otherwise the upper (bit 8). */
    @Override
    public int onBlockPlaced(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
            int meta) {
        if (!mcc$slab) return super.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, meta);
        if (isFullBlock) return meta;
        return side != 0 && (side == 1 || hitY <= 0.5D) ? meta : meta | 8;
    }

    /** The double slab drops two half slabs. */
    @Override
    public int quantityDropped(java.util.Random random) {
        if (!mcc$slab) return super.quantityDropped(random);
        return isFullBlock ? 2 : 1;
    }

    @Override
    public int damageDropped(int meta) {
        return mcc$slab ? meta & 7 : super.damageDropped(meta);
    }

    @Override
    public int getDamageValue(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        return mcc$slab ? super.getDamageValue(world, x, y, z) & 7 : super.getDamageValue(world, x, y, z);
    }

    /** As in 1.7.10 only the double slab is an opaque cube (mods such as Thaumic Tinkerer register the half slab's recipe on that). */
    @Override
    public boolean isOpaqueCube() {
        return mcc$slab ? isFullBlock : super.isOpaqueCube();
    }
}
