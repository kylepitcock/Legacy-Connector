package org.kylepitcock.legacyconnector.v1_7_10.client;

import legacyconnector.v1_7_10.net.minecraft.client.particle.EffectRenderer;
import legacyconnector.v1_7_10.net.minecraft.client.particle.EntityFX;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Ticks legacy particles with the client and draws them right after vanilla particles. */
public final class LegacyParticles {
    private LegacyParticles() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LegacyParticles::onTick);
        NeoForge.EVENT_BUS.addListener(LegacyParticles::onRender);
    }

    private static EffectRenderer effects() {
        return legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().effectRenderer;
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            effects().clearEffects(null);
            return;
        }
        if (PARTICLE_SETTING) syncParticleSetting(mc);
        if (!mc.isPaused()) effects().updateEffects();
    }

    private static final boolean CULL_BUILT_IN = org.kylepitcock.legacyconnector.core.LegacyConfig.perfCullLegacyParticles();
    private static final boolean CULL_MODS = org.kylepitcock.legacyconnector.core.LegacyConfig.perfCullModParticles();
    private static final boolean PARTICLE_SETTING = org.kylepitcock.legacyconnector.core.LegacyConfig.perfParticleSetting();

    /**
     * 1.7.10's particleSetting (0 all, 1 decreased, 2 minimal: the same order as the modern option) as the player set it: mods
     * read it to spawn fewer of their own particles (Thaumcraft, Reika's mods), which 1.7.10 left to them.
     */
    private static void syncParticleSetting(Minecraft mc) {
        var settings = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.mcc$get$gameSettings(null);
        if (settings == null) return;
        int value = switch (mc.options.particles().get()) {
            case ALL -> 0;
            case DECREASED -> 1;
            case MINIMAL -> 2;
        };
        if (settings.particleSetting != value) settings.particleSetting = value;
    }

    private static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var camera = event.getCamera();
        var cam = camera.getPosition();
        EntityFX.interpPosX = cam.x;
        EntityFX.interpPosY = cam.y;
        EntityFX.interpPosZ = cam.z;
        // The legacy camera basis (ActiveRenderInfo): particles span these vectors to face the camera.
        double yaw = Math.toRadians(camera.getYRot());
        double pitch = Math.toRadians(camera.getXRot());
        float rx = (float) Math.cos(yaw);
        float rz = (float) Math.sin(yaw);
        float ryz = (float) (-rz * Math.sin(pitch));
        float rxy = (float) (rx * Math.sin(pitch));
        float rxz = (float) Math.cos(pitch);
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderContext.beginWorld(event.getPoseStack(), buffers, 0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        try {
            RenderContext.setBlend(true);
            effects().mcc$render(event.getPartialTick().getGameTimeDeltaPartialTick(false), rx, rxz, rz, ryz, rxy, event.getFrustum(), CULL_BUILT_IN, CULL_MODS);
        } finally {
            RenderContext.end();
        }
        buffers.endBatch();
    }
}
