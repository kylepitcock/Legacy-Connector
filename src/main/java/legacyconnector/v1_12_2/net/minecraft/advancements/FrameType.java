package legacyconnector.v1_12_2.net.minecraft.advancements;

import legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting;

/** The frame of an advancement's icon: task, challenge or goal (its name, texture offset and title colour). */
public enum FrameType {
    TASK("task", 0, TextFormatting.GREEN),
    CHALLENGE("challenge", 26, TextFormatting.DARK_PURPLE),
    GOAL("goal", 52, TextFormatting.GREEN);

    private final String name;
    private final int icon;
    private final TextFormatting format;

    FrameType(String name, int icon, TextFormatting format) {
        this.name = name;
        this.icon = icon;
        this.format = format;
    }

    public String getName() {
        return name;
    }

    public int getIcon() {
        return icon;
    }

    public TextFormatting getFormat() {
        return format;
    }

    public static FrameType byName(String name) {
        for (FrameType t : values()) if (t.name.equals(name)) return t;
        throw new IllegalArgumentException("Unknown frame type '" + name + "'");
    }

    public static FrameType mcc$of(net.minecraft.advancements.AdvancementType type) {
        return switch (type) {
            case CHALLENGE -> CHALLENGE;
            case GOAL -> GOAL;
            default -> TASK;
        };
    }
}
