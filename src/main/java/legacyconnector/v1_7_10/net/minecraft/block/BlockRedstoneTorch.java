package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A redstone torch (1.7.10 {@code BlockRedstoneTorch}), lit or unlit by the constructor flag: it powers everything around it except the block it is
 * stuck on, turns off while that block is powered, and burns out when it was toggled eight times within 60 ticks.
 */
public class BlockRedstoneTorch extends BlockTorch {
    /** Whether this is the lit torch. */
    private final boolean field_150113_a;
    /** The recent toggles by world, to detect a burn-out. */
    private static final Map<World, List<Toggle>> field_150112_b = new WeakHashMap<>();

    private static final class Toggle {
        final int x;
        final int y;
        final int z;
        final long time;

        Toggle(int x, int y, int z, long time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
        }
    }

    public BlockRedstoneTorch(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.field_150113_a = info.legacyName().equals("redstone_torch");
    }

    protected BlockRedstoneTorch(boolean lit) {
        this.field_150113_a = lit;
        setTickRandomly(true);
        setCreativeTab(null);
    }

    /** Records a toggle (when {@code add}) and tells whether this torch has been toggled eight times within the last 60 ticks. */
    private boolean func_150111_a(World world, int x, int y, int z, boolean add) {
        List<Toggle> list;
        synchronized (field_150112_b) {
            list = field_150112_b.computeIfAbsent(world, w -> new ArrayList<>());
        }
        if (add) list.add(new Toggle(x, y, z, world.getTotalWorldTime()));
        int count = 0;
        for (Toggle t : list) {
            if (t.x == x && t.y == y && t.z == z && ++count >= 8) return true;
        }
        return false;
    }

    @Override
    public int tickRate(World world) {
        return 2;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (mcc$vanilla != null) {
            super.onBlockAdded(world, x, y, z);
            return;
        }
        if (world.getBlockMetadata(x, y, z) == 0) super.onBlockAdded(world, x, y, z);
        if (field_150113_a) notifyAround(world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (field_150113_a) notifyAround(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
    }

    private void notifyAround(World world, int x, int y, int z) {
        world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
        world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
        world.notifyBlocksOfNeighborChange(x - 1, y, z, this);
        world.notifyBlocksOfNeighborChange(x + 1, y, z, this);
        world.notifyBlocksOfNeighborChange(x, y, z - 1, this);
        world.notifyBlocksOfNeighborChange(x, y, z + 1, this);
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        if (!field_150113_a) return 0;
        int meta = world.getBlockMetadata(x, y, z);
        if (meta == 5 && side == 1) return 0;
        if (meta == 3 && side == 3) return 0;
        if (meta == 4 && side == 2) return 0;
        if (meta == 1 && side == 5) return 0;
        if (meta == 2 && side == 4) return 0;
        return 15;
    }

    /** Whether the block the torch is stuck on is powered (which turns the torch off). */
    private boolean func_150110_m(World world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        return meta == 5 && world.getIndirectPowerOutput(x, y - 1, z, 0) || meta == 3 && world.getIndirectPowerOutput(x, y, z - 1, 2)
                || meta == 4 && world.getIndirectPowerOutput(x, y, z + 1, 3) || meta == 1 && world.getIndirectPowerOutput(x - 1, y, z, 4)
                || meta == 2 && world.getIndirectPowerOutput(x + 1, y, z, 5);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (mcc$vanilla != null) {
            super.updateTick(world, x, y, z, random);
            return;
        }
        boolean powered = func_150110_m(world, x, y, z);
        List<Toggle> list;
        synchronized (field_150112_b) {
            list = field_150112_b.get(world);
        }
        while (list != null && !list.isEmpty() && world.getTotalWorldTime() - list.get(0).time > 60L) list.remove(0);
        if (field_150113_a) {
            if (powered) {
                world.setBlock(x, y, z, Blocks.unlit_redstone_torch, world.getBlockMetadata(x, y, z), 3);
                if (func_150111_a(world, x, y, z, true)) {
                    world.playSoundEffect(x + 0.5F, y + 0.5F, z + 0.5F, "random.fizz", 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
                    for (int i = 0; i < 5; i++) {
                        world.spawnParticle("smoke", x + random.nextDouble() * 0.6D + 0.2D, y + random.nextDouble() * 0.6D + 0.2D,
                                z + random.nextDouble() * 0.6D + 0.2D, 0.0D, 0.0D, 0.0D);
                    }
                }
            }
        } else if (!powered && !func_150111_a(world, x, y, z, false)) {
            world.setBlock(x, y, z, Blocks.redstone_torch, world.getBlockMetadata(x, y, z), 3);
        }
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (mcc$vanilla != null) {
            super.onNeighborBlockChange(world, x, y, z, neighbor);
            return;
        }
        if (!func_150108_b(world, x, y, z, neighbor)) {
            boolean powered = func_150110_m(world, x, y, z);
            if (field_150113_a && powered || !field_150113_a && !powered) world.scheduleBlockUpdate(x, y, z, this, tickRate(world));
        }
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        return side == 0 ? isProvidingWeakPower(world, x, y, z, side) : 0;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Item.getItemFromBlock(Blocks.redstone_torch);
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    /** The lit torch is the one the pick-block and the creative tab give for both. */
    @Override
    public Item getItem(World world, int x, int y, int z) {
        return Item.getItemFromBlock(Blocks.redstone_torch);
    }

    @Override
    public boolean isAssociatedBlock(Block other) {
        return other == Blocks.unlit_redstone_torch || other == Blocks.redstone_torch;
    }
}
