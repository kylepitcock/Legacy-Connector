package org.kylepitcock.legacyconnector.compat.nei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.codechicken.nei.PositionedStack;
import legacyconnector.v1_7_10.codechicken.nei.api.IConfigureNEI;
import legacyconnector.v1_7_10.codechicken.nei.recipe.ICraftingHandler;
import legacyconnector.v1_7_10.codechicken.nei.recipe.IUsageHandler;
import legacyconnector.v1_7_10.codechicken.nei.recipe.TemplateRecipeHandler;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NEI -> JEI bridge, JEI-independent half: runs the {@code NEI*Config} plugins legacy mods ship (found through FML's
 * class scan, as NEI found them by name), records registered recipe handlers and hidden items, and turns a handler's
 * recipes into plain data for the JEI plugin.
 */
public final class NeiBridge {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/NEI");
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
        fillItemList();
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

    /** NEI's {@code ItemList.items}: a stack per registered item and subtype (handlers look for fuels and ingredients in it). */
    private static void fillItemList() {
        List<ItemStack> all = new ArrayList<>();
        try {
            for (Object o : org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge.legacyRegistry()) {
                if (!(o instanceof legacyconnector.v1_7_10.net.minecraft.item.Item item)) continue;
                try {
                    List<ItemStack> subs = new ArrayList<>();
                    if (item.getHasSubtypes()) item.getSubItems(item, null, subs);
                    if (subs.isEmpty()) subs.add(new ItemStack(item, 1, 0));
                    all.addAll(subs);
                } catch (RuntimeException | LinkageError e) {
                    // an item that cannot list its subtypes
                }
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Could not list the items for NEI's ItemList: {}", e.toString());
        }
        legacyconnector.v1_7_10.codechicken.nei.ItemList.items = all;
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

    /**
     * All recipes a handler lists for its own recipe ids (NEI's "show all recipes of this type"): its overlay id, plus the ids of
     * the transfer rectangles of its machine GUI, which is where handlers that extend the shaped handler (Traincraft's workbench,
     * assembly table, open hearth furnace) keep theirs. "fuel" lists burn times, not recipes.
     */
    public static List<Recipe> recipes(TemplateRecipeHandler handler) {
        List<Recipe> out = new ArrayList<>();
        java.util.LinkedHashMap<String, Object[]> ids = new java.util.LinkedHashMap<>();
        String overlay = handler.getOverlayIdentifier();
        if (overlay != null) ids.put(overlay, new Object[0]);
        try {
            for (var rect : handler.transferRects) {
                String id = rect.mcc$outputId();
                if (id != null && !"fuel".equals(id)) ids.putIfAbsent(id, rect.mcc$results() == null ? new Object[0] : rect.mcc$results());
            }
        } catch (RuntimeException | LinkageError e) {
            // a handler whose transfer rectangles cannot be read lists only its overlay id
        }
        for (var id : ids.entrySet()) {
            try {
                TemplateRecipeHandler h = handler.newInstance();
                h.loadCraftingRecipes(id.getKey(), id.getValue());
                for (int i = 0; i < h.numRecipes(); i++) {
                    try {
                        PositionedStack result = h.getResultStack(i);
                        out.add(new Recipe(slots(h.getIngredientStacks(i)), slots(result == null ? List.of() : List.of(result)), slots(h.getOtherStacks(i))));
                    } catch (RuntimeException | LinkageError e) {
                        // skip a broken recipe
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("NEI handler {} could not list its recipes for \"{}\": {}", handler.getClass().getName(), id.getKey(), e.toString());
            }
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

    private static final Map<String, List<Recipe>> LOOKUPS = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * The recipes a handler shows for one item, as NEI asked on a recipe/usage lookup: {@code loadCraftingRecipes("item", stack)}
     * (what makes the item) or {@code loadUsageRecipes("item", stack)} (what uses it). Handlers without a recipe list of their
     * own, such as Chisel's, only answer these. Results are cached per handler, item and direction.
     */
    public static List<Recipe> lookup(TemplateRecipeHandler handler, net.minecraft.world.item.ItemStack stack, boolean usage) {
        if (stack == null || stack.isEmpty()) return List.of();
        String key = handler.getClass().getName() + (usage ? "|use|" : "|make|") + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
                + "|" + stack.getOrDefault(org.kylepitcock.legacyconnector.core.data.CompatComponents.legacyMeta(), 0);
        List<Recipe> cached = LOOKUPS.get(key);
        if (cached != null) return cached;
        List<Recipe> out = new ArrayList<>();
        try {
            ItemStack legacy = ItemStack.mcc$wrap(stack);
            TemplateRecipeHandler h = handler.newInstance();
            if (usage) h.loadUsageRecipes("item", legacy);
            else h.loadCraftingRecipes("item", legacy);
            for (int i = 0; i < h.numRecipes(); i++) {
                try {
                    PositionedStack result = h.getResultStack(i);
                    out.add(new Recipe(slots(h.getIngredientStacks(i)), slots(result == null ? List.of() : List.of(result)), slots(h.getOtherStacks(i))));
                } catch (RuntimeException | LinkageError e) {
                    // skip a broken recipe
                }
            }
        } catch (RuntimeException | LinkageError e) {
            // a handler that cannot answer for this item has no recipes for it
        }
        LOOKUPS.put(key, out);
        return out;
    }
}
