package legacyconnector.v1_12_2.net.minecraftforge.common.crafting;

import java.util.HashMap;
import java.util.Map;

/** Context of a recipe JSON file: the mod it belongs to and its named constants. */
public class JsonContext {
    private final String modId;
    private final Map<String, com.google.gson.JsonElement> constants = new HashMap<>();

    public JsonContext(String modId) {
        this.modId = modId;
    }

    public String getModId() {
        return modId;
    }

    public com.google.gson.JsonElement getConstant(String name) {
        return constants.get(name);
    }

    public void addConstant(String name, com.google.gson.JsonElement value) {
        constants.put(name, value);
    }
}
