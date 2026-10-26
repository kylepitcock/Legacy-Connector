package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityArrow;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/**
 * A button, ported from 1.7.10: metadata 1-4 is the wall it hangs on (east, west, south, north face of the neighbour), bit 8
 * pressed. Pressing it powers the block behind it for 20 ticks (30 for wooden buttons, which arrows also press).
 */
public abstract class BlockButton extends Block {
    /** Wooden: longer press, pressed by arrows. */
    private final boolean field_150047_a;

    public BlockButton(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.field_150047_a = info.legacyName().endsWith("wooden_button");
    }

    protected BlockButton(boolean wooden) {
        super(Material.circuits);
        setTickRandomly(true);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabRedstone);
        this.field_150047_a = wooden;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public int tickRate(World world) {
        return field_150047_a ? 30 : 20;
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
    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        return dir == ForgeDirection.NORTH && world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH)
                || dir == ForgeDirection.SOUTH && world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH)
                || dir == ForgeDirection.WEST && world.isSideSolid(x + 1, y, z, ForgeDirection.WEST)
                || dir == ForgeDirection.EAST && world.isSideSolid(x - 1, y, z, ForgeDirection.EAST);
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return world.isSideSolid(x - 1, y, z, ForgeDirection.EAST) || world.isSideSolid(x + 1, y, z, ForgeDirection.WEST)
                || world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH) || world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH);
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
        int current = world.getBlockMetadata(x, y, z);
        int pressed = current & 8;
        int facing;
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        if (dir == ForgeDirection.NORTH && world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH)) facing = 4;
        else if (dir == ForgeDirection.SOUTH && world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH)) facing = 3;
        else if (dir == ForgeDirection.WEST && world.isSideSolid(x + 1, y, z, ForgeDirection.WEST)) facing = 2;
        else if (dir == ForgeDirection.EAST && world.isSideSolid(x - 1, y, z, ForgeDirection.EAST)) facing = 1;
        else facing = func_150045_e(world, x, y, z);
        return facing + pressed;
    }

    /** The first wall around the position the button can hang on. */
    private int func_150045_e(World world, int x, int y, int z) {
        if (world.isSideSolid(x - 1, y, z, ForgeDirection.EAST)) return 1;
        if (world.isSideSolid(x + 1, y, z, ForgeDirection.WEST)) return 2;
        if (world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH)) return 3;
        if (world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH)) return 4;
        return 1;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!func_150044_m(world, x, y, z)) return;
        int facing = world.getBlockMetadata(x, y, z) & 7;
        boolean lost = !world.isSideSolid(x - 1, y, z, ForgeDirection.EAST) && facing == 1
                || !world.isSideSolid(x + 1, y, z, ForgeDirection.WEST) && facing == 2
                || !world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH) && facing == 3
                || !world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH) && facing == 4;
        if (lost) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
    }

    /** Drops the button when no wall is left around it; false then. */
    private boolean func_150044_m(World world, int x, int y, int z) {
        if (canPlaceBlockAt(world, x, y, z)) return true;
        dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
        world.setBlockToAir(x, y, z);
        return false;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        func_150043_b(world.getBlockMetadata(x, y, z));
    }

    /** The button's box for a metadata (pressed buttons stick out half as far). */
    private void func_150043_b(int meta) {
        int facing = meta & 7;
        boolean pressed = (meta & 8) > 0;
        float bottom = 0.375F;
        float top = 0.625F;
        float halfWidth = 0.1875F;
        float depth = pressed ? 0.0625F : 0.125F;
        if (facing == 1) setBlockBounds(0.0F, bottom, 0.5F - halfWidth, depth, top, 0.5F + halfWidth);
        else if (facing == 2) setBlockBounds(1.0F - depth, bottom, 0.5F - halfWidth, 1.0F, top, 0.5F + halfWidth);
        else if (facing == 3) setBlockBounds(0.5F - halfWidth, bottom, 0.0F, 0.5F + halfWidth, top, depth);
        else if (facing == 4) setBlockBounds(0.5F - halfWidth, bottom, 1.0F - depth, 0.5F + halfWidth, top, 1.0F);
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        int meta = world.getBlockMetadata(x, y, z);
        int facing = meta & 7;
        int press = 8 - (meta & 8);
        if (press == 0) return true;
        world.setBlockMetadataWithNotify(x, y, z, facing + press, 3);
        world.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
        world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.click", 0.3F, 0.6F);
        func_150042_a(world, x, y, z, facing);
        world.scheduleBlockUpdate(x, y, z, this, tickRate(world));
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if ((meta & 8) > 0) func_150042_a(world, x, y, z, meta & 7);
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        return (world.getBlockMetadata(x, y, z) & 8) > 0 ? 15 : 0;
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        int meta = world.getBlockMetadata(x, y, z);
        if ((meta & 8) == 0) return 0;
        int facing = meta & 7;
        return facing == 5 && side == 1 || facing == 4 && side == 2 || facing == 3 && side == 3 || facing == 2 && side == 4
                || facing == 1 && side == 5 ? 15 : 0;
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote) return;
        int meta = world.getBlockMetadata(x, y, z);
        if ((meta & 8) == 0) return;
        if (field_150047_a) {
            func_150046_n(world, x, y, z);
        } else {
            world.setBlockMetadataWithNotify(x, y, z, meta & 7, 3);
            func_150042_a(world, x, y, z, meta & 7);
            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.click", 0.3F, 0.5F);
            world.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
        }
    }

    @Override
    public void setBlockBoundsForItemRender() {
        float w = 0.1875F;
        float h = 0.125F;
        float d = 0.125F;
        setBlockBounds(0.5F - w, 0.5F - h, 0.5F - d, 0.5F + w, 0.5F + h, 0.5F + d);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (!world.isRemote && field_150047_a && (world.getBlockMetadata(x, y, z) & 8) == 0) func_150046_n(world, x, y, z);
    }

    /** Wooden buttons: pressed while an arrow sticks in them. */
    private void func_150046_n(World world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        int facing = meta & 7;
        boolean pressed = (meta & 8) != 0;
        func_150043_b(meta);
        List arrows = world.getEntitiesWithinAABB(EntityArrow.class,
                AxisAlignedBB.getBoundingBox(x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ));
        boolean hit = arrows != null && !arrows.isEmpty();
        if (hit && !pressed) {
            world.setBlockMetadataWithNotify(x, y, z, facing | 8, 3);
            func_150042_a(world, x, y, z, facing);
            world.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.click", 0.3F, 0.6F);
        }
        if (!hit && pressed) {
            world.setBlockMetadataWithNotify(x, y, z, facing, 3);
            func_150042_a(world, x, y, z, facing);
            world.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.click", 0.3F, 0.5F);
        }
        if (hit) world.scheduleBlockUpdate(x, y, z, this, tickRate(world));
    }

    /** Updates the neighbours of the button and of the block it hangs on. */
    private void func_150042_a(World world, int x, int y, int z, int facing) {
        world.notifyBlocksOfNeighborChange(x, y, z, this);
        if (facing == 1) world.notifyBlocksOfNeighborChange(x - 1, y, z, this);
        else if (facing == 2) world.notifyBlocksOfNeighborChange(x + 1, y, z, this);
        else if (facing == 3) world.notifyBlocksOfNeighborChange(x, y, z - 1, this);
        else if (facing == 4) world.notifyBlocksOfNeighborChange(x, y, z + 1, this);
        else world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
    }

    /** Buttons take the icon of the block they are made of (stone, planks) in their subclasses. */
    @Override
    public void registerBlockIcons(IIconRegister register) {
    }
}
