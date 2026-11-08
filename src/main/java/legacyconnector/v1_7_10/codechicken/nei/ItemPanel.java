package legacyconnector.v1_7_10.codechicken.nei;

import legacyconnector.v1_7_10.codechicken.lib.vec.Rectangle4i;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** NEI's item list panel; the bridge shows JEI's instead, so it holds no slots. */
public class ItemPanel {
    public static class ItemPanelSlot {
        public ItemStack item;
        public int slotIndex;

        public ItemPanelSlot(ItemStack item, int slotIndex) {
            this.item = item;
            this.slotIndex = slotIndex;
        }
    }

    public ItemStack draggedStack;

    public ItemPanelSlot getSlotMouseOver(int mousex, int mousey) {
        return null;
    }

    public Rectangle4i getSlotRect(int slotIndex) {
        return new Rectangle4i(0, 0, 0, 0);
    }

    public int getPage() {
        return 0;
    }
}
