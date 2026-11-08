package legacyconnector.vb1_7_3.net.minecraft.inventory;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;

/** Beta 1.7.3 {@code IInventory}. */
public interface Inventory {
    int size();

    ItemStack getStack(int slot);

    ItemStack removeStack(int slot, int amount);

    void setStack(int slot, ItemStack stack);

    String getName();

    int getMaxCountPerStack();

    void markDirty();

    boolean canPlayerUse(PlayerEntity player);
}
