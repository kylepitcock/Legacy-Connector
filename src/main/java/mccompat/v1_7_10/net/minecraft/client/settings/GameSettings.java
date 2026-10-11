package mccompat.v1_7_10.net.minecraft.client.settings;

/**
 * 1.7.10's options. Values are the vanilla defaults; mods read a few (mipmaps, graphics, language). The shim does not
 * write to the modern options.
 */
public class GameSettings {
    public int mipmapLevels = 4;
    public int anisotropicFiltering = 1;
    public boolean fancyGraphics = true;
    public boolean ambientOcclusion = true;
    public int renderDistanceChunks = 8;
    public int particleSetting = 0;
    public float gammaSetting = 0.0F;
    public int thirdPersonView = 0;
    public boolean hideGUI = false;
    public boolean showDebugInfo = false;
    public String language = "en_US";
    public boolean advancedItemTooltips = false;
    public int guiScale = 0;
    public boolean anaglyph = false;
}
