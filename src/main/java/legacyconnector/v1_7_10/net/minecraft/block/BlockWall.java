package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A wall built from another block (1.7.10 {@code BlockWall}): it takes that block's material, hardness (a third of its resistance), sound and texture;
 * a post with arms towards the walls and solid blocks around it, 1.5 blocks high for walking into it.
 */
public class BlockWall extends Block {
    /** The block the wall is made of (its texture). */
    private Block mcc$base;

    public BlockWall(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockWall(Material material) {
        super(material);
    }

    public BlockWall(Block block) {
        super(block.getMaterial());
        this.mcc$base = block;
        setHardness(block.blockHardness);
        setResistance(block.blockResistance / 3.0F);
        setStepSound(block.stepSound);
        setCreativeTab(CreativeTabs.tabBlock);
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return mcc$base != null ? mcc$base.getIcon(side, meta) : super.getIcon(side, meta);
    }

    @Override
    public int getRenderType() {
        return 32;
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
    public boolean getBlocksMovement(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    /** Whether the wall has an arm towards the block there: another wall, a fence gate or a solid block that is not a pumpkin or melon. */
    public boolean canConnectWallTo(IBlockAccess world, int x, int y, int z) {
        Block other = world.getBlock(x, y, z);
        if (other == this || other == legacyconnector.v1_7_10.net.minecraft.init.Blocks.fence_gate) return true;
        if (other instanceof BlockWall) return true;
        return other.getMaterial().isOpaque() && other.renderAsNormalBlock() && other.getMaterial() != Material.gourd;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        boolean north = canConnectWallTo(world, x, y, z - 1);
        boolean south = canConnectWallTo(world, x, y, z + 1);
        boolean west = canConnectWallTo(world, x - 1, y, z);
        boolean east = canConnectWallTo(world, x + 1, y, z);
        float minX = 0.25F;
        float maxX = 0.75F;
        float minZ = 0.25F;
        float maxZ = 0.75F;
        float maxY = 1.0F;
        if (north) minZ = 0.0F;
        if (south) maxZ = 1.0F;
        if (west) minX = 0.0F;
        if (east) maxX = 1.0F;
        if (north && south && !west && !east) {
            maxY = 0.8125F;
            minX = 0.3125F;
            maxX = 0.6875F;
        } else if (!north && !south && west && east) {
            maxY = 0.8125F;
            minZ = 0.3125F;
            maxZ = 0.6875F;
        }
        setBlockBounds(minX, 0.0F, minZ, maxX, maxY, maxZ);
    }

    /** The collision box is 1.5 blocks high so nothing walks over the wall. */
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        AxisAlignedBB box = super.getCollisionBoundingBoxFromPool(world, x, y, z);
        return box == null ? null : AxisAlignedBB.getBoundingBox(box.minX, box.minY, box.minZ, box.maxX, y + 1.5D, box.maxZ);
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
        list.add(new ItemStack(item, 1, 1));
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return side == 0 || super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
    }
}
