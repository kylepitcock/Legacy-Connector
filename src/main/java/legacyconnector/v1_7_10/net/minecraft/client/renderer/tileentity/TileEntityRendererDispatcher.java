package legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Legacy TESR registry. Modern rendering drives it through {@code LegacyBlockEntityRenderer}, which refreshes the
 * camera fields below before every legacy render call.
 */
public class TileEntityRendererDispatcher {
    public static TileEntityRendererDispatcher instance = new TileEntityRendererDispatcher();
    public static double staticPlayerX;
    public static double staticPlayerY;
    public static double staticPlayerZ;
    public Map mapSpecialRenderers = new HashMap();
    public TextureManager renderEngine = TextureManager.INSTANCE;
    public World worldObj;
    public EntityLivingBase field_147551_g;
    public float field_147562_h;
    public float field_147563_i;
    public double field_147560_j;
    public double field_147561_k;
    public double field_147558_l;

    public TileEntitySpecialRenderer getSpecialRendererByClass(Class cls) {
        TileEntitySpecialRenderer r = (TileEntitySpecialRenderer) mapSpecialRenderers.get(cls);
        if (r == null && cls != TileEntity.class && cls != null) {
            r = getSpecialRendererByClass(cls.getSuperclass());
            if (r != null) mapSpecialRenderers.put(cls, r);
        }
        return r;
    }

    public boolean hasSpecialRenderer(TileEntity te) {
        return getSpecialRenderer(te) != null;
    }

    public TileEntitySpecialRenderer getSpecialRenderer(TileEntity te) {
        return te == null ? null : getSpecialRendererByClass(te.getClass());
    }

    /** Renders at a camera-relative offset; the active render context provides the matrix and buffers. */
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
        TileEntitySpecialRenderer r = getSpecialRenderer(te);
        if (r != null) r.renderTileEntityAt(te, x, y, z, partialTicks);
    }

    public void renderTileEntity(TileEntity te, float partialTicks) {
        renderTileEntityAt(te, te.xCoord - staticPlayerX, te.yCoord - staticPlayerY, te.zCoord - staticPlayerZ, partialTicks);
    }

    public FontRenderer getFontRenderer() {
        return FontRenderer.INSTANCE;
    }

    public void func_147543_a(World world) {
        worldObj = world;
    }

    /** Caches the view for the frame: world, viewer, its interpolated look and position (the camera fields). */
    public void cacheActiveRenderInfo(World world, TextureManager textures, FontRenderer font, EntityLivingBase viewer, float partialTicks) {
        if (worldObj != world) func_147543_a(world);
        renderEngine = textures;
        field_147551_g = viewer;
        var m = viewer == null ? null : viewer.mcc$entity;
        if (m == null) return;
        field_147562_h = net.minecraft.util.Mth.lerp(partialTicks, m.yRotO, m.getYRot());
        field_147563_i = net.minecraft.util.Mth.lerp(partialTicks, m.xRotO, m.getXRot());
        field_147560_j = net.minecraft.util.Mth.lerp(partialTicks, m.xOld, m.getX());
        field_147561_k = net.minecraft.util.Mth.lerp(partialTicks, m.yOld, m.getY());
        field_147558_l = net.minecraft.util.Mth.lerp(partialTicks, m.zOld, m.getZ());
    }

    public void mcc$setCamera(double x, double y, double z) {
        staticPlayerX = x;
        staticPlayerY = y;
        staticPlayerZ = z;
        field_147560_j = x;
        field_147561_k = y;
        field_147558_l = z;
    }
}
