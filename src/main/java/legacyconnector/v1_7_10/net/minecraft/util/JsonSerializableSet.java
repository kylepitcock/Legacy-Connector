package legacyconnector.v1_7_10.net.minecraft.util;

import com.google.common.collect.ForwardingSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Set;

/** A set that saves itself as a JSON array (1.7.10 {@code JsonSerializableSet}): the achievements a player has, as the statistics file stores them. */
public class JsonSerializableSet extends ForwardingSet implements IJsonSerializable {
    private final Set underlying = Sets.newHashSet();

    public JsonSerializableSet() {
    }

    @Override
    public void func_152753_a(JsonElement json) {
        if (json != null && json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) underlying.add(element.getAsString());
        }
    }

    @Override
    public JsonElement func_151003_a() {
        JsonArray array = new JsonArray();
        for (Object o : underlying) array.add(new JsonPrimitive((String) o));
        return array;
    }

    @Override
    protected Set delegate() {
        return underlying;
    }
}
