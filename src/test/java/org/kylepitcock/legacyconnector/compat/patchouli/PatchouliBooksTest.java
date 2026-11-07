package org.kylepitcock.legacyconnector.compat.patchouli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** The 1.12 to modern Patchouli book json conversion, with a resolver that stands in for the layer's registries. */
class PatchouliBooksTest {
    /** Items "mod:thing:3" become "mod:thing_3", blocks get a meta property, recipes the _code suffix. */
    private static final PatchouliBooks.Resolver RESOLVER = new PatchouliBooks.Resolver() {
        public String item(String spec) {
            String[] p = spec.split(":");
            return p.length == 3 && p[2].matches("\\d+") ? p[0] + ":" + p[1] + "_" + p[2] : spec;
        }

        public String block(String spec) {
            return spec.startsWith("mod:") ? spec.replaceAll(":(\\d+)$", "[meta=$1]") : spec;
        }

        public String entity(String id) {
            return id.replace("Old", "old");
        }

        public String recipe(String id) {
            return id + "_code";
        }

        public String smelting(String inputSpec) {
            return inputSpec.equals("minecraft:iron_ore") ? "legacyconnector:smelting_1122/minecraft/iron_ingot" : null;
        }
    };

    private static JsonObject convert(String path, String json) {
        byte[] out = PatchouliBooks.convert(path, json.getBytes(StandardCharsets.UTF_8), "mymod", RESOLVER);
        return JsonParser.parseString(new String(out, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void registryHasTheConverterOfEveryPresentVersion() {
        for (var e : BookConverter.BY_VERSION.entrySet()) assertEquals(e.getKey(), e.getValue().legacyVersion());
        assertEquals(true, BookConverter.BY_VERSION.get("1.12.2").booksInAssets());
    }

    @Test
    void bookJsonMapsTabAndItem() {
        JsonObject book = convert("book.json", """
                {"name": "mymod.guide", "landing_text": "hi", "creative_tab": "misc", "custom_book_item": "mymod:guide:2",
                 "book_texture": "Patchouli:textures/gui/book_brown.png", "model": "patchouli:book_brown"}""");
        assertEquals("minecraft:tools_and_utilities", book.get("creative_tab").getAsString());
        assertEquals("mymod:guide_2", book.get("custom_book_item").getAsString());
        assertEquals("patchouli:textures/gui/book_brown.png", book.get("book_texture").getAsString());
        assertEquals("mymod.guide", book.get("name").getAsString());
        assertEquals(true, book.get("use_resource_pack").getAsBoolean());
    }

    @Test
    void unknownCreativeTabIsDropped() {
        JsonObject book = convert("book.json", "{\"name\": \"n\", \"creative_tab\": \"mymod.tab\"}");
        assertFalse(book.has("creative_tab"));
    }

    @Test
    void entryItemsPageTypesAndRecipes() {
        JsonObject entry = convert("en_us/entries/basics/start.json", """
                {"name": "Start", "icon": "mymod:thing:3", "category": "basics",
                 "pages": [
                   {"type": "text", "text": "Hello"},
                   {"type": "spotlight", "item": "mymod:thing:1,minecraft:stone", "text": "x"},
                   {"type": "crafting", "recipe": "mymod:gizmo", "recipe2": "minecraft:stick"},
                   {"type": "entity", "entity": "mymod:Old"},
                   {"type": "mymod:custom_template", "input": "#x", "item": "mymod:thing:2"}
                 ]}""");
        assertEquals("mymod:thing_3", entry.get("icon").getAsString());
        assertEquals("mymod:basics", entry.get("category").getAsString());
        JsonArray pages = entry.getAsJsonArray("pages");
        assertEquals("patchouli:text", pages.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("mymod:thing_1,minecraft:stone", pages.get(1).getAsJsonObject().get("item").getAsString());
        assertEquals("mymod:gizmo_code", pages.get(2).getAsJsonObject().get("recipe").getAsString());
        assertEquals("minecraft:stick_code", pages.get(2).getAsJsonObject().get("recipe2").getAsString());
        assertEquals("mymod:old", pages.get(3).getAsJsonObject().get("entity").getAsString());
        assertEquals("mymod:custom_template", pages.get(4).getAsJsonObject().get("type").getAsString());
        assertEquals("mymod:thing_2", pages.get(4).getAsJsonObject().get("item").getAsString());
        assertEquals("#x", pages.get(4).getAsJsonObject().get("input").getAsString());
    }

    @Test
    void smeltingPageBecomesRecipePageOrSpotlight() {
        JsonObject entry = convert("en_us/entries/c/e.json", """
                {"name": "E", "icon": "minecraft:stone", "category": "c",
                 "pages": [{"type": "smelting", "recipe": "minecraft:iron_ore", "text": "a"},
                           {"type": "smelting", "recipe": "mymod:ore:1", "title": "T", "text": "b"}]}""");
        JsonArray pages = entry.getAsJsonArray("pages");
        assertEquals("patchouli:smelting", pages.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("legacyconnector:smelting_1122/minecraft/iron_ingot", pages.get(0).getAsJsonObject().get("recipe").getAsString());
        assertEquals("patchouli:spotlight", pages.get(1).getAsJsonObject().get("type").getAsString());
        assertEquals("mymod:ore_1", pages.get(1).getAsJsonObject().get("item").getAsString());
        assertEquals("T", pages.get(1).getAsJsonObject().get("title").getAsString());
        assertNull(pages.get(1).getAsJsonObject().get("recipe"));
    }

    @Test
    void multiblockMappingUsesModernBlockStrings() {
        JsonObject entry = convert("en_us/entries/c/m.json", """
                {"name": "M", "icon": "minecraft:stone", "category": "c",
                 "pages": [{"type": "multiblock", "name": "Shrine", "multiblock": {
                    "pattern": [["0"], ["B"]], "mapping": {"B": "mod:block:3", "C": "minecraft:stone"}}}]}""");
        JsonObject mapping = entry.getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonObject("multiblock").getAsJsonObject("mapping");
        // the stand-in resolver only rewrites "mod:" blocks; the real one maps every block through the layer's state bridge
        assertEquals("mod:block[meta=3]", mapping.get("B").getAsString());
        assertEquals("minecraft:stone", mapping.get("C").getAsString());
    }

    @Test
    void categoryAndTemplate() {
        JsonObject category = convert("en_us/categories/basics.json", "{\"name\": \"Basics\", \"icon\": \"mymod:thing:3\", \"parent\": \"top\"}");
        assertEquals("mymod:thing_3", category.get("icon").getAsString());
        assertEquals("mymod:top", category.get("parent").getAsString());
        JsonObject template = convert("en_us/templates/foo.json", """
                {"components": [{"type": "item", "item": "mymod:thing:1", "x": 1}], "include": [{"template": "other", "as": "o"}]}""");
        assertEquals("mymod:thing_1", template.getAsJsonArray("components").get(0).getAsJsonObject().get("item").getAsString());
        assertEquals("mymod:other", template.getAsJsonArray("include").get(0).getAsJsonObject().get("template").getAsString());
    }
}
