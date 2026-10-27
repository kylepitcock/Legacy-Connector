package legacyconnector.v1_12_2.net.minecraft.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import legacyconnector.v1_12_2.net.minecraft.item.Item;

/** Helpers for reading JSON the way 1.12 mods do. */
public class JsonUtils {
    public static boolean isString(JsonObject json, String memberName) {
        return isJsonPrimitive(json, memberName) && json.getAsJsonPrimitive(memberName).isString();
    }

    public static boolean isString(JsonElement json) {
        return json.isJsonPrimitive() && json.getAsJsonPrimitive().isString();
    }

    public static boolean isNumber(JsonElement json) {
        return json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber();
    }

    public static boolean isBoolean(JsonObject json, String memberName) {
        return isJsonPrimitive(json, memberName) && json.getAsJsonPrimitive(memberName).isBoolean();
    }

    public static boolean isJsonArray(JsonObject json, String memberName) {
        return hasField(json, memberName) && json.get(memberName).isJsonArray();
    }

    public static boolean isJsonPrimitive(JsonObject json, String memberName) {
        return hasField(json, memberName) && json.get(memberName).isJsonPrimitive();
    }

    public static boolean hasField(JsonObject json, String memberName) {
        return json != null && json.get(memberName) != null;
    }

    public static String getString(JsonElement json, String memberName) {
        if (json.isJsonPrimitive()) return json.getAsString();
        throw new JsonSyntaxException("Expected " + memberName + " to be a string, was " + toString(json));
    }

    public static String getString(JsonObject json, String memberName) {
        if (json.has(memberName)) return getString(json.get(memberName), memberName);
        throw new JsonSyntaxException("Missing " + memberName + ", expected to find a string");
    }

    public static String getString(JsonObject json, String memberName, String fallback) {
        return json.has(memberName) ? getString(json.get(memberName), memberName) : fallback;
    }

    public static Item getItem(JsonElement json, String memberName) {
        if (json.isJsonPrimitive()) {
            String s = json.getAsString();
            Item item = Item.REGISTRY.getObject(new ResourceLocation(s));
            if (item == null) throw new JsonSyntaxException("Expected " + memberName + " to be an item, was unknown string '" + s + "'");
            return item;
        }
        throw new JsonSyntaxException("Expected " + memberName + " to be an item, was " + toString(json));
    }

    public static Item getItem(JsonObject json, String memberName) {
        if (json.has(memberName)) return getItem(json.get(memberName), memberName);
        throw new JsonSyntaxException("Missing " + memberName + ", expected to find an item");
    }

    public static boolean getBoolean(JsonElement json, String memberName) {
        if (json.isJsonPrimitive()) return json.getAsBoolean();
        throw new JsonSyntaxException("Expected " + memberName + " to be a Boolean, was " + toString(json));
    }

    public static boolean getBoolean(JsonObject json, String memberName) {
        if (json.has(memberName)) return getBoolean(json.get(memberName), memberName);
        throw new JsonSyntaxException("Missing " + memberName + ", expected to find a Boolean");
    }

    public static boolean getBoolean(JsonObject json, String memberName, boolean fallback) {
        return json.has(memberName) ? getBoolean(json.get(memberName), memberName) : fallback;
    }

