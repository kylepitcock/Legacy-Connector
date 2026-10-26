package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A carpet: 1/16 high, coloured like wool by its metadata, needs a block below it. */
public class BlockCarpet extends Block {
    public BlockCarpet(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockCarpet() {
        super(Material.carpet);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F);
        setTickRandomly(true);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabDecorations);
        func_150089_b(0);
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return legacyconnector.v1_7_10.net.minecraft.init.Blocks.wool.getIcon(side, meta);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(x + minX, y + minY, z + minZ, x + maxX, y, z + maxZ);
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
    public void setBlockBoundsForItemRender() {
        func_150089_b(0);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        func_150089_b(world.getBlockMetadata(x, y, z));
    }

    protected void func_150089_b(int meta) {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F / 16.0F, 1.0F);
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return super.canPlaceBlockAt(world, x, y, z) && canBlockStay(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        func_150090_e(world, x, y, z);
    }

    /** Drops the carpet when it lost its support; false then. */
    private boolean func_150090_e(World world, int x, int y, int z) {
        if (canBlockStay(world, x, y, z)) return true;
        dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
        world.setBlockToAir(x, y, z);
        return false;
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return !world.isAirBlock(x, y - 1, z);
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return side == 1 || super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    @Override
    public void getSubBlocks(legacyconnector.v1_7_10.net.minecraft.item.Item item, legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs tab, List list) {
        for (int i = 0; i < 16; ++i) list.add(new ItemStack(item, 1, i));
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.block.material.MapColor getMapColor(int meta) {
        return legacyconnector.v1_7_10.net.minecraft.block.material.MapColor.getMapColorForBlockColored(meta);
    }
}
