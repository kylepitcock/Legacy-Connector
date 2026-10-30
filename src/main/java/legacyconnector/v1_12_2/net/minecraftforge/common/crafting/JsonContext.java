package legacyconnector.v1_12_2.net.minecraftforge.common.crafting;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;

/**
 * Context of a recipe JSON file: the mod it belongs to and the ingredient constants of its {@code _constants.json}, which recipes
 * name as {@code "item": "#NAME"}.
 */
public class JsonContext {
    private final String modId;
    private final Map<String, Ingredient> constants = new HashMap<>();

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

    /** The ingredient constant of that name (without the leading #), or null. */
    public Ingredient getConstant(String name) {
        return constants.get(name);
    }

    /** Reads a {@code _constants.json} array: each entry names an ingredient ({@code name}, {@code ingredient}, optional conditions). */
    void loadConstants(com.google.gson.JsonObject[] jsons) {
        if (jsons == null) return;
        for (com.google.gson.JsonObject json : jsons) {
            if (json == null || !json.has("name") || !json.has("ingredient")) continue;
            try {
                constants.put(json.get("name").getAsString(), CraftingHelper.getIngredient(json.get("ingredient"), this));
            } catch (RuntimeException e) {
                // an ingredient this game cannot read is left out
            }
        }
    }

    /** Adds a constant (the layer's recipe loading, which reads the constants file itself). */
    public void mcc$addConstant(String name, Ingredient ingredient) {
        if (name != null && ingredient != null) constants.put(name, ingredient);
    }
}
