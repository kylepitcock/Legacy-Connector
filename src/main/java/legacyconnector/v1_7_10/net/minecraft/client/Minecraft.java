package legacyconnector.v1_7_10.net.minecraft.client;

import legacyconnector.v1_7_10.net.minecraft.client.entity.EntityClientPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.client.multiplayer.WorldClient;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.legacyconnector.v1_7_10.client.ClientGui;

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
    /** The game directory (mods build paths from it). */
    public final java.io.File mcDataDir = net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().toFile();
    /** Read reflectively by mods that add a resource pack (the modern game loads packs itself, so adding is a no-op). */
    private java.util.List defaultResourcePacks = new java.util.ArrayList();
    public final legacyconnector.v1_7_10.net.minecraft.client.gui.GuiIngame ingameGUI = new legacyconnector.v1_7_10.net.minecraft.client.gui.GuiIngame();
    public final legacyconnector.v1_7_10.net.minecraft.client.multiplayer.PlayerControllerMP playerController = new legacyconnector.v1_7_10.net.minecraft.client.multiplayer.PlayerControllerMP();
    public legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal renderGlobal = new legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal();
    public FontRenderer fontRenderer = FontRenderer.INSTANCE;
    public FontRenderer standardGalacticFontRenderer = FontRenderer.INSTANCE;
    public final legacyconnector.v1_7_10.net.minecraft.profiler.Profiler mcProfiler = legacyconnector.v1_7_10.net.minecraft.profiler.Profiler.INSTANCE;
    public final legacyconnector.v1_7_10.net.minecraft.client.particle.EffectRenderer effectRenderer =
            new legacyconnector.v1_7_10.net.minecraft.client.particle.EffectRenderer(null, TextureManager.INSTANCE);

    private static legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager resources;

    /** The mods' resources (legacy mods read their own lang files, structures and models through this). */
    public legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager getResourceManager() {
        if (resources == null) resources = new org.kylepitcock.legacyconnector.v1_7_10.client.LegacySprites.ModResources();
        return resources;
    }

    public legacyconnector.v1_7_10.net.minecraft.client.renderer.EntityRenderer entityRenderer =
            new legacyconnector.v1_7_10.net.minecraft.client.renderer.EntityRenderer();

    private legacyconnector.v1_7_10.net.minecraft.util.Session session;

    public legacyconnector.v1_7_10.net.minecraft.util.Session getSession() {
        if (session == null) session = legacyconnector.v1_7_10.net.minecraft.util.Session.mcc$current();
        return session;
    }

    private legacyconnector.v1_7_10.net.minecraft.client.resources.LanguageManager languageManager;

    public legacyconnector.v1_7_10.net.minecraft.client.resources.LanguageManager getLanguageManager() {
        if (languageManager == null) languageManager = new legacyconnector.v1_7_10.net.minecraft.client.resources.LanguageManager();
        return languageManager;
    }

    public boolean isAmbientOcclusionEnabled() {
        return true;
    }

    public legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap getTextureMapBlocks() {
        return org.kylepitcock.legacyconnector.v1_7_10.Assets1710.blockAtlas();
    }

    private static final legacyconnector.v1_7_10.net.minecraft.client.shader.Framebuffer FRAMEBUFFER = new legacyconnector.v1_7_10.net.minecraft.client.shader.Framebuffer();

    public legacyconnector.v1_7_10.net.minecraft.client.shader.Framebuffer getFramebuffer() {
        var target = mc().getMainRenderTarget();
        FRAMEBUFFER.framebufferWidth = FRAMEBUFFER.framebufferTextureWidth = target.width;
        FRAMEBUFFER.framebufferHeight = FRAMEBUFFER.framebufferTextureHeight = target.height;
        return FRAMEBUFFER;
    }

    public legacyconnector.v1_7_10.net.minecraft.client.resources.ResourcePackRepository getResourcePackRepository() {
        return legacyconnector.v1_7_10.net.minecraft.client.resources.ResourcePackRepository.INSTANCE;
    }

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

    /** The entity the camera follows (the local player unless a mod/spectating changed it). */
    public static legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase mcc$get$renderViewEntity(Minecraft m) {
        var camera = mc().getCameraEntity();
        return camera == null ? null : (legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase) EntityBridge.wrap(camera);
    }

    private static final legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings SETTINGS =
            new legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings();

    public static legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings mcc$get$gameSettings(Minecraft m) {
        return SETTINGS;
    }

    public static int mcc$get$displayWidth(Minecraft m) {
        return mc().getWindow().getWidth();
    }

    public static int mcc$get$displayHeight(Minecraft m) {
        return mc().getWindow().getHeight();
    }

    public legacyconnector.v1_7_10.net.minecraft.client.audio.SoundHandler getSoundHandler() {
        return legacyconnector.v1_7_10.net.minecraft.client.audio.SoundHandler.INSTANCE;
    }

    public com.mojang.authlib.minecraft.MinecraftSessionService getSessionService() {
        return mc().getMinecraftSessionService();
    }

    public legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient getNetHandler() {
        return mc().getConnection() == null ? null : legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient.INSTANCE;
    }

    /** Captures the mouse for the game (as closing a screen does). */
    public void setIngameFocus() {
        if (mc().screen == null) mc().mouseHandler.grabMouse();
    }

    public void setIngameNotInFocus() {
        mc().mouseHandler.releaseMouse();
    }

    public boolean isIntegratedServerRunning() {
        return mc().hasSingleplayerServer();
    }

    public void refreshResources() {
        mc().reloadResourcePacks();
    }

    public void shutdown() {
        mc().stop();
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
