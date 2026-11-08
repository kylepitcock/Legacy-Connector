package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side signature changes of Minecraft 1.20.1 to 1.21.1 (see {@code mc-calls.tsv}, {@code mc-fields.tsv}): the player skin record, nine-slice blits that
 * 1.21.1 replaced with GUI sprites, vertex format elements, multi-line labels and the checkbox builder. Kept apart from {@link MiddleCompat} so the common
 * helpers never link client classes.
 */
public final class MiddleClientCompat {
    private MiddleClientCompat() {}

    // ------------------------------------------------------------------ player skins (PlayerSkin record)

    public static ResourceLocation getSkinLocation(PlayerInfo info) {
        return info.getSkin().texture();
    }

    public static String getModelName(PlayerInfo info) {
        return info.getSkin().model().id();
    }

    public static ResourceLocation getCapeLocation(PlayerInfo info) {
        return info.getSkin().capeTexture();
    }

    public static ResourceLocation getElytraLocation(PlayerInfo info) {
        return info.getSkin().elytraTexture();
    }

    public static boolean isSkinLoaded(PlayerInfo info) {
        return true;
    }

    public static String getModelName(AbstractClientPlayer player) {
        return player.getSkin().model().id();
    }

    public static ResourceLocation getCloakTextureLocation(AbstractClientPlayer player) {
        return player.getSkin().capeTexture();
    }

    public static boolean isCapeLoaded(AbstractClientPlayer player) {
        return player.getSkin().capeTexture() != null;
    }

    public static ResourceLocation getElytraTextureLocation(AbstractClientPlayer player) {
        return player.getSkin().elytraTexture();
    }

    public static boolean isElytraLoaded(AbstractClientPlayer player) {
        return player.getSkin().elytraTexture() != null;
    }

    // ------------------------------------------------------------------ nine-slice blits (1.21.1 draws GUI sprites instead)

    private static void blit(GuiGraphics g, ResourceLocation tex, int x, int y, int u, int v, int w, int h, int tw, int th) {
        g.blit(tex, x, y, (float) u, (float) v, w, h, tw, th);
    }

    public static void blitRepeating(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int u, int v, int rw, int rh) {
        blitRepeating(g, tex, x, y, w, h, u, v, rw, rh, 256, 256);
    }

    public static void blitRepeating(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int u, int v, int rw, int rh, int tw, int th) {
        if (rw <= 0 || rh <= 0) return;
        int px = x;
        for (int ix; px < x + w; px += ix) {
            ix = Math.min(rw, x + w - px);
            int py = y;
            for (int iy; py < y + h; py += iy) {
                iy = Math.min(rh, y + h - py);
                blit(g, tex, px, py, u, v, ix, iy, tw, th);
            }
        }
    }

    public static void blitNineSliced(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int corner, int rw, int rh, int u, int v) {
        blitNineSlicedSized(g, tex, x, y, w, h, corner, corner, corner, corner, rw, rh, u, v, 256, 256);
    }

    public static void blitNineSliced(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int cw, int ch, int rw, int rh, int u, int v) {
        blitNineSlicedSized(g, tex, x, y, w, h, cw, ch, cw, ch, rw, rh, u, v, 256, 256);
    }

    public static void blitNineSliced(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int left, int top, int right, int bottom, int rw, int rh,
            int u, int v) {
        blitNineSlicedSized(g, tex, x, y, w, h, left, top, right, bottom, rw, rh, u, v, 256, 256);
    }

    public static void blitNineSlicedSized(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int corner, int rw, int rh, int u, int v, int tw, int th) {
        blitNineSlicedSized(g, tex, x, y, w, h, corner, corner, corner, corner, rw, rh, u, v, tw, th);
    }

    public static void blitNineSlicedSized(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int cw, int ch, int rw, int rh, int u, int v, int tw, int th) {
        blitNineSlicedSized(g, tex, x, y, w, h, cw, ch, cw, ch, rw, rh, u, v, tw, th);
    }

