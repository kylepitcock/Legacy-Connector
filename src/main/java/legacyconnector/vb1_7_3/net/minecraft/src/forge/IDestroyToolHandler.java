package legacyconnector.vb1_7_3.net.minecraft.src.forge;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;

/** Forge 1.0: told when the item a player holds is used up. */
public interface IDestroyToolHandler {
    void onDestroyCurrentItem(PlayerEntity player, ItemStack original);
}
