package legacyconnector.v1_12_2.net.minecraftforge.common.crafting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraftforge.oredict.OreIngredient;

public class CraftingHelper {
    /** A parsed shaped-recipe description. */
    public static class ShapedPrimer {
        public int width;
        public int height;
        public boolean mirrored = true;
        public NonNullList<Ingredient> input = NonNullList.create();
    }

    public static Ingredient getIngredient(Object obj) {
        if (obj instanceof Ingredient i) return i;
        if (obj instanceof ItemStack s) return s.isEmpty() ? Ingredient.EMPTY : Ingredient.fromStacks(s);
        if (obj instanceof Item item) return Ingredient.fromItem(item);
        if (obj instanceof Block block) return Ingredient.fromBlock(block);
        if (obj instanceof String ore) return new OreIngredient(ore);
        if (obj instanceof List<?> list) {
            ItemStack[] stacks = list.stream().filter(e -> e instanceof ItemStack).map(e -> (ItemStack) e).toArray(ItemStack[]::new);
            return Ingredient.fromStacks(stacks);
        }
        return Ingredient.EMPTY;
    }

    public static ShapedPrimer parseShaped(Object... recipe) {
        ShapedPrimer primer = new ShapedPrimer();
        int i = 0;
        if (recipe.length > 0 && recipe[0] instanceof Boolean b) {
            primer.mirrored = b;
            i = 1;
        }
        java.util.ArrayList<String> rows = new java.util.ArrayList<>();
        if (i < recipe.length && recipe[i] instanceof String[] arr) {
            for (String row : arr) rows.add(row);
            i++;
        } else {
            while (i < recipe.length && recipe[i] instanceof String row) {
                rows.add(row);
                i++;
            }
        }
        Map<Character, Ingredient> key = new HashMap<>();
        key.put(' ', Ingredient.EMPTY);
        for (; i + 1 < recipe.length; i += 2) {
            if (recipe[i] instanceof Character c) key.put(c, getIngredient(recipe[i + 1]));
        }
        primer.height = rows.size();
        primer.width = rows.isEmpty() ? 0 : rows.get(0).length();
        for (String row : rows) {
            for (int x = 0; x < primer.width; x++) {
                Ingredient ing = x < row.length() ? key.get(row.charAt(x)) : null;
                primer.input.add(ing == null ? Ingredient.EMPTY : ing);
            }
        }
        return primer;
    }

    // ------------------------------------------------------------------ JSON recipes

    public static ItemStack getItemStackBasic(com.google.gson.JsonObject json, JsonContext context) {
        String itemName = legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getString(json, "item");
        Item item = Item.REGISTRY.getObject(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(itemName));
        if (item == null) throw new com.google.gson.JsonSyntaxException("Unknown item '" + itemName + "'");
        int data = legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getInt(json, "data", 0);
        int count = legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getInt(json, "count", 1);
        return new ItemStack(item, count, data == 32767 ? 0 : data);
    }

    public static ItemStack getItemStack(com.google.gson.JsonObject json, JsonContext context) {
        return getItemStackBasic(json, context);
    }

    public static Ingredient getIngredient(com.google.gson.JsonElement json, JsonContext context) {
        if (json == null || json.isJsonNull()) throw new com.google.gson.JsonSyntaxException("Json cannot be null");
        if (json.isJsonArray()) {
            java.util.ArrayList<ItemStack> stacks = new java.util.ArrayList<>();
            for (com.google.gson.JsonElement e : json.getAsJsonArray()) stacks.addAll(java.util.Arrays.asList(getIngredient(e, context).getMatchingStacks()));
            return Ingredient.fromStacks(stacks.toArray(new ItemStack[0]));
        }
        if (!json.isJsonObject()) throw new com.google.gson.JsonSyntaxException("Expected ingredient to be a object or array of objects");
        com.google.gson.JsonObject obj = json.getAsJsonObject();
        String type = legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getString(obj, "type", "minecraft:item");
        if (type.equals("forge:ore_dict") && obj.has("ore")) return new OreIngredient(obj.get("ore").getAsString());
        if (obj.has("item")) {
            ItemStack stack = getItemStackBasic(obj, context);
            int data = legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getInt(obj, "data", 0);
            if (data == 32767) return Ingredient.fromItem(stack.getItem());
            return Ingredient.fromStacks(stack);
        }
        return Ingredient.EMPTY;
    }

