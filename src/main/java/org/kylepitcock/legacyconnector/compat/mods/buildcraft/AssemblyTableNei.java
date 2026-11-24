package org.kylepitcock.legacyconnector.compat.mods.buildcraft;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.codechicken.nei.PositionedStack;
import legacyconnector.v1_7_10.codechicken.nei.recipe.TemplateRecipeHandler;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.compat.nei.NeiBridge;

/**
 * BuildCraft (1.4.7 to 1.6.4) keeps its Assembly Table recipes in a plain list ({@code AssemblyRecipe.assemblyRecipes}: inputs, output,
 * energy) and ships no NEI plugin. This handler shows that list through the NEI -> JEI bridge, so JEI gets an Assembly Table category.
 * Mod specific on purpose (BuildCraft is popular and its list is not a registry the layer could read generically); read by reflection,
 * does nothing when the mod is absent.
 */
public final class AssemblyTableNei extends TemplateRecipeHandler {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/NEI");

    /** Called once when the NEI plugins load: registers the handler when the recipe list exists. */
    public static void register() {
        try {
            Class<?> recipe = Class.forName("buildcraft.api.recipes.AssemblyRecipe", true, Thread.currentThread().getContextClassLoader());
            if (!(recipe.getField("assemblyRecipes").get(null) instanceof List<?>)) return;
            AssemblyTableNei handler = new AssemblyTableNei();
            NeiBridge.registerCraftingHandler(handler);
            try {
                Class<?> silicon = Class.forName("buildcraft.BuildCraftSilicon", true, Thread.currentThread().getContextClassLoader());
                Object block = silicon.getField("assemblyTableBlock").get(null);
                if (block instanceof legacyconnector.v1_7_10.net.minecraft.block.Block b) NeiBridge.addCatalyst(NeiBridge.handlerId(handler), new ItemStack(b, 1, 0));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                LOG.debug("No Assembly Table catalyst: {}", e.toString());
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            // the assembly API is not loaded
        }
    }

    private static List<?> list() {
        try {
            Class<?> recipe = Class.forName("buildcraft.api.recipes.AssemblyRecipe", true, Thread.currentThread().getContextClassLoader());
            return (List<?>) recipe.getField("assemblyRecipes").get(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return List.of();
        }
    }

    private void load(ItemStack focus, boolean usage) {
        for (Object r : new ArrayList<>(list())) {
            try {
                Class<?> c = r.getClass();
                ItemStack[] in = (ItemStack[]) c.getField("input").get(r);
                ItemStack out = (ItemStack) c.getField("output").get(r);
                if (out == null) continue;
                boolean match = focus == null;
                if (!match && !usage) match = same(out, focus);
                if (!match && usage) for (ItemStack s : in) if (s != null && same(s, focus)) match = true;
                if (match) arecipes.add(new Cached(in, out));
            } catch (ReflectiveOperationException | RuntimeException e) {
                // a recipe of another shape
            }
        }
    }

    private static boolean same(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && (b.getItemDamage() == 32767 || a.getItemDamage() == 32767 || a.getItemDamage() == b.getItemDamage());
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if ("item".equals(outputId) && results.length > 0 && results[0] instanceof ItemStack s) load(s, false);
        else if (!"item".equals(outputId)) load(null, false);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        load(result, false);
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        load(ingredient, true);
    }

    @Override
    public String getRecipeName() {
        return "Assembly Table";
    }

    /** The id the bridge lists all of this handler's recipes under. */
    @Override
    public String getOverlayIdentifier() {
        return "buildcraft_assembly";
    }

    @Override
    public String getGuiTexture() {
        return null;
    }

    private final class Cached extends CachedRecipe {
        private final List<PositionedStack> inputs = new ArrayList<>();
        private final PositionedStack output;

        Cached(ItemStack[] in, ItemStack out) {
            for (int i = 0; i < in.length; i++) {
                if (in[i] != null) inputs.add(new PositionedStack(in[i].copy(), 12 + (i % 4) * 18, 18 + (i / 4) * 18, false));
            }
            output = new PositionedStack(out.copy(), 126, 28, false);
        }

        @Override
        public PositionedStack getResult() {
            return output;
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return inputs;
        }
    }
}
