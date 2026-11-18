package legacyconnector.v1_7_10.net.minecraft.client.resources.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_7_10.net.minecraft.client.resources.Language;

/** Reads {@code "language": {"xx_YY": {"name": "...", "region": "...", "bidirectional": false}}}. */
public class LanguageMetadataSectionSerializer extends BaseMetadataSectionSerializer {
    @Override
    public Object deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject json = element.getAsJsonObject();
        Set<Language> languages = new HashSet<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            JsonObject body = entry.getValue().getAsJsonObject();
            String region = body.get("region").getAsString();
            String name = body.get("name").getAsString();
            boolean bidirectional = body.has("bidirectional") && body.get("bidirectional").getAsBoolean();
            if (region.isEmpty()) throw new JsonParseException("Invalid language->'" + entry.getKey() + "'->region: empty value");
            if (name.isEmpty()) throw new JsonParseException("Invalid language->'" + entry.getKey() + "'->name: empty value");
            if (!languages.add(new Language(entry.getKey(), region, name, bidirectional))) {
                throw new JsonParseException("Duplicate language->'" + entry.getKey() + "' defined");
            }
        }
        return new LanguageMetadataSection(languages);
    }

    @Override
    public String getSectionName() {
        return "language";
    }
}
