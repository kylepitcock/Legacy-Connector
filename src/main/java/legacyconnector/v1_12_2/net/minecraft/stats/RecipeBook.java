package legacyconnector.v1_12_2.net.minecraft.stats;

import java.util.BitSet;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe;

/**
 * 1.12.2's recipe book (unlocked and new recipes, whether the book is open and filtering). A view of a player's modern recipe book
 * ({@link #mcc$of}) answers from it, by recipe name; a book a mod makes keeps its own state.
 */
public class RecipeBook {
    protected final BitSet recipes = new BitSet();
    protected final BitSet newRecipes = new BitSet();
    protected boolean isGuiOpen;
    protected boolean isFilteringCraftable;
    private final net.minecraft.stats.RecipeBook mcc$modern;

    public RecipeBook() {
        this(null);
    }

    private RecipeBook(net.minecraft.stats.RecipeBook modern) {
        this.mcc$modern = modern;
    }

    /** A view of a modern recipe book (null gives null). */
    public static RecipeBook mcc$of(net.minecraft.stats.RecipeBook modern) {
        return modern == null ? null : new RecipeBook(modern);
    }

    public net.minecraft.stats.RecipeBook mcc$modern() {
        return mcc$modern;
    }

    /** The modern recipe of the legacy one's name, if the running game knows it. */
    private static net.minecraft.world.item.crafting.RecipeHolder<?> holder(IRecipe recipe) {
        if (recipe == null || recipe.getRegistryName() == null) return null;
        var id = recipe.getRegistryName().mcc$modern();
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.getRecipeManager().byKey(id).orElse(null);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) return ClientAccess.holder(id);
        return null;
    }

    public static int getRecipeId(IRecipe recipe) {
        return legacyconnector.v1_12_2.net.minecraft.item.crafting.CraftingManager.REGISTRY.getIDForObject(recipe);
    }

    public void copyFrom(RecipeBook that) {
        if (that == null) return;
        recipes.clear();
        newRecipes.clear();
        recipes.or(that.recipes);
        newRecipes.or(that.newRecipes);
        isGuiOpen = that.isGuiOpen();
        isFilteringCraftable = that.isFilteringCraftable();
        if (mcc$modern != null && that.mcc$modern != null) mcc$modern.copyOverData(that.mcc$modern);
    }

    public void unlock(IRecipe recipe) {
        if (mcc$modern != null) {
            var h = holder(recipe);
            if (h != null) mcc$modern.add(h);
            return;
        }
        if (!recipe.isDynamic()) recipes.set(getRecipeId(recipe));
    }

    public boolean isUnlocked(IRecipe recipe) {
        if (mcc$modern != null) return recipe != null && recipe.getRegistryName() != null && mcc$modern.contains(recipe.getRegistryName().mcc$modern());
        return recipes.get(getRecipeId(recipe));
    }

    public void lock(IRecipe recipe) {
        if (mcc$modern != null) {
            var h = holder(recipe);
            if (h != null) mcc$modern.remove(h);
            return;
        }
        int i = getRecipeId(recipe);
        recipes.clear(i);
        newRecipes.clear(i);
    }

    public boolean isNew(IRecipe recipe) {
        if (mcc$modern != null) {
            var h = holder(recipe);
            return h != null && mcc$modern.willHighlight(h);
        }
        return newRecipes.get(getRecipeId(recipe));
    }

    public void markSeen(IRecipe recipe) {
        if (mcc$modern != null) {
            var h = holder(recipe);
            if (h != null) mcc$modern.removeHighlight(h);
            return;
        }
        newRecipes.clear(getRecipeId(recipe));
    }

    public void markNew(IRecipe recipe) {
        if (mcc$modern != null) {
            var h = holder(recipe);
            if (h != null) mcc$modern.addHighlight(h);
            return;
        }
        newRecipes.set(getRecipeId(recipe));
    }

    public boolean isGuiOpen() {
        return mcc$modern != null ? mcc$modern.isOpen(net.minecraft.world.inventory.RecipeBookType.CRAFTING) : isGuiOpen;
    }

    public void setGuiOpen(boolean open) {
        if (mcc$modern != null) mcc$modern.setOpen(net.minecraft.world.inventory.RecipeBookType.CRAFTING, open);
        else isGuiOpen = open;
    }

    public boolean isFilteringCraftable() {
        return mcc$modern != null ? mcc$modern.isFiltering(net.minecraft.world.inventory.RecipeBookType.CRAFTING) : isFilteringCraftable;
    }

    public void setFilteringCraftable(boolean filtering) {
        if (mcc$modern != null) mcc$modern.setFiltering(net.minecraft.world.inventory.RecipeBookType.CRAFTING, filtering);
        else isFilteringCraftable = filtering;
    }

    /** The client level's recipes, kept apart so this class verifies on a dedicated server. */
    private static final class ClientAccess {
        static net.minecraft.world.item.crafting.RecipeHolder<?> holder(net.minecraft.resources.ResourceLocation id) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            return level == null ? null : level.getRecipeManager().byKey(id).orElse(null);
        }
    }
}
