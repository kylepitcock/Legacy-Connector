package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * Forge 1.12.2's finite fluid: each block holds a quantity (level + 1 quanta) that is conserved, falling first and then spreading evenly
 * over its horizontal neighbours; it never makes new sources.
 */
public class BlockFluidFinite extends BlockFluidBase {
    public BlockFluidFinite(Fluid fluid, Material material) {
        super(fluid, material);
    }

    public BlockFluidFinite(Fluid fluid, Material material, legacyconnector.v1_12_2.net.minecraft.block.material.MapColor mapColor) {
        super(fluid, material, mapColor);
    }

    @Override
    public int getQuantaValue(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock().isAir(state, world, pos)) return 0;
        if (state.getBlock() != this) return -1;
        return state.getValue(LEVEL) + 1;
    }

    @Override
    public boolean canCollideCheck(IBlockState state, boolean fullHit) {
        return fullHit;
    }

    @Override
    public int getMaxRenderHeightMeta() {
        return quantaPerBlock - 1;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        boolean changed = false;
        int quantaRemaining = state.getValue(LEVEL) + 1;
        int prevRemaining = quantaRemaining;
        quantaRemaining = tryToFlowVerticallyInto(world, pos, quantaRemaining);
        if (quantaRemaining < 1) return;
        if (quantaRemaining != prevRemaining) {
            changed = true;
            if (quantaRemaining == 1) {
                world.setBlockState(pos, state.withProperty(LEVEL, quantaRemaining - 1), 2);
                return;
            }
        } else if (quantaRemaining == 1) {
            return;
        }
        int lowerThan = quantaRemaining - 1;
        int total = quantaRemaining;
        int count = 1;
        EnumFacing[] sides = {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST};
        for (EnumFacing side : sides) {
            BlockPos off = pos.offset(side);
            if (displaceIfPossible(world, off)) world.setBlockToAir(off);
            int quanta = getQuantaValue(world, off);
            if (quanta >= 0 && quanta < lowerThan) {
                count++;
                total += quanta;
            }
        }
        if (count == 1) {
            if (changed) world.setBlockState(pos, state.withProperty(LEVEL, quantaRemaining - 1), 2);
            return;
        }
        int each = total / count;
        int rem = total % count;
        for (EnumFacing side : sides) {
            BlockPos off = pos.offset(side);
            int quanta = getQuantaValue(world, off);
            if (quanta >= 0 && quanta < lowerThan) {
                int newQuanta = each;
                if (rem == count || rem > 1 && rand.nextInt(count - rem) != 0) {
                    ++newQuanta;
                    --rem;
                }
                if (newQuanta != quanta) {
                    if (newQuanta == 0) world.setBlockToAir(off);
                    else world.setBlockState(off, getDefaultState().withProperty(LEVEL, newQuanta - 1), 2);
                    world.scheduleUpdate(off, this, tickRate);
                }
                --count;
            }
        }
        if (rem > 0) ++each;
        world.setBlockState(pos, state.withProperty(LEVEL, each - 1), 2);
    }

    /** Moves as much as fits into the block below (above for gases); the quanta left here. */
    public int tryToFlowVerticallyInto(World world, BlockPos pos, int amtToInput) {
        IBlockState myState = world.getBlockState(pos);
        BlockPos other = pos.add(0, densityDir, 0);
        if (other.getY() < 0 || other.getY() >= 256) {
            world.setBlockToAir(pos);
            return 0;
        }
        int amt = getQuantaValue(world, other);
        if (amt >= 0) {
            amt += amtToInput;
            if (amt > quantaPerBlock) {
                world.setBlockState(other, myState.withProperty(LEVEL, quantaPerBlock - 1), 3);
                world.scheduleUpdate(other, this, tickRate);
                return amt - quantaPerBlock;
            }
            if (amt > 0) {
                world.setBlockState(other, myState.withProperty(LEVEL, amt - 1), 3);
                world.scheduleUpdate(other, this, tickRate);
                world.setBlockToAir(pos);
                return 0;
            }
            return amtToInput;
        }
        int density_other = getDensity(world, other);
        if (density_other == Integer.MAX_VALUE) {
            if (displaceIfPossible(world, other)) {
                world.setBlockState(other, myState, 3);
                world.scheduleUpdate(other, this, tickRate);
                world.setBlockToAir(pos);
                return 0;
            }
            return amtToInput;
        }
        if (densityDir < 0 ? density_other < density : density_other > density) {
            IBlockState state = world.getBlockState(other);
            world.setBlockState(other, myState, 3);
            world.setBlockState(pos, state, 3);
            world.scheduleUpdate(other, this, tickRate);
            world.scheduleUpdate(pos, state.getBlock(), state.getBlock().tickRate(world));
            return 0;
        }
        return amtToInput;
    }

    @Override
    public FluidStack drain(World world, BlockPos pos, boolean doDrain) {
        Fluid fluid = getFluid();
        if (fluid == null) return null;
        int amount = (int) (getFilledPercentage(world, pos) * Fluid.BUCKET_VOLUME);
        if (amount <= 0) return null;
        if (doDrain) world.setBlockToAir(pos);
        return new FluidStack(fluid, amount);
    }

    @Override
    public boolean canDrain(World world, BlockPos pos) {
        return true;
    }
}
