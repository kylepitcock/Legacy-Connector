package org.kylepitcock.legacyconnector.vb1_7_3.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;
import org.kylepitcock.legacyconnector.vb1_7_3.EntityRenderingB173;
import org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173;
import org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockEntity;
import org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173;

/**
 * Draws a legacy tile entity with the {@code TileEntitySpecialRenderer} its mod registered ({@code ModLoader.RegisterTileEntity(class, id,
 * renderer)}): the original {@code render(tile, x, y, z, partial)} runs against the block's origin and its model parts, texture binds
 * and transforms are replayed natively, as for an original entity renderer.
 */
public final class LegacyB173BlockEntityRendererB173 implements BlockEntityRenderer<LegacyB173BlockEntity> {
    private static final java.util.Set<Class<?>> REPORTED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final net.minecraft.client.renderer.blockentity.SignRenderer signs;
    private final java.util.Map<net.minecraft.world.level.block.entity.BlockEntity,String> signKeys=new java.util.WeakHashMap<>();
    private final java.util.Map<LegacyB173BlockEntity,net.minecraft.world.level.block.entity.BlockEntity> stand=new java.util.WeakHashMap<>();

    public LegacyB173BlockEntityRendererB173(BlockEntityRendererProvider.Context context) {
        signs=new net.minecraft.client.renderer.blockentity.SignRenderer(context);
    }

    /** A sign tile entity without a renderer of its own is drawn the way Beta's sign renderer drew it: with the native sign model for its state. (Beta chests are cubes, not tile entity models.) */
    private boolean renderBase(LegacyB173BlockEntity host, legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int meta = host.getBlockState().getValue(org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Block.META);
        var pos = host.getBlockPos();
        var level = host.getLevel();
        if (tile instanceof legacyconnector.vb1_7_3.net.minecraft.block.entity.SignBlockEntity sign && host.getBlockState().getBlock() instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Block hosted
                && hosted.legacy() instanceof legacyconnector.vb1_7_3.net.minecraft.block.SignBlock signBlock) {
            net.minecraft.world.level.block.state.BlockState state;
            if (signBlock.standing) state = net.minecraft.world.level.block.Blocks.OAK_SIGN.defaultBlockState().setValue(net.minecraft.world.level.block.StandingSignBlock.ROTATION, meta & 15);
            else {
                var facing = switch (meta) { case 3 -> net.minecraft.core.Direction.SOUTH; case 4 -> net.minecraft.core.Direction.WEST; case 5 -> net.minecraft.core.Direction.EAST; default -> net.minecraft.core.Direction.NORTH; };
                state = net.minecraft.world.level.block.Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(net.minecraft.world.level.block.WallSignBlock.FACING, facing);
            }
            var finalState = state;
            var dummy = (net.minecraft.world.level.block.entity.SignBlockEntity) stand.compute(host, (k, old) -> old instanceof net.minecraft.world.level.block.entity.SignBlockEntity s && s.getBlockState() == finalState ? s : new net.minecraft.world.level.block.entity.SignBlockEntity(pos, finalState) {
                // Beta's sign renderer drew each line whole, centred; the native one wraps a line at 90 pixels and shows only its first part
                @Override public int getMaxTextLineWidth() { return 10_000; }
            });
            dummy.setLevel(level);
            // loaded from a tag, only when the lines change: setText would mark the real client level changed every frame (a chunk rebuild each time)
            String key = String.join("\u0000", sign.texts) + "|" + finalState;
            if (!key.equals(signKeys.get(dummy))) {
                var messages = new net.minecraft.network.chat.Component[4];
                for (int i = 0; i < 4; i++) messages[i] = net.minecraft.network.chat.Component.literal(sign.texts[i] == null ? "" : sign.texts[i]);
                var text = new net.minecraft.world.level.block.entity.SignText(messages, messages, net.minecraft.world.item.DyeColor.BLACK, false);
                var tag = new net.minecraft.nbt.CompoundTag();
                net.minecraft.world.level.block.entity.SignText.DIRECT_CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, text).result().ifPresent(t -> tag.put("front_text", t));
                dummy.loadCustomOnly(tag, level.registryAccess());
                signKeys.put(dummy, key);
            }
            signs.render(dummy, partial, pose, buffers, light, overlay);
            return true;
        }
        return false;
    }

    @Override
    public void render(LegacyB173BlockEntity host, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var tile = host.legacy();
        if (tile == null || host.getLevel() == null) return;
        if (!(ContentB173.tileEntityRenderer(tile.getClass()) instanceof legacyconnector.vb1_7_3.net.minecraft.client.render.block.entity.BlockEntityRenderer renderer)) {
            try { renderBase(host, tile, partial, pose, buffers, light, overlay); }
            catch (RuntimeException | LinkageError error) {
                if (REPORTED.add(tile.getClass())) org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Render").error("Base tile entity render failed for {}", tile.getClass().getName(), error);
            }
            return;
        }
        var mod = ContentB173.ownerOf(tile.getClass());
        RenderContext.beginWorld(pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.pushPose();
        try (var readOnly = EntityViewsB173.readOnly()) {
            synchronized (renderer) {
                renderer.setWorld(tile.world);
                try (var scope = RendererContextB173.open(path -> EntityRenderingB173.texture(mod, path), (part, texture) -> {
                    NativeRendererExtrasB173.applyStyle(RendererContextB173.style());
                    int color = RendererContextB173.argb();
                    RenderContext.color(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, ((color >>> 24) & 255) / 255F);
                    RenderContext.bindTexture(texture);
                    ModelPartsB173.draw(part);
                }, EntityRenderingB173.sheets(NativeRendererExtrasB173.create(host.getLevel(), host.getBlockPos().hashCode(), pose, buffers)))) {
                    renderer.render(tile, 0, 0, 0, partial);
                }
            }
        } catch (RuntimeException | LinkageError error) {
            if (REPORTED.add(renderer.getClass()))
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Render").error("Original Beta tile entity renderer failed for {}", tile.getClass().getName(), error);
        } finally {
            pose.popPose();
            RenderContext.end();
        }
    }
}
