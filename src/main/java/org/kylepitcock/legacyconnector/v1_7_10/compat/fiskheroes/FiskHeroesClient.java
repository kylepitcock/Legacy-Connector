package org.kylepitcock.legacyconnector.v1_7_10.compat.fiskheroes;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MOD-SPECIFIC COMPAT (mod id "fiskheroes"), client half of {@link FiskHeroesHooks}: the client hooks Fisk's coremod put into
 * Minecraft's renderer and input code ({@code ASMHooksClient}), called from the closest modern event or mixin:
 * <ul>
 * <li>{@code applyPlayerRenderTranslation}: a shrunk or grown hero's model is scaled by their size ({@code RenderLivingEvent}); the
 * hitbox follows through {@code Entity.setSize} ({@code EntitySizes1710}).</li>
 * <li>{@code orientCameraPre}: flight roll, recoil and camera shake, wall crawling and web rappel flips rotate the view
 * ({@code ViewportEvent.ComputeCameraAngles}); the hook draws into a proxy that accumulates the rotations.</li>
 * <li>{@code renderFirstPersonArm}/{@code renderFirstPersonArmPost}: the suit's own first-person arms ({@code RenderArmEvent}).</li>
 * <li>{@code disableLeftClick}/{@code disableBlockDestruction}: shield blocking and left-click abilities do not attack or break blocks
 * ({@code mixin/MinecraftAttackMixin}, {@code mixin/MultiPlayerGameModeMixin}).</li>
 * </ul>
 * Not wired: the off-hand item rendering ({@code SHRenderHooks.renderItemInFirstPerson}), night vision overrides, light level tweaks for
 * intangible heroes, inventory preview scale. Every hook is guarded: after a few failures it switches itself off. A no-op without the mod.
 */
public final class FiskHeroesClient {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/FiskHeroes");

    private static volatile boolean resolved;
    private static volatile boolean absent;
    private static int failures;
    private static Method interpolateScale;
    private static Object scaleVar;
    private static Method renderArm;
    private static Method renderArmPost;
    private static Method orient;
    private static Method disableLeftClick;
    private static Method disableBlockDestruction;
    private static Object renderProxy;
    private static final Matrix4f CAMERA = new Matrix4f();
    private static final Vector3f EULER = new Vector3f();
    /** Entities whose pose stack was pushed by the scale hook and awaits the matching pop. */
    private static final Deque<Object> SCALED = new ArrayDeque<>();

