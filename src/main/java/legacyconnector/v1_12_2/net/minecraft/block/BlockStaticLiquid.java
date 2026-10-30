package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's still water and lava: turns into its flowing block when a neighbour changes (unless it mixed), lava sets fire around. */
public class BlockStaticLiquid extends BlockLiquid {
    protected BlockStaticLiquid(Material materialIn) {
        super(materialIn);
        setTickRandomly(false);
        if (materialIn == Material.LAVA) setTickRandomly(true);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!checkForMixing(worldIn, pos, state)) updateLiquid(worldIn, pos, state);
    }

    private void updateLiquid(World worldIn, BlockPos pos, IBlockState state) {
        BlockDynamicLiquid flowing = getFlowingBlock(material);
        worldIn.setBlockState(pos, flowing.getDefaultState().withProperty(LEVEL, state.getValue(LEVEL)), 2);
        worldIn.scheduleUpdate(pos, flowing, tickRate(worldIn));
    }

    /** Lava's random tick: sets fire to flammable blocks above and around it. */
    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (material != Material.LAVA || !worldIn.getGameRules().getBoolean("doFireTick")) return;
        int i = rand.nextInt(3);
        if (i > 0) {
            BlockPos p = pos;
            for (int j = 0; j < i; ++j) {
                p = p.add(rand.nextInt(3) - 1, 1, rand.nextInt(3) - 1);
                if (p.getY() >= 0 && p.getY() < 256 && !worldIn.isBlockLoaded(p)) return;
                IBlockState at = worldIn.getBlockState(p);
                if (at.getMaterial() == Material.AIR) {
                    if (isSurroundingBlockFlammable(worldIn, p)) {
                        worldIn.setBlockState(p, legacyconnector.v1_12_2.net.minecraft.init.Blocks.FIRE.getDefaultState());
                        return;
                    }
                } else if (at.getMaterial().blocksMovement()) {
                    return;
                }
            }
        } else {
            for (int k = 0; k < 3; ++k) {
                BlockPos p = pos.add(rand.nextInt(3) - 1, 0, rand.nextInt(3) - 1);
                if (p.getY() >= 0 && p.getY() < 256 && !worldIn.isBlockLoaded(p)) return;
                if (worldIn.isAirBlock(p.up()) && getCanBlockBurn(worldIn, p)) {
                    worldIn.setBlockState(p.up(), legacyconnector.v1_12_2.net.minecraft.init.Blocks.FIRE.getDefaultState());
                }
            }
        }
    }

    protected boolean isSurroundingBlockFlammable(World worldIn, BlockPos pos) {
        for (EnumFacing f : EnumFacing.values()) if (getCanBlockBurn(worldIn, pos.offset(f))) return true;
        return false;
    }

    private boolean getCanBlockBurn(World worldIn, BlockPos pos) {
        return pos.getY() >= 0 && pos.getY() < 256 && !worldIn.isBlockLoaded(pos) ? false : worldIn.getBlockState(pos).getMaterial().getCanBurn();
    }
}
