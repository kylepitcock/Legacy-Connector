package legacyconnector.v1_12_2.net.minecraft.advancements;

import legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting;

/** The frame of an advancement's icon (task, challenge, goal), as 1.12.2's. */
public enum FrameType {
    TASK("task", 0, TextFormatting.GREEN),
    CHALLENGE("challenge", 26, TextFormatting.DARK_PURPLE),
    GOAL("goal", 52, TextFormatting.GREEN);

    private final String name;
    private final int icon;
    private final TextFormatting format;

    FrameType(String nameIn, int iconIn, TextFormatting formatIn) {
        this.name = nameIn;
        this.icon = iconIn;
        this.format = formatIn;
    }

    public String getName() {
        return name;
    }

    public int getIcon() {
        return icon;
    }

    public static FrameType byName(String nameIn) {
        for (FrameType t : values()) if (t.name.equals(nameIn)) return t;
        throw new IllegalArgumentException("Unknown frame type '" + nameIn + "'");
    }

    public TextFormatting getFormat() {
        return format;
    }

    public static FrameType mcc$of(net.minecraft.advancements.AdvancementType type) {
        return type == null ? TASK : byName(type.getSerializedName());
    }
}
