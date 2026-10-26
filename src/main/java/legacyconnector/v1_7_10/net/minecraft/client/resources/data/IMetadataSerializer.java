package legacyconnector.v1_7_10.net.minecraft.client.resources.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads metadata sections of .mcmeta files. The vanilla sections legacy code asks for ("animation", "texture") are read
 * here; serializers mods register for their own sections are not used (the modern game reads its own metadata).
 */
public class IMetadataSerializer {
    public void registerMetadataSectionType(Object serializer, Class<?> type) {
    }

    /** The named section of a parsed .mcmeta file, or null when it has none. */
    public IMetadataSection parseMetadataSection(String section, JsonObject json) {
        if (section == null || json == null || !(json.get(section) instanceof JsonObject body)) return null;
        try {
            return switch (section) {
                case "animation" -> mcc$animation(body);
                case "texture" -> new TextureMetadataSection(mcc$bool(body, "blur"), mcc$bool(body, "clamp"), mcc$mipmaps(body));
                default -> null;
            };
        } catch (RuntimeException e) {
            return null;
        }
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

    private static AnimationMetadataSection mcc$animation(JsonObject o) {
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
