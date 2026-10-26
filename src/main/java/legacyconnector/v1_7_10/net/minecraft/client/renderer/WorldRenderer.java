package legacyconnector.v1_7_10.net.minecraft.client.renderer;

/**
 * 1.7.10's renderer of one 16x16x16 chunk section. The modern game renders sections itself; this class exists so mods that
 * reach for {@link RenderGlobal#worldRenderers} (Fisk's Heroes looks it up by reflection) find the 1.7.10 type. Marking one
 * dirty or rebuilding it asks the modern renderer to rebuild that section.
 */
public class WorldRenderer {
    public int posX;
    public int posY;
    public int posZ;
    public int posXMinus;
    public int posYMinus;
    public int posZMinus;
    public int posXClip;
    public int posYClip;
    public int posZClip;
    public int posXPlus;
    public int posYPlus;
    public int posZPlus;
    public boolean needsUpdate;
    public boolean isInFrustum = true;
    public boolean isInitialized;
    public boolean[] skipRenderPass = new boolean[] {true, true};
    public java.util.List tileEntityRenderers = new java.util.ArrayList();
    public legacyconnector.v1_7_10.net.minecraft.world.World worldObj;
    private java.util.List tileEntities;
    private int glRenderList = -1;

    public WorldRenderer() {
    }

    /** A renderer of the section at (x, y, z); {@code tileEntities} is the shared list of tile entities to draw specially. */
    public WorldRenderer(legacyconnector.v1_7_10.net.minecraft.world.World world, java.util.List tileEntities, int x, int y, int z, int glRenderList) {
        this.worldObj = world;
        this.tileEntities = tileEntities;
        this.glRenderList = glRenderList;
        this.isInFrustum = false;
        this.posX = -999;
        setPosition(x, y, z);
        this.needsUpdate = false;
    }

    /** Moves the renderer to the section whose corner is (x, y, z) and marks it for a rebuild. */
    public void setPosition(int x, int y, int z) {
        if (x == posX && y == posY && z == posZ) return;
        posX = x;
        posY = y;
        posZ = z;
        posXPlus = x + 8;
        posYPlus = y + 8;
        posZPlus = z + 8;
        posXClip = x & 1023;
        posYClip = y;
        posZClip = z & 1023;
        posXMinus = x - posXClip;
        posYMinus = y - posYClip;
        posZMinus = z - posZClip;
        markDirty();
    }

    /** Rebuilds the section's geometry: the modern renderer rebuilds it on its next pass. */
    public void updateRenderer(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase viewer) {
        markDirty();
        needsUpdate = false;
        isInitialized = true;
    }

    /** Squared distance from the section's centre to the entity. */
    public float distanceToEntitySquared(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        if (entity == null || entity.mcc$entity == null) return 0.0F;
        float dx = (float) (entity.mcc$entity.getX() - posXPlus);
        float dy = (float) (entity.mcc$entity.getY() - posYPlus);
        float dz = (float) (entity.mcc$entity.getZ() - posZPlus);
        return dx * dx + dy * dy + dz * dz;
    }

    /** Forgets the section's geometry (both passes empty). */
    public void stopRendering() {
        skipRenderPass[0] = skipRenderPass[1] = true;
        tileEntityRenderers.clear();
        isInFrustum = false;
        isInitialized = false;
    }

    public boolean skipAllRenderPasses() {
        return isInitialized && skipRenderPass[0] && skipRenderPass[1];
    }

    public void markDirty() {
        needsUpdate = true;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.levelRenderer == null || mc.level == null) return;
        try {
            mc.levelRenderer.setSectionDirty(posX >> 4, posY >> 4, posZ >> 4);
        } catch (RuntimeException e) {
            // no sections yet (the level is being set up)
        }
    }
}
