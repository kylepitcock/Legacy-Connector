package org.kylepitcock.legacyconnector.compat.mods.railcraft;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.codechicken.nei.PositionedStack;
import legacyconnector.v1_7_10.codechicken.nei.recipe.TemplateRecipeHandler;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.item.crafting.IRecipe;
import legacyconnector.v1_7_10.net.minecraft.item.crafting.ShapedRecipes;
import legacyconnector.v1_7_10.net.minecraft.item.crafting.ShapelessRecipes;
import legacyconnector.v1_7_10.net.minecraftforge.oredict.ShapedOreRecipe;
import legacyconnector.v1_7_10.net.minecraftforge.oredict.ShapelessOreRecipe;
import org.kylepitcock.legacyconnector.compat.nei.NeiBridge;

/**
 * Railcraft (1.4.7) keeps the Rock Crusher and Rolling Machine recipes in lists of its own ({@code RailcraftCraftingManager.rockCrusher} /
 * {@code rollingMachine}) and ships no NEI plugin (only a CraftGuide one), so JEI had no category for either. These handlers show the lists
 * through the NEI -> JEI bridge, with the machine as the category's catalyst. Mod specific on purpose (the lists are the mod's own API, not a
 * registry the layer could read generically); read through the mod's class loader, does nothing when Railcraft is absent.
 */
public final class RailcraftNei {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/NEI");

    /** The mod's manager class, found when the NEI plugins load (the loader the handlers are asked on later is not the mod's). */
    private static volatile Class<?> MANAGER;

    private RailcraftNei() {}

