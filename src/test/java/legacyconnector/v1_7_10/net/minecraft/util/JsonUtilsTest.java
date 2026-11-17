package legacyconnector.v1_7_10.net.minecraft.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;

/** 1.7.10 {@code JsonUtils}: typed field access with defaults and the game's error behaviour. */
class JsonUtilsTest {
    private static JsonObject obj(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    @Test
    void readsTypedFields() {
        JsonObject o = obj("{\"s\":\"text\",\"b\":true,\"i\":7,\"f\":1.5,\"o\":{\"x\":1},\"a\":[1,2]}");
        assertEquals("text", JsonUtils.getJsonObjectStringFieldValue(o, "s"));
        assertTrue(JsonUtils.getJsonObjectBooleanFieldValue(o, "b"));
        assertEquals(7, JsonUtils.getJsonObjectIntegerFieldValue(o, "i"));
        assertEquals(1.5F, JsonUtils.getJsonObjectFloatFieldValue(o, "f"));
        assertEquals(1, JsonUtils.getJsonObject(o, "o").get("x").getAsInt());
        assertEquals(2, JsonUtils.getJsonObjectJsonArrayField(o, "a").size());
    }

    @Test
    void defaultsApplyOnlyToMissingFields() {
        JsonObject o = obj("{\"b\":false}");
        assertEquals("dflt", JsonUtils.getJsonObjectStringFieldValueOrDefault(o, "s", "dflt"));
        assertFalse(JsonUtils.getJsonObjectBooleanFieldValueOrDefault(o, "b", true));
        assertTrue(JsonUtils.getJsonObjectBooleanFieldValueOrDefault(o, "missing", true));
        assertEquals(3, JsonUtils.getJsonObjectIntegerFieldValueOrDefault(o, "i", 3));
    }

    @Test
    void missingOrWrongTypedFieldsThrow() {
        JsonObject o = obj("{\"s\":\"text\",\"o\":{}}");
        assertThrows(JsonSyntaxException.class, () -> JsonUtils.getJsonObjectStringFieldValue(o, "absent"));
        assertThrows(JsonSyntaxException.class, () -> JsonUtils.getJsonObjectIntegerFieldValue(o, "s"));
        assertThrows(JsonSyntaxException.class, () -> JsonUtils.getJsonObject(o, "s"));
        assertThrows(JsonSyntaxException.class, () -> JsonUtils.getJsonElementAsJsonObject(o.get("s"), "s"));
    }

    @Test
    void typeProbes() {
        JsonObject o = obj("{\"s\":\"text\",\"n\":4,\"a\":[]}");
        assertTrue(JsonUtils.jsonObjectFieldTypeIsString(o, "s"));
        assertFalse(JsonUtils.jsonObjectFieldTypeIsString(o, "n"));
        assertTrue(JsonUtils.jsonObjectFieldTypeIsArray(o, "a"));
        assertTrue(JsonUtils.jsonObjectHasNamedField(o, "n"));
        assertFalse(JsonUtils.jsonObjectHasNamedField(o, "zz"));
    }
}
