package org.kylepitcock.legacyconnector.vb1_7_3.client;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.world.dimension.Dimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.kylepitcock.legacyconnector.vb1_7_3.DimensionsB173;

/** Native rendering effects of registered Beta dimensions. */
public final class DimensionEffectsB173 extends DimensionSpecialEffects {
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    public DimensionEffectsB173() {
        super(108.0F, true, SkyType.NORMAL, false, false);
    }

    private static Dimension provider(ClientLevel level) {
        if (level == null || !(level.effects() instanceof DimensionEffectsB173)
                || DimensionsB173.registeredId(level) == null) return null;
        try {
            return DimensionsB173.providerFor(World.mcc$wrap(level));
        } catch (RuntimeException | LinkageError error) {
            return null;
        }
    }

    private static Dimension provider() {
        return provider(Minecraft.getInstance().level);
    }

    @Override public SkyType skyType() {
        var dimension = provider();
        return dimension != null && dimension.hasNoSky ? SkyType.NONE : SkyType.NORMAL;
    }

    @Override public boolean hasGround() {
        var dimension = provider();
        if (dimension == null) return super.hasGround();
        try { return dimension.hasGround(); }
        catch (RuntimeException | LinkageError error) { fail(dimension, "ground", error); return super.hasGround(); }
    }

    @Override public float getCloudHeight() {
        var dimension = provider();
        if (dimension == null) return super.getCloudHeight();
        if (dimension.hasNoSky) return Float.NaN;
        try { return dimension.getCloudHeight(); }
        catch (RuntimeException | LinkageError error) { fail(dimension, "clouds", error); return super.getCloudHeight(); }
    }

    @Override public float[] getSunriseColor(float timeOfDay, float partialTick) {
        var dimension = provider();
        if (dimension == null) return super.getSunriseColor(timeOfDay, partialTick);
        try {
            return dimension.getBackgroundColor(dimension.getTimeOfDay(dimension.world.mcc$level().getDayTime(), partialTick), partialTick);
        } catch (RuntimeException | LinkageError error) {
            fail(dimension, "sunrise", error);
            return super.getSunriseColor(timeOfDay, partialTick);
        }
    }

    @Override public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        var dimension = provider();
        if (dimension != null) {
            try {
                float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
                float time = dimension.getTimeOfDay(dimension.world.mcc$level().getDayTime(), partial);
                var color = dimension.getFogColor(time, partial);
                if (color != null) return new Vec3(color.x, color.y, color.z);
            } catch (RuntimeException | LinkageError error) { fail(dimension, "fog", error); }
        }
        return fogColor.multiply(brightness * .94F + .06F, brightness * .94F + .06F, brightness * .91F + .09F);
    }

    @Override public boolean isFoggyAt(int x, int z) { return false; }

    /** Replace native light inputs with the original provider's table, retaining native gamma and vision effects. */
    @Override public void adjustLightmapColors(ClientLevel level, float partialTick, float skyDarken,
            float blockLightRedFlicker, float skyLight, int blockLight, int skyLevel, Vector3f colors) {
        var dimension = provider(level);
        if (dimension == null) return;
        try {
            float[] table = dimension.lightLevelToLuminance;
            if (table == null || table.length < 16) return;
            float skyScale = level.getSkyFlashTime() > 0 ? 1.0F : skyDarken * .95F + .05F;
            float sky = table[skyLevel] * skyScale;
            float block = table[blockLight] * blockLightRedFlicker;
            var skyColor = new Vector3f(skyDarken, skyDarken, 1.0F).lerp(new Vector3f(1, 1, 1), .35F).mul(sky);
            colors.set(block, block * ((block * .6F + .4F) * .6F + .4F), block * (block * block * .6F + .4F));
            colors.add(skyColor).lerp(new Vector3f(.75F, .75F, .75F), .04F);
            float darken = Minecraft.getInstance().gameRenderer.getDarkenWorldAmount(partialTick);
            if (darken > 0) colors.lerp(new Vector3f(colors).mul(.7F, .6F, .6F), darken);
        } catch (RuntimeException | LinkageError error) { fail(dimension, "lightmap", error); }
    }

    private static void fail(Dimension dimension, String hook, Throwable error) {
        if (FAILED.add(dimension.getClass().getName() + ':' + hook))
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Dimensions").warn(
                    "Beta dimension {} {} failed", dimension.id, hook, error);
    }
}
