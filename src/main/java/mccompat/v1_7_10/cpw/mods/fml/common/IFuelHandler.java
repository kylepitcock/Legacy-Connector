package mccompat.v1_7_10.cpw.mods.fml.common;

import mccompat.v1_7_10.net.minecraft.item.ItemStack;

public interface IFuelHandler {
    int getBurnTime(ItemStack fuel);
}
