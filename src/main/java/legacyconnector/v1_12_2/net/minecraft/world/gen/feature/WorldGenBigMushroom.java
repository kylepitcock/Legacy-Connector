package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's huge mushroom (random kind unless given): the modern game's own feature (its successor) is placed. */
public class WorldGenBigMushroom extends WorldGenerator {
    private legacyconnector.v1_12_2.net.minecraft.block.Block mushroomType;

    private net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> kind(Random rand) {
        var name = mushroomType == null ? null : mushroomType.getRegistryName();
        boolean red = name != null ? name.getPath().contains("red") : rand.nextBoolean();
        return red ? net.minecraft.data.worldgen.features.TreeFeatures.HUGE_RED_MUSHROOM : net.minecraft.data.worldgen.features.TreeFeatures.HUGE_BROWN_MUSHROOM;
    }

    public WorldGenBigMushroom() {
        super(true);
        this.mushroomType = null;
    }

    public WorldGenBigMushroom(legacyconnector.v1_12_2.net.minecraft.block.Block block) {
        super(true);
        this.mushroomType = block;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.place(worldIn, kind(rand), rand, position);
    }
}
