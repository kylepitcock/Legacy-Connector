package legacyconnector.v1_6_4.facade.net.minecraft.block;

/** Facade of the 1.7.10 shim's {@code BlockLeaves} for 1.6.4 mods: the texture names by graphics mode and variant. Never instantiated. */
public final class BlockLeaves {
    private BlockLeaves() {}

    private static final String[][] TEXTURES = {
        {"leaves", "leaves_spruce", "leaves", "leaves_jungle"},
        {"leaves_opaque", "leaves_spruce_opaque", "leaves_opaque", "leaves_jungle_opaque"}};

    /** 1.6.4 {@code field_94396_b}: index 0 fancy graphics, 1 fast; then oak, spruce, birch, jungle. */
    public static String[][] mcc$get$field_94396_b() {
        return TEXTURES;
    }
}
