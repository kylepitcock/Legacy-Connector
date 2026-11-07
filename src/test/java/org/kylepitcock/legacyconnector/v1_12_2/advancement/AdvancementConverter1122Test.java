package org.kylepitcock.legacyconnector.v1_12_2.advancement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The 1.12 to modern advancement json conversion, with a resolver that stands in for the layer's registries. */
class AdvancementConverter1122Test {
    private static final AdvancementConverter1122.Resolver R = new AdvancementConverter1122.Resolver() {
        public List<String> items(String id, Integer data) {
            if (id.equals("mod:missing")) return List.of();
            String base = id.toLowerCase();
            if (data != null) return List.of(data == 0 ? base : base + "_" + data);
            return base.equals("mod:gem") ? List.of("mod:gem", "mod:gem_1", "mod:gem_2") : List.of(base);
        }

        public String block(String id) {
            return id.equals("mod:nothing") ? null : id.toLowerCase();
        }

        public String entity(String id) {
            return id.equals("mod:ghost") ? null : id.toLowerCase();
        }

        public String effect(String id) {
            return id.toLowerCase();
        }

        public String recipe(String id) {
            return id.startsWith("minecraft:") ? id : id.replace('/', '_') + "_code";
        }

        public String biome(String id) {
            return id.toLowerCase();
        }

        public List<String> structures(String name) {
            return name.equals("Village") ? List.of("minecraft:village_plains", "minecraft:village_desert") : List.of();
        }

        public String dimension(String name) {
            return name.equals("deep") ? "legacyconnector:dim_7" : null;
        }

        public String resource(String id) {
            return id.toLowerCase();
        }
    };

    private static AdvancementConverter1122.Result convert(String json) {
        return AdvancementConverter1122.convert(JsonParser.parseString(json).getAsJsonObject(), R);
    }

    private static JsonObject criterion(AdvancementConverter1122.Result r, String name) {
        return r.json.getAsJsonObject("criteria").getAsJsonObject(name);
    }

    @Test
    void displayIconAndFlags() {
        var r = convert("""
                {"parent":"Mod:Root","display":{"icon":{"item":"mod:Gem","data":2},"title":{"translate":"adv.t"},"description":"d","frame":"CHALLENGE",
                 "background":"mod:textures/gui/bg.png","hidden":true,"announce_to_chat":false},
                 "criteria":{"c":{"trigger":"minecraft:impossible"}},"requirements":[["c"]],
                 "rewards":{"experience":10,"recipes":["mod:thing","minecraft:stick"],"loot":["mod:loot/x"],"function":"mod:f"}}""");
        JsonObject d = r.json.getAsJsonObject("display");
        assertEquals("mod:gem_2", d.getAsJsonObject("icon").get("id").getAsString());
        assertEquals("challenge", d.get("frame").getAsString());
        assertEquals("mod:textures/gui/bg.png", d.get("background").getAsString());
        assertTrue(d.get("hidden").getAsBoolean());
        assertFalse(d.get("announce_to_chat").getAsBoolean());
        assertEquals("adv.t", d.getAsJsonObject("title").get("translate").getAsString());
        assertEquals("mod:root", r.json.get("parent").getAsString());
        assertEquals("mod:root", r.parent);
        var rewards = r.json.getAsJsonObject("rewards");
        assertEquals(10, rewards.get("experience").getAsInt());
        assertEquals("mod:thing_code", rewards.getAsJsonArray("recipes").get(0).getAsString());
        assertEquals("minecraft:stick", rewards.getAsJsonArray("recipes").get(1).getAsString());
        assertEquals("mod:loot/x", rewards.getAsJsonArray("loot").get(0).getAsString());
        assertFalse(rewards.has("function"));
        assertTrue(r.notes.stream().anyMatch(n -> n.contains("function")));
    }

    @Test
    void unknownIconBecomesBarrier() {
        var r = convert("{\"display\":{\"icon\":{\"item\":\"mod:missing\"},\"title\":\"t\",\"description\":\"d\"},\"criteria\":{}}");
        assertEquals("minecraft:barrier", r.json.getAsJsonObject("display").getAsJsonObject("icon").get("id").getAsString());
    }

    @Test
    void inventoryChangedUsesSubtypes() {
        var r = convert("""
                {"criteria":{"a":{"trigger":"minecraft:inventory_changed","conditions":{"items":[{"item":"mod:gem","data":1},{"item":"mod:gem"}]}},
                 "b":{"trigger":"inventory_changed","conditions":{"items":[{"item":"mod:missing"}]}}}}""");
        var items = criterion(r, "a").getAsJsonObject("conditions").getAsJsonArray("items");
        assertEquals("mod:gem_1", items.get(0).getAsJsonObject().getAsJsonArray("items").get(0).getAsString());
        assertEquals(3, items.get(1).getAsJsonObject().getAsJsonArray("items").size());
        assertEquals("minecraft:impossible", criterion(r, "b").get("trigger").getAsString());
    }

