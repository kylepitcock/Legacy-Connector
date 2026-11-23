package legacyconnector.v1_12_2.mezz.jei.api.ingredients;

/** JEI 4 (Minecraft 1.12.2) API: the vanilla ingredient types. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public final class VanillaTypes {
    private VanillaTypes() {}

    public static final legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> ITEM = () -> legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class;
    public static final legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack> FLUID = () -> legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack.class;
    public static final legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentData> ENCHANT = () -> legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentData.class;
}
