package org.kylepitcock.legacyconnector.v1_12_2.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.WorldProvider;
import legacyconnector.v1_12_2.net.minecraftforge.client.IRenderHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.kylepitcock.legacyconnector.v1_12_2.dimension.Dimensions1122;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sky, fog, clouds and weather of 1.12.2 mod dimensions: asks the dimension's legacy {@code WorldProvider} (the 1.7.10 twin is
 * {@code LegacyDimensionEffects}). Only what the provider's class overrides takes effect, and a legacy renderer that fails is dropped
 * for that dimension (the vanilla one is drawn instead).
 */
public final class LegacyDimensionEffects1122 extends DimensionSpecialEffects {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Dimensions1122");
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private static final java.util.Map<String, Boolean> OVERRIDES = new ConcurrentHashMap<>();
    /** A provider's own sky colour may call the default, which asks the game again: answer that one vanilla. */
    private static final ThreadLocal<Boolean> IN_HOOK = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public LegacyDimensionEffects1122() {
        super(128.0F, true, SkyType.NORMAL, false, false);
    }

    private static WorldProvider provider() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || Dimensions1122.idOf(level.dimension().location()) == null) return null;
        try {
            return World.mcc$get$provider(org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(level));
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static boolean overrides(WorldProvider p, String name, Class<?>... params) {
        return OVERRIDES.computeIfAbsent(p.getClass().getName() + "#" + name, k -> {
            try {
                return LegacyBlockAdapter.overrides(p, name, params);
            } catch (RuntimeException | LinkageError e) {
                return false;
            }
        });
    }

    private static Vec3 vec(legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d v) {
        return new Vec3(v.x, v.y, v.z);
    }

    /** The sky colour a provider's {@code getSkyColor} overrides (from ClientLevelSkyColorMixin); the game's colour otherwise. */
    public static Vec3 skyColor(ClientLevel level, Vec3 current, float partialTick) {
        if (IN_HOOK.get() || level != Minecraft.getInstance().level) return current;
        WorldProvider p = provider();
        if (p == null || FAILED.contains(p.getClass().getName() + ":skyColor")
                || !overrides(p, "getSkyColor", legacyconnector.v1_12_2.net.minecraft.entity.Entity.class, float.class)) return current;
        IN_HOOK.set(Boolean.TRUE);
        try {
            var cam = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(Minecraft.getInstance().getCameraEntity());
            var legacy = p.getSkyColor(cam, partialTick);
            return legacy == null ? current : vec(legacy);
        } catch (RuntimeException | LinkageError e) {
            fail(p, "skyColor", e);
            return current;
        } finally {
            IN_HOOK.set(Boolean.FALSE);
        }
    }

    /** The star brightness a provider overrides (from the mixin). */
    public static float starBrightness(ClientLevel level, float current, float partialTick) {
        if (IN_HOOK.get() || level != Minecraft.getInstance().level) return current;
        WorldProvider p = provider();
        if (p == null || FAILED.contains(p.getClass().getName() + ":starBrightness") || !overrides(p, "getStarBrightness", float.class)) return current;
        IN_HOOK.set(Boolean.TRUE);
        try {
            return p.getStarBrightness(partialTick);
        } catch (RuntimeException | LinkageError e) {
            fail(p, "starBrightness", e);
            return current;
        } finally {
            IN_HOOK.set(Boolean.FALSE);
        }
    }

    @Override
    public SkyType skyType() {
        WorldProvider p = provider();
        try {
            return p != null && !p.hasSkyLight() && !p.isSurfaceWorld() ? SkyType.NONE : SkyType.NORMAL;
        } catch (RuntimeException | LinkageError e) {
            return SkyType.NORMAL;
        }
    }

    @Override
    public float getCloudHeight() {
        WorldProvider p = provider();
        if (p == null) return super.getCloudHeight();
        try {
            if (p.getCloudRenderer() == null && !p.isSurfaceWorld()) return Float.NaN;
            return p.getCloudHeight();
        } catch (RuntimeException | LinkageError e) {
            return super.getCloudHeight();
        }
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        Vec3 vanilla = fogColor.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
        WorldProvider p = provider();
        if (p == null || !overrides(p, "getFogColor", float.class, float.class)) return vanilla;
        try {
            float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            var legacy = p.getFogColor(Minecraft.getInstance().level.getTimeOfDay(partial), partial);
            if (legacy != null) return vec(legacy);
        } catch (RuntimeException | LinkageError e) {
            fail(p, "getFogColor", e);
        }
        return vanilla;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        WorldProvider p = provider();
        if (p == null) return false;
        try {
            return p.doesXZShowFog(x, z);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelView, Camera camera, Matrix4f projection, boolean foggy,
            Runnable setupFog) {
        WorldProvider p = provider();
        IRenderHandler handler = p == null || FAILED.contains(p.getClass().getName() + ":sky") ? null : p.getSkyRenderer();
        if (p != null && REPORTED.add(p.getClass().getName())) {
            LOG.info("Sky of {} ({}): legacy sky renderer {}", level.dimension().location(), p.getClass().getName(),
                    handler == null ? "none (the vanilla sky is drawn, coloured by the provider)" : handler.getClass().getName());
        }
        if (handler == null) return false;
        setupFog.run();
        return draw(p, "sky", handler, level, partialTick, modelView, RenderContext.KIND_SKY);
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack pose, double camX, double camY, double camZ, Matrix4f modelView,
            Matrix4f projection) {
        WorldProvider p = provider();
        IRenderHandler handler = p == null || FAILED.contains(p.getClass().getName() + ":clouds") ? null : p.getCloudRenderer();
        if (handler == null) return false;
        return draw(p, "clouds", handler, level, partialTick, modelView, RenderContext.KIND_WORLD);
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture light, double camX, double camY, double camZ) {
        WorldProvider p = provider();
        IRenderHandler handler = p == null || FAILED.contains(p.getClass().getName() + ":weather") ? null : p.getWeatherRenderer();
        if (handler == null) return false;
        return draw(p, "weather", handler, level, partialTick, new Matrix4f(), RenderContext.KIND_WORLD);
    }

    private static boolean draw(WorldProvider p, String what, IRenderHandler handler, ClientLevel level, float partialTick, Matrix4f modelView, int kind) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack pose = new PoseStack();
        pose.last().pose().set(modelView);
        var buffers = mc.renderBuffers().bufferSource();
        RenderContext.beginWorld(pose, buffers, LightTexture.FULL_BRIGHT, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, kind);
        try {
            handler.render(partialTick, (legacyconnector.v1_12_2.net.minecraft.client.multiplayer.WorldClient) org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(level),
                    legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft());
            return true;
        } catch (RuntimeException | LinkageError e) {
            fail(p, what, e);
            return false;
        } finally {
            RenderContext.end();
            buffers.endBatch();
        }
    }

    private static void fail(WorldProvider p, String what, Throwable e) {
        if (FAILED.add(p.getClass().getName() + ":" + what)) {
            LOG.warn("The legacy {} of dimension {} failed; the vanilla one is used instead: {}", what, p.getClass().getName(), e.toString());
        }
    }
}