    /** A recipe from its JSON (shaped and shapeless, plain and ore dictionary); other recipe types need their mod's own factory. */
    public static legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe getRecipe(com.google.gson.JsonObject json, JsonContext context) {
        String type = legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getString(json, "type");
        String group = legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getString(json, "group", "");
        legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation groupId = group.isEmpty() ? null : new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(group);
        if (type.endsWith("crafting_shaped") || type.equals("forge:ore_shaped")) {
            Map<Character, Ingredient> key = new HashMap<>();
            for (var e : legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getJsonObject(json, "key").entrySet()) {
                key.put(e.getKey().charAt(0), getIngredient(e.getValue(), context));
            }
            key.put(' ', Ingredient.EMPTY);
            com.google.gson.JsonArray pattern = legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getJsonArray(json, "pattern");
            ShapedPrimer primer = new ShapedPrimer();
            primer.height = pattern.size();
            primer.width = primer.height == 0 ? 0 : pattern.get(0).getAsString().length();
            for (com.google.gson.JsonElement row : pattern) {
                String r = row.getAsString();
                for (int x = 0; x < primer.width; x++) {
                    Ingredient ing = x < r.length() ? key.get(r.charAt(x)) : null;
                    primer.input.add(ing == null ? Ingredient.EMPTY : ing);
                }
            }
            primer.mirrored = legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getBoolean(json, "mirrored", true);
            ItemStack result = getItemStack(legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getJsonObject(json, "result"), context);
            return new legacyconnector.v1_12_2.net.minecraftforge.oredict.ShapedOreRecipe(groupId, result, primer);
        }
        if (type.endsWith("crafting_shapeless") || type.equals("forge:ore_shapeless")) {
            NonNullList<Ingredient> ings = NonNullList.create();
            for (com.google.gson.JsonElement e : legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getJsonArray(json, "ingredients")) ings.add(getIngredient(e, context));
            ItemStack result = getItemStack(legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getJsonObject(json, "result"), context);
            return new legacyconnector.v1_12_2.net.minecraftforge.oredict.ShapelessOreRecipe(groupId, ings, result);
        }
        return customFactory(context, type, "recipes") instanceof IRecipeFactory f ? f.parse(context, json) : null;
    }

    /** The factory a mod's assets/&lt;mod&gt;/recipes/_factories.json names for a recipe or ingredient type. */
    private static Object customFactory(JsonContext context, String type, String kind) {
        try {
            int colon = type.indexOf(':');
            String modId = colon >= 0 ? type.substring(0, colon) : context.getModId();
            String name = colon >= 0 ? type.substring(colon + 1) : type;
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try (var in = loader.getResourceAsStream("assets/" + modId + "/recipes/_factories.json")) {
                if (in == null) return null;
                com.google.gson.JsonObject root = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                if (!root.has(kind) || !root.getAsJsonObject(kind).has(name)) return null;
                String className = root.getAsJsonObject(kind).get(name).getAsString();
                return Class.forName(className, true, loader).getDeclaredConstructor().newInstance();
            }
        } catch (java.io.IOException | ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    /**
     * Visits the files under {@code base} (for example "assets/mod/recipes") of the mod's jar: the preprocessor gets the root, the processor each file
     * with the root. Returns false when a callback failed or nothing was found and {@code defaultUnfoundRoot} is false.
     */
    public static boolean findFiles(legacyconnector.v1_12_2.net.minecraftforge.fml.common.ModContainer mod, String base,
            java.util.function.Function<java.nio.file.Path, Boolean> preprocessor,
            java.util.function.BiFunction<java.nio.file.Path, java.nio.file.Path, Boolean> processor, boolean defaultUnfoundRoot, boolean visitAllFiles) {
        boolean success = true;
        boolean found = false;
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            java.util.Enumeration<java.net.URL> urls = loader.getResources(base);
            while (urls.hasMoreElements()) {
                java.net.URL url = urls.nextElement();
                java.nio.file.Path root;
                try {
                    java.net.URI uri = url.toURI();
                    if ("jar".equals(uri.getScheme())) {
                        java.nio.file.FileSystem fs;
                        try {
                            fs = java.nio.file.FileSystems.getFileSystem(uri);
                        } catch (java.nio.file.FileSystemNotFoundException e) {
                            fs = java.nio.file.FileSystems.newFileSystem(uri, java.util.Map.of());
                        }
                        root = fs.getPath(uri.getSchemeSpecificPart().substring(uri.getSchemeSpecificPart().indexOf("!/") + 1));
                    } else {
                        root = java.nio.file.Paths.get(uri);
                    }
                } catch (java.net.URISyntaxException | java.io.IOException | RuntimeException e) {
                    continue;
                }
                if (!java.nio.file.Files.exists(root)) continue;
                found = true;
                if (preprocessor != null) {
                    Boolean ok = preprocessor.apply(root);
                    if (ok != null && !ok) return false;
                }
                if (processor != null) {
                    final java.nio.file.Path base0 = root;
                    try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(root)) {
                        for (java.nio.file.Path file : (Iterable<java.nio.file.Path>) walk.filter(java.nio.file.Files::isRegularFile)::iterator) {
                            Boolean ok = processor.apply(base0, file);
                            if (ok != null && !ok) {
                                success = false;
                                if (!visitAllFiles) return false;
                            }
                        }
                    }
                }
            }
        } catch (java.io.IOException | RuntimeException e) {
            return false;
        }
        return found ? success : defaultUnfoundRoot;
    }

