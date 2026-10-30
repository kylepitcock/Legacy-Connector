package legacyconnector.v1_12_2.net.minecraft.world.biome;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 forest (normal, flower, birch, roofed): dense trees, wolves, and tall flowers in clusters. */
public class BiomeForest extends Biome {
    protected final Type type;

    public BiomeForest(Type typeIn, BiomeProperties properties) {
        super(properties);
        this.type = typeIn == null ? Type.NORMAL : typeIn;
        decorator.treesPerChunk = 10;
        decorator.grassPerChunk = 2;
        if (this.type == Type.FLOWER) {
            decorator.treesPerChunk = 6;
            decorator.flowersPerChunk = 100;
            decorator.grassPerChunk = 1;
            spawnableCreatureList.add(new SpawnListEntry(legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityRabbit.class, 4, 2, 3));
        }
        if (this.type == Type.NORMAL) {
            spawnableCreatureList.add(new SpawnListEntry(legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityWolf.class, 5, 4, 4));
        }
        if (this.type == Type.ROOFED) decorator.treesPerChunk = -999;
    }

    /** Places {@code amount} clusters of lilacs, rose bushes or peonies (five tries each) in the chunk at {@code pos}. */
    public void addDoublePlants(World worldIn, Random random, BlockPos pos, int amount) {
        try {
            for (int i = 0; i < amount; ++i) {
                int j = random.nextInt(3);
                DOUBLE_PLANT_GENERATOR.mcc$setPlantType(j == 0 ? "SYRINGA" : j == 1 ? "ROSE" : "PAEONIA");
                for (int k = 0; k < 5; ++k) {
                    int x = random.nextInt(16) + 8;
                    int z = random.nextInt(16) + 8;
                    int top = worldIn.getHeight(pos.add(x, 0, z)).getY() + 32;
                    if (top <= 0) continue;
                    int y = random.nextInt(top);
                    if (DOUBLE_PLANT_GENERATOR.generate(worldIn, random, new BlockPos(pos.getX() + x, y, pos.getZ() + z))) break;
                }
            }
        } catch (RuntimeException e) {
            // world generation must not fail
        }
    }

    public enum Type {
        NORMAL, FLOWER, BIRCH, ROOFED
    }
}
