package legacyconnector.v1_7_10.net.minecraft.client.resources.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;

/** Reads {@code "animation": {"frametime": 1, "frames": [...], "width": 16, "height": 16}}. */
public class AnimationMetadataSectionSerializer extends BaseMetadataSectionSerializer {
    @Override
    public Object deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
        return IMetadataSerializer.mcc$animation(element.getAsJsonObject());
    }

    @Override
    public String getSectionName() {
        return "animation";
    }
}
