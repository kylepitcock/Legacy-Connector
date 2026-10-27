package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.OpenGlHelper;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.culling.ICamera;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.ClientRegistrations1122;
import org.kylepitcock.legacyconnector.v1_12_2.client.LegacyEntityRenderer1122;
import org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122;

/**
 * 1.12.2 {@code RenderManager}: the legacy entity renderers by entity class. Mod renderers come from the factories registered with
 * {@code RenderingRegistry} (created on first use, as Forge created them when the manager was built); entities without a legacy
 * renderer that stand for modern entities (vanilla mobs, a mod's {@code new EntityItem(...)} drawn on a pedestal) are drawn by the
 * modern entity renderer at the legacy matrix. The view fields (camera position and angles, viewer, world) are refreshed from the
 * modern camera before legacy renderers run.
 */
public class RenderManager {
    private static RenderManager instance;

    public final Map<Class<? extends Entity>, Render<? extends Entity>> entityRenderMap = new HashMap<>();
    private final Map<String, RenderPlayer> skinMap = new HashMap<>();
    private final RenderPlayer playerRenderer;
    private FontRenderer textRenderer;
    public double renderPosX;
    public double renderPosY;
    public double renderPosZ;
    public TextureManager renderEngine;
    public World world;
    public Entity renderViewEntity;
    public Entity pointedEntity;
    public float playerViewY;
    public float playerViewX;
    public GameSettings options;
    public double viewerPosX;
    public double viewerPosY;
    public double viewerPosZ;
    private boolean renderOutlines;
    private boolean renderShadow = true;
    private boolean debugBoundingBox;
    private final Set<Class<?>> mcc$loaded = new HashSet<>();
    private int mcc$loadedCount = -1;
    private final Render<Entity> mcc$modern = new ModernRender(this);
    private final Render<Entity> mcc$placeholder;

    public RenderManager(TextureManager renderEngineIn, RenderItem itemRendererIn) {
        this.renderEngine = renderEngineIn;
        this.textRenderer = FontRenderer.mcc$INSTANCE;
        this.mcc$placeholder = new RenderEntity(this);
        this.playerRenderer = new RenderPlayer(this);
        skinMap.put("default", playerRenderer);
        skinMap.put("slim", new RenderPlayer(this, true));
    }

    /** The game's manager ({@code Minecraft.getRenderManager()}). */
    public static synchronized RenderManager mcc$instance() {
        if (instance == null) {
            instance = new RenderManager(TextureManager.mcc$instance(), RenderItem.mcc$instance());
            instance.options = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$gameSettings(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft());
        }
        return instance;
    }

    public Map<String, RenderPlayer> getSkinMap() {
        return skinMap;
    }

    public void setRenderPosition(double renderPosXIn, double renderPosYIn, double renderPosZIn) {
        this.renderPosX = renderPosXIn;
        this.renderPosY = renderPosYIn;
        this.renderPosZ = renderPosZIn;
    }

    /** Creates the renderers of factories registered since the last look (RenderingRegistry.registerEntityRenderingHandler). */
    private void mcc$loadFactories() {
        int count = ClientRegistrations1122.entityRendererCount();
        if (count == mcc$loadedCount) return;
        mcc$loadedCount = count;
        for (var e : ClientRegistrations1122.entityRenderers().entrySet()) {
            if (!mcc$loaded.add(e.getKey())) continue;
            Render<?> render = mcc$create(e.getValue());
            if (render == null) continue;
            @SuppressWarnings("unchecked")
            Class<? extends Entity> cls = (Class<? extends Entity>) e.getKey();
            entityRenderMap.put(cls, render);
        }
    }

