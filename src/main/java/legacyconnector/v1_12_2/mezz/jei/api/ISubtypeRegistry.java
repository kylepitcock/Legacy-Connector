package legacyconnector.v1_12_2.mezz.jei.api;

/** JEI 4 (Minecraft 1.12.2) API: item subtypes. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface ISubtypeRegistry {
    void useNbtForSubtypes(legacyconnector.v1_12_2.net.minecraft.item.Item... items);
    void registerSubtypeInterpreter(legacyconnector.v1_12_2.net.minecraft.item.Item item, ISubtypeInterpreter interpreter);
    ISubtypeInterpreter getSubtypeInterpreter(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack);
    String getSubtypeInfo(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack);
    boolean hasSubtypeInterpreter(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack);

    @FunctionalInterface
    interface ISubtypeInterpreter {
        String NONE = "";
        String apply(legacyconnector.v1_12_2.net.minecraft.item.ItemStack itemStack);
    }
}
