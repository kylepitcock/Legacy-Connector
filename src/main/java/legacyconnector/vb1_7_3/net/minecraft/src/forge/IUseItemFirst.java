package legacyconnector.vb1_7_3.net.minecraft.src.forge;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Forge 1.0: an item asked before the clicked block's {@code blockActivated}; true ends the click. */
public interface IUseItemFirst {
    boolean onItemUseFirst(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int side);
}
