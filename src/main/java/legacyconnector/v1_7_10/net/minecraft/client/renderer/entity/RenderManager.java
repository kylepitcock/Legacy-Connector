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
    public final java.util.Map<Class<?>, Render> entityRenderMap = new RenderMap();

    /**
     * The renderer map. Mods read 1.7.10's vanilla renderers straight out of it (ChromatiCraft swaps the slime's model): a
     * vanilla entity class absent from it answers a legacy renderer built on first read. Lookups for drawing
     * ({@link #getEntityClassRenderObject}) only see what mods registered, so vanilla entities keep their modern rendering.
     */
    private static final class RenderMap extends java.util.concurrent.ConcurrentHashMap<Class<?>, Render> {
        private final java.util.Map<Class<?>, Render> vanilla = new java.util.concurrent.ConcurrentHashMap<>();

        Render registered(Class<?> cls) {
            return super.get(cls);
        }

        @Override
        public Render get(Object key) {
            Render r = super.get(key);
            if (r != null || !(key instanceof Class<?> cls)) return r;
            java.util.function.Supplier<Render> factory = VANILLA.get(cls);
            return factory == null ? null : vanilla.computeIfAbsent(cls, c -> factory.get());
        }
    }

    private static final java.util.Map<Class<?>, java.util.function.Supplier<Render>> VANILLA = java.util.Map.ofEntries(
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.monster.EntitySlime.class,
                    () -> new RenderSlime(new legacyconnector.v1_7_10.net.minecraft.client.model.ModelSlime(16), new legacyconnector.v1_7_10.net.minecraft.client.model.ModelSlime(0), 0.25F)),
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityPig.class,
                    () -> new RenderPig(new legacyconnector.v1_7_10.net.minecraft.client.model.ModelPig(), new legacyconnector.v1_7_10.net.minecraft.client.model.ModelPig(0.5F), 0.7F)),
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntitySheep.class,
                    () -> new RenderSheep(new legacyconnector.v1_7_10.net.minecraft.client.model.ModelSheep2(), new legacyconnector.v1_7_10.net.minecraft.client.model.ModelSheep1(), 0.7F)),
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityCow.class,
                    () -> new RenderCow(new legacyconnector.v1_7_10.net.minecraft.client.model.ModelCow(), 0.7F)),
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityWolf.class,
                    () -> new RenderWolf(new legacyconnector.v1_7_10.net.minecraft.client.model.ModelWolf(), new legacyconnector.v1_7_10.net.minecraft.client.model.ModelWolf(), 0.5F)),
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityChicken.class,
                    () -> new RenderChicken(new legacyconnector.v1_7_10.net.minecraft.client.model.ModelChicken(), 0.3F)),
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityOcelot.class,
                    () -> new RenderOcelot(new legacyconnector.v1_7_10.net.minecraft.client.model.ModelOcelot(), 0.4F)),
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.monster.EntitySilverfish.class, RenderSilverfish::new),
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.monster.EntitySpider.class, RenderSpider::new),
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityCreeper.class, RenderCreeper::new),
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityVillager.class, RenderVillager::new),
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityZombie.class, RenderZombie::new),
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityBat.class, RenderBat::new),
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.boss.EntityWither.class, RenderWither::new),
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityBoat.class, RenderBoat::new),
            java.util.Map.entry(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart.class, RenderMinecart::new));

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
            Render r = ((RenderMap) entityRenderMap).registered(c);
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
