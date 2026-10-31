package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.vertex.PoseStack;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockEntity;
import org.slf4j.LoggerFactory;

/**
 * Runs the legacy TileEntitySpecialRenderer bound to a legacy tile entity. Legacy TESRs receive camera-relative
 * coordinates and translate by them themselves; the pose is pre-translated by the inverse so the final placement
 * equals the modern block position while distance checks inside the legacy code still see real distances.
 */
public class LegacyBlockEntityRenderer implements BlockEntityRenderer<LegacyBlockEntity> {
    /** Consecutive failures per renderer class: a renderer that keeps failing is disabled, an occasional one is not. */
    private static final java.util.Map<Class<?>, Integer> FAILURES = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int MAX_FAILURES = 20;
    /** All failures per renderer class, only for logging: a renderer that fails for some blocks and works for others must not log every frame. */
    private static final java.util.Map<Class<?>, Integer> TOTAL = new java.util.concurrent.ConcurrentHashMap<>();
    /** A block entity whose render just failed is skipped for a few seconds (it would fail again every frame). */
    private static final java.util.Map<LegacyBlockEntity, Long> RETRY_AT = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private static final long RETRY_NANOS = 5_000_000_000L;
    /** Farther than this from the camera a block entity cannot be rendered where it is stored (render distance is at most 32 chunks). */
    private static final double FAR = 4096;

    @Override
    public void render(LegacyBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        TileEntity te = be.legacy();
        if (te == null) return;
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        TileEntitySpecialRenderer renderer = dispatcher.getSpecialRenderer(te);
        if (renderer == null || FAILURES.getOrDefault(renderer.getClass(), 0) >= MAX_FAILURES) return;
        Long retryAt = RETRY_AT.get(be);
        if (retryAt != null) {
            if (System.nanoTime() - retryAt < 0) return;
            RETRY_AT.remove(be);
        }
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        dispatcher.mcc$setCamera(cam.x, cam.y, cam.z);
        if (be.getLevel() != null) dispatcher.worldObj = World.mcc$wrap(be.getLevel());
        refreshEnvironment(dispatcher, Minecraft.getInstance());
        double x = be.getBlockPos().getX() - cam.x, y = be.getBlockPos().getY() - cam.y, z = be.getBlockPos().getZ() - cam.z;
        if (Math.abs(x) > FAR || Math.abs(y) > FAR || Math.abs(z) > FAR || org.kylepitcock.legacyconnector.core.VirtualLevels.isVirtual(be.getLevel())) {
            // The block is not where the camera sees it: a moving Create contraption (contraption-local position in its render world)
            // or a Sable/Create Aeronautics sub-level (stored in plot chunks ~20 million blocks out, drawn through the ship's pose).
            // The pose already places the block; its translation is the block's offset from the camera, which the legacy renderer
            // gets instead (the -x/+x round trip in float would be off by blocks at plot coordinates, and distances would be absurd).
            var m = pose.last().pose();
            x = m.m30();
            y = m.m31();
            z = m.m32();
        }
        RenderContext.beginWorld(pose, buffers, light, overlay);
        try {
            pose.pushPose();
            pose.translate(-x, -y, -z);
            renderer.renderTileEntityAt(te, x, y, z, partialTick);
            if (FAILURES.containsKey(renderer.getClass())) FAILURES.remove(renderer.getClass());
        } catch (RuntimeException | LinkageError e) {
            int n = FAILURES.merge(renderer.getClass(), 1, Integer::sum);
            RETRY_AT.put(be, System.nanoTime() + RETRY_NANOS);
            int total = TOTAL.merge(renderer.getClass(), 1, Integer::sum);
            boolean power = total == 1 || total == 10 || total == 100 || total == 1000 || total == 10000 || total == 100000;
            if (power || n == MAX_FAILURES) {
                LoggerFactory.getLogger("LegacyConnector/Render").error("Legacy renderer {} failed ({} times so far{}){}", renderer.getClass().getName(), total,
                        total > 1 ? ", later failures are not logged" : "", n >= MAX_FAILURES ? "; it keeps failing and is disabled" : "", total == 1 ? e : null);
            }
        } finally {
            RenderContext.end();
        }
    }

    /** The viewer and settings legacy renderers read from the dispatchers (rotationYaw of the camera entity, fancyGraphics). */
    private static void refreshEnvironment(TileEntityRendererDispatcher dispatcher, Minecraft mc) {
        var camera = mc.getCameraEntity();
        if (camera instanceof net.minecraft.world.entity.LivingEntity living) {
            var view = (legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase) org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(living);
            dispatcher.field_147551_g = view;
            dispatcher.field_147562_h = camera.getYRot();
            dispatcher.field_147563_i = camera.getXRot();
            var manager = legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderManager.instance;
            manager.livingPlayer = view;
            manager.playerViewY = camera.getYRot();
            manager.playerViewX = camera.getXRot();
            manager.viewerPosX = dispatcher.field_147560_j;
            manager.viewerPosY = dispatcher.field_147561_k;
            manager.viewerPosZ = dispatcher.field_147558_l;
        }
        var manager = legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderManager.instance;
        if (manager.options == null) manager.options = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.mcc$get$gameSettings(legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft());
    }

    @Override
    public boolean shouldRenderOffScreen(LegacyBlockEntity be) {
        return true;
    }
}
