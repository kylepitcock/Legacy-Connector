package legacyconnector.v1_12_2.mezz.jei.api.recipe;

/** JEI 4 (Minecraft 1.12.2) API: item stack utilities. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IStackHelper {
    java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> getSubtypes(legacyconnector.v1_12_2.net.minecraft.item.ItemStack itemStack);
    java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> getAllSubtypes(Iterable<?> stacks);
    java.util.List<java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack>> expandRecipeItemStackInputs(java.util.List<?> inputs);
    java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> toItemStackList(Object stacks);
    boolean isEquivalent(legacyconnector.v1_12_2.net.minecraft.item.ItemStack lhs, legacyconnector.v1_12_2.net.minecraft.item.ItemStack rhs);
    String getUniqueIdentifierForStack(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack);
}
