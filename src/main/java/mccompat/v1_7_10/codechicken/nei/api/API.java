package mccompat.v1_7_10.codechicken.nei.api;

import mccompat.v1_7_10.codechicken.nei.recipe.ICraftingHandler;
import mccompat.v1_7_10.codechicken.nei.recipe.IUsageHandler;
import mccompat.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import org.kylepitcock.mccompat.compat.nei.NeiBridge;

/** NEI's public registration API, recorded for the JEI bridge. GUI-overlay registrations are accepted and ignored. */
public class API {
    public static void registerRecipeHandler(ICraftingHandler handler) {
        NeiBridge.registerCraftingHandler(handler);
    }

    public static void registerUsageHandler(IUsageHandler handler) {
        NeiBridge.registerUsageHandler(handler);
    }

    public static void hideItem(ItemStack stack) {
        NeiBridge.hide(stack);
    }

    public static void hideItem(Item item) {
        if (item != null) NeiBridge.hide(new ItemStack(item, 1, 32767));
    }

    public static void addItemListEntry(ItemStack stack) {
    }

    public static void setItemListEntries(Item item, Iterable<ItemStack> entries) {
    }

    public static void setOverrideName(ItemStack stack, String name) {
    }

    public static void setMaxDamageException(int id, int maxDamage) {
    }

    public static void registerGuiOverlay(Class<? extends GuiContainer> gui, String identifier) {
    }

    public static void registerGuiOverlay(Class<? extends GuiContainer> gui, String identifier, int x, int y) {
    }

    public static void registerGuiOverlayHandler(Class<? extends GuiContainer> gui, IOverlayHandler handler, String identifier) {
    }

    public static void setGuiOffset(Class<? extends GuiContainer> gui, int x, int y) {
    }
}
