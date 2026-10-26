package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** Burn time of items a mod makes into furnace fuel (0 when the handler does not know the item). */
public interface IFuelHandler {
    int getBurnTime(ItemStack fuel);
}
