package legacyconnector.v1_7_10.net.minecraft.world.biome;

import legacyconnector.v1_7_10.net.minecraft.init.Blocks;

/** The mesa (badlands): red sand over stained clay, no animals (mods extend it for their own badlands). */
public class BiomeGenMesa extends BiomeGenBase {
    private final boolean field_150626_aH;
    private final boolean field_150620_aI;

    public BiomeGenMesa(int id, boolean bryce, boolean trees) {
        super(id);
        this.field_150626_aH = bryce;
        this.field_150620_aI = trees;
        this.spawnableCreatureList.clear();
        this.topBlock = Blocks.sand;
        this.fillerBlock = Blocks.hardened_clay;
        this.theBiomeDecorator.treesPerChunk = trees ? 5 : -999;
        this.theBiomeDecorator.deadBushPerChunk = 20;
        this.theBiomeDecorator.reedsPerChunk = 3;
        this.theBiomeDecorator.cactiPerChunk = 5;
    }
}
