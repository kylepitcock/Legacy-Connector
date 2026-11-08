package legacyconnector.vb1_7_3.net.minecraft.screen;

import java.util.List;

import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;

/** Beta 1.7.3 {@code ICrafting}: who a container reports slot changes and progress values to. */
public interface ScreenHandlerListener {
    void onContentsUpdate(ScreenHandler handler, List<?> stacks);

    void onSlotUpdate(ScreenHandler handler, int slot, ItemStack stack);

    /** MCP {@code updateCraftingInventoryInfo}: a progress value (furnace cook time, ...). */
    void onPropertyUpdate(ScreenHandler handler, int id, int value);
}
