package org.kylepitcock.legacyconnector.v1_12_2.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Runs the coremod hooks of {@code Render.renderEntityOnFire} (1.12.2) where the modern game draws the flames of a burning entity: the legacy drawing code gets
 * the legacy render context at the camera-relative position it expects.
 */
public final class EntityFireHook1122 {
    private static int failures;

    private EntityFireHook1122() {}

    /** True when a hook drew the flames (the game's own are then left out). */
    public static boolean draw(Entity entity, PoseStack pose, MultiBufferSource buffers) {
        if (!CoremodSites1122.ENTITY_FIRE.active() || failures > 5) return false;
        var legacy = EntityBridge1122.wrap(entity);
        if (legacy == null) return false;
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX()) - cam.x;
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY()) - cam.y;
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - cam.z;
        RenderContext.beginWorld(pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.pushPose();
        try {
            legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator.mcc$reset();
            pose.translate(-x, -y, -z); // the pose is at the entity; the legacy code works relative to the camera
            return CoremodSites1122.ENTITY_FIRE.cancels(legacy, x, y, z);
        } catch (RuntimeException | LinkageError e) {
            failures++;
            RenderErrors1122.report("running a coremod hook for the flames of an entity", e);
            return false;
        } finally {
            legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator.mcc$reset();
            pose.popPose();
            RenderContext.end();
        }
    }
}
