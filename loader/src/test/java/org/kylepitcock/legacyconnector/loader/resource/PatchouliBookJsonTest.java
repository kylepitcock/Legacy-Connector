package org.kylepitcock.legacyconnector.loader.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PatchouliBookJsonTest {
    private static JsonObject convert(String json) {
        return JsonParser.parseString(new String(PatchouliBookJson.convert(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void bookJsonIsStagedIntoTheDataFolderOfTheJar() {
        assertEquals("data/mymod/patchouli_books/guide/book.json", PatchouliBookJson.dataPath("assets/mymod/patchouli_books/guide/book.json"));
        assertNull(PatchouliBookJson.dataPath("assets/mymod/patchouli_books/guide/en_us/entries/a.json"));
        assertNull(PatchouliBookJson.dataPath("assets/mymod/textures/book.json"));
    }

    @Test
    void creativeTabAndItemAreConverted() {
        JsonObject o = convert("{\"name\":\"n\",\"creative_tab\":\"tools\",\"custom_book_item\":\"MyMod:Guide:2\",\"book_texture\":\"Patchouli:textures/gui/book_red.png\"}");
        assertEquals("minecraft:tools_and_utilities", o.get("creative_tab").getAsString());
        assertEquals("mymod:guide", o.get("custom_book_item").getAsString());
        assertEquals("patchouli:textures/gui/book_red.png", o.get("book_texture").getAsString());
        assertEquals("n", o.get("name").getAsString());
        assertTrue(o.get("use_resource_pack").getAsBoolean());
        assertFalse(convert("{\"creative_tab\":\"mymod.tab\"}").has("creative_tab"));
        assertTrue(convert("{\"creative_tab\":\"mymod:tab\"}").has("creative_tab"));
    }

    @Test
    void unreadableJsonIsKept() {
        byte[] junk = "not json {".getBytes(StandardCharsets.UTF_8);
        assertEquals(new String(junk), new String(PatchouliBookJson.convert(junk)));
    }
}
