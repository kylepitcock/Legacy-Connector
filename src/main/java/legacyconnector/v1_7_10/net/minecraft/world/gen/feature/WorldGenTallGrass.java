package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;

/** Scatters a plant (tall grass, ferns, mod plants) on the ground around a position. */
public class WorldGenTallGrass extends WorldGenerator {
    private Block block;
    /** The block of a 1.6.4 id constructor, looked up when generating (1.6.4 read {@code blocksList[id]} then). */
    private java.util.function.Supplier<Block> mcc$byId;
    private int meta;

    public WorldGenTallGrass(Block block, int meta) {
        this.block = block;
        this.meta = meta;
    }

    /** 1.6.4: {@code new WorldGenTallGrass(blockId, meta)}. */
    public WorldGenTallGrass(int blockId, int meta) {
        this.mcc$byId = legacyconnector.v1_6_4.net.minecraft.util.Ids.lazyBlock(blockId);
        this.meta = meta;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        if (block == null && mcc$byId != null) block = mcc$byId.get();
        if (block == null) return false;
        var level = world.mcc$level();
        // Down through air and leaves to the ground.
        while (y > level.getMinBuildHeight()) {
            var s = level.getBlockState(new BlockPos(x, y, z));
            if (!s.isAir() && !s.is(BlockTags.LEAVES)) break;
            y--;
        }
        var state = BlockBridge.toModern(block, meta);
        for (int i = 0; i < 128; i++) {
            BlockPos p = new BlockPos(x + random.nextInt(8) - random.nextInt(8), y + random.nextInt(4) - random.nextInt(4), z + random.nextInt(8) - random.nextInt(8));
            if (level.getBlockState(p).isAir() && state.canSurvive(level, p)) level.setBlock(p, state, 2);
        }
        return true;
    }
}