    /** Called once when the NEI plugins load. */
    public static void register() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> manager = Class.forName("railcraft.common.api.crafting.RailcraftCraftingManager", true, cl);
            MANAGER = manager;
            Class<?> machines = Class.forName("railcraft.common.blocks.machine.alpha.EnumMachineAlpha", true, cl);
            if (manager.getField("rockCrusher").get(null) != null) add(new RockCrusher(), machines, "ROCK_CRUSHER");
            if (manager.getField("rollingMachine").get(null) != null) add(new RollingMachine(), machines, "ROLLING_MACHINE");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // Railcraft is not loaded
        }
    }

    private static void add(TemplateRecipeHandler handler, Class<?> machines, String machine) {
        NeiBridge.registerCraftingHandler(handler);
        try {
            Object constant = machines.getField(machine).get(null);
            if (machines.getMethod("getItem").invoke(constant) instanceof ItemStack s) NeiBridge.addCatalyst(NeiBridge.handlerId(handler), s);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOG.debug("No Railcraft catalyst {}: {}", machine, e.toString());
        }
    }

    private static boolean same(ItemStack a, ItemStack b) {
        return a != null && b != null && a.getItem() == b.getItem()
                && (b.getItemDamage() == 32767 || a.getItemDamage() == 32767 || a.getItemDamage() == b.getItemDamage());
    }

    private static boolean matches(Object ingredient, ItemStack focus) {
        if (ingredient instanceof ItemStack s) return same(s, focus);
        if (ingredient instanceof Iterable<?> it) {
            for (Object o : it) if (matches(o, focus)) return true;
        }
        return false;
    }

    private static Object call(Object target, String method) throws ReflectiveOperationException {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            for (Class<?> i : c.getInterfaces()) {
                try {
                    Method m = i.getMethod(method);
                    return m.invoke(target);
                } catch (NoSuchMethodException e) {
                    // next interface
                }
            }
        }
        return target.getClass().getMethod(method).invoke(target);
    }

    /** The shared part: a handler over one of the mod's recipe lists. */
    public abstract static class ListHandler extends TemplateRecipeHandler {
        private final String field;
        private final String method;

        ListHandler(String field, String method) {
            this.field = field;
            this.method = method;
        }

        final List<?> recipes() {
            try {
                Object m = MANAGER.getField(field).get(null);
                return new ArrayList<>((List<?>) m.getClass().getMethod(method).invoke(m));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return List.of();
            }
        }

        /** Adds the entries of the list that match: {@code focus} null = all, else the stack as a result ({@code usage} false) or an ingredient. */
        abstract void load(ItemStack focus, boolean usage);

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
        public String getGuiTexture() {
            return null;
        }

        final void add(List<PositionedStack> in, ItemStack out) {
            arecipes.add(new Cached(in, out));
        }

        private final class Cached extends CachedRecipe {
            private final List<PositionedStack> inputs;
            private final PositionedStack output;

            Cached(List<PositionedStack> in, ItemStack out) {
                this.inputs = in;
                this.output = new PositionedStack(out.copy(), 119, 24, false);
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

    /** Rock Crusher: one entry per input and possible output. */
    public static final class RockCrusher extends ListHandler {
        public RockCrusher() {
            super("rockCrusher", "getRecipes");
        }

        @Override
        void load(ItemStack focus, boolean usage) {
            for (Object r : recipes()) {
                try {
                    ItemStack in = (ItemStack) call(r, "getInput");
                    Map<?, ?> outputs = (Map<?, ?>) call(r, "getOutputs");
                    if (in == null || outputs == null) continue;
                    if (focus != null && usage && !same(in, focus)) continue;
                    for (Map.Entry<?, ?> e : outputs.entrySet()) {
                        if (!(e.getKey() instanceof ItemStack out)) continue;
                        if (focus != null && !usage && !same(out, focus)) continue;
                        add(List.of(new PositionedStack(in.copy(), 30, 24, false)), out);
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    // a recipe of another shape
                }
            }
        }

        @Override
        public String getRecipeName() {
            return "Rock Crusher";
        }

        @Override
        public String getOverlayIdentifier() {
            return "railcraft_rock_crusher";
        }
    }

    /** Rolling Machine: the shaped / shapeless recipes of its list, laid out as a 3x3 grid. */
    public static final class RollingMachine extends ListHandler {
        public RollingMachine() {
            super("rollingMachine", "getRecipeList");
        }

        @Override
        void load(ItemStack focus, boolean usage) {
            for (Object o : recipes()) {
                if (!(o instanceof IRecipe recipe)) continue;
                try {
                    ItemStack out = recipe.getRecipeOutput();
                    if (out == null) continue;
                    List<PositionedStack> in = ingredients(recipe);
                    if (in == null) continue;
                    boolean match = focus == null;
                    if (!match && !usage) match = same(out, focus);
                    if (!match && usage) {
                        for (PositionedStack p : in) if (matches(java.util.Arrays.asList(p.items), focus)) match = true;
                    }
                    if (match) add(in, out);
                } catch (RuntimeException e) {
                    // a recipe of another shape
                }
            }
        }

        private static List<PositionedStack> ingredients(IRecipe recipe) {
            List<PositionedStack> in = new ArrayList<>();
            if (recipe instanceof ShapedRecipes r) {
                for (int i = 0; i < r.recipeItems.length; i++) put(in, r.recipeItems[i], i % r.recipeWidth, i / r.recipeWidth);
            } else if (recipe instanceof ShapedOreRecipe r) {
                Object[] grid = r.getInput();
                int w = width(r, grid.length);
                for (int i = 0; i < grid.length; i++) put(in, grid[i], i % w, i / w);
            } else if (recipe instanceof ShapelessRecipes r) {
                int i = 0;
                for (Object o : r.recipeItems) {
                    put(in, o, i % 3, i / 3);
                    i++;
                }
            } else if (recipe instanceof ShapelessOreRecipe r) {
                int i = 0;
                for (Object o : r.getInput()) {
                    put(in, o, i % 3, i / 3);
                    i++;
                }
            } else {
                return null;
            }
            return in;
        }

        private static int width(ShapedOreRecipe r, int cells) {
            try {
                var f = ShapedOreRecipe.class.getDeclaredField("width");
                f.setAccessible(true);
                return Math.max(1, f.getInt(r));
            } catch (ReflectiveOperationException | RuntimeException e) {
                return cells >= 9 ? 3 : Math.max(1, cells);
            }
        }

        private static void put(List<PositionedStack> in, Object ingredient, int x, int y) {
            if (ingredient == null) return;
            PositionedStack p = new PositionedStack(ingredient, 25 + x * 18, 6 + y * 18, false);
            if (p.items != null && p.items.length > 0) in.add(p);
        }

        @Override
        public String getRecipeName() {
            return "Rolling Machine";
        }

        @Override
        public String getOverlayIdentifier() {
            return "railcraft_rolling_machine";
        }
    }
}
