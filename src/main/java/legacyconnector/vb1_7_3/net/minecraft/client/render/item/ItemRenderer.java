package legacyconnector.vb1_7_3.net.minecraft.client.render.item;

import legacyconnector.vb1_7_3.net.minecraft.client.font.TextRenderer;
import legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;

/**
 * Beta 1.7.3's {@code RenderItem} as GUIs use it: a stack's icon ({@code renderItemIntoGUI}) and its count and wear
 * ({@code renderItemOverlayIntoGUI}) at a position of the screen, drawn by the native item renderer (a trader's goods, a citizen's tools).
 */
public class ItemRenderer {
    public ItemRenderer() {}

    /** Beta's {@code renderItemIntoGUI}. */
    public void renderGuiItem(TextRenderer textRenderer, TextureManager textureManager, ItemStack stack, int x, int y) {
        if (stack != null) GuiDrawB173.item(stack, x, y, false);
    }

    /** Beta's {@code renderItemOverlayIntoGUI}: the stack size and the damage bar. */
    public void renderGuiItemDecoration(TextRenderer textRenderer, TextureManager textureManager, ItemStack stack, int x, int y) {
        if (stack != null) GuiDrawB173.item(stack, x, y, true);
    }
}
