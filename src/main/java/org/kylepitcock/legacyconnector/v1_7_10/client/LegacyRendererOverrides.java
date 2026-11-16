package org.kylepitcock.legacyconnector.v1_7_10.client;

import legacyconnector.v1_7_10.net.minecraft.client.multiplayer.PlayerControllerMP;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.EntityRenderer;
import org.kylepitcock.legacyconnector.core.LegacyMods;

/**
 * Legacy mods that REPLACED the game's objects with subclasses of their own ({@code mc.entityRenderer = new MyEntityRenderer(mc)},
 * {@code mc.playerController = new MyPlayerControllerMP(...)}): the modern game draws and picks with its own classes, so the overridden methods never ran.
 * This runs them where 1.7.10 called them, with the modern code as their {@code super} call:
 * <ul>
 * <li>{@code updateCameraAndRender(partial)}: runs once per frame around the whole modern frame ({@link #frame}); the shim's own method is the "super" call and
 * yields into the modern frame, so everything the override sets up before {@code super} (the eye height of the player, field of view...) holds while it is
 * drawn and what it undoes after is undone after;</li>
 * <li>{@code getBlockReachDistance()}: the client reach of the player ({@link #blockReach});</li>
 * <li>the eye height ({@code yOffset} over 1.62) and {@code thirdPersonDistance} the override left in the legacy objects shift and move the camera
 * ({@link #cameraRaise}, {@link #thirdPersonDistance}).</li>
 * </ul>
 * Without a subclass of those classes nothing here does anything.
 */
public final class LegacyRendererOverrides {
    private static volatile Boolean legacyPresent;
    private static int failures;
    private static final ThreadLocal<Runnable> CONTINUATION = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IN_REACH = new ThreadLocal<>();

    private LegacyRendererOverrides() {}

    private static boolean present() {
        Boolean p = legacyPresent;
        if (p == null) {
            boolean any = false;
            for (var mod : LegacyMods.all()) any |= "1.7.10".equals(LegacyMods.family(mod.version));
            legacyPresent = p = any;
        }
        return p;
    }

    private static legacyconnector.v1_7_10.net.minecraft.client.Minecraft legacyMc() {
        return legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft();
    }

    /** Whether the renderer object is a mod's subclass (it replaced the game's). */
    private static EntityRenderer overriddenRenderer() {
        if (!present()) return null;
        var mc = legacyMc();
        var renderer = mc == null ? null : mc.entityRenderer;
        return renderer != null && renderer.getClass() != EntityRenderer.class ? renderer : null;
    }

    /**
     * One frame of the modern game, wrapped by a mod's {@code updateCameraAndRender} override. Returns false when there is none (the caller then draws the
     * frame itself); true when the frame was drawn through it (once, even if the override never reached its {@code super} call).
     */
    public static boolean frame(float partialTick, Runnable modernFrame) {
        var renderer = overriddenRenderer();
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (renderer == null || mc.level == null || mc.player == null || failures > 5) return false;
        boolean[] drawn = new boolean[1];
        Runnable once = () -> {
            if (drawn[0]) return;
            drawn[0] = true;
            modernFrame.run();
        };
        CONTINUATION.set(once);
        try {
            renderer.updateCameraAndRender(partialTick);
        } catch (RuntimeException | LinkageError e) {
            if (failures++ < 3) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Render").warn("A mod's EntityRenderer.updateCameraAndRender failed", e);
        } finally {
            CONTINUATION.remove();
        }
        once.run();
        return true;
    }

    /** The shim's {@code EntityRenderer.updateCameraAndRender}: the modern frame, when a mod's override is running. */
    public static void yieldToModernFrame() {
        Runnable r = CONTINUATION.get();
        if (r != null) r.run();
    }

    /** How far the eyes of the camera entity sit above the modern eye position: what the mod made the player's {@code yOffset} (1.62 is the normal one). */
    public static double cameraRaise(net.minecraft.world.entity.Entity entity) {
        if (entity != net.minecraft.client.Minecraft.getInstance().player || overriddenRenderer() == null) return 0.0;
        var legacy = org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(entity);
        return legacy == null ? 0.0 : legacy.yOffset - 1.62F;
    }

    /** The distance of the third person camera the mod set ({@code EntityRenderer.thirdPersonDistance}), or the normal 4. */
    public static float thirdPersonDistance(float normal) {
        var renderer = overriddenRenderer();
        if (renderer == null) return normal;
        float d = renderer.thirdPersonDistance;
        return d > 0.0F ? d : normal;
    }

    /** The client reach of a mod's {@code PlayerControllerMP} subclass, or {@code normal}. */
    public static double blockReach(double normal) {
        if (!present()) return normal;
        var mc = legacyMc();
        PlayerControllerMP controller = mc == null ? null : mc.playerController;
        if (controller == null || controller.getClass() == PlayerControllerMP.class) return normal;
        if (IN_REACH.get() != null) return normal; // the override's own super call asks the game for the plain reach
        IN_REACH.set(Boolean.TRUE);
        try {
            return controller.getBlockReachDistance();
        } catch (RuntimeException | LinkageError e) {
            if (failures++ < 3) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Render").warn("A mod's PlayerControllerMP.getBlockReachDistance failed", e);
            return normal;
        } finally {
            IN_REACH.remove();
        }
    }
}
