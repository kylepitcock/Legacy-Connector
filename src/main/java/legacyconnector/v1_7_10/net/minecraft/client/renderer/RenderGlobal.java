package legacyconnector.v1_7_10.net.minecraft.client.renderer;

/** The world renderer; mods only ask it to redraw blocks after their tile entities changed. */
public class RenderGlobal {
    /** Block-breaking progress by breaker id (the modern renderer draws it; reflection finds the 1.7.10 field). */
    private final java.util.Map damagedBlocks = new java.util.HashMap();
    /** 1.7.10's chunk section renderers: the modern renderer has its own, so this is empty. */
    private WorldRenderer[] worldRenderers = new WorldRenderer[0];
    private java.util.List worldRenderersToUpdate = new java.util.ArrayList();
    public void markBlockForUpdate(int x, int y, int z) {
        markBlocksForUpdate(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
    }

    public void markBlockForRenderUpdate(int x, int y, int z) {
        markBlockForUpdate(x, y, z);
    }

    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
        markBlocksForUpdate(x1 - 1, y1 - 1, z1 - 1, x2 + 1, y2 + 1, z2 + 1);
    }

    public void markBlocksForUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
        var renderer = net.minecraft.client.Minecraft.getInstance().levelRenderer;
        if (renderer != null) renderer.setBlocksDirty(x1, y1, z1, x2, y2, z2);
    }

    public legacyconnector.v1_7_10.net.minecraft.client.particle.EntityFX doSpawnParticle(String name, double x, double y, double z, double vx, double vy, double vz) {
        return null;
    }
}
