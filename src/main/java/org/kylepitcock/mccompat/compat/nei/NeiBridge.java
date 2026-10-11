package org.kylepitcock.mccompat.compat.nei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mccompat.v1_7_10.codechicken.nei.PositionedStack;
import mccompat.v1_7_10.codechicken.nei.api.IConfigureNEI;
import mccompat.v1_7_10.codechicken.nei.recipe.ICraftingHandler;
import mccompat.v1_7_10.codechicken.nei.recipe.IUsageHandler;
import mccompat.v1_7_10.codechicken.nei.recipe.TemplateRecipeHandler;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NEI -> JEI bridge, JEI-independent half: runs the {@code NEI*Config} plugins legacy mods ship (found through FML's
 * class scan, as NEI found them by name), records registered recipe handlers and hidden items, and turns a handler's
 * recipes into plain data for the JEI plugin.
 */
public final class NeiBridge {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/NEI");
    private static final Map<Class<?>, ICraftingHandler> CRAFTING = new LinkedHashMap<>();
    private static final List<IUsageHandler> USAGE = new ArrayList<>();
    private static final List<ItemStack> HIDDEN = new ArrayList<>();
    private static boolean configsLoaded;

    /** A slot of a legacy recipe: position in the handler's area and the modern stacks it cycles through. */
    public record Slot(int x, int y, List<net.minecraft.world.item.ItemStack> stacks) {}

    public record Recipe(List<Slot> inputs, List<Slot> outputs, List<Slot> others) {}

    private NeiBridge() {}

    public static synchronized void registerCraftingHandler(ICraftingHandler handler) {
        if (handler != null) CRAFTING.putIfAbsent(handler.getClass(), handler);
    }

    public static synchronized void registerUsageHandler(IUsageHandler handler) {
        if (handler != null) USAGE.add(handler);
    }

    public static synchronized void hide(ItemStack stack) {
        if (stack != null) HIDDEN.add(stack);
    }

    /** Runs every NEI*Config plugin once (client only, after legacy loading). */
    public static synchronized void loadConfigs() {
        if (configsLoaded) return;
        configsLoaded = true;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        int n = 0;
        for (var file : ModList.get().getModFiles()) {
            for (var data : file.getFile().getScanResult().getClasses()) {
                String name = data.clazz().getClassName();
                String simple = name.substring(name.lastIndexOf('.') + 1);
                if (!simple.startsWith("NEI") || !simple.endsWith("Config")) continue;
                try {
                    Class<?> cls = Class.forName(name, true, loader);
                    if (!IConfigureNEI.class.isAssignableFrom(cls)) continue;
                    var ctor = cls.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    ((IConfigureNEI) ctor.newInstance()).loadConfig();
                    n++;
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    LOG.warn("NEI plugin {} failed: {}", name, e.toString());
                }
            }
        }
        LOG.info("Ran {} NEI plugin(s): {} recipe handler(s), {} hidden item(s)", n, CRAFTING.size(), HIDDEN.size());
    }

    public static synchronized List<TemplateRecipeHandler> templateHandlers() {
        List<TemplateRecipeHandler> out = new ArrayList<>();
        for (ICraftingHandler h : CRAFTING.values()) if (h instanceof TemplateRecipeHandler t) out.add(t);
        return out;
    }

    public static synchronized List<net.minecraft.world.item.ItemStack> hiddenItems() {
        List<net.minecraft.world.item.ItemStack> out = new ArrayList<>();
        for (ItemStack s : HIDDEN) {
            try {
                net.minecraft.world.item.ItemStack m = s.mcc$modern();
                if (!m.isEmpty()) out.add(m.copy());
            } catch (RuntimeException e) {
                // unregistered item
            }
        }
        return out;
    }

    /** All recipes a handler lists for its own overlay id (NEI's "show all recipes of this type"). */
    public static List<Recipe> recipes(TemplateRecipeHandler handler) {
        List<Recipe> out = new ArrayList<>();
        String id = handler.getOverlayIdentifier();
        if (id == null) return out;
        try {
            TemplateRecipeHandler h = handler.newInstance();
            h.loadCraftingRecipes(id);
            for (int i = 0; i < h.numRecipes(); i++) {
                try {
                    PositionedStack result = h.getResultStack(i);
                    out.add(new Recipe(slots(h.getIngredientStacks(i)), slots(result == null ? List.of() : List.of(result)), slots(h.getOtherStacks(i))));
                } catch (RuntimeException | LinkageError e) {
                    // skip a broken recipe
                }
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("NEI handler {} could not list its recipes: {}", handler.getClass().getName(), e.toString());
        }
        return out;
    }

    private static List<Slot> slots(List<PositionedStack> stacks) {
        List<Slot> out = new ArrayList<>();
        if (stacks == null) return out;
        for (PositionedStack ps : stacks) {
            if (ps == null) continue;
            List<net.minecraft.world.item.ItemStack> modern = new ArrayList<>();
            for (ItemStack s : ps.items) {
                if (s == null) continue;
                net.minecraft.world.item.ItemStack m = s.mcc$modern();
                if (!m.isEmpty()) modern.add(m.copy());
            }
            if (!modern.isEmpty()) out.add(new Slot(ps.relx, ps.rely, modern));
        }
        return out;
    }
}
