package legacyconnector.v1_12_2.vazkii.patchouli.api;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** What a custom book component can draw with (Patchouli 1.12 API); backed by the modern component render context. */
public interface IComponentRenderContext {
    GuiScreen getGui();

    FontRenderer getFont();

    void renderItemStack(int x, int y, int mouseX, int mouseY, ItemStack stack);

    void renderIngredient(int x, int y, int mouseX, int mouseY, Ingredient ingredient);

    boolean isAreaHovered(int mouseX, int mouseY, int x, int y, int w, int h);

    void setHoverTooltip(List<String> tooltip);

    void registerButton(GuiButton button, int pageNum, Runnable onClick);

    ResourceLocation getBookTexture();

    ResourceLocation getCraftingTexture();

    int getTextColor();

    int getHeaderColor();
}
