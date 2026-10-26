package legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity;

import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations;

/**
 * 1.7.10's beacon renderer: the white beam rising 256 blocks (times the beam's fade-in strength) from the block. Mods extend
 * it for their own beacons (Chisel's carvable beacons) and draw the beam with their colour. The beam is the modern beacon
 * beam render, at the legacy height and in the legacy texture.
 */
public class TileEntityBeaconRenderer extends TileEntitySpecialRenderer {
    private static final ResourceLocation field_147523_b = new ResourceLocation("textures/entity/beacon_beam.png");

    /**
     * Vanilla beacons are drawn by the modern game. A legacy beacon tile entity (the shim has no TileEntityBeacon of its own
     * yet) reaches its renderer's own typed overload; here nothing is known about its beam.
     */
    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
    }

    /** Draws a beam of the given strength (0..1, the beacon's fade-in) and RGB colour at the block corner (x, y, z). */
    protected void mcc$renderBeam(double x, double y, double z, float partialTicks, float strength, int rgb) {
        var pose = RenderContext.pose();
        var buffers = RenderContext.buffers();
        if (pose == null || buffers == null || strength <= 0.0F) return;
        var level = net.minecraft.client.Minecraft.getInstance().level;
        long time = level == null ? 0L : level.getGameTime();
        pose.pushPose();
        pose.translate(x, y, z);
        try {
            net.minecraft.client.renderer.blockentity.BeaconRenderer.renderBeaconBeam(pose, buffers,
                    TextureLocations.modern(field_147523_b.getResourceDomain(), field_147523_b.getResourcePath()), partialTicks, 1.0F, time, 0,
                    Math.max(1, (int) (256.0F * strength)), 0xFF000000 | rgb, 0.2F, 0.25F);
        } finally {
            pose.popPose();
        }
    }
}
