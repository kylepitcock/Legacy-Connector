package legacyconnector.v1_12_2.net.minecraft.client;

import legacyconnector.v1_12_2.net.minecraft.client.entity.EntityPlayerSP;
import legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.client.multiplayer.WorldClient;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IReloadableResourceManager;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122;

/**
 * The client singleton as 1.12 mods see it: a view over the modern client. Fields that change (player, world, open screen, window size)
 * are phantoms answered by their {@code mcc$get$} accessors.
 */
public class Minecraft implements legacyconnector.v1_12_2.net.minecraft.util.IThreadListener {
    /**
     * Whether this is a client. Client-only mods call {@link #getMinecraft()} on dedicated servers too (CTM's preInit); there the
     * client singletons below stay null, since loading them would load modern client classes.
     */
    private static final boolean CLIENT = net.neoforged.fml.loading.FMLEnvironment.dist.isClient();
    private static final Minecraft INSTANCE = new Minecraft();

    public static final boolean IS_RUNNING_ON_MAC = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("mac");

    @Deprecated
    public EntityPlayerSP player;
    @Deprecated
    public WorldClient world;
    @Deprecated
    public GuiScreen currentScreen;
    @Deprecated
    public int displayWidth;
    @Deprecated
    public int displayHeight;
    @Deprecated
    public GameSettings gameSettings;
    /** Phantom: true while playing with the mouse captured. */
    @Deprecated
    public boolean inGameHasFocus;
    /** Phantom: the entity under the crosshair. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.entity.Entity pointedEntity;

    public FontRenderer fontRenderer = CLIENT ? FontRenderer.mcc$INSTANCE : null;
    public FontRenderer standardGalacticFontRenderer = CLIENT ? FontRenderer.mcc$INSTANCE : null;
    /** The mods' particles (ticked and drawn by the layer). */
    public final legacyconnector.v1_12_2.net.minecraft.client.particle.ParticleManager effectRenderer = CLIENT ? new legacyconnector.v1_12_2.net.minecraft.client.particle.ParticleManager() : null;
    public legacyconnector.v1_12_2.net.minecraft.client.multiplayer.PlayerControllerMP playerController =
            CLIENT ? legacyconnector.v1_12_2.net.minecraft.client.multiplayer.PlayerControllerMP.mcc$INSTANCE : null;
    public final legacyconnector.v1_12_2.net.minecraft.client.gui.GuiIngame ingameGUI = CLIENT ? legacyconnector.v1_12_2.net.minecraft.client.gui.GuiIngame.mcc$INSTANCE : null;

    public legacyconnector.v1_12_2.net.minecraft.client.resources.LanguageManager getLanguageManager() {
        return legacyconnector.v1_12_2.net.minecraft.client.resources.LanguageManager.mcc$INSTANCE;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.gui.toasts.GuiToast getToastGui() {
        return legacyconnector.v1_12_2.net.minecraft.client.gui.toasts.GuiToast.mcc$INSTANCE;
    }

    /** The game directory (mods build paths from it). */
    public final java.io.File gameDir = net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().toFile();

    private final IReloadableResourceManager resourceManager = new legacyconnector.v1_12_2.net.minecraft.client.resources.SimpleReloadableResourceManager();

    public static Minecraft getMinecraft() {
        return INSTANCE;
    }

    private static net.minecraft.client.Minecraft mc() {
        return net.minecraft.client.Minecraft.getInstance();
    }

    public IResourceManager getResourceManager() {
        return resourceManager;
    }

    public boolean isCallingFromMinecraftThread() {
        return !net.neoforged.fml.loading.FMLEnvironment.dist.isClient() || mc().isSameThread();
    }

    public com.google.common.util.concurrent.ListenableFuture<Object> addScheduledTask(Runnable task) {
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            mc().execute(task);
        } else {
            task.run();
        }
        return com.google.common.util.concurrent.Futures.immediateFuture(null);
    }

