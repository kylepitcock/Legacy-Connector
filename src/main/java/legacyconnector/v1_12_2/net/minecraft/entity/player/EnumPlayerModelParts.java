package legacyconnector.v1_12_2.net.minecraft.entity.player;

/**
 * The skin layers a player can show (cape, jacket, sleeves, trouser legs, hat), in the game's order.
 * (Created for area R2b's {@code isWearing}; area E owns the class.)
 */
public enum EnumPlayerModelParts {
    CAPE(0, "cape"),
    JACKET(1, "jacket"),
    LEFT_SLEEVE(2, "left_sleeve"),
    RIGHT_SLEEVE(3, "right_sleeve"),
    LEFT_PANTS_LEG(4, "left_pants_leg"),
    RIGHT_PANTS_LEG(5, "right_pants_leg"),
    HAT(6, "hat");

    private final int partId;
    private final int partMask;
    private final String partName;
    private final legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent name;

    EnumPlayerModelParts(int partId, String partName) {
        this.partId = partId;
        this.partMask = 1 << partId;
        this.partName = partName;
        this.name = new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation("options.modelPart." + partName);
    }

    public int getPartMask() {
        return partMask;
    }

    public int getPartId() {
        return partId;
    }

    public String getPartName() {
        return partName;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getName() {
        return name;
    }

    /** The game's part (not a 1.12 method). */
    public net.minecraft.world.entity.player.PlayerModelPart mcc$modern() {
        return net.minecraft.world.entity.player.PlayerModelPart.values()[partId];
    }
}
