package org.kylepitcock.legacyconnector.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.gui.handlers.IScreenHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.vb1_7_3.client.InventoryRecipeAreaB173;
import org.kylepitcock.legacyconnector.vb1_7_3.client.OriginalInventoryScreenB173;

/**
 * JEI's "show recipes" hotspot of the player inventory is fixed at the vanilla crafting arrow. When an original inventory layout moves
 * the crafting grid, the screen's reported corner is shifted so the hotspot lands on the layout's arrow; the right and bottom edges stay.
 */
@JeiPlugin
public final class LegacyInventoryJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("legacyconnector", "inventory_layout");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(InventoryScreen.class, (IScreenHandler<InventoryScreen>) screen -> {
            if (OriginalInventoryScreenB173.original(screen) == null) return null;
            var slots = screen.getMenu().slots;
            if (slots.size() < 5) return null;
            int[][] grid = new int[4][];
            for (int i = 0; i < 4; i++) grid[i] = new int[] {slots.get(1 + i).x, slots.get(1 + i).y};
            int[] shift = InventoryRecipeAreaB173.shift(grid, new int[] {slots.get(0).x, slots.get(0).y});
            if (shift[0] == 0 && shift[1] == 0) return null;
            int left = screen.getGuiLeft() + shift[0], top = screen.getGuiTop() + shift[1];
            int right = screen.getGuiLeft() + screen.getXSize(), bottom = screen.getGuiTop() + screen.getYSize();
            return new IGuiProperties() {
                @Override public Class<? extends net.minecraft.client.gui.screens.Screen> screenClass() { return screen.getClass(); }
                @Override public int guiLeft() { return left; }
                @Override public int guiTop() { return top; }
                @Override public int guiXSize() { return right - left; }
                @Override public int guiYSize() { return bottom - top; }
                @Override public int screenWidth() { return screen.width; }
                @Override public int screenHeight() { return screen.height; }
            };
        });
    }
}