    /** Runs the task on the client thread; the future completes with its result. */
    public <V> com.google.common.util.concurrent.ListenableFuture<V> addScheduledTask(java.util.concurrent.Callable<V> task) {
        com.google.common.util.concurrent.SettableFuture<V> future = com.google.common.util.concurrent.SettableFuture.create();
        Runnable run = () -> {
            try {
                future.set(task.call());
            } catch (Exception e) {
                future.setException(e);
            }
        };
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient() && !mc().isSameThread()) mc().execute(run);
        else run.run();
        return future;
    }

    /** True unless connected to a remote server. */
    public boolean isSingleplayer() {
        return !net.neoforged.fml.loading.FMLEnvironment.dist.isClient() || mc().hasSingleplayerServer();
    }

    public boolean isIntegratedServerRunning() {
        return net.neoforged.fml.loading.FMLEnvironment.dist.isClient() && mc().hasSingleplayerServer();
    }

    public static long getSystemTime() {
        return System.currentTimeMillis();
    }

    // ------------------------------------------------------------------ screens and focus

    /** Shows a legacy screen in a modern one (null closes the screen). */
    public void displayGuiScreen(GuiScreen screen) {
        org.kylepitcock.legacyconnector.v1_12_2.client.ClientGui1122.display(screen);
    }

    public void setIngameFocus() {
        if (mc().screen == null) mc().mouseHandler.grabMouse();
    }

    public void setIngameNotInFocus() {
        mc().mouseHandler.releaseMouse();
    }

    public boolean isGamePaused() {
        return mc().isPaused();
    }

    public float getRenderPartialTicks() {
        return mc().getTimer().getGameTimeDeltaPartialTick(false);
    }

    public static boolean isAmbientOcclusionEnabled() {
        return mc().options.ambientOcclusion().get();
    }

    public static boolean isFancyGraphicsEnabled() {
        return mc().options.graphicsMode().get() != net.minecraft.client.GraphicsStatus.FAST;
    }

    public static boolean isGuiEnabled() {
        return !mc().options.hideGui;
    }

    public boolean isUnicode() {
        return mc().options.forceUnicodeFont().get();
    }

    public boolean isSnooperEnabled() {
        return false;
    }

    public boolean isReducedDebug() {
        return mc().showOnlyReducedInfo();
    }

    public static int getDebugFPS() {
        return mc().getFps();
    }

    public java.net.Proxy getProxy() {
        return java.net.Proxy.NO_PROXY;
    }

    public com.mojang.authlib.minecraft.MinecraftSessionService getSessionService() {
        return mc().getMinecraftSessionService();
    }

    /** Closes the game as the player would. */
    public void shutdown() {
        mc().stop();
    }

    public legacyconnector.v1_12_2.net.minecraft.client.audio.SoundHandler getSoundHandler() {
        return legacyconnector.v1_12_2.net.minecraft.client.audio.SoundHandler.mcc$INSTANCE;
    }

    /** The entity the camera follows (the local player unless spectating or a mod changed it). */
    public legacyconnector.v1_12_2.net.minecraft.entity.Entity getRenderViewEntity() {
        var camera = mc().getCameraEntity();
        return camera == null ? null : EntityBridge1122.wrap(camera);
    }

    public void setRenderViewEntity(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        var modern = entity == null ? null : entity.mcc$entity;
        mc().setCameraEntity(modern != null ? modern : ClientAccess.player());
    }

    /**
     * Reads of the modern client's player and level, kept out of this class: verifying it would load their client classes, and
     * client-only mods call {@link #getMinecraft()} on dedicated servers too (CTM's preInit).
     */
    private static final class ClientAccess {
        static net.minecraft.world.entity.Entity player() {
            return mc().player;
        }

        static net.minecraft.world.level.Level level() {
            return mc().level;
        }
    }

    // ------------------------------------------------------------------ phantom fields

    public static EntityPlayerSP mcc$get$player(Minecraft m) {
        var p = ClientAccess.player();
        return p == null ? null : EntityBridge1122.wrap(p) instanceof EntityPlayerSP sp ? sp : null;
    }

    public static WorldClient mcc$get$world(Minecraft m) {
        var level = ClientAccess.level();
        return level == null ? null : WorldBridge1122.wrap(level) instanceof WorldClient w ? w : null;
    }

    public static GuiScreen mcc$get$currentScreen(Minecraft m) {
        return org.kylepitcock.legacyconnector.v1_12_2.client.ClientGui1122.currentLegacyScreen();
    }

    /** Mods assign the field to show a screen without the open/close callbacks; showing it properly is the closest. */
    public static void mcc$set$currentScreen(Minecraft m, GuiScreen screen) {
        org.kylepitcock.legacyconnector.v1_12_2.client.ClientGui1122.display(screen);
    }

    public static int mcc$get$displayWidth(Minecraft m) {
        return mc().getWindow().getWidth();
    }

    public static int mcc$get$displayHeight(Minecraft m) {
        return mc().getWindow().getHeight();
    }

    private static GameSettings settings;

    public static synchronized GameSettings mcc$get$gameSettings(Minecraft m) {
        if (settings == null) settings = new GameSettings();
        return settings;
    }

    public static boolean mcc$get$inGameHasFocus(Minecraft m) {
        return mc().screen == null && mc().mouseHandler.isMouseGrabbed();
    }

    public static void mcc$set$inGameHasFocus(Minecraft m, boolean focus) {
        // the game owns the mouse grab; setIngameFocus/setIngameNotInFocus do the switching
    }

    public static legacyconnector.v1_12_2.net.minecraft.entity.Entity mcc$get$pointedEntity(Minecraft m) {
        var e = mc().crosshairPickEntity;
        return e == null ? null : EntityBridge1122.wrap(e);
    }

    // ------------------------------------------------------------------ rendering objects (area R: client.renderer)

    public final legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager renderEngine =
            CLIENT ? new legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager(getResourceManager()) : null;

    public legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager getTextureManager() {
        return renderEngine;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap getTextureMapBlocks() {
        return legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap.mcc$blocks();
    }

    // ------------------------------------------------------------------ the rest of the client (area R2b)

    private static boolean client() {
        return net.neoforged.fml.loading.FMLEnvironment.dist.isClient();
    }

    /** The client's singletons below are null on a dedicated server (client-only mods call {@code getMinecraft()} there too). */
    public legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem getRenderItem() {
        return CLIENT ? legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem.mcc$instance() : null;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.renderer.BlockRendererDispatcher getBlockRendererDispatcher() {
        return CLIENT ? legacyconnector.v1_12_2.net.minecraft.client.renderer.BlockRendererDispatcher.mcc$instance() : null;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.renderer.color.BlockColors getBlockColors() {
        return CLIENT ? legacyconnector.v1_12_2.net.minecraft.client.renderer.color.BlockColors.mcc$instance() : null;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.renderer.color.ItemColors getItemColors() {
        return CLIENT ? legacyconnector.v1_12_2.net.minecraft.client.renderer.color.ItemColors.mcc$instance() : null;
    }

    private legacyconnector.v1_12_2.net.minecraft.client.renderer.EntityRenderer entityRendererObject;
    private legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderGlobal renderGlobalObject;

    public legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderManager getRenderManager() {
        return CLIENT ? legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderManager.mcc$instance() : null;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.renderer.ItemRenderer getItemRenderer() {
        return CLIENT ? legacyconnector.v1_12_2.net.minecraft.client.renderer.ItemRenderer.mcc$instance() : null;
    }

    /** Phantom: the game renderer (camera, fog, light map); mods may replace it. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.client.renderer.EntityRenderer entityRenderer;

    public static synchronized legacyconnector.v1_12_2.net.minecraft.client.renderer.EntityRenderer mcc$get$entityRenderer(Minecraft m) {
        if (m.entityRendererObject == null && CLIENT) m.entityRendererObject = legacyconnector.v1_12_2.net.minecraft.client.renderer.EntityRenderer.mcc$instance();
        return m.entityRendererObject;
    }

    public static synchronized void mcc$set$entityRenderer(Minecraft m, legacyconnector.v1_12_2.net.minecraft.client.renderer.EntityRenderer value) {
        m.entityRendererObject = value;
    }

    /** Phantom: the world renderer; made on first use, mods may replace it. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderGlobal renderGlobal;

    public static synchronized legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderGlobal mcc$get$renderGlobal(Minecraft m) {
        if (m.renderGlobalObject == null && CLIENT) m.renderGlobalObject = legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderGlobal.mcc$instance();
        return m.renderGlobalObject;
    }

    public static synchronized void mcc$set$renderGlobal(Minecraft m, legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderGlobal value) {
        m.renderGlobalObject = value;
    }

    /** Phantom: the main framebuffer (see {@link #getFramebuffer}). */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.client.shader.Framebuffer framebuffer;

    public static legacyconnector.v1_12_2.net.minecraft.client.shader.Framebuffer mcc$get$framebuffer(Minecraft m) {
        return m.getFramebuffer();
    }

    /** The main framebuffer the world and screens draw into. */
    public legacyconnector.v1_12_2.net.minecraft.client.shader.Framebuffer getFramebuffer() {
        return CLIENT ? legacyconnector.v1_12_2.net.minecraft.client.shader.Framebuffer.mcc$main() : null;
    }

    /** Phantom: what the crosshair is on. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult objectMouseOver;

    public static legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult mcc$get$objectMouseOver(Minecraft m) {
        return client() ? org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.objectMouseOver() : null;
    }

    public static void mcc$set$objectMouseOver(Minecraft m, legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult value) {
        if (client()) org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.setObjectMouseOver(value);
    }

    /** The connection to the server (null when not in a world). */
    public legacyconnector.v1_12_2.net.minecraft.client.network.NetHandlerPlayClient getConnection() {
        return client() && org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.connected()
                ? legacyconnector.v1_12_2.net.minecraft.client.network.NetHandlerPlayClient.mcc$INSTANCE : null;
    }

    private legacyconnector.v1_12_2.net.minecraft.server.integrated.IntegratedServer integratedServer;

    /** The single player server while one runs (a view answering from it), else null. */
    public synchronized legacyconnector.v1_12_2.net.minecraft.server.integrated.IntegratedServer getIntegratedServer() {
        if (!isIntegratedServerRunning()) return null;
        if (integratedServer == null) {
            integratedServer = org.kylepitcock.legacyconnector.v1_12_2.client.ClientObjects1122.make(
                    legacyconnector.v1_12_2.net.minecraft.server.integrated.IntegratedServer.class);
        }
        return integratedServer;
    }

    private legacyconnector.v1_12_2.net.minecraft.util.Session session;

    /** The signed-in account. */
    public synchronized legacyconnector.v1_12_2.net.minecraft.util.Session getSession() {
        if (session == null) {
            session = client() ? org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.session()
                    : new legacyconnector.v1_12_2.net.minecraft.util.Session("Player", "", "", "legacy");
        }
        return session;
    }

    /** The multiplayer server played on (null in single player). */
    public legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData getCurrentServerData() {
        return client() ? org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.currentServerData() : null;
    }

    /** The client's section profiler (records nothing until enabled). */
    public final legacyconnector.v1_12_2.net.minecraft.profiler.Profiler profiler = new legacyconnector.v1_12_2.net.minecraft.profiler.Profiler();

    private boolean connectedToRealms;

    /** Realms is not supported: never connected unless a mod says so. */
    public boolean isConnectedToRealms() {
        return connectedToRealms;
    }

    public void setConnectedToRealms(boolean value) {
        this.connectedToRealms = value;
    }

    public final legacyconnector.v1_12_2.net.minecraft.client.resources.data.MetadataSerializer metadataSerializer =
            new legacyconnector.v1_12_2.net.minecraft.client.resources.data.MetadataSerializer();
    public final java.io.File mcDataDir = gameDir;
    public final java.io.File fileResourcepacks = new java.io.File(gameDir, "resourcepacks");
    public final legacyconnector.v1_12_2.net.minecraft.client.resources.LanguageManager languageManager = !CLIENT ? null :
            legacyconnector.v1_12_2.net.minecraft.client.resources.LanguageManager.mcc$INSTANCE;
    /** The game's own assets as a legacy pack. */
    public final legacyconnector.v1_12_2.net.minecraft.client.resources.DefaultResourcePack defaultResourcePack = CLIENT ? new legacyconnector.v1_12_2.net.minecraft.client.resources.DefaultResourcePack() : null;
    /** Packs mods add by reflection, then {@code refreshResources()}: they are served to legacy resource lookups. */
    private final java.util.List<legacyconnector.v1_12_2.net.minecraft.client.resources.IResourcePack> defaultResourcePacks = new java.util.ArrayList<>();
    public final legacyconnector.v1_12_2.net.minecraft.client.resources.ResourcePackRepository resourcePackRepository =
            new legacyconnector.v1_12_2.net.minecraft.client.resources.ResourcePackRepository(fileResourcepacks,
                    new java.io.File(gameDir, "server-resource-packs"), null, metadataSerializer, null);

    public legacyconnector.v1_12_2.net.minecraft.client.resources.ResourcePackRepository getResourcePackRepository() {
        return resourcePackRepository;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.resources.SkinManager getSkinManager() {
        return legacyconnector.v1_12_2.net.minecraft.client.resources.SkinManager.mcc$instance();
    }

    /** Reloads the resources; packs mods put into {@code defaultResourcePacks} are added to the legacy lookups first. */
    public void refreshResources() {
        if (resourceManager instanceof legacyconnector.v1_12_2.net.minecraft.client.resources.SimpleReloadableResourceManager m) {
            synchronized (defaultResourcePacks) {
                for (var pack : defaultResourcePacks) if (!m.mcc$has(pack)) m.reloadResourcePack(pack);
            }
        }
        if (client()) org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.refreshResources();
    }

    /** Re-applies the window size to the game (the window itself belongs to the game). */
    public void resize(int width, int height) {
        if (client()) org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.resize();
    }

    /** Crashes the game with the report (on the next frame, as 1.12 did). */
    public void crashed(legacyconnector.v1_12_2.net.minecraft.crash.CrashReport report) {
        if (client()) org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.crash(report);
    }

    public void displayCrashReport(legacyconnector.v1_12_2.net.minecraft.crash.CrashReport report) {
        crashed(report);
    }

    public legacyconnector.v1_12_2.net.minecraft.crash.CrashReport addGraphicsAndWorldToCrashReport(legacyconnector.v1_12_2.net.minecraft.crash.CrashReport report) {
        return report;
    }

    private legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker mcMusicTicker;

    public synchronized legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker getMusicTicker() {
        if (mcMusicTicker == null) mcMusicTicker = new legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker(this);
        return mcMusicTicker;
    }

    /** The music ticker if a mod replaced it with its own (by reflection), else null (not a 1.12 method). */
    public synchronized legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker mcc$replacedMusicTicker() {
        var t = mcMusicTicker;
        return t == null || t.getClass() == legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker.class ? null : t;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker.MusicType getAmbientMusicType() {
        return client() ? org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.ambientMusicType()
                : legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker.MusicType.MENU;
    }

    /** Opens a saved world by folder name (main menu mods' "continue" buttons); a missing world opens the world creation screen. */
    public void launchIntegratedServer(String folderName, String worldName, legacyconnector.v1_12_2.net.minecraft.world.WorldSettings settings) {
        if (client()) org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.launchIntegratedServer(folderName);
    }

    /** Phantom: 1.12 skipped drawing the world while one loaded; the game decides that now. */
    @Deprecated
    public boolean skipRenderWorld;

    public static boolean mcc$get$skipRenderWorld(Minecraft m) {
        return false;
    }

    /** 1.12 swapped the window buffers here (mods drew their own loading progress); the game presents its frames itself. */
    public void updateDisplay() {
    }

    /** 1.12 checked the global keys (F1, F2, F3, F11) after a screen took a key; the game handles them itself. */
    public void dispatchKeypresses() {
    }

    public legacyconnector.v1_12_2.net.minecraft.util.datafix.DataFixer getDataFixer() {
        return legacyconnector.v1_12_2.net.minecraft.util.datafix.DataFixesManager.getDataFixer();
    }

    /** The ticks the last frame advanced. */
    public float getTickLength() {
        return client() ? org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.tickLength() : 1.0F;
    }

    public String getVersion() {
        return "1.12.2";
    }

    public String getVersionType() {
        return client() ? org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.versionType() : "release";
    }

    public boolean isDemo() {
        return client() && org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.isDemo();
    }

    /** {@code loadWorld(null)} leaves the world (back to the title screen); a client world cannot be swapped in. */
    public void loadWorld(WorldClient world) {
        if (world == null && client()) org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.leaveWorld();
    }

    public void loadWorld(WorldClient world, String loadingMessage) {
        loadWorld(world);
    }

    public LoadingScreenRenderer loadingScreen = CLIENT ? new LoadingScreenRenderer(this) : null;

    /** Phantom: the model manager. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelManager modelManager;

    public static legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelManager mcc$get$modelManager(Minecraft m) {
        return legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelManager.mcc$instance();
    }

    /** Phantom: the partial tick frozen while the game is paused. */
    @Deprecated
    public float renderPartialTicksPaused;

    public static float mcc$get$renderPartialTicksPaused(Minecraft m) {
        return client() ? org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.pausedPartialTicks() : 0.0F;
    }

    /** Phantom: whether the window is full screen. */
    @Deprecated
    public boolean fullscreen;

    public static boolean mcc$get$fullscreen(Minecraft m) {
        return client() && org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.fullscreen();
    }

    public boolean isFullScreen() {
        return mcc$get$fullscreen(this);
    }

    public void toggleFullscreen() {
        if (client()) org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.toggleFullscreen();
    }

    public static int getGLMaximumTextureSize() {
        return org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.maxTextureSize();
    }

    /** Phantom: chunks drawn in many passes (always). */
    @Deprecated
    public boolean renderChunksMany;

    public static boolean mcc$get$renderChunksMany(Minecraft m) {
        return true;
    }
}
