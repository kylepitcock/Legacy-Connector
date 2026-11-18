package legacyconnector.v1_7_10.net.minecraft.client.resources.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText;

/** Reads {@code "pack": {"pack_format": 1, "description": "..."}}. */
public class PackMetadataSectionSerializer extends BaseMetadataSectionSerializer {
    @Override
    public Object deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject json = element.getAsJsonObject();
        String description = json.has("description") ? json.get("description").getAsString() : "";
        int format = json.has("pack_format") ? json.get("pack_format").getAsInt() : 1;
        return new PackMetadataSection(new ChatComponentText(description), format);
    }

    @Override
    public String getSectionName() {
        return "pack";
    }
}
