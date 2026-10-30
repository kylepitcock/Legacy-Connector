package legacyconnector.v1_12_2.net.minecraft.world.biome;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 desert: sand surface, no trees, dead bushes, reeds and cacti, rabbits instead of farm animals, rare wells and fossils. */
public class BiomeDesert extends Biome {
    public BiomeDesert(BiomeProperties properties) {
        super(properties);
        spawnableCreatureList.clear();
        try {
            var sand = org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.blockByName("sand");
            if (sand != null) {
                topBlock = sand.getDefaultState();
                fillerBlock = sand.getDefaultState();
            }
        } catch (RuntimeException | LinkageError e) {
            // keeps the default surface
        }
        decorator.treesPerChunk = -999;
        decorator.deadBushPerChunk = 2;
        decorator.reedsPerChunk = 50;
        decorator.cactiPerChunk = 10;
        spawnableCreatureList.add(new SpawnListEntry(legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityRabbit.class, 4, 2, 3));
    }

    @Override
    public void decorate(World worldIn, Random rand, BlockPos pos) {
        super.decorate(worldIn, rand, pos);
        try {
            if (rand.nextInt(1000) == 0) {
                int i = rand.nextInt(16) + 8;
                int j = rand.nextInt(16) + 8;
                BlockPos at = worldIn.getHeight(pos.add(i, 0, j)).up();
                new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenDesertWells().generate(worldIn, rand, at);
            }
            if (rand.nextInt(64) == 0) new legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenFossils().generate(worldIn, rand, pos);
        } catch (RuntimeException e) {
            // world generation must not fail
        }
    }
}
