package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's spring: a fluid source in a wall of stone with one open side. */
public class WorldGenLiquids extends WorldGenerator {
    private final Block block;

    public WorldGenLiquids(Block blockIn) {
        this.block = blockIn;
    }

    private boolean stone(World w, BlockPos p) {
        var s = mcc$modernAt(w, p);
        return s.is(net.minecraft.world.level.block.Blocks.STONE) || s.is(net.minecraft.world.level.block.Blocks.DEEPSLATE) || s.is(net.minecraft.world.level.block.Blocks.GRANITE) || s.is(net.minecraft.world.level.block.Blocks.DIORITE) || s.is(net.minecraft.world.level.block.Blocks.ANDESITE);
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        if (!stone(worldIn, position.up()) || !stone(worldIn, position.down())) return false;
        if (!worldIn.isAirBlock(position) && !stone(worldIn, position)) return false;
        int rock = 0;
        int air = 0;
        for (BlockPos p : new BlockPos[] {position.west(), position.east(), position.north(), position.south()}) {
            if (stone(worldIn, p)) ++rock;
            if (worldIn.isAirBlock(p)) ++air;
        }
        if (rock == 3 && air == 1 && block != null) worldIn.setBlockState(position, block.getDefaultState(), 2);
        return true;
    }
}
