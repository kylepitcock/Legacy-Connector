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
    public legacyconnector.v1_7_10.net.minecraft.client.renderer.ItemRenderer itemRenderer = new legacyconnector.v1_7_10.net.minecraft.client.renderer.ItemRenderer();
    /** Legacy entity class -> renderer (RenderingRegistry.registerEntityRenderingHandler). */
    public final java.util.Map<Class<?>, Render> entityRenderMap = new java.util.concurrent.ConcurrentHashMap<>();

    public FontRenderer getFontRenderer() {
        return fontRenderer;
    }

    /** The entity under the crosshair when the view was cached. */
    public legacyconnector.v1_7_10.net.minecraft.entity.Entity field_147941_i;

    /**
     * Caches the view for the frame: world, viewer, its interpolated look (turned around in the front third-person view)
     * and position. The layer refreshes these from the modern camera itself before legacy renderers run.
     */
    public void cacheActiveRenderInfo(World world, TextureManager textures, FontRenderer font, legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase viewer,
            legacyconnector.v1_7_10.net.minecraft.entity.Entity pointed, legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings settings, float partialTicks) {
        this.worldObj = world;
        this.renderEngine = textures;
        this.options = settings;
        this.livingPlayer = viewer;
        this.field_147941_i = pointed;
        this.fontRenderer = font;
        var m = viewer == null ? null : viewer.mcc$entity;
        if (m == null) return;
        this.playerViewY = net.minecraft.util.Mth.lerp(partialTicks, m.yRotO, m.getYRot());
        this.playerViewX = net.minecraft.util.Mth.lerp(partialTicks, m.xRotO, m.getXRot());
        if (net.minecraft.client.Minecraft.getInstance().options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_FRONT) this.playerViewY += 180.0F;
        this.viewerPosX = net.minecraft.util.Mth.lerp(partialTicks, m.xOld, m.getX());
        this.viewerPosY = net.minecraft.util.Mth.lerp(partialTicks, m.yOld, m.getY());
        this.viewerPosZ = net.minecraft.util.Mth.lerp(partialTicks, m.zOld, m.getZ());
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
        // 1.7.10 mapped EntityPlayer to RenderPlayer: mods fetch it to draw the first-person arm (Thaumcraft's Thaumometer)
        if (legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer.class.isAssignableFrom(cls)) return mcc$playerRenderer();
        // vanilla's own: dropped items (pedestals and altars draw their item as an EntityItem), then anything with a modern entity
        if (legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem.class.isAssignableFrom(cls)) return RenderItem.getInstance();
        if (legacyconnector.v1_7_10.net.minecraft.entity.Entity.class.isAssignableFrom(cls)) return RenderModernEntity.INSTANCE;
        return null;
    }

    private RenderPlayer mcc$playerRenderer;

    /** The one player renderer (also what RenderPlayerEvent carries). */
    public synchronized RenderPlayer mcc$playerRenderer() {
        if (mcc$playerRenderer == null) mcc$playerRenderer = new RenderPlayer();
        return mcc$playerRenderer;
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
