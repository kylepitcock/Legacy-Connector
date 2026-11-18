package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A trapdoor (1.7.10 {@code BlockTrapDoor}): metadata bits 0-1 are the wall it hangs on (south, north, east, west neighbour), bit 4 is
 * open, bit 8 is the top half. It is 3/16 thick, opens on right-click (not iron) or when powered, and breaks when its wall goes.
 */
public class BlockTrapDoor extends Block {
    public BlockTrapDoor(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockTrapDoor(Material material) {
        super(material);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        setCreativeTab(CreativeTabs.tabRedstone);
    }

    /** Forge's switch for mods whose trapdoors may hang without a support block; the modern trapdoor never needs one, so it only records the request. */
    public static boolean disableValidation = false;

    public static boolean func_150118_d(int meta) {
        return (meta & 4) != 0;
    }

    public static boolean isTrapdoorOpen(int meta) {
        return (meta & 4) != 0;
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
        return !func_150118_d(world.getBlockMetadata(x, y, z));
    }

    @Override
    public int getRenderType() {
        return 0;
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getSelectedBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        func_150117_b(world.getBlockMetadata(x, y, z));
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.1875F, 1.0F);
    }

    /** The box for a metadata value: a 3/16 slab at the bottom or top, or standing against its wall when open. */
    public void func_150117_b(int meta) {
        float thickness = 0.1875F;
        if ((meta & 8) != 0) {
            setBlockBounds(0.0F, 1.0F - thickness, 0.0F, 1.0F, 1.0F, 1.0F);
        } else {
            setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, thickness, 1.0F);
        }
        if (func_150118_d(meta)) {
            if ((meta & 3) == 0) setBlockBounds(0.0F, 0.0F, 1.0F - thickness, 1.0F, 1.0F, 1.0F);
            if ((meta & 3) == 1) setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, thickness);
            if ((meta & 3) == 2) setBlockBounds(1.0F - thickness, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
            if ((meta & 3) == 3) setBlockBounds(0.0F, 0.0F, 0.0F, thickness, 1.0F, 1.0F);
        }
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (this.blockMaterial == Material.iron) return true;
        int meta = world.getBlockMetadata(x, y, z);
        world.setBlockMetadataWithNotify(x, y, z, meta ^ 4, 2);
        world.playAuxSFXAtEntity(player, 1003, x, y, z, 0);
        return true;
    }

    /** Opens or closes the trapdoor to match {@code open} (with the door sound); nothing happens when it already is. */
    public void func_150120_a(World world, int x, int y, int z, boolean open) {
        int meta = world.getBlockMetadata(x, y, z);
        boolean isOpen = (meta & 4) > 0;
        if (isOpen == open) return;
        world.setBlockMetadataWithNotify(x, y, z, meta ^ 4, 2);
        world.playAuxSFXAtEntity(null, 1003, x, y, z, 0);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.isRemote) return;
        int meta = world.getBlockMetadata(x, y, z);
        int wallX = x;
        int wallZ = z;
        if ((meta & 3) == 0) wallZ++;
        if ((meta & 3) == 1) wallZ--;
        if ((meta & 3) == 2) wallX++;
        if ((meta & 3) == 3) wallX--;
        if (!func_150119_a(world.getBlock(wallX, y, wallZ))) {
            world.setBlockToAir(x, y, z);
            dropBlockAsItem(world, x, y, z, meta, 0);
        }
        boolean powered = world.isBlockIndirectlyGettingPowered(x, y, z);
        if (powered || neighbor.canProvidePower()) func_150120_a(world, x, y, z, powered);
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 start, Vec3 end) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.collisionRayTrace(world, x, y, z, start, end);
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
        int result = 0;
        if (side == 2) result = 0;
        if (side == 3) result = 1;
        if (side == 4) result = 2;
        if (side == 5) result = 3;
        if (side != 1 && side != 0 && hitY > 0.5F) result |= 8;
        return result;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
        if (disableValidation) return true;
        if (side == 0) return false;
        if (side == 1) return false;
        if (side == 2) z++;
        if (side == 3) z--;
        if (side == 4) x++;
        if (side == 5) x--;
        return func_150119_a(world.getBlock(x, y, z));
    }

    /** Whether a block can carry a trapdoor: a solid full block, glowstone, a slab or stairs. */
    private static boolean func_150119_a(Block block) {
        return block.getMaterial().isOpaque() && block.renderAsNormalBlock()
                || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.glowstone || block instanceof BlockSlab
                || block instanceof BlockStairs;
    }
}
