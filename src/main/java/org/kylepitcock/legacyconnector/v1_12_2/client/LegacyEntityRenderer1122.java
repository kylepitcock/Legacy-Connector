package org.kylepitcock.legacyconnector.v1_12_2.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.Render;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.slf4j.LoggerFactory;

/**
 * Modern renderer of the 1.12.2 host entity types: runs the legacy {@link Render} the mod registered for the entity's class (through
 * {@link RenderManager}). Legacy renderers take camera-relative coordinates and translate by them, so the pose (at the entity) is moved
 * back to the camera first. The modern renderer draws the shadow (the legacy shadow size) and fire; name tags are the legacy renderer's.
 */
public class LegacyEntityRenderer1122 extends EntityRenderer<Entity> {
    private static final ResourceLocation NONE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final Map<Class<?>, Integer> FAILURES = new ConcurrentHashMap<>();
    private static final int MAX_FAILURES = 20;

    public LegacyEntityRenderer1122(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(Entity entity) {
        return NONE;
    }

    /**
     * Sodium (and Embeddium) and Entity Culling cull entities by their bounding box against the sections they consider visible, and
     * Entity Culling also by occlusion queries. A legacy entity's box is the 1.12 collision size, often much smaller than what its
     * model draws (parasites, bosses), so with those mods installed legacy mobs were culled or half-drawn (#110). Their own
     * opt-out is the vanilla {@code noCulling} flag, which both read (Sodium's hook sits inside the frustum test that
     * {@code noCulling} skips; Entity Culling's {@code ignoresCulling} returns the flag). Without those mods nothing changes.
     */
    private static final boolean CULLING_MODS = isAnyLoaded("sodium", "embeddium", "entityculling");

    private static boolean isAnyLoaded(String... ids) {
        try {
            for (String id : ids) if (net.neoforged.fml.ModList.get().isLoaded(id)) return true;
        } catch (RuntimeException | LinkageError e) {
            // no mod list yet: treat as absent
        }
        return false;
    }

    @Override
    public boolean shouldRender(Entity entity, net.minecraft.client.renderer.culling.Frustum frustum, double camX, double camY, double camZ) {
        if (CULLING_MODS && entity instanceof Host1122) entity.noCulling = true;
        return super.shouldRender(entity, frustum, camX, camY, camZ);
    }

    /** Name tags are drawn by the legacy renderer (Render.renderName). */
    @Override
    protected boolean shouldShowName(Entity entity) {
        return false;
    }

    @Override
    public void render(Entity entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        var legacy = entity instanceof Host1122 h ? h.mcc$legacy() : null;
        if (legacy != null) {
            RenderManager manager = RenderManager.mcc$instance();
            Render<legacyconnector.v1_12_2.net.minecraft.entity.Entity> renderer = manager.getEntityRenderObject(legacy);
            if (renderer != null && FAILURES.getOrDefault(renderer.getClass(), 0) < MAX_FAILURES) {
                this.shadowRadius = renderer.mcc$shadowSize();
                Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                updateView(manager, partialTicks);
                double x = Mth.lerp(partialTicks, entity.xOld, entity.getX()) - cam.x;
                double y = Mth.lerp(partialTicks, entity.yOld, entity.getY()) - cam.y;
                double z = Mth.lerp(partialTicks, entity.zOld, entity.getZ()) - cam.z;
                int overlay = entity instanceof LivingEntity l ? LivingEntityRenderer.getOverlayCoords(l, 0.0F) : OverlayTexture.NO_OVERLAY;
                RenderContext.beginWorld(pose, buffers, light, overlay);
                pose.pushPose();
                try {
                    legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator.mcc$reset();
                    pose.translate(-x, -y, -z);
                    renderer.setRenderOutlines(false);
                    renderer.doRender(legacy, x, y, z, Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()), partialTicks);
                    FAILURES.remove(renderer.getClass());
                } catch (RuntimeException | LinkageError e) {
                    int n = FAILURES.merge(renderer.getClass(), 1, Integer::sum);
                    RenderErrors1122.report("running the entity renderer " + renderer.getClass().getName(), e);
                    if (n == MAX_FAILURES) LoggerFactory.getLogger("LegacyConnector/Render1122").error("Legacy entity renderer {} keeps failing and is disabled", renderer.getClass().getName());
                } finally {
                    legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator.mcc$reset();
                    pose.popPose();
                    RenderContext.end();
                }
            }
        }
        super.render(entity, yaw, partialTicks, pose, buffers, light);
    }

    /** RenderManager's view of the frame (1.12 cached it in cacheActiveRenderInfo): camera, angles, viewer, pointed entity, world. */
    public static void updateView(RenderManager manager, float partialTicks) {
        var mc = Minecraft.getInstance();
        var camera = mc.gameRenderer.getMainCamera();
        Vec3 cam = camera.getPosition();
        manager.playerViewY = camera.getYRot();
        manager.playerViewX = camera.getXRot();
        if (mc.options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_FRONT) manager.playerViewY += 180.0F;
        manager.viewerPosX = cam.x;
        manager.viewerPosY = cam.y;
        manager.viewerPosZ = cam.z;
        manager.setRenderPosition(cam.x, cam.y, cam.z);
        if (mc.level != null && (manager.world == null || manager.world.mcc$level() != mc.level)) manager.world = WorldBridge1122.wrap(mc.level);
        manager.renderViewEntity = camera.getEntity() == null ? null : EntityBridge1122.wrap(camera.getEntity());
        manager.pointedEntity = mc.crosshairPickEntity == null ? null : EntityBridge1122.wrap(mc.crosshairPickEntity);
        if (manager.options == null) {
            manager.options = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$gameSettings(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft());
        }
    }

    /**
     * Draws a modern entity with its modern renderer at (x, y, z) of the legacy matrix (a vanilla entity, or an item a mod draws as an
     * EntityItem), with the light and buffers of the render context.
     */
    public static void drawModern(Entity modern, double x, double y, double z, float yaw, float partialTicks) {
        PoseStack pose = RenderContext.pose();
        MultiBufferSource buffers = RenderContext.buffers();
        if (pose == null || buffers == null || modern == null) return;
        try {
            Minecraft.getInstance().getEntityRenderDispatcher().render(modern, x, y, z, yaw, partialTicks, pose, buffers, RenderContext.vertexLight());
        } catch (RuntimeException e) {
            RenderErrors1122.report("drawing the modern entity " + modern.getType(), e);
        }
    }
}
