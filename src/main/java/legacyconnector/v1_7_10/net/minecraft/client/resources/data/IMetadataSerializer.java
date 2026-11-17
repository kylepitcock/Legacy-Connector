package legacyconnector.v1_7_10.net.minecraft.client.resources.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads metadata sections of .mcmeta files. The vanilla sections ("pack", "animation", "texture", "language") are read here, and so are the
 * sections mods register a serializer for ({@link #registerMetadataSectionType}): their Gson deserializer reads the section's JSON into the
 * class they named, as 1.7.10 did.
 */
public class IMetadataSerializer {
    /** Section name -> the serializer and the class it makes. */
    private final java.util.Map<String, Object[]> registered = new java.util.concurrent.ConcurrentHashMap<>();

    public void registerMetadataSectionType(IMetadataSectionSerializer serializer, Class<?> type) {
        if (serializer == null || serializer.getSectionName() == null) return;
        registered.put(serializer.getSectionName(), new Object[] {serializer, type});
    }

    /** The named section of a parsed .mcmeta file, or null when it has none. */
    public IMetadataSection parseMetadataSection(String section, JsonObject json) {
        if (section == null || json == null || !(json.get(section) instanceof JsonElement element)) return null;
        Object[] custom = registered.get(section);
        try {
            if (custom != null) {
                return (IMetadataSection) ((IMetadataSectionSerializer) custom[0]).deserialize(element, (Class<?>) custom[1], mcc$context());
            }
            if (!(element instanceof JsonObject body)) return null;
            return switch (section) {
                case "animation" -> mcc$animation(body);
                case "texture" -> mcc$texture(body);
                case "pack" -> (IMetadataSection) new PackMetadataSectionSerializer().deserialize(body, PackMetadataSection.class, null);
                case "language" -> (IMetadataSection) new LanguageMetadataSectionSerializer().deserialize(body, LanguageMetadataSection.class, null);
                default -> null;
            };
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** A Gson deserialization context that knows the registered serializers (so a section class can nest other registered classes). */
    private com.google.gson.JsonDeserializationContext mcc$context() {
        com.google.gson.GsonBuilder builder = new com.google.gson.GsonBuilder();
        for (Object[] r : registered.values()) builder.registerTypeHierarchyAdapter((Class<?>) r[1], r[0]);
        com.google.gson.Gson gson = builder.create();
        return new com.google.gson.JsonDeserializationContext() {
            @Override
            public <T> T deserialize(JsonElement json, java.lang.reflect.Type type) {
                return gson.fromJson(json, type);
            }
        };
    }

    static TextureMetadataSection mcc$texture(JsonObject body) {
        return new TextureMetadataSection(mcc$bool(body, "blur"), mcc$bool(body, "clamp"), mcc$mipmaps(body));
    }

    /** A section of raw .mcmeta bytes (not a 1.7.10 method). */
    public static IMetadataSection mcc$parse(byte[] mcmeta, String section) {
        try {
            JsonElement root = com.google.gson.JsonParser.parseString(new String(mcmeta, java.nio.charset.StandardCharsets.UTF_8));
            return root.isJsonObject() ? new IMetadataSerializer().parseMetadataSection(section, root.getAsJsonObject()) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean mcc$bool(JsonObject o, String key) {
        return o.has(key) && o.get(key).getAsBoolean();
    }

    private static int mcc$int(JsonObject o, String key, int fallback) {
        return o.has(key) ? o.get(key).getAsInt() : fallback;
    }

    private static List mcc$mipmaps(JsonObject o) {
        List<Integer> out = new ArrayList<>();
        if (o.get("mipmaps") instanceof JsonArray list) for (JsonElement e : list) out.add(e.getAsInt());
        return out;
    }

    static AnimationMetadataSection mcc$animation(JsonObject o) {
        List<AnimationFrame> frames = new ArrayList<>();
        int frameTime = Math.max(1, mcc$int(o, "frametime", 1));
        if (o.get("frames") instanceof JsonArray list) {
            for (JsonElement e : list) {
                if (e.isJsonPrimitive()) {
                    frames.add(new AnimationFrame(e.getAsInt()));
                } else if (e instanceof JsonObject frame && frame.has("index")) {
                    frames.add(new AnimationFrame(frame.get("index").getAsInt(), mcc$int(frame, "time", -1)));
                }
            }
        }
        return new AnimationMetadataSection(frames, mcc$int(o, "width", -1), mcc$int(o, "height", -1), frameTime);
    }
}
