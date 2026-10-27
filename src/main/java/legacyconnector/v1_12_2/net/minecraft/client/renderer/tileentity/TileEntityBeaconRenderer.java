package legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/**
 * 1.12.2 beacon renderer's beam helper, which mods use for their own beams (waypoint beacons, totems): a rotating inner beam and a
 * translucent outer glow with the bound texture (normally {@link #TEXTURE_BEACON_BEAM}) scrolling along it.
 */
public class TileEntityBeaconRenderer extends TileEntitySpecialRenderer<TileEntity> {
    public static final ResourceLocation TEXTURE_BEACON_BEAM = new ResourceLocation("textures/entity/beacon_beam.png");

    public static void renderBeamSegment(double x, double y, double z, double partialTicks, double textureScale, double totalWorldTime, int yOffset, int height,
            float[] colors) {
        renderBeamSegment(x, y, z, partialTicks, textureScale, totalWorldTime, yOffset, height, colors, 0.2D, 0.25D);
    }

    public static void renderBeamSegment(double x, double y, double z, double partialTicks, double textureScale, double totalWorldTime, int yOffset, int height,
            float[] colors, double beamRadius, double glowRadius) {
        int top = yOffset + height;
        GlStateManager.glTexParameteri(3553, 10242, 10497);
        GlStateManager.glTexParameteri(3553, 10243, 10497);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder b = tessellator.getBuffer();
        double time = totalWorldTime + partialTicks;
        double dir = height < 0 ? time : -time;
        double scroll = MathHelper.frac(dir * 0.2D - (double) MathHelper.floor(dir * 0.1D));
        float r = colors[0], g = colors[1], bl = colors[2];
        double angle = time * 0.025D * -1.5D;
        double x1 = 0.5D + Math.cos(angle + 2.356194490192345D) * beamRadius, z1 = 0.5D + Math.sin(angle + 2.356194490192345D) * beamRadius;
        double x2 = 0.5D + Math.cos(angle + (Math.PI / 4D)) * beamRadius, z2 = 0.5D + Math.sin(angle + (Math.PI / 4D)) * beamRadius;
        double x3 = 0.5D + Math.cos(angle + 3.9269908169872414D) * beamRadius, z3 = 0.5D + Math.sin(angle + 3.9269908169872414D) * beamRadius;
        double x4 = 0.5D + Math.cos(angle + 5.497787143782138D) * beamRadius, z4 = 0.5D + Math.sin(angle + 5.497787143782138D) * beamRadius;
        double v0 = -1.0D + scroll;
        double v1 = (double) height * textureScale * (0.5D / beamRadius) + v0;
        b.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        quad(b, x, y, z, top, yOffset, x1, z1, x2, z2, v0, v1, r, g, bl, 1.0F);
        quad(b, x, y, z, top, yOffset, x4, z4, x3, z3, v0, v1, r, g, bl, 1.0F);
        quad(b, x, y, z, top, yOffset, x2, z2, x4, z4, v0, v1, r, g, bl, 1.0F);
        quad(b, x, y, z, top, yOffset, x3, z3, x1, z1, v0, v1, r, g, bl, 1.0F);
        tessellator.draw();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.depthMask(false);
        double lo = 0.5D - glowRadius, hi = 0.5D + glowRadius;
        double gv0 = -1.0D + scroll;
        double gv1 = (double) height * textureScale + gv0;
        b.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        quad(b, x, y, z, top, yOffset, lo, lo, hi, lo, gv0, gv1, r, g, bl, 0.125F);
        quad(b, x, y, z, top, yOffset, hi, hi, lo, hi, gv0, gv1, r, g, bl, 0.125F);
        quad(b, x, y, z, top, yOffset, hi, lo, hi, hi, gv0, gv1, r, g, bl, 0.125F);
        quad(b, x, y, z, top, yOffset, lo, hi, lo, lo, gv0, gv1, r, g, bl, 0.125F);
        tessellator.draw();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);
    }

    /** One upright face of the beam from (ax, az) to (bx, bz), between yOffset and top. */
    private static void quad(BufferBuilder b, double x, double y, double z, int top, int bottom, double ax, double az, double bx, double bz, double v0, double v1,
            float r, float g, float bl, float a) {
        b.pos(x + ax, y + top, z + az).tex(1.0D, v1).color(r, g, bl, a).endVertex();
        b.pos(x + ax, y + bottom, z + az).tex(1.0D, v0).color(r, g, bl, a).endVertex();
        b.pos(x + bx, y + bottom, z + bz).tex(0.0D, v0).color(r, g, bl, a).endVertex();
        b.pos(x + bx, y + top, z + bz).tex(0.0D, v1).color(r, g, bl, a).endVertex();
    }
}
