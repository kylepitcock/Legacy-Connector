package legacyconnector.v1_12_2.net.minecraftforge.client;

/**
 * Forge's HUD. Its static switches turn HUD elements off (a mod drawing its own hearts clears {@code renderHealth}): a switched-off element's
 * game layer is skipped. {@code left_height}/{@code right_height} are the heights of the rows above the hotbar, reset to 39 every frame;
 * handlers that draw rows of their own add to them.
 */
public class GuiIngameForge extends legacyconnector.v1_12_2.net.minecraft.client.gui.GuiIngame {
    public static final legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation ICONS =
            new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("textures/gui/icons.png");
    public static boolean renderVignette = true;
    public static boolean renderHelmet = true;
    public static boolean renderPortal = true;
    public static boolean renderHotbar = true;
    public static boolean renderCrosshairs = true;
    public static boolean renderBossHealth = true;
    public static boolean renderHealth = true;
    public static boolean renderArmor = true;
    public static boolean renderFood = true;
    public static boolean renderHealthMount = true;
    public static boolean renderAir = true;
    public static boolean renderExperiance = true;
    public static boolean renderJumpBar = true;
    public static boolean renderObjective = true;
    public static int left_height = 39;
    public static int right_height = 39;

    public GuiIngameForge(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc) {
        super(mc);
    }
}
