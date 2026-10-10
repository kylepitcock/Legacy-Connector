package mccompat.v1_7_10.net.minecraft.client.renderer.entity;

import mccompat.v1_7_10.net.minecraft.client.gui.FontRenderer;
import mccompat.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import mccompat.v1_7_10.net.minecraft.world.World;

public class RenderManager {
    public static RenderManager instance = new RenderManager();
    public static double renderPosX;
    public static double renderPosY;
    public static double renderPosZ;
    public TextureManager renderEngine = TextureManager.INSTANCE;
    public FontRenderer fontRenderer = FontRenderer.INSTANCE;
    public World worldObj;
    public float playerViewY;
    public float playerViewX;
    public double viewerPosX;
    public double viewerPosY;
    public double viewerPosZ;

    public FontRenderer getFontRenderer() {
        return fontRenderer;
    }
}
