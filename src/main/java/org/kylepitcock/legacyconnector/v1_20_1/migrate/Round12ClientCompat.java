package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-side helpers of gap round 12 (1.20.1 mods on 1.21.1). Reached from {@code mc-calls.tsv}, {@code mc-fields.tsv} and {@code mc-classes.tsv}. */
public final class Round12ClientCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("legacyconnector/1.20.1");

    private Round12ClientCompat() {}

    /** {@code MultipartModelData.builder()} (Forge 1.20.1): see {@link LegacyMultipartBuilder}. */
    public static LegacyMultipartBuilder multipartBuilder() {
        return new LegacyMultipartBuilder();
    }

    // ---- text fields: the "select" flag became a parameter ------------------------------------------------------------------------------------

    /** {@code EditBox.moveCursorToEnd()} (1.20.1): 1.21.1 takes whether the selection extends; the old call never extended it. */
    public static void moveCursorToEnd(EditBox box) {
        box.moveCursorToEnd(false);
    }

    /** {@code EditBox.moveCursor(delta)} (1.20.1): see {@link #moveCursorToEnd}. */
    public static void moveCursor(EditBox box, int delta) {
        box.moveCursor(delta, false);
    }

    // ---- the widgets texture -------------------------------------------------------------------------------------------------------------------

    private static final ResourceLocation WIDGETS = ResourceLocation.withDefaultNamespace("textures/gui/legacy_widgets.png");
    private static boolean widgetsRegistered;

    /**
     * {@code AbstractWidget.WIDGETS_LOCATION} (1.20.1: {@code textures/gui/widgets.png}; 1.21.1 draws widgets from GUI sprites and has no such file). The location
     * answered is a texture the layer composes once from the 1.21.1 sprites at the places the old file had them (hotbar at 0,0 and its selection at 0,22; the
     * disabled, normal and highlighted button rows of 200x20 at y 46, 66 and 86), so a mod that blits its own button backgrounds from it still sees buttons.
     * Regions of the old file with no sprite counterpart stay transparent.
     */
    public static synchronized ResourceLocation widgetsLocation() {
        if (!widgetsRegistered) {
            widgetsRegistered = true;
            Runnable register = () -> {
                try {
                    Minecraft mc = Minecraft.getInstance();
                    NativeImage image = new NativeImage(256, 256, true);
                    paste(mc, image, "widget/button_disabled", 0, 46);
                    paste(mc, image, "widget/button", 0, 66);
                    paste(mc, image, "widget/button_highlighted", 0, 86);
                    paste(mc, image, "hud/hotbar", 0, 0);
                    paste(mc, image, "hud/hotbar_selection", 0, 22);
                    mc.getTextureManager().register(WIDGETS, new DynamicTexture(image));
                } catch (Exception | LinkageError e) {
                    LOGGER.warn("could not compose the legacy widgets texture: {}", e.toString());
                }
            };
            if (RenderSystem.isOnRenderThread()) register.run();
            else RenderSystem.recordRenderCall(register::run);
        }
        return WIDGETS;
    }

    private static void paste(Minecraft mc, NativeImage target, String sprite, int x, int y) {
        ResourceLocation file = ResourceLocation.withDefaultNamespace("textures/gui/sprites/" + sprite + ".png");
        try (InputStream in = mc.getResourceManager().getResourceOrThrow(file).open(); NativeImage src = NativeImage.read(in)) {
            for (int j = 0; j < src.getHeight() && y + j < target.getHeight(); j++) {
                for (int i = 0; i < src.getWidth() && x + i < target.getWidth(); i++) target.setPixelRGBA(x + i, y + j, src.getPixelRGBA(i, j));
            }
        } catch (Exception e) {
            LOGGER.debug("no sprite {} for the legacy widgets texture: {}", sprite, e.toString());
        }
    }

    // ---- renderers --------------------------------------------------------------------------------------------------------------------------

    /** The partial tick 1.21.1's {@code renderNameTag} takes last (1.20.1's did not): the one of the frame being drawn. */
    public static float partialTick(Entity ignored) {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }

    /**
     * {@code ItemRenderer.getCompassFoilBufferDirect(source, type, pose)}. APPROXIMATION: 1.21.1 only has the non-direct compass glint; the direct variant (the
     * glint drawn at once, for fabulous graphics) draws through the normal one.
     */
    public static VertexConsumer getCompassFoilBufferDirect(MultiBufferSource source, RenderType type, PoseStack.Pose pose) {
        return ItemRenderer.getCompassFoilBuffer(source, type, pose);
    }

    // ---- selection lists: the list draws its own background now -----------------------------------------------------------------------------

    /** {@code AbstractSelectionList.setRenderBackground(boolean)} (1.20.1): 1.21.1 lists always draw their own menu background. No-op: APPROXIMATION. */
    public static void setRenderBackground(AbstractSelectionList<?> list, boolean render) {}

    /** {@code AbstractSelectionList.setRenderSelection(boolean)} (1.20.1): 1.21.1 lists always draw the selection box. No-op: APPROXIMATION. */
    public static void setRenderSelection(AbstractSelectionList<?> list, boolean render) {}

    /** {@code AbstractSelectionList.setRenderTopAndBottom(boolean)} (1.20.1): 1.21.1 lists have header and footer separators of their own. No-op: APPROXIMATION. */
    public static void setRenderTopAndBottom(AbstractSelectionList<?> list, boolean render) {}

    /** {@code AbstractSelectionList.updateSize(width, height, top, bottom)} (1.20.1): the list is as wide as the screen, {@code bottom - top} high, starting at {@code top}. */
    public static void updateSize(AbstractSelectionList<?> list, int width, int height, int top, int bottom) {
        list.updateSizeAndPosition(width, bottom - top, top);
    }

    /** The three colour floats of 1.20.1's layer renderers as the opaque ARGB int 1.21.1's take. */
    public static int rgb(float r, float g, float b) {
        return FastColor.ARGB32.colorFromFloat(1.0F, r, g, b);
    }
}
