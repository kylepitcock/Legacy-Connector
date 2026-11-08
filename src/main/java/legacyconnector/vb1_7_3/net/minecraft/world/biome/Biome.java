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
    public String name;
    public int grassColor,foliageColor;
    public byte topBlockId=2,soilBlockId=3;
    private final String modern;
    protected Biome(){modern=null;}
    private Biome(String name,String modern){this.name=name;this.modern=modern;}
    protected Biome setName(String name){this.name=name;return this;}
    protected Biome setGrassColor(int color){grassColor=color;return this;}
    protected Biome setFoliageColor(int color){foliageColor=color;return this;}
    public final String mcc$modernBiome(){return modern==null?null:"minecraft:"+modern;}
    public static java.util.List<Biome> mcc$standardBiomes(){return java.util.List.of(RAINFOREST,SWAMPLAND,SEASONAL_FOREST,FOREST,SAVANNA,SHRUBLAND,TAIGA,DESERT,PLAINS,ICE_DESERT,TUNDRA);}
}
