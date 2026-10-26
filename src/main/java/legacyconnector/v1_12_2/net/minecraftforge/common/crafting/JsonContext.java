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

    /** Prefixes a name without a domain with this context's mod id ("gear" becomes "modid:gear"). */
    public String appendModId(String data) {
        return data.indexOf(':') == -1 ? modId + ":" + data : data;
    }

    public com.google.gson.JsonElement getConstant(String name) {
        return constants.get(name);
    }

    public void addConstant(String name, com.google.gson.JsonElement value) {
        constants.put(name, value);
    }
}
