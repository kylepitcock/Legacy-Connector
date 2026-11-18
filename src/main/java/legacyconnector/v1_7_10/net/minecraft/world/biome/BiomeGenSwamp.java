package legacyconnector.v1_7_10.net.minecraft.world.biome;

/** The swamp (1.7.10 {@code BiomeGenSwamp}): few trees, lily pads, reeds, clay, mushrooms, murky water, slimes at night (mods extend it for their own bogs). */
public class BiomeGenSwamp extends BiomeGenBase {
    public BiomeGenSwamp(int id) {
        super(id);
        this.theBiomeDecorator.treesPerChunk = 2;
        this.theBiomeDecorator.flowersPerChunk = 1;
        this.theBiomeDecorator.deadBushPerChunk = 1;
        this.theBiomeDecorator.mushroomsPerChunk = 8;
        this.theBiomeDecorator.reedsPerChunk = 10;
        this.theBiomeDecorator.clayPerChunk = 1;
        this.theBiomeDecorator.waterlilyPerChunk = 4;
        this.theBiomeDecorator.sandPerChunk2 = 0;
        this.theBiomeDecorator.sandPerChunk = 0;
        this.theBiomeDecorator.grassPerChunk = 5;
        this.waterColorMultiplier = 14745518;
        this.spawnableMonsterList.add(new SpawnListEntry(legacyconnector.v1_7_10.net.minecraft.entity.monster.EntitySlime.class, 1, 1, 1));
    }
}