    private static final java.util.Map<String, IConditionFactory> CONDITIONS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, IIngredientFactory> INGREDIENTS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, IRecipeFactory> RECIPES = new java.util.concurrent.ConcurrentHashMap<>();

    public static void register(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation key, IConditionFactory factory) {
        if (key != null && factory != null) CONDITIONS.put(key.toString(), factory);
    }

    public static void register(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation key, IIngredientFactory factory) {
        if (key != null && factory != null) INGREDIENTS.put(key.toString(), factory);
    }

    public static void register(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation key, IRecipeFactory factory) {
        if (key != null && factory != null) RECIPES.put(key.toString(), factory);
    }

    /** True when every condition of a recipe's {@code conditions} array holds (Forge's built-in ones and the registered factories). */
    public static boolean processConditions(com.google.gson.JsonArray conditions, JsonContext context) {
        for (int x = 0; x < conditions.size(); x++) {
            if (!conditions.get(x).isJsonObject()) throw new com.google.gson.JsonSyntaxException("Conditions must be an array of JsonObjects");
            if (!getCondition(conditions.get(x).getAsJsonObject(), context).getAsBoolean()) return false;
        }
        return true;
    }

    public static java.util.function.BooleanSupplier getCondition(com.google.gson.JsonObject json, JsonContext context) {
        String type = legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getString(json, "type");
        if (type.indexOf(':') < 0 && context != null && context.getModId() != null) type = context.getModId() + ":" + type;
        switch (type) {
            case "forge:mod_loaded":
                String modid = legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getString(json, "modid");
                return () -> legacyconnector.v1_12_2.net.minecraftforge.fml.common.Loader.isModLoaded(modid);
            case "forge:item_exists":
                String item = legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getString(json, "item");
                return () -> Item.REGISTRY.containsKey(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(item));
            case "forge:not":
                java.util.function.BooleanSupplier child = getCondition(legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getJsonObject(json, "value"), context);
                return () -> !child.getAsBoolean();
            case "forge:or": {
                java.util.List<java.util.function.BooleanSupplier> children = new java.util.ArrayList<>();
                for (com.google.gson.JsonElement e : legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getJsonArray(json, "values")) children.add(getCondition(e.getAsJsonObject(), context));
                return () -> children.stream().anyMatch(java.util.function.BooleanSupplier::getAsBoolean);
            }
            case "forge:and": {
                java.util.List<java.util.function.BooleanSupplier> children = new java.util.ArrayList<>();
                for (com.google.gson.JsonElement e : legacyconnector.v1_12_2.net.minecraft.util.JsonUtils.getJsonArray(json, "values")) children.add(getCondition(e.getAsJsonObject(), context));
                return () -> children.stream().allMatch(java.util.function.BooleanSupplier::getAsBoolean);
            }
            case "forge:false":
                return () -> false;
            default:
                break;
        }
        IConditionFactory factory = CONDITIONS.get(type);
        if (factory == null && customFactory(context, type, "conditions") instanceof IConditionFactory f) factory = f;
        if (factory == null) throw new com.google.gson.JsonSyntaxException("Unknown condition type: " + type);
        return factory.parse(context, json);
    }
}
