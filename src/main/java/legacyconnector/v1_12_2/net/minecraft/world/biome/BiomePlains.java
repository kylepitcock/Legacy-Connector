package legacyconnector.v1_12_2.net.minecraft.world.biome;

import legacyconnector.v1_12_2.net.minecraft.block.BlockDoublePlant;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 plains: open grassland (no trees, many flowers and grass; sunflower plains add sunflower patches), horses and donkeys. */
public class BiomePlains extends Biome {
    protected boolean sunflowers;

    public BiomePlains(boolean sunflowersIn, BiomeProperties properties) {
        super(properties);
        this.sunflowers = sunflowersIn;
        this.spawnableCreatureList.add(new SpawnListEntry(legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityHorse.class, 5, 2, 6));
        this.spawnableCreatureList.add(new SpawnListEntry(legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityDonkey.class, 1, 1, 3));
        this.decorator.treesPerChunk = -999;
        this.decorator.flowersPerChunk = 4;
        this.decorator.grassPerChunk = 10;
    }

    @Override
    public void decorate(World worldIn, java.util.Random rand, BlockPos pos) {
        if (sunflowers) {
            DOUBLE_PLANT_GENERATOR.setPlantType(BlockDoublePlant.EnumPlantType.SUNFLOWER);
            for (int i = 0; i < 10; ++i) {
                int x = rand.nextInt(16) + 8;
                int z = rand.nextInt(16) + 8;
                int y = rand.nextInt(worldIn.getHeight(pos.add(x, 0, z)).getY() + 32);
                DOUBLE_PLANT_GENERATOR.generate(worldIn, rand, pos.add(x, y, z));
            }
        }
        super.decorate(worldIn, rand, pos);
    }
}
