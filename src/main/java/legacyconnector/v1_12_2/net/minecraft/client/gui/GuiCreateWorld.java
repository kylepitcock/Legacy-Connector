package legacyconnector.v1_12_2.net.minecraft.client.gui;

/**
 * The create-world screen. Displaying it opens the modern world creation screen. Mods' world type customisation screens get one as
 * their parent and write their settings into {@link #chunkProviderSettingsJson}, as 1.12's customize button expected; the selected
 * world type index is {@link #selectedIndex}.
 */
public class GuiCreateWorld extends GuiScreen {
    public static final String[] DISALLOWED_FILENAMES = new String[] {"CON", "COM", "PRN", "AUX", "CLOCK$", "NUL", "COM1", "COM2", "COM3",
            "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};
    private final GuiScreen parentScreen;
    public String chunkProviderSettingsJson = "";
    public int selectedIndex;

    public GuiCreateWorld(GuiScreen parent) {
        this.parentScreen = parent;
    }

    public GuiScreen mcc$parent() {
        return parentScreen;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /** A save folder name for a world name: unsafe characters and reserved device names escaped, then "-" added until it is free. */
    public static String getUncollidingSaveDirName(legacyconnector.v1_12_2.net.minecraft.world.storage.ISaveFormat saveLoader, String name) {
        name = name.replaceAll("[\\./\"]", "_");
        for (String s : DISALLOWED_FILENAMES) {
            if (name.equalsIgnoreCase(s)) name = "_" + name + "_";
        }
        while (saveLoader != null && saveLoader.getWorldInfo(name) != null) name = name + "-";
        return name;
    }
}