    /** 1.20.1's {@code GuiGraphics.blitNineSliced}: the corners as they are, the edges and the middle tiled. */
    public static void blitNineSlicedSized(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int left, int top, int right, int bottom, int rw, int rh,
            int u, int v, int tw, int th) {
        left = Math.min(left, w / 2);
        right = Math.min(right, w / 2);
        top = Math.min(top, h / 2);
        bottom = Math.min(bottom, h / 2);
        if (w == rw && h == rh) {
            blit(g, tex, x, y, u, v, w, h, tw, th);
        } else if (h == rh) {
            blit(g, tex, x, y, u, v, left, h, tw, th);
            blitRepeating(g, tex, x + left, y, w - right - left, h, u + left, v, rw - right - left, rh, tw, th);
            blit(g, tex, x + w - right, y, u + rw - right, v, right, h, tw, th);
        } else if (w == rw) {
            blit(g, tex, x, y, u, v, w, top, tw, th);
            blitRepeating(g, tex, x, y + top, w, h - bottom - top, u, v + top, rw, rh - bottom - top, tw, th);
            blit(g, tex, x, y + h - bottom, u, v + rh - bottom, w, bottom, tw, th);
        } else {
            blit(g, tex, x, y, u, v, left, top, tw, th);
            blitRepeating(g, tex, x + left, y, w - right - left, top, u + left, v, rw - right - left, top, tw, th);
            blit(g, tex, x + w - right, y, u + rw - right, v, right, top, tw, th);
            blit(g, tex, x, y + h - bottom, u, v + rh - bottom, left, bottom, tw, th);
            blitRepeating(g, tex, x + left, y + h - bottom, w - right - left, bottom, u + left, v + rh - bottom, rw - right - left, bottom, tw, th);
            blit(g, tex, x + w - right, y + h - bottom, u + rw - right, v + rh - bottom, right, bottom, tw, th);
            blitRepeating(g, tex, x, y + top, left, h - bottom - top, u, v + top, left, rh - bottom - top, tw, th);
            blitRepeating(g, tex, x + left, y + top, w - right - left, h - bottom - top, u + left, v + top, rw - right - left, rh - bottom - top, tw, th);
            blitRepeating(g, tex, x + w - right, y + top, left, h - bottom - top, u + rw - right, v + top, right, rh - bottom - top, tw, th);
        }
    }

    // ------------------------------------------------------------------ vertex format elements (looked up by constant now)

    public static VertexFormatElement elementPosition() {
        return VertexFormatElement.POSITION;
    }

    public static VertexFormatElement elementColor() {
        return VertexFormatElement.COLOR;
    }

    public static VertexFormatElement elementUv0() {
        return VertexFormatElement.UV0;
    }

    public static VertexFormatElement elementUv() {
        return VertexFormatElement.UV;
    }

    public static VertexFormatElement elementUv1() {
        return VertexFormatElement.UV1;
    }

    public static VertexFormatElement elementUv2() {
        return VertexFormatElement.UV2;
    }

    public static VertexFormatElement elementNormal() {
        return VertexFormatElement.NORMAL;
    }

    // ------------------------------------------------------------------ vertex formats, quads, entity previews, models

    public static com.google.common.collect.ImmutableList<VertexFormatElement> getElements(com.mojang.blaze3d.vertex.VertexFormat format) {
        return com.google.common.collect.ImmutableList.copyOf(format.getElements());
    }

    public static int getIntegerSize(com.mojang.blaze3d.vertex.VertexFormat format) {
        return format.getVertexSize() / 4;
    }

    /** {@code VertexFormat.getOffset(int index)}: the byte offset of the index-th element. */
    public static int getOffset(com.mojang.blaze3d.vertex.VertexFormat format, int index) {
        return format.getOffset(format.getElements().get(index));
    }

    /** {@code new VertexFormat(ImmutableMap<String, VertexFormatElement>)}. */
    public static com.mojang.blaze3d.vertex.VertexFormat newVertexFormat(com.google.common.collect.ImmutableMap<String, VertexFormatElement> elements) {
        com.mojang.blaze3d.vertex.VertexFormat.Builder builder = com.mojang.blaze3d.vertex.VertexFormat.builder();
        elements.forEach(builder::add);
        return builder.build();
    }

    /** {@code consumer.putBulkData(pose, quad, r, g, b, light, overlay)}: fully opaque. */
    public static void putBulkData(com.mojang.blaze3d.vertex.VertexConsumer consumer, com.mojang.blaze3d.vertex.PoseStack.Pose pose,
            net.minecraft.client.renderer.block.model.BakedQuad quad, float r, float g, float b, int light, int overlay) {
        consumer.putBulkData(pose, quad, r, g, b, 1.0F, light, overlay);
    }

