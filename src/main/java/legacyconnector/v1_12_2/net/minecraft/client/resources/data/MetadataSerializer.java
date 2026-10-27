package legacyconnector.v1_12_2.net.minecraft.client.resources.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads {@code .mcmeta} sections. Serializers mods register are used for their sections; the vanilla sections ("animation", "texture",
 * "pack") are read here when nobody registered one.
 */
public class MetadataSerializer {
    private final Map<String, IMetadataSectionSerializer<?>> serializers = new java.util.concurrent.ConcurrentHashMap<>();

    public MetadataSerializer() {
    }

    public <T extends IMetadataSection> void registerMetadataSectionType(IMetadataSectionSerializer<T> serializer, Class<T> clazz) {
        if (serializer != null && serializer.getSectionName() != null) serializers.put(serializer.getSectionName(), serializer);
    }

    @SuppressWarnings("unchecked")
    public <T extends IMetadataSection> T parseMetadataSection(String section, JsonObject json) {
        if (section == null) throw new IllegalArgumentException("Metadata section name cannot be null");
        if (json == null || !json.has(section)) return null;
        if (!json.get(section).isJsonObject()) throw new IllegalArgumentException("Invalid metadata for '" + section + "' - expected object");
        JsonObject body = json.getAsJsonObject(section);
        IMetadataSectionSerializer<?> s = serializers.get(section);
        if (s != null) return (T) s.deserialize(body, IMetadataSection.class, null);
        return (T) vanilla(section, body);
    }

    /** A section of raw .mcmeta bytes (not a 1.12 method); null when it is missing or broken. */
    public static IMetadataSection mcc$parse(MetadataSerializer serializer, byte[] mcmeta, String section) {
        try {
            JsonElement root = com.google.gson.JsonParser.parseString(new String(mcmeta, java.nio.charset.StandardCharsets.UTF_8));
            return root.isJsonObject() ? (serializer == null ? new MetadataSerializer() : serializer).parseMetadataSection(section, root.getAsJsonObject()) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static IMetadataSection vanilla(String section, JsonObject o) {
        return switch (section) {
            case "animation" -> animation(o);
            case "texture" -> new TextureMetadataSection(bool(o, "blur"), bool(o, "clamp"));
            case "pack" -> new PackMetadataSection(o.has("description")
                    ? legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent.Serializer.mcc$fromTree(o.get("description"))
                    : new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(""), integer(o, "pack_format", 3));
            default -> null;
        };
    }

    private static boolean bool(JsonObject o, String key) {
        return o.has(key) && o.get(key).getAsBoolean();
    }

    private static int integer(JsonObject o, String key, int fallback) {
        return o.has(key) ? o.get(key).getAsInt() : fallback;
    }

    private static AnimationMetadataSection animation(JsonObject o) {
        List<AnimationFrame> frames = new ArrayList<>();
        int frameTime = Math.max(1, integer(o, "frametime", 1));
        if (o.get("frames") instanceof JsonArray list) {
            for (JsonElement e : list) {
                if (e.isJsonPrimitive()) frames.add(new AnimationFrame(e.getAsInt()));
                else if (e instanceof JsonObject f && f.has("index")) frames.add(new AnimationFrame(f.get("index").getAsInt(), integer(f, "time", -1)));
            }
        }
        return new AnimationMetadataSection(frames, integer(o, "width", -1), integer(o, "height", -1), frameTime, bool(o, "interpolate"));
    }
}
