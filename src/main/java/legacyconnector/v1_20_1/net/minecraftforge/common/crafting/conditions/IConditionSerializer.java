package legacyconnector.v1_20_1.net.minecraftforge.common.crafting.conditions;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/** Forge 1.20.1's recipe condition serializer (NeoForge 1.21.1 uses codecs); remembered by {@code CraftingHelper.register}, not consulted when data is read. */
public interface IConditionSerializer<T> {
    void write(JsonObject json, T value);

    T read(JsonObject json);

    ResourceLocation getID();

    default JsonObject getJson(T value) {
        JsonObject json = new JsonObject();
        write(json, value);
        json.addProperty("type", getID().toString());
        return json;
    }
}
