package org.kylepitcock.mccompat.v1_7_10.client;

import com.mojang.blaze3d.vertex.PoseStack;
import mccompat.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import mccompat.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import mccompat.v1_7_10.net.minecraft.tileentity.TileEntity;
import mccompat.v1_7_10.net.minecraft.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyBlockEntity;
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

    @Override
    public void render(LegacyBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        TileEntity te = be.legacy();
        if (te == null) return;
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        TileEntitySpecialRenderer renderer = dispatcher.getSpecialRenderer(te);
        if (renderer == null || FAILURES.getOrDefault(renderer.getClass(), 0) >= MAX_FAILURES) return;
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        dispatcher.mcc$setCamera(cam.x, cam.y, cam.z);
        if (be.getLevel() != null) dispatcher.worldObj = World.mcc$wrap(be.getLevel());
        double x = be.getBlockPos().getX() - cam.x, y = be.getBlockPos().getY() - cam.y, z = be.getBlockPos().getZ() - cam.z;
        RenderContext.beginWorld(pose, buffers, light, overlay);
        try {
            pose.pushPose();
            pose.translate(-x, -y, -z);
            renderer.renderTileEntityAt(te, x, y, z, partialTick);
            if (FAILURES.containsKey(renderer.getClass())) FAILURES.remove(renderer.getClass());
        } catch (RuntimeException | LinkageError e) {
            int n = FAILURES.merge(renderer.getClass(), 1, Integer::sum);
            if (n == 1 || n == MAX_FAILURES) {
                LoggerFactory.getLogger("Retrofit/Render").error("Legacy renderer {} failed{}", renderer.getClass().getName(),
                        n >= MAX_FAILURES ? " repeatedly and is disabled" : "", e);
            }
        } finally {
            RenderContext.end();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(LegacyBlockEntity be) {
        return true;
    }
}
