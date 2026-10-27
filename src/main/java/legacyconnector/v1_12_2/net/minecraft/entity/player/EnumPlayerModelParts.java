package legacyconnector.v1_12_2.net.minecraft.entity.player;

/** 1.12.2 skin layers a player can show or hide; the same parts, bits and order as the modern {@code PlayerModelPart}. */
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

    EnumPlayerModelParts(int partId, String partName) {
        this.partId = partId;
        this.partMask = 1 << partId;
        this.partName = partName;
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
        return new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation("options.modelPart." + partName);
    }

    public net.minecraft.world.entity.player.PlayerModelPart mcc$modern() {
        return net.minecraft.world.entity.player.PlayerModelPart.values()[ordinal()];
    }
}
