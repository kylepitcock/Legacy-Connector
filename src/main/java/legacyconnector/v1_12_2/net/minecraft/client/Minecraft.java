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
public class Minecraft {
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

    public FontRenderer fontRenderer = FontRenderer.mcc$INSTANCE;
    public FontRenderer standardGalacticFontRenderer = FontRenderer.mcc$INSTANCE;
    /** The mods' particles (ticked and drawn by the layer). */
    public final legacyconnector.v1_12_2.net.minecraft.client.particle.ParticleManager effectRenderer = new legacyconnector.v1_12_2.net.minecraft.client.particle.ParticleManager();
    public legacyconnector.v1_12_2.net.minecraft.client.multiplayer.PlayerControllerMP playerController =
            legacyconnector.v1_12_2.net.minecraft.client.multiplayer.PlayerControllerMP.mcc$INSTANCE;
    public final legacyconnector.v1_12_2.net.minecraft.client.gui.GuiIngame ingameGUI = legacyconnector.v1_12_2.net.minecraft.client.gui.GuiIngame.mcc$INSTANCE;

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
            new legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager(getResourceManager());

    public legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager getTextureManager() {
        return renderEngine;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap getTextureMapBlocks() {
        return legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap.mcc$blocks();
    }
}