    public static int vertexElementTypeSize(VertexFormatElement.Type type) {
        return type.size();
    }

    /** {@code MultiPlayerGameMode.getPickRange()}. */
    public static float getPickRange(net.minecraft.client.multiplayer.MultiPlayerGameMode mode) {
        var player = Minecraft.getInstance().player;
        return player == null ? 4.5F : (float) player.blockInteractionRange();
    }

    /** {@code InventoryScreen.renderEntityInInventoryFollowsMouse(g, x, y, scale, mouseX, mouseY, entity)}: x and y were the feet, the mouse an offset. */
    public static void renderEntityInInventoryFollowsMouse(GuiGraphics g, int x, int y, int scale, float mouseX, float mouseY,
            net.minecraft.world.entity.LivingEntity entity) {
        int centerY = y - Math.round(entity.getBbHeight() * scale / 2.0F);
        net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(g, x - 1, centerY - 1, x + 1, centerY + 1, scale, 0.0625F,
                x - mouseX, centerY - mouseY, entity);
    }

    public static ResourceLocation getDefaultSkin() {
        return net.minecraft.client.resources.DefaultPlayerSkin.getDefaultTexture();
    }

    public static String getSkinModelName(java.util.UUID id) {
        return net.minecraft.client.resources.DefaultPlayerSkin.get(id).model().id();
    }

    public static void moveCursorToStart(net.minecraft.client.gui.components.EditBox box) {
        box.moveCursorToStart(false);
    }

    public static void putNonPlayerEntity(net.minecraft.client.multiplayer.ClientLevel level, int id, net.minecraft.world.entity.Entity entity) {
        entity.setId(id);
        level.addEntity(entity);
    }

    public static net.minecraft.core.RegistryAccess registryAccess(net.minecraft.client.multiplayer.ClientPacketListener listener) {
        var level = Minecraft.getInstance().level;
        return level != null ? level.registryAccess() : MigrateContext.registries();
    }

    /** The model location a 1.20.1 {@code new ModelResourceLocation(namespace, path, variant)} named. */
    public static ResourceLocation modelId(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static String modelNamespace(net.minecraft.client.resources.model.ModelResourceLocation location) {
        return location.id().getNamespace();
    }

    public static String modelPath(net.minecraft.client.resources.model.ModelResourceLocation location) {
        return location.id().getPath();
    }

    public static ResourceLocation guiIcons() {
        return ResourceLocation.withDefaultNamespace("textures/gui/icons.png");
    }

    // ------------------------------------------------------------------ widgets and render types

    /** {@code MultiLineLabel.create(Font, FormattedText, int width)}. */
    public static MultiLineLabel createLabel(Font font, FormattedText text, int width) {
        Component component = text instanceof Component c ? c : Component.literal(text.getString());
        return MultiLineLabel.create(font, component, width);
    }

    /** {@code label.renderCentered(g, x, y)} returned the y below the last line. */
    public static int renderCentered(MultiLineLabel label, GuiGraphics g, int x, int y) {
        label.renderCentered(g, x, y);
        return y + label.getLineCount() * 9;
    }

    public static int renderCentered(MultiLineLabel label, GuiGraphics g, int x, int y, int lineHeight, int color) {
        label.renderCentered(g, x, y, lineHeight, color);
        return y + label.getLineCount() * lineHeight;
    }

    public static int renderLeftAligned(MultiLineLabel label, GuiGraphics g, int x, int y, int lineHeight, int color) {
        label.renderLeftAligned(g, x, y, lineHeight, color);
        return y + label.getLineCount() * lineHeight;
    }

    /** {@code new Checkbox(x, y, width, height, label, selected)}. */
    public static Checkbox newCheckbox(int x, int y, int width, int height, Component label, boolean selected) {
        return Checkbox.builder(label, Minecraft.getInstance().font).pos(x, y).selected(selected).build();
    }

    public static Checkbox newCheckbox(int x, int y, int width, int height, Component label, boolean selected, boolean showLabel) {
        return Checkbox.builder(showLabel ? label : Component.empty(), Minecraft.getInstance().font).pos(x, y).selected(selected).build();
    }

    public static RenderType glintDirect() {
        return RenderType.entityGlintDirect();
    }

    public static RenderType armorGlint() {
        return RenderType.armorEntityGlint();
    }
}