    public static float getFloat(JsonElement json, String memberName) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) return json.getAsFloat();
        throw new JsonSyntaxException("Expected " + memberName + " to be a Float, was " + toString(json));
    }

    public static float getFloat(JsonObject json, String memberName) {
        if (json.has(memberName)) return getFloat(json.get(memberName), memberName);
        throw new JsonSyntaxException("Missing " + memberName + ", expected to find a Float");
    }

    public static float getFloat(JsonObject json, String memberName, float fallback) {
        return json.has(memberName) ? getFloat(json.get(memberName), memberName) : fallback;
    }

    public static int getInt(JsonElement json, String memberName) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) return json.getAsInt();
        throw new JsonSyntaxException("Expected " + memberName + " to be a Int, was " + toString(json));
    }

    public static int getInt(JsonObject json, String memberName) {
        if (json.has(memberName)) return getInt(json.get(memberName), memberName);
        throw new JsonSyntaxException("Missing " + memberName + ", expected to find a Int");
    }

    public static int getInt(JsonObject json, String memberName, int fallback) {
        return json.has(memberName) ? getInt(json.get(memberName), memberName) : fallback;
    }

    public static JsonObject getJsonObject(JsonElement json, String memberName) {
        if (json.isJsonObject()) return json.getAsJsonObject();
        throw new JsonSyntaxException("Expected " + memberName + " to be a JsonObject, was " + toString(json));
    }

    public static JsonObject getJsonObject(JsonObject json, String memberName) {
        if (json.has(memberName)) return getJsonObject(json.get(memberName), memberName);
        throw new JsonSyntaxException("Missing " + memberName + ", expected to find a JsonObject");
    }

    public static JsonObject getJsonObject(JsonObject json, String memberName, JsonObject fallback) {
        return json.has(memberName) ? getJsonObject(json.get(memberName), memberName) : fallback;
    }

    public static JsonArray getJsonArray(JsonElement json, String memberName) {
        if (json.isJsonArray()) return json.getAsJsonArray();
        throw new JsonSyntaxException("Expected " + memberName + " to be a JsonArray, was " + toString(json));
    }

    public static JsonArray getJsonArray(JsonObject json, String memberName) {
        if (json.has(memberName)) return getJsonArray(json.get(memberName), memberName);
        throw new JsonSyntaxException("Missing " + memberName + ", expected to find a JsonArray");
    }

    public static JsonArray getJsonArray(JsonObject json, String memberName, JsonArray fallback) {
        return json.has(memberName) ? getJsonArray(json.get(memberName), memberName) : fallback;
    }

    public static <T> T gsonDeserialize(Gson gson, String json, Class<T> adapter, boolean lenient) {
        return gsonDeserialize(gson, new StringReader(json), adapter, lenient);
    }

    public static <T> T gsonDeserialize(Gson gson, java.io.Reader reader, Class<T> adapter, boolean lenient) {
        try {
            JsonReader jsonreader = new JsonReader(reader);
            jsonreader.setLenient(lenient);
            return gson.getAdapter(adapter).read(jsonreader);
        } catch (java.io.IOException e) {
            throw new JsonParseException(e);
        }
    }

    public static <T> T fromJson(Gson gson, java.io.Reader reader, Class<T> adapter) {
        return gsonDeserialize(gson, reader, adapter, false);
    }

    public static <T> T fromJson(Gson gson, String json, Class<T> adapter) {
        return gsonDeserialize(gson, json, adapter, false);
    }

    public static <T> T fromJson(Gson gson, java.io.Reader reader, Class<T> adapter, boolean lenient) {
        return gsonDeserialize(gson, reader, adapter, lenient);
    }

    public static JsonObject gsonDeserialize(String json) {
        return com.google.gson.JsonParser.parseString(json).getAsJsonObject();
    }

    public static String toString(JsonElement json) {
        String s = String.valueOf(json);
        return s.length() > 10 ? s.substring(0, 10) + "..." : s;
    }

    // ---- area F

    /** The member deserialized as {@code adapter} through the context (loot tables and advancements read their parts so). */
    public static <T> T deserializeClass(JsonElement json, String memberName, com.google.gson.JsonDeserializationContext context, Class<? extends T> adapter) {
        if (json != null) return context.deserialize(json, adapter);
        throw new com.google.gson.JsonSyntaxException("Missing " + memberName);
    }

    public static <T> T deserializeClass(JsonObject json, String memberName, com.google.gson.JsonDeserializationContext context, Class<? extends T> adapter) {
        if (json.has(memberName)) return deserializeClass(json.get(memberName), memberName, context, adapter);
        throw new com.google.gson.JsonSyntaxException("Missing " + memberName);
    }

    public static <T> T deserializeClass(JsonObject json, String memberName, T fallback, com.google.gson.JsonDeserializationContext context,
            Class<? extends T> adapter) {
        return json.has(memberName) ? deserializeClass(json.get(memberName), memberName, context, adapter) : fallback;
    }

    public static long getLong(JsonElement json, String memberName) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) return json.getAsLong();
        throw new com.google.gson.JsonSyntaxException("Expected " + memberName + " to be a Long, was " + toString(json));
    }

    public static long getLong(JsonObject json, String memberName) {
        if (json.has(memberName)) return getLong(json.get(memberName), memberName);
        throw new com.google.gson.JsonSyntaxException("Missing " + memberName + ", expected to find a Long");
    }

    public static long getLong(JsonObject json, String memberName, long fallback) {
        return json.has(memberName) ? getLong(json.get(memberName), memberName) : fallback;
    }
}
