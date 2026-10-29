package legacyconnector.v1_7_10.net.minecraft.item.crafting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.RecipeBridge;

/**
 * The legacy crafting registry. Its list holds the recipes legacy mods registered (vanilla recipes live in the modern
 * recipe manager); anything added to it - including {@code getRecipeList().add(...)} - becomes a modern recipe.
 */
public class CraftingManager {
    private static final CraftingManager instance = new CraftingManager();
    @SuppressWarnings("rawtypes")
    private List recipes = new RecipeList();
    /** Recipes already handed to the bridge (a list a mod swapped in cannot do that itself). */
    private final java.util.Set<Object> bridged = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private boolean listSwapChecked;

    public static CraftingManager getInstance() {
        return instance;
    }

    public ShapedRecipes addRecipe(ItemStack output, Object... params) {
        StringBuilder pattern = new StringBuilder();
        int i = 0;
        int width = 0;
        int height = 0;
        if (params[i] instanceof String[] rows) {
            i++;
            for (String row : rows) {
                height++;
                width = row.length();
                pattern.append(row);
            }
        } else {
            while (params[i] instanceof String row) {
                i++;
                height++;
                width = row.length();
                pattern.append(row);
            }
        }
        java.util.Map<Character, ItemStack> key = new java.util.HashMap<>();
        for (; i + 1 < params.length; i += 2) {
            Object v = params[i + 1];
            ItemStack s = v instanceof Item it ? new ItemStack(it) : v instanceof Block b ? new ItemStack(b, 1, 32767) : v instanceof ItemStack st ? st : null;
            key.put((Character) params[i], s);
        }
        ItemStack[] items = new ItemStack[width * height];
        for (int k = 0; k < items.length; k++) {
            ItemStack s = key.get(pattern.charAt(k));
            items[k] = s == null ? null : s.copy();
        }
        ShapedRecipes recipe = new ShapedRecipes(width, height, items, output);
        register(recipe);
        return recipe;
    }

    public void addShapelessRecipe(ItemStack output, Object... params) {
        List<ItemStack> items = new ArrayList<>();
        for (Object o : params) {
            if (o instanceof ItemStack s) items.add(s.copy());
            else if (o instanceof Item it) items.add(new ItemStack(it));
            else if (o instanceof Block b) items.add(new ItemStack(b));
        }
        register(new ShapelessRecipes(output, items));
    }

    public ItemStack findMatchingRecipe(InventoryCrafting inv, World world) {
        for (Object o : recipes) {
            IRecipe r = (IRecipe) o;
            RecipeBridge.Replacement replaced = RecipeBridge.replacementOf(r);
            if (replaced != null) {
                ItemStack out = mcc$craftReplacement(replaced, inv, world);
                if (out != null) return out;
            } else if (r.matches(inv, world)) {
                return r.getCraftingResult(inv);
            }
        }
        return mcc$findModern(inv, world);
    }

    /**
     * The vanilla recipes (and tool repair) that 1.7.10's list held live in the modern recipe manager: a grid no legacy recipe
     * matches is looked up there, so crafting tables built on {@code ContainerWorkbench} craft everything a vanilla table does.
     */
    private static ItemStack mcc$findModern(InventoryCrafting inv, World world) {
        net.minecraft.world.level.Level level = world == null ? null : world.mcc$liveLevel();
        if (level == null || inv == null) return null;
        var input = inv.mcc$craftingInput();
        if (input == null || MODERN_LOOKUP.get()) return null;
        MODERN_LOOKUP.set(Boolean.TRUE);
        try {
            var found = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level);
            if (found.isEmpty()) return null;
            net.minecraft.world.item.ItemStack result = found.get().value().assemble(input, level.registryAccess());
            return result.isEmpty() ? null : ItemStack.mcc$wrap(result);
        } catch (RuntimeException e) {
            return null;
        } finally {
            MODERN_LOOKUP.set(Boolean.FALSE);
        }
    }

    /** A legacy recipe a data pack or KubeJS changed crafts as the changed recipe (and not at all once removed). */
    private static ItemStack mcc$craftReplacement(RecipeBridge.Replacement replaced, InventoryCrafting inv, World world) {
        if (!(replaced.recipe() instanceof net.minecraft.world.item.crafting.CraftingRecipe recipe) || inv == null) return null;
        net.minecraft.world.level.Level level = world == null ? null : world.mcc$liveLevel();
        if (level == null) return null;
        var input = inv.mcc$craftingInput();
        try {
            if (input == null || !recipe.matches(input, level)) return null;
            net.minecraft.world.item.ItemStack result = recipe.assemble(input, level.registryAccess());
            return result.isEmpty() ? null : ItemStack.mcc$wrap(result);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Set while the modern recipe manager is searched: a bridged legacy recipe that asks this manager again is not looped back. */
    private static final ThreadLocal<Boolean> MODERN_LOOKUP = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void register(IRecipe recipe) {
        bridge(recipe);
        recipes.add(recipe);
    }

    private void bridge(IRecipe recipe) {
        synchronized (bridged) {
            if (!bridged.add(recipe)) return;
        }
        RecipeBridge.addLegacyRecipe(recipe);
    }

    /**
     * DragonAPI's coremod makes the vanilla list its own EventRecipeList (a patch of CraftingManager's constructor
     * that cannot run on this class), and casts what getRecipeList() returns. If that class is installed, serve one.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public synchronized List getRecipeList() {
        if (!listSwapChecked) {
            listSwapChecked = true;
            for (ClassLoader loader : new ClassLoader[] {Thread.currentThread().getContextClassLoader(), CraftingManager.class.getClassLoader()}) {
                if (loader == null) continue;
                try {
                    Class<?> cls = Class.forName("Reika.DragonAPI.Instantiable.Data.Collections.EventRecipeList", true, loader);
                    List swapped = (List) cls.getDeclaredConstructor().newInstance();
                    swapped.addAll(recipes);
                    recipes = swapped;
                    break;
                } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                    // not installed
                }
            }
        }
        return recipes;
    }

    /** A copy of the list (the recipe bridge compares it with what it translated); throws if it is being edited. */
    @SuppressWarnings("unchecked")
    public List<Object> mcc$snapshot() {
        return new ArrayList<Object>(recipes);
    }

    /** Recipes added straight to a list a mod swapped in are translated once loading is over. */
    public synchronized void mcc$syncSwappedList() {
        if (recipes instanceof RecipeList) return;
        for (Object o : new ArrayList<>(recipes)) if (o instanceof IRecipe r) bridge(r);
    }

    /** Lists a recipe the bridge already translated (GameRegistry paths), without translating it again. */
    public void mcc$track(IRecipe recipe) {
        // A mod may have swapped the list for its own (DragonAPI's EventRecipeList): then the recipe is only listed.
        synchronized (bridged) {
            bridged.add(recipe);
        }
        if (recipes instanceof RecipeList list) list.addUntracked(recipe);
        else recipes.add(recipe);
    }

    /** Forwards additions to the recipe bridge (mods often append to the list directly). */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class RecipeList extends ArrayList {
        void addUntracked(Object o) {
            super.add(o);
        }

        @Override
        public boolean add(Object o) {
            if (o instanceof IRecipe r) CraftingManager.instance.bridge(r);
            return super.add(o);
        }

        @Override
        public void add(int index, Object o) {
            if (o instanceof IRecipe r) CraftingManager.instance.bridge(r);
            super.add(index, o);
        }

        @Override
        public boolean addAll(Collection c) {
            for (Object o : c) if (o instanceof IRecipe r) CraftingManager.instance.bridge(r);
            return super.addAll(c);
        }
    }
}
