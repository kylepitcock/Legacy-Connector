package legacyconnector.v1_7_10.net.minecraft.world.biome;

import legacyconnector.v1_7_10.net.minecraft.init.Blocks;

/** The desert: sand over sand, no trees or animals, dead bushes, reeds and cacti (mods extend it for their own deserts). */
public class BiomeGenDesert extends BiomeGenBase {
    public BiomeGenDesert(int id) {
        super(id);
        this.spawnableCreatureList.clear();
        this.topBlock = Blocks.sand;
        this.fillerBlock = Blocks.sand;
        this.theBiomeDecorator.treesPerChunk = -999;
        this.theBiomeDecorator.deadBushPerChunk = 2;
        this.theBiomeDecorator.reedsPerChunk = 50;
        this.theBiomeDecorator.cactiPerChunk = 10;
    }
}
