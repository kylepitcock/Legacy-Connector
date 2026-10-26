package legacyconnector.v1_7_10.net.minecraft.client.renderer;

/**
 * 1.7.10's renderer of one 16x16x16 chunk section. The modern game renders sections itself; this class exists so mods that
 * reach for {@link RenderGlobal#worldRenderers} (Fisk's Heroes looks it up by reflection) find the 1.7.10 type. Marking one
 * dirty asks the modern renderer to rebuild that section.
 */
public class WorldRenderer {
    public int posX;
    public int posY;
    public int posZ;
    public boolean needsUpdate;
    public boolean isInFrustum = true;
    public boolean isInitialized;

    public void markDirty() {
        needsUpdate = true;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.levelRenderer != null) mc.levelRenderer.setSectionDirty(posX >> 4, posY >> 4, posZ >> 4);
    }
}
