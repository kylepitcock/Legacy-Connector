package legacyconnector.vb1_7_3.net.minecraft.block.material;

import legacyconnector.vb1_7_3.net.minecraft.block.MapColor;

/**
 * Beta 1.7.3 {@code Material}. The original used subclasses for liquids ({@code MaterialLiquid}), "logic" materials that neither block
 * vision nor movement (plants, circuits, snow layers), transparent ones (air, fire) and the portal; here they are flags set by the
 * constants, so a mod's own {@code new Material(color)} still is a plain solid material.
 */
public class Material {
    private static final int SOLID = 0, LIQUID = 1, LOGIC = 2, TRANSPARENT = 3, PORTAL = 4;

    public static final Material AIR = new Material(MapColor.CLEAR, TRANSPARENT).setReplaceable();
    public static final Material SOLID_ORGANIC = new Material(MapColor.PALE_GREEN, SOLID);
    public static final Material SOIL = new Material(MapColor.BROWN, SOLID);
    public static final Material WOOD = new Material(MapColor.BROWN, SOLID).setBurnable();
    public static final Material STONE = new Material(MapColor.GRAY, SOLID).setRequiresTool();
    public static final Material METAL = new Material(MapColor.SILVER, SOLID).setRequiresTool();
    public static final Material WATER = new Material(MapColor.BLUE, LIQUID).setDestroyPistonBehavior();
    public static final Material LAVA = new Material(MapColor.RED, LIQUID).setDestroyPistonBehavior();
    public static final Material LEAVES = new Material(MapColor.GREEN, SOLID).setBurnable().setTransparent().setDestroyPistonBehavior();
    public static final Material PLANT = new Material(MapColor.GREEN, LOGIC).setDestroyPistonBehavior();
    public static final Material SPONGE = new Material(MapColor.WHITE, SOLID);
    public static final Material WOOL = new Material(MapColor.WHITE, SOLID).setBurnable();
    public static final Material FIRE = new Material(MapColor.CLEAR, TRANSPARENT).setDestroyPistonBehavior();
    public static final Material SAND = new Material(MapColor.PALE_YELLOW, SOLID);
    public static final Material PISTON_BREAKABLE = new Material(MapColor.CLEAR, LOGIC).setDestroyPistonBehavior();
    public static final Material GLASS = new Material(MapColor.CLEAR, SOLID).setTransparent();
    public static final Material TNT = new Material(MapColor.RED, SOLID).setBurnable().setTransparent();
    public static final Material UNUSED = new Material(MapColor.GREEN, SOLID).setDestroyPistonBehavior();
    public static final Material ICE = new Material(MapColor.LIGHT_BLUE, SOLID).setTransparent();
    public static final Material SNOW_LAYER = new Material(MapColor.WHITE, LOGIC).setReplaceable().setTransparent().setRequiresTool().setDestroyPistonBehavior();
    public static final Material SNOW_BLOCK = new Material(MapColor.WHITE, SOLID).setRequiresTool();
    public static final Material CACTUS = new Material(MapColor.GREEN, SOLID).setTransparent().setDestroyPistonBehavior();
    public static final Material CLAY = new Material(MapColor.LIGHT_GRAY, SOLID);
    public static final Material PUMPKIN = new Material(MapColor.GREEN, SOLID).setDestroyPistonBehavior();
    public static final Material NETHER_PORTAL = new Material(MapColor.CLEAR, PORTAL).setUnpushablePistonBehavior();
    public static final Material CAKE = new Material(MapColor.CLEAR, SOLID).setDestroyPistonBehavior();
    public static final Material COBWEB = new Material(MapColor.WHITE, SOLID).setRequiresTool().setDestroyPistonBehavior();
    public static final Material PISTON = new Material(MapColor.GRAY, SOLID).setUnpushablePistonBehavior();

    public boolean burnable;
    public boolean replaceable;
    public boolean transparent;
    public final MapColor mapColor;
    public boolean handHarvestable = true;
    public int pistonBehavior;
    private final int kind;

    public Material(MapColor mapColor) {
        this(mapColor, SOLID);
    }

    private Material(MapColor mapColor, int kind) {
        this.mapColor = mapColor;
        this.kind = kind;
    }

    public boolean isFluid() {
        return kind == LIQUID;
    }

    public boolean isSolid() {
        return kind == SOLID;
    }

    public boolean blocksVision() {
        return kind == SOLID;
    }

    public boolean blocksMovement() {
        return kind == SOLID;
    }

    protected Material setTransparent() {
        this.transparent = true;
        return this;
    }

    protected Material setRequiresTool() {
        this.handHarvestable = false;
        return this;
    }

    protected Material setBurnable() {
        this.burnable = true;
        return this;
    }

    public boolean isBurnable() {
        return burnable;
    }

    public Material setReplaceable() {
        this.replaceable = true;
        return this;
    }

    public boolean isReplaceable() {
        return replaceable || kind == LIQUID;
    }

    public boolean suffocates() {
        return !transparent && blocksMovement();
    }

    public boolean isHandHarvestable() {
        return handHarvestable;
    }

    public int getPistonBehavior() {
        return pistonBehavior;
    }

    protected Material setDestroyPistonBehavior() {
        this.pistonBehavior = 1;
        return this;
    }

    protected Material setUnpushablePistonBehavior() {
        this.pistonBehavior = 2;
        return this;
    }

    public boolean mcc$isPortal() {
        return kind == PORTAL;
    }
}
