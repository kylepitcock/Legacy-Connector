package legacyconnector.v1_7_10.codechicken.nei.recipe;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.codechicken.nei.PositionedStack;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** NEI's shapeless crafting handler (see {@link ShapedRecipeHandler}). */
public class ShapelessRecipeHandler extends TemplateRecipeHandler {
    public class CachedShapelessRecipe extends CachedRecipe {
        public ArrayList<PositionedStack> ingredients = new ArrayList<>();
        public PositionedStack result;

        public CachedShapelessRecipe() {
        }

        public CachedShapelessRecipe(ItemStack output) {
            this();
            setResult(output);
        }

        public CachedShapelessRecipe(Object[] input, ItemStack output) {
            this(java.util.Arrays.asList(input), output);
        }

        public CachedShapelessRecipe(List<?> input, ItemStack output) {
            this(output);
            setIngredients(input);
        }

        public void setIngredients(List<?> items) {
            ingredients.clear();
            for (int ingred = 0; ingred < items.size(); ingred++) {
                if (items.get(ingred) == null) continue;
                PositionedStack stack = new PositionedStack(items.get(ingred), 25 + ingred % 3 * 18, 6 + ingred / 3 * 18);
                stack.setMaxSize(1);
                ingredients.add(stack);
            }
        }

        public void setResult(ItemStack output) {
            result = new PositionedStack(output, 119, 24);
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return getCycledIngredients(cycleticks / 20, ingredients);
        }

        @Override
        public PositionedStack getResult() {
            return result;
        }
    }

    @Override
    public String getRecipeName() {
        return "Shapeless Crafting";
    }

    @Override
    public String getGuiTexture() {
        return "textures/gui/container/crafting_table.png";
    }

    @Override
    public String getOverlayIdentifier() {
        return "crafting";
    }

    @Override
    public Class<? extends GuiContainer> getGuiClass() {
        return null;
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
    }
}
