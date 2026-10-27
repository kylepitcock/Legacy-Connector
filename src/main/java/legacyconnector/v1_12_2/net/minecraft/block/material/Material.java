package legacyconnector.v1_12_2.net.minecraft.block.material;

/** 1.12.2 block material: solidity, tool requirement, flammability and piston behaviour; the bridge maps these onto modern block properties. */
public class Material {
    public static final Material AIR = new Material("air", false, false).setReplaceable();
    public static final Material GRASS = new Material("grass", true, true);
    public static final Material GROUND = new Material("ground", true, true);
    public static final Material WOOD = new Material("wood", true, true).setBurning();
    public static final Material ROCK = new Material("rock", true, true).setRequiresTool();
    public static final Material IRON = new Material("iron", true, true).setRequiresTool();
    public static final Material ANVIL = new Material("anvil", true, true).setRequiresTool().setImmovableMobility();
    public static final Material WATER = new Material("water", false, false).setNoPushMobility().setReplaceable();
    public static final Material LAVA = new Material("lava", false, false).setNoPushMobility().setReplaceable();
    public static final Material LEAVES = new Material("leaves", true, false).setBurning().setTranslucent().setNoPushMobility();
    public static final Material PLANTS = new Material("plants", false, false).setNoPushMobility();
    public static final Material VINE = new Material("vine", false, false).setBurning().setNoPushMobility().setReplaceable();
    public static final Material SPONGE = new Material("sponge", true, true);
    public static final Material CLOTH = new Material("cloth", true, true).setBurning();
    public static final Material FIRE = new Material("fire", false, false).setNoPushMobility().setReplaceable();
    public static final Material SAND = new Material("sand", true, true);
    public static final Material CIRCUITS = new Material("circuits", false, false).setNoPushMobility();
    public static final Material CARPET = new Material("carpet", false, false).setBurning();
    public static final Material GLASS = new Material("glass", true, false).setTranslucent();
    public static final Material REDSTONE_LIGHT = new Material("redstone_light", true, true);
    public static final Material TNT = new Material("tnt", true, false).setBurning().setTranslucent();
    public static final Material CORAL = new Material("coral", true, true).setNoPushMobility();
    public static final Material ICE = new Material("ice", true, false).setTranslucent();
    public static final Material PACKED_ICE = new Material("packed_ice", true, true);
    public static final Material SNOW = new Material("snow", true, true).setReplaceable().setTranslucent().setRequiresTool().setNoPushMobility();
    public static final Material CRAFTED_SNOW = new Material("crafted_snow", true, true).setRequiresTool();
    public static final Material CACTUS = new Material("cactus", true, false).setTranslucent().setNoPushMobility();
    public static final Material CLAY = new Material("clay", true, true);
    public static final Material GOURD = new Material("gourd", true, true).setNoPushMobility();
    public static final Material DRAGON_EGG = new Material("dragon_egg", true, true).setNoPushMobility();
    public static final Material PORTAL = new Material("portal", false, false).setImmovableMobility();
    public static final Material CAKE = new Material("cake", true, true).setNoPushMobility();
    public static final Material WEB = new Material("web", true, false).setRequiresTool().setNoPushMobility();
    public static final Material PISTON = new Material("piston", true, true).setImmovableMobility();
    public static final Material BARRIER = new Material("barrier", true, true).setRequiresTool().setImmovableMobility();
    public static final Material STRUCTURE_VOID = new Material("structure_void", false, false).setReplaceable();

    private final String name;
    private final boolean solid;
    private final boolean blocksMovement;
    private boolean canBurn;
    private boolean replaceable;
    private boolean isTranslucent;
    private boolean requiresNoTool = true;
    /** 0 normal, 1 destroyed when pushed, 2 immovable: the order of 1.12's EnumPushReaction NORMAL, DESTROY, BLOCK. */
    private int mobilityFlag;
    private MapColor materialMapColor = MapColor.STONE;

    public Material(String name, boolean solid, boolean blocksMovement) {
        this.name = name;
        this.solid = solid;
        this.blocksMovement = blocksMovement;
    }

    /** The 1.12 constructor mods use: a solid material that blocks movement (MaterialLiquid/Logic/Transparent say otherwise). */
    public Material(MapColor color) {
        this("", true, true);
        this.materialMapColor = color == null ? MapColor.STONE : color;
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
        return !isTranslucent && blocksMovement;
    }

    /** Light passes through translucent materials (leaves, glass, ice). */
    public boolean blocksLight() {
        return !isTranslucent && solid;
    }

    public boolean isToolNotRequired() {
        return requiresNoTool;
    }

    protected Material setRequiresTool() {
        this.requiresNoTool = false;
        return this;
    }

    protected Material setBurning() {
        this.canBurn = true;
        return this;
    }

    public boolean getCanBurn() {
        return canBurn;
    }

    public Material setReplaceable() {
        this.replaceable = true;
        return this;
    }

    public boolean isReplaceable() {
        return replaceable;
    }

    protected Material setTranslucent() {
        this.isTranslucent = true;
        return this;
    }

    private boolean isAdventureModeExempt;

    protected Material setAdventureModeExempt() {
        this.isAdventureModeExempt = true;
        return this;
    }

    public boolean isAdventureModeExempt() {
        return isAdventureModeExempt;
    }

    protected Material setNoPushMobility() {
        this.mobilityFlag = 1;
        return this;
    }

    protected Material setImmovableMobility() {
        this.mobilityFlag = 2;
        return this;
    }

    /** The modern piston behaviour of this material's blocks. */
    public net.minecraft.world.level.material.PushReaction mcc$pushReaction() {
        return switch (mobilityFlag) {
            case 1 -> net.minecraft.world.level.material.PushReaction.DESTROY;
            case 2 -> net.minecraft.world.level.material.PushReaction.BLOCK;
            default -> net.minecraft.world.level.material.PushReaction.NORMAL;
        };
    }

    public MapColor getMaterialMapColor() {
        return materialMapColor;
    }

    public EnumPushReaction getPushReaction() {
        return EnumPushReaction.values()[mobilityFlag];
    }
}
