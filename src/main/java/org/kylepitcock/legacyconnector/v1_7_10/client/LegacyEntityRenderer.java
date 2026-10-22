package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.Render;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderManager;
import legacyconnector.v1_7_10.net.minecraft.world.World;
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
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHost;
import org.slf4j.LoggerFactory;

/**
 * Renderer of legacy mod entities: runs the legacy {@code Render} registered for the entity's class. Legacy
 * renderers get camera-relative coordinates and translate by them themselves, so the pose (already at the entity)
 * is pre-translated by the inverse. Name tags and shadows are drawn by the modern renderer.
 */
public class LegacyEntityRenderer extends EntityRenderer<Entity> {
    private static final ResourceLocation NONE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final java.util.Map<Class<?>, Integer> FAILURES = new ConcurrentHashMap<>();
    private static final int MAX_FAILURES = 20;

    public LegacyEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(Entity entity) {
        return NONE;
    }

    @Override
    public void render(Entity entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        var legacy = entity instanceof LegacyHost h ? h.mcc$legacy() : null;
        Render renderer = legacy == null ? null : RenderManager.instance.getEntityRenderObject(legacy);
        if (renderer != null && FAILURES.getOrDefault(renderer.getClass(), 0) < MAX_FAILURES) {
            this.shadowRadius = renderer.mcc$shadowSize();
            Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            updateRenderManager(cam, entity);
            double x = Mth.lerp(partialTicks, entity.xOld, entity.getX()) - cam.x;
            double y = Mth.lerp(partialTicks, entity.yOld, entity.getY()) - cam.y;
            double z = Mth.lerp(partialTicks, entity.zOld, entity.getZ()) - cam.z;
            int overlay = entity instanceof LivingEntity l ? LivingEntityRenderer.getOverlayCoords(l, 0.0F) : OverlayTexture.NO_OVERLAY;
            RenderContext.beginWorld(pose, buffers, light, overlay);
            pose.pushPose();
            try {
                pose.translate(-x, -y, -z);
                renderer.doRender(legacy, x, y, z, Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()), partialTicks);
                if (FAILURES.containsKey(renderer.getClass())) FAILURES.remove(renderer.getClass());
            } catch (RuntimeException | LinkageError e) {
                int n = FAILURES.merge(renderer.getClass(), 1, Integer::sum);
                if (n == 1 || n == MAX_FAILURES) {
                    LoggerFactory.getLogger("LegacyConnector/Render").error("Legacy entity renderer {} failed{}", renderer.getClass().getName(),
                            n >= MAX_FAILURES ? " repeatedly and is disabled" : "", e);
                }
            } finally {
                pose.popPose();
                RenderContext.end();
            }
        }
        super.render(entity, yaw, partialTicks, pose, buffers, light);
    }

    private static void updateRenderManager(Vec3 cam, Entity entity) {
        RenderManager rm = RenderManager.instance;
        var mc = Minecraft.getInstance();
        var camera = mc.gameRenderer.getMainCamera();
        rm.playerViewY = camera.getYRot();
        rm.playerViewX = camera.getXRot();
        rm.viewerPosX = cam.x;
        rm.viewerPosY = cam.y;
        rm.viewerPosZ = cam.z;
        RenderManager.renderPosX = cam.x;
        RenderManager.renderPosY = cam.y;
        RenderManager.renderPosZ = cam.z;
        if (mc.level != null) rm.worldObj = World.mcc$wrap(mc.level);
        if (mc.player != null && EntityBridge.wrap(mc.player) instanceof legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase p) rm.livingPlayer = p;
    }
}
