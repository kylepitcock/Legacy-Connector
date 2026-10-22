package legacyconnector.v1_7_10.net.minecraft.client.renderer;

/** The world renderer; mods only ask it to redraw blocks after their tile entities changed. */
public class RenderGlobal {
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
}
