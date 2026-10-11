package org.kylepitcock.mccompat.v1_7_10.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashSet;
import java.util.Set;
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
    private static final Set<Class<?>> FAILED = new HashSet<>();

    @Override
    public void render(LegacyBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        TileEntity te = be.legacy();
        if (te == null) return;
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        TileEntitySpecialRenderer renderer = dispatcher.getSpecialRenderer(te);
        if (renderer == null || FAILED.contains(renderer.getClass())) return;
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        dispatcher.mcc$setCamera(cam.x, cam.y, cam.z);
        if (be.getLevel() != null) dispatcher.worldObj = World.mcc$wrap(be.getLevel());
        double x = be.getBlockPos().getX() - cam.x, y = be.getBlockPos().getY() - cam.y, z = be.getBlockPos().getZ() - cam.z;
        RenderContext.beginWorld(pose, buffers, light, overlay);
        try {
            pose.pushPose();
            pose.translate(-x, -y, -z);
            renderer.renderTileEntityAt(te, x, y, z, partialTick);
        } catch (RuntimeException | LinkageError e) {
            FAILED.add(renderer.getClass());
            LoggerFactory.getLogger("Retrofit/Render").error("Legacy renderer {} failed and is disabled", renderer.getClass().getName(), e);
        } finally {
            RenderContext.end();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(LegacyBlockEntity be) {
        return true;
    }
}
