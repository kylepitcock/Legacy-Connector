package legacyconnector.v1_12_2.net.minecraft.util;

/** The random "galactic" words the enchanting table shows: the modern client's own generator (client only). */
public class EnchantmentNameParts {
    private static final EnchantmentNameParts INSTANCE = new EnchantmentNameParts();

    public static EnchantmentNameParts getInstance() {
        return INSTANCE;
    }

    /** A random name that fits {@code length} pixels, in the enchanting table's alphabet. */
    public String generateNewRandomName(legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer fontRendererIn, int length) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return net.minecraft.client.gui.screens.inventory.EnchantmentNames.getInstance().getRandomName(mc.font, length).getString();
    }

    public void reseedRandomGenerator(long seed) {
        net.minecraft.client.gui.screens.inventory.EnchantmentNames.getInstance().initSeed(seed);
    }
}
