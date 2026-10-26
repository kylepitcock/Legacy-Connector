package legacyconnector.v1_7_10.net.minecraft.block;

/** The bed: metadata bits 0-1 are the direction, 8 the head half, 4 occupied. RenderBlocks draws it (render type 14). */
public class BlockBed extends BlockDirectional {
    public static final int[][] bedDirections = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}};
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon[] iconEnd, iconSide, iconTop;

    public BlockBed(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockBed() {
        super(legacyconnector.v1_7_10.net.minecraft.block.material.Material.cloth);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5625F, 1.0F);
    }

    public static boolean isBlockHeadOfBed(int meta) {
        return (meta & 8) != 0;
    }

    public static boolean isBedOccupied(int meta) {
        return (meta & 4) != 0;
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        String t = getTextureName();
        iconTop = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[] {register.registerIcon(t + "_feet_top"), register.registerIcon(t + "_head_top")};
        iconEnd = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[] {register.registerIcon(t + "_feet_end"), register.registerIcon(t + "_head_end")};
        iconSide = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[] {register.registerIcon(t + "_feet_side"), register.registerIcon(t + "_head_side")};
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getIcon(int side, int meta) {
        if (side == 0) return legacyconnector.v1_7_10.net.minecraft.init.Blocks.planks.getBlockTextureFromSide(side);
        int face = legacyconnector.v1_7_10.net.minecraft.util.Direction.bedDirection[getDirection(meta)][side];
        int head = isBlockHeadOfBed(meta) ? 1 : 0;
        String part = head == 1 ? "head" : "feet";
        if (head == 1 && face == 2 || head == 0 && face == 3) return iconEnd != null ? iconEnd[head] : mcc$vanillaIcon("bed_" + part + "_end");
        if (face == 5 || face == 4) return iconSide != null ? iconSide[head] : mcc$vanillaIcon("bed_" + part + "_side");
        return iconTop != null ? iconTop[head] : mcc$vanillaIcon("bed_" + part + "_top");
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 14;
    }

    @Override
    public void setBlockBoundsBasedOnState(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5625F, 1.0F);
    }

    @Override
    public boolean isBed(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase player) {
        return true;
    }

    /** Forge: the bed's direction from its metadata. */
    @Override
    public int getBedDirection(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return getDirection(world.getBlockMetadata(x, y, z));
    }

    /** Forge (named the other way round): true for the head half. */
    @Override
    public boolean isBedFoot(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return isBlockHeadOfBed(world.getBlockMetadata(x, y, z));
    }
}
