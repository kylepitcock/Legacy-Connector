package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 patch of tall grass: from the surface below the position (through air and leaves), 128 tries scatter the plant where it can
 * stay. Legacy code constructs it with a {@code BlockTallGrass.EnumType}, a synthesized enum the shim cannot name in a constructor;
 * that call links to the plain constructor, so the patch is grass (the type mods pass nearly always).
 */
public class WorldGenTallGrass extends WorldGenerator {
    private net.minecraft.world.level.block.Block mcc$plant = net.minecraft.world.level.block.Blocks.SHORT_GRASS;

    public WorldGenTallGrass() {
        super(false);
    }

    /** The plant by its legacy type name (DEAD_BUSH, GRASS, FERN). */
    public WorldGenTallGrass mcc$setType(String name) {
        if ("FERN".equals(name)) mcc$plant = net.minecraft.world.level.block.Blocks.FERN;
        else if ("DEAD_BUSH".equals(name)) mcc$plant = net.minecraft.world.level.block.Blocks.DEAD_BUSH;
        else mcc$plant = net.minecraft.world.level.block.Blocks.SHORT_GRASS;
        return this;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        var access = worldIn == null ? null : worldIn.mcc$access();
        if (access == null || position == null) return false;
        try {
            var p = position.mcc$modern();
            while (p.getY() > access.getMinBuildHeight()) {
                var s = access.getBlockState(p);
                if (!s.isAir() && !s.is(net.minecraft.tags.BlockTags.LEAVES)) break;
                p = p.below();
            }
            var state = mcc$plant.defaultBlockState();
            for (int i = 0; i < 128; ++i) {
                var q = p.offset(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(4) - rand.nextInt(4), rand.nextInt(8) - rand.nextInt(8));
                if (access.isEmptyBlock(q) && state.canSurvive(access, q)) access.setBlock(q, state, 2);
            }
        } catch (RuntimeException e) {
            // a position outside the generation region
        }
        return true;
    }
}
