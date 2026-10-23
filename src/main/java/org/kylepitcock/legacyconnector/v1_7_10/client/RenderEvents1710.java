package org.kylepitcock.legacyconnector.v1_7_10.client;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderManager;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.DrawBlockHighlightEvent;
import legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderWorldLastEvent;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;

/**
 * World-render events for legacy mods: {@link RenderWorldLastEvent} once the level is drawn (overlays draw in world space
 * relative to the camera, as 1.7.10 code did with {@code RenderManager.renderPosX}) and {@link DrawBlockHighlightEvent}.
 */
public final class RenderEvents1710 {
    private RenderEvents1710() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RenderEvents1710::onStage);
        NeoForge.EVENT_BUS.addListener(RenderEvents1710::onHighlight);
    }

    private static void onStage(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !MinecraftForge.EVENT_BUS.mcc$hasListeners(RenderWorldLastEvent.class)) return;
        Minecraft mc = Minecraft.getInstance();
        var cam = e.getCamera().getPosition();
        RenderManager.renderPosX = cam.x;
        RenderManager.renderPosY = cam.y;
        RenderManager.renderPosZ = cam.z;
        var buffers = mc.renderBuffers().bufferSource();
        RenderContext.beginWorld(e.getPoseStack(), buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        try {
            MinecraftForge.EVENT_BUS.post(new RenderWorldLastEvent(legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().renderGlobal,
                    e.getPartialTick().getGameTimeDeltaPartialTick(false)));
        } catch (RuntimeException | LinkageError ex) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Render").warn("A RenderWorldLastEvent handler failed: {}", ex.toString());
        } finally {
            RenderContext.end();
            buffers.endBatch();
        }
    }

    private static void onHighlight(RenderHighlightEvent.Block e) {
        if (!MinecraftForge.EVENT_BUS.mcc$hasListeners(DrawBlockHighlightEvent.class) || Minecraft.getInstance().player == null) return;
        var hit = e.getTarget();
        var pos = hit.getBlockPos();
        var at = hit.getLocation();
        MovingObjectPosition target = new MovingObjectPosition(pos.getX(), pos.getY(), pos.getZ(), hit.getDirection().get3DDataValue(),
                Vec3.createVectorHelper(at.x, at.y, at.z));
        EntityPlayer player = (EntityPlayer) EntityBridge.wrap(Minecraft.getInstance().player);
        ItemStack held = ItemStack.mcc$wrap(Minecraft.getInstance().player.getMainHandItem());
        var legacy = new DrawBlockHighlightEvent(legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().renderGlobal, player, target, 0, held,
                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (MinecraftForge.EVENT_BUS.post(legacy)) e.setCanceled(true);
    }
}