    @Test
    void entityTriggersUseContextAwarePredicates() {
        var r = convert("""
                {"criteria":{"k":{"trigger":"minecraft:player_killed_entity","conditions":{"entity":{"type":"Mod:Beast"}}},
                 "g":{"trigger":"minecraft:player_killed_entity","conditions":{"entity":{"type":"mod:ghost"}}},
                 "h":{"trigger":"minecraft:entity_hurt_player","conditions":{"damage":{"dealt":{"min":3},"source_entity":{"type":"mod:beast"}}}}}}""");
        var check = criterion(r, "k").getAsJsonObject("conditions").getAsJsonArray("entity").get(0).getAsJsonObject();
        assertEquals("minecraft:entity_properties", check.get("condition").getAsString());
        assertEquals("mod:beast", check.getAsJsonObject("predicate").get("type").getAsString());
        assertEquals("minecraft:impossible", criterion(r, "g").get("trigger").getAsString());
        var dmg = criterion(r, "h").getAsJsonObject("conditions").getAsJsonObject("damage");
        assertEquals(3, dmg.getAsJsonObject("dealt").get("min").getAsInt());
        assertEquals("mod:beast", dmg.getAsJsonObject("source_entity").get("type").getAsString());
    }

    @Test
    void locationTrigger() {
        var r = convert("""
                {"criteria":{"b":{"trigger":"location","conditions":{"biome":"Mod:Dark"}},
                 "f":{"trigger":"minecraft:location","conditions":{"feature":"Village"}},
                 "x":{"trigger":"minecraft:location","conditions":{"feature":"Nope"}},
                 "d":{"trigger":"minecraft:changed_dimension","conditions":{"to":"deep"}}}}""");
        var loc = criterion(r, "b").getAsJsonObject("conditions").getAsJsonArray("player").get(0).getAsJsonObject()
                .getAsJsonObject("predicate").getAsJsonObject("location");
        assertEquals("mod:dark", loc.get("biomes").getAsString());
        var structures = criterion(r, "f").getAsJsonObject("conditions").getAsJsonArray("player").get(0).getAsJsonObject()
                .getAsJsonObject("predicate").getAsJsonObject("location").getAsJsonArray("structures");
        assertEquals(2, structures.size());
        assertEquals("minecraft:impossible", criterion(r, "x").get("trigger").getAsString());
        assertEquals("legacyconnector:dim_7", criterion(r, "d").getAsJsonObject("conditions").get("to").getAsString());
    }

    @Test
    void simpleTriggers() {
        var r = convert("""
                {"criteria":{"t":{"trigger":"minecraft:tick"},
                 "r":{"trigger":"minecraft:recipe_unlocked","conditions":{"recipe":"mod:a/b"}},
                 "c":{"trigger":"minecraft:consume_item","conditions":{"item":{"item":"mod:food","count":{"min":2},"durability":{"max":5}}}},
                 "p":{"trigger":"minecraft:placed_block","conditions":{"block":"mod:plant"}},
                 "e":{"trigger":"minecraft:enter_block","conditions":{"block":"mod:nothing"}},
                 "u":{"trigger":"minecraft:something_new"}}}""");
        assertEquals("minecraft:tick", criterion(r, "t").get("trigger").getAsString());
        assertEquals("mod:a_b_code", criterion(r, "r").getAsJsonObject("conditions").get("recipe").getAsString());
        var item = criterion(r, "c").getAsJsonObject("conditions").getAsJsonObject("item");
        assertEquals("mod:food", item.getAsJsonArray("items").get(0).getAsString());
        assertEquals(2, item.getAsJsonObject("count").get("min").getAsInt());
        assertEquals(5, item.getAsJsonObject("predicates").getAsJsonObject("minecraft:damage").getAsJsonObject("durability").get("max").getAsInt());
        assertEquals("mod:plant", criterion(r, "p").getAsJsonObject("conditions").getAsJsonArray("location").get(0).getAsJsonObject().get("block").getAsString());
        assertEquals("minecraft:impossible", criterion(r, "e").get("trigger").getAsString());
        assertEquals("minecraft:impossible", criterion(r, "u").get("trigger").getAsString());
        assertTrue(r.notes.stream().anyMatch(n -> n.contains("something_new")));
    }

    @Test
    void triggersOfModsAreWrapped() {
        var r = convert("{\"criteria\":{\"m\":{\"trigger\":\"mod:cast_spell\",\"conditions\":{\"spell\":\"fire\"}},\"n\":{\"trigger\":\"mod:plain\"}}}");
        var m = criterion(r, "m");
        assertEquals("legacyconnector:legacy", m.get("trigger").getAsString());
        assertEquals("mod:cast_spell", m.getAsJsonObject("conditions").get("trigger").getAsString());
        assertEquals("fire", m.getAsJsonObject("conditions").getAsJsonObject("conditions").get("spell").getAsString());
        assertEquals(0, criterion(r, "n").getAsJsonObject("conditions").getAsJsonObject("conditions").size());
    }
}
