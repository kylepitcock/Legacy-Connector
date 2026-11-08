package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.common.crafting.CraftingHelper;
import legacyconnector.v1_12_2.net.minecraftforge.common.crafting.JsonContext;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JSON recipes of 1.12.2 mods ({@code assets/<mod>/recipes/*.json}), loaded the way Forge 1.12 loaded them after the recipe
 * registry event: the {@code _constants.json} of the folder first (ingredient constants recipes name as {@code "#NAME"}), then each
 * file that is not underscore-prefixed whose conditions hold, parsed by {@link CraftingHelper#getRecipe} (shaped and shapeless, plain
 * and ore dictionary, and the mod's own factories from {@code _factories.json}) and registered under {@code <mod>:<file name>}. They
 * then take the same way into the data pack as recipes a mod registered in code ({@link RecipeBridge1122}). Before this, none of a
 * 1.12.2 mod's JSON recipes existed (Atum 176, AbyssalCraft 434).
 */
public final class JsonRecipes1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Recipes");

    private JsonRecipes1122() {}

    /** Forge 1.12 read recipe files leniently and stopped after the first value: libVulpes ships files with the recipe written twice. */
    private static JsonElement parseLenient(java.io.Reader in) {
        com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(in);
        reader.setLenient(true);
        return JsonParser.parseReader(reader);
    }

    /** Loads the JSON recipes of every 1.12.2 mod and registers them. */
    public static void load() {
        int loaded = 0, failed = 0, skipped = 0;
        String firstFailure = null;
        for (LegacyModRecord mod : LegacyMods.all()) {
            if (!"1.12.2".equals(mod.version)) continue;
            int[] counts = {0, 0, 0};
            String[] first = {null};
            String ns = mod.modernId;
            JsonContext ctx = new JsonContext(mod.legacyId.toLowerCase(java.util.Locale.ROOT));
            walk(mod, ns, ctx, (root, file) -> {
                String rel = root.relativize(file).toString().replace('\\', '/');
                if (!rel.endsWith(".json") || rel.startsWith("_") || rel.contains("/_")) return true;
                String name = rel.substring(0, rel.length() - ".json".length());
                try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonElement root0 = parseLenient(in);
                    if (!root0.isJsonObject()) {
                        counts[2]++;
                        return true;
                    }
                    JsonObject json = root0.getAsJsonObject();
                    if (json.has("conditions") && !CraftingHelper.processConditions(json.getAsJsonArray("conditions"), ctx)) {
                        counts[2]++;
                        return true;
                    }
                    IRecipe recipe = CraftingHelper.getRecipe(json, ctx);
                    if (recipe == null) {
                        counts[2]++;
                        return true;
                    }
                    recipe.setRegistryName(new ResourceLocation(ctx.getModId(), name));
                    Registries1122.RECIPES.register(recipe);
                    counts[0]++;
                } catch (Exception | LinkageError e) {
                    counts[1]++;
                    if (first[0] == null) first[0] = ns + ":" + name + ": " + e;
                }
                return true;
            });
            loaded += counts[0];
            failed += counts[1];
            skipped += counts[2];
            if (counts[0] + counts[1] + counts[2] > 0) {
                LOG.info("1.12.2 JSON recipes of {}: {} loaded, {} failed, {} skipped (condition or unsupported type)", mod.legacyId, counts[0], counts[1], counts[2]);
                if (first[0] != null) {
                    LOG.warn("  first failure: {}", first[0]);
                    if (firstFailure == null) firstFailure = first[0];
                }
            }
        }
        if (loaded + failed + skipped > 0) LOG.info("1.12.2 JSON recipes: {} loaded, {} failed, {} skipped", loaded, failed, skipped);
    }

    /** The constants file of the recipe folder, then each file of it, read through the mod's own container (the class loader of the loading thread does not see the mod jars). */
    private static void walk(LegacyModRecord mod, String ns, JsonContext ctx, java.util.function.BiFunction<java.nio.file.Path, java.nio.file.Path, Boolean> visit) {
        try {
            java.nio.file.Path root = mod.container.getModInfo().getOwningFile().getFile().findResource("legacyconnector_legacy", "assets", ns, "recipes");
            if (root == null || !Files.isDirectory(root)) return;
            loadConstants(root, ctx);
            try (var files = Files.walk(root)) {
                for (java.nio.file.Path file : (Iterable<java.nio.file.Path>) files.filter(Files::isRegularFile).sorted()::iterator) visit.apply(root, file);
            }
        } catch (java.io.IOException | RuntimeException e) {
            LOG.warn("Cannot read the recipes of {}: {}", mod.legacyId, e.toString());
        }
    }

    /** A file of a mod's recipe folder (staged under legacyconnector_legacy/ in the translated jar), or null. */
    public static java.io.InputStream open(String modId, String file, ClassLoader fallback) throws java.io.IOException {
        var mod = LegacyMods.find(modId);
        if (mod != null) {
            try {
                java.nio.file.Path p = mod.container.getModInfo().getOwningFile().getFile().findResource("legacyconnector_legacy", "assets", mod.modernId, "recipes", file);
                if (p != null && Files.isRegularFile(p)) return Files.newInputStream(p);
            } catch (RuntimeException e) {
                // falls back to the class loader
            }
        }
        return fallback == null ? null : fallback.getResourceAsStream("legacyconnector_legacy/assets/" + modId + "/recipes/" + file);
    }

    private static void loadConstants(java.nio.file.Path root, JsonContext ctx) {
        java.nio.file.Path file = root.resolve("_constants.json");
        if (!Files.exists(file)) return;
        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement parsed = parseLenient(in);
            if (!parsed.isJsonArray()) return;
            JsonArray array = parsed.getAsJsonArray();
            for (JsonElement e : array) {
                if (!e.isJsonObject()) continue;
                JsonObject json = e.getAsJsonObject();
                if (!json.has("name") || !json.has("ingredient")) continue;
                if (json.has("conditions") && !CraftingHelper.processConditions(json.getAsJsonArray("conditions"), ctx)) continue;
                try {
                    ctx.mcc$addConstant(json.get("name").getAsString(), CraftingHelper.getIngredient(json.get("ingredient"), ctx));
                } catch (RuntimeException ex) {
                    // an ingredient this game cannot read is left out
                }
            }
        } catch (java.io.IOException | RuntimeException e) {
            LOG.warn("Cannot read the recipe constants of {}: {}", ctx.getModId(), e.toString());
        }
    }
}
