package org.kylepitcock.legacyconnector.vb1_7_3.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;
import org.kylepitcock.legacyconnector.vb1_7_3.EntityRenderingB173;
import org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173;
import org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Block;
import org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173;
import org.kylepitcock.legacyconnector.vb1_7_3.StacksB173;

/**
 * Forge 1.0's block highlight handlers ({@code MinecraftForgeClient.registerHighlightHandler}): asked before the outline of the targeted
 * block is drawn, with the target as Beta's raytrace made it (a multipart block's part in {@code subHit}). What a handler draws (a cover's
 * placement preview) is drawn in the camera space Beta used; a handler that answers true replaces the outline unless it drew the standard one.
 * The mining overlay of a part is the native destroy overlay over the part's shape, so handlers see no mining progress.
 */
final class HighlightClientB173 {
    private static final java.util.Set<Class<?>> REPORTED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static boolean installed;

    private HighlightClientB173() {}

    static synchronized void install() {
        if (installed) return;
        installed = true;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(HighlightClientB173::onHighlight);
    }

    private static void onHighlight(RenderHighlightEvent.Block event) {
        var handlers = legacyconnector.vb1_7_3.net.minecraft.src.forge.MinecraftForgeClient.mcc$highlightHandlers();
        if (handlers.isEmpty()) return;
        var mc = Minecraft.getInstance();
        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null) return;
        var hit = event.getTarget();
        var pos = hit.getBlockPos();
        float partial = event.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        var world = legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(level);
        if (!(EntityViewsB173.wrap(player) instanceof legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity view)) return;
        var target = target(world, level, pos, hit, player, partial);
        var held = StacksB173.toLegacy(player.getMainHandItem());
        var renderer = legacyconnector.vb1_7_3.net.minecraft.client.render.WorldRenderer.mcc$instance();
        var pose = event.getPoseStack();
        var buffers = event.getMultiBufferSource();
        boolean handled = false;
        for (var handler : handlers) {
            var mod = ContentB173.ownerOf(handler.getClass());
            renderer.mcc$begin(0F);
            RenderContext.beginWorld(pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.pushPose();
            try (var readOnly = EntityViewsB173.readOnly();
                 var scope = RendererContextB173.open(path -> EntityRenderingB173.texture(mod, path), (part, texture) -> {
                     NativeRendererExtrasB173.applyStyle(RendererContextB173.style());
                     int color = RendererContextB173.argb();
                     RenderContext.color(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, ((color >>> 24) & 255) / 255F);
                     RenderContext.bindTexture(texture);
                     ModelPartsB173.draw(part);
                 }, EntityRenderingB173.sheets(NativeRendererExtrasB173.create(level, pos.hashCode(), pose, buffers)))) {
                RendererContextB173.bind("/terrain.png");
                handled = handler.onBlockHighlight(renderer, view, target, 0, held, partial);
            } catch (RuntimeException | LinkageError error) {
                if (REPORTED.add(handler.getClass()))
                    org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Render").error("Beta highlight handler {} failed", handler.getClass().getName(), error);
            } finally {
                pose.popPose();
                RenderContext.end();
            }
            if (handled) break;
        }
        if (handled && !renderer.mcc$defaultOutline()) event.setCanceled(true);
    }

    /** Beta's {@code objectMouseOver}: the target block's own raytrace along the look (a multipart block names the part), else the native hit. */
    private static legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult target(legacyconnector.vb1_7_3.net.minecraft.world.World world,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.BlockHitResult hit,
            net.minecraft.world.entity.player.Player player, float partial) {
        var location = hit.getLocation();
        var plain = new legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult(pos.getX(), pos.getY(), pos.getZ(), hit.getDirection().get3DDataValue(),
                legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d.create(location.x, location.y, location.z));
        if (!(level.getBlockState(pos).getBlock() instanceof LegacyB173Block hosted)) return plain;
        var block = hosted.legacy();
        var eye = player.getEyePosition(partial);
        var look = player.getViewVector(partial);
        double reach = player.blockInteractionRange() + 1.0;
        var from = legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d.create(eye.x, eye.y, eye.z);
        var to = legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d.create(eye.x + look.x * reach, eye.y + look.y * reach, eye.z + look.z * reach);
        synchronized (block) {
            double[] bounds = {block.minX, block.minY, block.minZ, block.maxX, block.maxY, block.maxZ};
            try {
                var own = block.raycast(world, pos.getX(), pos.getY(), pos.getZ(), from, to);
                return own == null ? plain : own;
            } catch (RuntimeException | LinkageError e) {
                return plain;
            } finally {
                block.minX = bounds[0]; block.minY = bounds[1]; block.minZ = bounds[2]; block.maxX = bounds[3]; block.maxY = bounds[4]; block.maxZ = bounds[5];
            }
        }
    }
}
