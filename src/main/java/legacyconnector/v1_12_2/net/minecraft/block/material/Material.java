package legacyconnector.v1_12_2.net.minecraft.block.material;

/** 1.12.2 block material: only what the bridge needs to pick a sound and mining rules. */
public class Material {
    public static final Material AIR = new Material("air", false, false);
    public static final Material GRASS = new Material("grass", true, true);
    public static final Material GROUND = new Material("ground", true, true);
    public static final Material WOOD = new Material("wood", true, true);
    public static final Material ROCK = new Material("rock", true, true);
    public static final Material IRON = new Material("iron", true, true);
    public static final Material ANVIL = new Material("anvil", true, true);
    public static final Material WATER = new Material("water", false, false);
    public static final Material LAVA = new Material("lava", false, false);
    public static final Material LEAVES = new Material("leaves", true, false);
    public static final Material PLANTS = new Material("plants", false, false);
    public static final Material VINE = new Material("vine", false, false);
    public static final Material SPONGE = new Material("sponge", true, true);
    public static final Material CLOTH = new Material("cloth", true, true);
    public static final Material FIRE = new Material("fire", false, false);
    public static final Material SAND = new Material("sand", true, true);
    public static final Material CIRCUITS = new Material("circuits", false, false);
    public static final Material CARPET = new Material("carpet", false, false);
    public static final Material GLASS = new Material("glass", true, false);
    public static final Material REDSTONE_LIGHT = new Material("redstone_light", true, true);
    public static final Material TNT = new Material("tnt", true, false);
    public static final Material CORAL = new Material("coral", true, true);
    public static final Material ICE = new Material("ice", true, false);
    public static final Material PACKED_ICE = new Material("packed_ice", true, true);
    public static final Material SNOW = new Material("snow", true, true);
    public static final Material CRAFTED_SNOW = new Material("crafted_snow", true, true);
    public static final Material CACTUS = new Material("cactus", true, false);
    public static final Material CLAY = new Material("clay", true, true);
    public static final Material GOURD = new Material("gourd", true, true);
    public static final Material DRAGON_EGG = new Material("dragon_egg", true, true);
    public static final Material PORTAL = new Material("portal", false, false);
    public static final Material CAKE = new Material("cake", true, true);
    public static final Material WEB = new Material("web", true, false);
    public static final Material PISTON = new Material("piston", true, true);
    public static final Material BARRIER = new Material("barrier", true, true);
    public static final Material STRUCTURE_VOID = new Material("structure_void", false, false);

    private final String name;
    private final boolean solid;
    private final boolean blocksMovement;

    public Material(String name, boolean solid, boolean blocksMovement) {
        this.name = name;
        this.solid = solid;
        this.blocksMovement = blocksMovement;
    }

    public String mcc$name() {
        return name;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean blocksMovement() {
        return blocksMovement;
    }

    public boolean isLiquid() {
        return this == WATER || this == LAVA;
    }

    public boolean isOpaque() {
        return solid && blocksMovement;
    }

    public boolean isToolNotRequired() {
        return this == GRASS || this == GROUND || this == WOOD || this == LEAVES || this == PLANTS || this == CLOTH || this == SAND
                || this == GLASS || this == CIRCUITS || this == CLAY || this == SNOW || this == CRAFTED_SNOW || this == GOURD || this == CACTUS;
    }

    public MapColor getMaterialMapColor() {
        return MapColor.STONE;
    }
}
