package legacyconnector.vb1_7_3.net.minecraft.world.biome;

/** Identity-bearing Beta biome constants for the ModLoader spawn API. Custom biomes need a native biome bridge. */
public class Biome {
    public static final Biome RAINFOREST=new Biome("Rainforest","jungle");
    public static final Biome SWAMPLAND=new Biome("Swampland","swamp");
    public static final Biome SEASONAL_FOREST=new Biome("Seasonal Forest","forest");
    public static final Biome FOREST=new Biome("Forest","forest");
    public static final Biome SAVANNA=new Biome("Savanna","savanna");
    public static final Biome SHRUBLAND=new Biome("Shrubland","plains");
    public static final Biome TAIGA=new Biome("Taiga","taiga");
    public static final Biome DESERT=new Biome("Desert","desert");
    public static final Biome PLAINS=new Biome("Plains","plains");
    public static final Biome ICE_DESERT=new Biome("Ice Desert","snowy_plains");
    public static final Biome TUNDRA=new Biome("Tundra","snowy_plains");
    public static final Biome HELL=new Biome("Hell","nether_wastes");
    public static final Biome SKY=new Biome("Sky","the_end");
    public java.util.List<EntitySpawnGroup> spawnableMonsters=new java.util.ArrayList<>();
    public java.util.List<EntitySpawnGroup> spawnablePassive=new java.util.ArrayList<>();
    public java.util.List<EntitySpawnGroup> spawnableWaterCreatures=new java.util.ArrayList<>();
    private boolean snow;
    public String name;
    public int grassColor,foliageColor;
    public byte topBlockId=2,soilBlockId=3;
    private final String modern;
    protected Biome(){modern=null;}
    private Biome(String name,String modern){this.name=name;this.modern=modern;}
    protected Biome setName(String name){this.name=name;return this;}
    protected Biome setGrassColor(int color){grassColor=color;return this;}
    protected Biome setFoliageColor(int color){foliageColor=color;return this;}
    public java.util.List<EntitySpawnGroup> getSpawnableEntities(legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup group){return switch(group){case MONSTER->spawnableMonsters;case CREATURE->spawnablePassive;case WATER_CREATURE->spawnableWaterCreatures;};}
    public legacyconnector.vb1_7_3.net.minecraft.world.gen.feature.Feature getRandomTreeFeature(java.util.Random random){return new legacyconnector.vb1_7_3.net.minecraft.world.gen.feature.TreeFeature();}
    protected Biome enableSnow(){snow=true;return this;}
    public boolean canSnow(){return snow;}
    public boolean canRain(){return !snow&&this!=HELL&&this!=DESERT&&this!=ICE_DESERT;}
    public int getSkyColor(float temperature){float t=Math.max(-1,Math.min(1,temperature/3));return java.awt.Color.HSBtoRGB(.62222224F-t*.05F,.5F+t*.1F,1F);}
    public static Biome locateBiome(float temperature,float downfall){return getBiome(temperature,downfall);}
    public static Biome getBiome(double temperature,double downfall){
        downfall*=temperature;
        if(temperature<.1)return TUNDRA;
        if(downfall<.2){if(temperature<.5)return TUNDRA;if(temperature<.95)return SAVANNA;return DESERT;}
        if(downfall>.5&&temperature<.7)return SWAMPLAND;
        if(temperature<.5)return TAIGA;
        if(temperature<.97)return downfall<.35?SHRUBLAND:FOREST;
        if(downfall<.45)return PLAINS;
        return downfall<.9?SEASONAL_FOREST:RAINFOREST;
    }
    public final String mcc$modernBiome(){return modern==null?null:"minecraft:"+modern;}
    public static java.util.List<Biome> mcc$standardBiomes(){return java.util.List.of(RAINFOREST,SWAMPLAND,SEASONAL_FOREST,FOREST,SAVANNA,SHRUBLAND,TAIGA,DESERT,PLAINS,ICE_DESERT,TUNDRA);}
}
