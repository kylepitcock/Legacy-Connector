package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The detector rail: powered (bit 3) while a cart is on it; comparators read the cart's inventory. */
public class BlockRailDetector extends BlockRailBase {
    public BlockRailDetector(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockRailDetector() {
        super(true);
        setTickRandomly(true);
    }

    @Override
    public int tickRate(World world) {
        return 20;
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        if (mcc$vanilla != null || world.isRemote) return;
        int meta = world.getBlockMetadata(x, y, z);
        if ((meta & 8) == 0) func_150054_a(world, x, y, z, meta);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (mcc$vanilla != null) {
            super.updateTick(world, x, y, z, random);
            return;
        }
        if (world.isRemote) return;
        int meta = world.getBlockMetadata(x, y, z);
        if ((meta & 8) != 0) func_150054_a(world, x, y, z, meta);
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        return (world.getBlockMetadata(x, y, z) & 8) != 0 ? 15 : 0;
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        return (world.getBlockMetadata(x, y, z) & 8) == 0 ? 0 : side == 1 ? 15 : 0;
    }

    private static AxisAlignedBB mcc$detectionBox(int x, int y, int z) {
        float f = 0.125F;
        return AxisAlignedBB.getBoundingBox(x + f, y, z + f, x + 1 - f, y + 1 - f, z + 1 - f);
    }

    /** Updates the powered bit from whether a cart is on the rail, and keeps checking while one is. */
    protected void func_150054_a(World world, int x, int y, int z, int meta) {
        boolean was = (meta & 8) != 0;
        boolean cart = !world.getEntitiesWithinAABB(EntityMinecart.class, mcc$detectionBox(x, y, z)).isEmpty();
        if (cart != was) {
            world.setBlockMetadataWithNotify(x, y, z, cart ? meta | 8 : meta & 7, 3);
            world.notifyBlocksOfNeighborChange(x, y, z, this);
            world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
            world.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
        }
        if (cart) world.scheduleBlockUpdate(x, y, z, this, tickRate(world));
        world.func_147453_f(x, y, z, this);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        if (mcc$vanilla == null && !world.isRemote) func_150054_a(world, x, y, z, world.getBlockMetadata(x, y, z));
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        if ((world.getBlockMetadata(x, y, z) & 8) == 0) return 0;
        List<?> carts = world.getEntitiesWithinAABB(EntityMinecart.class, mcc$detectionBox(x, y, z));
        for (Object o : carts) {
            if (o instanceof legacyconnector.v1_7_10.net.minecraft.inventory.IInventory inv) {
                return legacyconnector.v1_7_10.net.minecraft.inventory.Container.calcRedstoneFromInventory(inv);
            }
        }
        return 0;
    }
}
