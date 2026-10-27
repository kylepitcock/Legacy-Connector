package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's nether lava spring (a flowing source in netherrack). */
public class WorldGenHellLava extends WorldGenerator {
    private final Block block;
    private final boolean insideRock;

    public WorldGenHellLava(Block blockIn, boolean insideRockIn) {
        this.block = blockIn;
        this.insideRock = insideRockIn;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        if (!mcc$modernAt(worldIn, position.up()).is(net.minecraft.world.level.block.Blocks.NETHERRACK)) return false;
        if (!worldIn.isAirBlock(position) && !mcc$modernAt(worldIn, position).is(net.minecraft.world.level.block.Blocks.NETHERRACK)) return false;
        int rock = 0;
        int air = 0;
        for (var f : new legacyconnector.v1_12_2.net.minecraft.util.EnumFacing[] {legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.WEST,
                legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.EAST, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.NORTH,
                legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.SOUTH, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.DOWN}) {
            if (mcc$modernAt(worldIn, position.offset(f)).is(net.minecraft.world.level.block.Blocks.NETHERRACK)) ++rock;
            if (worldIn.isAirBlock(position.offset(f))) ++air;
        }
        if (!insideRock && rock == 4 && air == 1 || rock == 5) {
            if (block != null) worldIn.setBlockState(position, block.getDefaultState(), 2);
            else mcc$setModern(worldIn, position, net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState());
            var access = worldIn.mcc$access();
            if (access != null) access.scheduleTick(new net.minecraft.core.BlockPos(position.getX(), position.getY(), position.getZ()), net.minecraft.world.level.material.Fluids.LAVA, 0);
        }
        return true;
    }
}
