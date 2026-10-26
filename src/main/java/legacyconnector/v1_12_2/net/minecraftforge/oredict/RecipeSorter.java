package legacyconnector.v1_12_2.net.minecraftforge.oredict;

/**
 * 1.12's crafting recipe sorter (ordered recipes for the old linear recipe list). The modern recipe manager matches recipes by
 * type and never needs an order, so registrations are accepted and change nothing.
 */
public class RecipeSorter {
    public enum Category {
        UNKNOWN,
        SHAPELESS,
        SHAPED
    }

    public static void register(String name, Class<?> recipe, Category category, String dependencies) {
    }

    public static void setCategory(Class<?> recipe, Category category) {
    }

    public static void sortCraftManager() {
    }
}
