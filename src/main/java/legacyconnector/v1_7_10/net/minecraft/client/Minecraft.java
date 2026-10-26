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
    /** Phantom: what the crosshair is on (block or entity), null when nothing. */
    @Deprecated
    public legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition objectMouseOver;
    /** Phantom: the entity under the crosshair. */
    @Deprecated
    public legacyconnector.v1_7_10.net.minecraft.entity.Entity pointedEntity;
    /** Phantom: true while playing with the mouse captured (key handlers check it: Thaumcraft's wand focus key). */
    @Deprecated
    public boolean inGameHasFocus;
    /** 1.7.10's frames-per-second counter. */
    public static int debugFPS;
    public TextureManager renderEngine = TextureManager.INSTANCE;
    /** The frame timer (obfuscated name field_71428_T): its partial tick is set by the layer each frame. */
    public legacyconnector.v1_7_10.net.minecraft.util.Timer timer = legacyconnector.v1_7_10.net.minecraft.util.Timer.INSTANCE;
    public legacyconnector.v1_7_10.net.minecraft.util.Timer field_71428_T = legacyconnector.v1_7_10.net.minecraft.util.Timer.INSTANCE;
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

    /** Static in 1.7.10 (block renderers ask it while choosing smooth lighting): the modern smooth lighting option. */
    public static boolean isAmbientOcclusionEnabled() {
        return net.minecraft.client.Minecraft.getInstance().options.ambientOcclusion().get();
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

    /** Whether this is the demo version of the game (it never is). */
    public boolean isDemo() {
        return false;
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

    /** Mods move the camera by setting it (Thermal Dynamics' duct transport). */
    public static void mcc$set$renderViewEntity(Minecraft m, legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity) {
        var modern = entity == null ? null : entity.mcc$entity;
        mc().setCameraEntity(modern != null ? modern : mc().player);
    }

    private static final legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings SETTINGS =
            new legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings();

    public static legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings mcc$get$gameSettings(Minecraft m) {
        return SETTINGS;
    }

    private static final legacyconnector.v1_7_10.net.minecraft.util.MouseHelper MOUSE_HELPER = new legacyconnector.v1_7_10.net.minecraft.util.MouseHelper();

    public static legacyconnector.v1_7_10.net.minecraft.util.MouseHelper mcc$get$mouseHelper(Minecraft m) {
        return MOUSE_HELPER;
    }

    public static legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition mcc$get$objectMouseOver(Minecraft m) {
        var hit = mc().hitResult;
        if (hit == null || hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return null;
        var at = hit.getLocation();
        var vec = legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(at.x, at.y, at.z);
        if (hit instanceof net.minecraft.world.phys.EntityHitResult e) {
            return new legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition(EntityBridge.wrap(e.getEntity()), vec);
        }
        var b = (net.minecraft.world.phys.BlockHitResult) hit;
        return new legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition(b.getBlockPos().getX(), b.getBlockPos().getY(), b.getBlockPos().getZ(),
                b.getDirection().get3DDataValue(), vec);
    }

    public static legacyconnector.v1_7_10.net.minecraft.entity.Entity mcc$get$pointedEntity(Minecraft m) {
        var e = mc().crosshairPickEntity;
        return e == null ? null : EntityBridge.wrap(e);
    }

    public static boolean mcc$get$inGameHasFocus(Minecraft m) {
        return mc().screen == null && mc().mouseHandler.isMouseGrabbed();
    }

    public static void mcc$set$inGameHasFocus(Minecraft m, boolean focus) {
        // the game owns the mouse grab; setIngameFocus/setIngameNotInFocus do the switching
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

    public legacyconnector.v1_7_10.net.minecraft.server.integrated.IntegratedServer getIntegratedServer() {
        return (legacyconnector.v1_7_10.net.minecraft.server.integrated.IntegratedServer)
                legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer.mcc$wrap(mc().getSingleplayerServer());
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

    /**
     * Starts the integrated server on a saved world (FML's old-save prompt, main menu mods' "load world" buttons). A folder
     * without a world opens the modern world creation screen, which takes over choosing the settings.
     */
    public void launchIntegratedServer(String folder, String worldName, legacyconnector.v1_7_10.net.minecraft.world.WorldSettings settings) {
        var mc = mc();
        if (getSaveLoader().canLoadWorld(folder)) {
            mc.createWorldOpenFlows().openWorld(folder, () -> mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen()));
        } else {
            net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.openFresh(mc, mc.screen);
        }
    }

    private legacyconnector.v1_7_10.net.minecraft.world.storage.ISaveFormat saveLoader;

    /** The saves folder (1.7.10's AnvilSaveConverter); worlds are addressed by folder name. */
    public legacyconnector.v1_7_10.net.minecraft.world.storage.ISaveFormat getSaveLoader() {
        if (saveLoader == null) saveLoader = new legacyconnector.v1_7_10.net.minecraft.world.storage.SaveFormatOld(new java.io.File(mcDataDir, "saves"));
        return saveLoader;
    }

    private legacyconnector.v1_7_10.net.minecraft.client.multiplayer.ServerData serverData;
    private net.minecraft.client.multiplayer.ServerData serverDataSource;

    /** The multiplayer server the client is connected to (null in single player), as a legacy server entry. */
    public legacyconnector.v1_7_10.net.minecraft.client.multiplayer.ServerData getCurrentServerData() {
        var modern = mc().getCurrentServer();
        if (modern == null) return null;
        if (modern != serverDataSource || serverData == null) {
            serverDataSource = modern;
            serverData = new legacyconnector.v1_7_10.net.minecraft.client.multiplayer.ServerData(modern.name, modern.ip);
        }
        serverData.serverMOTD = modern.motd == null ? null : modern.motd.getString();
        serverData.pingToServer = modern.ping;
        serverData.populationInfo = modern.status == null ? null : modern.status.getString();
        return serverData;
    }

    public java.net.Proxy getProxy() {
        return mc().getProxy();
    }

    /** The largest texture side the graphics driver takes (1.7.10 probed it with proxy textures). */
    public static int getGLMaximumTextureSize() {
        return com.mojang.blaze3d.systems.RenderSystem.maxSupportedTextureSize();
    }

    /** Twitch broadcasting is gone: the stream is always the inactive null stream. */
    public legacyconnector.v1_7_10.net.minecraft.client.stream.IStream getTwitchStream() {
        return legacyconnector.v1_7_10.net.minecraft.client.stream.NullStream.INSTANCE;
    }

    /** 1.7.10 checked the global keys (F1, F2 screenshot, F3, F11) after a screen took a key; the modern game handles them itself. */
    public void dispatchKeypresses() {
    }

    /** Re-applies the window size to the game (after a mod changed the display mode). */
    public void resetSize() {
        mc().resizeDisplay();
    }

    /** {@code loadWorld(null)} leaves the world (back to the title screen); a client world cannot be swapped in. */
    public void loadWorld(WorldClient world) {
        if (world != null) return;
        var mc = mc();
        if (mc.level != null) mc.level.disconnect();
        mc.disconnect(new net.minecraft.client.gui.screens.TitleScreen());
    }

    public void loadWorld(WorldClient world, String loadingMessage) {
        loadWorld(world);
    }
}