    private FiskHeroesClient() {}

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOWEST, (RenderLivingEvent.Pre<?, ?> e) -> onRenderPre(e));
        bus.addListener(EventPriority.HIGHEST, (RenderLivingEvent.Post<?, ?> e) -> onRenderPost(e));
        bus.addListener(EventPriority.LOW, FiskHeroesClient::onCameraAngles);
        bus.addListener(EventPriority.LOW, FiskHeroesClient::onRenderArm);
    }

    private static boolean ready() {
        if (resolved) return !absent;
        LegacyModRecord rec = LegacyMods.byLegacyId("fiskheroes");
        if (rec == null) {
            absent = true;
            resolved = true;
            return false;
        }
        if (rec.mainClass == null) return false;
        synchronized (FiskHeroesClient.class) {
            if (resolved) return !absent;
            try {
                ClassLoader loader = rec.mainClass.getClassLoader();
                Class<?> vars = Class.forName("com.fiskmods.heroes.common.data.var.Vars", true, loader);
                scaleVar = vars.getField("SCALE").get(null);
                interpolateScale = scaleVar.getClass().getMethod("interpolate", legacyconnector.v1_7_10.net.minecraft.entity.Entity.class);
                Class<?> client = Class.forName("com.fiskmods.heroes.asm.ASMHooksClient", true, loader);
                Class<?> renderPlayer = legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderPlayer.class;
                Class<?> player = legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer.class;
                renderArm = client.getMethod("renderFirstPersonArm", renderPlayer, player);
                renderArmPost = client.getMethod("renderFirstPersonArmPost", renderPlayer, player);
                disableLeftClick = client.getMethod("disableLeftClick");
                disableBlockDestruction = client.getMethod("disableBlockDestruction");
                Class<?> glProxy = Class.forName("com.fiskmods.heroes.client.animation.GLProxy", true, loader);
                orient = Class.forName("com.fiskmods.heroes.client.CameraHandler", true, loader).getMethod("orient", glProxy, float.class);
                renderProxy = Proxy.newProxyInstance(loader, new Class<?>[] {glProxy}, new CameraProxy());
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                LOG.warn("Fisk's Heroes' client hooks are not available: {}", e.toString());
                absent = true;
            }
            resolved = true;
            return !absent;
        }
    }

    private static boolean enabled() {
        return failures < 6 && ready();
    }

    private static void failed(String what, Throwable t) {
        Throwable cause = t instanceof java.lang.reflect.InvocationTargetException && t.getCause() != null ? t.getCause() : t;
        if (failures++ < 3) LOG.warn("Fisk's Heroes' {} hook failed{}", what, failures >= 3 ? " (switching the client hooks off after more failures)" : "", cause);
    }

    // ------------------------------------------------------------------ player scale

    private static float scaleOf(AbstractClientPlayer player) throws ReflectiveOperationException {
        Object legacy = EntityBridge.wrap(player);
        Object value = interpolateScale.invoke(scaleVar, legacy);
        return value instanceof Number n ? n.floatValue() : 1.0F;
    }

    private static void onRenderPre(RenderLivingEvent.Pre<?, ?> e) {
        if (!(e.getEntity() instanceof AbstractClientPlayer player) || e.isCanceled() || !enabled()) return;
        try {
            float scale = scaleOf(player);
            if (scale > 0.0F && Math.abs(scale - 1.0F) > 1.0E-3F) {
                PoseStack pose = e.getPoseStack();
                pose.pushPose();
                pose.scale(scale, scale, scale);
                SCALED.push(player);
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            failed("player scale", ex);
        }
    }

    private static void onRenderPost(RenderLivingEvent.Post<?, ?> e) {
        if (SCALED.peek() == e.getEntity()) {
            SCALED.pop();
            e.getPoseStack().popPose();
        }
    }

    // ------------------------------------------------------------------ camera

    private static void onCameraAngles(ViewportEvent.ComputeCameraAngles e) {
        if (!enabled() || Minecraft.getInstance().player == null || e.getCamera().getEntity() != Minecraft.getInstance().player) return;
        try {
            CAMERA.identity();
            orient.invoke(null, renderProxy, (float) e.getPartialTick());
            if (CAMERA.m00() == 1.0F && CAMERA.m11() == 1.0F && CAMERA.m22() == 1.0F && CAMERA.m01() == 0.0F && CAMERA.m02() == 0.0F && CAMERA.m12() == 0.0F) return;
            // the hook rotated the view in front of the camera's own yaw and pitch: roll, plus small yaw and pitch changes
            CAMERA.getEulerAnglesZYX(EULER);
            e.setRoll(e.getRoll() + (float) Math.toDegrees(EULER.z));
            e.setYaw(e.getYaw() + (float) Math.toDegrees(EULER.y));
            e.setPitch(e.getPitch() + (float) Math.toDegrees(EULER.x));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            failed("camera", ex);
        }
    }

    /** The GLProxy the camera hook draws into: the rotations multiply into {@link #CAMERA}. */
    private static final class CameraProxy implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "rotate" -> CAMERA.rotate((float) Math.toRadians((double) args[0]), (float) (double) args[1], (float) (double) args[2], (float) (double) args[3]);
                case "translate" -> CAMERA.translate((float) (double) args[0], (float) (double) args[1], (float) (double) args[2]);
                case "scale" -> {
                    if (args.length == 1) CAMERA.scale((float) (double) args[0]);
                    else CAMERA.scale((float) (double) args[0], (float) (double) args[1], (float) (double) args[2]);
                }
                case "toString" -> {
                    return "Fisk camera proxy";
                }
                case "hashCode" -> {
                    return System.identityHashCode(proxy);
                }
                case "equals" -> {
                    return proxy == args[0];
                }
                default -> {
                    // push/pop and the like: nothing to keep
                }
            }
            return null;
        }
    }

    // ------------------------------------------------------------------ first-person arm

    private static void onRenderArm(RenderArmEvent e) {
        if (!enabled() || e.isCanceled() || e.getArm() != e.getPlayer().getMainArm() || e.getPlayer() != Minecraft.getInstance().player) return;
        try {
            var render = legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderManager.instance.mcc$playerRenderer();
            Object legacy = EntityBridge.wrap(e.getPlayer());
            RenderContext.beginWorld(e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight(), OverlayTexture.NO_OVERLAY);
            try {
                boolean defaultArm = (boolean) renderArm.invoke(null, render, legacy);
                if (!defaultArm) e.setCanceled(true);
                renderArmPost.invoke(null, render, legacy);
            } finally {
                RenderContext.end();
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            failed("first-person arm", ex);
        }
    }

    // ------------------------------------------------------------------ input

    /** {@code ASMHooksClient.disableLeftClick}: the attack and the continued block breaking are skipped. */
    public static boolean disableLeftClick() {
        if (resolved && absent) return false;
        if (!enabled()) return false;
        try {
            return (boolean) disableLeftClick.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            failed("left click", ex);
            return false;
        }
    }

    /** {@code ASMHooksClient.disableBlockDestruction}: creative-mode block breaking is skipped while a weapon or ability is out. */
    public static boolean disableBlockDestruction() {
        if (resolved && absent) return false;
        if (!enabled()) return false;
        try {
            return (boolean) disableBlockDestruction.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            failed("block destruction", ex);
            return false;
        }
    }
}
