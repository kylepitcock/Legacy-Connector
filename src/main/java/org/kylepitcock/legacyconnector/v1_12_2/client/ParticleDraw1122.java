package org.kylepitcock.legacyconnector.v1_12_2.client;

import legacyconnector.v1_12_2.net.minecraft.client.particle.Particle;
import legacyconnector.v1_12_2.net.minecraft.client.particle.ParticleManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations;

/**
 * Draws 1.12.2 particles through their {@code renderParticle} ({@link ParticleManager#mcc$customRenderer}), as 1.12's particle manager
 * did: layer 0 with the particle sheet, layer 1 with the block atlas, each into a begun buffer that is drawn after; layer 3 particles
 * bind their texture and draw themselves. Runs inside the world render context of {@code LegacyParticles1122}.
 */
public final class ParticleDraw1122 implements ParticleManager.CustomRenderer {
    private static final net.minecraft.resources.ResourceLocation SHEET = TextureLocations.modern("minecraft", "textures/particle/particles.png");

    private ParticleDraw1122() {}

    public static void install() {
        ParticleManager.mcc$customRenderer = new ParticleDraw1122();
    }

    @Override
    public void render(Particle particle, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getEntity();
        var viewer = camera == null ? null : EntityBridge1122.wrap(camera);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        try {
            int layer = particle.getFXLayer();
            if (layer == 3) {
                particle.renderParticle(buffer, viewer, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
                return;
            }
            RenderContext.bindTexture(layer == 1 ? net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS : SHEET);
            buffer.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
            particle.renderParticle(buffer, viewer, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
            tessellator.draw();
        } finally {
            Tessellator.mcc$reset();
        }
    }
}
