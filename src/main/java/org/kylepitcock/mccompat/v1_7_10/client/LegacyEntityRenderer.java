package org.kylepitcock.mccompat.v1_7_10.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mccompat.v1_7_10.net.minecraft.client.renderer.entity.Render;
import mccompat.v1_7_10.net.minecraft.client.renderer.entity.RenderManager;
import mccompat.v1_7_10.net.minecraft.world.World;
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
import org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.mccompat.v1_7_10.entity.LegacyHost;
import org.slf4j.LoggerFactory;

/**
 * Renderer of legacy mod entities: runs the legacy {@code Render} registered for the entity's class. Legacy
 * renderers get camera-relative coordinates and translate by them themselves, so the pose (already at the entity)
 * is pre-translated by the inverse. Name tags and shadows are drawn by the modern renderer.
 */
public class LegacyEntityRenderer extends EntityRenderer<Entity> {
    private static final ResourceLocation NONE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final Set<Class<?>> FAILED = ConcurrentHashMap.newKeySet();

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
        if (renderer != null && !FAILED.contains(renderer.getClass())) {
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
            } catch (RuntimeException | LinkageError e) {
                FAILED.add(renderer.getClass());
                LoggerFactory.getLogger("Retrofit/Render").error("Legacy entity renderer {} failed and is disabled", renderer.getClass().getName(), e);
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
        if (mc.player != null && EntityBridge.wrap(mc.player) instanceof mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase p) rm.livingPlayer = p;
    }
}
