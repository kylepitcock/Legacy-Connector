package legacyconnector.v1_20_1.net.minecraftforge.items.wrapper;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/** Forge 1.20.1's {@code RecipeWrapper}: a modifiable handler seen as a recipe input (NeoForge's wrapper takes a plain {@code IItemHandler}). */
public class RecipeWrapper extends net.neoforged.neoforge.items.wrapper.RecipeWrapper {
    protected final IItemHandlerModifiable handler;

    public RecipeWrapper(IItemHandlerModifiable inv) {
        super(inv);
        this.handler = inv;
    }

    public int getContainerSize() {
        return handler.getSlots();
    }

    public boolean isEmpty() {
        for (int i = 0; i < handler.getSlots(); i++) if (!handler.getStackInSlot(i).isEmpty()) return false;
        return true;
    }

    public void setItem(int slot, ItemStack stack) {
        handler.setStackInSlot(slot, stack);
    }

    public void clearContent() {
        for (int i = 0; i < handler.getSlots(); i++) handler.setStackInSlot(i, ItemStack.EMPTY);
    }

    public void setChanged() {
    }
}
