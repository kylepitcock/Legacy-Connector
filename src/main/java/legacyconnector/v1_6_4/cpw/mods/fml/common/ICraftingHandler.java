package legacyconnector.v1_6_4.cpw.mods.fml.common;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** FML 1.6's crafting and smelting callbacks; registered with {@code GameRegistry.registerCraftingHandler} (see {@code Crafting164}). */
public interface ICraftingHandler {
    /** A player took {@code item} out of a crafting grid; {@code craftMatrix} is the grid. */
    void onCrafting(EntityPlayer player, ItemStack item, IInventory craftMatrix);

    /** A player took {@code item} out of a furnace. */
    void onSmelting(EntityPlayer player, ItemStack item);
}
