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
    /** All failures per renderer class, only for logging (a renderer that works for some entities must not log every frame). */
    private static final java.util.Map<Class<?>, Integer> TOTAL = new java.util.concurrent.ConcurrentHashMap<>();

    /** Diagnostics: draws logged per renderer class (the first few show what each legacy renderer actually emitted) and entity classes without a renderer. */
    private static final java.util.Map<Class<?>, Integer> LOGGED = new ConcurrentHashMap<>();
    private static final java.util.Set<Class<?>> NO_RENDERER = ConcurrentHashMap.newKeySet();
    private static final int MAX_DRAW_LOGS = 3;

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
        if (renderer instanceof legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderModernEntity) renderer = null;
        if (renderer == null && legacy != null && NO_RENDERER.add(legacy.getClass())) {
            LoggerFactory.getLogger("LegacyConnector/Render").info("{} (legacy entity {}) has no legacy renderer registered: nothing is drawn for it", entity.getType(), legacy.getClass().getName());
        }
        if (renderer != null && FAILURES.getOrDefault(renderer.getClass(), 0) < MAX_FAILURES) {
            this.shadowRadius = renderer.mcc$shadowSize();
            Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            updateRenderManager(cam, entity);
            double x = Mth.lerp(partialTicks, entity.xOld, entity.getX()) - cam.x;
            // the legacy posY (mid-height for carts: 1.7.10 renderers draw the cart around it)
            double y = Mth.lerp(partialTicks, entity.yOld, entity.getY()) + legacy.mcc$yShift() - cam.y;
            double z = Mth.lerp(partialTicks, entity.zOld, entity.getZ()) - cam.z;
            int overlay = entity instanceof LivingEntity l ? LivingEntityRenderer.getOverlayCoords(l, 0.0F) : OverlayTexture.NO_OVERLAY;
            RenderContext.beginWorld(pose, buffers, light, overlay);
            pose.pushPose();
            try {
                pose.translate(-x, -y, -z);
                long quadsBefore = RenderContext.quadCount();
                renderer.doRender(legacy, x, y, z, Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()), partialTicks);
                int logged = LOGGED.merge(renderer.getClass(), 1, Integer::sum);
                if (logged <= MAX_DRAW_LOGS) {
                    LoggerFactory.getLogger("LegacyConnector/Render").info("Legacy renderer {} drew {} ({}) at {}, {}, {} relative to the camera: {} quads/lines emitted, texture {}",
                            renderer.getClass().getName(), legacy.getClass().getName(), entity.getType(), (int) x, (int) y, (int) z,
                            RenderContext.quadCount() - quadsBefore, RenderContext.texture());
                }
                if (FAILURES.containsKey(renderer.getClass())) FAILURES.remove(renderer.getClass());
            } catch (RuntimeException | LinkageError e) {
                int n = FAILURES.merge(renderer.getClass(), 1, Integer::sum);
                int total = TOTAL.merge(renderer.getClass(), 1, Integer::sum);
                boolean power = total == 1 || total == 10 || total == 100 || total == 1000 || total == 10000 || total == 100000;
                if (power || n == MAX_FAILURES) {
                    LoggerFactory.getLogger("LegacyConnector/Render").error("Legacy entity renderer {} failed ({} times so far{}){}", renderer.getClass().getName(), total,
                            total > 1 ? ", later failures are not logged" : "", n >= MAX_FAILURES ? "; it keeps failing and is disabled" : "", total == 1 ? e : null);
                }
            } finally {
                pose.popPose();
                RenderContext.end();
            }
        } else if (renderer != null && NO_RENDERER.add(renderer.getClass())) {
            LoggerFactory.getLogger("LegacyConnector/Render").warn("Legacy entity renderer {} is disabled after repeated failures", renderer.getClass().getName());
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
        // renderers read the view settings through it (Fisk's Heroes' web lines start at the hand in first person)
        if (rm.options == null) {
            var legacy = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft();
            rm.options = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.mcc$get$gameSettings(legacy);
        }
    }
}
