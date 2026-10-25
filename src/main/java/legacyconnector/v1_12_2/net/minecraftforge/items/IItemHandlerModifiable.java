package legacyconnector.v1_12_2.net.minecraftforge.items;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

public interface IItemHandlerModifiable extends IItemHandler {
    void setStackInSlot(int slot, ItemStack stack);
}
