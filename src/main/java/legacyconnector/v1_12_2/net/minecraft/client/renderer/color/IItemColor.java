package legacyconnector.v1_12_2.net.minecraft.client.renderer.color;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** The tint of an item's quads with a tint index. */
public interface IItemColor {
    int colorMultiplier(ItemStack stack, int tintIndex);
}
