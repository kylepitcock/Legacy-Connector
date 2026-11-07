package org.kylepitcock.legacyconnector.v1_12_2.advancement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A 1.12.2 advancement file ({@code assets/<mod>/advancements/**.json}) as the modern data pack format ({@code data/<mod>/advancement/**.json}).
 * Pure JSON work: everything that needs the registries (item subtypes, entity types, recipes, biomes) comes from the {@link Resolver}, so the
 * rules are testable without a game.
 *
 * <p>The vanilla triggers are converted to the modern conditions (the modern game fires them; the shim's trigger classes call the same
 * modern triggers). A trigger of a mod becomes the {@code legacyconnector:legacy} trigger carrying the original conditions, which the
 * mod's own {@code ICriterionTrigger} parses and tests (see {@code LegacyTrigger1122}). A vanilla trigger or a condition the modern
 * game cannot express becomes {@code minecraft:impossible} (or drops the condition when that only widens it) and is reported in
 * {@link Result#notes}.</p>
 */
public final class AdvancementConverter1122 {
    public static final String LEGACY_TRIGGER = "legacyconnector:legacy";

    private AdvancementConverter1122() {}

    /** Registry knowledge of the running game. Every method returns null (or an empty list) for what does not exist. */
    public interface Resolver {
        /** Modern item ids of a 1.12 item: the one subtype of {@code data}, or all subtypes when {@code data} is null or the wildcard. */
        List<String> items(String id, Integer data);

        /** Modern block id of a 1.12 block id. */
        String block(String id);

        /** Modern entity type id. */
        String entity(String id);

        /** Modern mob effect id. */
        String effect(String id);

        /** Modern recipe id of a 1.12 recipe id. */
        String recipe(String id);

        /** Modern biome id. */
        String biome(String id);

        /** Modern structure ids of a 1.12 structure name (empty when unknown). */
        List<String> structures(String name);

        /** Modern dimension id of a 1.12 dimension type name. */
        String dimension(String name);

        /** A resource id (parent advancement, loot table, background) in the modern namespace rules. */
        String resource(String id);
    }

    public static final class Result {
        /** The modern advancement. */
        public final JsonObject json;
        /** What could not be converted exactly (one line each, without the advancement's own name so equal ones repeat). */
        public final List<String> notes = new ArrayList<>();
        /** The parent advancement id (modern), or null. */
        public final String parent;

        Result(JsonObject json, String parent) {
            this.json = json;
            this.parent = parent;
        }
    }

    public static Result convert(JsonObject src, Resolver r) {
        Context c = new Context(r);
        JsonObject out = new JsonObject();
        String parent = null;
        if (src.has("parent") && src.get("parent").isJsonPrimitive()) {
            parent = r.resource(src.get("parent").getAsString());
            out.addProperty("parent", parent);
        }
        if (src.has("display") && src.get("display").isJsonObject()) out.add("display", c.display(src.getAsJsonObject("display")));
        JsonObject criteria = new JsonObject();
        if (src.has("criteria") && src.get("criteria").isJsonObject()) {
            for (var e : src.getAsJsonObject("criteria").entrySet()) {
                if (e.getValue().isJsonObject()) criteria.add(e.getKey(), c.criterion(e.getValue().getAsJsonObject()));
            }
        }
        out.add("criteria", criteria);
        if (src.has("requirements") && src.get("requirements").isJsonArray()) out.add("requirements", src.get("requirements"));
        if (src.has("rewards") && src.get("rewards").isJsonObject()) {
            JsonObject rewards = c.rewards(src.getAsJsonObject("rewards"));
            if (rewards.size() > 0) out.add("rewards", rewards);
        }
        Result result = new Result(out, parent);
        result.notes.addAll(c.notes);
        return result;
    }

    private static final class Context {
        final Resolver r;
        final List<String> notes = new ArrayList<>();

        Context(Resolver r) {
            this.r = r;
        }

        void note(String s) {
            if (!notes.contains(s)) notes.add(s);
        }

        // ---------------------------------------------------------------- display, rewards

        JsonObject display(JsonObject d) {
            JsonObject out = new JsonObject();
            JsonObject icon = new JsonObject();
            String id = null;
            if (d.has("icon") && d.get("icon").isJsonObject()) {
                JsonObject i = d.getAsJsonObject("icon");
                if (i.has("item")) {
                    List<String> ids = r.items(i.get("item").getAsString(), i.has("data") ? i.get("data").getAsInt() : 0);
                    if (!ids.isEmpty()) id = ids.get(0);
                    else note("icon item " + i.get("item").getAsString() + " is unknown");
                }
            }
            icon.addProperty("id", id == null ? "minecraft:barrier" : id);
            out.add("icon", icon);
            for (String key : new String[] {"title", "description"}) {
                if (d.has(key)) out.add(key, d.get(key));
                else out.addProperty(key, "");
            }
            if (d.has("frame") && d.get("frame").isJsonPrimitive()) out.addProperty("frame", d.get("frame").getAsString().toLowerCase(Locale.ROOT));
            if (d.has("background") && d.get("background").isJsonPrimitive()) out.addProperty("background", r.resource(d.get("background").getAsString()));
            for (String flag : new String[] {"show_toast", "announce_to_chat", "hidden"}) {
                if (d.has(flag) && d.get(flag).isJsonPrimitive()) out.add(flag, d.get(flag));
            }
            return out;
        }

        JsonObject rewards(JsonObject rw) {
            JsonObject out = new JsonObject();
            if (rw.has("experience")) out.add("experience", rw.get("experience"));
            if (rw.has("recipes") && rw.get("recipes").isJsonArray()) {
                JsonArray a = new JsonArray();
                for (JsonElement e : rw.getAsJsonArray("recipes")) a.add(r.recipe(e.getAsString()));
                out.add("recipes", a);
            }
            if (rw.has("loot") && rw.get("loot").isJsonArray()) {
                JsonArray a = new JsonArray();
                for (JsonElement e : rw.getAsJsonArray("loot")) a.add(r.resource(e.getAsString()));
                out.add("loot", a);
            }
            if (rw.has("function") && rw.get("function").isJsonPrimitive() && !rw.get("function").getAsString().isEmpty()) {
                note("reward function dropped (1.12 functions are not converted)");
            }
            return out;
        }

        // ---------------------------------------------------------------- criteria

        JsonObject criterion(JsonObject c) {
            String trigger = c.has("trigger") && c.get("trigger").isJsonPrimitive() ? c.get("trigger").getAsString() : "minecraft:impossible";
            if (trigger.indexOf(':') < 0) trigger = "minecraft:" + trigger;
            JsonObject cond = c.has("conditions") && c.get("conditions").isJsonObject() ? c.getAsJsonObject("conditions") : new JsonObject();
            if (!trigger.startsWith("minecraft:")) {
                JsonObject wrapped = new JsonObject();
                wrapped.addProperty("trigger", trigger);
                wrapped.add("conditions", cond);
                return trig(LEGACY_TRIGGER, wrapped);
            }
            JsonObject converted;
            try {
                converted = vanilla(trigger.substring("minecraft:".length()), cond);
            } catch (RuntimeException e) {
                note("trigger " + trigger + " has conditions that could not be read (" + e + ")");
                converted = null;
            }
            if (converted == null) return trig("minecraft:impossible", new JsonObject());
            return trig(trigger, converted);
        }

        static JsonObject trig(String trigger, JsonObject conditions) {
            JsonObject o = new JsonObject();
            o.addProperty("trigger", trigger);
            o.add("conditions", conditions);
            return o;
        }

        /** Modern conditions of a vanilla 1.12 trigger, or null when the criterion can never be met in this game. */
        JsonObject vanilla(String name, JsonObject cond) {
            JsonObject o = new JsonObject();
            switch (name) {
                case "impossible" -> {
                    return o;
                }
                case "tick" -> {
                    return o;
                }
                case "location" -> {
                    JsonObject loc = location(cond);
                    if (loc == null) return null;
                    if (loc.size() > 0) o.add("player", entityCheck(wrapPredicate("location", loc)));
                    return o;
                }
                case "inventory_changed" -> {
                    if (cond.has("items") && cond.get("items").isJsonArray()) {
                        JsonArray items = new JsonArray();
                        for (JsonElement e : cond.getAsJsonArray("items")) {
                            JsonObject p = item(e);
                            if (p == null) return null;
                            items.add(p);
                        }
                        o.add("items", items);
                    }
                    if (cond.has("slots") && cond.get("slots").isJsonObject()) o.add("slots", cond.get("slots"));
                    return o;
                }
                case "player_killed_entity", "entity_killed_player" -> {
                    if (!putEntity(o, "entity", cond.get("entity"))) return null;
                    if (cond.has("killing_blow")) note("killing_blow condition of " + name + " dropped");
                    return o;
                }
                case "player_hurt_entity", "entity_hurt_player" -> {
                    if (cond.has("damage") && cond.get("damage").isJsonObject()) {
                        JsonObject dmg = damage(cond.getAsJsonObject("damage"));
                        if (dmg == null) return null;
                        o.add("damage", dmg);
                    }
                    if (name.equals("player_hurt_entity") && !putEntity(o, "entity", cond.get("entity"))) return null;
                    return o;
                }
                case "tame_animal", "summoned_entity" -> {
                    return putEntity(o, "entity", cond.get("entity")) ? o : null;
                }
                case "bred_animals" -> {
                    for (String k : new String[] {"parent", "partner", "child"}) if (!putEntity(o, k, cond.get(k))) return null;
                    return o;
                }
                case "villager_trade" -> {
                    if (!putEntity(o, "villager", cond.get("villager"))) return null;
                    if (cond.has("item")) {
                        JsonObject p = item(cond.get("item"));
                        if (p == null) return null;
                        o.add("item", p);
                    }
                    return o;
                }
                case "cured_zombie_villager" -> {
                    return putEntity(o, "zombie", cond.get("zombie")) && putEntity(o, "villager", cond.get("villager")) ? o : null;
                }
                case "consume_item", "enchanted_item", "used_totem", "filled_bucket", "item_durability_changed" -> {
                    if (cond.has("item")) {
                        JsonObject p = item(cond.get("item"));
                        if (p == null) return null;
                        o.add("item", p);
                    }
                    for (String k : new String[] {"levels", "durability", "delta"}) if (cond.has(k)) o.add(k, cond.get(k));
                    return o;
                }
                case "placed_block" -> {
                    if (cond.has("block") && cond.get("block").isJsonPrimitive()) {
                        String block = r.block(cond.get("block").getAsString());
                        if (block == null) {
                            note("block " + cond.get("block").getAsString() + " is unknown");
                            return null;
                        }
                        JsonObject check = new JsonObject();
                        check.addProperty("condition", "minecraft:block_state_property");
                        check.addProperty("block", block);
                        JsonArray a = new JsonArray();
                        a.add(check);
                        o.add("location", a);
                    }
                    return o;
                }
                case "enter_block" -> {
                    if (cond.has("block") && cond.get("block").isJsonPrimitive()) {
                        String block = r.block(cond.get("block").getAsString());
                        if (block == null) {
                            note("block " + cond.get("block").getAsString() + " is unknown");
                            return null;
                        }
                        o.addProperty("block", block);
                    }
                    return o;
                }
                case "recipe_unlocked" -> {
                    if (!cond.has("recipe")) return null;
                    o.addProperty("recipe", r.recipe(cond.get("recipe").getAsString()));
                    return o;
                }
                case "effects_changed" -> {
                    if (cond.has("effects") && cond.get("effects").isJsonObject()) {
                        JsonObject effects = new JsonObject();
                        for (Map.Entry<String, JsonElement> e : cond.getAsJsonObject("effects").entrySet()) {
                            String id = r.effect(e.getKey());
                            if (id == null) {
                                note("effect " + e.getKey() + " is unknown");
                                return null;
                            }
                            effects.add(id, e.getValue());
                        }
                        o.add("effects", effects);
                    }
                    return o;
                }
                case "changed_dimension" -> {
                    for (String k : new String[] {"from", "to"}) {
                        if (!cond.has(k) || !cond.get(k).isJsonPrimitive()) continue;
                        String dim = r.dimension(cond.get(k).getAsString());
                        if (dim == null) {
                            note("dimension " + cond.get(k).getAsString() + " is unknown");
                            return null;
                        }
                        o.addProperty(k, dim);
                    }
                    return o;
                }
                case "construct_beacon" -> {
                    if (cond.has("level")) o.add("level", cond.get("level"));
                    return o;
                }
                case "brewed_potion" -> {
                    if (cond.has("potion") && cond.get("potion").isJsonPrimitive()) o.addProperty("potion", cond.get("potion").getAsString());
                    return o;
                }
                case "levitation" -> {
                    for (String k : new String[] {"distance", "duration"}) if (cond.has(k)) o.add(k, cond.get(k));
                    return o;
                }
                case "nether_travel" -> {
                    if (cond.has("distance")) o.add("distance", cond.get("distance"));
                    return o;
                }
                default -> {
                    note("trigger minecraft:" + name + " is not in this game (the criterion can not be met)");
                    return null;
                }
            }
        }

        // ---------------------------------------------------------------- predicates

        /** {@code entity_properties} as a list of loot conditions (the modern conditions of an entity are context aware predicates). */
        static JsonArray entityCheck(JsonObject predicate) {
            JsonObject c = new JsonObject();
            c.addProperty("condition", "minecraft:entity_properties");
            c.addProperty("entity", "this");
            c.add("predicate", predicate);
            JsonArray a = new JsonArray();
            a.add(c);
            return a;
        }

        static JsonObject wrapPredicate(String key, JsonObject value) {
            JsonObject o = new JsonObject();
            o.add(key, value);
            return o;
        }

        /** Adds the entity predicate of a condition as a context aware one; false when it names an unknown entity type. */
        boolean putEntity(JsonObject out, String key, JsonElement e) {
            if (e == null || !e.isJsonObject()) return true;
            JsonObject p = entity(e.getAsJsonObject());
            if (p == null) return false;
            if (p.size() > 0) out.add(key, entityCheck(p));
            return true;
        }

        /** A 1.12 entity predicate as the modern one; null when the entity type is unknown. */
        JsonObject entity(JsonObject e) {
            JsonObject o = new JsonObject();
            if (e.has("type") && e.get("type").isJsonPrimitive()) {
                String type = r.entity(e.get("type").getAsString());
                if (type == null) {
                    note("entity type " + e.get("type").getAsString() + " is unknown");
                    return null;
                }
                o.addProperty("type", type);
            }
            if (e.has("distance")) o.add("distance", e.get("distance"));
            if (e.has("location") && e.get("location").isJsonObject()) {
                JsonObject loc = location(e.getAsJsonObject("location"));
                if (loc == null) return null;
                if (loc.size() > 0) o.add("location", loc);
            }
            if (e.has("effects") && e.get("effects").isJsonObject()) {
                JsonObject effects = new JsonObject();
                for (Map.Entry<String, JsonElement> en : e.getAsJsonObject("effects").entrySet()) {
                    String id = r.effect(en.getKey());
                    if (id == null) return null;
                    effects.add(id, en.getValue());
                }
                o.add("effects", effects);
            }
            if (e.has("nbt")) note("entity nbt condition dropped (legacy NBT does not match the modern entity data)");
            return o;
        }

        JsonObject damage(JsonObject d) {
            JsonObject o = new JsonObject();
            for (String k : new String[] {"blocked", "dealt", "taken"}) if (d.has(k)) o.add(k, d.get(k));
            if (d.has("source_entity") && d.get("source_entity").isJsonObject()) {
                JsonObject src = entity(d.getAsJsonObject("source_entity"));
                if (src == null) return null;
                o.add("source_entity", src);
            }
            if (d.has("type")) note("damage type condition dropped");
            return o;
        }

        /** A 1.12 location predicate (biome, feature, dimension, x/y/z) as the modern one; null when it names something that does not exist. */
        JsonObject location(JsonObject l) {
            JsonObject o = new JsonObject();
            if (l.has("biome") && l.get("biome").isJsonPrimitive()) {
                String biome = r.biome(l.get("biome").getAsString());
                if (biome == null) {
                    note("biome " + l.get("biome").getAsString() + " is unknown");
                    return null;
                }
                o.addProperty("biomes", biome);
            }
            if (l.has("feature") && l.get("feature").isJsonPrimitive()) {
                List<String> structures = r.structures(l.get("feature").getAsString());
                if (structures.isEmpty()) {
                    note("structure " + l.get("feature").getAsString() + " is unknown");
                    return null;
                }
                JsonArray a = new JsonArray();
                structures.forEach(a::add);
                o.add("structures", a);
            }
            if (l.has("dimension") && l.get("dimension").isJsonPrimitive()) {
                String dim = r.dimension(l.get("dimension").getAsString());
                if (dim == null) {
                    note("dimension " + l.get("dimension").getAsString() + " is unknown");
                    return null;
                }
                o.addProperty("dimension", dim);
            }
            JsonObject pos = new JsonObject();
            for (String axis : new String[] {"x", "y", "z"}) if (l.has(axis)) pos.add(axis, l.get(axis));
            if (pos.size() > 0) o.add("position", pos);
            return o;
        }

        /** A 1.12 item predicate as the modern one; null when the item does not exist. */
        JsonObject item(JsonElement element) {
            if (element == null || !element.isJsonObject()) return new JsonObject();
            JsonObject i = element.getAsJsonObject();
            JsonObject o = new JsonObject();
            if (i.has("item") && i.get("item").isJsonPrimitive()) {
                Integer data = null;
                if (i.has("data") && i.get("data").isJsonPrimitive() && i.get("data").getAsInt() != 32767) data = i.get("data").getAsInt();
                List<String> ids = r.items(i.get("item").getAsString(), data);
                if (ids.isEmpty()) {
                    note("item " + i.get("item").getAsString() + " is unknown");
                    return null;
                }
                JsonArray a = new JsonArray();
                ids.forEach(a::add);
                o.add("items", a);
            }
            if (i.has("count")) o.add("count", i.get("count"));
            JsonObject predicates = new JsonObject();
            if (i.has("durability")) {
                JsonObject damage = new JsonObject();
                damage.add("durability", i.get("durability"));
                predicates.add("minecraft:damage", damage);
            }
            if (i.has("enchantments") && i.get("enchantments").isJsonArray()) {
                JsonArray list = new JsonArray();
                for (JsonElement e : i.getAsJsonArray("enchantments")) {
                    if (!e.isJsonObject()) continue;
                    JsonObject en = new JsonObject();
                    if (e.getAsJsonObject().has("enchantment")) en.addProperty("enchantments", e.getAsJsonObject().get("enchantment").getAsString());
                    if (e.getAsJsonObject().has("levels")) en.add("levels", e.getAsJsonObject().get("levels"));
                    list.add(en);
                }
                if (list.size() > 0) predicates.add("minecraft:enchantments", list);
            }
            if (predicates.size() > 0) o.add("predicates", predicates);
            if (i.has("nbt") || i.has("potion")) note("item nbt/potion condition dropped");
            return o;
        }
    }
}
