package legacyconnector.v1_7_10.net.minecraftforge.oredict;

import java.util.Comparator;
import legacyconnector.v1_7_10.net.minecraft.item.crafting.IRecipe;

/**
 * Forge's recipe ordering. Modern recipes are data-driven and matched by type, so registrations are only accepted:
 * the order legacy mods ask for has no observable effect.
 */
public class RecipeSorter implements Comparator<IRecipe> {
    public enum Category {
        UNKNOWN, SHAPELESS, SHAPED
    }

    public static final RecipeSorter INSTANCE = new RecipeSorter();

    public static void register(String name, Class<?> recipe, Category category, String dependencies) {
    }

    public static void sortCraftManager() {
    }

    @Override
    public int compare(IRecipe a, IRecipe b) {
        return 0;
    }
}
