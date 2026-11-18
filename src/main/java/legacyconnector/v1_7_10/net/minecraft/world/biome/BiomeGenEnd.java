package legacyconnector.v1_7_10.net.minecraft.world.biome;

import legacyconnector.v1_7_10.net.minecraft.init.Blocks;

/** The End (1.7.10 {@code BiomeGenEnd}): only endermen spawn, dirt on top, the End's island decorator (mods extend it for their own End-like biomes). */
public class BiomeGenEnd extends BiomeGenBase {
    public BiomeGenEnd(int id) {
        super(id);
        this.spawnableMonsterList.clear();
        this.spawnableCreatureList.clear();
        this.spawnableWaterCreatureList.clear();
        this.spawnableCaveCreatureList.clear();
        this.spawnableMonsterList.add(new SpawnListEntry(legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityEnderman.class, 10, 4, 4));
        this.topBlock = Blocks.dirt;
        this.fillerBlock = Blocks.dirt;
        this.theBiomeDecorator = new BiomeEndDecorator();
    }
}
