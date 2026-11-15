package legacyconnector.vb1_7_3.net.minecraft.src.forge;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;

/** Forge 1.0: told when a player takes a crafting result, before the ingredients are used up. */
public interface ICraftingHandler {
    void onTakenFromCrafting(PlayerEntity player, ItemStack result, Inventory craftMatrix);
}
