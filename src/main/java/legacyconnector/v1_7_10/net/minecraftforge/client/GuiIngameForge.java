package legacyconnector.v1_7_10.net.minecraftforge.client;

import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiIngame;

/** Forge's HUD: mods that draw their own bars below the health row read and bump the heights. */
public class GuiIngameForge extends GuiIngame {
    public static int left_height = 39;
    public static int right_height = 39;
    public static boolean renderHelmet = true;
    public static boolean renderPortal = true;
    public static boolean renderHotbar = true;
    public static boolean renderCrosshairs = true;
    public static boolean renderBossHealth = true;
    public static boolean renderHealth = true;
    public static boolean renderArmor = true;
    public static boolean renderFood = true;
    public static boolean renderAir = true;
    public static boolean renderExperiance = true;
    public static boolean renderObjective = true;
}
