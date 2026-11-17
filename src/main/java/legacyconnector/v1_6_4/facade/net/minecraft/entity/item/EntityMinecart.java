package legacyconnector.v1_6_4.facade.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/**
 * Facade of the 1.7.10 shim's {@code EntityMinecart} for 1.4.7 and 1.5.2 mods: the base minecart implemented {@code IInventory} then (the
 * chest cart's 27 slots; the others had none), 1.7.10 only gives them to the chest-like subclasses. See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class EntityMinecart {
    private EntityMinecart() {}

    public static int getSizeInventory(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart self) {
        return self instanceof IInventory inventory ? inventory.getSizeInventory() : 0;
    }

    public static ItemStack getStackInSlot(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart self, int slot) {
        return self instanceof IInventory inventory && slot >= 0 && slot < inventory.getSizeInventory() ? inventory.getStackInSlot(slot) : null;
    }
}
