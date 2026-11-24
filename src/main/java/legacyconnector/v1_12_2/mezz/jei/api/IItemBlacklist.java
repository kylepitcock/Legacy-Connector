package legacyconnector.v1_12_2.mezz.jei.api;

/** JEI 3 API: hidden items (JEI 4: IIngredientBlacklist). Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IItemBlacklist {
    void addItemToBlacklist(legacyconnector.v1_12_2.net.minecraft.item.ItemStack itemStack);
    void removeItemFromBlacklist(legacyconnector.v1_12_2.net.minecraft.item.ItemStack itemStack);
    boolean isItemBlacklisted(legacyconnector.v1_12_2.net.minecraft.item.ItemStack itemStack);
}
