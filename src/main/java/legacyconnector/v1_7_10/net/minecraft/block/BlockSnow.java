package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.EnumSkyBlock;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A snow layer, ported from 1.7.10 with Forge's changes: metadata 0-7 is the number of layers minus one (each 1/8 block), it
 * drops one snowball per layer, melts in block light above 11 and needs an opaque block (or leaves, or a full layer) below.
 */
public class BlockSnow extends Block {
    public BlockSnow(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockSnow() {
        super(Material.snow);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
        setTickRandomly(true);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabDecorations);
        func_150154_b(0);
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon("snow");
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        int layers = world.getBlockMetadata(x, y, z) & 7;
        return AxisAlignedBB.getBoundingBox(x + minX, y + minY, z + minZ, x + maxX, y + layers * 0.125F, z + maxZ);
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
        func_150154_b(0);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        func_150154_b(world.getBlockMetadata(x, y, z));
    }

    /** Sets the box to the height of the layers in a metadata value. */
    protected void func_150154_b(int meta) {
        int layers = meta & 7;
        float height = 2 * (1 + layers) / 16.0F;
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, height, 1.0F);
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        Block below = world.getBlock(x, y - 1, z);
        if (below == Blocks.ice || below == Blocks.packed_ice) return false;
        if (below.isLeaves(world, x, y - 1, z)) return true;
        if (below == this && (world.getBlockMetadata(x, y - 1, z) & 7) == 7) return true;
        return below.isOpaqueCube() && below.getMaterial().blocksMovement();
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        func_150155_m(world, x, y, z);
    }

    /** Removes the snow when it lost its support; false then. */
    private boolean func_150155_m(World world, int x, int y, int z) {
        if (canPlaceBlockAt(world, x, y, z)) return true;
        world.setBlockToAir(x, y, z);
        return false;
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta) {
        super.harvestBlock(world, player, x, y, z, meta);
        world.setBlockToAir(x, y, z);
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Items.snowball;
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    /** Forge: one snowball per layer. */
    @Override
    public int quantityDropped(int meta, int fortune, Random random) {
        return (meta & 7) + 1;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.getSavedLightValue(EnumSkyBlock.Block, x, y, z) > 11) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return side == 1 || super.shouldSideBeRendered(world, x, y, z, side);
    }

    /** Forge: a layer can be replaced (by another layer or a placed block) until it is full. */
    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        return meta < 7 && blockMaterial.isReplaceable();
    }
}
