package org.kylepitcock.legacyconnector.v1_20_1.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Client-side adapters for Palladium 4.5.x bytecode (called from the rewrites of PalladiumWidgetButtons1201). */
public final class PalladiumClientCompat {
    private PalladiumClientCompat() {
    }

    // Palladium 4.5.10 still reads Button.WIDGETS_LOCATION and supplies the old
    // widgets.png row offset. In 1.21.1 these are GUI-atlas sprites whose
    // nine-slice metadata defines the border scaling.
    private static final ResourceLocation PALLADIUM_BUTTON =
            ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation PALLADIUM_BUTTON_DISABLED =
            ResourceLocation.withDefaultNamespace("widget/button_disabled");
    private static final ResourceLocation PALLADIUM_BUTTON_HIGHLIGHTED =
            ResourceLocation.withDefaultNamespace("widget/button_highlighted");

    /**
     * Draws Palladium's old default button background using 1.21.1's GUI atlas.
     * {@code legacyTexture}, the old UV origin and the old slice dimensions are
     * retained in the descriptor so the bytecode adapter can replace the
     * existing call without changing its operand stack shape.
     *
     * The old texture's state rows were y=46 (disabled), y=66 (normal), and
     * y=86 (hovered/focused). The 1.21.1 sprites carry their own nine-slice
     * scaling metadata, so the old texture and UV/slice arguments are unused.
     */
    public static void blitPalladiumDefaultButton(GuiGraphics g, ResourceLocation legacyTexture,
            int x, int y, int width, int height, int cornerWidth, int cornerHeight,
            int regionWidth, int regionHeight, int u, int v) {
        ResourceLocation sprite = switch (v) {
            case 46 -> PALLADIUM_BUTTON_DISABLED;
            case 86 -> PALLADIUM_BUTTON_HIGHLIGHTED;
            default -> PALLADIUM_BUTTON;
        };
        g.blitSprite(sprite, x, y, width, height);
    }

}
