package mccompat.v1_7_10.net.minecraft.client;

import mccompat.v1_7_10.net.minecraft.client.entity.EntityClientPlayerMP;
import mccompat.v1_7_10.net.minecraft.client.gui.FontRenderer;
import mccompat.v1_7_10.net.minecraft.client.gui.GuiScreen;
import mccompat.v1_7_10.net.minecraft.client.multiplayer.WorldClient;
import mccompat.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import mccompat.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.mccompat.v1_7_10.client.ClientGui;

/** {@code Minecraft.getMinecraft()} as a view over the modern client. Changing fields are phantoms with accessors. */
public class Minecraft {
    private static final Minecraft INSTANCE = new Minecraft();

    @Deprecated
    public WorldClient theWorld;
    @Deprecated
    public EntityClientPlayerMP thePlayer;
    @Deprecated
    public GuiScreen currentScreen;
    @Deprecated
    public int displayWidth;
    @Deprecated
    public int displayHeight;
    public TextureManager renderEngine = TextureManager.INSTANCE;
    public FontRenderer fontRenderer = FontRenderer.INSTANCE;
    public FontRenderer standardGalacticFontRenderer = FontRenderer.INSTANCE;
    public final mccompat.v1_7_10.net.minecraft.client.particle.EffectRenderer effectRenderer =
            new mccompat.v1_7_10.net.minecraft.client.particle.EffectRenderer(null, TextureManager.INSTANCE);

    public static Minecraft getMinecraft() {
        return INSTANCE;
    }

    private static net.minecraft.client.Minecraft mc() {
        return net.minecraft.client.Minecraft.getInstance();
    }

    public static WorldClient mcc$get$theWorld(Minecraft m) {
        var level = mc().level;
        return level == null ? null : (WorldClient) World.mcc$wrap(level);
    }

    public static EntityClientPlayerMP mcc$get$thePlayer(Minecraft m) {
        var p = mc().player;
        return p == null ? null : (EntityClientPlayerMP) EntityBridge.wrap(p);
    }

    public static GuiScreen mcc$get$currentScreen(Minecraft m) {
        return ClientGui.currentLegacyScreen();
    }

    public static int mcc$get$displayWidth(Minecraft m) {
        return mc().getWindow().getWidth();
    }

    public static int mcc$get$displayHeight(Minecraft m) {
        return mc().getWindow().getHeight();
    }

    public TextureManager getTextureManager() {
        return renderEngine;
    }

    public void displayGuiScreen(GuiScreen screen) {
        ClientGui.display(screen);
    }

    public boolean isSingleplayer() {
        return mc().isSingleplayer();
    }

    public boolean isGamePaused() {
        return mc().isPaused();
    }

    public static boolean isGuiEnabled() {
        return !mc().options.hideGui;
    }

    public static boolean isFancyGraphicsEnabled() {
        return mc().options.graphicsMode().get() != net.minecraft.client.GraphicsStatus.FAST;
    }

    public static long getSystemTime() {
        return System.currentTimeMillis();
    }
}
