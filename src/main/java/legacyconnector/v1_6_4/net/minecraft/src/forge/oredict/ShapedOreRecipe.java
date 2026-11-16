package legacyconnector.v1_6_4.net.minecraft.src.forge.oredict;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Forge 3.x's shaped recipe with ore dictionary names as ingredients: the shim's, with the 1.2.x constructor. */
public class ShapedOreRecipe extends legacyconnector.v1_7_10.net.minecraftforge.oredict.ShapedOreRecipe {
    public ShapedOreRecipe(ItemStack result, Object... recipe) {
        super(result, recipe);
    }
}
