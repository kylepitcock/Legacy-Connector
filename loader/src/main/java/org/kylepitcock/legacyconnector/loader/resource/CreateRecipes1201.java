package org.kylepitcock.legacyconnector.loader.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;


/**
 * Recipes of the Create mod family ({@code create:mixing}, {@code create:pressing}, ..., {@code create:mechanical_crafting}, {@code create:sequenced_assembly})
 * written for Create 0.5 (Minecraft 1.20.1) in the format of Create 6 (1.21.1), which every Create add-on's data relies on:
 * <ul>
 *   <li>processing recipes: {@code ingredients} holds items or fluids ({@code {fluid|fluidTag, amount}} became {@code {fluid|tag, amount}}), {@code results}
 *       holds {@code {id, count, chance}} items or {@code {id, amount}} fluids ({@code item} became {@code id}), {@code processingTime} and
 *       {@code heatRequirement} became snake case, {@code keepHeldItem} too;</li>
 *   <li>{@code create:mechanical_crafting}: a shaped recipe that now has to say {@code accept_mirrored};</li>
 *   <li>{@code create:sequenced_assembly}: {@code transitionalItem} is {@code transitional_item}, its {@code results} and each step of {@code sequence}
 *       follow the rules above.</li>
 * </ul>
 */
final class CreateRecipes1201 {
    private CreateRecipes1201() {}

    static boolean handles(String type) {
        return type.startsWith("create:");
    }

    static void convert(JsonObject o, String type, DataConverter1201.Ctx c) {
        switch (type) {
            case "create:mechanical_crafting" -> {
                if (!o.has("accept_mirrored")) o.addProperty("accept_mirrored", true);
                if (o.has("result")) o.add("result", itemResult(o.get("result"), c));
                if (o.has("key") && o.get("key").isJsonObject()) {
                    JsonObject key = o.getAsJsonObject("key");
                    for (String k : new ArrayList<>(key.keySet())) key.add(k, DataConverter1201.ingredient(key.get(k), c));
                }
            }
            case "create:sequenced_assembly" -> {
                if (o.has("ingredient")) o.add("ingredient", DataConverter1201.ingredient(o.get("ingredient"), c));
                rename(o, "transitionalItem", "transitional_item");
                if (o.has("transitional_item")) o.add("transitional_item", itemResult(o.get("transitional_item"), c));
                if (o.has("results")) o.add("results", results(o.get("results"), c));
                if (o.has("sequence") && o.get("sequence").isJsonArray()) {
                    for (JsonElement step : o.getAsJsonArray("sequence")) {
                        if (step.isJsonObject()) {
                            JsonObject s = step.getAsJsonObject();
                            String t = s.has("type") && s.get("type").isJsonPrimitive() ? s.get("type").getAsString() : "";
                            if (handles(t)) convert(s, t, c);
                        }
                    }
                }
            }
            default -> processing(o, c);
        }
    }

    private static void processing(JsonObject o, DataConverter1201.Ctx c) {
        rename(o, "processingTime", "processing_time");
        rename(o, "heatRequirement", "heat_requirement");
        rename(o, "keepHeldItem", "keep_held_item");
        if (o.has("ingredients") && o.get("ingredients").isJsonArray()) {
            JsonArray out = new JsonArray();
            for (JsonElement e : o.getAsJsonArray("ingredients")) out.add(ingredient(e, c));
            o.add("ingredients", out);
        }
        if (o.has("results")) o.add("results", results(o.get("results"), c));
        else if (o.has("result")) {
            // a few recipe types (cutting, ...) wrote a single result; Create 6 reads the list
            JsonArray one = new JsonArray();
            one.add(o.remove("result"));
            o.add("results", results(one, c));
        }
    }

    private static JsonElement ingredient(JsonElement e, DataConverter1201.Ctx c) {
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("fluid") || o.has("fluidTag") || o.has("fluid_tag")) {
                JsonObject f = new JsonObject();
                f.add("amount", o.has("amount") ? o.get("amount") : new com.google.gson.JsonPrimitive(1000));
                if (o.has("fluid")) f.add("fluid", o.get("fluid"));
                else f.add("tag", o.has("fluidTag") ? o.get("fluidTag") : o.get("fluid_tag"));
                c.hit("create_fluid");
                return f;
            }
        }
        return DataConverter1201.ingredient(e, c);
    }

    private static JsonArray results(JsonElement e, DataConverter1201.Ctx c) {
        JsonArray out = new JsonArray();
        if (!e.isJsonArray()) {
            out.add(result(e, c));
            return out;
        }
        for (JsonElement r : e.getAsJsonArray()) out.add(result(r, c));
        return out;
    }

    private static JsonElement result(JsonElement e, DataConverter1201.Ctx c) {
        if (e.isJsonObject() && e.getAsJsonObject().has("fluid")) {
            JsonObject o = e.getAsJsonObject();
            JsonObject f = new JsonObject();
            f.add("id", o.get("fluid"));
            f.add("amount", o.has("amount") ? o.get("amount") : new com.google.gson.JsonPrimitive(1000));
            return f;
        }
        if (e.isJsonObject() && e.getAsJsonObject().has("id")) return e;
        return DataConverter1201.stack(e, c, "recipe_result");
    }

    private static JsonElement itemResult(JsonElement e, DataConverter1201.Ctx c) {
        if (e.isJsonObject() && e.getAsJsonObject().has("id")) return e;
        return DataConverter1201.stack(e, c, "recipe_result");
    }

    private static void rename(JsonObject o, String from, String to) {
        if (o.has(from) && !o.has(to)) o.add(to, o.remove(from));
    }
}
