package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import legacyconnector.v1_7_10.net.minecraft.client.multiplayer.WorldClient;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.WorldProvider;
import legacyconnector.v1_7_10.net.minecraftforge.client.IRenderHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.legacyconnector.v1_7_10.dimension.LegacyDimensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sky, fog, clouds and weather of mod dimensions: asks the legacy {@code WorldProvider} of the dimension the player is in.
 * Only what the provider's class overrides takes effect (a provider that keeps vanilla's fog colour keeps the biome fog),
 * and a legacy renderer that fails is dropped for that dimension (the vanilla sky is drawn instead).
 */
public final class LegacyDimensionEffects extends DimensionSpecialEffects {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Dimensions");
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    public LegacyDimensionEffects() {
        super(128.0F, true, SkyType.NORMAL, false, false);
    }

    /** The provider of the level the player is in, when it is a mod dimension; null otherwise. */
    private static WorldProvider provider() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || LegacyDimensions.idOf(level.dimension().location()) == null) return null;
        try {
            return World.mcc$get$provider(World.mcc$wrap(level));
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static boolean overrides(WorldProvider p, String name, Class<?>... params) {
        try {
            return LegacyBlockAdapter.overrides(p, name, params);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @Override
    public SkyType skyType() {
        WorldProvider p = provider();
        return p != null && p.hasNoSky ? SkyType.NONE : SkyType.NORMAL;
    }

    @Override
    public float getCloudHeight() {
        WorldProvider p = provider();
        if (p == null) return super.getCloudHeight();
        try {
            return p.getCloudHeight();
        } catch (RuntimeException | LinkageError e) {
            return super.getCloudHeight();
        }
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        WorldProvider p = provider();
        if (p == null || !overrides(p, "getFogColor", float.class, float.class)) return fogColor.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
        try {
            float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            var legacy = p.getFogColor(Minecraft.getInstance().level.getTimeOfDay(partial), partial);
            if (legacy != null) return new Vec3(legacy.xCoord, legacy.yCoord, legacy.zCoord);
        } catch (RuntimeException | LinkageError e) {
            fail(p, "getFogColor", e);
        }
        return fogColor.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
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
            LOG.info("Sky of dimension {} ({}): legacy sky renderer {}, cloud renderer {}, hasNoSky {}", p.dimensionId, p.getClass().getName(),
                    handler == null ? "none (the vanilla sky is drawn)" : handler.getClass().getName(), p.getCloudRenderer() == null ? "none" : "set", p.hasNoSky);
        }
        if (handler == null) return false;
        setupFog.run();
        return draw(p, "sky", handler, level, partialTick, modelView);
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack pose, double camX, double camY, double camZ, Matrix4f modelView,
            Matrix4f projection) {
        WorldProvider p = provider();
        IRenderHandler handler = p == null || FAILED.contains(p.getClass().getName() + ":clouds") ? null : p.getCloudRenderer();
        if (handler == null) return false;
        return draw(p, "clouds", handler, level, partialTick, modelView);
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture light, double camX, double camY, double camZ) {
        WorldProvider p = provider();
        IRenderHandler handler = p == null || FAILED.contains(p.getClass().getName() + ":weather") ? null : p.getWeatherRenderer();
        if (handler == null) return false;
        return draw(p, "weather", handler, level, partialTick, new Matrix4f());
    }

    /** Runs a legacy renderer against the current camera matrix; the geometry it draws is flushed right away. */
    private static boolean draw(WorldProvider p, String what, IRenderHandler handler, ClientLevel level, float partialTick, Matrix4f modelView) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack pose = new PoseStack();
        pose.last().pose().set(modelView);
        var buffers = mc.renderBuffers().bufferSource();
        RenderContext.beginWorld(pose, buffers, LightTexture.FULL_BRIGHT, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        try {
            handler.render(partialTick, (WorldClient) World.mcc$wrap(level), legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft());
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
            LOG.warn("The legacy {} of dimension {} ({}) failed; the vanilla one is used instead: {}", what, p.dimensionId, p.getClass().getName(), e.toString());
        }
    }
}
