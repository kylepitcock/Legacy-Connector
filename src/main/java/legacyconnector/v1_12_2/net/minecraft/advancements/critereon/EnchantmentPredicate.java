package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment;
import legacyconnector.v1_12_2.net.minecraft.util.JsonUtils;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** An enchantment (any, when null) whose level must be in a range. */
public class EnchantmentPredicate {
    public static final EnchantmentPredicate ANY = new EnchantmentPredicate();
    private final Enchantment enchantment;
    private final MinMaxBounds levels;

    public EnchantmentPredicate() {
        this.enchantment = null;
        this.levels = MinMaxBounds.UNBOUNDED;
    }

    public EnchantmentPredicate(Enchantment enchantment, MinMaxBounds levels) {
        this.enchantment = enchantment;
        this.levels = levels;
    }

    public boolean test(Map<Enchantment, Integer> enchantmentsIn) {
        if (enchantment != null) {
            if (!enchantmentsIn.containsKey(enchantment)) return false;
            int level = enchantmentsIn.get(enchantment);
            return levels == null || levels.test(level);
        }
        if (levels != null) {
            for (Integer level : enchantmentsIn.values()) if (levels.test(level)) return true;
            return false;
        }
        return true;
    }

    public static EnchantmentPredicate deserialize(JsonElement element) {
        if (element == null || element.isJsonNull()) return ANY;
        JsonObject json = JsonUtils.getJsonObject(element, "enchantment");
        Enchantment ench = null;
        if (json.has("enchantment")) {
            ResourceLocation id = new ResourceLocation(JsonUtils.getString(json, "enchantment"));
            ench = Enchantment.REGISTRY.getObject(id);
            if (ench == null) throw new JsonSyntaxException("Unknown enchantment '" + id + "'");
        }
        return new EnchantmentPredicate(ench, MinMaxBounds.deserialize(json.get("levels")));
    }

    public static EnchantmentPredicate[] deserializeArray(JsonElement element) {
        if (element == null || element.isJsonNull()) return new EnchantmentPredicate[0];
        JsonArray array = JsonUtils.getJsonArray(element, "enchantments");
        EnchantmentPredicate[] out = new EnchantmentPredicate[array.size()];
        for (int i = 0; i < out.length; i++) out[i] = deserialize(array.get(i));
        return out;
    }
}
