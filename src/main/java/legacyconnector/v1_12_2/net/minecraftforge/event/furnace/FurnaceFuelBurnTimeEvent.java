package legacyconnector.v1_12_2.net.minecraftforge.event.furnace;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** How long an item burns in a furnace; setting a time replaces the item's own (fed from the modern event). */
@Cancelable
public class FurnaceFuelBurnTimeEvent extends Event {
    private final ItemStack itemStack;
    private int burnTime;

    public FurnaceFuelBurnTimeEvent(ItemStack itemStack, int burnTime) {
        this.itemStack = itemStack;
        this.burnTime = burnTime;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    /** Sets the burn time (0 for none) and stops the item's own from applying. */
    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
        setCanceled(true);
    }

    public int getBurnTime() {
        return burnTime;
    }
}
