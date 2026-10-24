package legacyconnector.v1_7_10.codechicken.nei.recipe;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.codechicken.nei.PositionedStack;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/**
 * NEI's shaped crafting handler. Mods extend it for their own shaped recipes (adding to {@link #arecipes}); the vanilla
 * recipe list itself is JEI's.
 */
public class ShapedRecipeHandler extends TemplateRecipeHandler {
    public class CachedShapedRecipe extends CachedRecipe {
        public ArrayList<PositionedStack> ingredients = new ArrayList<>();
        public PositionedStack result;

        public CachedShapedRecipe(int width, int height, Object[] items, ItemStack out) {
            result = new PositionedStack(out, 119, 24);
            setIngredients(width, height, items);
        }

        public void setIngredients(int width, int height, Object[] items) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int index = y * width + x;
                    if (index >= items.length || items[index] == null) continue;
                    PositionedStack stack = new PositionedStack(items[index], 25 + x * 18, 6 + y * 18, false);
                    stack.setMaxSize(1);
                    ingredients.add(stack);
                }
            }
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return getCycledIngredients(cycleticks / 20, ingredients);
        }

        @Override
        public PositionedStack getResult() {
            return result;
        }

        public void computeVisuals() {
            for (PositionedStack p : ingredients) p.generatePermutations();
            result.generatePermutations();
        }
    }

    @Override
    public String getRecipeName() {
        return "Shaped Crafting";
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
        // The base handler would list the vanilla recipes, which JEI already has; mods override this for theirs.
    }
}
