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

    /**
     * One recipe: its slots, plus the handler instance that loaded it and its index there, so the JEI category can run the
     * handler's own drawing ({@code drawBackground(index)}, {@code drawForeground(index)}) for it as NEI did.
     */
    public record Recipe(List<Slot> inputs, List<Slot> outputs, List<Slot> others, TemplateRecipeHandler handler, int index) {}

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

    /** NEI's recipe catalysts by handler id (overlay identifier or class name). */
    private static final Map<String, List<ItemStack>> CATALYSTS = new LinkedHashMap<>();

    public static synchronized void addCatalyst(String handlerId, ItemStack stack) {
        if (handlerId != null && stack != null) CATALYSTS.computeIfAbsent(handlerId, k -> new ArrayList<>()).add(stack);
    }

    public static synchronized void removeCatalyst(String handlerId, ItemStack stack) {
        List<ItemStack> list = handlerId == null || stack == null ? null : CATALYSTS.get(handlerId);
        if (list != null) list.removeIf(s -> s.getItem() == stack.getItem() && s.getItemDamage() == stack.getItemDamage());
    }

    /** The id NEI keys a handler's catalysts by: its overlay identifier, else its class name. */
    public static String handlerId(Object handler) {
        if (handler instanceof TemplateRecipeHandler t) {
            try {
                String id = t.getOverlayIdentifier();
                if (id != null) return id;
            } catch (RuntimeException | LinkageError e) {
                // no overlay id
            }
        }
        return handler == null ? null : handler.getClass().getName();
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
                        out.add(new Recipe(slots(h.getIngredientStacks(i)), slots(result == null ? List.of() : List.of(result)), slots(h.getOtherStacks(i)), h, i));
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
                    out.add(new Recipe(slots(h.getIngredientStacks(i)), slots(result == null ? List.of() : List.of(result)), slots(h.getOtherStacks(i)), h, i));
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

    // ------------------------------------------------------------------ how a handler is presented (JEI category)

    /**
     * What a handler's JEI category shows: the namespace of the mod it belongs to (JEI names the category's mod after its
     * recipe type's namespace), its title and the machines that run its recipes (the category's icon and catalysts, may be
     * empty).
     */
    public record Presentation(String namespace, String title, List<net.minecraft.world.item.ItemStack> catalysts) {}

    private static final java.util.Set<String> STOP_WORDS = java.util.Set.of("recipe", "recipes", "handler", "nei", "the", "of", "and", "for",
            "with", "shaped", "shapeless", "usage", "usages", "uses", "tile", "item", "items", "block", "blocks", "name", "gui", "custom", "mod", "template");
    /** Words of machine names: of two blocks sharing a word with the title ("Arcane Stone", "Arcane Worktable"), the machine wins. */
    private static final java.util.Set<String> MACHINE_WORDS = java.util.Set.of("table", "worktable", "workbench", "bench", "altar", "furnace",
            "machine", "crafter", "station", "press", "grinder", "crusher", "smelter", "forge", "anvil", "crucible", "infuser", "assembler",
            "extractor", "compressor", "macerator", "centrifuge", "mixer", "oven", "kiln", "cauldron", "matrix", "pedestal", "fabricator", "engineering");
    /** Legacy stacks of each namespace that may be a handler's machine (blocks first), built once from NEI's item list. */
    private static Map<String, List<Candidate>> candidates;

    private record Candidate(net.minecraft.world.item.ItemStack stack, java.util.Set<String> words, boolean block) {}

    /**
     * The mod, title and machines of a handler. Machines: what the mod registered as the handler's catalysts (NEI's
     * {@code API.addRecipeCatalyst}); else the block or item of the mod whose name best matches the handler's title and class name
     * ("Crucible" for the crucible handler); else the vanilla machine of vanilla recipe ids (crafting table, furnace, brewing stand).
     * The mod: the one whose classes the handler is in, else the machine's, else the one most of its recipes' results are from.
     */
    public static synchronized Presentation present(TemplateRecipeHandler handler, List<Recipe> recipes) {
        String title = title(handler);
        String owner = ownerNamespace(handler.getClass());
        List<net.minecraft.world.item.ItemStack> catalysts = new ArrayList<>();
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        ids.add(handlerId(handler));
        ids.add(handler.getClass().getName());
        for (String id : ids) {
            for (ItemStack s : CATALYSTS.getOrDefault(id, List.of())) {
                try {
                    net.minecraft.world.item.ItemStack m = s.mcc$modern();
                    if (!m.isEmpty()) catalysts.add(m.copyWithCount(1));
                } catch (RuntimeException | LinkageError e) {
                    // an unregistered catalyst
                }
            }
        }
        String resultNamespace = commonResultNamespace(recipes);
        if (catalysts.isEmpty()) {
            java.util.LinkedHashSet<String> namespaces = new java.util.LinkedHashSet<>();
            if (owner != null) namespaces.add(owner);
            if (resultNamespace != null && !resultNamespace.equals("minecraft")) namespaces.add(resultNamespace);
            net.minecraft.world.item.ItemStack match = bestMatch(namespaces, title, handler.getClass().getSimpleName());
            if (match != null) catalysts.add(match);
        }
        if (catalysts.isEmpty()) {
            String id = handlerId(handler);
            net.minecraft.world.item.Item vanilla = "crafting".equals(id) ? net.minecraft.world.item.Items.CRAFTING_TABLE
                    : "smelting".equals(id) || "fuel".equals(id) ? net.minecraft.world.item.Items.FURNACE
                    : "brewing".equals(id) ? net.minecraft.world.item.Items.BREWING_STAND : null;
            if (vanilla != null) catalysts.add(new net.minecraft.world.item.ItemStack(vanilla));
        }
        // the machine's mod first: an NEI addon's handlers (Thaumcraft NEI Plugin) show the recipes of the mod whose machine runs them
        String namespace = null;
        if (!catalysts.isEmpty()) {
            String ns = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(catalysts.get(0).getItem()).getNamespace();
            if (!ns.equals("minecraft")) namespace = ns;
        }
        if (namespace == null) namespace = owner;
        if (namespace == null) namespace = resultNamespace != null && !resultNamespace.equals("minecraft") ? resultNamespace : "legacyconnector";
        return new Presentation(namespace, title, catalysts);
    }

    /** The handler's {@code getRecipeName()}, translated when it is a translation key; the class name when it has none. */
    private static String title(TemplateRecipeHandler handler) {
        String title;
        try {
            title = handler.getRecipeName();
        } catch (RuntimeException | LinkageError e) {
            title = null;
        }
        if (title != null && !title.isBlank() && title.indexOf(' ') < 0 && title.indexOf('.') > 0) {
            try {
                if (legacyconnector.v1_7_10.net.minecraft.util.StatCollector.canTranslate(title)) {
                    title = legacyconnector.v1_7_10.net.minecraft.util.StatCollector.translateToLocal(title);
                } else if (net.minecraft.client.resources.language.I18n.exists(title)) {
                    title = net.minecraft.client.resources.language.I18n.get(title);
                }
            } catch (RuntimeException | LinkageError e) {
                // keep the key
            }
        }
        if (title == null || title.isBlank()) {
            title = String.join(" ", words(handler.getClass().getSimpleName(), false));
            if (title.isEmpty()) title = handler.getClass().getSimpleName();
        }
        return title;
    }

    /**
     * The namespace of the legacy mod whose classes a class is in: of the mods in the same jar, the one whose main class shares
     * the longest package prefix with it.
     */
    private static String ownerNamespace(Class<?> type) {
        try {
            String best = null;
            int bestLength = -1;
            for (var c : legacyconnector.v1_7_10.cpw.mods.fml.common.Loader.instance().getActiveModList()) {
                Object mod = c.getMod();
                if (mod == null || c.getModId() == null) continue;
                Class<?> modType = mod.getClass();
                boolean sameJar = type.getModule().isNamed() ? modType.getModule() == type.getModule() : modType.getClassLoader() == type.getClassLoader();
                if (!sameJar) continue;
                int length = commonPrefix(type.getName(), modType.getName());
                if (length > bestLength) {
                    bestLength = length;
                    best = c.getModId();
                }
            }
            return best == null ? null : org.kylepitcock.legacyconnector.core.Ids.namespace(best);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static int commonPrefix(String a, String b) {
        int n = Math.min(a.length(), b.length()), i = 0;
        while (i < n && a.charAt(i) == b.charAt(i)) i++;
        return i;
    }

    /** The namespace most of a handler's results (first 50 recipes) are from. */
    private static String commonResultNamespace(List<Recipe> recipes) {
        Map<String, Integer> counts = new java.util.HashMap<>();
        for (int i = 0; i < recipes.size() && i < 50; i++) {
            for (Slot s : recipes.get(i).outputs()) {
                if (s.stacks().isEmpty()) continue;
                counts.merge(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.stacks().get(0).getItem()).getNamespace(), 1, Integer::sum);
            }
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    /**
     * The mod's stack whose name best matches a handler: shared significant words with the title count most, with the class
     * name less, words of the name that match neither count against it, a block wins a tie. Null without a shared word.
     */
    private static net.minecraft.world.item.ItemStack bestMatch(java.util.Collection<String> namespaces, String title, String className) {
        if (namespaces.isEmpty()) return null;
        java.util.Set<String> titleWords = new java.util.HashSet<>(words(title, true));
        java.util.Set<String> classWords = new java.util.HashSet<>(words(className, true));
        if (titleWords.isEmpty() && classWords.isEmpty()) return null;
        Map<String, List<Candidate>> all = candidates();
        for (String ns : namespaces) {
            Candidate best = null;
            int bestScore = 0;
            for (Candidate c : all.getOrDefault(ns, List.of())) {
                int score = 0, shared = 0;
                for (String w : c.words()) {
                    if (titleWords.contains(w)) {
                        score += 10;
                        shared++;
                    } else if (classWords.contains(w)) {
                        score += 4;
                        shared++;
                    } else {
                        score -= 2;
                    }
                }
                if (shared == 0) continue;
                if (c.words().equals(titleWords)) score += 100;
                if (c.block()) score += 1;
                for (String w : c.words()) if (MACHINE_WORDS.contains(w)) score += 3;
                if (score > bestScore) {
                    bestScore = score;
                    best = c;
                }
            }
            if (best != null) return best.stack().copyWithCount(1);
        }
        return null;
    }

    private static Map<String, List<Candidate>> candidates() {
        if (candidates != null) return candidates;
        Map<String, List<Candidate>> out = new java.util.HashMap<>();
        List<ItemStack> items = legacyconnector.v1_7_10.codechicken.nei.ItemList.items;
        if (items != null) {
            for (ItemStack s : items) {
                try {
                    net.minecraft.world.item.ItemStack m = s.mcc$modern();
                    if (m.isEmpty()) continue;
                    net.minecraft.resources.ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(m.getItem());
                    java.util.Set<String> words = new java.util.LinkedHashSet<>();
                    String name = null;
                    try {
                        name = s.getDisplayName();
                    } catch (RuntimeException | LinkageError e) {
                        // nameless subtype
                    }
                    if (name != null && !name.isBlank() && !name.contains(".name")) words.addAll(words(name.replaceAll("§.", ""), true));
                    else words.addAll(words(key.getPath(), true));
                    if (words.isEmpty()) continue;
                    boolean block = s.getItem() instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemBlock;
                    out.computeIfAbsent(key.getNamespace(), k -> new ArrayList<>()).add(new Candidate(m.copyWithCount(1), words, block));
                } catch (RuntimeException | LinkageError e) {
                    // an item that cannot be converted
                }
            }
        }
        candidates = out;
        return out;
    }

    /** Lower-case words of a name ({@code "ArcaneWorkbenchHandler"}, {@code "arcane_workbench"}); without filler words if asked. */
    private static List<String> words(String text, boolean significantOnly) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String w : text.replaceAll("([a-z0-9])([A-Z])", "$1 $2").split("[^A-Za-z0-9]+")) {
            String lw = w.toLowerCase(java.util.Locale.ROOT);
            if (lw.isEmpty()) continue;
            if (significantOnly && (lw.length() < 3 || STOP_WORDS.contains(lw))) continue;
            if (!significantOnly && (lw.equals("nei") || lw.equals("handler") || lw.equals("recipe"))) continue;
            out.add(lw);
        }
        return out;
    }
}
