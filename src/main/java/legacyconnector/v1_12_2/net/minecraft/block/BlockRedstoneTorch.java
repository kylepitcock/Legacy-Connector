package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 redstone torch with its private bookkeeping (mods subclass it for coloured torches and call {@code isBurnedOut} and
 * {@code shouldBeOff} by reflection): a torch powers everything but the block it is attached to, switches off when that block is powered
 * and burns out after eight toggles within sixty ticks.
 */
public class BlockRedstoneTorch extends BlockTorch {
    private static final Map<World, List<Toggle>> toggles = new WeakHashMap<>();
    private final boolean isOn;

    private boolean isBurnedOut(World worldIn, BlockPos pos, boolean turnOff) {
        List<Toggle> list;
        synchronized (toggles) {
            list = toggles.computeIfAbsent(worldIn, w -> new ArrayList<>());
            if (turnOff) list.add(new Toggle(pos, worldIn.getTotalWorldTime()));
            int i = 0;
            for (Toggle toggle : list) {
                if (toggle.pos.equals(pos) && ++i >= 8) return true;
            }
        }
        return false;
    }

    protected BlockRedstoneTorch(boolean isOn) {
        this.isOn = isOn;
        setTickRandomly(true);
        setCreativeTab(null);
    }

    @Override
    public int tickRate(World worldIn) {
        return 2;
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (isOn) {
            for (EnumFacing enumfacing : EnumFacing.values()) worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing), this, false);
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        if (isOn) {
            for (EnumFacing enumfacing : EnumFacing.values()) worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing), this, false);
        }
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return isOn && blockState.getValue(FACING) != side ? 15 : 0;
    }

    private boolean shouldBeOff(World worldIn, BlockPos pos, IBlockState state) {
        EnumFacing enumfacing = state.getValue(FACING).getOpposite();
        return worldIn.isSidePowered(pos.offset(enumfacing), enumfacing);
    }

    @Override
    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random) {}

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        boolean flag = shouldBeOff(worldIn, pos, state);
        synchronized (toggles) {
            List<Toggle> list = toggles.get(worldIn);
            while (list != null && !list.isEmpty() && worldIn.getTotalWorldTime() - list.get(0).time > 60L) list.remove(0);
        }
        if (isOn) {
            if (flag) {
                worldIn.setBlockState(pos, withFacing(legacyconnector.v1_12_2.net.minecraft.init.Blocks.UNLIT_REDSTONE_TORCH, state), 3);
                if (isBurnedOut(worldIn, pos, true)) {
                    worldIn.playSound(null, pos, legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.BLOCK_FIRE_EXTINGUISH,
                            legacyconnector.v1_12_2.net.minecraft.util.SoundCategory.BLOCKS, 0.5F, 2.6F + (worldIn.rand.nextFloat() - worldIn.rand.nextFloat()) * 0.8F);
                    for (int i = 0; i < 5; ++i) {
                        double d0 = pos.getX() + rand.nextDouble() * 0.6D + 0.2D;
                        double d1 = pos.getY() + rand.nextDouble() * 0.6D + 0.2D;
                        double d2 = pos.getZ() + rand.nextDouble() * 0.6D + 0.2D;
                        worldIn.spawnParticle(legacyconnector.v1_12_2.net.minecraft.util.EnumParticleTypes.SMOKE_NORMAL, d0, d1, d2, 0.0D, 0.0D, 0.0D);
                    }
                    worldIn.scheduleUpdate(pos, worldIn.getBlockState(pos).getBlock(), 160);
                }
            }
        } else if (!flag && !isBurnedOut(worldIn, pos, false)) {
            worldIn.setBlockState(pos, withFacing(legacyconnector.v1_12_2.net.minecraft.init.Blocks.REDSTONE_TORCH, state), 3);
        }
    }

    /** The default state of the vanilla torch block, facing like {@code from} (vanilla torches the shim does not model keep their own facing). */
    private static IBlockState withFacing(Block target, IBlockState from) {
        IBlockState out = target.getDefaultState();
        try {
            if (out.getPropertyKeys().contains(FACING)) out = out.withProperty(FACING, from.getValue(FACING));
        } catch (RuntimeException e) {
            // the target keeps its default facing
        }
        return out;
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!onNeighborChangeInternal(worldIn, pos, state)) {
            if (isOn == shouldBeOff(worldIn, pos, state)) worldIn.scheduleUpdate(pos, this, tickRate(worldIn));
        }
    }

    @Override
    public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return side == EnumFacing.DOWN ? blockState.getWeakPower(blockAccess, pos, side) : 0;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(legacyconnector.v1_12_2.net.minecraft.init.Blocks.REDSTONE_TORCH);
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Blocks.REDSTONE_TORCH);
    }

    @Override
    public boolean isAssociatedBlock(Block other) {
        return other == legacyconnector.v1_12_2.net.minecraft.init.Blocks.UNLIT_REDSTONE_TORCH || other == legacyconnector.v1_12_2.net.minecraft.init.Blocks.REDSTONE_TORCH;
    }

    /** A toggle of a torch at a world time (1.12 kept the last minute of them to burn a torch out). */
    public static class Toggle {
        BlockPos pos;
        long time;

        public Toggle(BlockPos pos, long time) {
            this.pos = pos;
            this.time = time;
        }
    }
}
