package legacyconnector.v1_7_10.net.minecraft.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

/**
 * 1.7.10 {@code net.minecraft.util.JsonUtils}: typed access to Gson trees with the game's error messages. The getters throw a
 * {@link JsonSyntaxException} naming the field when it is missing or has another type.
 */
public class JsonUtils {
    public static boolean jsonObjectFieldTypeIsString(JsonObject object, String field) {
        return jsonObjectFieldTypeIsPrimitive(object, field) && object.getAsJsonPrimitive(field).isString();
    }

    public static boolean jsonElementTypeIsString(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }

    public static boolean jsonObjectFieldTypeIsArray(JsonObject object, String field) {
        return object != null && object.has(field) && object.get(field).isJsonArray();
    }

    public static boolean jsonObjectFieldTypeIsPrimitive(JsonObject object, String field) {
        return object != null && object.has(field) && object.get(field).isJsonPrimitive();
    }

    public static boolean jsonObjectHasNamedField(JsonObject object, String field) {
        return object != null && object.get(field) != null;
    }

    public static String getJsonElementStringValue(JsonElement element, String name) {
        if (element.isJsonPrimitive()) return element.getAsString();
        throw new JsonSyntaxException("Expected " + name + " to be a string, was " + describe(element));
    }

    public static String getJsonObjectStringFieldValue(JsonObject object, String field) {
        if (object.has(field)) return getJsonElementStringValue(object.get(field), field);
        throw new JsonSyntaxException("Missing " + field + ", expected to find a string");
    }

    public static String getJsonObjectStringFieldValueOrDefault(JsonObject object, String field, String fallback) {
        return object.has(field) ? getJsonElementStringValue(object.get(field), field) : fallback;
    }

    public static boolean getJsonElementBooleanValue(JsonElement element, String name) {
        if (element.isJsonPrimitive()) return element.getAsBoolean();
        throw new JsonSyntaxException("Expected " + name + " to be a Boolean, was " + describe(element));
    }

    public static boolean getJsonObjectBooleanFieldValue(JsonObject object, String field) {
        if (object.has(field)) return getJsonElementBooleanValue(object.get(field), field);
        throw new JsonSyntaxException("Missing " + field + ", expected to find a Boolean");
    }

    public static boolean getJsonObjectBooleanFieldValueOrDefault(JsonObject object, String field, boolean fallback) {
        return object.has(field) ? getJsonElementBooleanValue(object.get(field), field) : fallback;
    }

    public static float getJsonElementFloatValue(JsonElement element, String name) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) return element.getAsFloat();
        throw new JsonSyntaxException("Expected " + name + " to be a Float, was " + describe(element));
    }

    public static float getJsonObjectFloatFieldValue(JsonObject object, String field) {
        if (object.has(field)) return getJsonElementFloatValue(object.get(field), field);
        throw new JsonSyntaxException("Missing " + field + ", expected to find a Float");
    }

    public static float getJsonObjectFloatFieldValueOrDefault(JsonObject object, String field, float fallback) {
        return object.has(field) ? getJsonElementFloatValue(object.get(field), field) : fallback;
    }

    public static int getJsonElementIntegerValue(JsonElement element, String name) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) return element.getAsInt();
        throw new JsonSyntaxException("Expected " + name + " to be an Int, was " + describe(element));
    }

    public static int getJsonObjectIntegerFieldValue(JsonObject object, String field) {
        if (object.has(field)) return getJsonElementIntegerValue(object.get(field), field);
        throw new JsonSyntaxException("Missing " + field + ", expected to find an Int");
    }

    public static int getJsonObjectIntegerFieldValueOrDefault(JsonObject object, String field, int fallback) {
        return object.has(field) ? getJsonElementIntegerValue(object.get(field), field) : fallback;
    }

    public static JsonObject getJsonElementAsJsonObject(JsonElement element, String name) {
        if (element.isJsonObject()) return element.getAsJsonObject();
        throw new JsonSyntaxException("Expected " + name + " to be a JsonObject, was " + describe(element));
    }

    public static JsonObject getJsonObject(JsonObject object, String field) {
        if (object.has(field)) return getJsonElementAsJsonObject(object.get(field), field);
        throw new JsonSyntaxException("Missing " + field + ", expected to find a JsonObject");
    }

    public static JsonObject getJsonObjectFieldOrDefault(JsonObject object, String field, JsonObject fallback) {
        return object.has(field) ? getJsonElementAsJsonObject(object.get(field), field) : fallback;
    }

    public static JsonArray getJsonElementAsJsonArray(JsonElement element, String name) {
        if (element.isJsonArray()) return element.getAsJsonArray();
        throw new JsonSyntaxException("Expected " + name + " to be a JsonArray, was " + describe(element));
    }

    public static JsonArray getJsonObjectJsonArrayField(JsonObject object, String field) {
        if (object.has(field)) return getJsonElementAsJsonArray(object.get(field), field);
        throw new JsonSyntaxException("Missing " + field + ", expected to find a JsonArray");
    }

    public static JsonArray getJsonObjectJsonArrayFieldOrDefault(JsonObject object, String field, JsonArray fallback) {
        return object.has(field) ? getJsonElementAsJsonArray(object.get(field), field) : fallback;
    }

    /** The game's description of a JSON value for its error messages ("a string (value)", "null"...). */
    private static String describe(JsonElement element) {
        if (element == null || element.isJsonNull()) return "null (missing)";
        String text = element.toString();
        if (text.length() > 10) text = text.substring(0, 10) + "...";
        if (element.isJsonArray()) return "an array (" + text + ")";
        if (element.isJsonObject()) return "an object (" + text + ")";
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) return "a number (" + text + ")";
            if (primitive.isBoolean()) return "a boolean (" + text + ")";
        }
        return text;
    }
}
