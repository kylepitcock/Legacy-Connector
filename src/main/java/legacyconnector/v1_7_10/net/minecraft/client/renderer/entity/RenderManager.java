package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_7_10.net.minecraft.world.World;

public class RenderManager {
    public static RenderManager instance = new RenderManager();
    public static double renderPosX;
    public static double renderPosY;
    public static double renderPosZ;
    public TextureManager renderEngine = TextureManager.INSTANCE;
    public FontRenderer fontRenderer = FontRenderer.INSTANCE;
    public World worldObj;
    public float playerViewY;
    public float playerViewX;
    public double viewerPosX;
    public double viewerPosY;
    public double viewerPosZ;

    public legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase livingPlayer;
    public legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings options;
    /** Legacy entity class -> renderer (RenderingRegistry.registerEntityRenderingHandler). */
    public final java.util.Map<Class<?>, Render> entityRenderMap = new java.util.concurrent.ConcurrentHashMap<>();

    public FontRenderer getFontRenderer() {
        return fontRenderer;
    }

    /** The renderer of a legacy entity class (the closest registered superclass). */
    public Render getEntityClassRenderObject(Class<?> cls) {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            Render r = entityRenderMap.get(c);
            if (r != null) {
                if (c != cls) entityRenderMap.put(cls, r);
                return r;
            }
        }
        return null;
    }

    public Render getEntityRenderObject(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        return entity == null ? null : getEntityClassRenderObject(entity.getClass());
    }

    public boolean renderEntityWithPosYaw(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        Render r = getEntityRenderObject(entity);
        if (r == null) return false;
        r.doRender(entity, x, y, z, yaw, partialTicks);
        return true;
    }

    public boolean renderEntitySimple(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, float partialTicks) {
        return renderEntityWithPosYaw(entity, 0, 0, 0, 0, partialTicks);
    }
}
