package legacyconnector.v1_7_10.net.minecraft.client.audio;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;

/** Reads one event of a sounds.json: {@code replace}, {@code category} and the {@code sounds} (names, or objects with volume, pitch, weight, type and stream). */
public class SoundListSerializer implements JsonDeserializer<SoundList> {
    @Override
    public SoundList deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonObject()) throw new JsonParseException("Expected entry to be a JsonObject, was " + json);
        JsonObject object = json.getAsJsonObject();
        SoundList list = new SoundList();
        list.setReplaceExisting(object.has("replace") && object.get("replace").getAsBoolean());
        String categoryName = object.has("category") ? object.get("category").getAsString() : SoundCategory.MASTER.getCategoryName();
        SoundCategory category = SoundCategory.func_147154_a(categoryName);
        if (category == null) throw new JsonParseException("Invalid category: " + categoryName);
        list.setSoundCategory(category);
        if (object.has("sounds")) {
            JsonArray sounds = object.getAsJsonArray("sounds");
            for (JsonElement element : sounds) {
                SoundList.SoundEntry entry = new SoundList.SoundEntry();
                if (element.isJsonPrimitive()) {
                    entry.setSoundEntryName(element.getAsString());
                } else {
                    JsonObject o = element.getAsJsonObject();
                    if (!o.has("name")) throw new JsonParseException("Missing sound name in " + element);
                    entry.setSoundEntryName(o.get("name").getAsString());
                    if (o.has("type")) {
                        SoundList.SoundEntry.Type entryType = SoundList.SoundEntry.Type.getType(o.get("type").getAsString());
                        if (entryType == null) throw new JsonParseException("Invalid type: " + o.get("type"));
                        entry.setSoundEntryType(entryType);
                    }
                    if (o.has("volume")) entry.setSoundEntryVolume(o.get("volume").getAsFloat());
                    if (o.has("pitch")) entry.setSoundEntryPitch(o.get("pitch").getAsFloat());
                    if (o.has("weight")) entry.setSoundEntryWeight(o.get("weight").getAsInt());
                    if (o.has("stream")) entry.setStreaming(o.get("stream").getAsBoolean());
                }
                list.getSoundList().add(entry);
            }
        }
        return list;
    }
}