    /** A registered renderer: an IRenderFactory (called with this manager) or, through the deprecated overload, the renderer itself. */
    private Render<?> mcc$create(Object factory) {
        if (factory instanceof Render<?> r) return r;
        try {
            for (Class<?> c = factory.getClass(); c != null; c = c.getSuperclass()) {
                for (Class<?> i : c.getInterfaces()) {
                    for (var m : i.getMethods()) {
                        if (m.getName().equals("createRenderFor") && m.getParameterCount() == 1) {
                            m.setAccessible(true);
                            return (Render<?>) m.invoke(factory, this);
                        }
                    }
                }
            }
        } catch (java.lang.reflect.InvocationTargetException e) {
            RenderErrors1122.report("creating the entity renderer of " + factory.getClass().getName(), e.getCause() == null ? e : e.getCause());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            RenderErrors1122.report("creating the entity renderer of " + factory.getClass().getName(), e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Render<T> getEntityClassRenderObject(Class<? extends Entity> entityClass) {
        mcc$loadFactories();
        Render<? extends Entity> render = entityRenderMap.get(entityClass);
        if (render == null && entityClass != null && entityClass != Entity.class) {
            for (Class<?> c = entityClass.getSuperclass(); c != null && c != Object.class && render == null; c = c.getSuperclass()) {
                render = entityRenderMap.get(c);
            }
            if (render != null) entityRenderMap.put(entityClass, render);
        }
        return (Render<T>) render;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Render<T> getEntityRenderObject(Entity entityIn) {
        if (entityIn == null) return null;
        if (entityIn instanceof legacyconnector.v1_12_2.net.minecraft.client.entity.AbstractClientPlayer player) {
            RenderPlayer render = skinMap.get(player.getSkinType());
            return (Render<T>) (render != null ? render : playerRenderer);
        }
        Render<T> render = getEntityClassRenderObject(entityIn.getClass());
        if (render != null) return render;
        // a view of a modern entity: the modern renderer draws it; a mod entity without a renderer was a white box in 1.12
        boolean hosted = entityIn.mcc$entity instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122;
        return (Render<T>) (hosted ? mcc$placeholder : mcc$modern);
    }

    public void cacheActiveRenderInfo(World worldIn, FontRenderer textRendererIn, Entity livingPlayerIn, Entity pointedEntityIn, GameSettings optionsIn, float partialTicks) {
        this.world = worldIn;
        this.options = optionsIn;
        this.renderViewEntity = livingPlayerIn;
        this.pointedEntity = pointedEntityIn;
        this.textRenderer = textRendererIn;
        if (livingPlayerIn == null) return;
        this.playerViewY = Entity.mcc$get$prevRotationYaw(livingPlayerIn) + (Entity.mcc$get$rotationYaw(livingPlayerIn) - Entity.mcc$get$prevRotationYaw(livingPlayerIn)) * partialTicks;
        this.playerViewX = Entity.mcc$get$prevRotationPitch(livingPlayerIn) + (Entity.mcc$get$rotationPitch(livingPlayerIn) - Entity.mcc$get$prevRotationPitch(livingPlayerIn)) * partialTicks;
        if (optionsIn != null && GameSettings.mcc$get$thirdPersonView(optionsIn) == 2) this.playerViewY += 180.0F;
        this.viewerPosX = Entity.mcc$get$lastTickPosX(livingPlayerIn) + (Entity.mcc$get$posX(livingPlayerIn) - Entity.mcc$get$lastTickPosX(livingPlayerIn)) * partialTicks;
        this.viewerPosY = Entity.mcc$get$lastTickPosY(livingPlayerIn) + (Entity.mcc$get$posY(livingPlayerIn) - Entity.mcc$get$lastTickPosY(livingPlayerIn)) * partialTicks;
        this.viewerPosZ = Entity.mcc$get$lastTickPosZ(livingPlayerIn) + (Entity.mcc$get$posZ(livingPlayerIn) - Entity.mcc$get$lastTickPosZ(livingPlayerIn)) * partialTicks;
    }

    public void setPlayerViewY(float playerViewYIn) {
        this.playerViewY = playerViewYIn;
    }

    public boolean isRenderShadow() {
        return renderShadow;
    }

    public void setRenderShadow(boolean renderShadowIn) {
        this.renderShadow = renderShadowIn;
    }

    public void setDebugBoundingBox(boolean debugBoundingBoxIn) {
        this.debugBoundingBox = debugBoundingBoxIn;
    }

    public boolean isDebugBoundingBox() {
        return debugBoundingBox;
    }

    public boolean isRenderMultipass(Entity entityIn) {
        Render<Entity> render = getEntityRenderObject(entityIn);
        return render != null && render.isMultipass();
    }

    public boolean shouldRender(Entity entityIn, ICamera camera, double camX, double camY, double camZ) {
        Render<Entity> render = getEntityRenderObject(entityIn);
        return render != null && render.shouldRender(entityIn, camera, camX, camY, camZ);
    }

    /** Draws an entity at its interpolated position (relative to the render position), with its own light. */
    public void renderEntityStatic(Entity entityIn, float partialTicks, boolean hideDebugBox) {
        if (Entity.mcc$get$ticksExisted(entityIn) == 0) {
            Entity.mcc$set$lastTickPosX(entityIn, Entity.mcc$get$posX(entityIn));
            Entity.mcc$set$lastTickPosY(entityIn, Entity.mcc$get$posY(entityIn));
            Entity.mcc$set$lastTickPosZ(entityIn, Entity.mcc$get$posZ(entityIn));
        }
        double x = Entity.mcc$get$lastTickPosX(entityIn) + (Entity.mcc$get$posX(entityIn) - Entity.mcc$get$lastTickPosX(entityIn)) * partialTicks;
        double y = Entity.mcc$get$lastTickPosY(entityIn) + (Entity.mcc$get$posY(entityIn) - Entity.mcc$get$lastTickPosY(entityIn)) * partialTicks;
        double z = Entity.mcc$get$lastTickPosZ(entityIn) + (Entity.mcc$get$posZ(entityIn) - Entity.mcc$get$lastTickPosZ(entityIn)) * partialTicks;
        float yaw = Entity.mcc$get$prevRotationYaw(entityIn) + (Entity.mcc$get$rotationYaw(entityIn) - Entity.mcc$get$prevRotationYaw(entityIn)) * partialTicks;
        int light = entityIn.isBurning() ? 15728880 : entityIn.getBrightnessForRender();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) (light % 65536), (float) (light / 65536));
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        renderEntity(entityIn, x - renderPosX, y - renderPosY, z - renderPosZ, yaw, partialTicks, hideDebugBox);
    }

    public void renderEntity(Entity entityIn, double x, double y, double z, float yaw, float partialTicks, boolean hideDebugBox) {
        Render<Entity> render = getEntityRenderObject(entityIn);
        if (render == null) return;
        try {
            render.setRenderOutlines(renderOutlines);
            render.doRender(entityIn, x, y, z, yaw, partialTicks);
            if (!renderOutlines) render.doRenderShadowAndFire(entityIn, x, y, z, yaw, partialTicks);
        } catch (RuntimeException | LinkageError e) {
            RenderErrors1122.report("rendering the entity " + entityIn.getClass().getName() + " with " + render.getClass().getName(), e);
        }
    }

    public void renderMultipass(Entity entityIn, float partialTicks) {
        double x = Entity.mcc$get$lastTickPosX(entityIn) + (Entity.mcc$get$posX(entityIn) - Entity.mcc$get$lastTickPosX(entityIn)) * partialTicks;
        double y = Entity.mcc$get$lastTickPosY(entityIn) + (Entity.mcc$get$posY(entityIn) - Entity.mcc$get$lastTickPosY(entityIn)) * partialTicks;
        double z = Entity.mcc$get$lastTickPosZ(entityIn) + (Entity.mcc$get$posZ(entityIn) - Entity.mcc$get$lastTickPosZ(entityIn)) * partialTicks;
        float yaw = Entity.mcc$get$prevRotationYaw(entityIn) + (Entity.mcc$get$rotationYaw(entityIn) - Entity.mcc$get$prevRotationYaw(entityIn)) * partialTicks;
        int light = entityIn.isBurning() ? 15728880 : entityIn.getBrightnessForRender();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) (light % 65536), (float) (light / 65536));
        Render<Entity> render = getEntityRenderObject(entityIn);
        if (render != null) render.renderMultipass(entityIn, x - renderPosX, y - renderPosY, z - renderPosZ, yaw, partialTicks);
    }

    public void setWorld(World worldIn) {
        this.world = worldIn;
        if (worldIn == null) this.renderViewEntity = null;
    }

    public double getDistanceToCamera(double x, double y, double z) {
        double dx = x - viewerPosX, dy = y - viewerPosY, dz = z - viewerPosZ;
        return dx * dx + dy * dy + dz * dz;
    }

    public FontRenderer getFontRenderer() {
        return textRenderer;
    }

    public void setRenderOutlines(boolean renderOutlinesIn) {
        this.renderOutlines = renderOutlinesIn;
    }

    /** Draws a view of a modern entity with the modern entity renderer (vanilla entities, items a mod draws as an EntityItem). */
    private static final class ModernRender extends Render<Entity> {
        ModernRender(RenderManager manager) {
            super(manager);
        }

        @Override
        public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
            if (entity.mcc$entity != null) LegacyEntityRenderer1122.drawModern(entity.mcc$entity, x, y, z, entityYaw, partialTicks);
        }

        @Override
        protected legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation getEntityTexture(Entity entity) {
            return null;
        }
    }
}
